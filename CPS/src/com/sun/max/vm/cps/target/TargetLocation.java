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
package com.sun.max.vm.cps.target;

import static com.sun.cri.ci.CiRegister.RegisterFlag.*;
import static com.sun.max.platform.Platform.*;

import java.io.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Base class in a hierarchy of types that encode data locations that can be addressed by target code.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public abstract class TargetLocation {

    public enum Tag {
        UNDEFINED,
        IMMEDIATE(-2, 9),
        IMMEDIATE_M2,
        IMMEDIATE_M1,
        IMMEDIATE_0,
        IMMEDIATE_1,
        IMMEDIATE_2,
        IMMEDIATE_3,
        IMMEDIATE_4,
        IMMEDIATE_5,
        IMMEDIATE_6,
        IMMEDIATE_7,
        IMMEDIATE_8,
        IMMEDIATE_9,
        SCALAR_LITERAL_0,
        SCALAR_LITERAL_1,
        SCALAR_LITERAL_2,
        SCALAR_LITERAL_3,
        SCALAR_LITERAL_4,
        SCALAR_LITERAL_5,
        SCALAR_LITERAL_6,
        SCALAR_LITERAL_7,
        SCALAR_LITERAL_8,
        SCALAR_LITERAL_9,
        SCALAR_LITERAL(0, 9) {
            @Override
            public Tag wideOperandTag() {
                return SCALAR_LITERAL_WIDE;
            }
        },
        SCALAR_LITERAL_WIDE,
        REFERENCE_LITERAL_0,
        REFERENCE_LITERAL_1,
        REFERENCE_LITERAL_2,
        REFERENCE_LITERAL_3,
        REFERENCE_LITERAL_4,
        REFERENCE_LITERAL_5,
        REFERENCE_LITERAL_6,
        REFERENCE_LITERAL_7,
        REFERENCE_LITERAL_8,
        REFERENCE_LITERAL_9,
        REFERENCE_LITERAL(0, 9) {
            @Override
            public Tag wideOperandTag() {
                return REFERENCE_LITERAL_WIDE;
            }
        },
        REFERENCE_LITERAL_WIDE,
        INTEGER_REGISTER_0,
        INTEGER_REGISTER_1,
        INTEGER_REGISTER_2,
        INTEGER_REGISTER_3,
        INTEGER_REGISTER_4,
        INTEGER_REGISTER_5,
        INTEGER_REGISTER_6,
        INTEGER_REGISTER_7,
        INTEGER_REGISTER_8,
        INTEGER_REGISTER_9,
        INTEGER_REGISTER_10,
        INTEGER_REGISTER_11,
        INTEGER_REGISTER_12,
        INTEGER_REGISTER_13,
        INTEGER_REGISTER_14,
        INTEGER_REGISTER_15,
        INTEGER_REGISTER_16,
        INTEGER_REGISTER_17,
        INTEGER_REGISTER_18,
        INTEGER_REGISTER_19,
        INTEGER_REGISTER_20,
        INTEGER_REGISTER_21,
        INTEGER_REGISTER_22,
        INTEGER_REGISTER_23,
        INTEGER_REGISTER_24,
        INTEGER_REGISTER_25,
        INTEGER_REGISTER_26,
        INTEGER_REGISTER_27,
        INTEGER_REGISTER_28,
        INTEGER_REGISTER_29,
        INTEGER_REGISTER_30,
        INTEGER_REGISTER_31,
        INTEGER_REGISTER(0, 31) {
            @Override
            public Tag wideOperandTag() {
                return INTEGER_REGISTER_WIDE;
            }
        },
        INTEGER_REGISTER_WIDE,
        FLOATING_POINT_REGISTER_0,
        FLOATING_POINT_REGISTER_1,
        FLOATING_POINT_REGISTER_2,
        FLOATING_POINT_REGISTER_3,
        FLOATING_POINT_REGISTER_4,
        FLOATING_POINT_REGISTER_5,
        FLOATING_POINT_REGISTER_6,
        FLOATING_POINT_REGISTER_7,
        FLOATING_POINT_REGISTER_8,
        FLOATING_POINT_REGISTER_9,
        FLOATING_POINT_REGISTER_10,
        FLOATING_POINT_REGISTER_11,
        FLOATING_POINT_REGISTER_12,
        FLOATING_POINT_REGISTER_13,
        FLOATING_POINT_REGISTER_14,
        FLOATING_POINT_REGISTER_15,
        FLOATING_POINT_REGISTER_16,
        FLOATING_POINT_REGISTER_17,
        FLOATING_POINT_REGISTER_18,
        FLOATING_POINT_REGISTER_19,
        FLOATING_POINT_REGISTER_20,
        FLOATING_POINT_REGISTER_21,
        FLOATING_POINT_REGISTER_22,
        FLOATING_POINT_REGISTER_23,
        FLOATING_POINT_REGISTER_24,
        FLOATING_POINT_REGISTER_25,
        FLOATING_POINT_REGISTER_26,
        FLOATING_POINT_REGISTER_27,
        FLOATING_POINT_REGISTER_28,
        FLOATING_POINT_REGISTER_29,
        FLOATING_POINT_REGISTER_30,
        FLOATING_POINT_REGISTER_31,
        FLOATING_POINT_REGISTER(0, 31) {
            @Override
            public Tag wideOperandTag() {
                return FLOATING_POINT_REGISTER_WIDE;
            }
        },
        FLOATING_POINT_REGISTER_WIDE,
        PARAMETER_STACK_SLOT_0,
        PARAMETER_STACK_SLOT_1,
        PARAMETER_STACK_SLOT_2,
        PARAMETER_STACK_SLOT_3,
        PARAMETER_STACK_SLOT_4,
        PARAMETER_STACK_SLOT_5,
        PARAMETER_STACK_SLOT_6,
        PARAMETER_STACK_SLOT_7,
        PARAMETER_STACK_SLOT_8,
        PARAMETER_STACK_SLOT_9,
        PARAMETER_STACK_SLOT(0, 9) {
            @Override
            public Tag wideOperandTag() {
                return PARAMETER_STACK_SLOT_WIDE;
            }
        },
        PARAMETER_STACK_SLOT_WIDE,
        LOCAL_STACK_SLOT_0,
        LOCAL_STACK_SLOT_1,
        LOCAL_STACK_SLOT_2,
        LOCAL_STACK_SLOT_3,
        LOCAL_STACK_SLOT_4,
        LOCAL_STACK_SLOT_5,
        LOCAL_STACK_SLOT_6,
        LOCAL_STACK_SLOT_7,
        LOCAL_STACK_SLOT_8,
        LOCAL_STACK_SLOT_9,
        LOCAL_STACK_SLOT_10,
        LOCAL_STACK_SLOT_11,
        LOCAL_STACK_SLOT_12,
        LOCAL_STACK_SLOT_13,
        LOCAL_STACK_SLOT_14,
        LOCAL_STACK_SLOT_15,
        LOCAL_STACK_SLOT_16,
        LOCAL_STACK_SLOT_17,
        LOCAL_STACK_SLOT_18,
        LOCAL_STACK_SLOT_19,
        LOCAL_STACK_SLOT_20,
        LOCAL_STACK_SLOT_21,
        LOCAL_STACK_SLOT_22,
        LOCAL_STACK_SLOT_23,
        LOCAL_STACK_SLOT_24,
        LOCAL_STACK_SLOT_25,
        LOCAL_STACK_SLOT_26,
        LOCAL_STACK_SLOT_27,
        LOCAL_STACK_SLOT_28,
        LOCAL_STACK_SLOT_29,
        LOCAL_STACK_SLOT(0, 29) {
            @Override
            public Tag wideOperandTag() {
                return LOCAL_STACK_SLOT_WIDE;
            }
        },
        LOCAL_STACK_SLOT_WIDE,
        BLOCK,
        METHOD;

        @HOSTED_ONLY
        Tag() {
            this(null);
        }

        @HOSTED_ONLY
        Tag(int lowestImplicitOperand, int highestImplicitOperand) {
            this(new Range(lowestImplicitOperand, highestImplicitOperand + 1));
        }

        @HOSTED_ONLY
        Tag(Range implicitOperandTagsRange) {
            final String name = name();
            final int lastUnderscoreIndex = name.lastIndexOf('_');
            if (lastUnderscoreIndex != -1) {
                final String suffix = name.substring(lastUnderscoreIndex + 1).replace('M', '-');
                if (suffix.matches("-?\\d+")) {
                    implicitOperand = Integer.parseInt(suffix);
                } else {
                    implicitOperand = INVALID_IMPLICIT_NUMERIC_OPERAND;
                }
            } else {
                implicitOperand = INVALID_IMPLICIT_NUMERIC_OPERAND;
            }
            this.implicitOperandTagsRange = implicitOperandTagsRange;
            if (implicitOperandTagsRange != null) {
                implicitOperandTags = new Tag[(int) implicitOperandTagsRange.length()];
            } else {
                implicitOperandTags = null;
            }
        }

        private final int implicitOperand;

        private Tag wideOperandTag;

        private final Range implicitOperandTagsRange;
        private final Tag[] implicitOperandTags;

        /**
         * Gets the numeric operand implied by this tag's name.
         *
         * @return {@value #INVALID_IMPLICIT_NUMERIC_OPERAND} if this tag's name does not imply a numeric operand
         */
        public int implicitOperand() {
            return implicitOperand;
        }

        public static final int INVALID_IMPLICIT_NUMERIC_OPERAND = 0x10101010;

        /**
         * Gets an alternative version of this tag that encodes an implicit operand value.
         *
         * @param operand
         *                an operand value
         * @return the version of this tag that encodes {@code operand} in it's mnemonic. If there is no such tag,
         *         {@code null} is returned.
         */
        public Tag implicitOperandTag(int operand) {
            if (implicitOperandTagsRange != null && implicitOperandTagsRange.contains(operand)) {
                assert implicitOperandTags != null;
                return implicitOperandTags[operand - implicitOperandTagsRange.start()];
            }
            return null;
        }

        /**
         * Gets an alternative version of this tag that is used when an operand cannot be encoded as an unsigned byte.
         *
         * @return the version of this tag that encodes operand values greater than 0xff or less than 0. If there is no such tag,
         *         {@code null} is returned.
         */
        public Tag wideOperandTag() {
            return null;
        }

        @HOSTED_ONLY
        private void initializeImplicitOperandTags() {
            final Range range = implicitOperandTagsRange;
            if (range != null) {
                for (int operand = range.start(); operand < range.end(); ++operand) {
                    final String suffix = operand < 0 ? "_M" + (-operand) : "_" + operand;
                    final Tag implicitOperandTag = Tag.valueOf(name() + suffix);
                    assert implicitOperandTag != null;
                    implicitOperandTags[operand - range.start()] = implicitOperandTag;
                }
            }
        }

        public static final List<Tag> VALUES = Arrays.asList(values());
        static {
            if (MaxineVM.isHosted()) {
                // Ensure that a Tag ordinal can be encoded as an unsigned byte.
                ProgramError.check(VALUES.size() <= 0xFF);

                for (Tag tag : VALUES) {
                    tag.initializeImplicitOperandTags();
                }
            }
        }
    }

    public abstract Tag tag();

    protected TargetLocation() {
    }

    public CiValue toCiValue() {
        return CiValue.IllegalValue;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public abstract void write(DataOutput stream) throws IOException;

    public static Value valueFromInt(KindEnum kind, int intValue) {
        switch (kind) {
            case BYTE: {
                return ByteValue.from((byte) intValue);
            }
            case BOOLEAN: {
                return BooleanValue.from(intValue != 0);
            }
            case CHAR: {
                return CharValue.from((char) intValue);
            }
            case SHORT: {
                return ShortValue.from((short) intValue);
            }
            case INT: {
                return IntValue.from(intValue);
            }
            case FLOAT: {
                return FloatValue.from(intValue);
            }
            case LONG: {
                return LongValue.from(intValue);
            }
            case DOUBLE: {
                return DoubleValue.from(intValue);
            }
            case REFERENCE: {
                if (intValue != 0) {
                    return null;
                }
                return ReferenceValue.NULL;
            }
            case WORD: {
                return WordValue.from(Address.fromUnsignedInt(intValue));
            }
            default:
                return null;
        }
    }

    public static TargetLocation read(DataInput stream) throws IOException {
        final int index = stream.readUnsignedByte();
        final Tag tag = Tag.VALUES.get(index);
        final int implicitOperand = tag.implicitOperand();
        switch (tag) {
            case UNDEFINED: {
                return undefined;
            }
            case IMMEDIATE_M2:
            case IMMEDIATE_M1:
            case IMMEDIATE_0:
            case IMMEDIATE_1:
            case IMMEDIATE_2:
            case IMMEDIATE_3:
            case IMMEDIATE_4:
            case IMMEDIATE_5:
            case IMMEDIATE_6:
            case IMMEDIATE_7:
            case IMMEDIATE_8:
            case IMMEDIATE_9: {
                final int ordinal = stream.readUnsignedByte();
                final KindEnum kind = KindEnum.VALUES.get(ordinal);
                final Value value = valueFromInt(kind, implicitOperand);
                if (value == null) {
                    throw ProgramError.unexpected();
                }
                return new Immediate(value);
            }
            case IMMEDIATE: {
                final KindEnum kind = KindEnum.VALUES.get(stream.readUnsignedByte());
                final Value value;
                switch (kind) {
                    case BYTE: {
                        value = ByteValue.from(stream.readByte());
                        break;
                    }
                    case BOOLEAN: {
                        value = BooleanValue.from(stream.readBoolean());
                        break;
                    }
                    case CHAR: {
                        value = CharValue.from(stream.readChar());
                        break;
                    }
                    case SHORT: {
                        value = ShortValue.from(stream.readShort());
                        break;
                    }
                    case INT: {
                        value = IntValue.from(stream.readInt());
                        break;
                    }
                    case FLOAT: {
                        value = FloatValue.from(stream.readFloat());
                        break;
                    }
                    case LONG: {
                        value = LongValue.from(stream.readLong());
                        break;
                    }
                    case DOUBLE: {
                        value = DoubleValue.from(stream.readDouble());
                        break;
                    }
                    case WORD: {
                        value = WordValue.from(Word.read(stream));
                        break;
                    }
                    default:
                        throw ProgramError.unexpected();
                }
                return new Immediate(value);
            }
            case SCALAR_LITERAL_0:
            case SCALAR_LITERAL_1:
            case SCALAR_LITERAL_2:
            case SCALAR_LITERAL_3:
            case SCALAR_LITERAL_4:
            case SCALAR_LITERAL_5:
            case SCALAR_LITERAL_6:
            case SCALAR_LITERAL_7:
            case SCALAR_LITERAL_8:
            case SCALAR_LITERAL_9: {
                return new ScalarLiteral(implicitOperand);
            }
            case SCALAR_LITERAL: {
                return new ScalarLiteral(stream.readUnsignedByte());
            }
            case SCALAR_LITERAL_WIDE: {
                return new ScalarLiteral(stream.readInt());
            }
            case REFERENCE_LITERAL_0:
            case REFERENCE_LITERAL_1:
            case REFERENCE_LITERAL_2:
            case REFERENCE_LITERAL_3:
            case REFERENCE_LITERAL_4:
            case REFERENCE_LITERAL_5:
            case REFERENCE_LITERAL_6:
            case REFERENCE_LITERAL_7:
            case REFERENCE_LITERAL_8:
            case REFERENCE_LITERAL_9: {
                return new ReferenceLiteral(implicitOperand);
            }
            case REFERENCE_LITERAL: {
                return new ReferenceLiteral(stream.readUnsignedByte());
            }
            case REFERENCE_LITERAL_WIDE: {
                return new ReferenceLiteral(stream.readInt());
            }
            case INTEGER_REGISTER_0:
            case INTEGER_REGISTER_1:
            case INTEGER_REGISTER_2:
            case INTEGER_REGISTER_3:
            case INTEGER_REGISTER_4:
            case INTEGER_REGISTER_5:
            case INTEGER_REGISTER_6:
            case INTEGER_REGISTER_7:
            case INTEGER_REGISTER_8:
            case INTEGER_REGISTER_9:
            case INTEGER_REGISTER_10:
            case INTEGER_REGISTER_11:
            case INTEGER_REGISTER_12:
            case INTEGER_REGISTER_13:
            case INTEGER_REGISTER_14:
            case INTEGER_REGISTER_15:
            case INTEGER_REGISTER_16:
            case INTEGER_REGISTER_17:
            case INTEGER_REGISTER_18:
            case INTEGER_REGISTER_19:
            case INTEGER_REGISTER_20:
            case INTEGER_REGISTER_21:
            case INTEGER_REGISTER_22:
            case INTEGER_REGISTER_23:
            case INTEGER_REGISTER_24:
            case INTEGER_REGISTER_25:
            case INTEGER_REGISTER_26:
            case INTEGER_REGISTER_27:
            case INTEGER_REGISTER_28:
            case INTEGER_REGISTER_29:
            case INTEGER_REGISTER_30:
            case INTEGER_REGISTER_31: {
                return new IntegerRegister(implicitOperand);
            }
            case INTEGER_REGISTER: {
                return new IntegerRegister(stream.readUnsignedByte());
            }
            case INTEGER_REGISTER_WIDE: {
                return new IntegerRegister(stream.readInt());
            }
            case FLOATING_POINT_REGISTER_0:
            case FLOATING_POINT_REGISTER_1:
            case FLOATING_POINT_REGISTER_2:
            case FLOATING_POINT_REGISTER_3:
            case FLOATING_POINT_REGISTER_4:
            case FLOATING_POINT_REGISTER_5:
            case FLOATING_POINT_REGISTER_6:
            case FLOATING_POINT_REGISTER_7:
            case FLOATING_POINT_REGISTER_8:
            case FLOATING_POINT_REGISTER_9:
            case FLOATING_POINT_REGISTER_10:
            case FLOATING_POINT_REGISTER_11:
            case FLOATING_POINT_REGISTER_12:
            case FLOATING_POINT_REGISTER_13:
            case FLOATING_POINT_REGISTER_14:
            case FLOATING_POINT_REGISTER_15:
            case FLOATING_POINT_REGISTER_16:
            case FLOATING_POINT_REGISTER_17:
            case FLOATING_POINT_REGISTER_18:
            case FLOATING_POINT_REGISTER_19:
            case FLOATING_POINT_REGISTER_20:
            case FLOATING_POINT_REGISTER_21:
            case FLOATING_POINT_REGISTER_22:
            case FLOATING_POINT_REGISTER_23:
            case FLOATING_POINT_REGISTER_24:
            case FLOATING_POINT_REGISTER_25:
            case FLOATING_POINT_REGISTER_26:
            case FLOATING_POINT_REGISTER_27:
            case FLOATING_POINT_REGISTER_28:
            case FLOATING_POINT_REGISTER_29:
            case FLOATING_POINT_REGISTER_30:
            case FLOATING_POINT_REGISTER_31: {
                return new FloatingPointRegister(implicitOperand);
            }
            case FLOATING_POINT_REGISTER: {
                return new FloatingPointRegister(stream.readUnsignedByte());
            }
            case FLOATING_POINT_REGISTER_WIDE: {
                return new FloatingPointRegister(stream.readInt());
            }
            case PARAMETER_STACK_SLOT_0:
            case PARAMETER_STACK_SLOT_1:
            case PARAMETER_STACK_SLOT_2:
            case PARAMETER_STACK_SLOT_3:
            case PARAMETER_STACK_SLOT_4:
            case PARAMETER_STACK_SLOT_5:
            case PARAMETER_STACK_SLOT_6:
            case PARAMETER_STACK_SLOT_7:
            case PARAMETER_STACK_SLOT_8:
            case PARAMETER_STACK_SLOT_9: {
                return new ParameterStackSlot(implicitOperand);
            }
            case PARAMETER_STACK_SLOT: {
                return new ParameterStackSlot(stream.readUnsignedByte());
            }
            case PARAMETER_STACK_SLOT_WIDE: {
                return new ParameterStackSlot(stream.readInt());
            }
            case LOCAL_STACK_SLOT_0:
            case LOCAL_STACK_SLOT_1:
            case LOCAL_STACK_SLOT_2:
            case LOCAL_STACK_SLOT_3:
            case LOCAL_STACK_SLOT_4:
            case LOCAL_STACK_SLOT_5:
            case LOCAL_STACK_SLOT_6:
            case LOCAL_STACK_SLOT_7:
            case LOCAL_STACK_SLOT_8:
            case LOCAL_STACK_SLOT_9:
            case LOCAL_STACK_SLOT_10:
            case LOCAL_STACK_SLOT_11:
            case LOCAL_STACK_SLOT_12:
            case LOCAL_STACK_SLOT_13:
            case LOCAL_STACK_SLOT_14:
            case LOCAL_STACK_SLOT_15:
            case LOCAL_STACK_SLOT_16:
            case LOCAL_STACK_SLOT_17:
            case LOCAL_STACK_SLOT_18:
            case LOCAL_STACK_SLOT_19:
            case LOCAL_STACK_SLOT_20:
            case LOCAL_STACK_SLOT_21:
            case LOCAL_STACK_SLOT_22:
            case LOCAL_STACK_SLOT_23:
            case LOCAL_STACK_SLOT_24:
            case LOCAL_STACK_SLOT_25:
            case LOCAL_STACK_SLOT_26:
            case LOCAL_STACK_SLOT_27:
            case LOCAL_STACK_SLOT_28:
            case LOCAL_STACK_SLOT_29: {
                return new LocalStackSlot(implicitOperand);
            }
            case LOCAL_STACK_SLOT: {
                return new LocalStackSlot(stream.readUnsignedByte());
            }
            case LOCAL_STACK_SLOT_WIDE: {
                return new LocalStackSlot(stream.readInt());
            }
            case BLOCK: {
                return new Block(stream.readInt());
            }
            case METHOD: {
                return new Method((ClassMethodActor) MethodActor.read(stream));
            }
            default:
                throw ProgramError.unknownCase(tag.toString());
        }
    }

    public abstract void acceptVisitor(TargetLocationVisitor visitor);

    public static final class Undefined extends TargetLocation {

        @Override
        public Tag tag() {
            return Tag.UNDEFINED;
        }

        private Undefined() {
            super();
        }

        @Override
        public void write(DataOutput stream) throws IOException {
            stream.write(tag().ordinal());
        }

        @Override
        public void acceptVisitor(TargetLocationVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final Undefined undefined = new Undefined();

    public static final class Immediate extends TargetLocation {
        @Override
        public Tag tag() {
            return Tag.IMMEDIATE;
        }

        final Value value;

        public Value value() {
            return value;
        }

        public Immediate(Value value) {
            this.value = value;
        }

        @Override
        public CiValue toCiValue() {
            return value.asCiConstant();
        }

        @Override
        public void write(DataOutput stream) throws IOException {
            if (value.kind() != Kind.REFERENCE) {
                final int intValue = value.toInt();
                if (valueFromInt(value.kind().asEnum, intValue).equals(value)) {
                    final Tag implicitOperandTag = tag().implicitOperandTag(intValue);
                    if (implicitOperandTag != null) {
                        stream.write(implicitOperandTag.ordinal());
                        stream.write(value.kind().asEnum.ordinal());
                        return;
                    }
                }
            } else {
                if (value.isZero()) {
                    stream.write(Tag.IMMEDIATE_0.ordinal());
                    stream.write(KindEnum.REFERENCE.ordinal());
                    return;
                }
            }
            stream.write(tag().ordinal());
            stream.write(value.kind().asEnum.ordinal());
            value.write(stream);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Immediate) {
                final Immediate immediate = (Immediate) other;
                return value.equals(immediate.value);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return value.kind().toString() + ":" + value.toString();
        }

        @Override
        public void acceptVisitor(TargetLocationVisitor visitor) {
            visitor.visit(this);
        }
    }

    public abstract static class Index extends TargetLocation {

        public final int index;

        public int index() {
            return index;
        }

        public Index(int index) {
            this.index = index;
        }

        @Override
        public boolean equals(Object other) {
            if (other.getClass().equals(getClass())) {
                final Index otherIndex = (Index) other;
                return tag() == otherIndex.tag() && this.index == otherIndex.index;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return index;
        }

        @Override
        public String toString() {
            return super.toString() + "#" + index();
        }

        @Override
        public void write(DataOutput stream) throws IOException {
            final Tag implicitOperandTag = tag().implicitOperandTag(index);
            if (implicitOperandTag != null) {
                stream.write(implicitOperandTag.ordinal());
            } else if (index <= 0xFF) {
                stream.write(tag().ordinal());
                stream.writeByte(index());
            } else {
                final Tag wideOperandTag = tag().wideOperandTag();
                assert wideOperandTag != null;
                stream.write(wideOperandTag.ordinal());
                stream.writeInt(index());
            }
        }
    }

    /**
     * A scalar literal target location. The value returned by {@link #index()} for a {@code ScalarLiteral} object is
     * the index of the first byte of the scalar value in the
     * {@linkplain TargetMethod#scalarLiterals() scalar values} of a target method. The bytes of the scalar value
     * are encoded in the target-specific endianness.
     */
    public static final class ScalarLiteral extends Index {
        @Override
        public Tag tag() {
            return Tag.SCALAR_LITERAL;
        }

        public ScalarLiteral(int index) {
            super(index);
        }

        @Override
        public CiValue toCiValue() {
            return new CiAddress(CiKind.Object, CiRegister.Literals.asValue(), index);
        }

        @Override
        public void acceptVisitor(TargetLocationVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * A reference literal target location. The value returned by {@link #index()} for a {@code ReferenceLiteral} object
     * is the index of the object reference in the {@linkplain TargetMethod#referenceLiterals() reference literal} of a
     * target method.
     */
    public static final class ReferenceLiteral extends Index {
        @Override
        public Tag tag() {
            return Tag.REFERENCE_LITERAL;
        }

        public ReferenceLiteral(int index) {
            super(index);
        }

        @Override
        public CiValue toCiValue() {
            return new CiAddress(CiKind.Object, CiRegister.Literals.asValue(), index * Word.size());
        }

        @Override
        public void acceptVisitor(TargetLocationVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final class IntegerRegister extends Index {
        @Override
        public Tag tag() {
            return Tag.INTEGER_REGISTER;
        }

        public IntegerRegister(int register) {
            super(register);
        }

        @Override
        public CiValue toCiValue() {
            return target().arch.registerFor(index, CPU).asValue();
        }

        @Override
        public void acceptVisitor(TargetLocationVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final class FloatingPointRegister extends Index {
        @Override
        public Tag tag() {
            return Tag.FLOATING_POINT_REGISTER;
        }

        public FloatingPointRegister(int register) {
            super(register);
        }

        @Override
        public CiValue toCiValue() {
            return target().arch.registerFor(index, FPU).asValue();
        }

        @Override
        public void acceptVisitor(TargetLocationVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final class ParameterStackSlot extends Index {
        @Override
        public Tag tag() {
            return Tag.PARAMETER_STACK_SLOT;
        }

        public ParameterStackSlot(int index) {
            super(index);
        }

        @Override
        public CiValue toCiValue() {
            return CiStackSlot.get(CiKind.Word, index, true);
        }

        @Override
        public void acceptVisitor(TargetLocationVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final class LocalStackSlot extends Index {
        @Override
        public Tag tag() {
            return Tag.LOCAL_STACK_SLOT;
        }

        public LocalStackSlot(int index) {
            super(index);
        }

        @Override
        public CiValue toCiValue() {
            return CiStackSlot.get(CiKind.Word, index, false);
        }

        @Override
        public void acceptVisitor(TargetLocationVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final class Block extends TargetLocation {
        @Override
        public Tag tag() {
            return Tag.BLOCK;
        }

        private int position;

        public int position() {
            return position;
        }

        public Block(int position) {
            this.position = position;
        }

        @Override
        public void write(DataOutput stream) throws IOException {
            stream.write(tag().ordinal());
            stream.writeInt(position);
        }

        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        public int hashCode() {
            return position;
        }

        @Override
        public String toString() {
            return "block@" + position;
        }

        @Override
        public void acceptVisitor(TargetLocationVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final class Method extends TargetLocation {
        @Override
        public Tag tag() {
            return Tag.METHOD;
        }

        final ClassMethodActor classMethodActor;

        public MethodActor classMethodActor() {
            return classMethodActor;
        }

        public Method(ClassMethodActor classMethodActor) {
            this.classMethodActor = classMethodActor;
        }

        @Override
        public CiValue toCiValue() {
            return CiConstant.forObject(classMethodActor);
        }

        @Override
        public void write(DataOutput stream) throws IOException {
            stream.write(tag().ordinal());
            classMethodActor().write(stream);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Method) {
                final Method method = (Method) other;
                return classMethodActor == method.classMethodActor;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return classMethodActor.hashCode();
        }

        @Override
        public String toString() {
            return classMethodActor.toString();
        }

        @Override
        public void acceptVisitor(TargetLocationVisitor visitor) {
            visitor.visit(this);
        }
    }
}
