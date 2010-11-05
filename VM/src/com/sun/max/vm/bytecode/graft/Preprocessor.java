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
package com.sun.max.vm.bytecode.graft;

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.jni.*;

/**
 * A bytecode preprocessor that uses bytecode rewriting to implement certain runtime features. The runtime features
 * implemented are the following:
 * <ul>
 * <li>{@linkplain NativeStubGenerator JNI stubs}</li>
 * <li>exception dispatching</li>
 * <li>synchronized methods</li>
 * </ul>
 *
 * @author Doug Simon
 */
public final class Preprocessor {

    private static final int TRACE_LEVEL = 6;

    private final ClassMethodActor classMethodActor;
    private ConstantPoolEditor constantPoolEditor;

    private ConstantPoolEditor constantPoolEditor() {
        if (constantPoolEditor == null) {
            constantPoolEditor = classMethodActor.holder().constantPool().edit();
        }
        return constantPoolEditor;
    }

    private void releaseConstantPoolEditor() {
        if (constantPoolEditor != null) {
            constantPoolEditor.release();
        }
        constantPoolEditor = null;
    }

    private Preprocessor(ClassMethodActor classMethodActor) {
        this.classMethodActor = classMethodActor;
    }

    public static CodeAttribute apply(ClassMethodActor classMethodActor, CodeAttribute originalCodeAttribute) {
        final Preprocessor preprocessor = new Preprocessor(classMethodActor);
        try {
            return preprocessor.run(originalCodeAttribute);
        } finally {
            preprocessor.releaseConstantPoolEditor();
        }

    }

    private CodeAttribute run(CodeAttribute originalCodeAttribute) {
        CodeAttribute codeAttribute = originalCodeAttribute;
        String reason = "";
        if (classMethodActor.isNative()) {
            assert codeAttribute == null;
            codeAttribute = new NativeStubGenerator(constantPoolEditor(), classMethodActor).codeAttribute();
            reason = "native";
        }

        if (codeAttribute == null) {
            return null;
        }

        if (codeAttribute.exceptionHandlerTable().length != 0) {
            codeAttribute = new ExceptionDispatchingPreprocessor(constantPoolEditor(), codeAttribute).codeAttribute();
            reason = reason + " dispatching";
        }

        if (classMethodActor.isSynchronized()) {
            codeAttribute = new SynchronizedMethodPreprocessor(constantPoolEditor(), classMethodActor, codeAttribute).codeAttribute();
            reason = reason + " synchronizedMethod";
        }

        if (codeAttribute != originalCodeAttribute) {

            String javaSignature = null;
            if (Trace.hasLevel(TRACE_LEVEL)) {
                javaSignature = classMethodActor.format("%r %H.%n(%p)");
                Trace.line(TRACE_LEVEL);
                Trace.line(TRACE_LEVEL, "bytecode preprocessed [" + reason + "]: " + javaSignature);
                if (!classMethodActor.isNative()) {
                    Trace.stream().println("--- BEFORE PREPROCESSING ---");
                    CodeAttributePrinter.print(Trace.stream(), originalCodeAttribute);
                }
                Trace.stream().println("--- AFTER PREPROCESSING ---");
                CodeAttributePrinter.print(Trace.stream(), codeAttribute);
            }

            ExceptionHandlerEntry.ensureExceptionDispatchersAreDisjoint(codeAttribute.exceptionHandlerTable());
        }
        return codeAttribute;
    }
}
