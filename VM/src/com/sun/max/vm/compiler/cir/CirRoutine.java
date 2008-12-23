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
package com.sun.max.vm.compiler.cir;

import java.lang.reflect.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.interpreter.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A CirProcedure that represents a Java routine, i.e. a builtin or a method.
 *
 * "To fold" is a generalization of "to constant-fold". It means "to evaluate", typically "to meta-evaluate".
 */
public interface CirRoutine {

    String name();

    Kind resultKind();

    Kind[] parameterKinds();

    /**
     * This CirRoutine can be executed by reflective invocation of this method.
     * It can can thus be meta-evaluated by the {@link CirOptimizer}, the {@link CirInterpreter} or both.
     *
     * @return the method actor that meta-evaluates this routine
     */
    MethodActor foldingMethodActor();

    /**
     * @return whether a call to this CirRoutine needs to be accompanied by a Java frame descriptor
     */
    boolean needsJavaFrameDescriptor();

    /**
     * @return whether this operator can be raise a run-time exception and thus requires an
     * exception handler.  Operators that cannot throw exceptions get a void exception handler.
     */
    boolean mayThrowException();


    public static final class Static {
        private Static() {
        }

        public static Value[] cirArgumentsToValues(CirValue[] cirArguments) {
            final int numberOfArguments = cirArguments.length - 2; // clip the continuations
            final Value[] values = new Value[numberOfArguments];
            for (int i = 0; i < numberOfArguments; i++) {
                values[i] = cirArguments[i].value();
            }
            return values;
        }

        public static CirCall createExceptionCall(Throwable throwable, CirValue[] arguments) {
            final CirValue exceptionContinuation = arguments[arguments.length - 1];
            return new CirCall(exceptionContinuation, CirConstant.fromObject(throwable));
        }

        public static Value evaluate(MethodActor foldingMethodActor, CirValue[] cirArguments) throws InvocationTargetException {
            try {
                return foldingMethodActor.invoke(cirArgumentsToValues(cirArguments));
            } catch (IllegalAccessException illegalAccessException) {
                throw ProgramError.unexpected("could not access method for invocation: " + foldingMethodActor);
            }
        }

        private static Value evaluate(Constructor foldingConstructor, CirValue[] cirArguments) throws InvocationTargetException {
            try {
                return MethodActor.fromJavaConstructor(foldingConstructor).invokeConstructor(cirArgumentsToValues(cirArguments));
            } catch (InstantiationException instantiationException) {
                throw ProgramError.unexpected("error reflectively invoking constructor " + foldingConstructor, instantiationException);
            } catch (IllegalAccessException illegalAccessException) {
                throw ProgramError.unexpected("error reflectively invoking constructor " + foldingConstructor, illegalAccessException);
            }
        }

        public static CirCall fold(MethodActor foldingMethodActor, CirValue[] cirArguments) {
            try {
                final Value result = evaluate(foldingMethodActor, cirArguments);
                final CirValue normalContinuation = cirArguments[cirArguments.length - 2];
                if (result == VoidValue.VOID) {
                    return new CirCall(normalContinuation);
                }
                return new CirCall(normalContinuation, new CirConstant(result));
            } catch (InvocationTargetException invocationTargetException) {
                return createExceptionCall(invocationTargetException.getTargetException(), cirArguments);
            }
        }

        public static CirCall fold(Constructor foldingConstructor, CirValue[] cirArguments) {
            try {
                final Value result = evaluate(foldingConstructor, cirArguments);
                final CirValue normalContinuation = cirArguments[cirArguments.length - 2];
                if (result == VoidValue.VOID) {
                    return new CirCall(normalContinuation);
                }
                return new CirCall(normalContinuation, new CirConstant(result));
            } catch (InvocationTargetException invocationTargetException) {
                return createExceptionCall(invocationTargetException.getTargetException(), cirArguments);
            }
        }

        public static CirCall fold(CirRoutine cirRoutine, CirValue[] cirArguments) {
            return fold(cirRoutine.foldingMethodActor(), cirArguments);
        }
    }
}
