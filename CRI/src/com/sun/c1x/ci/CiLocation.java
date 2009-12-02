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

/**
 * This class represents either a register or a stack slot and is used when communicating the
 * locations of parameters for calling conventions across the compiler/runtime interface.
 *
 * @author Thomas Wuerthinger
 */
public final class CiLocation extends CiValue {

    /**
     * Singleton object representing an invalid location.
     */
    public static final CiLocation InvalidLocation = new CiLocation(CiKind.Illegal, null);

    public final CiRegister first;
    public final CiRegister second;
    public final int stackOffset;
    public final int stackSize;
    public final boolean callerStack;

    /**
     * Location representing a single register.
     *
     * @param kind the kind of the new location
     * @param register the register representing the new location
     */
    public CiLocation(CiKind kind, CiRegister register) {
        super(kind);
        this.first = register;
        this.second = null;
        this.stackOffset = 0;
        this.stackSize = 0;
        this.callerStack = false;
    }

    public CiLocation(CiKind kind, CiRegister first, CiRegister second) {
        super(kind);
        this.first = first;
        this.second = second;
        this.stackOffset = 0;
        this.stackSize = 0;
        this.callerStack = false;
    }

    public CiLocation(CiKind kind, int stackOffset, int stackSize, boolean callerStack) {
        super(kind);
        this.first = null;
        this.second = null;
        this.stackOffset = stackOffset;
        this.stackSize = stackSize;
        this.callerStack = callerStack;
    }

    public boolean isSingleRegister() {
        return second == null && first != null;
    }

    public boolean isDoubleRegister() {
        return second != null;
    }

    public boolean isRegister() {
        return first != null;
    }

    public boolean isStackOffset() {
        return !isRegister() && isValid();
    }

    public boolean isValid() {
        return this != InvalidLocation;
    }

    @Override
    public int hashCode() {
        return kind.hashCode() * 29 + (first == null ? 0 : first.hashCode()) * 13 + (second == null ? 0 : second.hashCode()) * 7 + stackOffset;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof CiLocation) {
            final CiLocation other = (CiLocation) obj;
            return other.kind == kind && other.first == first && other.second == second && other.stackOffset == stackOffset && other.stackSize == stackSize && other.callerStack == callerStack;
        }

        return false;
    }

    @Override
    public String toString() {
        if (this == InvalidLocation) {
            return "invalid";
        } else if (isSingleRegister()) {
            return first.name;
        } else if (isDoubleRegister()) {
            return first.name + "+" + second.name;
        }
        return "@" + stackOffset + (callerStack ? "(caller)" : "(callee)");
    }
}
