/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This class represents a code location in terms of JDWP semantics.
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
