//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.config.ConfigReference;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.EffectConfig;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * Handles an effect on the server.
 */
public class EffectLogic extends Logic
{
    /**
     * Initializes the logic.
     */
    public void init (
        TudeySceneManager scenemgr, ConfigReference<EffectConfig> ref,
        EffectConfig.Original config, int timestamp, Vector2f translation, float rotation)
    {
        super.init(scenemgr);
        _config = config;
        _effect = createEffect(ref, timestamp, translation, rotation);
    }

    /**
     * Returns a reference to the effect fired.
     */
    public Effect getEffect ()
    {
        return _effect;
    }

    /**
     * Returns the effect's area of influence.  Clients are notified of effects when their
     * areas of interest intersect the effect's area of influence.
     */
    public Rect getInfluence ()
    {
        return _influence;
    }

    /**
     * Creates the effect for this logic.
     */
    protected Effect createEffect (
        ConfigReference<EffectConfig> ref, int timestamp, Vector2f translation, float rotation)
    {
        return new Effect(ref, timestamp, translation, rotation);
    }

    /** The effect configuration. */
    protected EffectConfig.Original _config;

    /** The effect fired. */
    protected Effect _effect;

    /** The effect's area of influence. */
    protected Rect _influence = new Rect();
}
