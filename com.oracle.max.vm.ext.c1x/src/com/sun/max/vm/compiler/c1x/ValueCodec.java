/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.c1x.Package.C1XObjectMapContributor;
import com.sun.max.vm.runtime.*;

/**
 * Utility for encoding/decoding {@link CiValue}s to/from encoding/decoding streams.
 */
public class ValueCodec {

    final static class UnsignedBitField {
        /**
         * Number of bits in value.
         */
        final int width;

        /**
         * Bit position of lowest bit in field.
         */
        final int shift;

        /**
         * Mask applied to (unshifted) value to obtain field.
         */
        final int mask;

        int max() {
            return mask;
        }

        UnsignedBitField(int width, int shift) {
            assert width < 32;
            this.width = width;
            this.shift = shift;
            this.mask = (1 << width) - 1;
        }

        /**
         * Extracts the value of this bit field from {@code word}.
         */
        int get(int word) {
            return (word >>> shift) & mask;
        }

        /**
         * Sets the value of this bit field in {@code word}.
         *
         * @return the value of {@code word} modified such that {@code this.get(word) == value}
         */
        int set(int word, int value) {
            assert (value & mask) == value : "value is out of range";
            word &= ~(mask << shift); // clear current value
            word |= value << shift; // set new value
            assert get(word) == value;
            return word;
        }
    }

    /**
     * Bit field in first byte of an encoded {@link CiValue} specifying the type of the value.
     */
    final static UnsignedBitField TYPE = new UnsignedBitField(2, 6);

    /**
     * Value in {@link #TYPE} bit field denoting a {@link CiRegisterValue}.
     */
    final static int TYPE_REGISTER = 0;

    /**
     * Value in {@link #TYPE} bit field denoting a {@link CiStackSlot}.
     */
    final static int TYPE_STACK    = 1;

    /**
     * Value in {@link #TYPE} bit field denoting an object {@link CiConstant}.
     */
    final static int TYPE_OBJECT_CONSTANT = 2;

    /**
     * Value in {@link #TYPE} bit field denoting a non-object {@link CiConstant} or {@link CiValue#IllegalValue}.
     */
    final static int TYPE_NONOBJECT_CONSTANT = 3;

    /**
     * Bit field in first byte of an encoded {@link CiStackSlot} specifying the stack slot index and frame category (current frame of caller frame).
     */
    final static UnsignedBitField STACK = new UnsignedBitField(6, 0);

    /**
     * Value in {@link #STACK} bit field specifying that the 4 following bytes encode the index of a slot in the current frame.
     */
    final static int STACK_5_CURRENT_FRAME = STACK.max();

    /**
     * Value in {@link #STACK} bit field specifying that the 4 following bytes encode the index of a slot in the caller frame
     * (e.g. a stack slot for an incoming parameter).
     */
    final static int STACK_5_CALLER_FRAME = STACK_5_CURRENT_FRAME - 1;

    /**
     * Maximum value in the {@link #STACK} bit field encoding a slot index and frame category.
     */
    final static int STACK_1_MAX = STACK_5_CALLER_FRAME - 1;

    /**
     * Maximum number of caller frame slots that can be encoded in one byte.
     */
    final static int STACK_1_CALLER_FRAME_MAX = 8;

    /**
     * Maximum number of current frame slots that can be encoded in one byte.
     */
    final static int STACK_1_CURRENT_FRAME_MAX = STACK_1_MAX - STACK_1_CALLER_FRAME_MAX - 1;

    /**
     * Bit field in first byte of an encoded {@link CiRegisterValue} specifying the {@link CiRegister#number register number}.
     */
    final static UnsignedBitField REGISTER = new UnsignedBitField(6, 0);

    /**
     * Bit field in first byte of an encoded object {@link CiConstant} specifying the value.
     */
    final static UnsignedBitField OBJECT_CONSTANT = new UnsignedBitField(6, 0);

    /**
     * Bit field in first byte of an encoded non-object {@link CiConstant} specifying the value.
     */
    final static UnsignedBitField NONOBJECT_CONSTANT = new UnsignedBitField(6, 0);

    /**
     * Maximum value in the {@link #NONOBJECT_CONSTANT} bit field encoding an index in {@link #nonObjectConstantsByID}.
     * The complete range of indexes encodable in {@link #NONOBJECT_CONSTANT} is {@code [0 .. NONOBJECT_CONSTANT_SHARED_INDEX_MAX]}.
     * The range of values between {@code [NONOBJECT_CONSTANT_SHARED_INDEX_MAX + 1 .. NONOBJECT_CONSTANT.max]} encode the
     * ordinal of the value's kind and the following bytes encode the value itself.
     */
    final static int NONOBJECT_CONSTANT_SHARED_INDEX_MAX = OBJECT_CONSTANT.max() - CiKind.VALUES.length;

    /**
     * Constant pool of object {@link CiConstant} values.
     * This pool is re-initialized lazily at runtime to account for {@link System#identityHashCode(Object)}
     * giving different values at boot image time.
     *
     * @see C1XObjectMapContributor
     */
    static final IdentityHashMap<Object, Integer> objectConstants = new IdentityHashMap<Object, Integer>();

    /**
     * This is used to decode an index back to an object constant in {@link #objectConstants}.
     */
    static final LinearIDMap<CiConstant> objectConstantsByID = new LinearIDMap<CiConstant>(300);

    /**
     * Constant pool of shared non-object {@link CiConstant} values.
     *
     * The special entry with a key of {@code null} denotes {@link CiValue#IllegalValue}.
     */
    static final HashMap<CiConstant, Integer> nonObjectConstants = new HashMap<CiConstant, Integer>();

    /**
     * This is used to decode an index back to a non-object constant in {@link #nonObjectConstants}.
     */
    static final LinearIDMap<CiConstant> nonObjectConstantsByID = new LinearIDMap<CiConstant>(20);

    static {
        nonObjectConstants.put(null, 0);

        for (Field field : CiConstant.class.getFields()) {
            if (field.getType() == CiConstant.class) {
                try {
                    field.setAccessible(true);
                    CiConstant c = (CiConstant) field.get(null);
                    if (c.kind.isObject()) {
                        Object obj = c.asObject();
                        if (!objectConstants.containsKey(obj)) {
                            int index = objectConstants.size();
                            objectConstants.put(obj, index);
                            objectConstantsByID.set(index, c);
                        }
                    } else {
                        if (!nonObjectConstants.containsKey(c)) {
                            int index = nonObjectConstants.size();
                            nonObjectConstants.put(c, index);
                            nonObjectConstantsByID.set(index, c);
                        }
                    }
                } catch (Exception e) {
                    throw FatalError.unexpected("Error reading value of " + field, e);
                }
            }
        }
    }

    static long valuesEncoded;
    static long valuesEncodedSize;

    /**
     * Encodes a {@link CiValue} to a data output stream.
     */
    static void writeValue(EncodingStream out, CiValue value) {
        int pos = out.pos;
        if (value.isIllegal()) {
            out.write(TYPE.set(0, TYPE_NONOBJECT_CONSTANT));
        } else if (value.isRegister()) {
            int b = TYPE.set(0, TYPE_REGISTER);
            b = REGISTER.set(b, value.asRegister().number);
            out.write(b);
        } else if (value.isStackSlot()) {
            CiStackSlot slot = (CiStackSlot) value;
            int index = slot.index();
            if (slot.inCallerFrame()) {
                if (index <= STACK_1_CALLER_FRAME_MAX) {
                    out.write(STACK.set(TYPE.set(0, TYPE_STACK), index + STACK_1_CURRENT_FRAME_MAX + 1));
                } else {
                    out.write(STACK.set(TYPE.set(0, TYPE_STACK), STACK_5_CALLER_FRAME));
                    out.writeInt(index);
                }
            } else {
                if (index <= STACK_1_CURRENT_FRAME_MAX) {
                    out.write(STACK.set(TYPE.set(0, TYPE_STACK), index));
                } else {
                    out.write(STACK.set(TYPE.set(0, TYPE_STACK), STACK_5_CURRENT_FRAME));
                    out.writeInt(index);
                }
            }
        } else {
            assert value.isConstant() : "cannot encode " + value;
            CiConstant c = (CiConstant) value;
            if (c.kind.isObject()) {
                int index;
                Object obj = c.asObject();
                synchronized (objectConstants) {
                    if (!isHosted() && objectConstants.isEmpty()) {
                        // re-initialize the map
                        for (int id = 0; id <= objectConstantsByID.maxID(); id++) {
                            CiConstant e = objectConstantsByID.get(id);
                            if (e != null) {
                                objectConstants.put(e.asObject(), id);
                            }
                        }
                    }
                    Integer key = objectConstants.get(obj);
                    if (key == null) {
                        index = objectConstants.size();
                        objectConstants.put(obj, index);
                        objectConstantsByID.set(index, c);
                    } else {
                        index = key;
                    }
                }
                if (index < OBJECT_CONSTANT.max()) {
                    out.write(OBJECT_CONSTANT.set(TYPE.set(0, TYPE_OBJECT_CONSTANT), index));
                } else {
                    out.write(OBJECT_CONSTANT.set(TYPE.set(0, TYPE_OBJECT_CONSTANT), OBJECT_CONSTANT.max()));
                    out.encodeUInt(index);
                }
            } else {
                Integer index = nonObjectConstants.get(c);
                if (index != null) {
                    assert index > 0 && index <= NONOBJECT_CONSTANT_SHARED_INDEX_MAX;
                    out.write(NONOBJECT_CONSTANT.set(TYPE.set(0, TYPE_NONOBJECT_CONSTANT), index));
                } else {
                    out.write(NONOBJECT_CONSTANT.set(TYPE.set(0, TYPE_NONOBJECT_CONSTANT), c.kind.ordinal() + NONOBJECT_CONSTANT_SHARED_INDEX_MAX + 1));
                    // Checkstyle: stop
                    switch (c.kind) {
                        case Byte    : out.writeByte(c.asInt()); break;
                        case Boolean : out.writeBoolean(c.asBoolean()); break;
                        case Char    : out.writeChar(c.asInt()); break;
                        case Short   : out.writeShort(c.asInt()); break;
                        case Float   : out.writeFloat(c.asFloat()); break;
                        case Long    : out.writeLong(c.asLong()); break;
                        case Double  : out.writeDouble(c.asDouble()); break;
                        case Jsr     : out.writeInt(c.asInt()); break;
                        case Word    : out.writeLong(c.asLong()); break;
                        case Int: {
                            int b0 = c.asInt();
                            if (b0 >= 0 && b0 < 0xff) {
                                out.write(b0);
                            } else {
                                out.write(0xff);
                                out.writeInt(c.asInt());
                            }
                            break;
                        }
                    }
                    // Checkstyle: resume
                }
            }
        }
        valuesEncoded++;
        valuesEncodedSize += out.pos - pos;
    }

    /**
     * Decodes a {@link CiValue} from a data input stream.
     */
    static CiValue readValue(DecodingStream in) {
        int b = in.read();
        assert b >= 0;
        int type = TYPE.get(b);
        if (type == TYPE_STACK) {
            int index = STACK.get(b);
            if (index <= STACK_1_CURRENT_FRAME_MAX) {
                return CiStackSlot.get(CiKind.Illegal, index, false);
            } else if (index <= STACK_1_MAX) {
                return CiStackSlot.get(CiKind.Illegal, index - STACK_1_CURRENT_FRAME_MAX - 1, true);
            } else  if (index == STACK_5_CALLER_FRAME) {
                index = in.readInt();
                return CiStackSlot.get(CiKind.Illegal, index, true);
            } else {
                assert index == STACK_5_CURRENT_FRAME;
                index = in.readInt();
                return CiStackSlot.get(CiKind.Illegal, index, false);
            }
        } else if (type == TYPE_REGISTER) {
            int num = REGISTER.get(b);
            return target().arch.registers[num].asValue();
        } else if (type == TYPE_OBJECT_CONSTANT) {
            int index = OBJECT_CONSTANT.get(b);
            if (index < OBJECT_CONSTANT.max()) {
                return objectConstantsByID.get(index);
            }
            index = in.decodeUInt();
            return objectConstantsByID.get(index);
        } else {
            assert type == TYPE_NONOBJECT_CONSTANT;
            int index = NONOBJECT_CONSTANT.get(b);
            if (index == 0) {
                return CiValue.IllegalValue;
            } else if (index <= NONOBJECT_CONSTANT_SHARED_INDEX_MAX) {
                return nonObjectConstantsByID.get(index);
            }
            int kindOrdinal = index - NONOBJECT_CONSTANT_SHARED_INDEX_MAX - 1;
            CiKind kind = CiKind.VALUES[kindOrdinal];
            // Checkstyle: stop
            switch (kind) {
                case Byte    : return CiConstant.forByte(in.readByte());
                case Boolean : return CiConstant.forBoolean(in.readBoolean());
                case Char    : return CiConstant.forChar(in.readChar());
                case Short   : return CiConstant.forShort(in.readShort());
                case Float   : return CiConstant.forFloat(in.readFloat());
                case Long    : return CiConstant.forLong(in.readLong());
                case Double  : return CiConstant.forDouble(in.readDouble());
                case Jsr     : return CiConstant.forJsr(in.readInt());
                case Word    : return CiConstant.forWord(in.readLong());
                case Int: {
                    int b0 = in.readByte() & 0xFF;
                    if (b0 == 0xFF) {
                        return CiConstant.forInt(in.readInt());
                    } else {
                        return CiConstant.forInt(b0);
                    }
                }
            }
            // Checkstyle: resume
            throw FatalError.unexpected("Unexpected kind: " + kind);
        }
    }

    /**
     * Tests codec by encoding and decoding a given value.
     *
     * @param value the value to pass through the codec
     */
    @HOSTED_ONLY
    static CiValue testCodec(CiValue value) {
        EncodingStream es = new EncodingStream(1024);
        writeValue(es, value);
        return readValue(new DecodingStream(es.toByteArray()));
    }
}
