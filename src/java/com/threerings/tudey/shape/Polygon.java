//
// $Id$

package com.threerings.tudey.shape;

import org.lwjgl.opengl.GL11;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.space.SpaceElement;

/**
 * A convex polygon.
 */
public class Polygon extends Shape
{
    /**
     * Creates a polygon with the supplied vertices.
     */
    public Polygon (Vector2f... vertices)
    {
        _vertices = new Vector2f[vertices.length];
        for (int ii = 0; ii < vertices.length; ii++) {
            _vertices[ii] = new Vector2f(vertices[ii]);
        }
        updateBounds();
    }

    /**
     * Creates an uninitialized polygon with the specified number of vertices.
     */
    public Polygon (int vcount)
    {
        initVertices(vcount);
    }

    /**
     * Returns the number of vertices in this polygon.
     */
    public int getVertexCount ()
    {
        return _vertices.length;
    }

    /**
     * Returns a reference to the indexed vertex.
     */
    public Vector2f getVertex (int idx)
    {
        return _vertices[idx];
    }

    /**
     * Checks whether the polygon contains the specified point.
     */
    public boolean contains (Vector2f pt)
    {
        return contains(pt.x, pt.y);
    }

    /**
     * Checks whether the polygon contains the specified point.
     */
    public boolean contains (float x, float y)
    {
        // check the point against each edge
        for (int ii = 0; ii < _vertices.length; ii++) {
            Vector2f start = _vertices[ii], end = _vertices[(ii + 1) % _vertices.length];
            float a = start.y - end.y;
            float b = end.x - start.x;
            if (a*x + b*y < a*start.x + b*start.y) {
                return false;
            }
        }
        return true;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _bounds.fromPoints(_vertices);
    }

    @Override // documentation inherited
    public Shape transform (Transform2D transform, Shape result)
    {
        Polygon presult = (result instanceof Polygon) ?
            ((Polygon)result) : new Polygon(_vertices.length);
        if (presult.getVertexCount() != _vertices.length) {
            presult.initVertices(_vertices.length);
        }
        for (int ii = 0; ii < _vertices.length; ii++) {
            transform.transformPoint(_vertices[ii], presult._vertices[ii]);
        }
        presult.updateBounds();
        return presult;
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        // see if we start inside the polygon
        Vector2f origin = ray.getOrigin();
        if (contains(origin)) {
            result.set(origin);
            return true;
        }
        // check the ray against each edge (making sure it's on the right side)
        for (int ii = 0; ii < _vertices.length; ii++) {
            Vector2f start = _vertices[ii], end = _vertices[(ii + 1) % _vertices.length];
            float a = start.y - end.y;
            float b = end.x - start.x;
            if (a*origin.x + b*origin.y <= a*start.x + b*start.y &&
                    ray.getIntersection(start, end, result)) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public IntersectionType getIntersectionType (Rect rect)
    {
        // make sure the bounds intersect (this is equivalent to doing a separating axis test
        // using the axes of the rectangle)
        if (!_bounds.intersects(rect)) {
            return IntersectionType.NONE;
        }

        // consider each edge of this polygon as a potential separating axis
        int ccount = 0;
        Vector2f rmin = rect.getMinimumExtent(), rmax = rect.getMaximumExtent();
        for (int ii = 0; ii < _vertices.length; ii++) {
            Vector2f start = _vertices[ii], end = _vertices[(ii + 1) % _vertices.length];
            float a = start.y - end.y;
            float b = end.x - start.x;
            float c = a*start.x + b*start.y;

            // determine how many vertices fall inside/outside the edge
            int inside =
                (a*rmin.x + b*rmin.y >= c ? 1 : 0) +
                (a*rmax.x + b*rmin.y >= c ? 1 : 0) +
                (a*rmax.x + b*rmax.y >= c ? 1 : 0) +
                (a*rmin.x + b*rmax.y >= c ? 1 : 0);
            if (inside == 0) {
                return IntersectionType.NONE;
            } else if (inside == 4) {
                ccount++;
            }
        }
        return (ccount == _vertices.length) ?
            IntersectionType.CONTAINS : IntersectionType.INTERSECTS;
    }

    @Override // documentation inherited
    public boolean intersects (SpaceElement element)
    {
        return element.intersects(this);
    }

    @Override // documentation inherited
    public boolean intersects (Shape shape)
    {
        return shape.intersects(this);
    }

    @Override // documentation inherited
    public boolean intersects (Point point)
    {
        return contains(point.getLocation());
    }

    @Override // documentation inherited
    public boolean intersects (Segment segment)
    {
        // see if we start inside the polygon
        Vector2f origin = segment.getStart();
        if (contains(origin)) {
            return true;
        }
        // check the segment against each edge (making sure it's on the right side)
        for (int ii = 0; ii < _vertices.length; ii++) {
            Vector2f start = _vertices[ii], end = _vertices[(ii + 1) % _vertices.length];
            float a = start.y - end.y;
            float b = end.x - start.x;
            if (a*origin.x + b*origin.y <= a*start.x + b*start.y &&
                    segment.intersects(start, end)) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Circle circle)
    {
        // find the first edge that the circle's center is outside
        Vector2f center = circle.getCenter();
        for (int ii = 0; ii < _vertices.length; ii++) {
            Vector2f start = _vertices[ii], end = _vertices[(ii + 1) % _vertices.length];
            float a = start.y - end.y;
            float b = end.x - start.x;
            float l2 = a*a + b*b;
            float d = a*center.x + b*center.y - a*start.x - b*start.y;
            if (d >= 0f) {
                continue;
            }
            // now classify with respect to the adjacent edges
            Vector2f previous = _vertices[(ii + _vertices.length - 1) % _vertices.length];
            a = previous.y - start.y;
            b = start.x - previous.x;
            if (a*center.x + b*center.y <= a*previous.x + b*previous.y) {
                // left: closest feature is start vertex
                return start.distanceSquared(center) <= circle.radius*circle.radius;
            }
            Vector2f next = _vertices[(ii + 2) % _vertices.length];
            a = end.y - next.y;
            b = next.x - end.x;
            if (a*center.x + b*center.y > a*end.x + b*end.y) {
                // middle: closest feature is edge
                return d*d <= l2*circle.radius*circle.radius;
            } else {
                // right: closest feature is end vertex
                return end.distanceSquared(center) <= circle.radius*circle.radius;
            }
        }
        return true; // center is inside all edges
    }

    @Override // documentation inherited
    public boolean intersects (Capsule capsule)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Polygon polygon)
    {
        return intersectsOnAxes(polygon) && polygon.intersectsOnAxes(this);
    }

    @Override // documentation inherited
    public boolean intersects (Compound compound)
    {
        return compound.intersects(this);
    }

    @Override // documentation inherited
    public void draw (boolean outline)
    {
        GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_POLYGON);
        for (Vector2f vertex : _vertices) {
            GL11.glVertex2f(vertex.x, vertex.y);
        }
        GL11.glEnd();
    }

    /**
     * (Re)initializes the vertex array for the specified number of vertices.
     */
    protected void initVertices (int vcount)
    {
        _vertices = new Vector2f[vcount];
        for (int ii = 0; ii < vcount; ii++) {
            _vertices[ii] = new Vector2f();
        }
    }

    /**
     * Tests the edges of this polygon as potential separating axes for this polygon and the
     * specified other.
     *
     * @return false if the polygons are disjoint on any of this polygon's axes, true if they
     * intersect on all axes.
     */
    protected boolean intersectsOnAxes (Polygon other)
    {
        // consider each edge of this polygon as a potential separating axis
    OUTER:
        for (int ii = 0; ii < _vertices.length; ii++) {
            Vector2f start = _vertices[ii], end = _vertices[(ii + 1) % _vertices.length];
            float a = start.y - end.y;
            float b = end.x - start.x;
            float c = a*start.x + b*start.y;

            // if all vertices fall outside the edge, the polygons are disjoint
            for (Vector2f vertex : other._vertices) {
                if (a*vertex.x + b*vertex.y >= c) {
                    continue OUTER;
                }
            }
            return false;
        }
        return true;
    }

    /** The vertices of the polygon. */
    protected Vector2f[] _vertices;
}
