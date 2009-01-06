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

import junit.framework.*;

import org.junit.runner.*;

import com.sun.max.tele.*;
import com.sun.max.vm.value.*;

/**
 * Prototyping tests for the Maxine Inspector's interpreter.
 *
 * @author Athul Acharya
 */
@RunWith(org.junit.runners.AllTests.class)
public class TeleInterpreterTest extends TeleInterpreterTestCase {
    public TeleInterpreterTest() {
    }

    public TeleInterpreterTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TeleInterpreterTest.class);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(TeleInterpreterTest.class.getSimpleName());
        suite.addTestSuite(TeleInterpreterTest.class);
        return new TeleInterpreterTestSetup(suite);
    }

    @Override
    protected Class defaultDeclaringClass() {
        return TeleInterpreterTestClass.class;
    }

    @Override
    protected TeleVM teleVM() {
        return  null;
    }

    public void test_aastore_iastore_aaload_iaload() {
        final int[][] i = {{1, 2, 3}, {4, 5, 6}};
        final int[] b = {6, 7, 8};

        final Value ret = execute("aastore_iastore_aaload_iaload", ReferenceValue.from(i), ReferenceValue.from(b));
        assertTrue(ret.asInt() == 11);
    }

    public void test_iadd() {
        final int a = 1;
        final int b = 2;

        final Value ret = execute("iadd", IntValue.from(a), IntValue.from(b));
        assertTrue(ret.asInt() == 3);
    }

    public void test_argpad() {
        final Value ret = execute("argpad", DoubleValue.from(1.0), IntValue.from(1), DoubleValue.from(2.0), IntValue.from(2),
                                            DoubleValue.from(1.0), IntValue.from(3));
        assertTrue(ret.asInt() == 6);
    }

    public void test_getstatic() {
        final Value ret = execute("getstatic");
        assertTrue(ret.asInt() == 0xdeadbeef);
    }

    public void test_putstatic() {
        final Value ret = execute("putstatic");
        assertTrue(ret.asInt() == 4);
    }

    public void test_getfield() {
        final Value ret = execute("getfield", ReferenceValue.from(new TeleInterpreterTestClass()));
        assertTrue(ret.asInt() == 0xcafebabe);
    }

    public void test_putfield() {
        final Value ret = execute("putfield", ReferenceValue.from(new TeleInterpreterTestClass()));
        assertTrue(ret.asInt() == 4);
    }

    public void test_invokevirtual1() {
        final Value ret = execute("invokevirtual1", ReferenceValue.from(new TeleInterpreterTestClass()));
        assertTrue(ret.asInt() == 0xcafebabe);
    }

    public void test_invokevirtual2() {
        final Value ret = execute("invokevirtual2", ReferenceValue.from(new TeleInterpreterTestClass()));
        assertTrue(ret.asInt() == 12);
    }

    public void test_invokevirtual3() {
        final Value ret = execute("invokevirtual3", ReferenceValue.from(new TeleInterpreterTestChildClass()));
        assertTrue(ret.asInt() == 6);
    }

    public void test_invokespecial_super() {
        final Value ret = execute("invokespecial_super", ReferenceValue.from(new TeleInterpreterTestChildClass()));
        assertTrue(ret.asInt() == 3);
    }

    public void test_invokespecial_private() {
        final Value ret = execute("invokespecial_private", ReferenceValue.from(new TeleInterpreterTestClass()));
        assertTrue(ret.asInt() == 3);
    }

    public void test_invokestatic() {
        final Value ret = execute("invokestatic");
        assertTrue(ret.asInt() == 4); //because we did putstatic earlier
    }

    public void test_invokeinterface() {
        final Value ret = execute("invokeinterface", ReferenceValue.from(new TeleInterpreterTestChildClass()));
        assertTrue(ret.asInt() == 9);
    }

    public void test_newarray() {
        final Value ret = execute("newarray");
        assertTrue(ret.asInt() == 5);
    }

    public void test_anewarray() {
        final Value ret = execute("anewarray");
        assertTrue(ret.asInt() == 1);
    }

    public void test_arraylength() {
        final Value ret = execute("arraylength", ReferenceValue.from(new Object[20]));
        assertTrue(ret.asInt() == 20);
    }

    public void test_athrow1() {
        executeWithExpectedException("athrow1", Exception.class, ReferenceValue.from(new Exception("test")));
    }

    public void test_athrow2() {
        final Value ret = execute("athrow2", ReferenceValue.from(new Exception("test")));
        assertTrue(ret.asInt() == 1);
    }

    public void test_athrow3() {
        final Value ret = execute("athrow3", ReferenceValue.from(new Exception("test")));
        assertTrue(ret.asInt() == 3);
    }

    public void test_athrow4() {
        final Value ret = execute("athrow4", ReferenceValue.from(new Exception("test")));
        assertTrue(ret.asInt() == 1);
    }

    public void test_athrow5() {
        executeWithExpectedException("athrow5", Exception.class, ReferenceValue.from(new Exception("test")));
    }

    public void test_checkcast1() {
        final Value ret = execute("checkcast", ReferenceValue.from(new TeleInterpreterTestClass(1)));
        assertTrue(ret.asInt() == 1);
    }

    public void test_checkcast2() {
        final Value ret = execute("checkcast", ReferenceValue.from(new TeleInterpreterTestChildClass(1)));
        assertTrue(ret.asInt() == 1);
    }

    public void test_checkcast3() {
        executeWithExpectedException("checkcast", ClassCastException.class, ReferenceValue.from(new Integer(2)));
    }

    public void test_instanceof1() {
        final Value ret = execute("instanceof_", ReferenceValue.from(new TeleInterpreterTestClass()));
        assertTrue(ret.asInt() == 1);
    }

    public void test_instanceof2() {
        final Value ret = execute("instanceof_", ReferenceValue.from(new TeleInterpreterTestChildClass()));
        assertTrue(ret.asInt() == 1);
    }

    public void test_instanceof3() {
        final Value ret = execute("instanceof_", ReferenceValue.from(new Integer(2)));
        assertTrue(ret.asInt() == 0);
    }

    public void test_multianewarray() {
        final Value ret = execute("multianewarray");
        assertTrue(ret.asInt() == 13);
    }

    public void test_new1() {
        final Value ret = execute("new1");
        assertTrue(ret.asInt() == 0xcafebabe);
    }

    public void test_new2() {
        final Value ret = execute("new2");
        assertTrue(ret.asInt() == 0x80081355);
    }

    public void test_println() {
        final Value ret = execute("println");
        assertTrue(ret.asInt() == 500);
    }
}
