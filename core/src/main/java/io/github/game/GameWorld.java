package io.github.game;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class GameWorld {
    private final Array<Rectangle> solidBlocks = new Array<>();

    public GameWorld(float width) {
        solidBlocks.add(new Rectangle(0f, 0f, width, 30f));
    }

    public void update(Player player, float delta, float gravity) {
        player.velocityY -= gravity * delta;
        moveHorizontally(player, player.x + player.velocityX * delta);
        moveVertically(player, player.y + player.velocityY * delta);

        if (player.y < 0f) {
            player.y = 0f;
            player.velocityY = 0f;
            player.onGround = true;
        }
    }

    private void moveHorizontally(Player player, float targetX) {
        Rectangle nextBox = new Rectangle(targetX, player.y, player.width, player.height);
        for (Rectangle block : solidBlocks) {
            if (nextBox.overlaps(block)) {
                if (player.velocityX > 0) {
                    player.x = block.x - player.width;
                } else if (player.velocityX < 0) {
                    player.x = block.x + block.width;
                }
                player.velocityX = 0f;
                return;
            }
        }
        player.x = targetX;
    }

    private void moveVertically(Player player, float targetY) {
        Rectangle nextBox = new Rectangle(player.x, targetY, player.width, player.height);
        player.onGround = false;
        for (Rectangle block : solidBlocks) {
            if (nextBox.overlaps(block)) {
                if (player.velocityY > 0) {
                    player.y = block.y - player.height;
                } else if (player.velocityY < 0) {
                    player.y = block.y + block.height;
                    player.onGround = true;
                }
                player.velocityY = 0f;
                return;
            }
        }
        player.y = targetY;
    }
}
