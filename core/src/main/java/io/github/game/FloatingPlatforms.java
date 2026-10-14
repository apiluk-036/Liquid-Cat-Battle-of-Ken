package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class FloatingPlatforms {
    private static final float DRAW_WIDTH = 192f;
    private static final float DRAW_HEIGHT = 96f;
    private static final float DECK_INSET_X = 4.5f;
    private static final float DECK_TOP_Y = 66f;
    private static final float DECK_THICKNESS = 10f;
    // Main.drawCatSprite draws the cat's feet up and to the right of its hitbox,
    // so the surfaces are shifted to keep the cat standing on the deck.
    private static final float CAT_FEET_OFFSET_X = 50f;
    private static final float CAT_FEET_OFFSET_Y = 36f;

    private final Texture texture;
    private final Array<Rectangle> sprites = new Array<>();
    private final Array<Rectangle> surfaces = new Array<>();

    public FloatingPlatforms() {
        texture = Gdx.files.internal("platform.png").exists()
            ? new Texture(Gdx.files.internal("platform.png"), true)
            : null;
        if (texture == null) {
            return;
        }
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);

        // Two low platforms, then a staircase up; the right side is left clear for the boss.
        add(10f, 150f);
        add(265f, 150f);
        add(140f, 236f);
        add(300f, 322f);
    }

    private void add(float x, float deckTopY) {
        sprites.add(new Rectangle(x, deckTopY - DECK_TOP_Y, DRAW_WIDTH, DRAW_HEIGHT));
        surfaces.add(new Rectangle(
            x + DECK_INSET_X - CAT_FEET_OFFSET_X,
            deckTopY - CAT_FEET_OFFSET_Y - DECK_THICKNESS,
            DRAW_WIDTH - DECK_INSET_X * 2f,
            DECK_THICKNESS
        ));
    }

    public Array<Rectangle> getSurfaces() {
        return surfaces;
    }

    public void draw(SpriteBatch spriteBatch) {
        for (Rectangle sprite : sprites) {
            spriteBatch.draw(texture, sprite.x, sprite.y, sprite.width, sprite.height);
        }
    }

    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }
}
