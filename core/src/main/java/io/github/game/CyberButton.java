package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/** Cyber-style BACK button shared by the menu screens: dark panel with cut corners and a neon outline. */
public class CyberButton {
    public static final Color NEON = new Color(0.3f, 0.95f, 1f, 1f);
    public static final Color PANEL = new Color(0.02f, 0.07f, 0.13f, 0.9f);
    private static final float CUT = 10f;
    private static final float LABEL_X = 38f;

    private final Rectangle bounds;
    private final String label;
    private final GlyphLayout layout = new GlyphLayout();
    private boolean hovered;

    public CyberButton(String label, float x, float y, float width, float height) {
        this.label = label;
        bounds = new Rectangle(x, y, width, height);
    }

    /** Returns true when the button was clicked this frame. */
    public boolean update(float mouseX, float mouseY) {
        hovered = bounds.contains(mouseX, mouseY);
        return hovered && Gdx.input.justTouched();
    }

    /** Call between shapeRenderer.begin(Filled) and end(). pulse goes from 0 to 1 and back. */
    public void drawShape(ShapeRenderer shapeRenderer, float pulse) {
        float x = bounds.x;
        float y = bounds.y;
        float width = bounds.width;
        float height = bounds.height;
        if (hovered) {
            shapeRenderer.setColor(NEON.r, NEON.g, NEON.b, 0.14f + 0.1f * pulse);
            cutCornerRect(shapeRenderer, x - 5f, y - 5f, width + 10f, height + 10f, CUT + 3f);
        }
        shapeRenderer.setColor(NEON.r, NEON.g, NEON.b, hovered ? 1f : 0.75f);
        cutCornerRect(shapeRenderer, x, y, width, height, CUT);
        shapeRenderer.setColor(PANEL);
        cutCornerRect(shapeRenderer, x + 2f, y + 2f, width - 4f, height - 4f, CUT - 1f);
        if (hovered) {
            shapeRenderer.setColor(NEON.r, NEON.g, NEON.b, 0.22f);
            cutCornerRect(shapeRenderer, x + 2f, y + 2f, width - 4f, height - 4f, CUT - 1f);
        }

        // Tech details: a short bar under the label and two notches on the right edge.
        shapeRenderer.setColor(NEON.r, NEON.g, NEON.b, 0.55f);
        shapeRenderer.rect(x + LABEL_X, y + 6f, 40f, 2f);
        shapeRenderer.rect(x + width - 14f, y + height - 12f, 6f, 2f);
        shapeRenderer.rect(x + width - 14f, y + height - 17f, 6f, 2f);

        float arrowX = x + 14f - (hovered ? 3f * pulse : 0f);
        float arrowY = y + height / 2f;
        shapeRenderer.setColor(hovered ? Color.WHITE : NEON);
        shapeRenderer.triangle(arrowX, arrowY, arrowX + 11f, arrowY + 8f, arrowX + 11f, arrowY - 8f);
        shapeRenderer.rect(arrowX + 11f, arrowY - 2.5f, 9f, 5f);
    }

    /** Call between spriteBatch.begin() and end(). */
    public void drawLabel(SpriteBatch spriteBatch, BitmapFont font) {
        font.setColor(hovered ? Color.WHITE : NEON);
        layout.setText(font, label);
        font.draw(spriteBatch, label, bounds.x + LABEL_X, bounds.y + (bounds.height + layout.height) / 2f);
    }

    /** Rectangle with the top-left and bottom-right corners cut off. */
    public static void cutCornerRect(ShapeRenderer shapeRenderer, float x, float y, float width, float height,
                                     float cut) {
        shapeRenderer.rect(x + cut, y, width - cut * 2f, height);
        shapeRenderer.rect(x, y, cut, height - cut);
        shapeRenderer.rect(x + width - cut, y + cut, cut, height - cut);
        shapeRenderer.triangle(x, y + height - cut, x + cut, y + height - cut, x + cut, y + height);
        shapeRenderer.triangle(x + width - cut, y, x + width - cut, y + cut, x + width, y + cut);
    }
}
