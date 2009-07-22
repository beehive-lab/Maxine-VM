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
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;

/**
 * Templates for implementation of bytecodes with a class operand that is initialized.
 *
 * @author Laurent Daynes
 * @author Bernd Mathiske
 */
@TEMPLATE(initialized = TemplateChooser.Initialized.YES)
public final class InitializedBytecodeTemplateSource {

    /**
     * Implementation of template for the NEW bytecode. The template is to be used in cases where the class instantiated is already initialized.
     * depends on it to use the ClassActor for the desired class.
     */
    @BYTECODE_TEMPLATE(bytecode = Bytecode.NEW)
    public static void new_(ClassActor classActor) {
        JitStackFrameOperation.pushReference(NoninlineTemplateRuntime.noninlineNew(classActor));
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.VOID)
    public static void invokestatic() {
        JitStackFrameOperation.directCallVoid();
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.FLOAT)
    public static void invokestaticFloat() {
        JitStackFrameOperation.directCallFloat();
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.LONG)
    public static void invokestaticLong() {
        JitStackFrameOperation.directCallLong();
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.DOUBLE)
    public static void invokestaticDouble() {
        JitStackFrameOperation.directCallDouble();
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.WORD)
    public static void invokestaticWord() {
        JitStackFrameOperation.directCallWord();
    }

}
