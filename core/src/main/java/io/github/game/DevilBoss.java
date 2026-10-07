package io.github.game;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Boss 2: Flame Devil. Three skills:
 * 1) Magma rain   - a magma ball falls at a random spot on the map every 2 s.
 * 2) Double laser - 2 lasers (one low, one high) at random heights every 5 s.
 * 3) Shield       - an area shield makes the boss invincible for 3 s, every 10 s.
 * Drops the "Shield" skill when defeated.
 */
public final class DevilBoss extends Boss {
    private static final int MAX_HP = 600;

    private static final float MAGMA_INTERVAL = 2f;
    private static final float MAGMA_FIRST_DELAY = 1.5f;
    private static final float MAGMA_SPAWN_Y = 560f;
    private static final float MAGMA_FALL_SPEED = 380f;
    private static final float MAGMA_RADIUS = 17f;
    private static final float MAGMA_FLOOR_Y = 62f;
    private static final float SPLASH_TIME = 0.35f;
    private static final int MAGMA_DAMAGE = 15;

    private static final float SHIELD_INTERVAL = 10f;
    private static final float SHIELD_FIRST_DELAY = 8f;
    private static final float SHIELD_DURATION = 3f;

    private final Array<Magma> magmas = new Array<>();
    private final Rectangle magmaBox = new Rectangle();
    private final Rectangle hitBox = new Rectangle();
    private float magmaTimer = MAGMA_FIRST_DELAY;
    private float shieldTimer = SHIELD_FIRST_DELAY;
    private float shieldRemaining;
    private float effectTime;

    public DevilBoss(float worldWidth, float groundY) {
        super("Flame Devil", SkillType.SHIELD, MAX_HP, "boss/devil.png", BossLaser.randomPair(),
            worldWidth - 200f, groundY, 170f, 170f, 249f);
    }

    @Override
    protected void updateSpecialSkill(float delta, Player player) {
        effectTime += delta;
        updateMagma(delta, player);
        updateShield(delta);
    }

    private void updateMagma(float delta, Player player) {
        magmaTimer -= delta;
        if (magmaTimer <= 0f) {
            float landingX = MathUtils.random(20f, x - 60f);
            magmas.add(new Magma(landingX));
            magmaTimer = MAGMA_INTERVAL;
        }

        for (int index = magmas.size - 1; index >= 0; index--) {
            Magma magma = magmas.get(index);
            if (magma.splashTime > 0f) {
                magma.splashTime -= delta;
                if (magma.splashTime <= 0f) {
                    magmas.removeIndex(index);
                }
                continue;
            }

            magma.y -= MAGMA_FALL_SPEED * delta;
            magmaBox.set(magma.x - MAGMA_RADIUS * 0.8f, magma.y - MAGMA_RADIUS * 0.8f,
                MAGMA_RADIUS * 1.6f, MAGMA_RADIUS * 1.6f);
            if (magmaBox.overlaps(player.getBounds())) {
                player.takeDamage(MAGMA_DAMAGE);
                magmas.removeIndex(index);
            } else if (magma.y <= MAGMA_FLOOR_Y) {
                magma.y = MAGMA_FLOOR_Y;
                magma.splashTime = SPLASH_TIME;
            }
        }
    }

    private void updateShield(float delta) {
        if (shieldRemaining > 0f) {
            shieldRemaining = Math.max(0f, shieldRemaining - delta);
            return;
        }
        shieldTimer -= delta;
        if (shieldTimer <= 0f) {
            shieldRemaining = SHIELD_DURATION;
            shieldTimer = SHIELD_INTERVAL;
        }
    }

    @Override
    public boolean isShielded() {
        return shieldRemaining > 0f && !isDefeated();
    }

    /** Body only (horns and flame are not hittable). */
    @Override
    public Rectangle getHitBox() {
        return hitBox.set(x + width * 0.12f, y + height * 0.03f, width * 0.76f, height * 0.62f);
    }

    @Override
    protected void drawSpecialSkill(SpriteBatch spriteBatch) {
        // Every effect of this boss is drawn with shapes.
    }

    @Override
    protected void drawSpecialShapes(ShapeRenderer shapeRenderer) {
        for (Magma magma : magmas) {
            if (magma.splashTime > 0f) {
                drawSplash(shapeRenderer, magma);
            } else {
                drawLandingMark(shapeRenderer, magma);
                drawMagmaBall(shapeRenderer, magma);
            }
        }
        if (isShielded()) {
            drawShield(shapeRenderer);
        }
    }

    private void drawLandingMark(ShapeRenderer shapeRenderer, Magma magma) {
        float closeness = 1f - (magma.y - MAGMA_FLOOR_Y) / (MAGMA_SPAWN_Y - MAGMA_FLOOR_Y);
        float alpha = 0.25f + 0.45f * closeness;
        shapeRenderer.setColor(1f, 0.35f, 0f, alpha);
        shapeRenderer.ellipse(magma.x - 26f, MAGMA_FLOOR_Y - 8f, 52f, 14f);
        shapeRenderer.setColor(1f, 0.75f, 0.2f, alpha);
        shapeRenderer.ellipse(magma.x - 12f, MAGMA_FLOOR_Y - 4f, 24f, 7f);
    }

    private void drawMagmaBall(ShapeRenderer shapeRenderer, Magma magma) {
        float flicker = 1f + 0.12f * MathUtils.sin(effectTime * 25f + magma.x);
        shapeRenderer.setColor(1f, 0.45f, 0.05f, 0.25f);
        shapeRenderer.circle(magma.x, magma.y + 22f, MAGMA_RADIUS * 0.7f);
        shapeRenderer.circle(magma.x, magma.y + 38f, MAGMA_RADIUS * 0.45f);
        shapeRenderer.setColor(1f, 0.4f, 0.05f, 0.35f);
        shapeRenderer.circle(magma.x, magma.y, MAGMA_RADIUS * 1.6f * flicker);
        shapeRenderer.setColor(0.85f, 0.18f, 0.05f, 1f);
        shapeRenderer.circle(magma.x, magma.y, MAGMA_RADIUS);
        shapeRenderer.setColor(1f, 0.6f, 0.1f, 1f);
        shapeRenderer.circle(magma.x - 2f, magma.y + 2f, MAGMA_RADIUS * 0.65f);
        shapeRenderer.setColor(1f, 0.92f, 0.5f, 1f);
        shapeRenderer.circle(magma.x - 4f, magma.y + 4f, MAGMA_RADIUS * 0.3f);
    }

    private void drawSplash(ShapeRenderer shapeRenderer, Magma magma) {
        float progress = 1f - magma.splashTime / SPLASH_TIME;
        float alpha = 1f - progress;
        shapeRenderer.setColor(1f, 0.45f, 0.05f, 0.6f * alpha);
        shapeRenderer.ellipse(magma.x - 30f - progress * 20f, MAGMA_FLOOR_Y - 8f,
            60f + progress * 40f, 16f);
        shapeRenderer.setColor(1f, 0.75f, 0.2f, alpha);
        for (int drop = -2; drop <= 2; drop++) {
            float dropX = magma.x + drop * 12f * (1f + progress * 2f);
            float dropY = MAGMA_FLOOR_Y + (30f - drop * drop * 6f) * progress;
            shapeRenderer.circle(dropX, dropY, 4f * alpha + 1f);
        }
    }

    private void drawShield(ShapeRenderer shapeRenderer) {
        float pulse = 1f + 0.04f * MathUtils.sin(effectTime * 8f);
        float radius = height * 0.58f * pulse;
        boolean endingSoon = shieldRemaining < 0.8f && ((int) (effectTime * 10f)) % 2 == 0;
        float alpha = endingSoon ? 0.12f : 0.25f;
        shapeRenderer.setColor(0.35f, 0.75f, 1f, alpha);
        shapeRenderer.circle(getCenterX(), getCenterY(), radius);
        shapeRenderer.setColor(0.7f, 0.9f, 1f, alpha * 0.8f);
        shapeRenderer.circle(getCenterX(), getCenterY(), radius * 0.82f);
        shapeRenderer.setColor(1f, 1f, 1f, alpha);
        shapeRenderer.circle(getCenterX() - radius * 0.4f, getCenterY() + radius * 0.45f, radius * 0.12f);
    }

    @Override
    protected void resetSpecialSkill() {
        magmas.clear();
        magmaTimer = MAGMA_FIRST_DELAY;
        shieldTimer = SHIELD_FIRST_DELAY;
        shieldRemaining = 0f;
        effectTime = 0f;
    }

    private static class Magma {
        private final float x;
        private float y = MAGMA_SPAWN_Y;
        private float splashTime;

        private Magma(float x) {
            this.x = x;
        }
    }
}
