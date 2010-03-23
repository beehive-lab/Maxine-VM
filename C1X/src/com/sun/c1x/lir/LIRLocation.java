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
 * The {@code LIRLocation} class represents a LIROperand that is either a {@linkplain #isStack() stack slot},
 * a {@linkplain #isRegister() CPU register} or a {@linkplain #isVariable() variable}.
 * Each definition and use of a variable must be given a physical location by the register
 * allocator.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public final class LIRLocation extends LIROperand {

    public final CiRegister register1;

    /**
     * The index for this location. A negative value indicates a {@linkplain #isStack() stack location},
     * a value of 0 indicates a {@linkplain #isRegister() physical CPU register} and a value greater
     * or equals to {@link CiRegister#LowestVirtualRegisterNumber} indicates a {@linkplain #isVariable() virtual register}.
     */
    public final int index;

    /**
     * Creates a new LIRLocation representing a CPU register.
     *
     * @param kind the kind of the location
     * @param number the register
     */
    LIRLocation(CiKind kind, CiRegister number) {
        super(kind);
        assert kind.jvmSlots != -1 || number == CiRegister.None;
        assert number != null;
        this.register1 = number;
        index = 0;
    }

    /**
     * Creates a new location representing either a variable or a stack location, with negative indexes representing
     * stack locations.
     *
     * @param kind the kind of the variable or a stack location
     * @param number the variable index (if greater than {@link CiRegister#LowestVirtualRegisterNumber}) or the stack
     *            location index (if negative)
     */
    LIRLocation(CiKind kind, int number) {
        super(kind);
        assert number < 0 || number >= CiRegister.LowestVirtualRegisterNumber;
        this.register1 = CiRegister.None;
        this.index = number;
    }

    @Override
    public int hashCode() {
        return register1.number + index;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LIRLocation) {
            LIRLocation l = (LIRLocation) o;
            return l.kind == kind && l.register1 == register1 && l.index == index;
        }
        return false;
    }

    @Override
    public boolean isStack() {
        return index < 0;
    }

    @Override
    public int variableNumber() {
        assert index >= CiRegister.LowestVirtualRegisterNumber : illegalOperation("variableNumber()");
        return index;
    }

    @Override
    public boolean isSingleStack() {
        final boolean kindFitsInWord = true;
        return isStack() && kindFitsInWord;
    }

    @Override
    public boolean isDoubleStack() {
        return isStack() && kind.sizeInSlots() == 2;
    }

    @Override
    public boolean isVariable() {
        return index >= CiRegister.LowestVirtualRegisterNumber;
    }

    @Override
    public boolean isRegister() {
        return index == 0;
    }

//    @Override
//    public boolean isSingleRegister() {
//        // TODO: Modify for 32-bit
//        return register1.isCpu() && register1.width >= 32;
//    }

//    @Override
//    public boolean isDoubleRegister() {
//        // TODO: Modify for 32-bit
//        return register1.isCpu() && register1.width >= 64;
//    }

    @Override
    public boolean isVariableOrRegister() {
        return !isStack();
    }

//    @Override
//    public boolean isSingleXmm() {
//        // TODO: Modify for 32-bit
//        return register1.isXmm() && register1.width >= 32;
//    }
//
//    @Override
//    public boolean isDoubleXmm() {
//        // TODO: Modify for 32-bit
//        return register1.isXmm() && register1.width >= 64;
//    }

    @Override
    public int stackIndex() {
        assert isStack() : illegalOperation("stackIndex()");
        return -index - 1;
    }

    @Override
    public int singleStackIndex() {
        assert isSingleStack() : illegalOperation("singleStackIndex()");
        return -index - 1;
    }

    @Override
    public int doubleStackIndex() {
        assert isDoubleStack() : illegalOperation("doubleStackIndex()");
        return -index - 1;
    }

    @Override
    public CiRegister asRegister() {
        assert isRegister() : illegalOperation("asRegister()");
        return this.register1;
    }

    @Override
    public int registerNumber() {
        assert index == 0 : illegalOperation("cpuRegNumber()");
        return register1.number;
    }
}
