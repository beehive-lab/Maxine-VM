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
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.cps.*;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.type.*;

/**
 * Tests of JIT-compilation of methods with exception handler.
 *
 * @author Laurent Daynes
 *
 */
public class JITTest_exceptions  extends JitCompilerTestCase {

    private int intField;

    /**
     * Method with single try-catch block and exception handler.
     */
    public void perform_simple_throw_catch() {
        @SuppressWarnings("unused")
        boolean caughtException = false;
        try {
            throw new RuntimeException();
        } catch (RuntimeException e) {
            caughtException = true;
        }
    }

    /**
     * Method with single try-catch block and exception handler.
     */
    public void perform_simple_throw_catch2() {
        @SuppressWarnings("unused")
        Throwable caughtException = null;
        try {
            if (intField == 0) {
                throw new NullPointerException();
            }
            throw new Exception();
        } catch (NullPointerException eNull) {
            caughtException = eNull;
        } catch (Exception e) {
            caughtException = e;
        }
    }

    private void check_num_handlers(String methodName, int num) {
        final CPSTargetMethod targetMethod = compileMethod(methodName);
        assert targetMethod.numberOfCatchRanges() == num;
        assert targetMethod.catchRangePositions() != null && targetMethod.catchRangePositions().length == num;
        assert targetMethod.catchBlockPositions() != null && targetMethod.catchBlockPositions().length == num;
    }

    public void  test_compile_simple_throw_catch() {
        check_num_handlers("perform_simple_throw_catch", 2);
    }

    public void  test_compile_simple_throw_catch2() {
        check_num_handlers("perform_simple_throw_catch", 2);
    }

    /**
     * Single-exception handler case.
     */
    public JITTest_exceptions oneException(Object[] o, int i) {
        try {
            return (JITTest_exceptions) o[i];
        } catch (ArrayIndexOutOfBoundsException eIndex) {
            return null;
        }
    }

    /**
     * multiple handlers on the same range.
     */
    public JITTest_exceptions twoExceptionsOneRange(Object[] o, int i) {
        try {
            return (JITTest_exceptions) o[i];
        } catch (ArrayIndexOutOfBoundsException eIndex) {
            return this;
        } catch (ClassCastException eCast) {
            return null;
        }
    }

    public JITTest_exceptions noCatch(Object[] o, int i) {
        return (JITTest_exceptions) o[i];
    }

    public JITTest_exceptions twoExceptionsOneRange2(Object[] o, int i) {
        try {
            return noCatch(o, i);
        } catch (ArrayIndexOutOfBoundsException eIndex) {
            return this;
        } catch (ClassCastException eCast) {
            return null;
        }
    }

    public JITTest_exceptions nestedExceptions(Object[] o, int i) {
        try {
            final JITTest_exceptions t = (JITTest_exceptions) o[i];
            try {
                final JITTest_exceptions t2 = (JITTest_exceptions) o[i + 1];
                o[i + 1] = t;
                return t2;
            } catch (ArrayIndexOutOfBoundsException eIndex) {
                return this;
            }
        } catch (ClassCastException eCast) {
            return null;
        }
    }

    public void printExceptionHandlers(CPSTargetMethod targetMethod) {
        final int[] catchRangePositions = targetMethod.catchRangePositions();
        if (catchRangePositions == null) {
            Trace.line(1, "Target method " + targetMethod.regionName() + " has no exception handlers");
            return;
        }
        final int[] catchBlockPositions = targetMethod.catchBlockPositions();
        for (int i = 0; i < catchRangePositions.length; i++) {
            if (catchBlockPositions[i] != 0) {
                Trace.line(1, "[" +  catchRangePositions[i] + ", " + catchRangePositions[i + 1] + "]");
            }
        }
    }

    private static final Throwable[] THROWABLES = new Throwable[]{new ArrayIndexOutOfBoundsException(), new ClassCastException() };

    private void compileMethodWithExceptionHandlers(String methodName) {
        final CPSTargetMethod targetMethod = compileMethod(methodName,  SignatureDescriptor.create(JITTest_exceptions.class, Object[].class, int.class));
        printExceptionHandlers(targetMethod);

        // Get handler at random address, walking 5 bytes at a time.
        int throwOffset = 0;
        while (throwOffset < targetMethod.codeLength()) {
            final Address throwAddress = targetMethod.codeStart().plus(throwOffset);
            for (Throwable throwable : THROWABLES) {
                final Address catchAddress = targetMethod.throwAddressToCatchAddress(false, throwAddress, throwable.getClass());
                Trace.line(1, throwable.getClass().getName() + " thrown at " + throwOffset + (catchAddress.isZero() ? " uncaught" :
                    " caught at " + (catchAddress.minus(targetMethod.codeStart())).toInt()));
            }
            throwOffset += 5;
        }
    }

    public void test_compile1() {
        compileMethodWithExceptionHandlers("oneException");
    }

    public void test_compile2() {
        compileMethodWithExceptionHandlers("twoExceptionsOneRange");
    }

    public void test_compile3() {
        compileMethodWithExceptionHandlers("twoExceptionsOneRange2");
    }

    public void test_compile4() {
        compileMethodWithExceptionHandlers("nestedExceptions");
    }
}
