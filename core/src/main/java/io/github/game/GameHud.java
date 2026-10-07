package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Boss HP bar, normal-attack shots (5 status) and win / lose screens. */
public class GameHud {
    private static final float BOSS_BAR_WIDTH = 300f;
    private static final float BOSS_BAR_HEIGHT = 18f;
    private static final float PIP_RADIUS = 7f;

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final BitmapFont bigFont = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    public GameHud() {
        bigFont.getData().setScale(2.5f);
    }

    public void draw(SpriteBatch spriteBatch, Boss boss, SkillEffect attack,
                     float worldWidth, float worldHeight) {
        float barX = worldWidth - BOSS_BAR_WIDTH - 24f;
        float barY = worldHeight - 44f;
        float bossPercent = boss.getHp() / (float) boss.getMaxHp();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.12f, 1f);
        shapeRenderer.rect(barX - 3f, barY - 3f, BOSS_BAR_WIDTH + 6f, BOSS_BAR_HEIGHT + 6f);
        shapeRenderer.setColor(0.35f, 0.05f, 0.08f, 1f);
        shapeRenderer.rect(barX, barY, BOSS_BAR_WIDTH, BOSS_BAR_HEIGHT);
        shapeRenderer.setColor(0.93f, 0.25f, 0.45f, 1f);
        shapeRenderer.rect(barX, barY, BOSS_BAR_WIDTH * bossPercent, BOSS_BAR_HEIGHT);

        float pipY = worldHeight - 110f;
        for (int index = 0; index < SkillEffect.MAX_SHOTS; index++) {
            float pipX = 40f + index * (PIP_RADIUS * 2f + 6f);
            boolean available = !attack.isReloading() && index < attack.getShotsLeft();
            shapeRenderer.setColor(available ? Color.SKY : Color.DARK_GRAY);
            shapeRenderer.circle(pipX, pipY, PIP_RADIUS);
        }
        if (attack.isReloading()) {
            float width = SkillEffect.MAX_SHOTS * (PIP_RADIUS * 2f + 6f) - 6f;
            shapeRenderer.setColor(Color.SKY);
            shapeRenderer.rect(40f - PIP_RADIUS, pipY - PIP_RADIUS - 7f,
                width * attack.getReloadProgress(), 3f);
        }
        shapeRenderer.end();

        spriteBatch.begin();
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "BOSS: " + boss.getName(), barX, barY + BOSS_BAR_HEIGHT + 20f);
        font.draw(spriteBatch, boss.getHp() + " / " + boss.getMaxHp(), barX + BOSS_BAR_WIDTH - 70f,
            barY + BOSS_BAR_HEIGHT + 20f);
        font.draw(spriteBatch, "A/D move   W jump   E attack", 24f, 22f);
        spriteBatch.end();
    }

    public void drawResult(SpriteBatch spriteBatch, boolean playerWon, String rewardSkill,
                           float worldWidth, float worldHeight) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(0f, 0f, worldWidth, worldHeight);
        shapeRenderer.end();

        spriteBatch.begin();
        String title = playerWon ? "YOU WIN!" : "GAME OVER";
        bigFont.setColor(playerWon ? Color.GOLD : Color.SCARLET);
        drawCentered(spriteBatch, bigFont, title, worldWidth, worldHeight / 2f + 60f);
        font.setColor(Color.WHITE);
        if (playerWon) {
            drawCentered(spriteBatch, font, "Boss dropped skill: " + rewardSkill, worldWidth, worldHeight / 2f);
        }
        drawCentered(spriteBatch, font, "Press R to restart", worldWidth, worldHeight / 2f - 30f);
        spriteBatch.end();
    }

    private void drawCentered(SpriteBatch spriteBatch, BitmapFont drawFont, String text,
                              float worldWidth, float y) {
        layout.setText(drawFont, text);
        drawFont.draw(spriteBatch, text, (worldWidth - layout.width) / 2f, y);
    }

    public ShapeRenderer getShapeRenderer() {
        return shapeRenderer;
    }

    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
        bigFont.dispose();
    }
}
