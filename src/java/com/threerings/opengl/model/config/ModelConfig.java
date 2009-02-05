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

package com.threerings.opengl.model.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.samskivert.util.ComparableTuple;
import com.samskivert.util.ObjectUtil;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.editor.util.EditorContext;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.math.Box;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.effect.config.MetaParticleSystemConfig;
import com.threerings.opengl.effect.config.ParticleSystemConfig;
import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.CollisionMesh;
import com.threerings.opengl.model.tools.ModelDef;
import com.threerings.opengl.model.tools.xml.ModelParser;
import com.threerings.opengl.renderer.config.TextureConfig;
import com.threerings.opengl.scene.config.ViewerAffecterConfig;
import com.threerings.opengl.scene.config.SceneInfluencerConfig;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.*;

/**
 * The configuration of a model.
 */
public class ModelConfig extends ParameterizedConfig
{
    /** The default tag for unskinned meshes. */
    public static final String DEFAULT_TAG = "default";

    /** The default tag for skinned meshes. */
    public static final String SKINNED_TAG = "skinned";

    /**
     * Contains the actual implementation of the model.
     */
    @EditorTypes({
        StaticConfig.class, StaticSetConfig.class, ArticulatedConfig.class,
        ParticleSystemConfig.class, MetaParticleSystemConfig.class, SceneInfluencerConfig.class,
        ViewerAffecterConfig.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Updates this implementation from its external source, if any.
         *
         * @param force if true, reload the source data even if it has already been loaded.
         */
        public void updateFromSource (EditorContext ctx, boolean force)
        {
            // nothing by default
        }

        /**
         * Adds the implementation's update references to the provided set.
         */
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Returns a reference to the config manager to use when resolving references.
         *
         * @param cfgmgr the config manager of the config containing the implementation.
         */
        public ConfigManager getConfigManager (ConfigManager cfgmgr)
        {
            return cfgmgr;
        }

        /**
         * Creates or updates a model implementation for this configuration.
         *
         * @param scope the model's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl);

        /**
         * Returns the {@link GeometryConfig} to use when this model is selected for use within a
         * particle system (or <code>null</code> if it cannot be used).
         */
        public GeometryConfig getParticleGeometry (GlContext ctx)
        {
            return null;
        }

        /**
         * Returns a reference to the material to use when this model is selected for use within a
         * particle system.
         */
        public ConfigReference<MaterialConfig> getParticleMaterial (GlContext ctx)
        {
            return null;
        }

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
    }

    /**
     * Superclass of the imported implementations (this is not abstract because in order for the
     * exporter to create a prototype of MaterialMapping, it must be able to instantiate this
     * class).
     */
    public static class Imported extends Implementation
    {
        /**
         * Represents a mapping from texture name to material.
         */
        public class MaterialMapping extends DeepObject
            implements Exportable
        {
            /** The name of the texture. */
            @Editable(editor="choice", hgroup="t")
            public String texture;

            /** The name of the tag. */
            @Editable(hgroup="t")
            public String tag = DEFAULT_TAG;

            /** The material for unskinned meshes. */
            @Editable(nullable=true)
            public ConfigReference<MaterialConfig> material;

            public MaterialMapping (String texture, String tag, String path)
            {
                this.texture = texture;
                this.tag = tag;

                String material = DEFAULT_MATERIALS.get(tag);
                this.material = new ConfigReference<MaterialConfig>(
                    (material == null) ? DEFAULT_MATERIAL : material,
                    "Texture", new ConfigReference<TextureConfig>(DEFAULT_TEXTURE, "File", path));
            }

            public MaterialMapping ()
            {
            }

            /**
             * Returns the options available for the texture field.
             */
            public String[] getTextureOptions ()
            {
                TreeSet<String> textures = new TreeSet<String>();
                getTextures(textures);
                return textures.toArray(new String[textures.size()]);
            }
        }

        /** The model scale. */
        @Editable(min=0, step=0.01, hgroup="s")
        public float scale = 0.01f;

        /** A fixed amount by which to expand the bounds (to account for skinning). */
        @Editable(min=0, step=0.01, hgroup="s")
        public float boundsExpansion;

        /** If true, ignore the transforms of the top-level children. */
        @Editable(hgroup="i")
        public boolean ignoreRootTransforms;

        /** If true, generate tangent attributes for meshes. */
        @Editable(hgroup="i")
        public boolean generateTangents;

        /** The influences allowed to affect this model. */
        @Editable
        public InfluenceFlagConfig influences = new InfluenceFlagConfig();

        /** The mappings from texture name to material. */
        @Editable(depends={"source"})
        public MaterialMapping[] materialMappings = new MaterialMapping[0];

        /**
         * Sets the source file from which to load the animation data.
         */
        @Editable(editor="resource", nullable=true)
        @FileConstraints(
            description="m.exported_models",
            extensions={".mxml"},
            directory="exported_model_dir")
        public void setSource (String source)
        {
            _source = source;
            _reload = true;
        }

        /**
         * Returns the source file.
         */
        @Editable
        public String getSource ()
        {
            return _source;
        }

        @Override // documentation inherited
        public void updateFromSource (EditorContext ctx, boolean force)
        {
            if (!(_reload || force)) {
                return;
            }
            _reload = false;
            if (_source == null) {
                updateFromSource(null);
                return;
            }
            if (_parser == null) {
                _parser = new ModelParser();
            }
            try {
                updateFromSource(_parser.parseModel(
                    ctx.getResourceManager().getResource(_source)));
                createDefaultMaterialMappings();
            } catch (Exception e) {
                log.warning("Error parsing model [source=" + _source + "].", e);
            }
        }

        @Override // documentation inherited
        public Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl)
        {
            return null;
        }

        @Override // documentation inherited
        public GeometryConfig getParticleGeometry (GlContext ctx)
        {
            VisibleMesh mesh = getParticleMesh();
            return (mesh == null) ? null : mesh.geometry;
        }

        @Override // documentation inherited
        public ConfigReference<MaterialConfig> getParticleMaterial (GlContext ctx)
        {
            VisibleMesh mesh = getParticleMesh();
            if (mesh == null) {
                return null;
            }
            MaterialMapping mapping = getMaterialMapping(mesh.texture, mesh.tag);
            return (mapping == null) ? null : mapping.material;
        }

        /**
         * Returns the {@link VisibleMesh} to use when this model is selected for use within a
         * particle system (or <code>null</code> if it cannot be used).
         */
        protected VisibleMesh getParticleMesh ()
        {
            return null;
        }

        /**
         * Updates from a parsed model definition.
         */
        protected void updateFromSource (ModelDef def)
        {
            // nothing by default
        }

        /**
         * Creates default material mappings for any unmapped textures.
         */
        protected void createDefaultMaterialMappings ()
        {
            TreeSet<ComparableTuple<String, String>> pairs = Sets.newTreeSet();
            getTextureTagPairs(pairs);
            ArrayList<MaterialMapping> mappings = new ArrayList<MaterialMapping>();
            Collections.addAll(mappings, materialMappings);
            String pref = _source.substring(0, _source.lastIndexOf('/') + 1);
            for (ComparableTuple<String, String> pair : pairs) {
                String texture = pair.left, tag = pair.right;
                if (getMaterialMapping(texture, tag) == null) {
                    mappings.add(new MaterialMapping(
                        texture, tag, (texture == null) ? null : pref + texture));
                }
            }
            materialMappings = mappings.toArray(new MaterialMapping[mappings.size()]);
        }

        /**
         * Returns the material mapping for the specified texture (if any).
         */
        protected MaterialMapping getMaterialMapping (String texture, String tag)
        {
            for (MaterialMapping mapping : materialMappings) {
                if (ObjectUtil.equals(texture, mapping.texture) && tag.equals(mapping.tag)) {
                    return mapping;
                }
            }
            return null;
        }

        /**
         * Populates the supplied set with the names of all referenced textures.
         */
        protected void getTextures (TreeSet<String> textures)
        {
            // nothing by default
        }

        /**
         * Populates the supplied set with the names of all referenced texture/tag pairs.
         */
        protected void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
        {
            // nothing by default
        }

        /** The resource from which we read the model data. */
        protected String _source;

        /** Indicates that {@link #updateFromSource} should reload the data. */
        @DeepOmit
        protected transient boolean _reload;
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The model reference. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(ModelConfig.class, model);
        }

        @Override // documentation inherited
        public ConfigManager getConfigManager (ConfigManager cfgmgr)
        {
            ModelConfig config = cfgmgr.getConfig(ModelConfig.class, model);
            return (config == null) ? cfgmgr : config.getConfigManager();
        }

        @Override // documentation inherited
        public Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl)
        {
            ModelConfig config = ctx.getConfigManager().getConfig(ModelConfig.class, model);
            return (config == null) ? null : config.getModelImplementation(ctx, scope, impl);
        }

        @Override // documentation inherited
        public GeometryConfig getParticleGeometry (GlContext ctx)
        {
            ModelConfig config = ctx.getConfigManager().getConfig(ModelConfig.class, model);
            return (config == null) ? null : config.getParticleGeometry(ctx);
        }

        @Override // documentation inherited
        public ConfigReference<MaterialConfig> getParticleMaterial (GlContext ctx)
        {
            ModelConfig config = ctx.getConfigManager().getConfig(ModelConfig.class, model);
            return (config == null) ? null : config.getParticleMaterial(ctx);
        }
    }

    /**
     * Contains a set of meshes.
     */
    public static class MeshSet extends DeepObject
        implements Exportable
    {
        /** The bounds of the meshes. */
        public Box bounds;

        /** The visible meshes. */
        public VisibleMesh[] visible;

        /** The collision mesh. */
        public CollisionMesh collision;

        public MeshSet (VisibleMesh[] visible, CollisionMesh collision)
        {
            bounds = new Box();
            for (VisibleMesh mesh : (this.visible = visible)) {
                bounds.addLocal(mesh.geometry.getBounds());
            }
            if ((this.collision = collision) != null) {
                bounds.addLocal(collision.getBounds());
            }
        }

        public MeshSet ()
        {
        }

        /**
         * Populates the supplied set with the names of all referenced textures.
         */
        public void getTextures (TreeSet<String> textures)
        {
            for (VisibleMesh mesh : visible) {
                textures.add(mesh.texture);
            }
        }

        /**
         * Populates the supplied set with the names of all referenced texture/tag pairs.
         */
        public void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
        {
            for (VisibleMesh mesh : visible) {
                pairs.add(new ComparableTuple<String, String>(mesh.texture, mesh.tag));
            }
        }
    }

    /**
     * Pairs a texture name with a geometry config.
     */
    public static class VisibleMesh extends DeepObject
        implements Exportable
    {
        /** The name of the texture associated with the mesh. */
        public String texture;

        /** The mesh tag. */
        public String tag;

        /** The mesh geometry. */
        public GeometryConfig geometry;

        public VisibleMesh (String texture, String tag, GeometryConfig geometry)
        {
            this.texture = texture;
            this.tag = tag;
            this.geometry = geometry;
        }

        public VisibleMesh ()
        {
        }
    }

    /** The actual model implementation. */
    @Editable
    public Implementation implementation = new StaticConfig();

    /**
     * Creates or updates a model implementation for this configuration.
     *
     * @param scope the model's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        return implementation.getModelImplementation(ctx, scope, impl);
    }

    /**
     * Returns the {@link GeometryConfig} to use when this model is selected for use within a
     * particle system (or <code>null</code> if it cannot be used).
     */
    public GeometryConfig getParticleGeometry (GlContext ctx)
    {
        return implementation.getParticleGeometry(ctx);
    }

    /**
     * Returns a reference to the material to use when this model is selected for use within a
     * particle system.
     */
    public ConfigReference<MaterialConfig> getParticleMaterial (GlContext ctx)
    {
        return implementation.getParticleMaterial(ctx);
    }

    @Override // documentation inherited
    public void init (ConfigManager cfgmgr)
    {
        _configs.init("model", cfgmgr);
        super.init(_configs);
    }

    @Override // documentation inherited
    public ConfigManager getConfigManager ()
    {
        return implementation.getConfigManager(_configs);
    }

    @Override // documentation inherited
    public void updateFromSource (EditorContext ctx, boolean force)
    {
        implementation.updateFromSource(ctx, force);
    }

    @Override // documentation inherited
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }

    /** The model's local config library. */
    protected ConfigManager _configs = new ConfigManager();

    /** Parses model exports. */
    protected static ModelParser _parser;

    /** The default material for the default tag. */
    protected static final String DEFAULT_MATERIAL = "Model/Opaque";

    /** The default texture config (which we expect to take a single parameter, "File,"
     * representing the texture image path). */
    protected static final String DEFAULT_TEXTURE = "2D/File/Default";

    /** Maps tags to default materials (each of which we expect to take a single parameter,
     * "Texture," representing the texture reference). */
    protected static final HashMap<String, String> DEFAULT_MATERIALS = Maps.newHashMap();
    static {
        DEFAULT_MATERIALS.put(DEFAULT_TAG, DEFAULT_MATERIAL);
        DEFAULT_MATERIALS.put(SKINNED_TAG, "Model/Skinned/Opaque");
    }
}
