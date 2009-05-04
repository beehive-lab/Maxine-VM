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
package com.sun.max.vm.compiler.cir.variable;

import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.type.*;

/**
 * A local variable in the sense of the JVM spec that is not a method parameter.
 *
 * @author Bernd Mathiske
 */
public class CirLocalVariable extends CirSlotVariable {

    public CirLocalVariable(int serial, Kind kind, int localIndex) {
        super(serial, kind, localIndex);
    }

    @Override
    public String toString() {
        String s = "l" + kind().character() + slotIndex() + "-" + serial();
        if (CirNode.printingIds()) {
            s += "_" + id();
        }
        return s;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CirLocalVariable)) {
            return false;
        }
        final CirLocalVariable otherLocalVariable = (CirLocalVariable) other;
        return kind().character() == otherLocalVariable.kind().character()
            && slotIndex() == otherLocalVariable.slotIndex()
            && serial() == otherLocalVariable.serial();
    }

    @Override
    public final void acceptVisitor(CirVisitor visitor) {
        visitor.visitLocalVariable(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitLocalVariable(this, scope);
    }

}
