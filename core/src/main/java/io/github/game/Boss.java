package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Base class for every boss.
 * Shared rules: moves up and down automatically, fires a laser every 15 seconds,
 * and each boss has its own special skill (implemented by the subclass).
 */
public abstract class Boss {
    protected static final float MOVE_SPEED_MEDIUM = 80f;
    private static final float HURT_FLASH_TIME = 0.12f;

    protected final String name;
    protected final String rewardSkill;
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

    private final Texture texture;
    private final BossLaser laser;
    private final Rectangle hitBox = new Rectangle();

    protected Boss(String name, String rewardSkill, int maxHp, String texturePath,
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
        this.laser = new BossLaser();
    }

    public void update(float delta, Player player) {
        if (isDefeated()) {
            return;
        }
        hurtTimer = Math.max(0f, hurtTimer - delta);
        moveUpAndDown(delta);
        laser.update(delta, this, player);
        updateSpecialSkill(delta, player);
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

    public void drawSprite(SpriteBatch spriteBatch) {
        if (texture == null || isDefeated()) {
            return;
        }
        if (hurtTimer > 0f) {
            spriteBatch.setColor(1f, 0.55f, 0.55f, 1f);
        }
        spriteBatch.draw(texture, x, y, width, height);
        spriteBatch.setColor(1f, 1f, 1f, 1f);
        drawSpecialSkill(spriteBatch);
    }

    public void drawShapes(ShapeRenderer shapeRenderer) {
        if (!isDefeated()) {
            laser.draw(shapeRenderer);
        }
    }

    public void takeDamage(int damage) {
        if (isDefeated()) {
            return;
        }
        hp = Math.max(0, hp - damage);
        hurtTimer = HURT_FLASH_TIME;
    }

    public void reset() {
        hp = maxHp;
        y = minY;
        moveDirection = 1;
        hurtTimer = 0f;
        laser.reset();
        resetSpecialSkill();
    }

    /** Body area only (the smoke above the head is not hittable). */
    public Rectangle getHitBox() {
        return hitBox.set(x + width * 0.15f, y + height * 0.05f, width * 0.65f, height * 0.6f);
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

    public String getRewardSkill() {
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
