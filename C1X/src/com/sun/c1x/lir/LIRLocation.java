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

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;


/**
 * The <code>LIRLocation</code> class represents a LIROperand that is
 * either a stack slot or a CPU register. LIRLocation objects are immutable.
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

    public final int location1;
    public final int location2;
    public final int flags;

    /**
     * Creates a new LIRLocation representing either a stack value or a CPU register.
     * @param basicType the basic type of the location
     * @param number the number of the location, with negative numbers representing values on the stack
     * @param flags the flags of this location
     */
    public LIRLocation(BasicType basicType, int number, int flags) {
        super(basicType);
        this.location1 = number;
        this.location2 = number;
        this.flags = flags;
    }

    /**
     * Creates a new LIRLocation representing either a stack value or a CPU register.
     * @param basicType the basic type of the location
     * @param number the number of the location, with negative numbers representing values on the stack
     */
    public LIRLocation(BasicType basicType, int number) {
        super(basicType);
        this.location1 = number;
        this.location2 = number;
        this.flags = 0;
    }

    /**
     * Creates a new LIRLocation representing either a stack value or a CPU register.
     * @param basicType the basic type of the location
     * @param location1 the number of the location
     * @param location2 the number of the second location
     * @param flags the flags for the location
     */
    public LIRLocation(BasicType basicType, int location1, int location2, int flags) {
        super(basicType);
        this.location1 = location1;
        this.location2 = location2;
        this.flags = flags;
    }

    @Override
    public int hashCode() {
        return location1 + location2 + flags;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LIRLocation) {
            LIRLocation l = (LIRLocation) o;
            return l.basicType == basicType && l.location1 == location1 && l.location2 == location2 && l.flags == flags;
        }
        return false;
    }

    @Override
    public boolean isStack() {
        return location1 < 0;
    }

    @Override
    public int vregNumber() {
        assert location1 >= virtualRegisterBase();
        return location1;
    }

    public int pregNumber() {
        assert location1 < virtualRegisterBase() && location1 >= 0;
        return location1;
    }

    @Override
    boolean isSameRegister(LIROperand opr) {
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
        return !isStack() && location1 >= virtualRegisterBase();
    }

    @Override
    public boolean isFixedCpu() {
        return !isStack() && location1 < virtualRegisterBase();
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
        return location1;
    }

    @Override
    public int doubleStackIx() {
        assert isDoubleStack() && !isVirtual() : "type check";
        return location1;
    }

    @Override
    public int cpuRegnr() {
        assert isSingleCpu() && !isVirtual() : "type check";
        return location1;
    }

    @Override
    public int cpuRegnrLo() {
        assert isDoubleCpu() && !isVirtual() : "type check";
        return location1;
    }

    @Override
    public int cpuRegnrHi() {
        assert isDoubleCpu() && !isVirtual() : "type check";
        return location2;
    }

    @Override
    public int fpuRegnr() {
        assert isSingleFpu() && !isVirtual() : "type check";
        return location1;
    }

    @Override
    public int fpuRegnrLo() {
        assert isDoubleFpu() && !isVirtual() : "type check";
        return location1;
    }

    @Override
    public int fpuRegnrHi() {
        assert isDoubleFpu() && !isVirtual() : "type check";
        return location2;
    }

    @Override
    public int xmmRegnr() {
        assert isSingleXmm() && !isVirtual() : "type check";
        return location1;
    }

    @Override
    public int xmmRegnrLo() {
        assert isDoubleXmm() && !isVirtual() : "type check";
        return location1;
    }

    @Override
    public int xmmRegnrHi() {
        assert isDoubleXmm() && !isVirtual() : "type check";
        return location2;
    }

    /**
     * @return the minimum virtual register value
     */
    public static int virtualRegisterBase() {
        return 10000; // 10000 registers should be enough for anybody
    }

    /**
     * @return <code>true</code> if this location is a floating point value
     */
    private boolean isFloatType() {
        return (basicType == BasicType.Float || basicType == BasicType.Double);
    }


}
