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
package com.sun.cri.ci;

import java.util.*;

/**
 * Represents a target machine register.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public final class CiRegister implements Comparable<CiRegister> {

    /**
     * Invalid register.
     */
    public static final CiRegister None = new CiRegister(-1, -1, 0, "noreg");

    /**
     * Frame pointer of the current method. All spill slots and outgoing stack-based arguments
     * are addressed relative to this register.
     */
    public static final CiRegister Frame = new CiRegister(-2, -2, 0, "framereg", RegisterFlag.CPU);

    /**
     * Frame pointer for the caller of the current method. All incoming stack-based arguments
     * are address relative to this register.
     */
    public static final CiRegister CallerFrame = new CiRegister(-3, -3, 0, "caller-framereg", RegisterFlag.CPU);

    /**
     * Literals pointer register.
     */
    public static final CiRegister Literals = new CiRegister(-4, -4, 0, "literals", RegisterFlag.CPU);

    /**
     * Register used to construct an RIP-Relative (x64) address.
     */
    public static final CiRegister InstructionRelative = new CiRegister(-5, -5, 0, "instr", RegisterFlag.CPU);

    /**
     * The identifier for this register that is unique across all the registers in a {@link CiArchitecture}.
     * A valid register has {@code number > 0}.
     */
    public final int number;

    /**
     * The mnemonic of this register.
     */
    public final String name;

    /**
     * The actual encoding in a target machine instruction for this register, which may or
     * may not be the same as {@link #number}.
     */
    public final int encoding;

    /**
     * The size of the stack slot used to spill the value of this register.
     */
    public final int spillSlotSize;
    
    /**
     * The set of {@link RegisterFlag} values associated with this register.
     */
    private final int flags;

    /**
     * An array of {@link CiRegisterValue} objects, for this register, with one entry
     * per {@link CiKind}, indexed by {@link CiKind#ordinal}.
     */
    private final CiRegisterValue[] values;

    /**
     * Attributes that characterize a register in a useful way.
     *
     */
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
         * Denotes a floating point register.
         */
        FPU;

        public final int mask = 1 << (ordinal() + 1);
    }

    /**
     * Creates a {@code CiRegister} instance.
     * 
     * @param number unique identifier for the register
     * @param encoding the target machine encoding for the register
     * @param spillSlotSize the size of the stack slot used to spill the value of the register
     * @param name the mnemonic name for the register
     * @param flags the set of {@link RegisterFlag} values for the register
     */
    public CiRegister(int number, int encoding, int spillSlotSize, String name, RegisterFlag... flags) {
        this.number = number;
        this.name = name;
        this.spillSlotSize = spillSlotSize;
        this.flags = createMask(flags);
        this.encoding = encoding;

        values = new CiRegisterValue[CiKind.VALUES.length];
        for (CiKind kind : CiKind.VALUES) {
            values[kind.ordinal()] = new CiRegisterValue(kind, this);
        }
    }

    private int createMask(RegisterFlag... flags) {
        int result = 0;
        for (RegisterFlag f : flags) {
            result |= f.mask;
        }
        return result;
    }

    public boolean isSet(RegisterFlag f) {
        return (flags & f.mask) != 0;
    }

    /**
     * Gets this register as a {@linkplain CiRegisterValue value} with a specified kind.
     * @param kind the specified kind
     * @return the {@link CiRegisterValue}
     */
    public CiRegisterValue asValue(CiKind kind) {
        return values[kind.ordinal()];
    }

    /**
     * Gets this register as a {@linkplain CiRegisterValue value} with no particular kind,
     * @return a {@link CiRegisterValue} with {@link CiKind#Illegal} kind.
     */
    public CiRegisterValue asValue() {
        return asValue(CiKind.Illegal);
    }

    /**
     * Determines if this is a valid register.
     * @return {@code true} iff this register is valid
     */
    public boolean isValid() {
        return number >= 0;
    }

    /**
     * Determines if this a floating point register.
     */
    public boolean isFpu() {
        return isSet(RegisterFlag.FPU);
    }

    /**
     * Determines if this a general purpose register.
     */
    public boolean isCpu() {
        return isSet(RegisterFlag.CPU);
    }

    /**
     * Determines if this register has the {@link RegisterFlag#Byte} attribute set.
     * @return {@code true} iff this register has the {@link RegisterFlag#Byte} attribute set.
     */
    public boolean isByte() {
        return isSet(RegisterFlag.Byte);
    }

    /**
     * Categorizes a set of registers by {@link RegisterFlag}.
     * 
     * @param registers a list of registers to be categorized
     * @return a map from each {@link RegisterFlag} constant to the list of registers for which the flag is
     *         {@linkplain #isSet(RegisterFlag) set}
     */
    public static EnumMap<RegisterFlag, CiRegister[]> categorize(CiRegister[] registers) {
        EnumMap<RegisterFlag, CiRegister[]> result = new EnumMap<RegisterFlag, CiRegister[]>(RegisterFlag.class);
        for (RegisterFlag flag : RegisterFlag.values()) {
            ArrayList<CiRegister> list = new ArrayList<CiRegister>();
            for (CiRegister r : registers) {
                if (r.isSet(flag)) {
                    list.add(r);
                }
            }
            result.put(flag, list.toArray(new CiRegister[list.size()]));
        }
        return result;
    }

    /**
     * Gets the maximum register {@linkplain #number number} in a given set of registers.
     * 
     * @param registers the set of registers to process
     * @return the maximum register number for any register in {@code registers}
     */
    public static int maxRegisterNumber(CiRegister[] registers) {
        int max = Integer.MIN_VALUE;
        for (CiRegister r : registers) {
            if (r.number > max) {
                max = r.number;
            }
        }
        return max;
    }
    
    /**
     * Gets the maximum register {@linkplain #encoding encoding} in a given set of registers.
     * 
     * @param registers the set of registers to process
     * @return the maximum register encoding for any register in {@code registers}
     */
    public static int maxRegisterEncoding(CiRegister[] registers) {
        int max = Integer.MIN_VALUE;
        for (CiRegister r : registers) {
            if (r.encoding > max) {
                max = r.encoding;
            }
        }
        return max;
    }

    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public int compareTo(CiRegister o) {
        if (number < o.number) {
            return -1;
        }
        if (number > o.number) {
            return 1;
        }
        return 0;
    }
}
