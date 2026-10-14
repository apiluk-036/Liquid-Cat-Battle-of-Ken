package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
 * "Select stage" screen shown before a boss fight.
 * Click a tube (or A/D) to choose a stage, then click PUSH START (or ENTER) to fight its boss.
 * Tubes without a boss yet are locked; tubes whose boss is already beaten show CLEARED.
 */
public class StageSelectScreen {
    private static final String IMAGE_PATH = "select_stage.jpg";
    private static final float IMAGE_WIDTH = 960f;
    private static final float IMAGE_HEIGHT = 717f;

    /** Areas on the image as {x, y, width, height} in pixels, measured from its top-left corner. */
    private static final float[][] TUBE_AREAS = {
        {205f, 188f, 152f, 362f},
        {398f, 172f, 172f, 376f},
        {608f, 188f, 184f, 364f}
    };
    private static final float[] START_AREA = {335f, 615f, 288f, 80f};

    private static final String[] STAGE_NAMES = {"Dis Math", "OOP", "Web"};
    /** Green light burst around the chosen tube: an oval of light, a soft halo and turning rays. */
    private static final float GLOW_RADIUS_X = 0.6f;
    private static final float GLOW_RADIUS_Y = 0.56f;
    private static final float HALO_SIZE = 36f;
    private static final int GLOW_SEGMENTS = 48;
    private static final int RAY_COUNT = 24;
    private static final float RAY_LONG = 64f;
    private static final float RAY_SHORT = 40f;
    private static final float RAY_HALF_ANGLE = 4.5f;
    private static final float RAY_SPIN = 14f;

    /** Returned by update() when the BACK button (or ESC) is pressed. */
    public static final int BACK = -2;
    /** Returned by update() when the info icon is clicked. */
    public static final int HOW_TO_PLAY = -3;
    private static final String INFO_ICON_PATH = "icon.png";
    private static final float BUTTON_MARGIN = 34f;
    private static final float BACK_WIDTH = 112f;
    private static final float BACK_HEIGHT = 36f;
    private static final float INFO_WIDTH = 46f;
    private static final float INFO_HOVER_SCALE = 1.12f;

    private final Texture background;
    private final Texture infoIcon;
    private final BitmapFont mediumFont = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();
    private final Rectangle[] tubes = new Rectangle[TUBE_AREAS.length];
    private final Rectangle startButton;
    private final CyberButton backButton;
    private final Rectangle infoButton;
    private final float imageX;
    private final float imageScale;
    private final int playableStages;
    /** Stages already beaten in this run (shared with Main); null = none. */
    private boolean[] cleared;

    private final Color glowCore = new Color(0.8f, 1f, 0.5f, 1f);
    private final Color glowRim = new Color(0.2f, 1f, 0.3f, 1f);
    private final Color glowRay = new Color(0.45f, 1f, 0.4f, 1f);
    private final Color glowFade = new Color(0.1f, 0.9f, 0.2f, 0f);

    /** -1 until the player picks a tube. */
    private int selected = -1;
    private int hovered = -1;
    private boolean startHovered;
    private boolean infoHovered;
    private float time;

    /** playableStages = how many stages already have a boss (see Main.createBoss). */
    public StageSelectScreen(float worldWidth, float worldHeight, int playableStages) {
        this.playableStages = playableStages;
        background = Gdx.files.internal(IMAGE_PATH).exists() ? new Texture(IMAGE_PATH) : null;
        mediumFont.getData().setScale(1.4f);

        // The image is 4:3, so it is fitted to the world height and centered.
        imageScale = worldHeight / IMAGE_HEIGHT;
        imageX = (worldWidth - IMAGE_WIDTH * imageScale) / 2f;
        for (int index = 0; index < tubes.length; index++) {
            tubes[index] = toWorld(TUBE_AREAS[index]);
        }
        startButton = toWorld(START_AREA);
        float buttonCenterY = worldHeight - BUTTON_MARGIN - BACK_HEIGHT / 2f;
        backButton = new CyberButton("BACK", imageX + BUTTON_MARGIN, buttonCenterY - BACK_HEIGHT / 2f,
            BACK_WIDTH, BACK_HEIGHT);

        infoIcon = Gdx.files.internal(INFO_ICON_PATH).exists() ? new Texture(INFO_ICON_PATH) : null;
        if (infoIcon != null) {
            infoIcon.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        float infoHeight = infoIcon == null ? INFO_WIDTH : INFO_WIDTH * infoIcon.getHeight() / infoIcon.getWidth();
        infoButton = new Rectangle(imageX + IMAGE_WIDTH * imageScale - BUTTON_MARGIN - INFO_WIDTH,
            buttonCenterY - infoHeight / 2f, INFO_WIDTH, infoHeight);
    }

    /** Call each time the screen is shown: remembers which bosses are beaten and clears the selection. */
    public void open(boolean[] clearedStages) {
        cleared = clearedStages;
        selected = -1;
    }

    private Rectangle toWorld(float[] area) {
        return new Rectangle(imageX + area[0] * imageScale,
            (IMAGE_HEIGHT - area[1] - area[3]) * imageScale,
            area[2] * imageScale, area[3] * imageScale);
    }

    /**
     * Returns the stage to start, BACK to return to the title, HOW_TO_PLAY to open the manual,
     * or -1 while the player is still choosing.
     */
    public int update(float delta, float worldWidth, float worldHeight) {
        time += delta;
        float mouseX = Gdx.input.getX() * (worldWidth / Gdx.graphics.getWidth());
        float mouseY = worldHeight - Gdx.input.getY() * (worldHeight / Gdx.graphics.getHeight());
        hovered = -1;
        for (int index = 0; index < tubes.length; index++) {
            if (tubes[index].contains(mouseX, mouseY)) {
                hovered = index;
            }
        }
        startHovered = startButton.contains(mouseX, mouseY);
        infoHovered = infoButton.contains(mouseX, mouseY);
        if (backButton.update(mouseX, mouseY) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            return BACK;
        }
        if (infoHovered && Gdx.input.justTouched()) {
            return HOW_TO_PLAY;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.A) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            selected = findSelectable(selected < 0 ? tubes.length : selected, -1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            selected = findSelectable(selected, 1);
        }

        boolean clicked = Gdx.input.justTouched();
        if (clicked && hovered >= 0 && !isCleared(hovered)) {
            selected = hovered;
        }
        boolean startPressed = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || (clicked && startHovered);
        return startPressed && canStart() ? selected : -1;
    }

    /** Next tube from 'from' in 'step' direction that is not cleared; keeps the current one if none. */
    private int findSelectable(int from, int step) {
        for (int index = from + step; index >= 0 && index < tubes.length; index += step) {
            if (!isCleared(index)) {
                return index;
            }
        }
        return selected;
    }

    private boolean isCleared(int stage) {
        return cleared != null && stage < cleared.length && cleared[stage];
    }

    private boolean isPlayable(int stage) {
        return stage < playableStages && !isCleared(stage);
    }

    private boolean canStart() {
        return selected >= 0 && isPlayable(selected);
    }

    public void draw(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer) {
        ScreenUtils.clear(0.05f, 0.05f, 0.06f, 1f);
        if (background != null) {
            spriteBatch.begin();
            spriteBatch.draw(background, imageX, 0f, IMAGE_WIDTH * imageScale, IMAGE_HEIGHT * imageScale);
            spriteBatch.end();
        }

        float pulse = 0.5f + 0.5f * MathUtils.sin(time * 6f);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int index = 0; index < tubes.length; index++) {
            Rectangle tube = tubes[index];
            if (!isPlayable(index)) {
                shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
                shapeRenderer.rect(tube.x, tube.y, tube.width, tube.height);
            } else if (index == hovered && index != selected) {
                shapeRenderer.setColor(1f, 1f, 1f, 0.12f);
                shapeRenderer.rect(tube.x, tube.y, tube.width, tube.height);
            }
        }
        if (canStart()) {
            shapeRenderer.setColor(0.5f, 0.9f, 1f, (startHovered ? 0.28f : 0.1f) + 0.14f * pulse);
            shapeRenderer.rect(startButton.x, startButton.y, startButton.width, startButton.height);
        } else if (selected >= 0) {
            shapeRenderer.setColor(0f, 0f, 0f, 0.85f);
            shapeRenderer.rect(startButton.x, startButton.y, startButton.width, startButton.height);
        }
        backButton.drawShape(shapeRenderer, pulse);
        if (infoHovered || infoIcon == null) {
            Color neon = CyberButton.NEON;
            shapeRenderer.setColor(neon.r, neon.g, neon.b, infoIcon == null ? 0.8f : 0.2f + 0.15f * pulse);
            shapeRenderer.ellipse(infoButton.x - 6f, infoButton.y - 6f, infoButton.width + 12f,
                infoButton.height + 12f);
        }
        shapeRenderer.end();
        if (selected >= 0) {
            drawGlow(shapeRenderer, tubes[selected], pulse);
        }

        spriteBatch.begin();
        backButton.drawLabel(spriteBatch, mediumFont);
        float infoCenterX = infoButton.x + infoButton.width / 2f;
        float infoCenterY = infoButton.y + infoButton.height / 2f;
        if (infoIcon != null) {
            float scale = infoHovered ? INFO_HOVER_SCALE : 1f;
            spriteBatch.draw(infoIcon, infoCenterX - infoButton.width * scale / 2f,
                infoCenterY - infoButton.height * scale / 2f, infoButton.width * scale, infoButton.height * scale);
        } else {
            mediumFont.setColor(Color.BLACK);
            drawCentered(spriteBatch, mediumFont, "i", infoCenterX, infoCenterY + 9f);
        }
        mediumFont.setColor(Color.WHITE);
        for (int index = 0; index < tubes.length; index++) {
            Rectangle tube = tubes[index];
            float centerX = tube.x + tube.width / 2f;
            if (background == null) {
                drawCentered(spriteBatch, mediumFont, STAGE_NAMES[index], centerX, tube.y + tube.height / 2f);
            }
            if (isCleared(index)) {
                drawCentered(spriteBatch, mediumFont, "CLEARED", centerX, tube.y + 34f);
            } else if (!isPlayable(index)) {
                drawCentered(spriteBatch, mediumFont, "LOCKED", centerX, tube.y + 34f);
            }
        }
        if (selected >= 0 && !isPlayable(selected)) {
            drawCentered(spriteBatch, mediumFont, "COMING SOON", startButton.x + startButton.width / 2f,
                startButton.y + startButton.height / 2f + 9f);
        } else if (background == null) {
            drawCentered(spriteBatch, mediumFont, "PUSH START", startButton.x + startButton.width / 2f,
                startButton.y + startButton.height / 2f + 9f);
        }
        spriteBatch.end();
    }

    /** Green light burst around the chosen tube, drawn with additive blending so it glows over the image. */
    private void drawGlow(ShapeRenderer shapeRenderer, Rectangle tube, float pulse) {
        float centerX = tube.x + tube.width / 2f;
        float centerY = tube.y + tube.height / 2f;
        float radiusX = tube.width * GLOW_RADIUS_X;
        float radiusY = tube.height * GLOW_RADIUS_Y;
        float strength = 0.7f + 0.3f * pulse;
        glowCore.a = 0.22f * strength;
        glowRim.a = 0.5f * strength;
        glowRay.a = 0.5f * strength;

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float segmentAngle = 360f / GLOW_SEGMENTS;
        for (int segment = 0; segment < GLOW_SEGMENTS; segment++) {
            float cosFrom = MathUtils.cosDeg(segment * segmentAngle);
            float sinFrom = MathUtils.sinDeg(segment * segmentAngle);
            float cosTo = MathUtils.cosDeg((segment + 1) * segmentAngle);
            float sinTo = MathUtils.sinDeg((segment + 1) * segmentAngle);
            float fromX = centerX + radiusX * cosFrom;
            float fromY = centerY + radiusY * sinFrom;
            float toX = centerX + radiusX * cosTo;
            float toY = centerY + radiusY * sinTo;
            float haloFromX = fromX + HALO_SIZE * cosFrom;
            float haloFromY = fromY + HALO_SIZE * sinFrom;
            float haloToX = toX + HALO_SIZE * cosTo;
            float haloToY = toY + HALO_SIZE * sinTo;
            shapeRenderer.triangle(centerX, centerY, fromX, fromY, toX, toY, glowCore, glowRim, glowRim);
            shapeRenderer.triangle(fromX, fromY, haloFromX, haloFromY, haloToX, haloToY, glowRim, glowFade, glowFade);
            shapeRenderer.triangle(fromX, fromY, haloToX, haloToY, toX, toY, glowRim, glowFade, glowRim);
        }

        float rayAngle = 360f / RAY_COUNT;
        for (int ray = 0; ray < RAY_COUNT; ray++) {
            float angle = ray * rayAngle + time * RAY_SPIN;
            float length = (ray % 2 == 0 ? RAY_LONG : RAY_SHORT) * (0.85f + 0.15f * MathUtils.sin(time * 3f + ray));
            float cos = MathUtils.cosDeg(angle);
            float sin = MathUtils.sinDeg(angle);
            shapeRenderer.triangle(
                centerX + radiusX * MathUtils.cosDeg(angle - RAY_HALF_ANGLE),
                centerY + radiusY * MathUtils.sinDeg(angle - RAY_HALF_ANGLE),
                centerX + radiusX * MathUtils.cosDeg(angle + RAY_HALF_ANGLE),
                centerY + radiusY * MathUtils.sinDeg(angle + RAY_HALF_ANGLE),
                centerX + radiusX * cos + length * cos,
                centerY + radiusY * sin + length * sin,
                glowRay, glowRay, glowFade);
        }
        shapeRenderer.end();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawCentered(SpriteBatch spriteBatch, BitmapFont drawFont, String text, float centerX, float y) {
        layout.setText(drawFont, text);
        drawFont.draw(spriteBatch, text, centerX - layout.width / 2f, y);
    }

    public void dispose() {
        if (background != null) {
            background.dispose();
        }
        if (infoIcon != null) {
            infoIcon.dispose();
        }
        mediumFont.dispose();
    }
}
