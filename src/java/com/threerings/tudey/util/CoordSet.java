//
// $Id$

package com.threerings.tudey.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Interator;
import com.samskivert.util.RandomUtil;

import com.threerings.opengl.gui.util.Rectangle;

/**
 * Contains a set of encoded coordinates.
 */
public class CoordSet extends AbstractSet<Coord>
{
    /**
     * Creates a coord set containing the contents of the supplied collection.
     */
    public CoordSet (Collection<Coord> collection)
    {
        addAll(collection);
    }

    /**
     * Creates a coord set containing all of the coordinates in the supplied region.
     */
    public CoordSet (Rectangle region)
    {
        addAll(region);
    }

    /**
     * Creates a coord set containing all of the coordinates in the supplied region.
     */
    public CoordSet (int x, int y, int width, int height)
    {
        addAll(x, y, width, height);
    }

    /**
     * Creates an empty coord set.
     */
    public CoordSet ()
    {
    }

    /**
     * Adds all of the coordinates in the specified region.
     *
     * @return whether or not the set changed as a result of the addition.
     */
    public boolean addAll (Rectangle region)
    {
        return addAll(region.x, region.y, region.width, region.height);
    }

    /**
     * Adds all of the coordinates in the specified region.
     *
     * @return whether or not the set changed as a result of the addition.
     */
    public boolean addAll (int x, int y, int width, int height)
    {
        boolean changed = false;
        for (int yy = y, yymax = y + height; yy < yymax; yy++) {
            for (int xx = x, xxmax = x + width; xx < xxmax; xx++) {
                changed |= add(xx, yy);
            }
        }
        return changed;
    }

    /**
     * Adds the specified coordinates to the set.
     *
     * @return whether or not the set changed as a result of the addition.
     */
    public boolean add (int x, int y)
    {
        return _coords.add(Coord.encode(x, y));
    }

    /**
     * Removes all of the coordinates in the specified region.
     *
     * @return whether or not the set changed as a result of the removal.
     */
    public boolean removeAll (Rectangle region)
    {
        return removeAll(region.x, region.y, region.width, region.height);
    }

    /**
     * Removes all of the coordinates in the specified region.
     *
     * @return whether or not the set changed as a result of the removal.
     */
    public boolean removeAll (int x, int y, int width, int height)
    {
        boolean changed = false;
        for (int yy = y, yymax = y + height; yy < yymax; yy++) {
            for (int xx = x, xxmax = x + width; xx < xxmax; xx++) {
                changed |= remove(xx, yy);
            }
        }
        return changed;
    }

    /**
     * Removes the specified coordinates from the set.
     *
     * @return whether or not the set changed as a result of the removal.
     */
    public boolean remove (int x, int y)
    {
        return _coords.remove(Coord.encode(x, y));
    }

    /**
     * Determines whether the set contains all of the coordinates in the specified region.
     */
    public boolean containsAll (Rectangle region)
    {
        return containsAll(region.x, region.y, region.width, region.height);
    }

    /**
     * Determines whether the set contains all of the coordinates in the specified region.
     */
    public boolean containsAll (int x, int y, int width, int height)
    {
        for (int yy = y, yymax = y + height; yy < yymax; yy++) {
            for (int xx = x, xxmax = x + width; xx < xxmax; xx++) {
                if (!contains(xx, yy)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines whether the set contains the specified coordinates.
     */
    public boolean contains (int x, int y)
    {
        return _coords.contains(Coord.encode(x, y));
    }

    /**
     * Selects and returns a random coordinate from the set.
     *
     * @return a new object containing the result.
     */
    public Coord pickRandom ()
    {
        return pickRandom(1, 1);
    }

    /**
     * Selects a random coordinate from the set and places it in the provided object.
     *
     * @return a reference to the result object, for chaining.
     */
    public Coord pickRandom (Coord result)
    {
        return pickRandom(1, 1, result);
    }

    /**
     * Selects a random coordinate that is the origin of a region within the set with the
     * supplied dimensions.
     *
     * @return a new object containing the result.
     */
    public Coord pickRandom (int width, int height)
    {
        return pickRandom(width, height, new Coord());
    }

    /**
     * Selects a random coordinate that is the origin of a region within the set with the
     * supplied dimensions and places it in the provided object.
     *
     * @return a reference to the result object, for chaining.
     */
    public Coord pickRandom (int width, int height, Coord result)
    {
        if (width == 1 && height == 1) {
            return get(RandomUtil.getInt(size()), result);
        }
        CoordSet origins = new CoordSet();
        for (Coord coord : this) {
            if (containsAll(coord.x, coord.y, width, height)) {
                origins.add(coord);
            }
        }
        return origins.pickRandom(result);
    }

    /**
     * Returns the coordinate at the specified index.
     *
     * @return a new object containing the result.
     */
    public Coord get (int idx)
    {
        return get(idx, new Coord());
    }

    /**
     * Retrieves the coordinate at the specified index and places it in the provided object.
     *
     * @return a reference to the result object, for chaining.
     */
    public Coord get (int idx, Coord result)
    {
        return result.set(_coords.get(idx));
    }

    /**
     * Finds the largest covered region of this set and stores it in the provided rectangle.
     */
    public Rectangle getLargestRegion (Rectangle result)
    {
        result.set(0, 0, 0, 0);
        for (Coord coord : this) {
            int maxWidth = Integer.MAX_VALUE;
            for (int yy = coord.y; contains(coord.x, yy); yy++) {
                int height = yy - coord.y + 1;
                int width = 1;
                while (width < maxWidth && contains(coord.x + width, yy)) {
                    width++;
                }
                maxWidth = width;
                if (width * height > result.getArea()) {
                    result.set(coord.x, coord.y, width, height);
                }
            }
        }
        return result;
    }

    /**
     * Creates a new set containing the coordinates that border the coordinates in this set.
     */
    public CoordSet getBorder ()
    {
        return getBorder(new CoordSet());
    }

    /**
     * Adds the coordinates that border the coordinates in this set to the provided set.
     *
     * @return a reference to the result set, for chaining.
     */
    public CoordSet getBorder (CoordSet result)
    {
        for (Coord coord : this) {
            for (Direction dir : Direction.values()) {
                int x = coord.x + dir.getX(), y = coord.y + dir.getY();
                if (!contains(x, y)) {
                    result.add(x, y);
                }
            }
        }
        return result;
    }

    /**
     * Creates a new set containing the coordinates that border the coordinates in this set in the
     * cardinal directions.
     */
    public CoordSet getCardinalBorder ()
    {
        return getCardinalBorder(new CoordSet());
    }

    /**
     * Adds the coordinates that border the coordinates in this set in cardinal directions to the
     * set provided.
     *
     * @return a reference to the result set, for chaining.
     */
    public CoordSet getCardinalBorder (CoordSet result)
    {
        for (Coord coord : this) {
            for (Direction dir : Direction.CARDINAL_VALUES) {
                int x = coord.x + dir.getX(), y = coord.y + dir.getY();
                if (!contains(x, y)) {
                    result.add(x, y);
                }
            }
        }
        return result;
    }

    @Override // documentation inherited
    public boolean add (Coord coord)
    {
        return add(coord.x, coord.y);
    }

    @Override // documentation inherited
    public boolean remove (Object object)
    {
        if (!(object instanceof Coord)) {
            return false;
        }
        Coord coord = (Coord)object;
        return remove(coord.x, coord.y);
    }

    @Override // documentation inherited
    public boolean contains (Object object)
    {
        if (!(object instanceof Coord)) {
            return false;
        }
        Coord coord = (Coord)object;
        return contains(coord.x, coord.y);
    }

    @Override // documentation inherited
    public int size ()
    {
        return _coords.size();
    }

    @Override // documentation inherited
    public Iterator<Coord> iterator ()
    {
        return new Iterator<Coord>() {
            public boolean hasNext () {
                return _it.hasNext();
            }
            public Coord next () {
                return _dummy.set(_it.nextInt());
            }
            public void remove () {
                _it.remove();
            }
            protected Interator _it = _coords.interator();
            protected Coord _dummy = new Coord();
        };
    }

    /** The underlying set. */
    protected ArrayIntSet _coords = new ArrayIntSet();
}