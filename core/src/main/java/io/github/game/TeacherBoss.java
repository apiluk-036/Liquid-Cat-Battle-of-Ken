package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Boss 1: "final class Teacher" (OOP stage).
 * Special skill: throws spiky candy at the player.
 * Drops the "Candy" skill when defeated.
 */
public final class TeacherBoss extends Boss {
    private static final int MAX_HP = 500;
    private static final float CANDY_INTERVAL = 3f;
    private static final float CANDY_FIRST_DELAY = 2f;
    private static final float CANDY_SPEED = 260f;
    private static final float CANDY_SIZE = 46f;
    private static final float CANDY_SPIN_SPEED = 540f;
    private static final int CANDY_DAMAGE = 15;

    private final Texture candyTexture;
    private final Array<Candy> candies = new Array<>();
    private final Rectangle candyBox = new Rectangle();
    private float candyTimer = CANDY_FIRST_DELAY;
    private final float worldWidth;

    public TeacherBoss(float worldWidth, float groundY) {
        super("final class Teacher", SkillType.CANDY, MAX_HP, "boss/teacher.png", BossLaser.targeted(),
            worldWidth - 230f, groundY, 200f, 200f, 219f);
        this.worldWidth = worldWidth;
        candyTexture = Gdx.files.internal("boss/candy.png").exists()
            ? new Texture("boss/candy.png") : null;
    }

    @Override
    protected void updateSpecialSkill(float delta, Player player) {
        candyTimer -= delta;
        if (candyTimer <= 0f) {
            throwCandy(player);
            candyTimer = CANDY_INTERVAL;
        }

        for (int index = candies.size - 1; index >= 0; index--) {
            Candy candy = candies.get(index);
            candy.x += candy.velocityX * delta;
            candy.y += candy.velocityY * delta;
            candy.rotation += CANDY_SPIN_SPEED * delta;

            candyBox.set(candy.x + CANDY_SIZE * 0.2f, candy.y + CANDY_SIZE * 0.2f,
                CANDY_SIZE * 0.6f, CANDY_SIZE * 0.6f);
            if (candyBox.overlaps(player.getBounds())) {
                player.takeDamage(CANDY_DAMAGE);
                candies.removeIndex(index);
            } else if (isOutside(candy)) {
                candies.removeIndex(index);
            }
        }
    }

    private void throwCandy(Player player) {
        float startX = getHandX() - CANDY_SIZE / 2f;
        float startY = getHandY() - CANDY_SIZE / 2f;
        float targetX = player.getCenterX() - CANDY_SIZE / 2f;
        float targetY = player.getCenterY() - CANDY_SIZE / 2f;
        float dx = targetX - startX;
        float dy = targetY - startY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1f) {
            return;
        }
        candies.add(new Candy(startX, startY, dx / length * CANDY_SPEED, dy / length * CANDY_SPEED));
    }

    private boolean isOutside(Candy candy) {
        return candy.x + CANDY_SIZE < 0f || candy.x > worldWidth
            || candy.y + CANDY_SIZE < 0f || candy.y > 600f;
    }

    @Override
    protected void drawSpecialSkill(SpriteBatch spriteBatch) {
        if (candyTexture == null) {
            return;
        }
        float half = CANDY_SIZE / 2f;
        for (Candy candy : candies) {
            spriteBatch.draw(candyTexture, candy.x, candy.y, half, half, CANDY_SIZE, CANDY_SIZE,
                1f, 1f, candy.rotation, 0, 0, candyTexture.getWidth(), candyTexture.getHeight(),
                false, false);
        }
    }

    @Override
    protected void resetSpecialSkill() {
        candies.clear();
        candyTimer = CANDY_FIRST_DELAY;
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
        private final float velocityY;
        private float rotation;

        private Candy(float x, float y, float velocityX, float velocityY) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
        }
    }
}
