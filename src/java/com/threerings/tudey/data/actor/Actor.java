//
// $Id$

package com.threerings.tudey.data.actor;

import com.threerings.io.Streamable;

import com.threerings.config.ConfigReference;
import com.threerings.delta.Deltable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;
import com.threerings.util.DeepObject;

import com.threerings.tudey.config.ActorConfig;

/**
 * Represents an active, stateful element of the scene.
 */
public class Actor extends DeepObject
    implements Streamable, Deltable
{
    /** A flag indicating that the actor has changed its position in a discontinuous fashion. */
    public static final int WARP = (1 << 0);

    /**
     * Creates a new actor.
     */
    public Actor (
        ConfigReference<ActorConfig> config, int id, int created,
        Vector2f translation, float rotation)
    {
        _config = config;
        _id = id;
        _created = created;
        _translation.set(translation);
        _rotation = rotation;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Actor ()
    {
        // these will be set later
        _id = 0;
        _created = 0;
    }

    /**
     * Sets the actor's config reference.
     */
    public void setConfig (ConfigReference<ActorConfig> config)
    {
        _config = config;
    }

    /**
     * Returns the actor's config reference.
     */
    public ConfigReference<ActorConfig> getConfig ()
    {
        return _config;
    }

    /**
     * Returns the actor's unique identifier.
     */
    public int getId ()
    {
        return _id;
    }

    /**
     * Returns the timestamp at which the actor was created.
     */
    public int getCreated ()
    {
        return _created;
    }

    /**
     * Sets the timestamp at which the actor was destroyed.
     */
    public void setDestroyed (int destroyed)
    {
        _destroyed = destroyed;
    }

    /**
     * Returns the timestamp at which the actor was destroyed, or {@link Integer.MAX_VALUE} if
     * not yet destroyed.
     */
    public int getDestroyed ()
    {
        return _destroyed;
    }

    /**
     * Returns a reference to the actor's translation vector.
     */
    public Vector2f getTranslation ()
    {
        return _translation;
    }

    /**
     * Sets the actor's rotation angle.
     */
    public void setRotation (float rotation)
    {
        _rotation = rotation;
    }

    /**
     * Returns the actor's rotation angle.
     */
    public float getRotation ()
    {
        return _rotation;
    }

    /**
     * Sets the actor's flags.
     */
    public void setFlags (int flags)
    {
        _flags = flags;
    }

    /**
     * Returns the actor's flags.
     */
    public int getFlags ()
    {
        return _flags;
    }

    /**
     * Sets a flag.
     */
    public void set (int flag)
    {
        _flags |= flag;
    }

    /**
     * Clears a flag.
     */
    public void clear (int flag)
    {
        _flags &= ~flag;
    }

    /**
     * Determines whether a flag is set.
     */
    public boolean isSet (int flag)
    {
        return (_flags & flag) != 0;
    }

    /**
     * Interpolates between the state of this actor and that of the specified other, placing the
     * result in the provided object.
     */
    public Actor interpolate (Actor other, float t, Actor result)
    {
        // start by deep-copying this actor
        copy(result);

        // interpolate translation and rotation unless warped
        if (!other.isSet(WARP)) {
            _translation.lerp(other.getTranslation(), t, result.getTranslation());
            result.setRotation(FloatMath.lerpa(_rotation, other.getRotation(), t));
        }
        return result;
    }

    /**
     * Extrapolates the state of this actor after the specified time interval, in seconds (which
     * may be negative).
     */
    public Actor extrapolate (float elapsed, Actor result)
    {
        return (Actor)copy(result);
    }

    /** The actor's configuration reference. */
    protected ConfigReference<ActorConfig> _config;

    /** The actor's unique identifier. */
    protected final int _id;

    /** The timestamp at which the actor was created. */
    protected final int _created;

    /** The timestamp at which the actor was destroyed. */
    protected int _destroyed = Integer.MAX_VALUE;

    /** The actor's translation. */
    protected Vector2f _translation = new Vector2f();

    /** The actor's rotation angle. */
    protected float _rotation;

    /** Various flags. */
    protected int _flags;
}
