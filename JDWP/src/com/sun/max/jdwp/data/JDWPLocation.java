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

package com.sun.max.jdwp.data;

import java.io.*;

/**
 * This class represents a code location in terms of JDWP semantics.
 *
 * @author Thomas Wuerthinger
 */
public class JDWPLocation {

    private byte type;
    private ID.ClassID clazz;
    private ID.MethodID method;
    private long index;

    public JDWPLocation(InputStream is) throws IOException {
        final DataInputStream dis = new DataInputStream(is);
        this.type = dis.readByte();
        this.clazz = ID.read(is, ID.ClassID.class);
        this.method = ID.read(is, ID.MethodID.class);
        this.index = dis.readLong();
    }

    public JDWPLocation(byte type, ID.ClassID klass, ID.MethodID method, long index) {
        this.type = type;
        this.clazz = klass;
        this.method = method;
        this.index = index;
    }

    public void write(OutputStream os) throws IOException {
        final DataOutputStream dos = new DataOutputStream(os);
        dos.writeByte(type);
        clazz.write(os);
        method.write(os);
        dos.writeLong(index);
    }

    public byte getType() {
        return type;
    }

    public ID.ClassID getClassID() {
        return clazz;
    }

    public ID.MethodID getMethodID() {
        return method;
    }

    public long getIndex() {
        return index;
    }

    @Override
    public int hashCode() {
        final int result = type + (int) index << 8;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JDWPLocation) {
            final JDWPLocation other = (JDWPLocation) obj;
            return other.type == type && other.index == index && other.clazz.equals(other.clazz) && other.method.equals(method);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Location[");
        sb.append("type=" + getType());
        sb.append(", class=" + getClassID());
        sb.append(", method=" + getMethodID());
        sb.append(", index=" + getIndex());
        sb.append("]");
        return sb.toString();

    }
}
