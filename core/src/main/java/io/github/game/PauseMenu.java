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
import com.badlogic.gdx.math.Rectangle;

/**
 * Pause icon shown during a boss fight (top right, under the boss HP bar).
 * Clicking it (or P / ESC) stops the game and shows two pixel-art buttons:
 * CONTINUE on top (back to the fight) and EXIT below (leave the fight).
 */
public class PauseMenu {
    public enum Action { NONE, EXIT }

    private static final String ICON_PATH = "icon2.png";
    private static final float ICON_SIZE = 44f;
    private static final float ICON_HOVER_SCALE = 1.12f;
    private static final float BUTTON_WIDTH = 260f;
    private static final float BUTTON_HEIGHT = 56f;
    private static final float BUTTON_GAP = 22f;
    /** Size of one "pixel" of the pixel-art buttons. */
    private static final float PIXEL = 4f;

    private static final Color OUTLINE = new Color(0.09f, 0.1f, 0.27f, 1f);
    private static final Color SHINE = new Color(1f, 1f, 1f, 0.85f);
    private static final Color BLUE = new Color(0.27f, 0.77f, 1f, 1f);
    private static final Color BLUE_DARK = new Color(0.13f, 0.5f, 0.86f, 1f);
    private static final Color RED = new Color(0.93f, 0.26f, 0.3f, 1f);
    private static final Color RED_DARK = new Color(0.68f, 0.12f, 0.2f, 1f);
    private static final Color RED_TEXT = new Color(0.33f, 0.04f, 0.1f, 1f);

    private final float worldWidth;
    private final float worldHeight;
    private final GameAudio audio;
    private final Texture icon;
    private final Rectangle iconBounds;
    private final Rectangle continueButton;
    private final Rectangle exitButton;
    private final BitmapFont buttonFont = new BitmapFont();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    private boolean open;
    private boolean iconHovered;
    private boolean continueHovered;
    private boolean exitHovered;

    public PauseMenu(float worldWidth, float worldHeight, GameAudio audio) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.audio = audio;
        icon = Gdx.files.internal(ICON_PATH).exists() ? new Texture(ICON_PATH) : null;
        if (icon != null) {
            icon.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        iconBounds = new Rectangle(worldWidth - 24f - ICON_SIZE, worldHeight - 58f - ICON_SIZE, ICON_SIZE, ICON_SIZE);

        float buttonX = (worldWidth - BUTTON_WIDTH) / 2f;
        float centerY = worldHeight / 2f;
        continueButton = new Rectangle(buttonX, centerY + BUTTON_GAP / 2f, BUTTON_WIDTH, BUTTON_HEIGHT);
        exitButton = new Rectangle(buttonX, centerY - BUTTON_GAP / 2f - BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT);

        // Scaled up without smoothing so the letters look like pixel art.
        buttonFont.getData().setScale(2.4f);
        buttonFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    /** True while the menu is open. The game must not update while this is true. */
    public boolean isOpen() {
        return open;
    }

    /** Returns EXIT when the player clicked EXIT. Opening and closing the menu is handled here. */
    public Action update() {
        float mouseX = Gdx.input.getX() * (worldWidth / Gdx.graphics.getWidth());
        float mouseY = worldHeight - Gdx.input.getY() * (worldHeight / Gdx.graphics.getHeight());
        boolean clicked = Gdx.input.justTouched();
        boolean toggleKey = Gdx.input.isKeyJustPressed(Input.Keys.P) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
        iconHovered = iconBounds.contains(mouseX, mouseY);
        continueHovered = open && continueButton.contains(mouseX, mouseY);
        exitHovered = open && exitButton.contains(mouseX, mouseY);

        if (clicked && exitHovered) {
            audio.playClick();
            open = false;
            return Action.EXIT;
        }
        if (toggleKey || (clicked && (iconHovered || continueHovered))) {
            audio.playClick();
            open = !open;
        }
        return Action.NONE;
    }

    public void draw(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer) {
        if (open) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
            shapeRenderer.rect(0f, 0f, worldWidth, worldHeight);
            drawPixelButton(shapeRenderer, continueButton, BLUE, BLUE_DARK, continueHovered);
            drawPixelButton(shapeRenderer, exitButton, RED, RED_DARK, exitHovered);
            shapeRenderer.end();
        }

        spriteBatch.begin();
        if (icon != null) {
            float size = ICON_SIZE * (iconHovered ? ICON_HOVER_SCALE : 1f);
            spriteBatch.draw(icon, iconBounds.x + (ICON_SIZE - size) / 2f, iconBounds.y + (ICON_SIZE - size) / 2f,
                size, size);
        } else {
            font.setColor(Color.WHITE);
            font.draw(spriteBatch, "P = pause", iconBounds.x - 20f, iconBounds.y + ICON_SIZE / 2f);
        }
        if (open) {
            drawButtonText(spriteBatch, "CONTINUE", continueButton, OUTLINE, continueHovered);
            drawButtonText(spriteBatch, "EXIT", exitButton, RED_TEXT, exitHovered);
            font.setColor(Color.WHITE);
            String hint = "Click CONTINUE or press P to keep playing";
            layout.setText(font, hint);
            font.draw(spriteBatch, hint, (worldWidth - layout.width) / 2f, exitButton.y - 22f);
        }
        spriteBatch.end();
    }

    /** Pixel-art pill: dark outline, flat color, a shine line on top and a darker line at the bottom. */
    private void drawPixelButton(ShapeRenderer shapeRenderer, Rectangle button, Color color, Color dark,
                                 boolean hovered) {
        float x = button.x;
        float y = button.y + (hovered ? PIXEL / 2f : 0f);
        float width = button.width;
        float height = button.height;
        shapeRenderer.setColor(OUTLINE);
        pixelPill(shapeRenderer, x - PIXEL, y - PIXEL, width + PIXEL * 2f, height + PIXEL * 2f);
        shapeRenderer.setColor(dark);
        pixelPill(shapeRenderer, x, y, width, height);
        if (hovered) {
            shapeRenderer.setColor(Math.min(1f, color.r + 0.15f), Math.min(1f, color.g + 0.15f),
                Math.min(1f, color.b + 0.15f), 1f);
        } else {
            shapeRenderer.setColor(color);
        }
        pixelPill(shapeRenderer, x, y + PIXEL * 2f, width, height - PIXEL * 2f);
        shapeRenderer.setColor(SHINE);
        shapeRenderer.rect(x + PIXEL * 4f, y + height - PIXEL * 2f, width - PIXEL * 12f, PIXEL);
        shapeRenderer.rect(x + width - PIXEL * 6f, y + height - PIXEL * 2f, PIXEL * 2f, PIXEL);
    }

    /** Rounded shape built from stacked rectangles, so the corners look like pixel steps. */
    private void pixelPill(ShapeRenderer shapeRenderer, float x, float y, float width, float height) {
        for (int step = 0; step <= 3; step++) {
            float insetX = (3 - step) * PIXEL;
            float insetY = step * PIXEL;
            shapeRenderer.rect(x + insetX, y + insetY, width - insetX * 2f, height - insetY * 2f);
        }
    }

    private void drawButtonText(SpriteBatch spriteBatch, String text, Rectangle button, Color color, boolean hovered) {
        buttonFont.setColor(color);
        layout.setText(buttonFont, text);
        float x = button.x + (button.width - layout.width) / 2f;
        float y = button.y + (button.height + layout.height) / 2f + PIXEL / 2f + (hovered ? PIXEL / 2f : 0f);
        // Drawn twice, one pixel apart, to make the letters bold.
        buttonFont.draw(spriteBatch, text, x, y);
        buttonFont.draw(spriteBatch, text, x + 2f, y);
    }

    public void dispose() {
        if (icon != null) {
            icon.dispose();
        }
        buttonFont.dispose();
        font.dispose();
    }
}
