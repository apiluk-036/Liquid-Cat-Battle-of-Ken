package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import java.util.EnumMap;
import java.util.Map;

/** Boss HP bar, attack shots (5 status), skill slots, "Get skill" card and win / lose screens. */
public class GameHud {
    private static final float BOSS_BAR_WIDTH = 300f;
    private static final float BOSS_BAR_HEIGHT = 18f;
    private static final float PIP_RADIUS = 7f;
    private static final float SLOT_SIZE = 38f;

    private static final Color PANEL_BLUE = new Color(0.78f, 0.9f, 0.98f, 1f);
    private static final Color PANEL_BORDER = new Color(0.12f, 0.14f, 0.2f, 1f);
    private static final Color NEXT_ORANGE = new Color(1f, 0.62f, 0.22f, 1f);
    private static final Color TEXT_DARK = new Color(0.12f, 0.12f, 0.16f, 1f);
    private static final Color CHAMPION_GOLD = new Color(1f, 0.93f, 0.6f, 1f);

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final BitmapFont mediumFont = new BitmapFont();
    private final BitmapFont bigFont = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();
    private final Map<SkillType, Texture> icons = new EnumMap<>(SkillType.class);
    private final Rectangle nextButton = new Rectangle();

    public GameHud() {
        mediumFont.getData().setScale(1.4f);
        bigFont.getData().setScale(2.5f);
        for (SkillType skill : SkillType.values()) {
            if (Gdx.files.internal(skill.iconPath).exists()) {
                icons.put(skill, new Texture(skill.iconPath));
            }
        }
    }

    public void draw(SpriteBatch spriteBatch, Boss boss, SkillEffect attack, PlayerSkills skills,
                     int stageNumber, float worldWidth, float worldHeight) {
        float barX = worldWidth - BOSS_BAR_WIDTH - 24f;
        float barY = worldHeight - 44f;
        float bossPercent = boss.getHp() / (float) boss.getMaxHp();
        float pipY = worldHeight - 110f;
        float slotY = pipY - 30f - SLOT_SIZE;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.12f, 1f);
        shapeRenderer.rect(barX - 3f, barY - 3f, BOSS_BAR_WIDTH + 6f, BOSS_BAR_HEIGHT + 6f);
        shapeRenderer.setColor(0.35f, 0.05f, 0.08f, 1f);
        shapeRenderer.rect(barX, barY, BOSS_BAR_WIDTH, BOSS_BAR_HEIGHT);
        if (boss.isShielded()) {
            shapeRenderer.setColor(0.4f, 0.75f, 1f, 1f);
        } else {
            shapeRenderer.setColor(0.93f, 0.25f, 0.45f, 1f);
        }
        shapeRenderer.rect(barX, barY, BOSS_BAR_WIDTH * bossPercent, BOSS_BAR_HEIGHT);

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

        float slotX = 33f;
        for (SkillType skill : skills.getOwned()) {
            shapeRenderer.setColor(0.08f, 0.1f, 0.14f, 0.85f);
            shapeRenderer.rect(slotX - 2f, slotY - 2f, SLOT_SIZE + 4f, SLOT_SIZE + 4f);
            slotX += SLOT_SIZE + 14f;
        }
        shapeRenderer.end();

        spriteBatch.begin();
        slotX = 33f;
        for (SkillType skill : skills.getOwned()) {
            Texture icon = icons.get(skill);
            float cooldown = skills.getCooldown(skill);
            if (icon != null) {
                spriteBatch.setColor(1f, 1f, 1f, cooldown > 0f ? 0.35f : 1f);
                spriteBatch.draw(icon, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
                spriteBatch.setColor(1f, 1f, 1f, 1f);
            }
            font.setColor(Color.WHITE);
            font.draw(spriteBatch, skill.key, slotX + 1f, slotY + SLOT_SIZE + 1f);
            if (cooldown > 0f) {
                drawCentered(spriteBatch, font, String.valueOf((int) Math.ceil(cooldown)),
                    slotX + SLOT_SIZE / 2f, slotY + SLOT_SIZE / 2f + 6f);
            }
            slotX += SLOT_SIZE + 14f;
        }

        font.setColor(Color.WHITE);
        String bossLabel = "BOSS: " + boss.getName() + (boss.isShielded() ? "  [SHIELD]" : "");
        font.draw(spriteBatch, bossLabel, barX, barY + BOSS_BAR_HEIGHT + 20f);
        font.draw(spriteBatch, boss.getHp() + " / " + boss.getMaxHp(), barX + BOSS_BAR_WIDTH - 70f,
            barY + BOSS_BAR_HEIGHT + 20f);
        mediumFont.setColor(Color.WHITE);
        drawCentered(spriteBatch, mediumFont, "STAGE " + stageNumber, worldWidth / 2f, worldHeight - 14f);
        font.draw(spriteBatch, getControlsHint(skills), 24f, 22f);
        spriteBatch.end();
    }

    private String getControlsHint(PlayerSkills skills) {
        StringBuilder hint = new StringBuilder("A/D move   W jump   E attack");
        for (SkillType skill : skills.getOwned()) {
            hint.append("   ").append(skill.key).append(' ').append(skill.displayName.toLowerCase());
        }
        return hint.toString();
    }

    /** "Get skill" card shown after collecting the boss drop. */
    public void drawSkillCard(SpriteBatch spriteBatch, SkillType skill, boolean hasNextStage,
                              float worldWidth, float worldHeight) {
        float panelWidth = 580f;
        float panelHeight = 300f;
        float panelX = (worldWidth - panelWidth) / 2f;
        float panelY = (worldHeight - panelHeight) / 2f;
        float iconSize = 130f;
        float iconX = panelX + 40f;
        float iconY = panelY + 70f;
        float boxX = panelX + 210f;
        float boxY = panelY + 70f;
        float boxWidth = 330f;
        float boxHeight = 150f;
        nextButton.set(panelX + panelWidth - 150f, panelY + 18f, 120f, 40f);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.55f);
        shapeRenderer.rect(0f, 0f, worldWidth, worldHeight);
        drawBorderedRect(panelX, panelY, panelWidth, panelHeight, PANEL_BLUE, 4f);
        shapeRenderer.setColor(1f, 0.95f, 0.6f, 0.6f);
        shapeRenderer.circle(iconX + iconSize / 2f, iconY + iconSize / 2f, iconSize * 0.62f);
        drawBorderedRect(boxX, boxY, boxWidth, boxHeight, Color.WHITE, 3f);
        drawBorderedRect(nextButton.x, nextButton.y, nextButton.width, nextButton.height, NEXT_ORANGE, 3f);
        shapeRenderer.end();

        spriteBatch.begin();
        Texture icon = icons.get(skill);
        if (icon != null) {
            spriteBatch.draw(icon, iconX, iconY, iconSize, iconSize);
        }
        bigFont.setColor(TEXT_DARK);
        drawCentered(spriteBatch, bigFont, "Get skill!", worldWidth / 2f, panelY + panelHeight - 18f);

        mediumFont.setColor(TEXT_DARK);
        mediumFont.draw(spriteBatch, "skill: " + skill.displayName + "  (" + skill.subject + ")",
            boxX + 16f, boxY + boxHeight - 14f);
        font.setColor(TEXT_DARK);
        float lineY = boxY + boxHeight - 52f;
        for (String line : skill.details) {
            font.draw(spriteBatch, "- " + line, boxX + 20f, lineY);
            lineY -= 24f;
        }
        font.draw(spriteBatch, "Press " + skill.key + " to use", boxX + 20f, lineY - 4f);

        mediumFont.setColor(TEXT_DARK);
        drawCentered(spriteBatch, mediumFont, "next", nextButton.x + nextButton.width / 2f,
            nextButton.y + nextButton.height - 10f);
        font.setColor(Color.DARK_GRAY);
        String hint = hasNextStage ? "ENTER / click next = go to next stage" : "ENTER / click next";
        font.draw(spriteBatch, hint, panelX + 24f, panelY + 44f);
        spriteBatch.end();
    }

    /** True when the player clicks the "next" button of the skill card. */
    public boolean isNextClicked(float worldWidth, float worldHeight) {
        if (!Gdx.input.justTouched()) {
            return false;
        }
        float clickX = Gdx.input.getX() * (worldWidth / Gdx.graphics.getWidth());
        float clickY = worldHeight - Gdx.input.getY() * (worldHeight / Gdx.graphics.getHeight());
        return nextButton.contains(clickX, clickY);
    }

    public void drawGameOver(SpriteBatch spriteBatch, float worldWidth, float worldHeight) {
        drawDim(worldWidth, worldHeight);
        spriteBatch.begin();
        bigFont.setColor(Color.SCARLET);
        drawCentered(spriteBatch, bigFont, "GAME OVER", worldWidth / 2f, worldHeight / 2f + 60f);
        font.setColor(Color.WHITE);
        drawCentered(spriteBatch, font, "All skills are lost", worldWidth / 2f, worldHeight / 2f);
        drawCentered(spriteBatch, font, "R = start over   ESC = menu", worldWidth / 2f, worldHeight / 2f - 30f);
        spriteBatch.end();
    }

    public void drawChampion(SpriteBatch spriteBatch, PlayerSkills skills, float worldWidth, float worldHeight) {
        float panelWidth = 520f;
        float panelHeight = 280f;
        float panelX = (worldWidth - panelWidth) / 2f;
        float panelY = (worldHeight - panelHeight) / 2f;
        float cupX = worldWidth / 2f;
        float cupY = panelY + 150f;

        drawDim(worldWidth, worldHeight);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawBorderedRect(panelX, panelY, panelWidth, panelHeight, CHAMPION_GOLD, 4f);
        shapeRenderer.setColor(Color.GOLD);
        shapeRenderer.circle(cupX - 34f, cupY + 30f, 14f);
        shapeRenderer.circle(cupX + 34f, cupY + 30f, 14f);
        shapeRenderer.setColor(CHAMPION_GOLD);
        shapeRenderer.circle(cupX - 34f, cupY + 30f, 7f);
        shapeRenderer.circle(cupX + 34f, cupY + 30f, 7f);
        shapeRenderer.setColor(Color.GOLD);
        shapeRenderer.triangle(cupX - 36f, cupY + 50f, cupX + 36f, cupY + 50f, cupX, cupY - 4f);
        shapeRenderer.rect(cupX - 36f, cupY + 36f, 72f, 16f);
        shapeRenderer.rect(cupX - 6f, cupY - 20f, 12f, 20f);
        shapeRenderer.rect(cupX - 24f, cupY - 28f, 48f, 10f);
        shapeRenderer.end();

        spriteBatch.begin();
        bigFont.setColor(TEXT_DARK);
        drawCentered(spriteBatch, bigFont, "You are champion", worldWidth / 2f, panelY + 100f);
        font.setColor(TEXT_DARK);
        drawCentered(spriteBatch, font, "Skills: " + skills.getOwned().size() + "   R = play again   ESC = menu",
            worldWidth / 2f, panelY + 34f);
        spriteBatch.end();
    }

    private void drawDim(float worldWidth, float worldHeight) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(0f, 0f, worldWidth, worldHeight);
        shapeRenderer.end();
    }

    private void drawBorderedRect(float x, float y, float width, float height, Color fill, float border) {
        shapeRenderer.setColor(PANEL_BORDER);
        shapeRenderer.rect(x - border, y - border, width + border * 2f, height + border * 2f);
        shapeRenderer.setColor(fill);
        shapeRenderer.rect(x, y, width, height);
    }

    private void drawCentered(SpriteBatch spriteBatch, BitmapFont drawFont, String text, float centerX, float y) {
        layout.setText(drawFont, text);
        drawFont.draw(spriteBatch, text, centerX - layout.width / 2f, y);
    }

    public ShapeRenderer getShapeRenderer() {
        return shapeRenderer;
    }

    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
        mediumFont.dispose();
        bigFont.dispose();
        for (Texture icon : icons.values()) {
            icon.dispose();
        }
    }
}
