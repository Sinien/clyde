//
// $Id$

package com.threerings.export;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import java.util.zip.InflaterInputStream;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Tuple;

import com.threerings.util.ReflectionUtil;

import static com.threerings.export.Log.*;

/**
 * Imports from the compact binary format generated by {@link BinaryExporter}.
 */
public class BinaryImporter extends Importer
{
    /**
     * Creates an importer to read from the specified stream.
     */
    public BinaryImporter (InputStream in)
    {
        _in = new DataInputStream(_base = in);

        // populate the class map with the bootstrap classes
        for (int ii = 0; ii < BinaryExporter.BOOTSTRAP_CLASSES.length; ii++) {
            _classes.put(ii + 1, getClassWrapper(BinaryExporter.BOOTSTRAP_CLASSES[ii]));
        }
        // get these by reference so we don't have to keep looking them up
        _objectClass = getClassWrapper(Object.class);
        _stringClass = getClassWrapper(String.class);
    }

    @Override // documentation inherited
    public Object readObject ()
        throws IOException
    {
        if (_objects == null) {
            // verify the preamble
            int magic = _in.readInt();
            if (magic != BinaryExporter.MAGIC_NUMBER) {
                throw new IOException("Invalid magic number [magic=" +
                    Integer.toHexString(magic) + "].");
            }
            int version = _in.readInt();
            if (version != BinaryExporter.VERSION) {
                throw new IOException("Invalid version [version=" +
                    Integer.toHexString(version) + "].");
            }

            // the rest of the stream will be compressed
            _in = new DataInputStream(new InflaterInputStream(_base));

            // initialize mapping
            _objects = new HashIntMap<Object>();
            _objects.put(0, NULL);
        }
        return read(_objectClass);
    }

    @Override // documentation inherited
    public boolean read (String name, boolean defvalue)
        throws IOException
    {
        try {
            Boolean value = (Boolean)_fields.get(name);
            return (value == null) ? defvalue : value;
        } catch (ClassCastException e) {
            log.warning("Wrong value type [value=" + _fields.get(name) + "].", e);
            return defvalue;
        }
    }

    @Override // documentation inherited
    public byte read (String name, byte defvalue)
        throws IOException
    {
        try {
            Byte value = (Byte)_fields.get(name);
            return (value == null) ? defvalue : value;
        } catch (ClassCastException e) {
            log.warning("Wrong value type [value=" + _fields.get(name) + "].", e);
            return defvalue;
        }
    }

    @Override // documentation inherited
    public char read (String name, char defvalue)
        throws IOException
    {
        try {
            Character value = (Character)_fields.get(name);
            return (value == null) ? defvalue : value;
        } catch (ClassCastException e) {
            log.warning("Wrong value type [value=" + _fields.get(name) + "].", e);
            return defvalue;
        }
    }

    @Override // documentation inherited
    public double read (String name, double defvalue)
        throws IOException
    {
        try {
            Double value = (Double)_fields.get(name);
            return (value == null) ? defvalue : value;
        } catch (ClassCastException e) {
            log.warning("Wrong value type [value=" + _fields.get(name) + "].", e);
            return defvalue;
        }
    }

    @Override // documentation inherited
    public float read (String name, float defvalue)
        throws IOException
    {
        try {
            Float value = (Float)_fields.get(name);
            return (value == null) ? defvalue : value;
        } catch (ClassCastException e) {
            log.warning("Wrong value type [value=" + _fields.get(name) + "].", e);
            return defvalue;
        }
    }

    @Override // documentation inherited
    public int read (String name, int defvalue)
        throws IOException
    {
        try {
            Integer value = (Integer)_fields.get(name);
            return (value == null) ? defvalue : value;
        } catch (ClassCastException e) {
            log.warning("Wrong value type [value=" + _fields.get(name) + "].", e);
            return defvalue;
        }
    }

    @Override // documentation inherited
    public long read (String name, long defvalue)
        throws IOException
    {
        try {
            Long value = (Long)_fields.get(name);
            return (value == null) ? defvalue : value;
        } catch (ClassCastException e) {
            log.warning("Wrong value type [value=" + _fields.get(name) + "].", e);
            return defvalue;
        }
    }

    @Override // documentation inherited
    public short read (String name, short defvalue)
        throws IOException
    {
        try {
            Short value = (Short)_fields.get(name);
            return (value == null) ? defvalue : value;
        } catch (ClassCastException e) {
            log.warning("Wrong value type [value=" + _fields.get(name) + "].", e);
            return defvalue;
        }
    }

    @Override // documentation inherited
    public <T> T read (String name, T defvalue, Class<T> clazz)
        throws IOException
    {
        try {
            @SuppressWarnings("unchecked") T value = (T)_fields.get(name);
            return (value == null) ? defvalue : value;
        } catch (ClassCastException e) {
            log.warning("Wrong value type [value=" + _fields.get(name) + "].", e);
            return defvalue;
        }
    }

    @Override // documentation inherited
    public void close ()
        throws IOException
    {
        // close the underlying stream
        _in.close();
    }

    /**
     * Reads in an object of the specified class.
     */
    protected Object read (Class clazz)
        throws IOException
    {
        return read(getClassWrapper(clazz));
    }

    /**
     * Reads in an object of the specified class.
     */
    protected Object read (ClassWrapper clazz)
        throws IOException
    {
        // read primitive values directly
        if (clazz.isPrimitive()) {
            return readValue(clazz, -1);
        }

        // read in the id, see if we've seen it before
        int objectId = _objectIdReader.read();
        Object value = _objects.get(objectId);
        if (value != null) {
            return (value == NULL) ? null : value;
        }
        // if not, read the value
        return readValue(clazz, objectId);
    }

    /**
     * Reads in an object of the specified class.
     */
    protected Object readValue (ClassWrapper clazz, int objectId)
        throws IOException
    {
        // read in the class unless we can determine it implicitly
        ClassWrapper cclazz = clazz;
        if (!clazz.isFinal()) {
            cclazz = readClass();
        }
        // see if we can stream the value directly
        Class wclazz = cclazz.getWrappedClass();
        Streamer streamer = (wclazz == null) ? null : Streamer.getStreamer(wclazz);
        if (streamer != null) {
            Object value = null;
            try {
                value = streamer.read(_in);
            } catch (ClassNotFoundException e) {
                log.warning("Class not found.", e);
            }
            if (value != null && objectId != -1) {
                _objects.put(objectId, value);
            }
            return value;
        }
        // otherwise, create and populate the object
        Object value = null;
        int length = 0;
        if (cclazz.isArray()) {
            length = _in.readInt();
            if (wclazz != null) {
                value = Array.newInstance(wclazz.getComponentType(), length);
            }
        } else {
            Object outer = cclazz.isInner() ? read(_objectClass) : null;
            if (wclazz != null) {
                value = ReflectionUtil.newInstance(wclazz, outer);
            }
        }
        _objects.put(objectId, (value == null) ? NULL : value);
        if (cclazz.isArray()) {
            readEntries(value == null ? new Object[length] : (Object[])value,
                cclazz.getComponentType());
        } else if (cclazz.isCollection()) {
            @SuppressWarnings("unchecked") Collection<Object> collection =
                (value == null) ? new ArrayList<Object>() : (Collection<Object>)value;
            readEntries(collection);
        } else if (cclazz.isMap()) {
            @SuppressWarnings("unchecked") Map<Object, Object> map =
                (value == null) ? new HashMap<Object, Object>() : (Map<Object, Object>)value;
            readEntries(map);
        } else {
            ClassData cdata = _classData.get(cclazz);
            if (cdata == null) {
                _classData.put(cclazz, cdata = new ClassData());
            }
            _fields = cdata.readFields();
            if (value instanceof Exportable) {
                readFields((Exportable)value);
            }
            _fields = null;
        }
        return value;
    }

    /**
     * Reads in a class reference.  While it's possibly simply to write the class reference out
     * as a normal object, we keep a separate id space for object/field classes in order to keep
     * the ids small.
     */
    protected ClassWrapper readClass ()
        throws IOException
    {
        // read in the id, see if we've seen it before
        int classId = _classIdReader.read();
        ClassWrapper clazz = _classes.get(classId);
        if (clazz != null) {
            return clazz;
        }
        // if not, read and map the value
        _classes.put(classId, clazz = getClassWrapper(_in.readUTF(), _in.readByte()));
        return clazz;
    }

    /**
     * Populates the supplied array with the entries under the current element.
     */
    protected void readEntries (Object[] array, ClassWrapper cclazz)
        throws IOException
    {
        for (int ii = 0; ii < array.length; ii++) {
            array[ii] = read(cclazz);
        }
    }

    /**
     * Populates the supplied collection with the entries under the current element.
     */
    protected void readEntries (Collection<Object> collection)
        throws IOException
    {
        for (int ii = 0, nn = _in.readInt(); ii < nn; ii++) {
            collection.add(read(_objectClass));
        }
    }

    /**
     * Populates the supplied map with the entries under the current element.
     */
    protected void readEntries (Map<Object, Object> map)
        throws IOException
    {
        for (int ii = 0, nn = _in.readInt(); ii < nn; ii++) {
            map.put(read(_objectClass), read(_objectClass));
        }
    }

    /**
     * Returns a shared class wrapper instance.
     */
    protected ClassWrapper getClassWrapper (String name, byte flags)
    {
        ClassWrapper wrapper = _wrappersByName.get(name);
        if (wrapper == null) {
            _wrappersByName.put(name, wrapper = new ClassWrapper(name, flags));
            Class clazz = wrapper.getWrappedClass();
            if (clazz != null) {
                _wrappersByClass.put(clazz, wrapper);
            }
        }
        return wrapper;
    }

    /**
     * Returns a shared class wrapper instance.
     */
    protected ClassWrapper getClassWrapper (Class clazz)
    {
        ClassWrapper wrapper = _wrappersByClass.get(clazz);
        if (wrapper == null) {
            _wrappersByClass.put(clazz, wrapper = new ClassWrapper(clazz));
            _wrappersByName.put(clazz.getName(), wrapper);
        }
        return wrapper;
    }

    /**
     * Contains information on a class in the stream, which may or may not be resolvable.
     */
    protected class ClassWrapper
    {
        public ClassWrapper (String name, byte flags)
        {
            _name = name;
            if (name.charAt(0) == '[') {
                _flags = BinaryExporter.FINAL_CLASS_FLAG;
                String cname = name.substring(1);
                char type = cname.charAt(0);
                if (type == '[') { // sub-array
                    _componentType = getClassWrapper(cname, flags);
                } else if (type == 'L') { // object class or interface
                    _componentType = getClassWrapper(cname.substring(1, cname.length()-1), flags);
                } else { // primitive array
                    try {
                        _clazz = Class.forName(name);
                    } catch (ClassNotFoundException e) { }
                    _componentType = getClassWrapper(_clazz.getComponentType());
                    return;
                }
                if (_componentType.getWrappedClass() == null) {
                    return; // don't bother trying to resolve the array class
                }
            } else {
                _flags = flags;
            }
            try {
                _clazz = Class.forName(name);
            } catch (ClassNotFoundException e) {
                log.warning("Couldn't find class to import [name=" + name + "].");
            }
        }

        public ClassWrapper (Class clazz)
        {
            _name = clazz.getName();
            _flags = BinaryExporter.getFlags(clazz);
            if (clazz.isArray()) {
                _componentType = getClassWrapper(clazz.getComponentType());
            }
            _clazz = clazz;
        }

        /**
         * Returns the name of the class.
         */
        public String getName ()
        {
            return _name;
        }

        /**
         * Determines whether the wrapped class is final.
         */
        public boolean isFinal ()
        {
            return (_flags & BinaryExporter.FINAL_CLASS_FLAG) != 0;
        }

        /**
         * Determines whether the wrapped class is a non-static inner class.
         */
        public boolean isInner ()
        {
            return (_flags & BinaryExporter.INNER_CLASS_FLAG) != 0;
        }

        /**
         * Determines whether the wrapped class is a collection class.
         */
        public boolean isCollection ()
        {
            return (_flags & BinaryExporter.COLLECTION_CLASS_FLAG) != 0;
        }

        /**
         * Determines whether the wrapped class is a map class.
         */
        public boolean isMap ()
        {
            return (_flags & BinaryExporter.MAP_CLASS_FLAG) != 0;
        }

        /**
         * Determines whether the wrapped class is an array class.
         */
        public boolean isArray ()
        {
            return (_componentType != null);
        }

        /**
         * Determines whether the wrapped class is a primitive class.
         */
        public boolean isPrimitive ()
        {
            return (_clazz != null && _clazz.isPrimitive());
        }

        /**
         * Returns the wrapper of the component type, if this is an array class.
         */
        public ClassWrapper getComponentType ()
        {
            return _componentType;
        }

        /**
         * Returns the wrapped class, if it could be resolved.
         */
        public Class getWrappedClass ()
        {
            return _clazz;
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return _name.hashCode();
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return ((ClassWrapper)other)._name.equals(_name);
        }

        /** The name of the class. */
        protected String _name;

        /** The class flags. */
        protected byte _flags;

        /** The component type wrapper. */
        protected ClassWrapper _componentType;

        /** The class reference, if it could be resolved. */
        protected Class _clazz;
    }

    /**
     * Contains information on an exportable class.
     */
    protected class ClassData
    {
        /**
         * Reads the field values in the supplied map.
         */
        public HashMap<String, Object> readFields ()
            throws IOException
        {
            int size = _in.readInt();
            HashMap<String, Object> fields = new HashMap<String, Object>(size);
            for (int ii = 0; ii < size; ii++) {
                readField(fields);
            }
            return fields;
        }

        /**
         * Reads in a single field value.
         */
        protected void readField (HashMap<String, Object> fields)
            throws IOException
        {
            int fieldId = _fieldIdReader.read();
            Tuple<String, ClassWrapper> fieldData = _fieldData.get(fieldId);
            if (fieldData == null) {
                String name = (String)read(_stringClass);
                ClassWrapper clazz = readClass();
                _fieldData.put(fieldId, fieldData = new Tuple<String, ClassWrapper>(
                    name, clazz));
            }
            fields.put(fieldData.left, read(fieldData.right));
        }

        /** Maps field ids to name/class pairs. */
        protected HashIntMap<Tuple<String, ClassWrapper>> _fieldData =
            new HashIntMap<Tuple<String, ClassWrapper>>();

        /** Used to read field ids. */
        protected IDReader _fieldIdReader = new IDReader();
    }

    /**
     * Reads in integer identifiers using a width that depends on the highest value read so
     * far.
     *
     * @see BinaryExporter.IDWriter
     */
    protected class IDReader
    {
        /**
         * Reads in an id whose width depends on the highest value read so far.
         */
        public int read ()
            throws IOException
        {
            int id;
            if (_highest < 255) {
                id = _in.readUnsignedByte();
            } else if (_highest < 65535) {
                id = _in.readUnsignedShort();
            } else {
                id = _in.readInt();
            }
            _highest = Math.max(_highest, id);
            return id;
        }

        /** The highest value written so far. */
        protected int _highest;
    }

    /** The underlying input stream. */
    protected InputStream _base;

    /** The stream that we use for reading data. */
    protected DataInputStream _in;

    /** Maps ids to objects read.  A null value indicates that the stream has not yet been
     * initialized. */
    protected HashIntMap<Object> _objects;

    /** Used to read object ids. */
    protected IDReader _objectIdReader = new IDReader();

    /** Field values associated with the current object. */
    protected HashMap<String, Object> _fields;

    /** Maps class names to wrapper objects (for classes identified in the stream). */
    protected HashMap<String, ClassWrapper> _wrappersByName = new HashMap<String, ClassWrapper>();

    /** Maps class objects to wrapper objects (for classes identified by reference). */
    protected HashMap<Class, ClassWrapper> _wrappersByClass = new HashMap<Class, ClassWrapper>();

    /** The wrapper for the object class. */
    protected ClassWrapper _objectClass;

    /** The wrapper for the String class. */
    protected ClassWrapper _stringClass;

    /** Maps ids to classes read. */
    protected HashIntMap<ClassWrapper> _classes = new HashIntMap<ClassWrapper>();

    /** Used to read class ids. */
    protected IDReader _classIdReader = new IDReader();

    /** Class data. */
    protected HashMap<ClassWrapper, ClassData> _classData = new HashMap<ClassWrapper, ClassData>();

    /** Signifies a null entry in the object map. */
    protected static final Object NULL = new Object() { };
}
