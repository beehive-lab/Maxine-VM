/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.compiler.builtin;

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
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
    @BUILTIN(value = MakeStackVariable.class)
    @INTRINSIC(ALLOCSTKVAR)
    public static native Pointer makeStackVariable(int value);

    @BUILTIN(value = MakeStackVariable.class)
    @INTRINSIC(ALLOCSTKVAR)
    public static native Pointer makeStackVariable(byte value);

    /**
     * @see #makeStackVariable(int)
     */
    @BUILTIN(value = MakeStackVariable.class)
    @INTRINSIC(ALLOCSTKVAR)
    public static native Pointer makeStackVariable(float value);

    /**
     * @see #makeStackVariable(int)
     */
    @BUILTIN(value = MakeStackVariable.class)
    @INTRINSIC(ALLOCSTKVAR)
    public static native Pointer makeStackVariable(long value);

    /**
     * @see #makeStackVariable(int)
     */
    @BUILTIN(value = MakeStackVariable.class)
    @INTRINSIC(ALLOCSTKVAR)
    public static native Pointer makeStackVariable(double value);

    /**
     * @see #makeStackVariable(int)
     */
    @BUILTIN(value = MakeStackVariable.class)
    @INTRINSIC(ALLOCSTKVAR)
    public static native Pointer makeStackVariable(Reference value);

    /**
     * @see #makeStackVariable(int)
     */
    @BUILTIN(value = MakeStackVariable.class)
    @INTRINSIC(ALLOCSTKVAR)
    public static native Pointer makeStackVariable(Word value);
}
