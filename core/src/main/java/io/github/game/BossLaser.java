package io.github.game;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * Boss laser. Shows a warning line first, then fires horizontal beam(s).
 * - targeted(): 1 beam at the player's height every 15 s (every boss).
 * - randomPair(): 2 beams, one low and one high, at random heights every 5 s.
 * - randomSingle(): 1 beam at a random height every 5 s.
 */
public class BossLaser {
    private static final float WARNING_TIME = 1.2f;
    private static final float FIRE_TIME = 0.5f;
    private static final float BEAM_THICKNESS = 34f;
    private static final float LOW_MIN_Y = 60f;
    private static final float LOW_MAX_Y = 160f;
    private static final float HIGH_MIN_Y = 200f;
    private static final float HIGH_MAX_Y = 400f;

    private enum State { IDLE, WARNING, FIRING }

    private final float interval;
    private final float firstDelay;
    private final boolean randomHeights;
    private final int damage;
    private final float[] beamYs;

    private State state = State.IDLE;
    private float timer;
    private float stateTime;
    private float beamEndX;
    private boolean hasHitPlayer;
    private final Rectangle beam = new Rectangle();

    private BossLaser(float interval, float firstDelay, int beamCount, boolean randomHeights, int damage) {
        this.interval = interval;
        this.firstDelay = firstDelay;
        this.randomHeights = randomHeights;
        this.damage = damage;
        this.beamYs = new float[beamCount];
        this.timer = firstDelay;
    }

    public static BossLaser targeted() {
        return new BossLaser(15f, 6f, 1, false, 25);
    }

    public static BossLaser randomSingle() {
        return new BossLaser(5f, 3f, 1, true, 20);
    }

    public static BossLaser randomPair() {
        return new BossLaser(5f, 3f, 2, true, 20);
    }

    public void update(float delta, Boss boss, Player player) {
        beamEndX = boss.getLaserOriginX();
        switch (state) {
            case IDLE:
                timer -= delta;
                if (timer <= 0f) {
                    state = State.WARNING;
                    stateTime = 0f;
                    chooseBeamHeights(player);
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
                if (!hasHitPlayer && touchesPlayer(player)) {
                    hasHitPlayer = player.takeDamage(damage);
                }
                if (stateTime >= FIRE_TIME) {
                    state = State.IDLE;
                    timer = interval;
                }
                break;
            default:
                break;
        }
    }

    private void chooseBeamHeights(Player player) {
        if (!randomHeights) {
            beamYs[0] = player.getCenterY();
            return;
        }
        if (beamYs.length == 1) {
            beamYs[0] = MathUtils.random(LOW_MIN_Y, HIGH_MAX_Y);
            return;
        }
        for (int index = 0; index < beamYs.length; index++) {
            boolean low = index % 2 == 0;
            beamYs[index] = low
                ? MathUtils.random(LOW_MIN_Y, LOW_MAX_Y)
                : MathUtils.random(HIGH_MIN_Y, HIGH_MAX_Y);
        }
    }

    private boolean touchesPlayer(Player player) {
        for (float beamY : beamYs) {
            beam.set(0f, beamY - BEAM_THICKNESS / 2f, beamEndX, BEAM_THICKNESS);
            if (beam.overlaps(player.getBounds())) {
                return true;
            }
        }
        return false;
    }

    /** Call inside shapeRenderer.begin(Filled) with blending enabled. */
    public void draw(ShapeRenderer shapeRenderer) {
        for (float beamY : beamYs) {
            if (state == State.WARNING) {
                drawWarning(shapeRenderer, beamY);
            } else if (state == State.FIRING) {
                drawBeam(shapeRenderer, beamY);
            }
        }
    }

    private void drawWarning(ShapeRenderer shapeRenderer, float beamY) {
        boolean blinkOn = ((int) (stateTime * 8f)) % 2 == 0;
        shapeRenderer.setColor(1f, 0.15f, 0.15f, blinkOn ? 0.35f : 0.15f);
        shapeRenderer.rect(0f, beamY - BEAM_THICKNESS / 2f, beamEndX, BEAM_THICKNESS);
        shapeRenderer.setColor(1f, 0.2f, 0.2f, 0.9f);
        shapeRenderer.rect(0f, beamY - 1.5f, beamEndX, 3f);
        drawWarningSign(shapeRenderer, 40f, beamY, blinkOn);
    }

    private void drawBeam(ShapeRenderer shapeRenderer, float beamY) {
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

    private void drawWarningSign(ShapeRenderer shapeRenderer, float cx, float cy, boolean blinkOn) {
        float size = 26f;
        shapeRenderer.setColor(1f, blinkOn ? 0.85f : 0.6f, 0f, 1f);
        shapeRenderer.triangle(cx - size / 2f, cy - size / 2.4f, cx + size / 2f, cy - size / 2.4f, cx, cy + size / 1.7f);
        shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 1f);
        shapeRenderer.rect(cx - 1.5f, cy - 2f, 3f, 10f);
        shapeRenderer.rect(cx - 1.5f, cy - 7f, 3f, 3f);
    }

    /** Warning line is showing (the boss is charging). */
    public boolean isCharging() {
        return state == State.WARNING;
    }

    public boolean isFiring() {
        return state == State.FIRING;
    }

    public void reset() {
        state = State.IDLE;
        timer = firstDelay;
        stateTime = 0f;
    }
}
