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
import java.nio.charset.*;
import java.util.*;

import com.sun.max.jdwp.constants.*;

/**
 * Stream for writing JDWP values. The bytes are written to an underlying stream object.
 *
 * @author Thomas Wuerthinger
 */
public class JDWPOutputStream {

    private DataOutputStream out;

    public JDWPOutputStream(OutputStream out) {
        this.out = new DataOutputStream(out);
    }

    public void write(boolean b) throws IOException {
        out.writeBoolean(b);
    }

    public void write(byte b) throws IOException {
        out.writeByte(b);
    }

    public void write(int i) throws IOException {
        out.writeInt(i);
    }

    public void write(long l) throws IOException {
        out.writeLong(l);
    }

    /**
     * This method retrieves the stream object, that is used to write the actual bytes.
     * @return the underlying stream object
     */
    public OutputStream getOutputStream() {
        return out;
    }

    public void write(String s) throws IOException {
        final byte[] bytes = s.getBytes(Charset.forName("UTF-8"));
        write(bytes.length);
        out.write(bytes);
    }

    public void write(JDWPLocation l) throws IOException {
        l.write(out);
    }

    public void write(JDWPValue v) throws IOException {
        v.write(out);
    }

    public void writeUntagged(JDWPValue v) throws IOException {
        v.writeUntagged(out);
    }

    /**
     * Writes a list of JDWP values to the stream. There is an analysis to know, whether writing tags is necessary or not.
     * @param list the list of values
     * @throws IOException this exception is thrown, when there was a problem writing the array
     */
    public void write(List<? extends JDWPValue> list) throws IOException {

        // There has to be at least one element in the array.
        if (list.size() == 0) {
            throw new IOException("Cannot write an empty array of JDWPValue objects!");
        }

        assert list.size() > 0;

        boolean hasObjects = false;
        boolean hasPrimitives = false;
        byte tag = -1;
        for (JDWPValue v : list) {
            if (v.asGeneralObjectID() != null) {
                hasObjects = true;
            } else {
                hasPrimitives = true;
            }

            if (tag == -1) {
                // First value => save its tag.
                tag = v.tag();
            } else {
                // Tags do not match, the only valid possibility is an object array containing objects of different classes, so set the tag to object.
                if (tag != v.tag()) {
                    tag = Tag.OBJECT;
                }
            }
        }

        if (hasObjects && hasPrimitives) {
            throw new IOException("Array cannot contain both object and primitive type objects! Array is: " + list);
        }

        // Tag was set to object despite the elements were primitives => we have different primitive types in the array.
        if (tag == Tag.OBJECT && hasPrimitives) {
            throw new IOException("If array has primtive type objects, they must be all of the same type! Array is: " + list);
        }

        out.writeByte(tag);
        out.writeInt(list.size());

        // There can only be either objects or primitives in the array.
        assert !(hasObjects && hasPrimitives);

        for (JDWPValue v : list) {
            if (hasObjects) {
                assert !hasPrimitives;
                write(v);

            } else {
                assert hasPrimitives;
                writeUntagged(v);
            }
        }
    }

    /**
     * Writes the given object identifier as a tagged value to the output stream. This method is currently not implemented, because it is not needed for
     * handling the current set of commands.
     * @param objectID the object identifier that should be serialized
     * @throws IOException this exception is thrown, when there was a problem when writing the object identifier
     */
    public void writeTagged(ID.ObjectID objectID) throws IOException {
        throw new IOException("Tried to write a tagged object ID. This operation is not supported!");
    }
}
