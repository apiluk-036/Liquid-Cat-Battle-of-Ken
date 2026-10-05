package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class SkillEffect {
    private static final float FRAME_DURATION = 0.06f;
    private static final float EFFECT_WIDTH = 210f;
    private static final float EFFECT_HEIGHT = 120f;
    private static final float MOVE_SPEED = 420f;
    private static final float COOLDOWN = 0.5f;

    private final Array<Texture> textures = new Array<>();
    private final Array<ActiveEffect> activeEffects = new Array<>();
    private final Animation<TextureRegion> animation;
    private float cooldownRemaining;

    public SkillEffect() {
        Array<TextureRegion> frames = new Array<>();
        for (int frameNumber = 1; frameNumber <= 10; frameNumber++) {
            String path = String.format("effect/frame_%03d.png", frameNumber);
            if (!Gdx.files.internal(path).exists()) {
                break;
            }
            Texture texture = new Texture(path);
            textures.add(texture);
            frames.add(new TextureRegion(texture));
        }
        animation = frames.size == 0 ? null
            : new Animation<>(FRAME_DURATION, frames, Animation.PlayMode.LOOP);
    }

    public void start(Player player) {
        if (animation != null && cooldownRemaining <= 0f) {
            activeEffects.add(new ActiveEffect(player));
            cooldownRemaining = COOLDOWN;
        }
    }

    public void update(float delta, float worldWidth) {
        cooldownRemaining = Math.max(0f, cooldownRemaining - delta);

        for (int index = activeEffects.size - 1; index >= 0; index--) {
            ActiveEffect effect = activeEffects.get(index);
            effect.stateTime += delta;
            effect.x += effect.direction * MOVE_SPEED * delta;

            boolean outsideMap = effect.direction > 0
                ? effect.x > worldWidth
                : effect.x + EFFECT_WIDTH < 0f;
            if (outsideMap) {
                activeEffects.removeIndex(index);
            }
        }
    }

    public void draw(SpriteBatch spriteBatch) {
        if (animation == null) {
            return;
        }

        for (ActiveEffect effect : activeEffects) {
            TextureRegion frame = animation.getKeyFrame(effect.stateTime);
            float scaleX = effect.direction > 0 ? 1f : -1f;
            spriteBatch.draw(frame, effect.x, effect.y, EFFECT_WIDTH / 2f, EFFECT_HEIGHT / 2f,
                EFFECT_WIDTH, EFFECT_HEIGHT, scaleX, 1f, 0f);
        }
    }

    public void dispose() {
        for (Texture texture : textures) {
            texture.dispose();
        }
    }

    private static class ActiveEffect {
        private float stateTime;
        private float x;
        private final float y;
        private final int direction;

        private ActiveEffect(Player player) {
            direction = player.facing;
            x = direction > 0
                ? player.x + player.width
                : player.x - EFFECT_WIDTH;
            y = player.y + player.height + 18f;
        }
    }
}
