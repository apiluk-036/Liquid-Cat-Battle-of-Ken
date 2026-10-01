package io.github.game;

public class Player {
    float x;
    float y;
    float width;
    float height;
    float velocityX;
    float velocityY;
    boolean onGround;
    int facing = 1;

    public Player(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
