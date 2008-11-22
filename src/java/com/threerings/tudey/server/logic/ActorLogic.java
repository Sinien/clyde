//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * Controls the state of an actor on the server.
 */
public class ActorLogic extends Logic
{
    /**
     * Initializes the actor.
     */
    public void init (
        TudeySceneManager scenemgr, ConfigReference<ActorConfig> ref,
        ActorConfig.Original config, int id, int timestamp, Vector2f translation, float rotation)
    {
        super.init(scenemgr);
        _config = config;
        _actor = createActor(ref, id, timestamp, translation, rotation);
    }

    /**
     * Returns a reference to the current state of the actor.
     */
    public Actor getActor ()
    {
        return _actor;
    }

    /**
     * Creates the actor object.
     */
    protected Actor createActor (
        ConfigReference<ActorConfig> ref, int id, int timestamp,
        Vector2f translation, float rotation)
    {
        return new Actor(ref, id, timestamp, translation, rotation);
    }

    /** The actor configuration. */
    protected ActorConfig.Original _config;

    /** The current state of the actor. */
    protected Actor _actor;
}
