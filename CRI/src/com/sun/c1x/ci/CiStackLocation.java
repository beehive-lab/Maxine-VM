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
 * This class represents a location on the stack, either the caller or callee's stack frame.
 *
 * @author Ben L. Titzer
 */
public final class CiStackLocation extends CiLocation {

    public final int stackOffset;
    public final byte stackSize;
    public final boolean callerFrame;

    public CiStackLocation(CiKind kind, int stackOffset, int stackSize, boolean callerFrame) {
        super(kind);
        assert stackSize >= 0 && stackSize <= 8;
        this.stackOffset = stackOffset;
        this.stackSize = (byte) stackSize;
        this.callerFrame = callerFrame;
    }

    public int hashCode() {
        return kind.ordinal() + stackOffset + (callerFrame ? 91 : 83);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof CiStackLocation) {
            CiStackLocation l = (CiStackLocation) o;
            return l.kind == kind && l.stackOffset == stackOffset && l.stackSize == stackSize && l.callerFrame == callerFrame;
        }
        return false;
    }

    public String toString() {
        return (callerFrame ? "+" : "-") + stackOffset + ":" + kind;
    }
    
    public int stackOffset() {
        return stackOffset;
    }

    public int stackSize() {
        return stackSize;
    }

    public boolean isCallerFrame() {
        return callerFrame;
    }
}
