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
