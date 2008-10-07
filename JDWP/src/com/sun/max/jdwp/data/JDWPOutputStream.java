/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
/*VCSID=7ac1642e-b8e1-40cb-9415-5137897065e4*/
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

    private DataOutputStream _out;

    public JDWPOutputStream(OutputStream out) {
        _out = new DataOutputStream(out);
    }

    public void write(boolean b) throws IOException {
        _out.writeBoolean(b);
    }

    public void write(byte b) throws IOException {
        _out.writeByte(b);
    }

    public void write(int i) throws IOException {
        _out.writeInt(i);
    }

    public void write(long l) throws IOException {
        _out.writeLong(l);
    }

    /**
     * This method retrieves the stream object, that is used to write the actual bytes.
     * @return the underlying stream object
     */
    public OutputStream getOutputStream() {
        return _out;
    }

    public void write(String s) throws IOException {
        final byte[] bytes = s.getBytes(Charset.forName("UTF-8"));
        write(bytes.length);
        _out.write(bytes);
    }

    public void write(JDWPLocation l) throws IOException {
        l.write(_out);
    }

    public void write(JDWPValue v) throws IOException {
        v.write(_out);
    }

    public void writeUntagged(JDWPValue v) throws IOException {
        v.writeUntagged(_out);
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

        _out.writeByte(tag);
        _out.writeInt(list.size());

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
