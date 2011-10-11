//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
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

package com.threerings.opengl.scene.config;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.util.DeepObject;

import com.threerings.opengl.compositor.RenderScheme;
import com.threerings.opengl.material.Projection;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.material.config.TechniqueConfig;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.config.ColorStateConfig;
import com.threerings.opengl.scene.LightInfluence;
import com.threerings.opengl.scene.SceneInfluence;
import com.threerings.opengl.util.GlContext;

/**
 * Represents a means of generating shadows from a light.
 */
@EditorTypes({ ShadowConfig.SilhouetteTexture.class })
public abstract class ShadowConfig extends DeepObject
    implements Exportable
{
    /**
     * Generates shadows by rendering the silhouettes of shadow casters from the perspective
     * of the light into a texture and projecting that texture onto shadow receivers.
     */
    public static class SilhouetteTexture extends ShadowConfig
    {
        /** The projection material. */
        @Editable(nullable=true)
        public ConfigReference<MaterialConfig> material;

        /** The color state for the projection. */
        @Editable(nullable=true)
        public ColorStateConfig colorState = new ColorStateConfig();

        @Override // documentation inherited
        public SceneInfluence createInfluence (
            GlContext ctx, Scope scope, final Light light, ArrayList<Updater> updaters)
        {
            MaterialConfig mconfig = ctx.getConfigManager().getConfig(
                MaterialConfig.class, material);
            TechniqueConfig technique = (mconfig == null) ?
                null : mconfig.getTechnique(ctx, getProjectionScheme(light.getType()));
            if (technique == null) {
                return new LightInfluence(light);
            }
            final Projection projection = new Projection(technique,
                (colorState == null) ? null : colorState.getState());
            return new LightInfluence(light) {
                @Override public Projection getProjection () {
                    return projection;
                }
            };
        }
    }

    /**
     * Creates the scene influence corresponding to this config.
     *
     * @param updaters a list to populate with required updaters.
     */
    public abstract SceneInfluence createInfluence (
        GlContext ctx, Scope scope, Light light, ArrayList<Updater> updaters);

    /**
     * Returns the render scheme to use for the projection of the specified light type.
     */
    protected static String getProjectionScheme (Light.Type type)
    {
        switch (type) {
            case DIRECTIONAL: return RenderScheme.ORTHOGRAPHIC_PROJECTION;
            case SPOT: return RenderScheme.PERSPECTIVE_PROJECTION;
            default: return null;
        }
    }
}
