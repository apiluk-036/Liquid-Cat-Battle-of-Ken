package io.github.game;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Boss laser: every 15 seconds the boss locks onto the player's height,
 * shows a warning line, then fires a horizontal beam. Jump to dodge it.
 */
public class BossLaser {
    private static final float INTERVAL = 15f;
    private static final float FIRST_DELAY = 6f;
    private static final float WARNING_TIME = 1.2f;
    private static final float FIRE_TIME = 0.5f;
    private static final float BEAM_THICKNESS = 34f;
    private static final int DAMAGE = 25;

    private enum State { IDLE, WARNING, FIRING }

    private State state = State.IDLE;
    private float timer = FIRST_DELAY;
    private float stateTime;
    private float beamY;
    private float beamEndX;
    private boolean hasHitPlayer;
    private final Rectangle beam = new Rectangle();

    public void update(float delta, Boss boss, Player player) {
        beamEndX = boss.getLaserOriginX();
        switch (state) {
            case IDLE:
                timer -= delta;
                if (timer <= 0f) {
                    state = State.WARNING;
                    stateTime = 0f;
                    beamY = player.y + player.height / 2f;
                }
                break;
            case WARNING:
                stateTime += delta;
                if (stateTime >= WARNING_TIME) {
                    state = State.FIRING;
                    stateTime = 0f;
                    hasHitPlayer = false;
                }
                break;
            case FIRING:
                stateTime += delta;
                if (!hasHitPlayer && getBeam().overlaps(player.getBounds())) {
                    hasHitPlayer = player.takeDamage(DAMAGE);
                }
                if (stateTime >= FIRE_TIME) {
                    state = State.IDLE;
                    timer = INTERVAL;
                }
                break;
            default:
                break;
        }
    }

    private Rectangle getBeam() {
        return beam.set(0f, beamY - BEAM_THICKNESS / 2f, beamEndX, BEAM_THICKNESS);
    }

    /** Call inside shapeRenderer.begin(Filled) with blending enabled. */
    public void draw(ShapeRenderer shapeRenderer) {
        if (state == State.WARNING) {
            boolean blinkOn = ((int) (stateTime * 8f)) % 2 == 0;
            shapeRenderer.setColor(1f, 0.15f, 0.15f, blinkOn ? 0.35f : 0.15f);
            shapeRenderer.rect(0f, beamY - BEAM_THICKNESS / 2f, beamEndX, BEAM_THICKNESS);
            shapeRenderer.setColor(1f, 0.2f, 0.2f, 0.9f);
            shapeRenderer.rect(0f, beamY - 1.5f, beamEndX, 3f);
            drawWarningSign(shapeRenderer, 40f, beamY, blinkOn);
        } else if (state == State.FIRING) {
            float pulse = 1f + 0.15f * (float) Math.sin(stateTime * 60f);
            float outer = BEAM_THICKNESS * pulse;
            shapeRenderer.setColor(1f, 0.1f, 0.2f, 0.55f);
            shapeRenderer.rect(0f, beamY - outer / 2f, beamEndX, outer);
            shapeRenderer.setColor(1f, 0.45f, 0.45f, 0.9f);
            shapeRenderer.rect(0f, beamY - BEAM_THICKNESS * 0.3f, beamEndX, BEAM_THICKNESS * 0.6f);
            shapeRenderer.setColor(1f, 1f, 1f, 0.95f);
            shapeRenderer.rect(0f, beamY - 3f, beamEndX, 6f);
            shapeRenderer.circle(beamEndX, beamY, outer * 0.7f);
        }
    }

    private void drawWarningSign(ShapeRenderer shapeRenderer, float cx, float cy, boolean blinkOn) {
        float size = 26f;
        shapeRenderer.setColor(1f, blinkOn ? 0.85f : 0.6f, 0f, 1f);
        shapeRenderer.triangle(cx - size / 2f, cy - size / 2.4f, cx + size / 2f, cy - size / 2.4f, cx, cy + size / 1.7f);
        shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 1f);
        shapeRenderer.rect(cx - 1.5f, cy - 2f, 3f, 10f);
        shapeRenderer.rect(cx - 1.5f, cy - 7f, 3f, 3f);
    }

    public void reset() {
        state = State.IDLE;
        timer = FIRST_DELAY;
        stateTime = 0f;
    }
}
