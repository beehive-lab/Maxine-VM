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
package com.sun.max.vm.cps.cir.optimize;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.ir.interpreter.*;
import com.sun.max.vm.value.*;

/**
 * A builtin or a method that can be folded (aka meta-evaluated).
 *
 * "Folded" means rewritten in a strictly monotonic direction,
 * i.e. repeated folding does come to a deterministic end.
 * So this does not necessarily mean "'constant'-folded" and
 * on the other hand, it is neither as general as merely "rewritten".
 *
 * @author Bernd Mathiske
 */
public interface CirFoldable {

    /**
     * This CirRoutine can be executed by reflective invocation of this method.
     * It can can thus be meta-evaluated by the {@link CirOptimizer}, the {@link CirInterpreter} or both.
     *
     * @return the method actor that meta-evaluates this routine
     */
    MethodActor foldingMethodActor();

    boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments);

    CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException;

    public static final class Static {
        private Static() {
        }

        public static Value[] cirArgumentsToValues(CirValue[] cirArguments, MethodActor methodActor) {
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
                    final CirValue[] constructorArguments = Arrays.copyOfRange(cirArguments, 1, cirArguments.length);
                    final Object uninitializedObject = cirArguments[0].value().asObject();
                    try {
                        final Object initializedObject = methodActor.invokeConstructor(cirArgumentsToValues(constructorArguments, methodActor)).asObject();
                        Objects.copy(initializedObject, uninitializedObject);
                        return VoidValue.VOID;
                    } catch (InstantiationException instantiationException) {
                        throw ProgramError.unexpected("could not instantiate an instance of " + uninitializedObject);
                    }
                }
                return methodActor.invoke(cirArgumentsToValues(cirArguments, methodActor));
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
        public static CirCall fold(CirFoldable cirRoutine, CirValue[] cirArguments) throws CirFoldingException {
            return fold(cirRoutine.foldingMethodActor(), cirArguments);
        }
    }
}
