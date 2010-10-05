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
package com.sun.max.vm.cps.cir.variable;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.type.*;

/**
 * A position in the Java operand stack.
 *
 * @author Bernd Mathiske
 */
public class CirStackVariable extends CirSlotVariable {

    public CirStackVariable(int serial, Kind kind, int stackSlotIndex) {
        super(serial, kind, stackSlotIndex);
    }

    @Override
    public String toString() {
        return "s" + kind().character + slotIndex() + "-" + serial();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CirStackVariable)) {
            return false;
        }
        final CirStackVariable otherStackVariable = (CirStackVariable) other;
        return kind().character == otherStackVariable.kind().character
            && slotIndex() == otherStackVariable.slotIndex()
            && serial() == otherStackVariable.serial();
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitStackVariable(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitStackVariable(this, scope);
    }

}
