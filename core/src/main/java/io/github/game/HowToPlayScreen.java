package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * "How to play" manual, opened with the info icon on the stage select screen.
 * Scroll with the mouse wheel, W/S, UP/DOWN or by dragging the scroll bar.
 * To change the manual, edit the text arrays below.
 */
public class HowToPlayScreen {
    /** {key, what it does} */
    private static final String[][] CONTROLS = {
        {"A / D  or  LEFT / RIGHT", "Move"},
        {"W  or  UP", "Jump"},
        {"E", "Attack"},
        {"Q  /  F  /  C", "Use a skill you collected"},
        {"ENTER", "Confirm / next"},
        {"R", "Play again (after you become champion)"},
        {"Y  /  N", "Continue or quit (game over screen)"},
        {"ESC", "Back to the menu"}
    };

    private static final String[] COMBAT = {
        "Liquid Cat has 100 HP. When it reaches 0 the game is over.",
        "Attack: " + SkillEffect.DAMAGE + " damage. You can shoot " + SkillEffect.MAX_SHOTS
            + " times in a row, then you wait 2 s to reload.",
        "After you get hit you are invincible for 1 s.",
        "Every boss attack shows a warning first (a line or a circle). Move away from it.",
        "Jump on the floating platforms to dodge."
    };

    private static final String[] HOW_TO_WIN = {
        "1. Click a capsule on the stage select, then click PUSH START.",
        "2. Bring the boss HP down to 0. The boss drops a skill item.",
        "3. Walk to the item to collect the skill.",
        "4. Press next to pick another boss. Beaten bosses show CLEARED. Your skills are kept.",
        "5. Beat all 3 bosses, in any order, to become champion.",
        "If you lose, all collected skills are lost."
    };

    /** One boss card. Keep the order the same as Main.createBoss. */
    private static final class BossInfo {
        final String stage;
        final String name;
        final int hp;
        final String imagePath;
        final SkillType drop;
        final String[] skills;

        BossInfo(String stage, String name, int hp, String imagePath, SkillType drop, String... skills) {
            this.stage = stage;
            this.name = name;
            this.hp = hp;
            this.imagePath = imagePath;
            this.drop = drop;
            this.skills = skills;
        }
    }

    private static final BossInfo[] BOSSES = {
        new BossInfo("STAGE 1   Dis Math (EASY)", "Flame Devil", 150, "boss/devil.png", SkillType.SHIELD,
            "Magma: falls on a random spot every 2 s (warning circle on the floor). Damage 15",
            "Laser: 1 laser at a random height every 5 s. Damage 20",
            "Shield: the boss is invincible for 3 s every 10 s (its HP bar turns blue)"),
        new BossInfo("STAGE 2   OOP (NORMAL)", "final class Teacher", 300, "boss/teacher.png", SkillType.GIANT_CANDY,
            "Candy: thrown to a random spot every 2 s (pink warning circle). Damage 15",
            "Double laser: 2 lasers, one low and one high, every 5 s. Damage 20",
            "Giant candy: thrown to a random spot every 10 s. Damage 30"),
        new BossInfo("STAGE 3   Web (HARD)", "Bug Hacker", 500, "boss/hacker.png", SkillType.CODE_BREATH,
            "Energy pulse: explodes on a random spot every 2 s (spinning blue dots). Damage 15",
            "Lane laser: 3 lasers on 4 lanes every 5 s. Stand in the green lane. Damage 20",
            "Code breath: a wave of code every 15 s. It cuts your HP in half")
    };

    private static final float MARGIN = 34f;
    private static final float VIEW_TOP = 452f;
    private static final float VIEW_BOTTOM = 40f;
    private static final float PADDING = 24f;
    private static final float LINE = 21f;
    private static final float HEADING = 34f;
    private static final float SECTION_GAP = 16f;
    private static final float KEY_COLUMN = 220f;
    private static final float CARD_HEIGHT = 158f;
    private static final float CARD_GAP = 14f;
    private static final float PORTRAIT = 110f;
    private static final float DROP_ICON = 26f;
    private static final float GRID = 40f;
    private static final float WHEEL_STEP = 60f;
    private static final float KEY_SCROLL_SPEED = 420f;
    private static final float BAR_WIDTH = 8f;

    private static final Color BACKGROUND = new Color(0.02f, 0.04f, 0.08f, 1f);
    private static final Color KEY_COLOR = new Color(0.75f, 1f, 0.5f, 1f);
    private static final Color TEXT_COLOR = new Color(0.86f, 0.92f, 0.96f, 1f);
    private static final Color DROP_COLOR = new Color(1f, 0.85f, 0.35f, 1f);

    private final float worldWidth;
    private final float worldHeight;
    private final Rectangle view;
    private final Rectangle scrollTrack;
    private final CyberButton backButton;
    private final Texture[] portraits = new Texture[BOSSES.length];
    private final Texture[] dropIcons = new Texture[BOSSES.length];
    private final BitmapFont titleFont = new BitmapFont();
    private final BitmapFont headingFont = new BitmapFont();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    private float time;
    private float scroll;
    private float maxScroll;
    private float wheelMoved;
    private boolean draggingBar;

    public HowToPlayScreen(float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        view = new Rectangle(MARGIN, VIEW_BOTTOM, worldWidth - MARGIN * 2f, VIEW_TOP - VIEW_BOTTOM);
        scrollTrack = new Rectangle(view.x + view.width - BAR_WIDTH - 8f, view.y + 12f, BAR_WIDTH, view.height - 24f);
        backButton = new CyberButton("BACK", MARGIN, worldHeight - MARGIN - 36f, 112f, 36f);
        titleFont.getData().setScale(2.4f);
        headingFont.getData().setScale(1.4f);
        for (int index = 0; index < BOSSES.length; index++) {
            portraits[index] = load(BOSSES[index].imagePath);
            dropIcons[index] = load(BOSSES[index].drop.iconPath);
        }
        // The mouse wheel is only reported through an input processor.
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                wheelMoved += amountY;
                return true;
            }
        });
    }

    private Texture load(String path) {
        return Gdx.files.internal(path).exists() ? new Texture(path) : null;
    }

    /** Call when the manual is opened, so it starts from the top. */
    public void open() {
        scroll = 0f;
        wheelMoved = 0f;
        draggingBar = false;
    }

    /** Returns true when the player leaves the manual (BACK button or ESC). */
    public boolean update(float delta) {
        time += delta;
        float mouseX = Gdx.input.getX() * (worldWidth / Gdx.graphics.getWidth());
        float mouseY = worldHeight - Gdx.input.getY() * (worldHeight / Gdx.graphics.getHeight());

        scroll += wheelMoved * WHEEL_STEP;
        wheelMoved = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            scroll += KEY_SCROLL_SPEED * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            scroll -= KEY_SCROLL_SPEED * delta;
        }

        if (Gdx.input.justTouched() && mouseX >= scrollTrack.x - 10f && view.contains(mouseX, mouseY)) {
            draggingBar = true;
        }
        if (!Gdx.input.isTouched()) {
            draggingBar = false;
        }
        if (draggingBar && maxScroll > 0f) {
            float barHeight = getBarHeight();
            float top = scrollTrack.y + scrollTrack.height - barHeight / 2f;
            scroll = (top - mouseY) / (scrollTrack.height - barHeight) * maxScroll;
        }
        scroll = MathUtils.clamp(scroll, 0f, maxScroll);

        return backButton.update(mouseX, mouseY) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
    }

    private float getBarHeight() {
        return Math.max(30f, scrollTrack.height * view.height / (view.height + maxScroll));
    }

    public void draw(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer) {
        ScreenUtils.clear(BACKGROUND);
        float pulse = 0.5f + 0.5f * MathUtils.sin(time * 6f);
        Color neon = CyberButton.NEON;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(neon.r, neon.g, neon.b, 0.05f);
        for (float x = 0f; x <= worldWidth; x += GRID) {
            shapeRenderer.rect(x, 0f, 1f, worldHeight);
        }
        for (float y = 0f; y <= worldHeight; y += GRID) {
            shapeRenderer.rect(0f, y, worldWidth, 1f);
        }
        shapeRenderer.setColor(neon.r, neon.g, neon.b, 0.6f);
        CyberButton.cutCornerRect(shapeRenderer, view.x - 2f, view.y - 2f, view.width + 4f, view.height + 4f, 16f);
        shapeRenderer.setColor(CyberButton.PANEL.r, CyberButton.PANEL.g, CyberButton.PANEL.b, 1f);
        CyberButton.cutCornerRect(shapeRenderer, view.x, view.y, view.width, view.height, 15f);
        shapeRenderer.setColor(neon.r, neon.g, neon.b, 0.5f + 0.3f * pulse);
        shapeRenderer.rect(worldWidth / 2f - 110f, worldHeight - 62f, 220f, 2f);
        backButton.drawShape(shapeRenderer, pulse);
        shapeRenderer.end();

        // Everything in the manual is cut off at the panel edges while it scrolls.
        float pixelsX = Gdx.graphics.getBackBufferWidth() / worldWidth;
        float pixelsY = Gdx.graphics.getBackBufferHeight() / worldHeight;
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor((int) (view.x * pixelsX), (int) ((view.y + 4f) * pixelsY),
            (int) (view.width * pixelsX), (int) ((view.height - 8f) * pixelsY));
        float top = view.y + view.height - PADDING + scroll;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawContent(top, null, shapeRenderer);
        shapeRenderer.end();
        spriteBatch.begin();
        float bottom = drawContent(top, spriteBatch, null);
        spriteBatch.end();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        maxScroll = Math.max(0f, (top - bottom) + PADDING - view.height);

        if (maxScroll > 0f) {
            float barHeight = getBarHeight();
            float barY = scrollTrack.y + (scrollTrack.height - barHeight) * (1f - scroll / maxScroll);
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(neon.r, neon.g, neon.b, 0.15f);
            shapeRenderer.rect(scrollTrack.x, scrollTrack.y, scrollTrack.width, scrollTrack.height);
            shapeRenderer.setColor(neon.r, neon.g, neon.b, draggingBar ? 1f : 0.75f);
            shapeRenderer.rect(scrollTrack.x, barY, scrollTrack.width, barHeight);
            shapeRenderer.end();
        }

        spriteBatch.begin();
        titleFont.setColor(neon);
        layout.setText(titleFont, "HOW TO PLAY");
        titleFont.draw(spriteBatch, "HOW TO PLAY", (worldWidth - layout.width) / 2f, worldHeight - 20f);
        backButton.drawLabel(spriteBatch, headingFont);
        font.setColor(Color.LIGHT_GRAY);
        String hint = "Mouse wheel, W/S or UP/DOWN = scroll     ESC or BACK = stage select";
        layout.setText(font, hint);
        font.draw(spriteBatch, hint, (worldWidth - layout.width) / 2f, 24f);
        spriteBatch.end();
    }

    /**
     * Lays out the whole manual from y = top downwards and returns the y under the last line.
     * It runs twice per frame: once with the shape renderer (card backgrounds) and once with the
     * sprite batch (text and images), so both passes share the same positions.
     */
    private float drawContent(float top, SpriteBatch spriteBatch, ShapeRenderer shapeRenderer) {
        float x = view.x + PADDING;
        float y = drawHeading(spriteBatch, "CONTROLS", x, top);
        for (String[] control : CONTROLS) {
            drawText(spriteBatch, control[0], KEY_COLOR, x, y);
            drawText(spriteBatch, control[1], TEXT_COLOR, x + KEY_COLUMN, y);
            y -= LINE;
        }
        y = drawHeading(spriteBatch, "COMBAT", x, y - SECTION_GAP);
        y = drawLines(spriteBatch, COMBAT, x, y);
        y = drawHeading(spriteBatch, "HOW TO WIN", x, y - SECTION_GAP);
        y = drawLines(spriteBatch, HOW_TO_WIN, x, y);
        y = drawHeading(spriteBatch, "BOSSES: SKILLS AND WHAT THEY DROP", x, y - SECTION_GAP);

        float cardWidth = view.width - PADDING * 2f - BAR_WIDTH - 8f;
        for (int index = 0; index < BOSSES.length; index++) {
            drawBossCard(spriteBatch, shapeRenderer, index, x, y - CARD_HEIGHT, cardWidth);
            y -= CARD_HEIGHT + CARD_GAP;
        }
        return y + CARD_GAP;
    }

    private void drawBossCard(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, int index,
                              float x, float y, float width) {
        BossInfo boss = BOSSES[index];
        if (shapeRenderer != null) {
            Color neon = CyberButton.NEON;
            shapeRenderer.setColor(neon.r, neon.g, neon.b, 0.35f);
            CyberButton.cutCornerRect(shapeRenderer, x, y, width, CARD_HEIGHT, 12f);
            shapeRenderer.setColor(0.04f, 0.11f, 0.19f, 1f);
            CyberButton.cutCornerRect(shapeRenderer, x + 1.5f, y + 1.5f, width - 3f, CARD_HEIGHT - 3f, 11f);
            return;
        }

        Texture portrait = portraits[index];
        if (portrait != null) {
            float scale = Math.min(PORTRAIT / portrait.getWidth(), PORTRAIT / portrait.getHeight());
            float drawWidth = portrait.getWidth() * scale;
            float drawHeight = portrait.getHeight() * scale;
            spriteBatch.draw(portrait, x + 16f + (PORTRAIT - drawWidth) / 2f,
                y + (CARD_HEIGHT - drawHeight) / 2f, drawWidth, drawHeight);
        }

        float textX = x + 16f + PORTRAIT + 18f;
        float lineY = y + CARD_HEIGHT - 14f;
        headingFont.setColor(CyberButton.NEON);
        headingFont.draw(spriteBatch, boss.stage, textX, lineY);
        lineY -= 30f;
        drawText(spriteBatch, "Boss: " + boss.name + "     HP " + boss.hp, KEY_COLOR, textX, lineY);
        lineY -= LINE;
        for (int skill = 0; skill < boss.skills.length; skill++) {
            drawText(spriteBatch, "Skill " + (skill + 1) + "  " + boss.skills[skill], TEXT_COLOR, textX, lineY);
            lineY -= LINE;
        }
        Texture icon = dropIcons[index];
        float dropX = textX;
        if (icon != null) {
            spriteBatch.draw(icon, textX, lineY - DROP_ICON + 6f, DROP_ICON, DROP_ICON);
            dropX += DROP_ICON + 8f;
        }
        SkillType drop = boss.drop;
        drawText(spriteBatch, "WIN TO GET  " + drop.displayName + " [" + drop.key + "]: " + drop.details[0]
            + ". " + drop.details[1] + ". " + drop.details[2], DROP_COLOR, dropX, lineY - 2f);
    }

    /** Draws a section heading and returns the y of the first text line under it. */
    private float drawHeading(SpriteBatch spriteBatch, String text, float x, float y) {
        if (spriteBatch != null) {
            headingFont.setColor(CyberButton.NEON);
            headingFont.draw(spriteBatch, "> " + text, x, y);
        }
        return y - HEADING;
    }

    /** Draws text lines and returns the y under the last one. */
    private float drawLines(SpriteBatch spriteBatch, String[] lines, float x, float y) {
        for (String line : lines) {
            drawText(spriteBatch, line, TEXT_COLOR, x, y);
            y -= LINE;
        }
        return y;
    }

    private void drawText(SpriteBatch spriteBatch, String text, Color color, float x, float y) {
        if (spriteBatch != null) {
            font.setColor(color);
            font.draw(spriteBatch, text, x, y);
        }
    }

    public void dispose() {
        for (int index = 0; index < BOSSES.length; index++) {
            if (portraits[index] != null) {
                portraits[index].dispose();
            }
            if (dropIcons[index] != null) {
                dropIcons[index].dispose();
            }
        }
        titleFont.dispose();
        headingFont.dispose();
        font.dispose();
    }
}
