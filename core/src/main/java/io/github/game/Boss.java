package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * Base class for every boss.
 * Shared rules: moves up and down automatically, fires lasers,
 * each boss has its own special skills (implemented by the subclass)
 * and drops a skill for the player when defeated.
 */
public abstract class Boss {
    protected static final float MOVE_SPEED_MEDIUM = 80f;
    private static final float HURT_FLASH_TIME = 0.12f;

    // Animation: the sprite is drawn in thin horizontal strips that wobble like jelly.
    private static final int STRIPS = 32;
    private static final float BREATH_SPEED = 2.6f;
    private static final float SWAY_SPEED = 1.8f;
    private static final float SWAY_AMOUNT = 5f;
    private static final float CAST_TIME = 0.45f;
    private static final float CAST_LEAN = 22f;
    private static final float RECOIL_TIME = 0.35f;
    private static final float KNOCKBACK = 7f;
    private static final float DEATH_TIME = 1.1f;

    protected final String name;
    protected final SkillType rewardSkill;
    protected final int maxHp;
    protected int hp;

    protected float x;
    protected float y;
    protected final float width;
    protected final float height;
    private final float minY;
    private final float maxY;
    private int moveDirection = 1;
    private float hurtTimer;
    private float animTime;
    private float castTimer;
    private float castStrength;
    private boolean castLean;
    private float recoilTimer;
    private float deathTimer;
    private float shakeX;
    private boolean wasFiring;

    private final Texture texture;
    private final BossLaser laser;
    private final Rectangle hitBox = new Rectangle();

    protected Boss(String name, SkillType rewardSkill, int maxHp, String texturePath, BossLaser laser,
                   float x, float minY, float maxY, float width, float height) {
        this.name = name;
        this.rewardSkill = rewardSkill;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.x = x;
        this.y = minY;
        this.minY = minY;
        this.maxY = maxY;
        this.width = width;
        this.height = height;
        this.texture = Gdx.files.internal(texturePath).exists() ? new Texture(texturePath) : null;
        this.laser = laser;
    }

    public void update(float delta, Player player) {
        animTime += delta;
        if (isDefeated()) {
            deathTimer += delta;
            return;
        }
        hurtTimer = Math.max(0f, hurtTimer - delta);
        castTimer = Math.max(0f, castTimer - delta);
        recoilTimer = Math.max(0f, recoilTimer - delta);
        moveUpAndDown(delta);
        laser.update(delta, this, player);
        updateSpecialSkill(delta, player);

        shakeX = laser.isCharging() ? MathUtils.random(-2.5f, 2.5f) : 0f;
        if (laser.isFiring() && !wasFiring) {
            recoilTimer = RECOIL_TIME;
        }
        wasFiring = laser.isFiring();
    }

    /**
     * Plays the "throw" pose: stretch up and lean toward the player.
     * @param strength 0..1, how big the pose is
     * @param lean false = stretch straight up (e.g. casting a shield)
     */
    protected void playCastAnimation(float strength, boolean lean) {
        castTimer = CAST_TIME;
        castStrength = strength;
        castLean = lean;
    }

    private void moveUpAndDown(float delta) {
        y += moveDirection * MOVE_SPEED_MEDIUM * delta;
        if (y >= maxY) {
            y = maxY;
            moveDirection = -1;
        } else if (y <= minY) {
            y = minY;
            moveDirection = 1;
        }
    }

    /** The unique skill of each boss. */
    protected abstract void updateSpecialSkill(float delta, Player player);

    protected abstract void drawSpecialSkill(SpriteBatch spriteBatch);

    protected abstract void resetSpecialSkill();

    /** Effects drawn with shapes (shield bubble, magma...). Called inside shapeRenderer.begin(Filled). */
    protected void drawSpecialShapes(ShapeRenderer shapeRenderer) {
    }

    /** While shielded the boss takes no damage. */
    public boolean isShielded() {
        return false;
    }

    public void drawSprite(SpriteBatch spriteBatch) {
        if (texture == null) {
            return;
        }
        if (isDefeated()) {
            if (deathTimer < DEATH_TIME) {
                drawAnimatedBody(spriteBatch);
            }
            return;
        }
        drawAnimatedBody(spriteBatch);
        drawSpecialSkill(spriteBatch);
    }

    private void drawAnimatedBody(SpriteBatch spriteBatch) {
        float scaleX = 1f;
        float scaleY = 1f;
        float alpha = 1f;

        // Breathing (squash and stretch, feet stay on the ground).
        float breath = MathUtils.sin(animTime * BREATH_SPEED);
        scaleY += 0.035f * breath;
        scaleX -= 0.025f * breath;

        // Cast pose.
        float castCurve = 0f;
        if (castTimer > 0f) {
            float progress = 1f - castTimer / CAST_TIME;
            castCurve = MathUtils.sin(progress * MathUtils.PI) * castStrength;
            scaleY += 0.14f * castCurve;
            scaleX -= 0.07f * castCurve;
        }

        // Melt when defeated.
        if (isDefeated()) {
            float progress = Math.min(1f, deathTimer / DEATH_TIME);
            scaleY *= 1f - progress;
            scaleX *= 1f + 0.5f * progress;
            alpha = 1f - progress * progress;
        }

        float drawWidth = width * scaleX;
        float drawHeight = height * scaleY;
        float baseX = x + (width - drawWidth) / 2f + shakeX
            + KNOCKBACK * (hurtTimer / HURT_FLASH_TIME);
        float lean = castLean ? -CAST_LEAN * castCurve : 0f;
        lean += 12f * (recoilTimer / RECOIL_TIME);

        applyTint(spriteBatch, alpha);
        int textureHeight = texture.getHeight();
        float stripHeight = drawHeight / STRIPS;
        for (int strip = 0; strip < STRIPS; strip++) {
            float heightRatio = (strip + 0.5f) / STRIPS;
            int sourceTop = Math.round(textureHeight * (1f - (strip + 1f) / STRIPS));
            int sourceBottom = Math.round(textureHeight * (1f - (float) strip / STRIPS));
            float offsetX = getWobbleOffset(heightRatio) + lean * heightRatio * heightRatio;
            spriteBatch.draw(texture, baseX + offsetX, y + strip * stripHeight, drawWidth, stripHeight + 0.5f,
                0, sourceTop, texture.getWidth(), sourceBottom - sourceTop, false, false);
        }
        spriteBatch.setColor(1f, 1f, 1f, 1f);
    }

    private void applyTint(SpriteBatch spriteBatch, float alpha) {
        if (isDefeated()) {
            float flash = Math.max(0f, 1f - deathTimer * 4f);
            spriteBatch.setColor(1f, 1f - 0.3f * flash, 1f - 0.3f * flash, alpha);
        } else if (hurtTimer > 0f) {
            spriteBatch.setColor(1f, 0.55f, 0.55f, alpha);
        } else if (laser.isCharging()) {
            float glow = 0.5f + 0.5f * MathUtils.sin(animTime * 20f);
            spriteBatch.setColor(1f, 1f - 0.35f * glow, 1f - 0.35f * glow, alpha);
        } else {
            spriteBatch.setColor(1f, 1f, 1f, alpha);
        }
    }

    /** Sideways jelly wobble; 0 at the feet, strongest at the top. */
    protected float getWobbleOffset(float heightRatio) {
        float wave = MathUtils.sin(animTime * SWAY_SPEED + heightRatio * 1.4f);
        return SWAY_AMOUNT * wave * heightRatio * (float) Math.sqrt(heightRatio);
    }

    protected float getAnimTime() {
        return animTime;
    }

    public void drawShapes(ShapeRenderer shapeRenderer) {
        if (!isDefeated()) {
            drawSpecialShapes(shapeRenderer);
            laser.draw(shapeRenderer);
        }
    }

    /** Returns true if the damage was applied. */
    public boolean takeDamage(int damage) {
        if (isDefeated() || isShielded()) {
            return false;
        }
        hp = Math.max(0, hp - damage);
        hurtTimer = HURT_FLASH_TIME;
        return true;
    }

    public void reset() {
        hp = maxHp;
        y = minY;
        moveDirection = 1;
        hurtTimer = 0f;
        castTimer = 0f;
        recoilTimer = 0f;
        deathTimer = 0f;
        wasFiring = false;
        laser.reset();
        resetSpecialSkill();
    }

    /** Body area only (the smoke above the head is not hittable). */
    public Rectangle getHitBox() {
        return hitBox.set(x + width * 0.15f, y + height * 0.05f, width * 0.65f, height * 0.6f);
    }

    public float getCenterX() {
        return x + width / 2f;
    }

    public float getCenterY() {
        return y + height / 2f;
    }

    /** Where the laser comes out of (the boss mouth). */
    public float getLaserOriginX() {
        return x + width * 0.45f;
    }

    /** Where the special skill is thrown from (the hand holding the candy). */
    public float getHandX() {
        return x + width * 0.82f;
    }

    public float getHandY() {
        return y + height * 0.68f;
    }

    public boolean isDefeated() {
        return hp <= 0;
    }

    public String getName() {
        return name;
    }

    public SkillType getRewardSkill() {
        return rewardSkill;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }
}
