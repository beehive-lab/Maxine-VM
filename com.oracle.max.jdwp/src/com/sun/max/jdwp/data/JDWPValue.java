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

import com.sun.max.jdwp.constants.*;
import com.sun.max.jdwp.data.ID.*;

/**
 * Class representing a JDWPValue. Instances of this class can represent any possible JDWP value (see {@link com.sun.max.jdwp.constants.Tag}).
 * For simplicity, this class is implemented as a union. The methods starting with "as" return the specific representation of the object or null, if
 * the object is of a different representation.
 *
 * @author Thomas Wuerthinger
 */
public class JDWPValue {

    private byte tag;
    private Object value;

    public JDWPValue(InputStream is) throws IOException {
        tag = (byte) is.read();
        readValue(is);
    }

    public JDWPValue(InputStream is, int typeTag) throws IOException {
        tag = (byte) typeTag;
        readValue(is);
    }

    public JDWPValue(ID.ArrayID v) {
        value = v;
        tag = Tag.ARRAY;
    }

    public JDWPValue(Byte v) {
        value = v;
        tag = Tag.BYTE;
    }

    public JDWPValue(Character v) {
        value = v;
        tag = Tag.CHAR;
    }

    public JDWPValue(ID.ObjectID v) {
        value = v;
        tag = Tag.OBJECT;
    }

    public JDWPValue(Float v) {
        value = v;
        tag = Tag.FLOAT;
    }

    public JDWPValue(Double v) {
        value = v;
        tag = Tag.DOUBLE;
    }

    public JDWPValue(Integer v) {
        value = v;
        tag = Tag.INT;
    }

    public JDWPValue(Long v) {
        value = v;
        tag = Tag.LONG;
    }

    public JDWPValue(Short v) {
        value = v;
        tag = Tag.SHORT;
    }

    public JDWPValue() {
        tag = Tag.VOID;
    }

    public JDWPValue(Boolean v) {
        value = v;
        tag = Tag.BOOLEAN;
    }

    public JDWPValue(ID.StringID v) {
        value = v;
        tag = Tag.STRING;
    }

    public JDWPValue(ID.ThreadID v) {
        value = v;
        tag = Tag.THREAD;
    }

    public JDWPValue(ID.ThreadGroupID v) {
        value = v;
        tag = Tag.THREAD_GROUP;
    }

    public JDWPValue(ID.ClassLoaderID v) {
        value = v;
        tag = Tag.CLASS_LOADER;
    }

    public JDWPValue(ID.ClassObjectID v) {
        value = v;
        tag = Tag.CLASS_OBJECT;
    }

    public byte tag() {
        return tag;
    }

    public ID.ArrayID asArray() {
        return (tag == Tag.ARRAY) ? (ID.ArrayID) value : null;
    }

    public Byte asByte() {
        return (tag == Tag.BYTE) ? (Byte) value : null;
    }

    public Character asCharacter() {
        return (tag == Tag.CHAR) ? (Character) value : null;
    }

    public ID.ObjectID asObject() {
        return (tag == Tag.OBJECT) ? (ID.ObjectID) value : null;
    }

    public Float asFloat() {
        return (tag == Tag.FLOAT) ? (Float) value : null;
    }

    public Double asDouble() {
        return (tag == Tag.DOUBLE) ? (Double) value : null;
    }

    public Integer asInteger() {
        return (tag == Tag.INT) ? (Integer) value : null;
    }

    public Long asLong() {
        return (tag == Tag.LONG) ? (Long) value : null;
    }

    public Short asShort() {
        return (tag == Tag.SHORT) ? (Short) value : null;
    }

    public boolean isVoid() {
        return tag == Tag.VOID;
    }

    public Boolean asBoolean() {
        return (tag == Tag.BOOLEAN) ? (Boolean) value : null;
    }

    public ID.StringID asString() {
        return (tag == Tag.STRING) ? (ID.StringID) value : null;
    }

    public ID.ThreadID asThread() {
        return (tag == Tag.THREAD) ? (ID.ThreadID) value : null;
    }

    public ID.ThreadGroupID asThreadGroup() {
        return (tag == Tag.THREAD_GROUP) ? (ID.ThreadGroupID) value : null;
    }

    public ID.ClassLoaderID asClassLoader() {
        return (tag == Tag.CLASS_LOADER) ? (ID.ClassLoaderID) value : null;
    }

    public ID.ClassObjectID asClassObject() {
        return (tag == Tag.CLASS_OBJECT) ? (ID.ClassObjectID) value : null;
    }

    /**
     * Writes this value untagged onto an output stream.
     * @param outputStream the stream to write the data on
     * @throws IOException this exception is thrown, when a problem occurred while writing the raw bytes
     */
    public void writeUntagged(OutputStream outputStream) throws IOException {
        final DataOutputStream dos = new DataOutputStream(outputStream);
        switch (tag) {
            case Tag.ARRAY:
                asArray().write(dos);
                break;
            case Tag.BYTE:
                dos.writeByte(asByte().byteValue());
                break;
            case Tag.CHAR:
                dos.writeChar(asCharacter().charValue());
                break;
            case Tag.OBJECT:
                asObject().write(dos);
                break;
            case Tag.FLOAT:
                dos.writeFloat(asFloat().floatValue());
                break;
            case Tag.DOUBLE:
                dos.writeDouble(asDouble().doubleValue());
                break;
            case Tag.INT:
                dos.writeInt(asInteger().intValue());
                break;
            case Tag.LONG:
                dos.writeLong(asLong().longValue());
                break;
            case Tag.SHORT:
                dos.writeShort(asShort().shortValue());
                break;
            case Tag.VOID:
                break;
            case Tag.BOOLEAN:
                dos.writeBoolean(asBoolean().booleanValue());
                break;
            case Tag.STRING:
                asString().write(dos);
                break;
            case Tag.THREAD:
                asThread().write(dos);
                break;
            case Tag.THREAD_GROUP:
                asThreadGroup().write(dos);
                break;
            case Tag.CLASS_LOADER:
                asClassLoader().write(dos);
                break;
            case Tag.CLASS_OBJECT:
                asClassObject().write(dos);
                break;
            default:
                assert false : "Unhandled Tag constant!";
        }
    }

    /**
     * Writes this value as a tagged value to the output stream.
     * @param outputStream stream to write the value on
     * @throws IOException this exception is thrown, when there is a problem writing the raw bytes
     */
    public void write(OutputStream outputStream) throws IOException {
        final DataOutputStream dos = new DataOutputStream(outputStream);
        dos.writeByte(tag());
        writeUntagged(outputStream);
    }

    private void readValue(InputStream is) throws IOException {
        switch (tag) {
            case Tag.ARRAY:
                value = ID.read(is, ID.ArrayID.class);
                break;
            case Tag.BYTE:
                value = new Byte(new DataInputStream(is).readByte());
                break;
            case Tag.CHAR:
                value = new Character(new DataInputStream(is).readChar());
                break;
            case Tag.OBJECT:
                value = ID.read(is, ID.ObjectID.class);
                break;
            case Tag.FLOAT:
                value = new Float(new DataInputStream(is).readFloat());
                break;
            case Tag.DOUBLE:
                value = new Double(new DataInputStream(is).readDouble());
                break;
            case Tag.INT:
                value = new Integer(new DataInputStream(is).readInt());
                break;
            case Tag.LONG:
                value = new Long(new DataInputStream(is).readLong());
                break;
            case Tag.SHORT:
                value = new Short(new DataInputStream(is).readShort());
                break;
            case Tag.VOID:
                break;
            case Tag.BOOLEAN:
                value = new Boolean(new DataInputStream(is).readBoolean());
                break;
            case Tag.STRING:
                value = ID.read(is, ID.StringID.class);
                break;
            case Tag.THREAD:
                value = ID.read(is, ID.ThreadID.class);
                break;
            case Tag.THREAD_GROUP:
                value = ID.read(is, ID.ThreadGroupID.class);
                break;
            case Tag.CLASS_LOADER:
                value = ID.read(is, ID.ClassLoaderID.class);
                break;
            case Tag.CLASS_OBJECT:
                value = ID.read(is, ID.ClassObjectID.class);
                break;
            default:
                assert false : "Unhandled Tag constant " + tag + "!";

        }
    }

    /**
     * Returns an object identifier, if this value represents any subclass of {@link ID.ObjectID}.
     * @return the object identifier or null, if the value cannot be converted to it
     */
    public ID.ObjectID asGeneralObjectID() {
        return (value instanceof ObjectID) ? (ID.ObjectID) value : null;
    }

    @Override
    public String toString() {
        return "JDWPValue(" + tag + ")[" + value + "]";
    }
}
