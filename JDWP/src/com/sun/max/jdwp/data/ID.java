/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.max.jdwp.data;

import java.io.*;
import java.lang.reflect.*;

/**
 * This class is a wrapper around a long value representing a JDWP identifier.
 *
 * @author Thomas Wuerthinger
 *
 */
public class ID {

    private final long value;

    public ID(long value) {
        this.value = value;
    }

    /**
     * The long value that is wrapped by this ID object.
     *
     * @return the long value
     */
    public long value() {
        return value;
    }

    /**
     * Writes the long value of this ID object to an output stream.
     *
     * @param outputStream the stream that is used for writing the value
     * @throws IOException this exception is thrown, when there was an error while writing the long value
     */
    public void write(OutputStream outputStream) throws IOException {
        new DataOutputStream(outputStream).writeLong(value);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + Long.toString(value) + ")";
    }

    @Override
    public int hashCode() {
        return (int) value;
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        }

        if (obj == null || !(ID.class.isAssignableFrom(obj.getClass()))) {
            return false;
        }

        final ID otherID = (ID) obj;
        if (otherID.value != value) {
            return false;
        }

        return true; // obj.getClass().isAssignableFrom(this.getClass()) || this.getClass().isAssignableFrom(obj.getClass());
    }

    /**
     * Creates a new ID object by reflection.
     *
     * @param value the long value of the created ID
     * @param klass the class of the created ID
     * @return the newly created object
     */
    public static <ID_Type extends ID> ID_Type create(long value, Class<ID_Type> klass) {
        ID_Type t;

        try {
            t = klass.getConstructor(long.class).newInstance(value);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e);
        } catch (SecurityException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }

        return t;
    }

    /**
     * Reads data from a stream to create a new ID object.
     *
     * @param inputStream the input stream to read the data from
     * @param klass the class of the newly created ID object
     * @return the read ID object
     * @throws IOException this exception is thrown when there was an exception while reading the long value
     */
    public static <ID_Type extends ID> ID_Type read(InputStream inputStream, Class<ID_Type> klass) throws IOException {
        return create(new DataInputStream(inputStream).readLong(), klass);
    }

    /**
     * Converts an ID object of one type to another.
     *
     * @param id the ID object to be converted
     * @param klass the type to convert to
     * @return a newly created ID object of the new type and the same value as the given ID object
     */
    public static <ID_Type extends ID> ID_Type convert(ID id, Class<ID_Type> klass) {
        return create(id.value(), klass);
    }

    public static class ObjectID extends ID {

        public static final ObjectID NULL = new ObjectID(0);

        public ObjectID(long v) {
            super(v);
        }
    }

    public static class ArrayID extends ObjectID {

        public ArrayID(long v) {
            super(v);
        }
    }

    public static class ClassLoaderID extends ObjectID {

        public ClassLoaderID(long v) {
            super(v);
        }
    }

    public static class ReferenceTypeID extends ObjectID {

        public ReferenceTypeID(long v) {
            super(v);
        }
    }

    public static class ClassID extends ReferenceTypeID {

        public ClassID(long v) {
            super(v);
        }
    }

    public static class ClassObjectID extends ObjectID {

        public ClassObjectID(long v) {
            super(v);
        }
    }

    public static class InterfaceID extends ReferenceTypeID {

        public InterfaceID(long v) {
            super(v);
        }
    }

    public static class ArrayTypeID extends ReferenceTypeID {

        public ArrayTypeID(long v) {
            super(v);
        }
    }

    public static class FieldID extends ObjectID {

        public FieldID(long v) {
            super(v);
        }
    }

    public static class FrameID extends ID {

        public FrameID(long v) {
            super(v);
        }
    }

    public static class MethodID extends ObjectID {

        public MethodID(long v) {
            super(v);
        }
    }

    public static class StringID extends ObjectID {

        public StringID(long v) {
            super(v);
        }
    }

    public static class ThreadID extends ObjectID {

        public ThreadID(long v) {
            super(v);
        }
    }

    public static class ThreadGroupID extends ObjectID {

        public ThreadGroupID(long v) {
            super(v);
        }
    }
}
