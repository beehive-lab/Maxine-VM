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
package com.sun.max.vm.compiler.eir.sparc;

import com.sun.max.asm.sparc.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * EIR representation of register for SPARC.
 * SPARC registers divide into general purpose registers and floating-point registers.
 * Floating point registers can be single-precision, double-precision or quad-precision.
 * Only the first two are supported in this representation.
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public abstract class SPARCEirRegister extends EirRegister {

    private static SPARCEirRegister[] registers = new SPARCEirRegister[32 + 32 + 16];

    private static Pool<SPARCEirRegister> pool = new ArrayPool<SPARCEirRegister>(registers);

    public static Pool<SPARCEirRegister> pool() {
        return pool;
    }

    private final int serial;

    private static int nextSerial;

    protected SPARCEirRegister(int ordinal) {
        super(ordinal);
        serial = nextSerial++;
        assert registers[serial] == null;
        registers[serial] = this;
    }

    @Override
    public final int serial() {
        return serial;
    }

    /**
     * EIR representation of general purpose register for SPARC. General purpose registers can be use as operand of non-floating point operations.
     */
    public static final class GeneralPurpose extends SPARCEirRegister implements StaticFieldName, PoolObject {

        private GeneralPurpose(int ordinal) {
            super(ordinal);
        }

        @Override
        public Kind kind() {
            return Kind.WORD;
        }

        public static final GeneralPurpose G0 = new GeneralPurpose(0);
        public static final GeneralPurpose G1 = new GeneralPurpose(1);
        public static final GeneralPurpose G2 = new GeneralPurpose(2);
        public static final GeneralPurpose G3 = new GeneralPurpose(3);
        public static final GeneralPurpose G4 = new GeneralPurpose(4);
        public static final GeneralPurpose G5 = new GeneralPurpose(5);
        public static final GeneralPurpose G6 = new GeneralPurpose(6);
        public static final GeneralPurpose G7 = new GeneralPurpose(7);
        public static final GeneralPurpose O0 = new GeneralPurpose(8);
        public static final GeneralPurpose O1 = new GeneralPurpose(9);
        public static final GeneralPurpose O2 = new GeneralPurpose(10);
        public static final GeneralPurpose O3 = new GeneralPurpose(11);
        public static final GeneralPurpose O4 = new GeneralPurpose(12);
        public static final GeneralPurpose O5 = new GeneralPurpose(13);
        public static final GeneralPurpose O6 = new GeneralPurpose(14);
        public static final GeneralPurpose O7 = new GeneralPurpose(15);
        public static final GeneralPurpose L0 = new GeneralPurpose(16);
        public static final GeneralPurpose L1 = new GeneralPurpose(17);
        public static final GeneralPurpose L2 = new GeneralPurpose(18);
        public static final GeneralPurpose L3 = new GeneralPurpose(19);
        public static final GeneralPurpose L4 = new GeneralPurpose(20);
        public static final GeneralPurpose L5 = new GeneralPurpose(21);
        public static final GeneralPurpose L6 = new GeneralPurpose(22);
        public static final GeneralPurpose L7 = new GeneralPurpose(23);
        public static final GeneralPurpose I0 = new GeneralPurpose(24);
        public static final GeneralPurpose I1 = new GeneralPurpose(25);
        public static final GeneralPurpose I2 = new GeneralPurpose(26);
        public static final GeneralPurpose I3 = new GeneralPurpose(27);
        public static final GeneralPurpose I4 = new GeneralPurpose(28);
        public static final GeneralPurpose I5 = new GeneralPurpose(29);
        public static final GeneralPurpose I6 = new GeneralPurpose(30);
        public static final GeneralPurpose I7 = new GeneralPurpose(31);

        private static final GeneralPurpose[] values = {G0, G1, G2, G3, G4, G5, G6, G7,
                                                        O0, O1, O2, O3, O4, O5, O6, O7,
                                                        L0, L1, L2, L3, L4, L5, L6, L7,
                                                        I0, I1, I2, I3, I4, I5, I6, I7};

        public static final IndexedSequence<GeneralPurpose> VALUES = new ArraySequence<GeneralPurpose>(values);

        private static final GeneralPurpose[] locals = {L0, L1, L2, L3, L4, L5, L6, L7};

        public static final IndexedSequence<GeneralPurpose> LOCAL_REGISTERS = new ArraySequence<GeneralPurpose>(locals);

        private static final GeneralPurpose[] ins = {I0, I1, I2, I3, I4, I5, I6, I7};

        public static final IndexedSequence<GeneralPurpose> IN_REGISTERS = new ArraySequence<GeneralPurpose>(ins);

        private static final GeneralPurpose[] outs = {O0, O1, O2, O3, O4, O5, O6, O7};

        public static final IndexedSequence<GeneralPurpose> OUT_REGISTERS = new ArraySequence<GeneralPurpose>(outs);

        private static final PoolSet<SPARCEirRegister> poolSet = PoolSet.of(pool, values);

        public static PoolSet<SPARCEirRegister> poolSet() {
            return poolSet;
        }

        public GPR as() {
            return GPR.SYMBOLIZER.fromValue(ordinal);
        }

        public static GeneralPurpose from(GPR register) {
            return values[register.value()];
        }

        @Override
        public EirLocationCategory category() {
            return EirLocationCategory.INTEGER_REGISTER;
        }

        /**
         * Gets the index into the spill area.
         * Negative return value indicates this register is not spilled.
         */
        public int registerSpillIndex() {
            return ordinal - 16;
        }

        /**
         * Gets the index within trap state at which this global integer register is saved.
         * Negative return value indicates this integer register is not a global in the trap state area.
         */
        public int trapStateIndex() {
            if (ordinal > G0.ordinal && ordinal <= G5.ordinal) {
                return ordinal - G1.ordinal;
            }
            if (ordinal >= O0.ordinal && ordinal <= O7.ordinal) {
                return ordinal - 3;  // Since G0, G6, G7 are not in trapState.
            }
            return -1;
        }

        private String name;

        public String name() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        static {
            StaticFieldName.Static.initialize(GeneralPurpose.class);
        }

        @Override
        public String toString() {
            return name();
        }

        @Override
        public TargetLocation toTargetLocation() {
            return new TargetLocation.IntegerRegister(serial());
        }
    }

    /**
     * EIR representation of floating-point register for SPARC.
     *
     */
    public static class FloatingPoint extends SPARCEirRegister {

        protected FloatingPoint(int value) {
            super(value);
        }

        @Override
        public Kind kind() {
            return Kind.FLOAT;
        }

        /**
         * Indicates whether the register can be used for double precision instructions.
         *
         * @return a boolean indicating the precision of the floating point register.
         */
        public boolean isDoublePrecision() {
            return (ordinal & 1) == 0;
        }

        private static boolean isDoublePrecisionOnly(int value) {
            return value > 31;
        }

        /**
         * Returns the single precision floating-point register this register overlaps with, null if none.
         */
        public FloatingPoint overlappingSinglePrecision() {
            final int value = ordinal;
            if (isDoublePrecision()) {
                return isDoublePrecisionOnly(value) ? null : singlePrecisionValues[value + 1];
            }
            return singlePrecisionValues[value - 1];
        }

        public static final DoublePrecision F0 = new DoublePrecision(0);
        public static final FloatingPoint F1 = new FloatingPoint(1);
        public static final DoublePrecision F2 = new DoublePrecision(2);
        public static final FloatingPoint F3 = new FloatingPoint(3);
        public static final DoublePrecision F4 = new DoublePrecision(4);
        public static final FloatingPoint F5 = new FloatingPoint(5);
        public static final DoublePrecision F6 = new DoublePrecision(6);
        public static final FloatingPoint F7 = new FloatingPoint(7);
        public static final DoublePrecision F8 = new DoublePrecision(8);
        public static final FloatingPoint F9 = new FloatingPoint(9);
        public static final DoublePrecision F10 = new DoublePrecision(10);
        public static final FloatingPoint F11 = new FloatingPoint(11);
        public static final DoublePrecision F12 = new DoublePrecision(12);
        public static final FloatingPoint F13 = new FloatingPoint(13);
        public static final DoublePrecision F14 = new DoublePrecision(14);
        public static final FloatingPoint F15 = new FloatingPoint(15);
        public static final DoublePrecision F16 = new DoublePrecision(16);
        public static final FloatingPoint F17 = new FloatingPoint(17);
        public static final DoublePrecision F18 = new DoublePrecision(18);
        public static final FloatingPoint F19 = new FloatingPoint(19);
        public static final DoublePrecision F20 = new DoublePrecision(20);
        public static final FloatingPoint F21 = new FloatingPoint(21);
        public static final DoublePrecision F22 = new DoublePrecision(22);
        public static final FloatingPoint F23 = new FloatingPoint(23);
        public static final DoublePrecision F24 = new DoublePrecision(24);
        public static final FloatingPoint F25 = new FloatingPoint(25);
        public static final DoublePrecision F26 = new DoublePrecision(26);
        public static final FloatingPoint F27 = new FloatingPoint(27);
        public static final DoublePrecision F28 = new DoublePrecision(28);
        public static final FloatingPoint F29 = new FloatingPoint(29);
        public static final DoublePrecision F30 = new DoublePrecision(30);
        public static final FloatingPoint F31 = new FloatingPoint(31);

        private static final FloatingPoint[] singlePrecisionValues = {F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, F25, F26, F27, F28, F29, F30, F31};



        public static final DoublePrecision F32 = new DoublePrecision(32);
        public static final DoublePrecision F34 = new DoublePrecision(34);
        public static final DoublePrecision F36 = new DoublePrecision(36);
        public static final DoublePrecision F38 = new DoublePrecision(38);
        public static final DoublePrecision F40 = new DoublePrecision(40);
        public static final DoublePrecision F42 = new DoublePrecision(42);
        public static final DoublePrecision F44 = new DoublePrecision(44);
        public static final DoublePrecision F46 = new DoublePrecision(46);
        public static final DoublePrecision F48 = new DoublePrecision(48);
        public static final DoublePrecision F50 = new DoublePrecision(50);
        public static final DoublePrecision F52 = new DoublePrecision(52);
        public static final DoublePrecision F54 = new DoublePrecision(54);
        public static final DoublePrecision F56 = new DoublePrecision(56);
        public static final DoublePrecision F58 = new DoublePrecision(58);
        public static final DoublePrecision F60 = new DoublePrecision(60);
        public static final DoublePrecision F62 = new DoublePrecision(62);

        private static final DoublePrecision[] doublePrecisionValues = {F0, F2, F4, F6, F8, F10, F12, F14, F16, F18, F20, F22, F24, F26, F28, F30, F32, F34, F36, F38, F40, F42, F44, F46, F48, F50, F52, F54, F56, F58, F60, F62};

        private static final FloatingPoint[] values = {F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, F25, F26, F27, F28, F29, F30, F31,
            F32, F34, F36, F38, F40, F42, F44, F46, F48, F50, F52, F54, F56, F58, F60, F62};

        /**
         * A sequence describing all the EIR representations of the floating-point registers.
         */
        public static final IndexedSequence<FloatingPoint> VALUES = new ArraySequence<FloatingPoint>(values);

        /**
         * A sequence describing all the EIR representations of the single-precision floating-point registers.
         */
        public static final IndexedSequence<FloatingPoint> SINGLE_PRECISION_VALUES = new ArraySequence<FloatingPoint>(singlePrecisionValues);

        /**
         * A sequence describing all the EIR representations of the double-precision floating-point registers.
         */
        public static final IndexedSequence<FloatingPoint> DOUBLE_PRECISION_VALUES = new ArraySequence<FloatingPoint>(doublePrecisionValues);

        private static final PoolSet<SPARCEirRegister> poolSet = PoolSet.of(pool, values);
        private static final PoolSet<SPARCEirRegister> doublePrecisionPoolSet = PoolSet.of(pool, doublePrecisionValues);
        private static final PoolSet<SPARCEirRegister> singlePrecisionPoolSet = PoolSet.of(pool, singlePrecisionValues);

        public static PoolSet<SPARCEirRegister> poolSet() {
            return poolSet;
        }

        public static PoolSet<SPARCEirRegister> doublePrecisionPoolSet() {
            return doublePrecisionPoolSet;
        }

        public static PoolSet<SPARCEirRegister> singlePrecisionPoolSet() {
            return singlePrecisionPoolSet;
        }

        public static FloatingPoint doublePrecisionFrom(FPR register) {
            final int value = register.value();
            if ((value & 1) != 0) {
                throw new IllegalArgumentException();
            }
            if (isDoublePrecisionOnly(value)) {
                return doublePrecisionValues[value >> 1];
            }
            return singlePrecisionValues[value];
        }

        public FPR as() {
            return FPR.fromValue(ordinal);
        }

        public SFPR asSinglePrecision() {
            return (SFPR) as();
        }

        public DFPR asDoublePrecision() {
            return (DFPR) as();
        }

        @Override
        public EirLocationCategory category() {
            return EirLocationCategory.FLOATING_POINT_REGISTER;
        }

        @Override
        public String toString() {
            return as().name();
        }

        @Override
        public TargetLocation toTargetLocation() {
            return new TargetLocation.FloatingPointRegister(ordinal);
        }
    }

    public static final class DoublePrecision extends FloatingPoint {

        private DoublePrecision(int value) {
            super(value);
        }

        @Override
        public Kind kind() {
            return Kind.DOUBLE;
        }

        @Override
        public boolean isDoublePrecision() {
            return true;
        }
    }
}

