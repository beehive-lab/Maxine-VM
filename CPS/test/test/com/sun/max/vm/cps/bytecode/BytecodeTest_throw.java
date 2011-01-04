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
package test.com.sun.max.vm.cps.bytecode;

import java.io.*;
import java.lang.reflect.*;

import test.com.sun.max.vm.cps.*;

import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 * @author Hiroshi Yamauchi
 */
public abstract class BytecodeTest_throw<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_throw(String name) {
        super(name);
    }

    private static final class TestException extends Exception {
        // an arbitrary exception type, distinct from any other throwable

        public final int trigger;

        public TestException(int trigger) {
            this.trigger = trigger;
        }
    }

    private int perform_catchNull() {
        try {
            final RuntimeException t = null;
            throw t;
        } catch (NullPointerException nullPointerException) {
            return 123;
        }
    }

    public void test_catchNull() {
        final Method_Type method = compileMethod("perform_catchNull");
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void athrow() {
                confirmPresence();
            }
        };
        assertTrue(perform_catchNull() == 123);
        final Value result = executeWithReceiver(method);
        assertTrue(result.asInt() == 123);
    }

    private int perform_controlFlow(int a, TestException exception) {
        int result = 0;
        if (a > 1) {
            result = 1;
        } else {
            result = 2;
        }
        try {
            if (a > 0) {
                result += 10;
            } else {
                result += 20;
            }
            perform_athrow(exception);
        } catch (TestException testException) {
            return 123;
        }
        return result;
    }

    public void test_perform_controlFlow() {
        final Method_Type method = compileMethod("perform_controlFlow", SignatureDescriptor.create(int.class, int.class, TestException.class));
        assertTrue(perform_controlFlow(2, new TestException(-1)) == 123);
        final Value result = executeWithReceiver(method, IntValue.from(2), ReferenceValue.from(new TestException(-1)));
        assertTrue(result.asInt() == 123);
    }

    private void perform_athrow(TestException testException) throws TestException {
        throw testException;
    }

    public void test_athrow() {
        final Method_Type method = compileMethod("perform_athrow", SignatureDescriptor.create(void.class, TestException.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void athrow() {
                confirmPresence();
            }
        };
        final TestException testException = new TestException(-1);
        try {
            executeWithReceiverAndException(method, ReferenceValue.from(testException));
            fail();
        } catch (InvocationTargetException invocationTargetException) {
            assertTrue(invocationTargetException.getTargetException() == testException);
        }
    }

    private int perform_catch0(TestException exception) {
        try {
            throw exception;
        } catch (TestException testException) {
            return 123;
        }
    }

    public void test_catch0() {
        final Method_Type method = compileMethod("perform_catch0", SignatureDescriptor.create(int.class, TestException.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void athrow() {
                confirmPresence();
            }
        };
        assertTrue(perform_catch0(new TestException(-1)) == 123);
        final Value result = executeWithReceiver(method, ReferenceValue.from(new TestException(-1)));
        assertTrue(result.asInt() == 123);
    }

    private int perform_catch1(TestException exception) {
        try {
            perform_athrow(exception);
            return 456;
        } catch (TestException testException) {
            return 123;
        }
    }

    public void test_catch1() {
        final Method_Type method = compileMethod("perform_catch1", SignatureDescriptor.create(int.class, TestException.class));
        assertTrue(perform_catch1(new TestException(-1)) == 123);
        final Value result = executeWithReceiver(method, ReferenceValue.from(new TestException(-1)));
        assertTrue(result.asInt() == 123);
    }

    public int trigger(TestException exception, int value) throws TestException {
        if (value == exception.trigger) {
            throw exception;
        }
        return value;
    }

    private int perform_catch2(TestException exception) {
        try {
            trigger(exception, 0);
            trigger(exception, 1);
            try {
                trigger(exception, 2);
                trigger(exception, 5);
            } catch (TestException testException) {
                trigger(exception, 3);
                trigger(exception, 5);
                return 789;
            }
            trigger(exception, 4);
        } catch (TestException testException) {
            return 123;
        }
        return 456;
    }

    public void test_catch2() {
        final Method_Type method = compileMethod("perform_catch2", SignatureDescriptor.create(int.class, TestException.class));

        assertTrue(perform_catch2(new TestException(-1)) == 456);
        Value result = executeWithReceiver(method, ReferenceValue.from(new TestException(-1)));
        assertTrue(result.asInt() == 456);

        assertTrue(perform_catch2(new TestException(1)) == 123);
        result = executeWithReceiver(method, ReferenceValue.from(new TestException(1)));
        assertTrue(result.asInt() == 123);

        assertTrue(perform_catch2(new TestException(2)) == 789);
        result = executeWithReceiver(method, ReferenceValue.from(new TestException(2)));
        assertTrue(result.asInt() == 789);

        assertTrue(perform_catch2(new TestException(3)) == 456);
        result = executeWithReceiver(method, ReferenceValue.from(new TestException(3)));
        assertTrue(result.asInt() == 456);

        assertTrue(perform_catch2(new TestException(4)) == 123);
        result = executeWithReceiver(method, ReferenceValue.from(new TestException(4)));
        assertTrue(result.asInt() == 123);

        assertTrue(perform_catch2(new TestException(5)) == 123);
        result = executeWithReceiver(method, ReferenceValue.from(new TestException(5)));
        assertTrue(result.asInt() == 123);
    }

    private int perform_catch3(TestException exception) throws TestException {
        int l = trigger(exception, 0);
        try {
            l = trigger(exception, 1) + 10;
            throw exception;
        } catch (TestException testException) {
            return l;
        }
    }

    public void test_catch3() throws TestException {
        final Method_Type method = compileMethod("perform_catch3", SignatureDescriptor.create(int.class, TestException.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void athrow() {
                confirmPresence();
            }
        };

        assertTrue(perform_catch3(new TestException(-1)) == 11);
        Value result = executeWithReceiver(method, ReferenceValue.from(new TestException(-1)));
        assertTrue(result.asInt() == 11);

        assertTrue(perform_catch3(new TestException(1)) == 0);
        result = executeWithReceiver(method, ReferenceValue.from(new TestException(1)));
        assertTrue(result.asInt() == 0);
    }

    private int perform_catch4() throws TestException {
        final int[] array = new int[4];
        try {
            array[5] = 42;
        } catch (ArrayIndexOutOfBoundsException e) {
            return 11;
        }
        throw new TestException(89);
    }

    public void test_catch4() throws TestException {
        final Method_Type method = compileMethod("perform_catch4", SignatureDescriptor.create(int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void athrow() {
                confirmPresence();
            }
        };
        assertTrue(perform_catch4() == 11);
        final Value< ? > result = executeWithReceiver(method);
        assertTrue(result.asInt() == 11);
    }

    private static int foo1() throws TestException {
        return 1;
    }

    private static int foo2() {
        return 2;
    }

    private static boolean condition;

    private int perform_blocks() {
        try {
            return foo1();
        } catch (TestException testException) {
            return foo2();
        }
    }

    public void test_blocks() {
        final Method_Type method = compileMethod("perform_blocks", SignatureDescriptor.create(int.class));
        assertTrue(perform_blocks() == 1);
        final Value< ? > result = executeWithReceiver(method);
        assertTrue(result.asInt() == 1);
    }

    private void throwClassNotFoundException() throws ClassNotFoundException, IOException {
        throw new ClassNotFoundException();
    }

    private int perform_type1() {
        try {
            throwClassNotFoundException();
            return 1;
        } catch (IOException ioException) {
            return 2;
        } catch (ClassNotFoundException classNotFoundException) {
            return 3;
        }
    }

    public void test_type1() {
        final Method_Type method = compileMethod("perform_type1", SignatureDescriptor.create(int.class));
        assertTrue(perform_type1() == 3);
        final Value< ? > result = executeWithReceiver(method);
        assertTrue(result.asInt() == 3);
    }

    private int perform_type2() {
        try {
            throwClassNotFoundException();
            return 1;
        } catch (ClassNotFoundException classNotFoundException) {
            return 2;
        } catch (IOException ioException) {
            return 3;
        }
    }

    public void test_type2() {
        final Method_Type method = compileMethod("perform_type2", SignatureDescriptor.create(int.class));
        assertTrue(perform_type2() == 2);
        final Value< ? > result = executeWithReceiver(method);
        assertTrue(result.asInt() == 2);
    }

}
