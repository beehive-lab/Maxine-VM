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

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.interpreter.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A CirProcedure that represents a Java routine, i.e. a builtin or a method.
 *
 * "To fold" is a generalization of "to constant-fold". It means "to evaluate", typically "to meta-evaluate".
 */
public interface CirRoutine extends Stoppable {

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

    int reasonsMayStop();

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

        /**
         *
         * @param throwable
         * @param arguments
         * @return
         */
        public static CirCall createExceptionCall(Throwable throwable, CirValue[] arguments) {
            final CirValue exceptionContinuation = arguments[arguments.length - 1];
            return new CirCall(exceptionContinuation, CirConstant.fromObject(throwable));
        }

        /**
         * Invokes a given method or constructor with a given set of CIR values as the arguments.
         *
         * @param methodActor the method to be invoked. If {@code methodActor} is a
         *            {@linkplain MethodActor#isInstanceInitializer() constructor}, then the first element of {@code
         *            cirArguments} must be an instance of {@code methodActor}'s holder. The fields of this instance
         *            will be {@linkplain Objects#copy(Object, Object) updated} from the fields of the instance created
         *            and returned by the constructor invocation.
         * @param cirArguments the arguments that will be converted to {@link Value} objects and passed to the
         *            invocation. The last two arguments in this array (i.e. the normal and exception continuation
         *            arguments) are truncated.
         * @return the result of the invocation
         * @throws CirFoldingException if {@code methodActor} triggers an exception while being invoked
         */
        public static Value evaluate(MethodActor methodActor, CirValue[] cirArguments) throws CirFoldingException {
            try {
                if (methodActor.isInstanceInitializer()) {
                    final CirValue[] constructorArguments = Arrays.subArray(cirArguments, 1);
                    final Object uninitializedObject = cirArguments[0].value().asObject();
                    try {
                        final Object initializedObject = methodActor.invokeConstructor(cirArgumentsToValues(constructorArguments)).asObject();
                        Objects.copy(initializedObject, uninitializedObject);
                        return VoidValue.VOID;
                    } catch (InstantiationException instantiationException) {
                        throw ProgramError.unexpected("could not instantiate an instance of " + uninitializedObject);
                    }
                }
                return methodActor.invoke(cirArgumentsToValues(cirArguments));
            } catch (InvocationTargetException invocationTargetException) {
                throw new CirFoldingException(invocationTargetException.getCause());
            } catch (IllegalAccessException illegalAccessException) {
                throw ProgramError.unexpected("could not access method for invocation: " + methodActor);
            }
        }

        /**
         * Folds a given method or constructor by invoking it with a given set of arguments.
         *
         * @param methodActor the method to be folded
         * @param cirArguments the arguments with which to invoke {@code methodActor} as well as the normal and
         *            exception continuations which are in the second to last and last position of {@code cirArguments}
         *            respectively
         * @return the result of the invocation as a CIR call
         * @throws CirFoldingException if an exception occurs while invoking {@code methodActor}
         */
        public static CirCall fold(MethodActor methodActor, CirValue[] cirArguments) throws CirFoldingException {
            final Value result = evaluate(methodActor, cirArguments);
            final CirValue normalContinuation = cirArguments[cirArguments.length - 2];
            if (result == VoidValue.VOID) {
                return new CirCall(normalContinuation, CirCall.NO_ARGUMENTS);
            }
            return new CirCall(normalContinuation, new CirConstant(result));
        }

        /**
         * Folds a given CIR routine by invoking its associated {@linkplain CirRoutine#foldingMethodActor() folding}
         * method with a given set of arguments.
         *
         * @param cirRoutine the CIR routine to be folded
         * @param cirArguments the arguments with which to fold {@code cirRoutine} as well as the normal and
         *            exception continuations which are in the second to last and last position of {@code cirArguments}
         *            respectively
         * @return the result of the invocation as a CIR call
         * @throws CirFoldingException if an exception occurs while invoking {@code methodActor}
         */
        public static CirCall fold(CirRoutine cirRoutine, CirValue[] cirArguments) throws CirFoldingException {
            return fold(cirRoutine.foldingMethodActor(), cirArguments);
        }
    }
}
