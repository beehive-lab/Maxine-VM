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
package com.sun.max.vm.test;




/**
 * A class on which to test the interpreter remotely.
 * 
 * @author Athul Acharya
 */
public class InterpreterRemoteTestClass {

    protected InterpreterRemoteTestClass() { }

    protected InterpreterRemoteTestClass(int y) {
        _y = y;
    }

    protected InterpreterRemoteTestClass(String z) {
        _z = z;
    }

    private String _z;

    private static int _x = 0xdeadbeef;

    private static InterpreterRemoteTestClass _i = new InterpreterRemoteTestClass();

    private static InterpreterRemoteTestChildClass _ic = new InterpreterRemoteTestChildClass();

    private static Object[] _array = new Object[2];

    private static int[][] _iarray = {{0xba5eba11, 2}, {3, 4}};

    public int _y = 0xcafebabe;

    @Override
    public String toString() {
        return _z;
    }

    //just here for sanity check
    public static int iadd(int a, int b) {
        return a + b;
    }

    public static int getstatic() {
        final int y = _x;
        return y;
    }

    public static int getfield() {
        return _i._y;
    }

    public static int arraylength() {
        return _array.length;
    }

    public static int iaload_aaload() {
        return _iarray[0][0];
    }

    public static int getfield2(InterpreterRemoteTestClass i) { //for use from inspector
        return i._y;
    }

    public int return_y() {
        return _y;
    }

    public int virtual_overriden(int a, int b, int c) {
        return 3;
    }

    public static int invokevirtual1(InterpreterRemoteTestClass i) { //for use from inspector
        return i.virtual_overriden(1, 2, 3);
    }

    public static int invokevirtual2() {
        final InterpreterRemoteTestClass i = _i;
        return i.virtual_overriden(1, 2, 3); //should return 3
    }

    public static int invokevirtual3() {
        final InterpreterRemoteTestClass i = _ic;
        return i.virtual_overriden(1, 2, 3); //should return 6
    }


    public static int new1() {
        final InterpreterRemoteTestClass i = new InterpreterRemoteTestClass();
        return i._y;
    }

    public static int new2() {
        final InterpreterRemoteTestClass i = new InterpreterRemoteTestClass(0x80081355);
        return i._y;
    }

    public static int putfield() {
        final InterpreterRemoteTestClass i = new InterpreterRemoteTestClass(0x80081355);

        i._y = 666;
        return i._y;
    }

    private static final String testString = new String("test");

    public static InterpreterRemoteTestClass stringnew() {
        return new InterpreterRemoteTestClass(testString);
    }
}
