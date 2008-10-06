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
/*VCSID=01e9711c-6c17-4815-9d9d-c6e3a0e803a3*/
package com.sun.max.vm.run.compilerTest;

import com.sun.max.vm.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.run.java.*;

/**
 */
public class CompilerTestRunScheme extends JavaRunScheme {

    public CompilerTestRunScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    static final Throwable _exception = new Throwable();

    private void b() throws Throwable {
        throw _exception;
    }

    private void a() throws Throwable {
        b();
    }

    @Override
    public void run() {
        initializeBasicFeatures();

        testException();
        testException1();
        testException2();
        testException3();
        testException4();

        testNullPointerException(null);

        testNative();
//        testMultipleThreads();
//        testNativeRecursive(5);
    }

    static class HelloWorldThread extends Thread {
        boolean _done;
        @Override
        public void run() {
            Debug.println("[" + this + "] Hello world");
            _done = true;
        }
    }

    private void testMultipleThreads() {
        final HelloWorldThread thread1 = new HelloWorldThread();
        final HelloWorldThread thread2 = new HelloWorldThread();
        final HelloWorldThread thread3 = new HelloWorldThread();

        thread1.start();
        thread2.start();
        thread3.start();

        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            Debug.println("testMultipleThreads: fail");
            return;
        }

        Debug.println("testMultipleThreads: pass");
    }

    private void testNullPointerException(Object object) {
        try {
            object.toString();
        } catch (NullPointerException nullPointerException) {
            Debug.println("testNullPointerException: pass");
        }
    }

    private void testException() {
        try {
            a();
            Debug.println("testException: fail");
        } catch (Throwable e) {
            Debug.println("testException: pass");
        }
    }

    private void testException1() {
        try {
            a();
            Debug.println("testException1: fail");
        } catch (Throwable e) {
            Debug.println(e.toString());
            Debug.println("testException1: pass");
        }
    }

    private void testException2() {
        try {
            a();
            Debug.println("testException2: fail");
        } catch (Throwable e) {
            e.printStackTrace(Debug.log);
            Debug.println("testException2: pass");
        }
    }

    private void testException3() {
        try {
            throw _exception;
        } catch (Throwable e) {
            Debug.println("testException3: pass");
        }
    }

    static class TestException extends Exception {
    }

    private void testException4() {
        try {
            throw new TestException();
        } catch (TestException testException) {
            Debug.println("testException4: pass");
        }
    }

    private int _i;
    private Object _o;

    private native void nativeUpdateFields(int recursion, int i, Object o);

    private void testNative() {
        final int intValue = 42;
        boolean pass = true;
        nativeUpdateFields(0, intValue, this);
        if (_i != intValue) {
            Debug.println(_i + " != " + intValue);
            pass = false;
        }
        if (_o != this) {
            Debug.println(Reference.fromJava(_o).toOrigin().toString() + " != " + Reference.fromJava(this).toOrigin().toString());
            pass = false;
        }
        Debug.println("testNative: " + (pass ? "pass" : "fail"));
    }

    private void testNativeRecursive(int recursion) {
        final int intValue = 42;
        boolean pass = true;
        if (recursion > 0) {
            nativeUpdateFields(recursion - 1, intValue, this);
            return;
        }
        if (_i != intValue) {
            Debug.println(_i + " != " + intValue);
            pass = false;
        }
        if (_o != this) {
            Debug.println(Reference.fromJava(_o).toOrigin().toString() + " != " + Reference.fromJava(this).toOrigin().toString());
            pass = false;
        }
        Debug.println("testNative: " + (pass ? "pass" : "fail"));
    }
}
