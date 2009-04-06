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
package com.sun.max.vm.template.source;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.NonFoldableSnippet.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;

/**
 * Templates for implementation of bytecodes with a class operands that is resolved.
 *
 * @author Laurent Daynes
 */
@TEMPLATE(resolved = TemplateChooser.Resolved.YES)
public final class ResolvedBytecodeTemplateSource {

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.LDC, kind = KindEnum.REFERENCE)
    public static void rldc(Object value) {
        JitStackFrameOperation.pushReference(value);
    }

    @INLINE
    public static void checkcast(ClassActor classActor) {
        Snippet.CheckCast.checkCast(classActor, JitStackFrameOperation.peekReference(0));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INSTANCEOF)
    public static void instanceof_(ClassActor classActor) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, UnsafeLoophole.booleanToByte(Snippet.InstanceOf.instanceOf(classActor, object)));
    }

    @INLINE
    public static void anewarray(ArrayClassActor arrayClassActor) {
        final int length = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeReference(0, NonFoldableSnippet.CreateReferenceArray.noninlineCreateReferenceArray(arrayClassActor, length));
    }

    @INLINE
    public static void multianewarray(ArrayClassActor arrayClassActor, int[] lengthsShared) {
        // Need to use an unsafe cast to remove the checkcast inserted by javac as that causes this
        // template to have a reference literal in its compiled form.
        final int[] lengths = UnsafeLoophole.cast(lengthsShared.clone());
        final int numberOfDimensions = lengths.length;
        for (int i = 1; i <= numberOfDimensions; i++) {
            final int length = JitStackFrameOperation.popInt();
            Snippet.CheckArrayDimension.checkArrayDimension(length);
            ArrayAccess.setInt(lengths, numberOfDimensions - i, length);
        }
        JitStackFrameOperation.pushReference(CreateMultiReferenceArray.createMultiReferenceArray(arrayClassActor, lengths));
    }
}
