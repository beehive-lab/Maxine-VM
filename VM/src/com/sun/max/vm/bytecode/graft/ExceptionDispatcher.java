/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.graft.BytecodeAssembler.Label;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * A {@code ExceptionDispatcher} instance synthesizes bytecodes to dispatch
 * an exception to a {@linkplain ExceptionHandler handler} in a given
 * list of handlers. The dispatcher rethrows the exception if it does not match
 * one of the handlers.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class ExceptionDispatcher {

    private final int bci;

    private boolean isThrowable(BytecodeAssembler assembler, int classConstantIndex) {
        ClassConstant classRef = assembler.constantPool().classAt(classConstantIndex);
        final TypeDescriptor type = classRef.typeDescriptor();
        if (type.equals(JavaTypeDescriptor.THROWABLE)) {
            return true;
        }

        // Prevent loading types during image building: can safely assume that the type
        // in an exception handler is indeed a subclass of Throwable.
        ClassActor catchType = null;
        if (MaxineVM.isHosted()) {
            if (classRef.isResolvableWithoutClassLoading(assembler.constantPool())) {
                catchType = classRef.resolve(assembler.constantPool(), classConstantIndex);
            }
        } else {
            catchType = classRef.resolve(assembler.constantPool(), classConstantIndex);
        }

        if (catchType != null && !ClassRegistry.THROWABLE.isAssignableFrom(catchType)) {
            throw ErrorContext.verifyError("Catch type is not a subclass of Throwable in handler");
        }
        return false;
    }

    private void assemble(BytecodeAssembler assembler, ExceptionHandler handler) {
        ExceptionHandler h = handler;
        while (h != null) {

            // Skip the test and go straight to the handler address if this is a 'finally' block
            // or if the catch type is java.lang.Throwable
            if (h.catchTypeIndex() == 0 || isThrowable(assembler, h.catchTypeIndex())) {

                // Clear the modeled stack
                assembler.pop(Kind.REFERENCE);

                // Leave the exception object on the stack and go to the handler
                assembler.goto_(h.bci());

                // Ignore subsequent handlers (should never be any anyway)
                return;
            }

            // Dup exception for subsequent handler or as input to handler
            assembler.dup();

            // Test if exception is an instance of the handler's catch type
            assembler.instanceof_(h.catchTypeIndex());

            // If so, cast the exception to the handler's catch type and go to the handler, otherwise test the
            // subsequent handler
            final Label testNextHandler = assembler.newLabel();
            assembler.ifeq(testNextHandler);
            assembler.checkcast(h.catchTypeIndex());
            assembler.goto_(h.bci());

            testNextHandler.bind();
            h = h.next();
        }

        // No handlers were matched so rethrow the exception
        assembler.athrow();
    }

    /**
     * Creates a synthesizer to dispatch an exception to the first handler in the list headed by {@code handler} whose
     * {@linkplain ExceptionHandler#_catchTypeConstant catch type} matches the exception.
     *
     * @param assembler
     *            the assembler used for synthesizing the dispatcher bytecode
     */
    ExceptionDispatcher(BytecodeAssembler assembler, ExceptionHandler handler) {
        bci = assembler.currentAddress();

        // Model the exception on the stack
        assert assembler.stack() == 0;
        assembler.push(Kind.REFERENCE);
        assemble(assembler, handler);
        assert assembler.stack() == 0;
    }

    /**
     * The BCI of the dispatcher synthesized by this object.
     */
    int bci() {
        return bci;
    }
}
