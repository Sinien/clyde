//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;

/**
 * Displays a track with a little frob somewhere along its length that allows a user to select a
 * smoothly varying value between two bounds.
 */
public class Slider extends Component
    implements UIConstants
{
    /**
     * Creates a slider with the specified orientation, range and value.
     *
     * @param orient either {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    public Slider (GlContext ctx, int orient, int min, int max, int value)
    {
        this(ctx, orient, new BoundedRangeModel(min, value, 0, max));
    }

    /**
     * Creates a slider with the specified orientation and range model. Note that the extent must
     * be set to zero.
     *
     * @param orient either {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    public Slider (GlContext ctx, int orient, BoundedRangeModel model)
    {
        super(ctx);
        _orient = orient;
        _model = model;
    }

    /**
     * Returns a reference to the slider's range model.
     */
    public BoundedRangeModel getModel ()
    {
        return _model;
    }

    @Override // documentation inherited
    protected String getDefaultStyleConfig ()
    {
        return ((_orient == HORIZONTAL) ? "Default/H" : "Default/V") + "Slider";
    }

    @Override // documentation inherited
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);

        // load up our frob
        _frobs[state] = (config.icon == null) ? null : config.icon.getIcon(_ctx);
    }

    // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        Dimension psize =
            new Dimension(getFrob().getWidth(), getFrob().getHeight());
        if (_orient == HORIZONTAL) {
            psize.width *= 2;
        } else {
            psize.height *= 2;
        }
        return psize;
    }

    // documentation inherited
    public boolean dispatchEvent (Event event)
    {
        if (isEnabled() && event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            int mx = mev.getX() - getAbsoluteX(), my = mev.getY() - getAbsoluteY();
            switch (mev.getType()) {
            case MouseEvent.MOUSE_PRESSED:
                if (mev.getButton() == 0) {
                    // move the slider based on the current mouse position
                    updateValue(mx, my);
                }
                break;

            case MouseEvent.MOUSE_DRAGGED:
                // move the slider based on the current mouse position
                updateValue(mx, my);
                break;

            case MouseEvent.MOUSE_WHEELED:
                // move by 1/10th if we're wheeled
                int delta = _model.getRange()/10, value = _model.getValue();
                _model.setValue(mev.getDelta() > 0 ? value + delta : value - delta);
                break;

            default:
                return super.dispatchEvent(event);
            }

            return true;
        }

        return super.dispatchEvent(event);
    }

    // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        // render our frob at the appropriate location
        Insets insets = getInsets();
        Icon frob = getFrob();
        int x, y, range = _model.getRange();
        int offset = _model.getValue() - _model.getMinimum();
        if (_orient == HORIZONTAL) {
            y = (getHeight() - frob.getHeight())/2;
            x = insets.left + (getWidth() - insets.getHorizontal() -
                               frob.getWidth()) * offset / range;
        } else {
            x = (getWidth() - frob.getWidth())/2;
            y = insets.bottom + (getHeight() - insets.getVertical() -
                                 frob.getHeight()) * offset / range;
        }
        frob.render(renderer, x, y, _alpha);
    }

    protected void updateValue (int mx, int my)
    {
        Insets insets = getInsets();
        Icon frob = getFrob();
        if (_orient == HORIZONTAL) {
            int fwid = frob.getWidth();
            _model.setValue(_model.getMinimum() + Math.round((mx - fwid*0.5f) * _model.getRange() /
                            (getWidth() - insets.getHorizontal() - fwid)));
        } else {
            int fhei = frob.getHeight();
            _model.setValue(_model.getMinimum() + Math.round((my - fhei*0.5f) * _model.getRange() /
                            (getHeight() - insets.getVertical() - fhei)));
        }
    }

    protected Icon getFrob ()
    {
        Icon frob = _frobs[getState()];
        return (frob != null) ? frob : _frobs[DEFAULT];
    }

    protected int _orient;
    protected BoundedRangeModel _model;
    protected Icon[] _frobs = new Icon[getStateCount()];
}
