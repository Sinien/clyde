//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl.model;

import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.Scoped;
import com.threerings.math.Box;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.model.config.CompoundConfig;
import com.threerings.opengl.model.config.CompoundConfig.ComponentModel;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.GlContext;

/**
 * A compound model implementation.
 */
public class Compound extends Model.Implementation
{
    /**
     * Creates a new static implementation.
     */
    public Compound (GlContext ctx, Scope parentScope, CompoundConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        setConfig(config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (CompoundConfig config)
    {
        _config = config;
        updateFromConfig();
    }

    @Override // documentation inherited
    public void reset ()
    {
        for (Model model : _models) {
            model.reset();
        }
    }

    @Override // documentation inherited
    public int getInfluenceFlags ()
    {
        return _influenceFlags;
    }

    @Override // documentation inherited
    public Box getBounds ()
    {
        return _bounds;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        // update the world transform
        if (_parentWorldTransform == null) {
            return;
        }
        _parentWorldTransform.compose(_localTransform, _worldTransform);

        // and the world bounds
        _nbounds.setToEmpty();
        for (Model model : _models) {
            model.updateBounds();
            _nbounds.addLocal(model.getBounds());
        }
        if (!_bounds.equals(_nbounds)) {
            ((Model)_parentScope).boundsWillChange();
            _bounds.set(_nbounds);
            ((Model)_parentScope).boundsDidChange();
        }
    }

    @Override // documentation inherited
    public void drawBounds ()
    {
        DebugBounds.draw(_bounds, Color4f.WHITE);
        for (Model model : _models) {
            model.drawBounds();
        }
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        // notify component models
        Scene scene = ((Model)_parentScope).getScene();
        for (Model model : _models) {
            model.wasAdded(scene);
        }
    }

    @Override // documentation inherited
    public void willBeRemoved ()
    {
        // notify component models
        for (Model model : _models) {
            model.willBeRemoved();
        }
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        // update the world transform
        _parentWorldTransform.compose(_localTransform, _worldTransform);

        // tick the component models
        _nbounds.setToEmpty();
        for (Model model : _models) {
            model.tick(elapsed);
            model.updateBounds();
            _nbounds.addLocal(model.getBounds());
        }

        // update the bounds if necessary
        if (!_bounds.equals(_nbounds)) {
            ((Model)_parentScope).boundsWillChange();
            _bounds.set(_nbounds);
            ((Model)_parentScope).boundsDidChange();
        }
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        // exit early if there's no bounds intersection
        if (!_bounds.intersects(ray)) {
            return false;
        }
        // check the component models
        Vector3f closest = result;
        for (Model model : _models) {
            if (model.getIntersection(ray, result)) {
                result = Articulated.updateClosest(ray.getOrigin(), result, closest);
            }
        }
        // if we ever changed the result reference, that means we hit something
        return (result != closest);
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        // update the view transform
        _parentViewTransform.compose(_localTransform, _viewTransform);

        // enqueue the component models
        for (Model model : _models) {
            model.enqueue();
        }
    }

    @Override // documentation inherited
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        updateFromConfig();
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        // create the component models
        Scene scene = ((Model)_parentScope).getScene();
        Model[] omodels = _models;
        _models = new Model[_config.models.length];
        for (int ii = 0; ii < _models.length; ii++) {
            Model model = (omodels == null || omodels.length <= ii) ?
                new Model(_ctx) : omodels[ii];
            _models[ii] = model;
            ComponentModel component = _config.models[ii];
            model.setParentScope(this);
            model.setConfig(component.model);
            model.getLocalTransform().set(component.transform);
            if (model.getScene() == null && scene != null) {
                model.wasAdded(scene);
            }
        }
        if (omodels != null) {
            for (int ii = _models.length; ii < omodels.length; ii++) {
                Model model = omodels[ii];
                if (scene != null) {
                    model.willBeRemoved();
                }
                model.dispose();
            }
        }

        // update the influence flags
        _influenceFlags = _config.influences.getFlags();

        // update the bounds
        updateBounds();
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model config. */
    protected CompoundConfig _config;

    /** The component models. */
    protected Model[] _models;

    /** The parent world transform. */
    @Bound("worldTransform")
    protected Transform3D _parentWorldTransform;

    /** The parent view transform. */
    @Bound("viewTransform")
    protected Transform3D _parentViewTransform;

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The world transform. */
    @Scoped
    protected Transform3D _worldTransform = new Transform3D();

    /** The view transform. */
    @Scoped
    protected Transform3D _viewTransform = new Transform3D();

    /** Flags indicating which influences can affect the model. */
    protected int _influenceFlags;

    /** The bounds of the model. */
    protected Box _bounds = new Box();

    /** Holds the bounds of the model when updating. */
    protected Box _nbounds = new Box();
}