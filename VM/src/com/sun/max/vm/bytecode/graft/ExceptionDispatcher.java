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

import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.graft.BytecodeAssembler.*;
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

   // private static final ClassMethodRefConstant reprotectGuardPage = PoolConstantFactory.createClassMethodConstant(Classes.getDeclaredMethod(Trap.class, "reprotectGuardPage", Throwable.class));
    private final int position;

    private boolean isThrowable(BytecodeAssembler assembler, int classConstantIndex) {
        try {
            final TypeDescriptor type = assembler.constantPool().classAt(classConstantIndex).typeDescriptor();
            return type.equals(JavaTypeDescriptor.forJavaClass(Throwable.class));
        } catch (LinkageError e) {
            // Defer linkage errors until the dispatcher is actually run or compiled
            return false;
        }
    }


    private void assemble(BytecodeAssembler assembler, ExceptionHandler handler) {

       // assembler.dup();
        //Before dispatching the exception handler always re-protect the guard page. In case of
        //stack-overflow exception due to an access to the guard page, it has to be protected again.
    //    assembler.invokestatic(reprotectGuardPage, 1, 0);
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
