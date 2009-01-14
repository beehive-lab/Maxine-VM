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

    private byte _tag;
    private Object _value;

    public JDWPValue(InputStream is) throws IOException {
        _tag = (byte) is.read();
        readValue(is);
    }

    public JDWPValue(InputStream is, int typeTag) throws IOException {
        _tag = (byte) typeTag;
        readValue(is);
    }

    public JDWPValue(ID.ArrayID v) {
        _value = v;
        _tag = Tag.ARRAY;
    }

    public JDWPValue(Byte v) {
        _value = v;
        _tag = Tag.BYTE;
    }

    public JDWPValue(Character v) {
        _value = v;
        _tag = Tag.CHAR;
    }

    public JDWPValue(ID.ObjectID v) {
        _value = v;
        _tag = Tag.OBJECT;
    }

    public JDWPValue(Float v) {
        _value = v;
        _tag = Tag.FLOAT;
    }

    public JDWPValue(Double v) {
        _value = v;
        _tag = Tag.DOUBLE;
    }

    public JDWPValue(Integer v) {
        _value = v;
        _tag = Tag.INT;
    }

    public JDWPValue(Long v) {
        _value = v;
        _tag = Tag.LONG;
    }

    public JDWPValue(Short v) {
        _value = v;
        _tag = Tag.SHORT;
    }

    public JDWPValue() {
        _tag = Tag.VOID;
    }

    public JDWPValue(Boolean v) {
        _value = v;
        _tag = Tag.BOOLEAN;
    }

    public JDWPValue(ID.StringID v) {
        _value = v;
        _tag = Tag.STRING;
    }

    public JDWPValue(ID.ThreadID v) {
        _value = v;
        _tag = Tag.THREAD;
    }

    public JDWPValue(ID.ThreadGroupID v) {
        _value = v;
        _tag = Tag.THREAD_GROUP;
    }

    public JDWPValue(ID.ClassLoaderID v) {
        _value = v;
        _tag = Tag.CLASS_LOADER;
    }

    public JDWPValue(ID.ClassObjectID v) {
        _value = v;
        _tag = Tag.CLASS_OBJECT;
    }

    public byte tag() {
        return _tag;
    }

    public ID.ArrayID asArray() {
        return (_tag == Tag.ARRAY) ? (ID.ArrayID) _value : null;
    }

    public Byte asByte() {
        return (_tag == Tag.BYTE) ? (Byte) _value : null;
    }

    public Character asCharacter() {
        return (_tag == Tag.CHAR) ? (Character) _value : null;
    }

    public ID.ObjectID asObject() {
        return (_tag == Tag.OBJECT) ? (ID.ObjectID) _value : null;
    }

    public Float asFloat() {
        return (_tag == Tag.FLOAT) ? (Float) _value : null;
    }

    public Double asDouble() {
        return (_tag == Tag.DOUBLE) ? (Double) _value : null;
    }

    public Integer asInteger() {
        return (_tag == Tag.INT) ? (Integer) _value : null;
    }

    public Long asLong() {
        return (_tag == Tag.LONG) ? (Long) _value : null;
    }

    public Short asShort() {
        return (_tag == Tag.SHORT) ? (Short) _value : null;
    }

    public boolean isVoid() {
        return _tag == Tag.VOID;
    }

    public Boolean asBoolean() {
        return (_tag == Tag.BOOLEAN) ? (Boolean) _value : null;
    }

    public ID.StringID asString() {
        return (_tag == Tag.STRING) ? (ID.StringID) _value : null;
    }

    public ID.ThreadID asThread() {
        return (_tag == Tag.THREAD) ? (ID.ThreadID) _value : null;
    }

    public ID.ThreadGroupID asThreadGroup() {
        return (_tag == Tag.THREAD_GROUP) ? (ID.ThreadGroupID) _value : null;
    }

    public ID.ClassLoaderID asClassLoader() {
        return (_tag == Tag.CLASS_LOADER) ? (ID.ClassLoaderID) _value : null;
    }

    public ID.ClassObjectID asClassObject() {
        return (_tag == Tag.CLASS_OBJECT) ? (ID.ClassObjectID) _value : null;
    }

    /**
     * Writes this value untagged onto an output stream.
     * @param outputStream the stream to write the data on
     * @throws IOException this exception is thrown, when a problem occurred while writing the raw bytes
     */
    public void writeUntagged(OutputStream outputStream) throws IOException {
        final DataOutputStream dos = new DataOutputStream(outputStream);
        switch (_tag) {
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
        switch (_tag) {
            case Tag.ARRAY:
                _value = ID.read(is, ID.ArrayID.class);
                break;
            case Tag.BYTE:
                _value = new Byte(new DataInputStream(is).readByte());
                break;
            case Tag.CHAR:
                _value = new Character(new DataInputStream(is).readChar());
                break;
            case Tag.OBJECT:
                _value = ID.read(is, ID.ObjectID.class);
                break;
            case Tag.FLOAT:
                _value = new Float(new DataInputStream(is).readFloat());
                break;
            case Tag.DOUBLE:
                _value = new Double(new DataInputStream(is).readDouble());
                break;
            case Tag.INT:
                _value = new Integer(new DataInputStream(is).readInt());
                break;
            case Tag.LONG:
                _value = new Long(new DataInputStream(is).readLong());
                break;
            case Tag.SHORT:
                _value = new Short(new DataInputStream(is).readShort());
                break;
            case Tag.VOID:
                break;
            case Tag.BOOLEAN:
                _value = new Boolean(new DataInputStream(is).readBoolean());
                break;
            case Tag.STRING:
                _value = ID.read(is, ID.StringID.class);
                break;
            case Tag.THREAD:
                _value = ID.read(is, ID.ThreadID.class);
                break;
            case Tag.THREAD_GROUP:
                _value = ID.read(is, ID.ThreadGroupID.class);
                break;
            case Tag.CLASS_LOADER:
                _value = ID.read(is, ID.ClassLoaderID.class);
                break;
            case Tag.CLASS_OBJECT:
                _value = ID.read(is, ID.ClassObjectID.class);
                break;
            default:
                assert false : "Unhandled Tag constant " + _tag + "!";

        }
    }

    /**
     * Returns an object identifier, if this value represents any subclass of {@link ID.ObjectID}.
     * @return the object identifier or null, if the value cannot be converted to it
     */
    public ID.ObjectID asGeneralObjectID() {
        return (_value instanceof ObjectID) ? (ID.ObjectID) _value : null;
    }

    @Override
    public String toString() {
        return "JDWPValue(" + _tag + ")[" + _value + "]";
    }
}
