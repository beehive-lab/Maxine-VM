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

import static com.sun.c1x.bytecode.Bytecodes.*;

import com.sun.c1x.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

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
    public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
        visitor.visitStackAllocate(this, result, arguments);
    }

    public static final StackAllocate BUILTIN = new StackAllocate();

    /**
     * Allocates a requested block of memory within the current activation frame.
     * The allocated memory is reclaimed when the method returns.
     *
     * The allocation is for the lifetime of the method execution. That is, the compiler
     * reserves the space in the compiled size of the frame. As such, a failure
     * to allocate the requested space will result in a {@link StackOverflowError}
     * when the method's prologue is executed.
     *
     * @param size bytes to allocate. This must be a constant.
     * @return the address of the allocated block. <b>The contents of the block are uninitialized</b>.
     */
    @BUILTIN(value = StackAllocate.class)
    @INTRINSIC(ALLOCA)
    public static native Pointer stackAllocate(int size);

}
