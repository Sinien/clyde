//
// $Id$

package com.threerings.tudey.shape;

import com.threerings.math.FloatMath;
import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.space.SpaceElement;

/**
 * A Tudey shape.
 */
public abstract class Shape
{
    /** Intersection types indicating that the shape does not intersect, intersects, or fully
     * contains, respectively, the parameter. */
    public enum IntersectionType { NONE, INTERSECTS, CONTAINS };

    /**
     * Returns a reference to the bounds of the shape.
     */
    public Rect getBounds ()
    {
        return _bounds;
    }

    /**
     * Updates the bounds of the shape.
     */
    public abstract void updateBounds ();

    /**
     * Retrieves the center of the shape.
     *
     * @return a new vector containing the result.
     */
    public Vector2f getCenter ()
    {
        return getCenter(new Vector2f());
    }

    /**
     * Retrieves the center of the shape and places it in the supplied vector.
     *
     * @return a reference to the result vector, for chaining.
     */
    public abstract Vector2f getCenter (Vector2f result);

    /**
     * Transforms this shape in-place.
     *
     * @return a reference to this shape, for chaining.
     */
    public Shape transformLocal (Transform2D transform)
    {
        return transform(transform, this);
    }

    /**
     * Transforms this shape.
     *
     * @return a new shape containing the result.
     */
    public Shape transform (Transform2D transform)
    {
        return transform(transform, null);
    }

    /**
     * Transforms this shape, placing the result in the provided object if possible.
     *
     * @return a reference to the result object, if it was reused; otherwise, a new object
     * containing the result.
     */
    public abstract Shape transform (Transform2D transform, Shape result);

    /**
     * Finds the intersection of a ray with this shape and places it in the supplied vector
     * (if it exists).
     *
     * @return true if the ray intersected the shape (in which case the result will contain the
     * point of intersection), false otherwise.
     */
    public abstract boolean getIntersection (Ray2D ray, Vector2f result);

    /**
     * Checks whether the intersector intersects the specified rect.
     */
    public abstract IntersectionType getIntersectionType (Rect rect);

    /**
     * Determines whether this shape intersects the specified element.  Uses double-dispatch to
     * invoke the appropriate specialization of {@link SpaceElement#intersects}.
     */
    public abstract boolean intersects (SpaceElement element);

    /**
     * Determines whether this shape intersects the specified shape.  Uses double-dispatch to
     * invoke the appropriate method specialization.
     */
    public abstract boolean intersects (Shape shape);

    /**
     * Checks for an intersection with this shape and the specified point.
     */
    public abstract boolean intersects (Point point);

    /**
     * Checks for an intersection with this shape and the specified segment.
     */
    public abstract boolean intersects (Segment segment);

    /**
     * Checks for an intersection with this shape and the specified circle.
     */
    public abstract boolean intersects (Circle circle);

    /**
     * Checks for an intersection with this shape and the specified capsule.
     */
    public abstract boolean intersects (Capsule capsule);

    /**
     * Checks for an intersection with this shape and the specified polygon.
     */
    public abstract boolean intersects (Polygon polygon);

    /**
     * Checks for an intersection with this shape and the specified compound.
     */
    public abstract boolean intersects (Compound compound);

    /**
     * Draws this shape in immediate mode.
     *
     * @param outline if true, draw the outline of the shape; otherwise, the solid form.
     */
    public abstract void draw (boolean outline);

    /**
     * Updates the value of the closest point and returns a new result vector reference.
     */
    protected static Vector2f updateClosest (Vector2f origin, Vector2f result, Vector2f closest)
    {
        if (result == closest) {
            return new Vector2f();
        }
        if (origin.distanceSquared(result) < origin.distanceSquared(closest)) {
            closest.set(result);
        }
        return result;
    }

    /** The bounds of the shape. */
    protected Rect _bounds = new Rect();

    /** The number of segments to use when we render circles. */
    protected static final int CIRCLE_SEGMENTS = 16;

    /** The increment angle between circle segments. */
    protected static final float CIRCLE_INCREMENT = FloatMath.TWO_PI / CIRCLE_SEGMENTS;
}
