package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/** The skill item a defeated boss drops. The player walks over it to collect it. */
public class SkillDrop {
    private static final float SIZE = 48f;
    private static final float FALL_SPEED = 260f;
    private static final float REST_Y = 70f;

    private final SkillType skill;
    private final Texture icon;
    private final Rectangle bounds = new Rectangle();
    private final float x;
    private float y;
    private float time;

    public SkillDrop(SkillType skill, float x, float y) {
        this.skill = skill;
        this.x = x;
        this.y = Math.max(REST_Y, y);
        this.icon = Gdx.files.internal(skill.iconPath).exists() ? new Texture(skill.iconPath) : null;
    }

    public void update(float delta) {
        time += delta;
        y = Math.max(REST_Y, y - FALL_SPEED * delta);
    }

    public boolean isTouching(Player player) {
        return bounds.set(x, y, SIZE, SIZE).overlaps(player.getBounds());
    }

    public SkillType getSkill() {
        return skill;
    }

    /** Glow under the item. Call inside shapeRenderer.begin(Filled). */
    public void drawShapes(ShapeRenderer shapeRenderer) {
        float pulse = 0.5f + 0.5f * MathUtils.sin(time * 5f);
        shapeRenderer.setColor(1f, 0.9f, 0.4f, 0.18f + 0.15f * pulse);
        shapeRenderer.circle(x + SIZE / 2f, getDrawY() + SIZE / 2f, SIZE * 0.85f);
    }

    public void drawSprite(SpriteBatch spriteBatch) {
        if (icon != null) {
            spriteBatch.draw(icon, x, getDrawY(), SIZE, SIZE);
        }
    }

    private float getDrawY() {
        return y + 6f * MathUtils.sin(time * 3f);
    }

    public void dispose() {
        if (icon != null) {
            icon.dispose();
        }
    }
}
