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

/**
 * Title screen with PLAY and SETTINGS buttons (ported from the JavaFX version on the Game branch).
 * SETTINGS opens a wooden panel with Music and Sound volume sliders.
 */
public class TitleScreen {
    private static final float LOGO_WIDTH = 620f;
    private static final float LOGO_CENTER_Y = 390f;
    private static final float PLAY_WIDTH = 230f;
    private static final float PLAY_CENTER_Y = 165f;
    private static final float SETTINGS_WIDTH = 180f;
    private static final float SETTINGS_CENTER_Y = 80f;
    private static final float HOVER_SCALE = 1.08f;
    private static final float PRESS_SCALE = 0.95f;

    private static final float PANEL_WIDTH = 430f;
    private static final float PANEL_HEIGHT = 330f;
    private static final float SLIDER_WIDTH = 240f;
    private static final float STEP = 0.1f;

    private static final Color FRAME_DARK = new Color(0.25f, 0.15f, 0.06f, 1f);
    private static final Color FRAME = new Color(0.6f, 0.4f, 0.18f, 1f);
    private static final Color PAPER_BORDER = new Color(0.79f, 0.66f, 0.42f, 1f);
    private static final Color PAPER = new Color(0.98f, 0.93f, 0.8f, 1f);
    private static final Color TITLE_BROWN = new Color(0.35f, 0.23f, 0.11f, 1f);
    private static final Color LABEL_BROWN = new Color(0.54f, 0.48f, 0.38f, 1f);
    private static final Color TRACK = new Color(0.55f, 0.42f, 0.24f, 1f);
    private static final Color ORANGE = new Color(0.91f, 0.54f, 0.05f, 1f);
    private static final Color ORANGE_LIGHT = new Color(1f, 0.72f, 0.25f, 1f);
    private static final Color ORANGE_BORDER = new Color(0.72f, 0.4f, 0.04f, 1f);

    private enum Button { NONE, PLAY, SETTINGS, CLOSE, MUSIC_MINUS, MUSIC_PLUS, SOUND_MINUS, SOUND_PLUS }

    private final float worldWidth;
    private final float worldHeight;
    private final GameAudio audio;
    private final Texture background;
    private final Texture logo;
    private final Texture playTexture;
    private final Texture settingsTexture;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont titleFont = new BitmapFont();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    private final Rectangle playBounds = new Rectangle();
    private final Rectangle settingsBounds = new Rectangle();
    private final Rectangle panel = new Rectangle();
    private final Rectangle closeBounds = new Rectangle();
    private final Rectangle musicMinus = new Rectangle();
    private final Rectangle musicPlus = new Rectangle();
    private final Rectangle soundMinus = new Rectangle();
    private final Rectangle soundPlus = new Rectangle();
    private final Rectangle musicSlider = new Rectangle();
    private final Rectangle soundSlider = new Rectangle();

    private boolean settingsOpen;
    private Button hovered = Button.NONE;
    private Button pressed = Button.NONE;
    private Rectangle draggingSlider;
    private float mouseX;
    private float mouseY;

    public TitleScreen(float worldWidth, float worldHeight, GameAudio audio) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.audio = audio;
        background = load("title/background.jpg");
        logo = load("title/logo.png");
        playTexture = load("title/play.png");
        settingsTexture = load("title/setting.png");
        titleFont.getData().setScale(2.2f);
        font.getData().setScale(1.3f);

        placeButton(playBounds, playTexture, PLAY_WIDTH, PLAY_CENTER_Y);
        placeButton(settingsBounds, settingsTexture, SETTINGS_WIDTH, SETTINGS_CENTER_Y);

        panel.set((worldWidth - PANEL_WIDTH) / 2f, (worldHeight - PANEL_HEIGHT) / 2f, PANEL_WIDTH, PANEL_HEIGHT);
        float centerX = worldWidth / 2f;
        float sliderX = centerX - SLIDER_WIDTH / 2f;
        musicSlider.set(sliderX, panel.y + 168f, SLIDER_WIDTH, 24f);
        soundSlider.set(sliderX, panel.y + 78f, SLIDER_WIDTH, 24f);
        musicMinus.set(sliderX - 54f, musicSlider.y - 6f, 40f, 36f);
        musicPlus.set(sliderX + SLIDER_WIDTH + 14f, musicSlider.y - 6f, 40f, 36f);
        soundMinus.set(sliderX - 54f, soundSlider.y - 6f, 40f, 36f);
        soundPlus.set(sliderX + SLIDER_WIDTH + 14f, soundSlider.y - 6f, 40f, 36f);
        closeBounds.set(panel.x + panel.width - 52f, panel.y + panel.height - 52f, 30f, 30f);
    }

    private Texture load(String path) {
        if (!Gdx.files.internal(path).exists()) {
            return null;
        }
        Texture texture = new Texture(Gdx.files.internal(path), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        return texture;
    }

    private void placeButton(Rectangle bounds, Texture texture, float width, float centerY) {
        float height = texture == null ? width * 0.37f : width * texture.getHeight() / texture.getWidth();
        bounds.set((worldWidth - width) / 2f, centerY - height / 2f, width, height);
    }

    /** Returns true when PLAY was clicked. */
    public boolean update() {
        mouseX = Gdx.input.getX() * (worldWidth / Gdx.graphics.getWidth());
        mouseY = worldHeight - Gdx.input.getY() * (worldHeight / Gdx.graphics.getHeight());
        hovered = findButton();

        if (Gdx.input.justTouched()) {
            pressed = hovered;
            if (pressed != Button.NONE) {
                audio.playClick();
            } else if (settingsOpen) {
                if (grow(musicSlider).contains(mouseX, mouseY)) {
                    draggingSlider = musicSlider;
                } else if (grow(soundSlider).contains(mouseX, mouseY)) {
                    draggingSlider = soundSlider;
                }
            }
        }

        if (draggingSlider != null) {
            float value = (mouseX - draggingSlider.x) / draggingSlider.width;
            if (draggingSlider == musicSlider) {
                audio.setMusicVolume(value);
            } else {
                audio.setSoundVolume(value);
            }
        }

        if (!Gdx.input.isTouched()) {
            Button released = pressed;
            pressed = Button.NONE;
            draggingSlider = null;
            if (released != Button.NONE && released == hovered) {
                return onClick(released);
            }
        }
        return false;
    }

    private Rectangle grow(Rectangle slider) {
        return new Rectangle(slider.x - 12f, slider.y - 10f, slider.width + 24f, slider.height + 20f);
    }

    private Button findButton() {
        if (settingsOpen) {
            if (closeBounds.contains(mouseX, mouseY)) return Button.CLOSE;
            if (musicMinus.contains(mouseX, mouseY)) return Button.MUSIC_MINUS;
            if (musicPlus.contains(mouseX, mouseY)) return Button.MUSIC_PLUS;
            if (soundMinus.contains(mouseX, mouseY)) return Button.SOUND_MINUS;
            if (soundPlus.contains(mouseX, mouseY)) return Button.SOUND_PLUS;
            return Button.NONE;
        }
        if (playBounds.contains(mouseX, mouseY)) return Button.PLAY;
        if (settingsBounds.contains(mouseX, mouseY)) return Button.SETTINGS;
        return Button.NONE;
    }

    private boolean onClick(Button button) {
        switch (button) {
            case PLAY:
                return true;
            case SETTINGS:
                settingsOpen = true;
                break;
            case CLOSE:
                settingsOpen = false;
                break;
            case MUSIC_MINUS:
                audio.setMusicVolume(audio.getMusicVolume() - STEP);
                break;
            case MUSIC_PLUS:
                audio.setMusicVolume(audio.getMusicVolume() + STEP);
                break;
            case SOUND_MINUS:
                audio.setSoundVolume(audio.getSoundVolume() - STEP);
                break;
            case SOUND_PLUS:
                audio.setSoundVolume(audio.getSoundVolume() + STEP);
                break;
            default:
                break;
        }
        return false;
    }

    public void draw(SpriteBatch batch) {
        batch.begin();
        if (background != null) {
            batch.draw(background, 0f, 0f, worldWidth, worldHeight);
        }
        if (logo != null) {
            float logoHeight = LOGO_WIDTH * logo.getHeight() / logo.getWidth();
            batch.draw(logo, (worldWidth - LOGO_WIDTH) / 2f, LOGO_CENTER_Y - logoHeight / 2f, LOGO_WIDTH, logoHeight);
        }
        drawImageButton(batch, playTexture, playBounds, Button.PLAY);
        drawImageButton(batch, settingsTexture, settingsBounds, Button.SETTINGS);
        batch.end();

        if (settingsOpen) {
            drawSettingsPanel(batch);
        }
    }

    private void drawImageButton(SpriteBatch batch, Texture texture, Rectangle bounds, Button button) {
        if (texture == null) {
            return;
        }
        float scale = 1f;
        if (!settingsOpen && pressed == button && hovered == button) {
            scale = PRESS_SCALE;
        } else if (!settingsOpen && hovered == button) {
            scale = HOVER_SCALE;
        }
        float width = bounds.width * scale;
        float height = bounds.height * scale;
        batch.draw(texture, bounds.x + (bounds.width - width) / 2f, bounds.y + (bounds.height - height) / 2f,
            width, height);
    }

    private void drawSettingsPanel(SpriteBatch batch) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.45f);
        shapes.rect(0f, 0f, worldWidth, worldHeight);
        shapes.setColor(0f, 0f, 0f, 0.4f);
        roundedRect(panel.x + 4f, panel.y - 8f, panel.width, panel.height, 22f);
        shapes.setColor(FRAME_DARK);
        roundedRect(panel.x - 4f, panel.y - 4f, panel.width + 8f, panel.height + 8f, 24f);
        shapes.setColor(FRAME);
        roundedRect(panel.x, panel.y, panel.width, panel.height, 22f);
        shapes.setColor(PAPER_BORDER);
        roundedRect(panel.x + 14f, panel.y + 14f, panel.width - 28f, panel.height - 28f, 13f);
        shapes.setColor(PAPER);
        roundedRect(panel.x + 16f, panel.y + 16f, panel.width - 32f, panel.height - 32f, 12f);
        shapes.setColor(PAPER_BORDER);
        shapes.rect(worldWidth / 2f - 130f, panel.y + panel.height - 92f, 260f, 2f);

        drawSlider(musicSlider, audio.getMusicVolume());
        drawSlider(soundSlider, audio.getSoundVolume());
        drawRoundButton(musicMinus, Button.MUSIC_MINUS);
        drawRoundButton(musicPlus, Button.MUSIC_PLUS);
        drawRoundButton(soundMinus, Button.SOUND_MINUS);
        drawRoundButton(soundPlus, Button.SOUND_PLUS);
        shapes.end();

        batch.begin();
        titleFont.setColor(TITLE_BROWN);
        drawCentered(batch, titleFont, "SETTINGS", worldWidth / 2f, panel.y + panel.height - 36f);
        font.setColor(LABEL_BROWN);
        drawCentered(batch, font, "Music  " + Math.round(audio.getMusicVolume() * 100f) + "%",
            worldWidth / 2f, musicSlider.y + 56f);
        drawCentered(batch, font, "Sound  " + Math.round(audio.getSoundVolume() * 100f) + "%",
            worldWidth / 2f, soundSlider.y + 56f);
        font.setColor(Color.WHITE);
        drawCentered(batch, font, "-", musicMinus.x + 20f, musicMinus.y + 27f);
        drawCentered(batch, font, "+", musicPlus.x + 20f, musicPlus.y + 27f);
        drawCentered(batch, font, "-", soundMinus.x + 20f, soundMinus.y + 27f);
        drawCentered(batch, font, "+", soundPlus.x + 20f, soundPlus.y + 27f);
        font.setColor(hovered == Button.CLOSE ? ORANGE : TITLE_BROWN);
        drawCentered(batch, font, "X", closeBounds.x + 15f, closeBounds.y + 24f);
        batch.end();
    }

    private void drawSlider(Rectangle slider, float value) {
        float centerY = slider.y + slider.height / 2f;
        shapes.setColor(TRACK);
        roundedRect(slider.x, centerY - 4f, slider.width, 8f, 4f);
        shapes.setColor(ORANGE);
        roundedRect(slider.x, centerY - 4f, Math.max(8f, slider.width * value), 8f, 4f);
        float knobX = slider.x + slider.width * value;
        shapes.setColor(ORANGE_BORDER);
        shapes.circle(knobX, centerY, 12f);
        shapes.setColor(ORANGE);
        shapes.circle(knobX, centerY, 10f);
        shapes.setColor(ORANGE_LIGHT);
        shapes.circle(knobX - 3f, centerY + 3f, 4f);
    }

    private void drawRoundButton(Rectangle bounds, Button button) {
        float grow = hovered == button ? 2f : 0f;
        shapes.setColor(ORANGE_BORDER);
        roundedRect(bounds.x - 2f - grow, bounds.y - 2f - grow, bounds.width + 4f + grow * 2f,
            bounds.height + 4f + grow * 2f, 10f);
        shapes.setColor(hovered == button ? ORANGE_LIGHT : ORANGE);
        roundedRect(bounds.x - grow, bounds.y - grow, bounds.width + grow * 2f, bounds.height + grow * 2f, 9f);
    }

    private void roundedRect(float x, float y, float width, float height, float radius) {
        shapes.rect(x + radius, y, width - radius * 2f, height);
        shapes.rect(x, y + radius, width, height - radius * 2f);
        shapes.circle(x + radius, y + radius, radius);
        shapes.circle(x + width - radius, y + radius, radius);
        shapes.circle(x + radius, y + height - radius, radius);
        shapes.circle(x + width - radius, y + height - radius, radius);
    }

    private void drawCentered(SpriteBatch batch, BitmapFont drawFont, String text, float centerX, float y) {
        layout.setText(drawFont, text);
        drawFont.draw(batch, text, centerX - layout.width / 2f, y);
    }

    public void dispose() {
        Texture[] textures = {background, logo, playTexture, settingsTexture};
        for (Texture texture : textures) {
            if (texture != null) {
                texture.dispose();
            }
        }
        shapes.dispose();
        titleFont.dispose();
        font.dispose();
    }
}
