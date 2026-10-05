package io.github.game;

public class Player {
    private static final int MAX_HP = 100;

    float x;
    float y;
    float width;
    float height;
    float velocityX;
    float velocityY;
    boolean onGround;
    int facing = 1;
    private int hp = MAX_HP;

    public Player(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
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
