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

import com.sun.max.vm.compiler.*;

/**
 * @author Bernd Mathiske
 */
public abstract class PointerAtomicBuiltin extends PointerBuiltin {

    PointerAtomicBuiltin() {
        super();
    }

    @Override
    public int reasonsMayStop() {
        return Stoppable.NULL_POINTER_CHECK;
    }

    public static class CompareAndSwapInt extends PointerAtomicBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitCompareAndSwapInt(this, result, arguments);
        }

        public static final CompareAndSwapInt BUILTIN = new CompareAndSwapInt();
    }

    public static class CompareAndSwapIntAtIntOffset extends PointerAtomicBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitCompareAndSwapIntAtIntOffset(this, result, arguments);
        }

        public static final CompareAndSwapIntAtIntOffset BUILTIN = new CompareAndSwapIntAtIntOffset();
    }

    public static class CompareAndSwapWord extends PointerAtomicBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitCompareAndSwapWord(this, result, arguments);
        }

        public static final CompareAndSwapWord BUILTIN = new CompareAndSwapWord();
    }

    public static class CompareAndSwapWordAtIntOffset extends PointerAtomicBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitCompareAndSwapWordAtIntOffset(this, result, arguments);
        }

        public static final CompareAndSwapWordAtIntOffset BUILTIN = new CompareAndSwapWordAtIntOffset();
    }

    public static class CompareAndSwapReference extends PointerAtomicBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitCompareAndSwapReference(this, result, arguments);
        }

        public static final CompareAndSwapReference BUILTIN = new CompareAndSwapReference();
    }

    public static class CompareAndSwapReferenceAtIntOffset extends PointerAtomicBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitCompareAndSwapReferenceAtIntOffset(this, result, arguments);
        }

        public static final CompareAndSwapReferenceAtIntOffset BUILTIN = new CompareAndSwapReferenceAtIntOffset();
    }
}
