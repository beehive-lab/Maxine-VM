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

import com.sun.c1x.target.Register;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.BasicType;

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

    public Register location1;
    public Register location2;
    public int index;

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
    public LIRLocation(BasicType basicType, Register number) {
        super(basicType);
        assert basicType.size == 1;
        assert number != null;
        this.location1 = number;
        this.location2 = Register.noreg;
        index = 0;
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
        assert number != 0;
        this.location1 = Register.noreg;
        this.location2 = Register.noreg;
        this.index = number;
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
     */
    public LIRLocation(BasicType basicType, Register location1, Register location2) {
        super(basicType);
        assert basicType.size == 2;
        assert location1 != null && location2 != null;
        this.location1 = location1;
        this.location2 = location2;
        index = 0;
    }

    @Override
    public int hashCode() {
        return location1.number + location2.number + index;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LIRLocation) {
            LIRLocation l = (LIRLocation) o;
            return l.basicType == basicType && l.location1 == location1 && l.location2 == location2 && l.index == index;
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
    public boolean isSingleStack() {
        return isStack() && basicType.sizeInSlots() == 1;
    }

    @Override
    public boolean isDoubleStack() {
        return isStack() && basicType.sizeInSlots() == 2;
    }

    @Override
    public boolean isCpuRegister() {
        return !isStack() && !location1.isXMM();
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
        return !isStack() && location2 == Register.noreg && location1.isCpu();
    }

    @Override
    public boolean isDoubleCpu() {
        return !isStack() && location2 != Register.noreg && location1.isCpu() && location2.isCpu();
    }

    @Override
    public boolean isXmmRegister() {
        return !isStack() && location1.isXMM();
    }

    @Override
    public boolean isSingleXmm() {
        return !isStack() && location1.isXMM() && basicType.sizeInSlots() == 1;
    }

    @Override
    public boolean isDoubleXmm() {
        return !isStack() && location1.isXMM() && basicType.sizeInSlots() == 2;
    }

    @Override
    public boolean isOopRegister() {
        return isCpuRegister() && basicType == BasicType.Object;
    }


    @Override
    public boolean isFpuStackOffset() {
        assert isRegister() : "only works for registers";
        throw Util.unimplemented();
    }

    @Override
    public LIROperand makeFpuStackOffset() {
        assert isRegister() : "only works for registers";
        throw Util.unimplemented();
    }

    @Override
    public int singleStackIx() {
        assert isSingleStack() && !isVirtual() : "type check";
        return -index;
    }

    @Override
    public int doubleStackIx() {
        assert isDoubleStack() && !isVirtual() : "type check";
        return -index;
    }

    @Override
    public Register asRegister() {
        assert location1 == location2 || location2 == Register.noreg;
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
        assert this.isRegister() && !this.isVirtualRegister();
        return location1.number;
    }

    @Override
    public int cpuRegnrLo() {
        assert this.isRegister() && !this.isVirtualRegister();
        return location1.number;
    }

    @Override
    public int cpuRegnrHi() {
        assert this.isRegister() && !this.isVirtualRegister();
        return location2.number;
    }

    /**
     * @return the minimum virtual register value
     */
    public static int virtualRegisterBase() {
        return Register.vregBase;
    }

    public void changeTo(LIROperand newValues) {
        assert newValues instanceof LIRLocation;
        final LIRLocation otherLocation = (LIRLocation) newValues;
        this.location1 = otherLocation.location1;
        this.location2 = otherLocation.location2;
        this.index = otherLocation.index;
        assert this.equals(otherLocation);
    }
}
