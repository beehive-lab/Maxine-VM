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

import java.util.ArrayList;
import java.util.Arrays;

import com.sun.c1x.ri.RiRegisterConfig;

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
    public static final CiRegister None = new CiRegister(-1, -1, -1, "noreg");

    /**
     * Frame pointer of the current method. All spill slots and outgoing stack-based arguments
     * are addressed relative to this register.
     */
    public static final CiRegister Frame = new CiRegister(-2, -2, -2, "framereg", RegisterFlag.CPU);

    /**
     * Frame pointer for the caller of the current method. All incoming stack-based arguments
     * are address relative to this register.
     */
    public static final CiRegister CallerFrame = new CiRegister(-3, -3, -3, "caller-framereg", RegisterFlag.CPU);

    public static final int LowestVirtualRegisterNumber = 40;

    /**
     * The identifier for this register that is unique across all the registers in a {@link RiRegisterConfig}. 
     */
    public final int number;
    
    /**
     * The size of this register in bits.
     */
    public final int width;
    
    /**
     * The mnemonic of this register.
     */
    public final String name;
    
    public final int encoding;

    private final int flags;

    public enum RegisterFlag {
        /**
         * Denotes an integral (i.e. non floating point) register.
         */
        CPU,
        
        /**
         * Denotes a register whose lowest order byte can be addressed separately.
         */
        Byte,
        
        /**
         * Denotes a 64-bit XMM register.
         */
        XMM,
        
        /**
         * Denotes an 80-bit MMX register.
         */
        MMX;

        public final int mask = 1 << (ordinal() + 1);
    }

    public CiRegister(int width, int number, int encoding, String name, RegisterFlag... flags) {
        assert number < LowestVirtualRegisterNumber : "cannot have a register number greater or equal " + LowestVirtualRegisterNumber;
        this.number = number;
        this.width = width;
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

    /**
     * Gets this register as a {@linkplain CiLocation location} with a specified kind.
     */
    public CiRegisterLocation asLocation(CiKind kind) {
        return CiRegisterLocation.get(kind, this);
    }

    public boolean isValid() {
        return number >= 0;
    }

    /**
     * Determines if this an XMM register.
     */
    public boolean isXmm() {
        return checkFlag(RegisterFlag.XMM);
    }

    /**
     * Determines if this a general purpose register.
     */
    public boolean isCpu() {
        return checkFlag(RegisterFlag.CPU);
    }

    public boolean isByte() {
        return checkFlag(RegisterFlag.Byte);
    }

    /**
     * Determines if this an MMX register.
     */
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
        public final CiRegister[] callerSaveRegisters;
        public final CiRegister[] callerSaveAllocatableRegisters;
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

        AllocationSet(CiRegister[] allocatableRegisters, CiRegister[] referenceMapTemplate, CiRegister[] callerSaveRegisters) {
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
            this.registerRefMapSize = referenceMapTemplate.length;
            this.registerMapping = new CiRegister[maxReg + 1];
            this.referenceMapIndex = new int[maxReg + 1];
            this.allocatableRegister = new boolean[maxReg + 1];
            this.callerSaveRegisters = callerSaveRegisters;
            for (CiRegister r : allocatableRegisters) {
                assert registerMapping[r.number] == null : "duplicate register!";
                registerMapping[r.number] = r;
                allocatableRegister[r.number] = true;
            }

            ArrayList<CiRegister> csList = new ArrayList<CiRegister>();
            for (CiRegister r : callerSaveRegisters) {
                if (allocatableRegister[r.number]) {
                    csList.add(r);
                }
            }
            this.callerSaveAllocatableRegisters = csList.toArray(new CiRegister[csList.size()]);

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
