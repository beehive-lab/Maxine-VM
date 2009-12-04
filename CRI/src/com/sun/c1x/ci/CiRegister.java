/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ci;

import java.util.Arrays;

/**
 * This class represents a machine register.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public final class CiRegister {

    /**
     * Invalid register.
     */
    public static final CiRegister None = new CiRegister(-1, -1, "noreg");

    /**
     * Stack register of the current method.
     */
    public static final CiRegister Stack = new CiRegister(-2, -2, "stackreg", RegisterFlag.CPU);

    /**
     * Stack register relative to the caller stack. When this register is used in relative addressing, it means that the
     * offset is based to stack register of the caller and not to the stack register of the current method.
     */
    public static final CiRegister CallerStack = new CiRegister(-3, -3, "caller-stackreg", RegisterFlag.CPU);

    public static final int MaxPhysicalRegisterNumber = 40;

    public final int number;
    public final String name;
    public final int encoding;

    private final int flags;

    public enum RegisterFlag {
        CPU, Byte, XMM, MMX;

        public final int mask = 1 << (ordinal() + 1);
    }

    public CiRegister(int number, int encoding, String name, RegisterFlag... flags) {
        assert number < MaxPhysicalRegisterNumber : "cannot have a register number greater or equal " + MaxPhysicalRegisterNumber;
        this.number = number;
        this.name = name;
        this.flags = createMask(flags);
        this.encoding = encoding;
    }

    private int createMask(RegisterFlag... flags) {
        int result = 0;
        for (RegisterFlag f : flags) {
            result |= f.mask;
        }
        return result;
    }

    private boolean checkFlag(RegisterFlag f) {
        return (flags & f.mask) != 0;
    }

    public boolean isValid() {
        return number >= 0;
    }

    public boolean isXmm() {
        return checkFlag(RegisterFlag.XMM);
    }

    public boolean isCpu() {
        return checkFlag(RegisterFlag.CPU);
    }

    public boolean isByte() {
        return checkFlag(RegisterFlag.Byte);
    }

    public boolean isMMX() {
        return checkFlag(RegisterFlag.MMX);
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Utility function for asserting that the given registers are all different.
     *
     * @param registers
     *            an array of registers that should be checked for equal entries
     * @return false if an equal entry is found, true otherwise
     */
    public static boolean assertDifferentRegisters(CiRegister... registers) {

        for (int i = 0; i < registers.length; i++) {
            for (int j = 0; j < registers.length; j++) {
                if (i != j) {
                    if (registers[i] == registers[j]) {
                        assert false : "Registers " + i + " and " + j + " are both " + registers[i];
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static class AllocationSet {
        public final CiRegister[] allocatableRegisters;
        public final CiRegister[] registerMapping;
        public final boolean[] allocatableRegister;
        public final int[] referenceMapIndex;

        public final int nofRegs;
        public final int registerRefMapSize;

        public final int pdFirstCpuReg;
        public final int pdLastCpuReg;
        public final int pdFirstByteReg;
        public final int pdLastByteReg;
        public final int pdFirstXmmReg;
        public final int pdLastXmmReg;

        AllocationSet(CiRegister[] allocatableRegisters, CiRegister[] referenceMapTemplate) {
            this.allocatableRegisters = allocatableRegisters;

            int cpuCnt = 0;
            int cpuFirst = Integer.MAX_VALUE;
            int cpuLast = Integer.MIN_VALUE;
            int byteCnt = 0;
            int byteFirst = Integer.MAX_VALUE;
            int byteLast = Integer.MIN_VALUE;
            int xmmCnt = 0;
            int xmmFirst = Integer.MAX_VALUE;
            int xmmLast = Integer.MIN_VALUE;

            for (CiRegister r : allocatableRegisters) {
                if (r.isCpu()) {
                    cpuCnt++;
                    cpuFirst = Math.min(cpuFirst, r.number);
                    cpuLast = Math.max(cpuLast, r.number);
                }

                if (r.isByte()) {
                    byteCnt++;
                    byteFirst = Math.min(byteFirst, r.number);
                    byteLast = Math.max(byteLast, r.number);
                }

                if (r.isXmm()) {
                    xmmCnt++;
                    xmmFirst = Math.min(xmmFirst, r.number);
                    xmmLast = Math.max(xmmLast, r.number);
                }
            }
            assert xmmCnt > 0 && cpuCnt > 0 && byteCnt > 0 : "missing a register kind!";

            int maxReg = Math.max(cpuLast, xmmLast);
            registerRefMapSize = referenceMapTemplate.length;
            registerMapping = new CiRegister[maxReg + 1];
            referenceMapIndex = new int[maxReg + 1];
            allocatableRegister = new boolean[maxReg + 1];
            for (CiRegister r : allocatableRegisters) {
                assert registerMapping[r.number] == null : "duplicate register!";
                registerMapping[r.number] = r;
                allocatableRegister[r.number] = true;
            }

            Arrays.fill(referenceMapIndex, -1);
            for (int i = 0; i < referenceMapTemplate.length; i++) {
                CiRegister r = referenceMapTemplate[i];
                if (r != null) {
                    referenceMapIndex[r.number] = i;
                }
            }

            pdFirstByteReg = byteFirst;
            pdLastByteReg = byteLast;

            pdFirstCpuReg = cpuFirst;
            pdLastCpuReg = cpuLast;

            pdFirstXmmReg = xmmFirst;
            pdLastXmmReg = xmmLast;

            nofRegs = registerMapping.length;
        }
    }
}
