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
package com.sun.max.vm.run.jitTest;

import java.lang.reflect.*;

import com.sun.max.annotate.*;

/**
 * @author Laurent Daynes
 */
public class JitTest implements Runnable {

    private static final RuntimeException _testException = new RuntimeException();

    private static int _intStaticVar;

    private static boolean _testResult = false;

    private static final JitTest _testObject = new JitTest(55);

    private static JitTest _testObject2 = new JitTest(55);

    private static Throwable _caughtException = null;

    int _intField;

    float _floatField;
    long _longField;
    double _doubleField;

    String _stringField;


    public JitTest() {
        _intField = 33;
    }

    public JitTest(int i) {
        _intField = i;
    }

    private void reset() {
        _intField = 33;
    }

    public void neg() {
        _intField = -_intField;
    }

    public int getInt() {
        return _intField;
    }

    public void add(int i) {
        _intField += i;
    }

    public void compute(int i1, int i2, int i3) {
        _intField += i1 * (i2 - i3);
    }

    public long addToLong(long l) {
        return l + _intField;
    }

    public static int fact(int n) {
        if (n == 0) {
            return 1;
        }
        return n * fact(n - 1);
    }

    public static void init() {
        _intStaticVar = 12345;
    }

    public static JitTest makeTest() {
        return new JitTest();
    }

    public void simonTest1() {
        _floatField =  11.33f + 20f;
    }

    public void simonTest2() {
        _doubleField =  11.33d + 20.54548498785151d;
    }

    public void simonTest3() {
        _stringField =  "Test String";
    }

    public void simonTest4() {
        _longField =  3L + 4000000000000000000L;
    }

    @BOOT_IMAGE_DIRECTIVE(useJitCompiler = false)
    @SuppressWarnings("unused")
    public void simonTest5() {
        final Object o = _testException;
        final boolean a1 = o instanceof RuntimeException;
        final boolean a2 = o instanceof Exception;
        final boolean a3 = o instanceof NullPointerException;
    }


    /**
     * Testing static variable access.
     */
    private static void test1() {
        @SuppressWarnings("unused")
        final JitTest t = _testObject;
    }

    /**
     * Testing static variable access.
     */
    private static void test2() {
        @SuppressWarnings("unused")
        final int i = _intStaticVar;
    }
    /**
     * Testing static variable access.
     */
    private static void test3() {
        _intStaticVar = 3333;
    }

    /**
     * Testing parameterless non virtual method invocation.
     */
    private static void test4() {
        final JitTest t = _testObject;
        t.reset();
    }

    /**
     * Testing parameterless virtual method invocation.
     */
    private static void test5() {
        @SuppressWarnings("unused")
        final JitTest t = _testObject;
    }

    /**
     * Testing parameterless virtual method invocation returning one-word result and
     * virtual method invocation with parameters and no return value.
     */
    private static void test6() {
        final JitTest t = _testObject;
        final int i = t.getInt();
        t.compute(i, 2, 3);
    }

    /**
     * Testing virtual method invocation taking 1 argument.
     */
    private static void test7() {
        final JitTest t = _testObject;
        t.add(7);
    }

    private static void test8() {
        final JitTest t = _testObject;
        @SuppressWarnings("unused")
        final long l = t.addToLong(999999999999L);
    }

    /**
     * Testing deep invocation of methods.
     */
    private static void test9() {
        @SuppressWarnings("unused")
        final int i = fact(10);
    }

    /**
     * Testing object allocation.
     */
    private static void test20() {
        @SuppressWarnings("unused")
        final JitTest t = new JitTest();
    }

    /**
     * Testing  switch bytecodes.
     */
    private static void test10() {
        final JitTest t = _testObject;
        t._intField = 0;
        t.tableSwitchTest1(1);
        t.tableSwitchTest1(2);
        t.tableSwitchTest1(12);
        t.tableSwitchTest1(0);
        t.tableSwitchTest1(-1);
        t.tableSwitchTest1(-5);

        t._intField = 0;
        t.tableSwitchTest2(1);
        t.tableSwitchTest2(8);
        t.tableSwitchTest2(5);
        t.tableSwitchTest2(6);
        t.tableSwitchTest2(7);
        t.tableSwitchTest2(-1);

        t._intField = 0;
        t.tableSwitchTest3(1);
        t.tableSwitchTest3(5);
        t.tableSwitchTest3(0);
        t.tableSwitchTest3(-3);
        t.tableSwitchTest3(-1);
        t.tableSwitchTest3(-2);

        t._intField = 0;
        t.tableSwitchTest4(1);
        t.tableSwitchTest4(-1);
        t.tableSwitchTest4(0);
        t.tableSwitchTest4(-3);
        t.tableSwitchTest4(-2);
        t.tableSwitchTest4(-5);

        t._intField = 0;
        t.lookupSwitchTest1(1);
        t.lookupSwitchTest1(10);
        t.lookupSwitchTest1(-10);
        t.lookupSwitchTest1(100);
        t.lookupSwitchTest1(1000);
        t.lookupSwitchTest1(99);
    }

    private void tableSwitchTest1(int i) {
        switch (i) {
            case 0:
                _intField = 10;
                break;
            case 1:
                _intField = 20;
                break;
            case 2:
                _intField = 30;
                break;
            default:
                _intField = 40;
        }
    }

    private void tableSwitchTest2(int i) {
        switch (i) {
            case 5:
                _intField = 11;
                break;
            case 6:
                _intField = 22;
                break;
            case 7:
                _intField = 33;
                break;
            default:
                _intField = 44;
        }
    }


    private void tableSwitchTest3(int i) {
        switch (i) {
            case -1:
                _intField = 11;
                break;
            case 0:
                _intField = 22;
                break;
            case 1:
                _intField = 33;
                break;
            default:
                _intField = 44;
        }
    }

    private void tableSwitchTest4(int i) {
        switch (i) {
            case -1:
                _intField = 11;
                break;
            case -2:
                _intField = 22;
                break;
            case -3:
                _intField = 33;
                break;
            default:
                _intField = 44;
        }
    }

    private void lookupSwitchTest1(int i) {
        switch(i) {
            case 10:
                _intField = 1;
                break;
            case 100:
                _intField = 2;
                break;
            case 1000:
                _intField = 3;
                break;
            default:
                _intField = -1;
        }
    }

    /**
     * Testing loop construct (i.e., goto and conditional branch).
     */
    private static void test11() {
        int mul = 1;
        for (int i = 0; i < 10; i++) {
            mul = 3 * mul;
        }
    }

    private static void test12() {
        int multiple3Count = 0;
        for (int i = 0; i < 10; i++) {
            if (i / 3 == 0) {
                multiple3Count++;
            }
        }
    }

    @BOOT_IMAGE_DIRECTIVE(keepUnlinked = true, useJitCompiler = true)
    public void setInt(int i) {
        _intField = i;
    }

    @BOOT_IMAGE_DIRECTIVE(keepUnlinked = true, useJitCompiler = true)
    public void incInt(int i) {
        _intField += i;
    }

    @BOOT_IMAGE_DIRECTIVE(keepUnlinked = true, useJitCompiler = false)
    public void optIncInt(int i) {
        _intField += i;
    }

    /**
     * Testing vtable trampoline && runtime resolution.
     */
    private static void test13() {
        final JitTest t = _testObject;
        t.setInt(13);
        t.incInt(5);
        t.incInt(10);
        t.optIncInt(5);
        t.optIncInt(12);
    }

    @BOOT_IMAGE_DIRECTIVE(keepUnlinked = true, useJitCompiler = true)
    public void run() {
        _intField = 1;
    }

    private static void test14() {
        final Runnable r = _testObject;
        r.run();
        // call again -- this time we should go via the trampoline
        r.run();
    }

    @BOOT_IMAGE_DIRECTIVE(keepUnlinked = true, useJitCompiler = true)
    public void timesInt(int i) {
        _intField *= i;
    }

    @BOOT_IMAGE_DIRECTIVE(keepUnlinked = true, useJitCompiler = false)
    public void optTimesInt(int i) {
        _intField *= i;
    }

    @BOOT_IMAGE_DIRECTIVE(keepUnlinked = true, exclude = true)
    public void modInt(int i) {
        _intField %= i;
    }

    @BOOT_IMAGE_DIRECTIVE(keepUnlinked = true, useJitCompiler = false)
    public void optTest15() {
        final JitTest t = _testObject;
        // first invocation calls trampoline
        t.timesInt(2);
        // called resolved virtual call
        t.timesInt(2);
        // first invocation calls trampoline
        t.optTimesInt(2);
        t.optTimesInt(2);
    }

    /**
     * Testing trampoline called from optimized code,
     * when callee is jited, and callee is optimized.
     */
    private static void test15() {
        final JitTest t = _testObject;
        t.optTest15();
    }

    /**
     * Testing instanceof.
     */
    @SuppressWarnings("unused")
    private static void test16() {
        final Object o = _testException;
        final boolean a1 = o instanceof RuntimeException;
        final boolean a2 = o instanceof Exception;
        final boolean a3 = o instanceof NullPointerException;
    }

    /**
     * Testing cast.
     */
    @SuppressWarnings("unused")
    private static void test17() {
        final Object o = _testException;
        final RuntimeException re = (RuntimeException) o;
        final Exception e = (Exception) o;
    }

    /*
     * Testing getting mirror.
     */
    private static void test18() {
        _testResult = false;
        final Class thisClass = JitTest.class;  // testing resolved ldc
        final Class thisClass2 = _testObject.getClass();
        _testResult = thisClass2 == thisClass;
    }


    /**
     * Testing exception throwing and catch within the same method.
     */
    private static void test21() {
        _caughtException = null;
        try {
            for (int i = 0; i < 100; i++) {
                if (i % 3 == 0) {
                    throw _testException;
                }
            }

        } catch (RuntimeException e) {
            _caughtException = e;
        }
    }

    private void recurseWithException(int level) {
        if (level == 0) {
            throw _testException;
        }
        recurseWithException(level - 1);
    }

    /**
     * Testing exception throwing and catch in outer method.
     */
    private static void test22() {
        _caughtException = null;
        try {
            _testObject.recurseWithException(1);
        } catch (RuntimeException e) {
            _caughtException = e;
        }
    }

    /**
     * Testing exception throwing and catch in outer method.
     */
    private static void test23() {
        _caughtException = null;
        try {
            _testObject.recurseWithException(5);
        } catch (RuntimeException e) {
            _caughtException = e;
        }
    }

    private static void test24() {
        _testObject.simonTest1();
        _testObject.simonTest2();
        _testObject.simonTest3();
        _testObject.simonTest4();
        _testObject.simonTest5();
    }

    private static void test30() {
        final JitTest o = new JitTest();
        o._stringField = "new test object";
    }

    static final OptTest optTest = new OptTest(111);

    static final InterfaceTest iTest = new OptTest(555);

    @BOOT_IMAGE_DIRECTIVE(keepUnlinked = true)
    private static void doUnlinkedVirtualOptCall() {
        optTest.callUnresolved();
    }

    private static void  test241() {
        doUnlinkedVirtualOptCall();
    }

    @BOOT_IMAGE_DIRECTIVE(keepUnlinked = true)
    private static int unlinkedJitedCall() {
        return OptTest.unlinkedStaticCall();
    }

    /*
     * Testing unlinked static calls
     */
    @BOOT_IMAGE_DIRECTIVE(keepUnlinked = true)
    private static void  test242() {
        int res = 0;
        for (int i = 0; i < 2; i++) {
            res += unlinkedJitedCall();
        }
    }

    private static void test25() {
        try {
            final Method m =  OptTest.class.getDeclaredMethod("getInt");
            final Integer result = (Integer) m.invoke(optTest);
            _testObject.add(result.intValue());
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private static void test26() {
        optTest.neg();
        final int i = optTest.addInt(4);
    }

    @SuppressWarnings("unused")
    @BOOT_IMAGE_DIRECTIVE(keepUnlinked = true)
    private static void test27() {
        final int i = iTest.computeMe();
    }

    @BOOT_IMAGE_DIRECTIVE(useJitCompiler = true)
    public static void test() {
        test242();
        test241();
        test25();
        test27();
        test15();
        test26();
  /*      test5();
        test6();
        test7();
        test8();
        test9();

        test30();
        test21();
        test22();
        test23();
        test16();
        test17();
        test18();
        test1();
        test2();
        test3();
        test4();

        test10();
        test11();
        test12();
        test13();
        test14();

        test24();*/
    }
}
