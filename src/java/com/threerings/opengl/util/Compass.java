//
// $Id$

package com.threerings.opengl.util;

import java.awt.Font;

import org.lwjgl.opengl.GL11;

import com.threerings.math.Quaternion;
import com.threerings.math.Vector3f;

import com.threerings.opengl.gui.Root;
import com.threerings.opengl.gui.text.CharacterTextFactory;
import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Batch;
import com.threerings.opengl.renderer.Renderer;

/**
 * A compass that displays the coordinate system axes.
 */
public class Compass
    implements Renderable
{
    /**
     * Creates a new compass.
     */
    public Compass (GlContext ctx)
    {
        _ctx = ctx;

        // create the axis labels
        CharacterTextFactory factory = CharacterTextFactory.getInstance(
            new Font("Dialog", Font.PLAIN, 12), true);
        _x = factory.createText("x", new Color4f(0.75f, 0f, 0f, 1f));
        _y = factory.createText("y", new Color4f(0f, 0.75f, 0f, 1f));
        _z = factory.createText("z", Color4f.BLUE);

        _batch = new Batch() {
            public boolean draw (Renderer renderer) {
                renderer.setStates(Root.STATES);
                Compass.this.draw(renderer);
                return true;
            }
        };
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        _ctx.getRenderer().enqueueOrtho(_batch);
    }

    /**
     * Draws the compass.
     */
    protected void draw (Renderer renderer)
    {
        Quaternion rotation = renderer.getCamera().getViewTransform().getRotation();
        rotation.transformUnitX(_vector);
        drawAxis(renderer, _x, _vector.x, _vector.y);

        rotation.transformUnitY(_vector);
        drawAxis(renderer, _y, _vector.x, _vector.y);

        rotation.transformUnitZ(_vector);
        drawAxis(renderer, _z, _vector.x, _vector.y);
    }

    /**
     * Draws one of the compass axes.
     *
     * @param x the x component of the rotated axis.
     * @param y the y component of the rotated axis.
     */
    protected void drawAxis (Renderer renderer, Text label, float x, float y)
    {
        // draw the label (coordinates fudged to get the text lined up right)
        label.render(renderer, (int)(29f + 28f*x), (int)(29f + 28f*y), 1f);

        // draw the line (in the same color)
        renderer.setTextureState(null);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(32f, 32f);
        GL11.glVertex2f(32f + 20f*x, 32f + 20f*y);
        GL11.glEnd();
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** The axis labels. */
    protected Text _x, _y, _z;

    /** The batch to enqueue for rendering. */
    protected Batch _batch;

    /** Temporary vector used to determine where the axes lie. */
    protected Vector3f _vector = new Vector3f();
}
