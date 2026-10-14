package io.github.game;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Boss 3: Bug Hacker (Web). Three skills:
 * 1) Energy pulse - an energy burst at a random spot on the map every 2 s.
 * 2) Lane laser   - 3 lasers on random lanes every 5 s, one lane is left safe.
 * 3) Code breath  - breathes a stream of code at a random height every 15 s;
 *                   getting hit cuts the player's HP in half.
 * Drops the "Code Breath" skill when defeated.
 */
public final class HackerBoss extends Boss {
    private static final int MAX_HP = 500;

    private static final float PULSE_INTERVAL = 2f;
    private static final float PULSE_FIRST_DELAY = 1.5f;
    private static final float PULSE_WARNING_TIME = 0.9f;
    private static final float PULSE_BURST_TIME = 0.35f;
    private static final float PULSE_RADIUS = 55f;
    private static final int PULSE_DAMAGE = 15;

    private static final float BREATH_INTERVAL = 15f;
    private static final float BREATH_FIRST_DELAY = 9f;
    private static final float BREATH_WARNING_TIME = 1.1f;
    private static final float BREATH_TIME = 1.3f;
    private static final float BREATH_HEIGHT = 96f;
    private static final float BREATH_MIN_Y = 100f;
    private static final float BREATH_MAX_Y = 370f;
    private static final float CODE_SPEED = 560f;
    private static final float CODE_SPAWN_TIME = 0.04f;
    private static final String[] CODE_WORDS = {
        "if(!ok)", "fail();", "0101", "</>", "{ }", "null", "404", "while(true)", "throw", "1101"
    };

    private enum BreathState { IDLE, WARNING, BREATHING }

    private final Array<Pulse> pulses = new Array<>();
    private final Array<CodeChar> codeStream = new Array<>();
    private final Rectangle breathBox = new Rectangle();
    private final BitmapFont codeFont = new BitmapFont();
    private float pulseTimer = PULSE_FIRST_DELAY;
    private float breathTimer = BREATH_FIRST_DELAY;
    private BreathState breathState = BreathState.IDLE;
    private float breathStateTime;
    private float breathY;
    private float codeSpawnTimer;
    private boolean breathHitPlayer;
    private float effectTime;

    public HackerBoss(float worldWidth, float groundY) {
        super("Bug Hacker", SkillType.CODE_BREATH, MAX_HP, "boss/hacker.png", BossLaser.lanes(),
            worldWidth - 225f, groundY, 190f, 200f, 226f);
        codeFont.getData().setScale(1.3f);
    }

    @Override
    protected void updateSpecialSkill(float delta, Player player) {
        effectTime += delta;
        updatePulses(delta, player);
        updateBreath(delta, player);
    }

    private void updatePulses(float delta, Player player) {
        pulseTimer -= delta;
        if (pulseTimer <= 0f) {
            pulses.add(new Pulse(MathUtils.random(40f, x - 70f), MathUtils.random(80f, 380f)));
            playCastAnimation(0.3f, false);
            pulseTimer = PULSE_INTERVAL;
        }
        for (int index = pulses.size - 1; index >= 0; index--) {
            Pulse pulse = pulses.get(index);
            boolean wasWarning = pulse.time < PULSE_WARNING_TIME;
            pulse.time += delta;
            if (wasWarning && pulse.time >= PULSE_WARNING_TIME
                && circleTouches(pulse.x, pulse.y, PULSE_RADIUS, player.getBounds())) {
                player.takeDamage(PULSE_DAMAGE);
            }
            if (pulse.time >= PULSE_WARNING_TIME + PULSE_BURST_TIME) {
                pulses.removeIndex(index);
            }
        }
    }

    private void updateBreath(float delta, Player player) {
        switch (breathState) {
            case IDLE:
                breathTimer -= delta;
                if (breathTimer <= 0f) {
                    breathState = BreathState.WARNING;
                    breathStateTime = 0f;
                    breathY = MathUtils.random(BREATH_MIN_Y, BREATH_MAX_Y);
                }
                break;
            case WARNING:
                breathStateTime += delta;
                if (breathStateTime >= BREATH_WARNING_TIME) {
                    breathState = BreathState.BREATHING;
                    breathStateTime = 0f;
                    breathHitPlayer = false;
                    playCastAnimation(1f, true);
                }
                break;
            case BREATHING:
                breathStateTime += delta;
                spawnCode(delta);
                breathBox.set(0f, breathY - BREATH_HEIGHT / 2f, getMouthX(), BREATH_HEIGHT);
                if (!breathHitPlayer && breathBox.overlaps(player.getBounds())) {
                    int halfHp = Math.max(1, (player.getHp() + 1) / 2);
                    breathHitPlayer = player.takeDamage(halfHp);
                }
                if (breathStateTime >= BREATH_TIME) {
                    breathState = BreathState.IDLE;
                    breathTimer = BREATH_INTERVAL;
                }
                break;
            default:
                break;
        }

        for (int index = codeStream.size - 1; index >= 0; index--) {
            CodeChar code = codeStream.get(index);
            code.x -= CODE_SPEED * delta;
            if (code.x < -120f) {
                codeStream.removeIndex(index);
            }
        }
    }

    private void spawnCode(float delta) {
        codeSpawnTimer -= delta;
        while (codeSpawnTimer <= 0f) {
            codeSpawnTimer += CODE_SPAWN_TIME;
            float codeY = breathY + MathUtils.random(-BREATH_HEIGHT / 2f + 14f, BREATH_HEIGHT / 2f);
            String word = CODE_WORDS[MathUtils.random(CODE_WORDS.length - 1)];
            codeStream.add(new CodeChar(word, getMouthX() - 20f, codeY, MathUtils.random(3) == 0));
        }
    }

    private float getMouthX() {
        return x + width * 0.45f;
    }

    private static boolean circleTouches(float cx, float cy, float radius, Rectangle box) {
        float closestX = MathUtils.clamp(cx, box.x, box.x + box.width);
        float closestY = MathUtils.clamp(cy, box.y, box.y + box.height);
        float dx = cx - closestX;
        float dy = cy - closestY;
        return dx * dx + dy * dy <= radius * radius;
    }

    @Override
    protected void drawSpecialShapes(ShapeRenderer shapeRenderer) {
        for (Pulse pulse : pulses) {
            if (pulse.time < PULSE_WARNING_TIME) {
                drawPulseWarning(shapeRenderer, pulse);
            } else {
                drawPulseBurst(shapeRenderer, pulse);
            }
        }
        if (breathState == BreathState.WARNING) {
            boolean blinkOn = ((int) (breathStateTime * 8f)) % 2 == 0;
            shapeRenderer.setColor(0.2f, 1f, 0.45f, blinkOn ? 0.2f : 0.08f);
            shapeRenderer.rect(0f, breathY - BREATH_HEIGHT / 2f, getMouthX(), BREATH_HEIGHT);
            shapeRenderer.setColor(1f, 0.3f, 0.3f, 0.9f);
            shapeRenderer.rect(0f, breathY + BREATH_HEIGHT / 2f - 2f, getMouthX(), 3f);
            shapeRenderer.rect(0f, breathY - BREATH_HEIGHT / 2f, getMouthX(), 3f);
        } else if (breathState == BreathState.BREATHING) {
            float pulse = 1f + 0.06f * MathUtils.sin(effectTime * 40f);
            shapeRenderer.setColor(0.05f, 0.35f, 0.15f, 0.45f);
            shapeRenderer.rect(0f, breathY - BREATH_HEIGHT * pulse / 2f, getMouthX(), BREATH_HEIGHT * pulse);
            shapeRenderer.setColor(0.3f, 1f, 0.5f, 0.25f);
            shapeRenderer.rect(0f, breathY - BREATH_HEIGHT * 0.2f, getMouthX(), BREATH_HEIGHT * 0.4f);
        }
    }

    private void drawPulseWarning(ShapeRenderer shapeRenderer, Pulse pulse) {
        float progress = pulse.time / PULSE_WARNING_TIME;
        shapeRenderer.setColor(0.3f, 0.8f, 1f, 0.12f + 0.2f * progress);
        shapeRenderer.circle(pulse.x, pulse.y, PULSE_RADIUS * progress);
        int dots = 14;
        float spin = effectTime * 2.5f;
        shapeRenderer.setColor(0.4f, 0.9f, 1f, 0.9f);
        for (int dot = 0; dot < dots; dot++) {
            float angle = spin + dot * MathUtils.PI2 / dots;
            shapeRenderer.circle(pulse.x + MathUtils.cos(angle) * PULSE_RADIUS,
                pulse.y + MathUtils.sin(angle) * PULSE_RADIUS, 3f);
        }
        shapeRenderer.circle(pulse.x, pulse.y, 4f);
    }

    private void drawPulseBurst(ShapeRenderer shapeRenderer, Pulse pulse) {
        float progress = (pulse.time - PULSE_WARNING_TIME) / PULSE_BURST_TIME;
        float alpha = 1f - progress;
        shapeRenderer.setColor(0.3f, 0.75f, 1f, 0.5f * alpha);
        shapeRenderer.circle(pulse.x, pulse.y, PULSE_RADIUS * (1f + 0.4f * progress));
        shapeRenderer.setColor(0.85f, 0.97f, 1f, 0.8f * alpha);
        shapeRenderer.circle(pulse.x, pulse.y, PULSE_RADIUS * 0.55f * (1f + progress));
    }

    @Override
    protected void drawSpecialSkill(SpriteBatch spriteBatch) {
        for (CodeChar code : codeStream) {
            if (code.red) {
                codeFont.setColor(1f, 0.4f, 0.4f, 1f);
            } else {
                codeFont.setColor(0.45f, 1f, 0.6f, 1f);
            }
            codeFont.draw(spriteBatch, code.text, code.x, code.y);
        }
    }

    @Override
    protected void resetSpecialSkill() {
        pulses.clear();
        codeStream.clear();
        pulseTimer = PULSE_FIRST_DELAY;
        breathTimer = BREATH_FIRST_DELAY;
        breathState = BreathState.IDLE;
        effectTime = 0f;
    }

    @Override
    public void dispose() {
        super.dispose();
        codeFont.dispose();
    }

    private static class Pulse {
        private final float x;
        private final float y;
        private float time;

        private Pulse(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class CodeChar {
        private final String text;
        private float x;
        private final float y;
        private final boolean red;

        private CodeChar(String text, float x, float y, boolean red) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.red = red;
        }
    }
}
