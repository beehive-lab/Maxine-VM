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
package test.com.sun.max.tele.interpreter;

import static com.sun.max.vm.classfile.constant.SymbolTable.*;

import java.lang.reflect.*;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.ir.*;
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
