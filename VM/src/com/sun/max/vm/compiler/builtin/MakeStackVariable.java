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
import com.sun.max.vm.reference.*;

/**
 * A mechanism for forcing a value onto the stack and obtaining the address of the stack slot. If the first parameter to
 * this built-in is a lvalue (i.e. something that can be assigned to), then the compiler must ensure the variable is
 * allocated on the stack. Otherwise, it must make a copy of the rvalue (i.e. a constant) and ensure the copy is
 * allocated on the stack.
 *
 * @author Doug Simon
 */
public class MakeStackVariable extends SpecialBuiltin {

    protected MakeStackVariable() {
        super(null);
    }

    @Override
    public boolean isFoldable(IrValue[] arguments) {
        return false;
    }

    @Override
    public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
        visitor.visitMakeStackVariable(this, result, arguments);
    }

    public static final MakeStackVariable BUILTIN = new MakeStackVariable();

    /**
     * Forces the register allocator to put a given value on the stack (as opposed to in a register). That stack slot is
     * pinned for the remainder of the method (i.e. it will not be reused by the register allocator for another
     * variable).
     *
     * @param value a value that is to be stack resident. If {@code value} is a lvalue, then the register allocator
     *            guarantees that it will be allocated on the stack. Otherwise, a stack slot is allocated and
     *            initialized with {@code value}.
     * @return the address of the stack slot where {@code value} resides
     */
    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(int value);

    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(byte value);

    /**
     * @see #makeStackVariable(int)
     */
    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(float value);

    /**
     * @see #makeStackVariable(int)
     */
    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(long value);

    /**
     * @see #makeStackVariable(int)
     */
    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(double value);

    /**
     * @see #makeStackVariable(int)
     */
    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(Reference value);

    /**
     * @see #makeStackVariable(int)
     */
    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(Word value);
}
