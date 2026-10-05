package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class HpPlayer {
    private static final float FRAME_X = 20f;
    private static final float FRAME_Y_OFFSET = 92f;
    private static final float FRAME_WIDTH = 235f;
    private static final float FRAME_HEIGHT = 78f;
    private static final float SOURCE_WIDTH = 157f;
    private static final float SOURCE_HEIGHT = 56f;

    private final Texture frameTexture;
    private final ShapeRenderer shapeRenderer;

    public HpPlayer() {
        frameTexture = loadTexture("hp_player/hp_full.png");
        shapeRenderer = new ShapeRenderer();
    }

    public void draw(SpriteBatch spriteBatch, Player player, float worldHeight) {
        if (frameTexture == null) {
            return;
        }

        float frameY = worldHeight - FRAME_Y_OFFSET;
        float fillX = FRAME_X + 48f / SOURCE_WIDTH * FRAME_WIDTH;
        float fillY = frameY + 18f / SOURCE_HEIGHT * FRAME_HEIGHT;
        float fillWidth = 96f / SOURCE_WIDTH * FRAME_WIDTH;
        float fillHeight = 20f / SOURCE_HEIGHT * FRAME_HEIGHT;
        float healthPercent = player.getHp() / (float) player.getMaxHp();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(getHealthColor(healthPercent));
        shapeRenderer.rect(fillX, fillY, fillWidth * healthPercent, fillHeight);
        shapeRenderer.end();

        spriteBatch.begin();
        spriteBatch.draw(frameTexture, FRAME_X, frameY, FRAME_WIDTH, FRAME_HEIGHT);
        spriteBatch.end();
    }

    private Color getHealthColor(float healthPercent) {
        if (healthPercent > 0.5f) {
            return Color.GREEN;
        } else if (healthPercent > 0.2f) {
            return Color.YELLOW;
        }
        return Color.RED;
    }

    private Texture loadTexture(String path) {
        return Gdx.files.internal(path).exists() ? new Texture(path) : null;
    }

    public void dispose() {
        if (frameTexture != null) {
            frameTexture.dispose();
        }
        shapeRenderer.dispose();
    }
}
