package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * "Get ready" panel shown over the arena before a boss fight starts.
 * The fight is frozen until the player presses ENTER or clicks, and the panel lists the controls,
 * so nobody has to open the manual first.
 */
public class StageIntro {
    /** {key, what it does} */
    private static final String[][] CONTROLS = {
        {"A / D  or  LEFT / RIGHT", "Move"},
        {"W  or  UP", "Jump"},
        {"E", "Attack (" + SkillEffect.MAX_SHOTS + " shots, then reload)"}
    };
    private static final String[] PAUSE_CONTROL = {"P  or  ESC", "Pause"};

    private static final String[] TIPS = {
        "Bring the boss HP down to 0 to win.",
        "Boss attacks show a warning first. Move away from it."
    };

    private static final String START_HINT = "PRESS ENTER OR CLICK TO START";

    private static final float PANEL_WIDTH = 560f;
    private static final float PADDING = 24f;
    private static final float TITLE = 46f;
    private static final float BOSS_LINE = 40f;
    private static final float HEADING = 34f;
    private static final float LINE = 21f;
    private static final float SECTION_GAP = 12f;
    private static final float START_LINE = 34f;
    private static final float KEY_COLUMN = 220f;

    private static final Color KEY_COLOR = new Color(0.75f, 1f, 0.5f, 1f);
    private static final Color TEXT_COLOR = new Color(0.86f, 0.92f, 0.96f, 1f);

    private final float worldWidth;
    private final float worldHeight;
    private final BitmapFont titleFont = new BitmapFont();
    private final BitmapFont headingFont = new BitmapFont();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    private float time;

    public StageIntro(float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        titleFont.getData().setScale(2.4f);
        headingFont.getData().setScale(1.4f);
    }

    /** Returns true when the player starts the fight (ENTER or a click). */
    public boolean update(float delta) {
        time += delta;
        return Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)
            || Gdx.input.justTouched();
    }

    public void draw(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, Boss boss, PlayerSkills skills,
                     int stageNumber) {
        int rows = CONTROLS.length + skills.getOwned().size() + 1;
        float panelHeight = PADDING * 2f + TITLE + BOSS_LINE + HEADING + rows * LINE + SECTION_GAP
            + TIPS.length * LINE + SECTION_GAP + START_LINE;
        float panelX = (worldWidth - PANEL_WIDTH) / 2f;
        float panelY = (worldHeight - panelHeight) / 2f;
        float pulse = 0.5f + 0.5f * MathUtils.sin(time * 6f);
        Color neon = CyberButton.NEON;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(0f, 0f, worldWidth, worldHeight);
        shapeRenderer.setColor(neon.r, neon.g, neon.b, 0.6f + 0.3f * pulse);
        CyberButton.cutCornerRect(shapeRenderer, panelX - 2f, panelY - 2f, PANEL_WIDTH + 4f, panelHeight + 4f, 16f);
        shapeRenderer.setColor(CyberButton.PANEL.r, CyberButton.PANEL.g, CyberButton.PANEL.b, 1f);
        CyberButton.cutCornerRect(shapeRenderer, panelX, panelY, PANEL_WIDTH, panelHeight, 15f);
        shapeRenderer.setColor(neon.r, neon.g, neon.b, 0.5f);
        shapeRenderer.rect(worldWidth / 2f - 110f, panelY + panelHeight - PADDING - TITLE + 4f, 220f, 2f);
        shapeRenderer.end();

        float x = panelX + PADDING;
        float y = panelY + panelHeight - PADDING;
        spriteBatch.begin();
        titleFont.setColor(neon);
        drawCentered(spriteBatch, titleFont, "STAGE " + stageNumber, y);
        y -= TITLE + 8f;
        headingFont.setColor(Color.WHITE);
        drawCentered(spriteBatch, headingFont, "BOSS: " + boss.getName(), y);
        y -= BOSS_LINE - 8f;

        headingFont.setColor(neon);
        headingFont.draw(spriteBatch, "> HOW TO PLAY", x, y);
        y -= HEADING;
        for (String[] control : CONTROLS) {
            y = drawControl(spriteBatch, control[0], control[1], x, y);
        }
        for (SkillType skill : skills.getOwned()) {
            y = drawControl(spriteBatch, skill.key, skill.displayName + ": " + skill.details[0], x, y);
        }
        y = drawControl(spriteBatch, PAUSE_CONTROL[0], PAUSE_CONTROL[1], x, y);

        y -= SECTION_GAP;
        font.setColor(TEXT_COLOR);
        for (String tip : TIPS) {
            font.draw(spriteBatch, "- " + tip, x, y);
            y -= LINE;
        }

        headingFont.setColor(1f, 1f, 1f, 0.35f + 0.65f * pulse);
        drawCentered(spriteBatch, headingFont, START_HINT, panelY + PADDING + 20f);
        spriteBatch.end();
    }

    /** Draws one "key - what it does" row and returns the y of the next row. */
    private float drawControl(SpriteBatch spriteBatch, String key, String action, float x, float y) {
        font.setColor(KEY_COLOR);
        font.draw(spriteBatch, key, x, y);
        font.setColor(TEXT_COLOR);
        font.draw(spriteBatch, action, x + KEY_COLUMN, y);
        return y - LINE;
    }

    private void drawCentered(SpriteBatch spriteBatch, BitmapFont drawFont, String text, float y) {
        layout.setText(drawFont, text);
        drawFont.draw(spriteBatch, text, (worldWidth - layout.width) / 2f, y);
    }

    public void dispose() {
        titleFont.dispose();
        headingFont.dispose();
        font.dispose();
    }
}
