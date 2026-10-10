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
 * Tubes without a boss yet are locked.
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
    private static final Color[] GLOW_COLORS = {
        new Color(0.45f, 0.85f, 1f, 1f),
        new Color(1f, 0.85f, 0.3f, 1f),
        new Color(1f, 0.4f, 0.45f, 1f)
    };
    private static final float BORDER = 4f;

    private final Texture background;
    private final BitmapFont font = new BitmapFont();
    private final BitmapFont mediumFont = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();
    private final Rectangle[] tubes = new Rectangle[TUBE_AREAS.length];
    private final Rectangle startButton;
    private final float imageX;
    private final float imageScale;
    private final int playableStages;

    private int selected;
    private int hovered = -1;
    private boolean startHovered;
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
    }

    private Rectangle toWorld(float[] area) {
        return new Rectangle(imageX + area[0] * imageScale,
            (IMAGE_HEIGHT - area[1] - area[3]) * imageScale,
            area[2] * imageScale, area[3] * imageScale);
    }

    /** Returns the stage to start, or -1 while the player is still choosing. */
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.A) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            selected = Math.max(0, selected - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            selected = Math.min(tubes.length - 1, selected + 1);
        }

        boolean clicked = Gdx.input.justTouched();
        if (clicked && hovered >= 0) {
            selected = hovered;
        }
        boolean startPressed = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || (clicked && startHovered);
        return startPressed && isPlayable(selected) ? selected : -1;
    }

    private boolean isPlayable(int stage) {
        return stage < playableStages;
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
        drawSelection(shapeRenderer, tubes[selected], GLOW_COLORS[selected], pulse);
        if (isPlayable(selected)) {
            shapeRenderer.setColor(0.5f, 0.9f, 1f, (startHovered ? 0.28f : 0.1f) + 0.14f * pulse);
        } else {
            shapeRenderer.setColor(0f, 0f, 0f, 0.85f);
        }
        shapeRenderer.rect(startButton.x, startButton.y, startButton.width, startButton.height);
        shapeRenderer.end();

        spriteBatch.begin();
        mediumFont.setColor(Color.WHITE);
        for (int index = 0; index < tubes.length; index++) {
            Rectangle tube = tubes[index];
            float centerX = tube.x + tube.width / 2f;
            if (background == null) {
                drawCentered(spriteBatch, mediumFont, STAGE_NAMES[index], centerX, tube.y + tube.height / 2f);
            }
            if (!isPlayable(index)) {
                drawCentered(spriteBatch, mediumFont, "LOCKED", centerX, tube.y + 34f);
            }
        }
        if (!isPlayable(selected)) {
            drawCentered(spriteBatch, mediumFont, "COMING SOON", startButton.x + startButton.width / 2f,
                startButton.y + startButton.height / 2f + 9f);
        } else if (background == null) {
            drawCentered(spriteBatch, mediumFont, "PUSH START", startButton.x + startButton.width / 2f,
                startButton.y + startButton.height / 2f + 9f);
        }
        font.setColor(Color.LIGHT_GRAY);
        drawCentered(spriteBatch, font, "A/D or click = choose stage     ENTER or click PUSH START = fight",
            startButton.x + startButton.width / 2f, 16f);
        spriteBatch.end();
    }

    /** Glowing frame around the chosen tube and an arrow above it. */
    private void drawSelection(ShapeRenderer shapeRenderer, Rectangle tube, Color color, float pulse) {
        shapeRenderer.setColor(color.r, color.g, color.b, 0.1f + 0.08f * pulse);
        shapeRenderer.rect(tube.x, tube.y, tube.width, tube.height);
        shapeRenderer.setColor(color.r, color.g, color.b, 0.6f + 0.4f * pulse);
        shapeRenderer.rect(tube.x - BORDER, tube.y - BORDER, tube.width + BORDER * 2f, BORDER);
        shapeRenderer.rect(tube.x - BORDER, tube.y + tube.height, tube.width + BORDER * 2f, BORDER);
        shapeRenderer.rect(tube.x - BORDER, tube.y, BORDER, tube.height);
        shapeRenderer.rect(tube.x + tube.width, tube.y, BORDER, tube.height);

        float arrowX = tube.x + tube.width / 2f;
        float arrowY = tube.y + tube.height + BORDER + 4f + 4f * pulse;
        shapeRenderer.triangle(arrowX, arrowY, arrowX - 9f, arrowY + 13f, arrowX + 9f, arrowY + 13f);
    }

    private void drawCentered(SpriteBatch spriteBatch, BitmapFont drawFont, String text, float centerX, float y) {
        layout.setText(drawFont, text);
        drawFont.draw(spriteBatch, text, centerX - layout.width / 2f, y);
    }

    public void dispose() {
        if (background != null) {
            background.dispose();
        }
        font.dispose();
        mediumFont.dispose();
    }
}
