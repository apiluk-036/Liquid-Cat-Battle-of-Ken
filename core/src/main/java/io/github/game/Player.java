package io.github.game;

import com.badlogic.gdx.math.Rectangle;

public class Player {
    private static final int MAX_HP = 100;
    private static final float INVINCIBLE_TIME = 1f;

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
    }

    /** Returns true if the damage was applied (false while invincible after a hit). */
    public boolean takeDamage(int damage) {
        if (invincibleTimer > 0f || hp <= 0) {
            return false;
        }
        setHp(hp - damage);
        invincibleTimer = INVINCIBLE_TIME;
        return true;
    }

    public boolean isInvincible() {
        return invincibleTimer > 0f;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public Rectangle getBounds() {
        return bounds.set(x, y, width, height);
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
