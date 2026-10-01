package io.github.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 960f;
    private static final float WORLD_HEIGHT = 540f;
    private static final float GRAVITY = 1200f;
    private static final float MOVE_SPEED = 210f;
    private static final float CAT_JUMP = 500f;

    private SpriteBatch spriteBatch;
    private Texture backgroundTexture;
    private CatAnimation catAnimation;
    private GameWorld gameWorld;
    private Player player;
    private float catIdleTime;

    @Override
    public void create() {
        spriteBatch = new SpriteBatch();
        backgroundTexture = loadTextureIfExists(
            "background.png",
            "background.jpg",
            "blackground.jpg",
            "blackground.png"
        );
        catAnimation = new CatAnimation();
        gameWorld = new GameWorld(WORLD_WIDTH);
        player = new Player(60f, 50f, 34f, 42f);
    }

    private Texture loadTextureIfExists(String... paths) {
        for (String path : paths) {
            if (Gdx.files.internal(path).exists()) {
                return new Texture(path);
            }
        }
        return null;
    }

    @Override
    public void render() {
        update(Gdx.graphics.getDeltaTime());
        drawWorld();
    }

    private void update(float delta) {
        handleInput();

        if (player.onGround && Math.abs(player.velocityX) < 0.01f) {
            catIdleTime += delta;
        } else {
            catIdleTime = 0f;
        }

        gameWorld.update(player, delta, GRAVITY);
    }

    private void handleInput() {
        float horizontal = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            horizontal -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            horizontal += 1f;
        }

        if (horizontal < 0f) {
            player.facing = -1;
        } else if (horizontal > 0f) {
            player.facing = 1;
        }

        player.velocityX = horizontal * MOVE_SPEED;

        if ((Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP))
            && player.onGround) {
            player.velocityY = CAT_JUMP;
            player.onGround = false;
        }
    }

    private void drawWorld() {
        ScreenUtils.clear(0.08f, 0.12f, 0.14f, 1f);

        spriteBatch.begin();
        if (backgroundTexture != null) {
            spriteBatch.draw(backgroundTexture, 0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        }

        boolean isIdle = player.onGround && Math.abs(player.velocityX) < 0.01f;
        TextureRegion catSprite = isIdle
            ? catAnimation.getIdleFrame(catIdleTime)
            : getMovingCatSprite();
        if (catSprite != null) {
            drawCatSprite(catSprite, player.facing > 0);
        }
        spriteBatch.end();
    }

    private TextureRegion getMovingCatSprite() {
        Texture texture = catAnimation.getMovingTexture(player.facing);
        return texture == null ? null : new TextureRegion(texture);
    }

    private void drawCatSprite(TextureRegion catSprite, boolean flipHorizontal) {
        float drawScale = 1.5f;
        float baseWidth = player.width + 20f;
        float baseHeight = player.height + 20f;
        float drawWidth = baseWidth * drawScale;
        float drawHeight = baseHeight * drawScale;
        float centerX = player.x + baseWidth / 2f;
        float centerY = player.y + baseHeight / 2f;
        float scaleX = flipHorizontal ? -1f : 1f;
        spriteBatch.draw(catSprite, centerX, centerY, drawWidth / 2f, drawHeight / 2f,
            drawWidth, drawHeight, scaleX, 1f, 0f);
    }

    @Override
    public void dispose() {
        if (backgroundTexture != null) backgroundTexture.dispose();
        catAnimation.dispose();
        spriteBatch.dispose();
    }
}
