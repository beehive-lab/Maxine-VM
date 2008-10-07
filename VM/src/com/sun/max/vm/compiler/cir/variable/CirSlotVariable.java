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
 * A variable that refers to a certain thread stack position,
 * i.e. either a local or an operand stack slot in the JVM spec sense
 *
 * @author Bernd Mathiske
 */
public abstract class CirSlotVariable extends CirVariable {

    private final int _slotIndex;

    public CirSlotVariable(int serial, Kind kind, int slotIndex) {
        super(serial, kind);
        _slotIndex = slotIndex;
    }

    public int slotIndex() {
        return _slotIndex;
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitSlotVariable(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitSlotVariable(this, scope);
    }

}
