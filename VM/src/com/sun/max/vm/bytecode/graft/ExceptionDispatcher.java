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

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.graft.BytecodeAssembler.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.ir.interpreter.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
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

    static final ClassMethodRefConstant safepointAndLoadExceptionObject = PoolConstantFactory.createClassMethodConstant(Classes.getDeclaredMethod(ExceptionDispatcher.class, "safepointAndLoadExceptionObject"));

    private final int position;

    /**
     * Thread local for passing the exception when interpreting with an {@link IrInterpreter}.
     */
    @PROTOTYPE_ONLY
    public static final ThreadLocal<Throwable> INTERPRETER_EXCEPTION = new ThreadLocal<Throwable>() {
        @Override
        public void set(Throwable value) {
            Throwable g = get();
            assert value == null || g == null;
            super.set(value);
        }
    };

    /**
     * Executes a safepoint and then gets the Throwable object from the
     * {@link VmThreadLocal#EXCEPTION_OBJECT} thread local.
     *
     * This method is only annotated to be never inlined so that it does something different
     * if being executed by an {@link IrInterpreter}.
     */
    @NEVER_INLINE
    public static Throwable safepointAndLoadExceptionObject() {
        if (MaxineVM.isPrototyping()) {
            return prototypeSafepointAndLoadExceptionObject();
        }
        Safepoint.safepoint();
        Throwable exception = UnsafeCast.asThrowable(VmThreadLocal.EXCEPTION_OBJECT.getVariableReference().toJava());
        VmThreadLocal.EXCEPTION_OBJECT.setVariableReference(null);
        return exception;
    }

    @PROTOTYPE_ONLY
    public static Throwable prototypeSafepointAndLoadExceptionObject() {
        Throwable throwable = INTERPRETER_EXCEPTION.get();
        INTERPRETER_EXCEPTION.set(null);
        return throwable;
    }

    private boolean isThrowable(BytecodeAssembler assembler, int classConstantIndex) {
        ClassConstant classAt = assembler.constantPool().classAt(classConstantIndex);
        final TypeDescriptor type = classAt.typeDescriptor();
        if (type.equals(JavaTypeDescriptor.THROWABLE)) {
            return true;
        }
        ClassActor catchType = classAt.resolve(assembler.constantPool(), classConstantIndex);
        if (!ClassRegistry.javaLangThrowableActor().isAssignableFrom(catchType)) {
            throw ErrorContext.verifyError("Catch type is not a subclass of Throwable in handler");
        }
        return false;
    }


    private void assemble(BytecodeAssembler assembler, ExceptionHandler handler) {
        assembler.pop();
        assembler.invokestatic(safepointAndLoadExceptionObject, 0, 1);
        ExceptionHandler h = handler;
        while (h != null) {

            // Skip the test and go straight to the handler address if this is a 'finally' block
            // or if the catch type is java.lang.Throwable
            if (h.catchTypeIndex() == 0 || isThrowable(assembler, h.catchTypeIndex())) {

                // Clear the modeled stack
                assembler.pop(Kind.REFERENCE);

                // Leave the exception object on the stack and go to the handler
                assembler.goto_(h.position());

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
            assembler.goto_(h.position());

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
        position = assembler.currentAddress();

        // Model the exception on the stack
        assert assembler.stack() == 0;
        assembler.push(Kind.REFERENCE);
        assemble(assembler, handler);
        assert assembler.stack() == 0;
    }

    /**
     * The byte code position of the dispatcher synthesized by this object.
     */
    int position() {
        return position;
    }
}
