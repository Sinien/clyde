//
// $Id$

package com.threerings.tudey.data;

import com.threerings.io.Streamable;
import com.threerings.util.DeepObject;

/**
 * Represents a single frame of user input.
 */
public class InputFrame extends DeepObject
    implements Streamable
{
    /** Indicates that the user wants to move. */
    public static final int MOVE = (1 << 0);

    /** Indicates that the user wants to strafe. */
    public static final int STRAFE = (1 << 1);

    /** Indicates that the user wants to interact. */
    public static final int INTERACT = (1 << 2);

    /**
     * Creates a new input frame.
     */
    public InputFrame (int timestamp, float direction, int flags)
    {
        _timestamp = timestamp;
        _direction = direction;
        _flags = flags;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public InputFrame ()
    {
    }

    /**
     * Returns the timestamp of this frame.
     */
    public int getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Returns the direction requested by the user.
     */
    public float getDirection ()
    {
        return _direction;
    }

    /**
     * Returns the user's input flags.
     */
    public int getFlags ()
    {
        return _flags;
    }

    /**
     * Determines whether a flag is set.
     */
    public boolean isSet (int flag)
    {
        return (_flags & flag) != 0;
    }

    @Override // documentation inherited
    public String toString ()
    {
        return "[timestamp=" + _timestamp + ", direction=" +
            _direction + ", flags=" + _flags + "]";
    }

    /** The timestamp of the input frame. */
    protected int _timestamp;

    /** The direction requested by the user. */
    protected float _direction;

    /** The user's input flags. */
    protected int _flags;
}
