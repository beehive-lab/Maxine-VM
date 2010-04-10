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
    public static final CiRegister None = new CiRegister(-1, -1, "noreg");

    /**
     * Frame pointer of the current method. All spill slots and outgoing stack-based arguments
     * are addressed relative to this register.
     */
    public static final CiRegister Frame = new CiRegister(-2, -2, "framereg", RegisterFlag.CPU);

    /**
     * Frame pointer for the caller of the current method. All incoming stack-based arguments
     * are address relative to this register.
     */
    public static final CiRegister CallerFrame = new CiRegister(-3, -3, "caller-framereg", RegisterFlag.CPU);

    /**
     * The identifier for this register that is unique across all the registers in a {@link RiRegisterConfig}. 
     */
    public final int number;
    
    /**
     * The mnemonic of this register.
     */
    public final String name;
    
    public final int encoding;

    private final int flags;
    
    private final CiRegisterLocation[] locations;
    
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

    public CiRegister(int number, int encoding, String name, RegisterFlag... flags) {
        this.number = number;
        this.name = name;
        this.flags = createMask(flags);
        this.encoding = encoding;
        
        locations = new CiRegisterLocation[CiKind.VALUES.length];
        for (CiKind kind : CiKind.VALUES) {
            locations[kind.ordinal()] = new CiRegisterLocation(kind, this);
        }
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
        return locations[kind.ordinal()];
    }

    public CiRegisterLocation asLocation() {
        return asLocation(CiKind.Illegal);
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
     * Register information required by a register allocator.
     */
    public static class AllocationSpec {

        /**
         * The set of registers a register allocator can use. This includes
         * both general purpose and floating point registers.
         */
        public final CiRegister[] allocatableRegisters;
        
        /**
         * The set of CPU registers a register allocator can use. The {@linkplain CiRegister#number numbers}
         * of the registers in this array are contiguous and increasing.
         */
        public final CiRegister[] allocatableCpuRegisters;

        /**
         * The set of floating point registers a register allocator can use. The {@linkplain CiRegister#number numbers}
         * of the registers in this array are contiguous and increasing.
         */
        public final CiRegister[] allocatableFPRegisters;

        /**
         * The set of {@linkplain RegisterFlag#Byte byte} registers a register allocator can use. The {@linkplain CiRegister#number numbers}
         * of the registers in this array are contiguous and increasing.
         */
        public final CiRegister[] allocatableByteRegisters;

        /**
         * Map from {@linkplain CiRegister#number register numbers} to registers.
         */
        public final CiRegister[] registerMap;
        
        /**
         * The set of caller saved registers. Values in these registers must be saved and restored around call sites.
         */
        public final CiRegister[] callerSaveRegisters;
        
        /**
         * The intersection of the {@linkplain #allocatableRegisters allocatable} and
         * {@linkplain #callerSaveRegisters caller-save} registers.
         */
        public final CiRegister[] callerSaveAllocatableRegisters;
        
        /**
         * Map from {@linkplain CiRegister#number register numbers} to booleans indicating if
         * the denoted registers are {@linkplain #allocatableRegisters allocatable}.
         */
        private final boolean[] allocatableRegistersMap;
        
        /**
         * Map from {@linkplain CiRegister#number register numbers} to booleans indicating if
         * the denoted registers are {@linkplain #callerSaveRegisters caller-saved}.
         */
        private final boolean[] callerSaveRegistersMap;

        /**
         * Map from {@linkplain CiRegister#number register numbers} to reference map indexes
         * for the denoted registers. Entries for registers not covered by the reference map
         * (i.e. registers that can never hold a reference) are {@code null}.
         */
        public final int[] refMapIndexMap;

        /**
         * The length of the {@linkplain #registerMap register map}.
         */
        public final int nofRegs;
        
        /**
         * The number of registers that can potentially contain an object reference at a safepoint.
         * There are exactly this many non-{@code null} entries in {@link #refMapIndexMap}.
         */
        public final int refMapSize;

        AllocationSpec(CiRegister[] allocatableRegisters, CiRegister[] referenceMapTemplate, CiRegister[] callerSaveRegisters) {
            this.allocatableRegisters = allocatableRegisters;

            ArrayList<CiRegister> cpuRegs = new ArrayList<CiRegister>(); 
            ArrayList<CiRegister> fpRegs = new ArrayList<CiRegister>(); 
            ArrayList<CiRegister> byteRegs = new ArrayList<CiRegister>();
            int maxRegNum = 0;
            
            for (CiRegister r : allocatableRegisters) {
                if (r.isCpu()) {
                    cpuRegs.add(r);
                }

                if (r.isByte()) {
                    byteRegs.add(r);
                }

                if (r.isXmm()) {
                    fpRegs.add(r);
                }
                maxRegNum = Math.max(maxRegNum, r.number);
            }

            allocatableFPRegisters = fpRegs.toArray(new CiRegister[fpRegs.size()]);
            allocatableCpuRegisters = cpuRegs.toArray(new CiRegister[cpuRegs.size()]);
            allocatableByteRegisters = byteRegs.toArray(new CiRegister[byteRegs.size()]);
            
            nofRegs = maxRegNum + 1;
            this.refMapSize = referenceMapTemplate.length;
            this.registerMap = new CiRegister[nofRegs];
            this.refMapIndexMap = new int[nofRegs];
            this.allocatableRegistersMap = new boolean[nofRegs];
            this.callerSaveRegistersMap = new boolean[nofRegs];
            this.callerSaveRegisters = callerSaveRegisters;
            for (CiRegister r : allocatableRegisters) {
                assert registerMap[r.number] == null : "duplicate register!";
                registerMap[r.number] = r;
                allocatableRegistersMap[r.number] = true;
            }

            ArrayList<CiRegister> csList = new ArrayList<CiRegister>();
            for (CiRegister r : callerSaveRegisters) {
                callerSaveRegistersMap[r.number] = true;
                if (isAllocatable(r)) {
                    csList.add(r);
                }
            }
            this.callerSaveAllocatableRegisters = csList.toArray(new CiRegister[csList.size()]);

            Arrays.fill(refMapIndexMap, -1);
            for (int i = 0; i < referenceMapTemplate.length; i++) {
                CiRegister r = referenceMapTemplate[i];
                if (r != null) {
                    refMapIndexMap[r.number] = i;
                }
            }
        }

        /**
         * Determines if a given register is allocatable by a register allocator.
         * 
         * @param register a register to test
         * @return {@code true} if {@code register} is allocatable, {@code false} otherwise
         */
        public boolean isAllocatable(CiRegister register) {
            return register.number >= 0 && register.number < nofRegs && allocatableRegistersMap[register.number];
        }

        /**
         * Determines if a given register is {@linkplain #callerSaveRegisters caller saved}.
         * 
         * @param register a register to test
         * @return {@code true} if {@code register} is caller saved, {@code false} otherwise
         */
        public boolean isCallerSave(CiRegister register) {
            return register.number < nofRegs && callerSaveRegistersMap[register.number];
        }
}
}
