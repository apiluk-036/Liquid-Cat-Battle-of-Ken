package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class CatAnimation {
    private final Texture leftTexture;
    private final Texture rightTexture;
    private final Array<Texture> idleTextures;
    private final Animation<TextureRegion> idleAnimation;

    public CatAnimation() {
        leftTexture = loadTextureIfExists(
            "cat_frames/cat_001.png",
            "cat-left.png",
            "cat-left.jpg",
            "cat_l.png",
            "cat_left.png"
        );
        rightTexture = loadTextureIfExists(
            "cat_frames/cat_001.png",
            "cat-right.png",
            "cat-right.jpg",
            "cat_r.png",
            "cat_right.png"
        );

        Array<TextureRegion> frames = new Array<>();
        idleTextures = new Array<>();
        for (int frameIndex = 1; ; frameIndex++) {
            String path = String.format("cat_frames/cat_%03d.png", frameIndex);
            if (!Gdx.files.internal(path).exists()) {
                break;
            }
            Texture texture = new Texture(path);
            idleTextures.add(texture);
            frames.add(new TextureRegion(texture));
        }
        idleAnimation = frames.size > 0
            ? new Animation<>(1f / 12f, frames, Animation.PlayMode.LOOP)
            : null;
    }

    public Texture getMovingTexture(int facing) {
        return facing >= 0 ? rightTexture : leftTexture;
    }

    public TextureRegion getIdleFrame(float stateTime) {
        return idleAnimation == null ? null : idleAnimation.getKeyFrame(stateTime);
    }

    public void dispose() {
        if (leftTexture != null) leftTexture.dispose();
        if (rightTexture != null && rightTexture != leftTexture) rightTexture.dispose();
        for (Texture texture : idleTextures) {
            if (texture != leftTexture && texture != rightTexture) texture.dispose();
        }
    }

    private Texture loadTextureIfExists(String... paths) {
        for (String path : paths) {
            if (Gdx.files.internal(path).exists()) {
                return new Texture(path);
            }
        }
        return null;
    }
}
