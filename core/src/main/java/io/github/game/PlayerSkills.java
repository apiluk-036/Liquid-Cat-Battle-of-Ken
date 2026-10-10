package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Skills the player collected from bosses.
 * They stay for the next stages and are lost when the game restarts after losing.
 */
public class PlayerSkills {
    private static final float SHIELD_DURATION = 3f;
    private static final float CANDY_SPEED = 360f;
    private static final float CANDY_SIZE = 72f;
    private static final float CANDY_SPIN_SPEED = 720f;
    private static final int CANDY_DAMAGE = 50;
    private static final float CODE_SPEED = 460f;
    private static final float CODE_SIZE = 64f;

    private final Set<SkillType> owned = EnumSet.noneOf(SkillType.class);
    private final Map<SkillType, Float> cooldowns = new EnumMap<>(SkillType.class);
    private final Array<CandyShot> candies = new Array<>();
    private final Rectangle candyBox = new Rectangle();
    private final Texture candyTexture;
    private final Texture codeTexture;
    private float effectTime;

    public PlayerSkills() {
        candyTexture = Gdx.files.internal(SkillType.GIANT_CANDY.iconPath).exists()
            ? new Texture(SkillType.GIANT_CANDY.iconPath) : null;
        codeTexture = Gdx.files.internal(SkillType.CODE_BREATH.iconPath).exists()
            ? new Texture(SkillType.CODE_BREATH.iconPath) : null;
    }

    public void add(SkillType skill) {
        owned.add(skill);
    }

    public boolean has(SkillType skill) {
        return owned.contains(skill);
    }

    public Set<SkillType> getOwned() {
        return owned;
    }

    public float getCooldown(SkillType skill) {
        Float remaining = cooldowns.get(skill);
        return remaining == null ? 0f : remaining;
    }

    public void use(SkillType skill, Player player) {
        if (!has(skill) || getCooldown(skill) > 0f) {
            return;
        }
        if (skill == SkillType.SHIELD) {
            player.activateShield(SHIELD_DURATION);
        } else if (skill == SkillType.GIANT_CANDY) {
            float startX = player.getCenterX() - CANDY_SIZE / 2f;
            float startY = player.getCenterY() - CANDY_SIZE / 2f;
            candies.add(new CandyShot(startX, startY, player.facing, false));
        } else if (skill == SkillType.CODE_BREATH) {
            float startX = player.getCenterX() - CODE_SIZE / 2f;
            float startY = player.getCenterY() - CODE_SIZE / 2f;
            candies.add(new CandyShot(startX, startY, player.facing, true));
        }
        cooldowns.put(skill, skill.cooldown);
    }

    public void update(float delta, Boss boss, float worldWidth) {
        effectTime += delta;
        for (SkillType skill : owned) {
            cooldowns.put(skill, Math.max(0f, getCooldown(skill) - delta));
        }

        for (int index = candies.size - 1; index >= 0; index--) {
            CandyShot candy = candies.get(index);
            float size = candy.code ? CODE_SIZE : CANDY_SIZE;
            candy.x += candy.direction * (candy.code ? CODE_SPEED : CANDY_SPEED) * delta;
            if (!candy.code) {
                candy.rotation -= candy.direction * CANDY_SPIN_SPEED * delta;
            }
            candyBox.set(candy.x + size * 0.15f, candy.y + size * 0.15f, size * 0.7f, size * 0.7f);
            if (!boss.isDefeated() && candyBox.overlaps(boss.getHitBox())) {
                // Code breath cuts the boss's current HP in half.
                int damage = candy.code ? Math.max(1, (boss.getHp() + 1) / 2) : CANDY_DAMAGE;
                boss.takeDamage(damage);
                candies.removeIndex(index);
            } else if (candy.x > worldWidth || candy.x + size < 0f) {
                candies.removeIndex(index);
            }
        }
    }

    public void drawSprites(SpriteBatch spriteBatch) {
        for (CandyShot candy : candies) {
            Texture texture = candy.code ? codeTexture : candyTexture;
            if (texture == null) {
                continue;
            }
            float size = candy.code ? CODE_SIZE : CANDY_SIZE;
            float half = size / 2f;
            if (candy.code) {
                // Fading trail behind the code wave.
                for (int trail = 3; trail >= 1; trail--) {
                    spriteBatch.setColor(1f, 1f, 1f, 0.15f * (4 - trail));
                    spriteBatch.draw(texture, candy.x - candy.direction * trail * 22f, candy.y, size, size);
                }
                spriteBatch.setColor(1f, 1f, 1f, 1f);
            }
            spriteBatch.draw(texture, candy.x, candy.y, half, half, size, size,
                1f, 1f, candy.rotation, 0, 0, texture.getWidth(), texture.getHeight(),
                false, false);
        }
    }

    /** Shield bubble around the player. Call inside shapeRenderer.begin(Filled). */
    public void drawShapes(ShapeRenderer shapeRenderer, Player player) {
        if (!player.isShielded()) {
            return;
        }
        float radius = 58f * (1f + 0.05f * MathUtils.sin(effectTime * 9f));
        shapeRenderer.setColor(0.35f, 0.7f, 1f, 0.22f);
        shapeRenderer.circle(player.getCenterX(), player.getCenterY(), radius);
        shapeRenderer.setColor(0.75f, 0.9f, 1f, 0.18f);
        shapeRenderer.circle(player.getCenterX(), player.getCenterY(), radius * 0.8f);
    }

    /** New stage: clear shots and cooldowns but keep the skills. */
    public void resetForStage() {
        candies.clear();
        cooldowns.clear();
    }

    /** Lost the game: skills are gone. */
    public void clearAll() {
        resetForStage();
        owned.clear();
    }

    public void dispose() {
        if (candyTexture != null) {
            candyTexture.dispose();
        }
        if (codeTexture != null) {
            codeTexture.dispose();
        }
    }

    private static class CandyShot {
        private float x;
        private final float y;
        private final int direction;
        private float rotation;
        private final boolean code;

        private CandyShot(float x, float y, int direction, boolean code) {
            this.code = code;
            this.x = x;
            this.y = y;
            this.direction = direction;
        }
    }
}
