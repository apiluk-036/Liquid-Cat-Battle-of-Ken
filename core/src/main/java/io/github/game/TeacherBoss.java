package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Boss 2: "final class Teacher" (OOP). Three skills:
 * 1) Candy toss   - throws a candy to a random spot on the map every 2 s.
 * 2) Laser        - 2 lasers (one low, one high) at random heights every 5 s.
 * 3) Giant candy  - throws a giant candy to a random spot every 10 s.
 * Drops the "Giant Candy" skill when defeated.
 */
public final class TeacherBoss extends Boss {
    private static final int MAX_HP = 300;

    private static final float GRAVITY = 900f;
    private static final float FLOOR_Y = 62f;
    private static final float SPIN_SPEED = 540f;
    private static final float SPLASH_TIME = 0.3f;

    private static final float CANDY_INTERVAL = 2f;
    private static final float CANDY_FIRST_DELAY = 1.5f;
    private static final float CANDY_SIZE = 46f;
    private static final float CANDY_FLIGHT_TIME = 1.1f;
    private static final int CANDY_DAMAGE = 15;

    private static final float GIANT_INTERVAL = 10f;
    private static final float GIANT_FIRST_DELAY = 7f;
    private static final float GIANT_SIZE = 120f;
    private static final float GIANT_FLIGHT_TIME = 1.6f;
    private static final int GIANT_DAMAGE = 30;

    private final Texture candyTexture;
    private final Array<Candy> candies = new Array<>();
    private final Rectangle candyBox = new Rectangle();
    private float candyTimer = CANDY_FIRST_DELAY;
    private float giantTimer = GIANT_FIRST_DELAY;

    public TeacherBoss(float worldWidth, float groundY) {
        super("final class Teacher", SkillType.GIANT_CANDY, MAX_HP, "boss/teacher.png", BossLaser.randomPair(),
            worldWidth - 230f, groundY, 200f, 200f, 219f);
        candyTexture = Gdx.files.internal("boss/candy.png").exists()
            ? new Texture("boss/candy.png") : null;
    }

    @Override
    protected void updateSpecialSkill(float delta, Player player) {
        candyTimer -= delta;
        if (candyTimer <= 0f) {
            throwCandy(CANDY_SIZE, CANDY_FLIGHT_TIME, CANDY_DAMAGE);
            playCastAnimation(0.6f, true);
            candyTimer = CANDY_INTERVAL;
        }
        giantTimer -= delta;
        if (giantTimer <= 0f) {
            throwCandy(GIANT_SIZE, GIANT_FLIGHT_TIME, GIANT_DAMAGE);
            playCastAnimation(1f, true);
            giantTimer = GIANT_INTERVAL;
        }

        for (int index = candies.size - 1; index >= 0; index--) {
            Candy candy = candies.get(index);
            if (candy.splashTime > 0f) {
                candy.splashTime -= delta;
                if (candy.splashTime <= 0f) {
                    candies.removeIndex(index);
                }
                continue;
            }

            candy.velocityY -= GRAVITY * delta;
            candy.x += candy.velocityX * delta;
            candy.y += candy.velocityY * delta;
            candy.rotation += SPIN_SPEED * delta;

            float hitSize = candy.size * 0.6f;
            candyBox.set(candy.x - hitSize / 2f, candy.y - hitSize / 2f, hitSize, hitSize);
            if (candyBox.overlaps(player.getBounds())) {
                player.takeDamage(candy.damage);
                candies.removeIndex(index);
            } else if (candy.velocityY < 0f && candy.y - candy.size / 2f <= FLOOR_Y) {
                candy.y = FLOOR_Y + candy.size / 2f;
                candy.splashTime = SPLASH_TIME;
            }
        }
    }

    /** Lobs a candy from the hand so it lands on a random spot of the map. */
    private void throwCandy(float size, float flightTime, int damage) {
        float startX = getHandX();
        float startY = getHandY();
        float targetX = MathUtils.random(30f, x - size);
        float targetY = FLOOR_Y + size / 2f;
        float velocityX = (targetX - startX) / flightTime;
        float velocityY = (targetY - startY + 0.5f * GRAVITY * flightTime * flightTime) / flightTime;
        candies.add(new Candy(startX, startY, velocityX, velocityY, targetX, size, damage));
    }

    @Override
    protected void drawSpecialShapes(ShapeRenderer shapeRenderer) {
        for (Candy candy : candies) {
            float markWidth = candy.size * 1.1f;
            if (candy.splashTime > 0f) {
                float progress = 1f - candy.splashTime / SPLASH_TIME;
                shapeRenderer.setColor(1f, 0.45f, 0.7f, 0.5f * (1f - progress));
                float width = markWidth * (1f + progress);
                shapeRenderer.ellipse(candy.x - width / 2f, FLOOR_Y - 8f, width, 16f);
            } else {
                shapeRenderer.setColor(1f, 0.3f, 0.6f, 0.35f);
                shapeRenderer.ellipse(candy.targetX - markWidth / 2f, FLOOR_Y - 8f, markWidth, 16f);
                shapeRenderer.setColor(1f, 0.75f, 0.85f, 0.45f);
                shapeRenderer.ellipse(candy.targetX - markWidth / 4f, FLOOR_Y - 4f, markWidth / 2f, 8f);
            }
        }
    }

    @Override
    protected void drawSpecialSkill(SpriteBatch spriteBatch) {
        if (candyTexture == null) {
            return;
        }
        for (Candy candy : candies) {
            if (candy.splashTime > 0f) {
                continue;
            }
            float half = candy.size / 2f;
            spriteBatch.draw(candyTexture, candy.x - half, candy.y - half, half, half, candy.size, candy.size,
                1f, 1f, candy.rotation, 0, 0, candyTexture.getWidth(), candyTexture.getHeight(),
                false, false);
        }
    }

    @Override
    protected void resetSpecialSkill() {
        candies.clear();
        candyTimer = CANDY_FIRST_DELAY;
        giantTimer = GIANT_FIRST_DELAY;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (candyTexture != null) {
            candyTexture.dispose();
        }
    }

    private static class Candy {
        private float x;
        private float y;
        private final float velocityX;
        private float velocityY;
        private final float targetX;
        private final float size;
        private final int damage;
        private float rotation;
        private float splashTime;

        private Candy(float x, float y, float velocityX, float velocityY, float targetX, float size, int damage) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.targetX = targetX;
            this.size = size;
            this.damage = damage;
        }
    }
}
