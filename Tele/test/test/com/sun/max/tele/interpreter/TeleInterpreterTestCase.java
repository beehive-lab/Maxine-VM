/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.tele.interpreter;

import static com.sun.max.vm.classfile.constant.SymbolTable.*;

import java.lang.reflect.*;

import test.com.sun.max.vm.cps.*;

import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.value.*;

/**
 *
 * @author Doug Simon
 */

public abstract class TeleInterpreterTestCase extends CompilerTestCase<ActorIrMethod> {
    public TeleInterpreterTestCase() {
        super();
    }

    public TeleInterpreterTestCase(String name) {
        super(name);
    }

    protected ActorIrMethod getIrMethod(String methodName) {
        Class thisClass = defaultDeclaringClass();
        ClassMethodActor classMethodActor;
        do {
            classMethodActor = ClassActor.fromJava(thisClass).findLocalClassMethodActor(makeSymbol(methodName), null);
            thisClass = thisClass.getSuperclass();
        } while (classMethodActor == null && thisClass != null);
        if (classMethodActor == null) {
            fail("No such method: " + defaultDeclaringClass().getName() + "." + methodName);
        }
        return new ActorIrMethod(classMethodActor);
    }

    protected abstract Class defaultDeclaringClass();

    protected abstract TeleVM teleVM();

    /**
     * Executes the first method found in {@link #defaultDeclaringClass()} named {@code methodName} with {@code arguments}.
     *
     * @return the result of the execution
     * @throws TeleInterpreterException if an uncaught exception occurs during execution of the method
     */
    protected Value executeWithException(String methodName, Value... arguments) throws InvocationTargetException {
        final ActorIrMethod method = getIrMethod(methodName);
        return executeWithException(method, arguments);
    }

    /**
     * Executes the first method found in {@link #defaultDeclaringClass()} named {@code methodName} with {@code arguments}.
     * The method to be executed is not expected to raise an exception and if it does, the test immediately
     * {@linkplain #fail() fails}.
     *
     * @return the result of the execution
     */
    protected Value execute(String methodName, Value... arguments) {
        return execute(getIrMethod(methodName), arguments);
    }

    /**
     * Executes the first method found in {@link #defaultDeclaringClass()} named {@code methodName} with {@code arguments}.
     * The method to be executed is expected to raise an exception of type assignable to {@code expectedExceptionType}.
     * If it does not, the test immediately {@linkplain #fail() fails}.
     */
    protected void executeWithExpectedException(String methodName, Class<? extends Throwable> expectedExceptionType, Value... arguments) {
        executeWithExpectedException(getIrMethod(methodName), expectedExceptionType, arguments);
    }
}
