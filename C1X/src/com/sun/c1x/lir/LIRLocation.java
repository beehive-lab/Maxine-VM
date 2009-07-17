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
package com.sun.c1x.lir;

import com.sun.c1x.target.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The <code>LIRLocation</code> class represents a LIROperand that is either a stack slot or a CPU register. LIRLocation
 * objects are immutable.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 *
 */
public class LIRLocation extends LIROperand {

    public static final int XMM = 0;
    public static final int LAST_USE = 1;
    public static final int DESTROY = 2;

    public final Register location1;
    public final Register location2;
    public final int index;
    public final int flags;

    /**
     * Creates a new LIRLocation representing a CPU register.
     *
     * @param basicType
     *            the basic type of the location
     * @param number
     *            the register
     * @param flags
     *            the flags of this location
     */
    public LIRLocation(BasicType basicType, Register number, int flags) {
        super(basicType);
        this.location1 = number;
        this.location2 = number;
        this.flags = flags;
        index = 0;
    }

    /**
     * Creates a new LIRLocation representing a CPU register.
     *
     * @param basicType
     *            the basic type of the location
     * @param number
     *            the register
     */
    public LIRLocation(BasicType basicType, Register number) {
        this(basicType, number, 0);
    }

    /**
     * Creates a new LIRLocation representing either a virtual register or a stack location, negative indices represent stack locations.
     *
     * @param basicType
     *            the basic type of the location
     * @param number
     *            the virtual register index or the stack location index if negative
     */
    public LIRLocation(BasicType basicType, int number) {
        super(basicType);
        this.location1 = null;
        this.location2 = null;
        this.index = number;
        this.flags = 0;
    }

    /**
     * Creates a new LIRLocation representing either a stack value or a CPU register.
     *
     * @param basicType
     *            the basic type of the location
     * @param location1
     *            the number of the location
     * @param location2
     *            the number of the second location
     * @param flags
     *            the flags for the location
     */
    public LIRLocation(BasicType basicType, Register location1, Register location2, int flags) {
        super(basicType);
        this.location1 = location1;
        this.location2 = location2;
        this.flags = flags;
        index = 0;
    }

    @Override
    public int hashCode() {
        return location1.number + location2.number + index + flags;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LIRLocation) {
            LIRLocation l = (LIRLocation) o;
            return l.basicType == basicType && l.location1 == location1 && l.location2 == location2 && l.index == index &&  l.flags == flags;
        }
        return false;
    }

    @Override
    public boolean isStack() {
        return index < 0;
    }

    @Override
    public int vregNumber() {
        assert index >= virtualRegisterBase();
        return index;
    }

    public int pregNumber() {
        assert index < virtualRegisterBase() && index >= 0;
        return index;
    }

    @Override
    public boolean isSameRegister(LIROperand opr) {
        throw Util.shouldNotReachHere();
    }

    @Override
    public boolean isSingleStack() {
        return isStack() && basicType.sizeInSlots() == 1;
    }

    @Override
    public boolean isDoubleStack() {
        return isStack() && basicType.sizeInSlots() == 2;
    }

    @Override
    public boolean isCpuRegister() {
        return !isStack();
    }

    @Override
    public boolean isVirtualCpu() {
        return !isStack() && index >= virtualRegisterBase();
    }

    @Override
    public boolean isFixedCpu() {
        return !isStack() && index < virtualRegisterBase();
    }

    @Override
    public boolean isSingleCpu() {
        return !isStack() && basicType.sizeInSlots() == 1;
    }

    @Override
    public boolean isDoubleCpu() {
        return !isStack() && basicType.sizeInSlots() == 2;
    }

    @Override
    public boolean isFpuRegister() {
        return !isStack() && isFloatType();
    }

    @Override
    public boolean isVirtualFpu() {
        return isVirtualCpu() && isFloatType();
    }

    @Override
    public boolean isFixedFpu() {
        return isFixedCpu() && isFloatType();
    }

    @Override
    public boolean isSingleFpu() {
        return !isStack() && basicType == BasicType.Float;
    }

    @Override
    public boolean isDoubleFpu() {
        return !isStack() && basicType == BasicType.Double;
    }

    @Override
    public boolean isXmmRegister() {
        return !isStack() && (flags & XMM) != 0;
    }

    @Override
    public boolean isSingleXmm() {
        return !isStack() && (flags & XMM) != 0 && basicType.sizeInSlots() == 1;
    }

    @Override
    public boolean isDoubleXmm() {
        return !isStack() && (flags & XMM) != 0 && basicType.sizeInSlots() == 2;
    }

    @Override
    public boolean isOopRegister() {
        return isCpuRegister() && basicType == BasicType.Object;
    }

    @Override
    public boolean isLastUse() {
        assert isRegister() : "only works for registers";
        return (flags & LAST_USE) != 0;
    }

    @Override
    public boolean isFpuStackOffset() {
        assert isRegister() : "only works for registers";
        throw Util.unimplemented();
    }

    @Override
    public LIROperand makeLastUse() {
        assert isRegister() : "only works for registers";
        return new LIRLocation(basicType, location1, location2, flags | LAST_USE);
    }

    @Override
    public LIROperand makeFpuStackOffset() {
        assert isRegister() : "only works for registers";
        throw Util.unimplemented();
    }

    @Override
    public int singleStackIx() {
        assert isSingleStack() && !isVirtual() : "type check";
        return index;
    }

    @Override
    public int doubleStackIx() {
        assert isDoubleStack() && !isVirtual() : "type check";
        return index;
    }

    @Override
    public Register asRegister() {
        assert location1 == location2;
        return this.location1;
    }

    @Override
    public Register asRegisterLo() {
        return this.location1;
    }

    @Override
    public Register asRegisterHi() {
        return this.location2;
    }

    @Override
    public boolean isVirtualRegister() {
        return index != 0 && !this.isStack();
    }


    @Override
    public int cpuRegnr() {
        assert this.isCpuRegister() && !this.isVirtualRegister();
        return location1.number;
    }

    @Override
    public int cpuRegnrLo() {
        assert this.isCpuRegister() && !this.isVirtualRegister();
        return location1.number;
    }

    @Override
    public int cpuRegnrHi() {
        assert this.isCpuRegister() && !this.isVirtualRegister();
        return location2.number;
    }

    @Override
    public int fpuRegnr() {
        assert this.isFpuRegister() && !this.isVirtualRegister();
        return location1.number;
    }

    @Override
    public int fpuRegnrLo() {
        assert this.isFpuRegister() && !this.isVirtualRegister();
        return location1.number;
    }

    @Override
    public int fpuRegnrHi() {
        assert this.isFpuRegister() && !this.isVirtualRegister();
        return location2.number;
    }

    @Override
    public int xmmRegnr() {
        assert this.isXmmRegister() && !this.isVirtualRegister();
        return location1.number;
    }

    @Override
    public int xmmRegnrLo() {
        assert this.isXmmRegister() && !this.isVirtualRegister();
        return location1.number;
    }

    @Override
    public int xmmRegnrHi() {
        assert this.isXmmRegister() && !this.isVirtualRegister();
        return location2.number;
    }


    /**
     * @return the minimum virtual register value
     */
    public static int virtualRegisterBase() {
        return Register.vregBase;
    }

    /**
     * @return <code>true</code> if this location is a floating point value
     */
    private boolean isFloatType() {
        return (basicType == BasicType.Float || basicType == BasicType.Double);
    }

}
