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

import com.sun.c1x.ci.*;

/**
 * The <code>LIRLocation</code> class represents a LIROperand that is either a stack slot or a CPU register.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public final class LIRLocation extends LIROperand {

    public final CiRegister location1;
    public final CiRegister location2;
    public final int index;

    /**
     * Creates a new LIRLocation representing a CPU register.
     *
     * @param kind the kind of the location
     * @param number the register
     */
    LIRLocation(CiKind kind, CiRegister number) {
        super(kind);
        assert kind.size == 1 || (kind.size == -1 && number == CiRegister.None);
        assert number != null;
        this.location1 = number;
        this.location2 = CiRegister.None;
        index = 0;
    }

    /**
     * Creates a new LIRLocation representing either a virtual register or a stack location, negative indices
     * represent stack locations.
     *
     * @param kind the kind of the location
     * @param number the virtual register index or the stack location index if negative
     */
    LIRLocation(CiKind kind, int number) {
        super(kind);
        assert number != 0;
        this.location1 = CiRegister.None;
        this.location2 = CiRegister.None;
        this.index = number;
    }

    /**
     * Creates a new LIRLocation representing either a stack value or a CPU register.
     *
     * @param kind the kind of the location
     * @param location1 the number of the location
     * @param location2 the number of the second location
     */
    LIRLocation(CiKind kind, CiRegister location1, CiRegister location2) {
        super(kind);
        assert kind.size == 2;
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
            return l.kind == kind && l.location1 == location1 && l.location2 == location2 && l.index == index;
        }
        return false;
    }

    @Override
    public boolean isStack() {
        return index < 0;
    }

    @Override
    public int vregNumber() {
        assert index >= CiRegister.FirstVirtualRegisterNumber;
        return index;
    }

    @Override
    public boolean isSingleStack() {
        return isStack() && kind.sizeInSlots() == 1;
    }

    @Override
    public boolean isDoubleStack() {
        return isStack() && kind.sizeInSlots() == 2;
    }

    @Override
    public boolean isVariable() {
        return !isStack() && index >= CiRegister.FirstVirtualRegisterNumber;
    }

    @Override
    public boolean isFixedCpu() {
        return !isStack() && index < CiRegister.FirstVirtualRegisterNumber;
    }

    @Override
    public boolean isSingleCpu() {
        return !isStack() && location2 == CiRegister.None && location1.isCpu();
    }

    @Override
    public boolean isDoubleCpu() {
        return !isStack() && location2 != CiRegister.None && location1.isCpu() && location2.isCpu();
    }

    @Override
    public boolean isRegister() {
        return !isStack();
    }

    @Override
    public boolean isSingleXmm() {
        return !isStack() && location1.isXmm() && kind.sizeInSlots() == 1;
    }

    @Override
    public boolean isDoubleXmm() {
        return !isStack() && location1.isXmm() && kind.sizeInSlots() == 2;
    }

    @Override
    public int stackIndex() {
        assert (isSingleStack() || isDoubleStack()) && !this.isVariable() : "type check";
        return -index - 1;
    }

    @Override
    public int singleStackIndex() {
        assert isSingleStack() && !this.isVariable() : "type check";
        return -index - 1;
    }

    @Override
    public int doubleStackIndex() {
        assert isDoubleStack() && !this.isVariable() : "type check";
        return -index - 1;
    }

    @Override
    public CiRegister asRegister() {
        assert location1 == location2 || location2 == CiRegister.None;
        return this.location1;
    }

    @Override
    public CiRegister asRegisterLow() {
        return this.location1;
    }

    @Override
    public CiRegister asRegisterHigh() {
        return this.location2;
    }

    @Override
    public boolean isVirtualRegister() {
        return index != 0 && !this.isStack();
    }

    @Override
    public int cpuRegNumber() {
        assert this.isRegister() && !this.isVirtualRegister();
        return location1.number;
    }

    @Override
    public int cpuRegNumberLow() {
        assert this.isRegister() && !this.isVirtualRegister();
        return location1.number;
    }

    @Override
    public int cpuRegNumberHigh() {
        assert this.isRegister() && !this.isVirtualRegister();
        return location2.number;
    }
}
