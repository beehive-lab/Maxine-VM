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
            Trace.line(1, "Target method " + targetMethod.description() + " has no exception handlers");
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
