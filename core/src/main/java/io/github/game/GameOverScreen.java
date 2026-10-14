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

/**
 * Retro arcade "GAME OVER" overlay drawn on top of the dimmed arena: a big yellow / red title
 * and "CONTINUE ?" with a YES / NO choice.
 * Pick with A/D or LEFT/RIGHT and confirm with ENTER, press Y / N, or click a word.
 */
public class GameOverScreen {
    public enum Action { NONE, YES, NO }

    private static final String TITLE = "GAME OVER";
    private static final String QUESTION = "CONTINUE ?";
    private static final String NOTE = "All skills are lost";
    private static final String YES = "YES";
    private static final String NO = "NO";

    private static final float TITLE_SCALE = 6f;
    private static final float MENU_SCALE = 2f;
    /** Thickness of the black outline around the title. */
    private static final float OUTLINE = 6f;
    private static final float OPTION_GAP = 70f;
    private static final float ARROW_SIZE = 14f;

    private static final Color TITLE_YELLOW = new Color(1f, 0.93f, 0.1f, 1f);
    private static final Color TITLE_ORANGE = new Color(1f, 0.55f, 0.08f, 1f);
    private static final Color TITLE_RED = new Color(0.93f, 0.12f, 0.12f, 1f);
    private static final Color NOTE_COLOR = new Color(0.7f, 0.7f, 0.74f, 1f);

    private final float worldWidth;
    private final float worldHeight;
    private final BitmapFont titleFont = new BitmapFont();
    private final BitmapFont menuFont = new BitmapFont();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();
    private final Rectangle yesBounds = new Rectangle();
    private final Rectangle noBounds = new Rectangle();

    private float time;
    private boolean yesSelected = true;

    public GameOverScreen(float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;

        // Scaled up without smoothing so the letters look like pixel art.
        titleFont.getData().setScale(TITLE_SCALE);
        titleFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        menuFont.getData().setScale(MENU_SCALE);
        menuFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        float optionY = worldHeight * 0.33f;
        layout.setText(menuFont, YES);
        float yesWidth = layout.width;
        float textHeight = layout.height;
        layout.setText(menuFont, NO);
        float noWidth = layout.width;
        float startX = (worldWidth - (yesWidth + OPTION_GAP + noWidth)) / 2f;
        yesBounds.set(startX, optionY - textHeight, yesWidth, textHeight);
        noBounds.set(startX + yesWidth + OPTION_GAP, optionY - textHeight, noWidth, textHeight);
    }

    /** Call when the player loses, so the arrow starts on YES. */
    public void open() {
        yesSelected = true;
    }

    /** Returns YES or NO once the player has chosen. */
    public Action update(float delta) {
        time += delta;
        float mouseX = Gdx.input.getX() * (worldWidth / Gdx.graphics.getWidth());
        float mouseY = worldHeight - Gdx.input.getY() * (worldHeight / Gdx.graphics.getHeight());
        boolean overYes = isOver(yesBounds, mouseX, mouseY);
        boolean overNo = isOver(noBounds, mouseX, mouseY);
        boolean clicked = Gdx.input.justTouched();

        // The mouse only moves the arrow while it is moving, so the keys still work when it rests on a word.
        boolean mouseMoved = Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0;
        if ((mouseMoved || clicked) && (overYes || overNo)) {
            yesSelected = overYes;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.A) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            yesSelected = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            yesSelected = false;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
            return Action.YES;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            return Action.NO;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || (clicked && (overYes || overNo))) {
            return yesSelected ? Action.YES : Action.NO;
        }
        return Action.NONE;
    }

    /** The words are small, so the clickable area is a little bigger than the text. */
    private boolean isOver(Rectangle bounds, float mouseX, float mouseY) {
        return mouseX >= bounds.x - 30f && mouseX <= bounds.x + bounds.width + 14f
            && mouseY >= bounds.y - 12f && mouseY <= bounds.y + bounds.height + 12f;
    }

    public void draw(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(0f, 0f, worldWidth, worldHeight);
        drawArrow(shapeRenderer);
        shapeRenderer.end();

        spriteBatch.begin();
        drawTitle(spriteBatch);
        menuFont.setColor(Color.WHITE);
        layout.setText(menuFont, QUESTION);
        drawBold(spriteBatch, menuFont, QUESTION, (worldWidth - layout.width) / 2f, worldHeight * 0.42f);
        drawOption(spriteBatch, YES, yesBounds, yesSelected);
        drawOption(spriteBatch, NO, noBounds, !yesSelected);
        font.setColor(NOTE_COLOR);
        layout.setText(font, NOTE);
        font.draw(spriteBatch, NOTE, (worldWidth - layout.width) / 2f, worldHeight * 0.5f);
        spriteBatch.end();
    }

    private void drawArrow(ShapeRenderer shapeRenderer) {
        Rectangle bounds = yesSelected ? yesBounds : noBounds;
        float nudge = 2f * MathUtils.sin(time * 8f);
        float tipX = bounds.x - 10f + nudge;
        float centerY = bounds.y + bounds.height / 2f;
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.triangle(tipX, centerY, tipX - ARROW_SIZE, centerY + ARROW_SIZE * 0.6f,
            tipX - ARROW_SIZE, centerY - ARROW_SIZE * 0.6f);
    }

    /** Big title: black outline, yellow letters with an orange stripe and a red bottom part. */
    private void drawTitle(SpriteBatch spriteBatch) {
        layout.setText(titleFont, TITLE);
        float x = (worldWidth - layout.width) / 2f;
        float y = worldHeight * 0.76f;
        float height = layout.height;

        titleFont.setColor(Color.BLACK);
        for (int offsetX = -1; offsetX <= 2; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                titleFont.draw(spriteBatch, TITLE, x + offsetX * OUTLINE, y + offsetY * OUTLINE);
            }
        }
        titleFont.setColor(TITLE_YELLOW);
        drawBold(spriteBatch, titleFont, TITLE, x, y);

        float bottom = y - height - TITLE_SCALE * 4f;
        drawTitleBand(spriteBatch, TITLE_ORANGE, x, y, bottom, y - height * 0.52f);
        drawTitleBand(spriteBatch, TITLE_RED, x, y, bottom, y - height * 0.62f);
    }

    /** Draws the title again in another color, but only the part between bandBottom and bandTop. */
    private void drawTitleBand(SpriteBatch spriteBatch, Color color, float x, float y, float bandBottom,
                               float bandTop) {
        float pixelsY = Gdx.graphics.getBackBufferHeight() / worldHeight;
        spriteBatch.flush();
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, (int) (bandBottom * pixelsY), Gdx.graphics.getBackBufferWidth(),
            (int) ((bandTop - bandBottom) * pixelsY));
        titleFont.setColor(color);
        drawBold(spriteBatch, titleFont, TITLE, x, y);
        spriteBatch.flush();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    private void drawOption(SpriteBatch spriteBatch, String text, Rectangle bounds, boolean selected) {
        menuFont.setColor(selected ? Color.WHITE : Color.LIGHT_GRAY);
        drawBold(spriteBatch, menuFont, text, bounds.x, bounds.y + bounds.height);
    }

    /** Drawn twice, one font pixel apart, to make the letters bold. */
    private void drawBold(SpriteBatch spriteBatch, BitmapFont drawFont, String text, float x, float y) {
        drawFont.draw(spriteBatch, text, x, y);
        drawFont.draw(spriteBatch, text, x + drawFont.getData().scaleX, y);
    }

    public void dispose() {
        titleFont.dispose();
        menuFont.dispose();
        font.dispose();
    }
}
