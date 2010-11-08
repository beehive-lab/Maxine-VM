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

import com.sun.cri.ci.CiRegister.RegisterFlag;


/**
 * Represents a CPU architecture, including information such as its endianness, CPU
 * registers, word width, etc.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public abstract class CiArchitecture {

    /**
     * The endianness of the architecture.
     */
    public static enum ByteOrder {
        LittleEndian,
        BigEndian
    }

    /**
     * Represents the natural size of words (typically registers and pointers) of this architecture, in bytes.
     */
    public final int wordSize;

    /**
     * The name of this architecture (e.g. "AMD64", "SPARCv9").
     */
    public final String name;

    /**
     * The name of the platform associated with this architecture (e.g. "X86" or "SPARC").
     */
    public final String platform;

    /**
     * The offset of the lower half of a word in bytes.
     */
    public final int lowWordOffset;

    /**
     * The offset of the upper half of a word in bytes.
     */
    public final int highWordOffset;

    /**
     * Array of all available registers on this architecture. The index of each register in this
     * array is equal to its {@linkplain CiRegister#number number}.
     */
    public final CiRegister[] registers;
    
    /**
     * Map of all registers keyed by their {@linkplain CiRegister#name names}.
     */
    public final HashMap<String, CiRegister> registersByName;

    /**
     * The byte ordering can be either little or big endian.
     */
    public final ByteOrder byteOrder;

    /**
     * Offset in bytes from the beginning of a call instruction to the displacement.
     */
    public final int machineCodeCallDisplacementOffset;

    public final int returnAddressSize;

    private final EnumMap<RegisterFlag, CiRegister[]> registersByTypeAndEncoding;
    
    /**
     * Gets the register for a given {@linkplain CiRegister#encoding encoding} and type.
     * 
     * @param encoding a register value as used in a machine instruction 
     * @param type the type of the register
     */
    public CiRegister registerFor(int encoding, RegisterFlag type) {
        CiRegister[] regs = registersByTypeAndEncoding.get(type);
        assert encoding >= 0 && encoding < regs.length;
        CiRegister reg = regs[encoding];
        assert reg != null;
        return reg;
    }

    protected CiArchitecture(String name, int wordSize, String backend, ByteOrder byteOrder, CiRegister[] registers, int nativeCallDisplacementOffset, int returnAddressSize) {
        this.name = name;
        this.registers = registers;
        this.wordSize = wordSize;
        this.platform = backend;
        this.byteOrder = byteOrder;
        this.machineCodeCallDisplacementOffset = nativeCallDisplacementOffset;
        this.returnAddressSize = returnAddressSize;

        if (byteOrder == ByteOrder.LittleEndian) {
            this.lowWordOffset = 0;
            this.highWordOffset = wordSize;
        } else {
            this.lowWordOffset = wordSize;
            this.highWordOffset = 0;
        }

        registersByName = new HashMap<String, CiRegister>(registers.length);
        for (CiRegister register : registers) {
            registersByName.put(register.name, register);
            assert registers[register.number] == register;
        }

        registersByTypeAndEncoding = new EnumMap<CiRegister.RegisterFlag, CiRegister[]>(RegisterFlag.class);
        EnumMap<RegisterFlag, CiRegister[]> categorizedRegs = CiRegister.categorize(registers);
        for (RegisterFlag type : RegisterFlag.values()) {
            CiRegister[] regs = categorizedRegs.get(type);
            int max = CiRegister.maxRegisterEncoding(regs);
            CiRegister[] regsByEnc = new CiRegister[max + 1];
            for (CiRegister reg : regs) {
                regsByEnc[reg.encoding] = reg;
            }
            registersByTypeAndEncoding.put(type, regsByEnc);
        }
    }

    /**
     * Converts this architecture to a string.
     * @return the string representation of this architecture
     */
    @Override
    public final String toString() {
        return name.toLowerCase();
    }

    /**
     * Checks whether this is a 32-bit architecture.
     * @return {@code true} if this architecture is 32-bit
     */
    public final boolean is32bit() {
        return wordSize == 4;
    }

    /**
     * Checks whether this is a 64-bit architecture.
     * @return {@code true} if this architecture is 64-bit
     */
    public final boolean is64bit() {
        return wordSize == 8;
    }

    // The following methods are architecture specific and not dependent on state
    // stored in this class. They have convenient default implementations.

    /**
     * Checks whether this architecture's normal arithmetic instructions use a two-operand form
     * (e.g. x86 which overwrites one operand register with the result when adding).
     * @return {@code true} if this architecture uses two-operand mode
     */
    public boolean twoOperandMode() {
        return false;
    }

    // TODO: Why enumerate the concrete subclasses here rather
    // than use instanceof comparisons in code that cares?

    /**
     * Checks whether the architecture is x86.
     * @return {@code true} if the architecture is x86
     */
    public boolean isX86() {
        return false;
    }

    /**
     * Checks whether the architecture is SPARC.
     * @return {@code true} if the architecture is SPARC
     */
    public boolean isSPARC() {
        return false;
    }

}
