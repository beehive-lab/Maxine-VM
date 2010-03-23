/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.cps.dir;

import com.sun.max.vm.cps.dir.transform.*;

/**
 * Non-local exit from the current method.
 *
 * @author Bernd Mathiske
 */
public class DirReturn extends DirInstruction {

    private final DirValue returnValue;

    public DirReturn(DirValue returnValue) {
        this.returnValue = (returnValue == null) ? DirConstant.VOID : returnValue;
    }

    public DirValue returnValue() {
        return returnValue;
    }

    @Override
    public int hashCodeForBlock() {
        return super.hashCodeForBlock() ^ returnValue.hashCodeForBlock();
    }

    @Override
    public boolean isEquivalentTo(DirInstruction other, DirBlockEquivalence dirBlockEquivalence) {
        if (other instanceof DirReturn) {
            final DirReturn dirReturn = (DirReturn) other;
            return returnValue.equals(dirReturn.returnValue);
        }
        return false;
    }

    @Override
    public String toString() {
        return "return " + returnValue;
    }

    @Override
    public void acceptVisitor(DirVisitor visitor) {
        visitor.visitReturn(this);
    }
}

