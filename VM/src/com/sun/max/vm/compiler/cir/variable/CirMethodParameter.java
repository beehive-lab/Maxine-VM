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
 * A Java method parameter.
 *
 * @author Bernd Mathiske
 */
public class CirMethodParameter extends CirSlotVariable {

    public CirMethodParameter(int serial, Kind kind, int parameterIndex) {
        super(serial, kind, parameterIndex);
    }

    @Override
    public String toString() {
        String s = "p" + kind().character + slotIndex() + "-" + serial();
        if (CirNode.printingIds()) {
            s += "_" + id();
        }
        return s;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CirMethodParameter)) {
            return false;
        }
        final CirMethodParameter otherMethodParameter = (CirMethodParameter) other;
        return kind().character == otherMethodParameter.kind().character && slotIndex() == otherMethodParameter.slotIndex() && serial() == otherMethodParameter.serial();
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitMethodParameter(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitMethodParameter(this, scope);
    }

}
