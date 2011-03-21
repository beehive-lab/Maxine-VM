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
