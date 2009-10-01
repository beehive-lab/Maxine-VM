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
package com.sun.max.vm.compiler.builtin;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.ir.*;

/**
 * A mechanism for allocating a block of memory within an activation frame for the lifetime of the frame.
 *
 * @author Doug Simon
 * @author Paul Caprioli
 */
public final class StackAllocate extends SpecialBuiltin {

    private StackAllocate() {
        super(null);
    }

    @Override
    public boolean isFoldable(IrValue[] arguments) {
        return false;
    }

    @Override
    public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
        visitor.visitStackAllocate(this, result, arguments);
    }

    public static final StackAllocate BUILTIN = new StackAllocate();

    /**
     * Allocates a requested block of memory within the current activation frame.
     * The allocated memory is reclaimed when the method returns.
     *
     * @param size bytes to allocate
     * @return the address of the allocated block
     */
    @BUILTIN(builtinClass = StackAllocate.class)
    public static native Address stackAllocate(int size);

}
