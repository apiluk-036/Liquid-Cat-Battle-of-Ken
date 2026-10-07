package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class SkillEffect {
    private static final float FRAME_DURATION = 0.06f;
    private static final float EFFECT_WIDTH = 210f;
    private static final float EFFECT_HEIGHT = 120f;
    private static final float MOVE_SPEED = 420f;
    /** Normal attack: 10 damage, 5 shots in a row (0.5 s apart), then 2 s cooldown. */
    public static final int DAMAGE = 10;
    public static final int MAX_SHOTS = 5;
    private static final float COOLDOWN = 0.5f;
    private static final float RELOAD_TIME = 2f;

    private final Array<Texture> textures = new Array<>();
    private final Array<ActiveEffect> activeEffects = new Array<>();
    private final Animation<TextureRegion> animation;
    private final Rectangle hitBox = new Rectangle();
    private float cooldownRemaining;
    private int shotsLeft = MAX_SHOTS;
    private float reloadRemaining;

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
        if (animation != null && cooldownRemaining <= 0f && reloadRemaining <= 0f) {
            activeEffects.add(new ActiveEffect(player));
            cooldownRemaining = COOLDOWN;
            shotsLeft--;
            if (shotsLeft <= 0) {
                reloadRemaining = RELOAD_TIME;
            }
        }
    }

    /** Removes every attack that touches the target and returns how many hit. */
    public int collectHits(Rectangle target) {
        int hits = 0;
        for (int index = activeEffects.size - 1; index >= 0; index--) {
            ActiveEffect effect = activeEffects.get(index);
            hitBox.set(effect.x + EFFECT_WIDTH * 0.15f, effect.y + EFFECT_HEIGHT * 0.2f,
                EFFECT_WIDTH * 0.7f, EFFECT_HEIGHT * 0.6f);
            if (hitBox.overlaps(target)) {
                activeEffects.removeIndex(index);
                hits++;
            }
        }
        return hits;
    }

    public int getShotsLeft() {
        return shotsLeft;
    }

    public boolean isReloading() {
        return reloadRemaining > 0f;
    }

    /** 0 = just started reloading, 1 = ready. */
    public float getReloadProgress() {
        return 1f - reloadRemaining / RELOAD_TIME;
    }

    public void reset() {
        activeEffects.clear();
        cooldownRemaining = 0f;
        reloadRemaining = 0f;
        shotsLeft = MAX_SHOTS;
    }

    public void update(float delta, float worldWidth) {
        cooldownRemaining = Math.max(0f, cooldownRemaining - delta);
        if (reloadRemaining > 0f) {
            reloadRemaining -= delta;
            if (reloadRemaining <= 0f) {
                reloadRemaining = 0f;
                shotsLeft = MAX_SHOTS;
            }
        }

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
