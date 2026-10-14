package io.github.game;

import com.badlogic.gdx.math.Rectangle;

public class Player {
    private static final int MAX_HP = 100;
    private static final float INVINCIBLE_TIME = 1f;
    // The cat sprite is drawn up and to the right of the movement box (see Main.drawCatSprite),
    // so attacks are checked against the visible body instead.
    private static final float BODY_OFFSET_X = 40f;
    private static final float BODY_OFFSET_Y = 38f;
    private static final float BODY_WIDTH = 56f;
    private static final float BODY_HEIGHT = 72f;

    float x;
    float y;
    float width;
    float height;
    float velocityX;
    float velocityY;
    boolean onGround;
    int facing = 1;
    private int hp = MAX_HP;
    private float invincibleTimer;
    private float shieldTimer;
    private final float startX;
    private final float startY;
    private final Rectangle bounds = new Rectangle();

    public Player(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.startX = x;
        this.startY = y;
    }

    public void update(float delta) {
        invincibleTimer = Math.max(0f, invincibleTimer - delta);
        shieldTimer = Math.max(0f, shieldTimer - delta);
    }

    /** Returns true if the damage was applied (false while invincible after a hit). */
    public boolean takeDamage(int damage) {
        if (invincibleTimer > 0f || shieldTimer > 0f || hp <= 0) {
            return false;
        }
        setHp(hp - damage);
        invincibleTimer = INVINCIBLE_TIME;
        return true;
    }

    public boolean isInvincible() {
        return invincibleTimer > 0f;
    }

    public void activateShield(float seconds) {
        shieldTimer = seconds;
    }

    public boolean isShielded() {
        return shieldTimer > 0f;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public float getCenterX() {
        return x + BODY_OFFSET_X + BODY_WIDTH / 2f;
    }

    public float getCenterY() {
        return y + BODY_OFFSET_Y + BODY_HEIGHT / 2f;
    }

    /** Right edge of the visible body, measured from x. */
    public float getBodyRightOffset() {
        return BODY_OFFSET_X + BODY_WIDTH;
    }

    /** The visible cat body, used for every hit check. */
    public Rectangle getBounds() {
        return bounds.set(x + BODY_OFFSET_X, y + BODY_OFFSET_Y, BODY_WIDTH, BODY_HEIGHT);
    }

    public void reset() {
        x = startX;
        y = startY;
        velocityX = 0f;
        velocityY = 0f;
        onGround = false;
        facing = 1;
        hp = MAX_HP;
        invincibleTimer = 0f;
        shieldTimer = 0f;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return MAX_HP;
    }

    public void setHp(int hp) {
        this.hp = Math.max(0, Math.min(MAX_HP, hp));
    }
}
