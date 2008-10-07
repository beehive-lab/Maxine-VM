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
/*VCSID=871a33d6-570c-48af-a164-452f1ff4f98c*/
package test.com.sun.max.ins.interpreter;

import junit.framework.*;

import org.junit.runner.*;

import com.sun.max.tele.interpreter.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Prototyping tests for the Maxine Inspector's interpreter.
 *
 * @author Athul Acharya
 */
@RunWith(org.junit.runners.AllTests.class)
public class InterpreterTest extends TestCase {
    public InterpreterTest() {
    }

    public InterpreterTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(InterpreterTest.class);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(InterpreterTest.class.getSimpleName());
        //$JUnit-BEGIN$
        suite.addTestSuite(InterpreterTest.class);
        //$JUnit-END$
        return new InterpreterTestSetup(suite);
    }

    public void test_aastore_iastore_aaload_iaload() throws ClassNotFoundException {
        final int[][] i = {{1, 2, 3}, {4, 5, 6}};
        final int[] b = {6, 7, 8};
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "aastore_iastore_aaload_iaload", SignatureDescriptor.create("([[I[I)I"),
                                            ReferenceValue.from(i), ReferenceValue.from(b));

        assertTrue(ret instanceof IntValue && ret.asInt() == 11);
    }

    public void test_iadd() throws ClassNotFoundException {
        final int a = 1;
        final int b = 2;
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "iadd", SignatureDescriptor.create("(II)I"),
                                            IntValue.from(a), IntValue.from(b));

        assertTrue(ret instanceof IntValue && ret.asInt() == 3);
    }

    public void test_argpad() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "argpad", SignatureDescriptor.create("(DIDIDI)I"),
                                            DoubleValue.from(1.0), IntValue.from(1), DoubleValue.from(2.0), IntValue.from(2),
                                            DoubleValue.from(1.0), IntValue.from(3));

        assertTrue(ret instanceof IntValue && ret.asInt() == 6);
    }

    public void test_getstatic() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "getstatic", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 0xdeadbeef);
    }

    public void test_putstatic() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "putstatic", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 4);
    }

    public void test_getfield() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "getfield", SignatureDescriptor.create("(Ltest/com/sun/max/ins/interpreter/InterpreterTestClass;)I"),
                                            ReferenceValue.from(new InterpreterTestClass()));

        assertTrue(ret instanceof IntValue && ret.asInt() == 0xcafebabe);
    }

    public void test_putfield() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "putfield", SignatureDescriptor.create("(Ltest/com/sun/max/ins/interpreter/InterpreterTestClass;)I"),
                                            ReferenceValue.from(new InterpreterTestClass()));

        assertTrue(ret instanceof IntValue && ret.asInt() == 4);
    }

    public void test_invokevirtual1() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "invokevirtual1", SignatureDescriptor.create("(Ltest/com/sun/max/ins/interpreter/InterpreterTestClass;)I"),
                                            ReferenceValue.from(new InterpreterTestClass()));

        assertTrue(ret instanceof IntValue && ret.asInt() == 0xcafebabe);
    }

    public void test_invokevirtual2() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "invokevirtual2", SignatureDescriptor.create("(Ltest/com/sun/max/ins/interpreter/InterpreterTestClass;)I"),
                                            ReferenceValue.from(new InterpreterTestClass()));

        assertTrue(ret instanceof IntValue && ret.asInt() == 12);
    }

    public void test_invokevirtual3() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "invokevirtual3", SignatureDescriptor.create("(Ltest/com/sun/max/ins/interpreter/InterpreterTestClass;)I"),
                                            ReferenceValue.from(new InterpreterTestChildClass()));

        assertTrue(ret instanceof IntValue && ret.asInt() == 6);
    }

    public void test_invokespecial_super() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "invokespecial_super", SignatureDescriptor.create("(Ltest/com/sun/max/ins/interpreter/InterpreterTestChildClass;)I"),
                                            ReferenceValue.from(new InterpreterTestChildClass()));

        assertTrue(ret instanceof IntValue && ret.asInt() == 3);
    }

    public void test_invokespecial_private() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "invokespecial_private", SignatureDescriptor.create("(Ltest/com/sun/max/ins/interpreter/InterpreterTestClass;)I"),
                                            ReferenceValue.from(new InterpreterTestClass()));

        assertTrue(ret instanceof IntValue && ret.asInt() == 3);
    }

    public void test_invokestatic() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "invokestatic", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 4); //because we did putstatic earlier
    }

    public void test_invokeinterface() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "invokeinterface", SignatureDescriptor.create("(Ltest/com/sun/max/ins/interpreter/InterpreterTestInterface;)I"),
                                            ReferenceValue.from(new InterpreterTestChildClass()));

        assertTrue(ret instanceof IntValue && ret.asInt() == 9);
    }

    public void test_newarray() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "newarray", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 5);
    }

    public void test_anewarray() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "anewarray", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 1);
    }

    public void test_arraylength() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "arraylength", SignatureDescriptor.create("([Ljava/lang/Object;)I"),
                                            ReferenceValue.from(new Object[20]));

        assertTrue(ret instanceof IntValue && ret.asInt() == 20);
    }

    public void test_athrow1() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "athrow1", SignatureDescriptor.create("(Ljava/lang/Throwable;)V"),
                                            ReferenceValue.from(new Exception("test")));

        assertTrue(ret instanceof ReferenceValue && ret == ReferenceValue.NULL);
    }

    public void test_athrow2() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "athrow2", SignatureDescriptor.create("(Ljava/lang/Throwable;)I"),
                                            ReferenceValue.from(new Exception("test")));

        assertTrue(ret instanceof IntValue && ret.asInt() == 1);
    }

    public void test_athrow3() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "athrow3", SignatureDescriptor.create("(Ljava/lang/Throwable;)I"),
                                            ReferenceValue.from(new Exception("test")));

        assertTrue(ret instanceof IntValue && ret.asInt() == 3);
    }

    public void test_athrow4() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "athrow4", SignatureDescriptor.create("(Ljava/lang/Throwable;)I"),
                                            ReferenceValue.from(new Exception("test")));

        assertTrue(ret instanceof IntValue && ret.asInt() == 1);
    }

    public void test_athrow5() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "athrow5", SignatureDescriptor.create("(Ljava/lang/Throwable;)I"),
                                            ReferenceValue.from(new Exception("test")));

        assertTrue(ret instanceof ReferenceValue && ret == ReferenceValue.NULL);
    }

    public void test_checkcast1() throws ClassCastException, ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "checkcast", SignatureDescriptor.create("(Ljava/lang/Object;)I"),
                                            ReferenceValue.from(new InterpreterTestClass()));

        assertTrue(ret instanceof IntValue && ret.asInt() == 1);
    }

    public void test_checkcast2() throws ClassCastException, ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "checkcast", SignatureDescriptor.create("(Ljava/lang/Object;)I"),
                                            ReferenceValue.from(new InterpreterTestChildClass()));

        assertTrue(ret instanceof IntValue && ret.asInt() == 1);
    }

    public void test_checkcast3() throws ClassCastException, ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "checkcast", SignatureDescriptor.create("(Ljava/lang/Object;)I"),
                                            ReferenceValue.from(new Integer(2)));

        assertTrue(ret instanceof ReferenceValue && ret == ReferenceValue.NULL);
    }

    public void test_instanceof1() throws ClassCastException, ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "instanceof_", SignatureDescriptor.create("(Ljava/lang/Object;)Z"),
                                            ReferenceValue.from(new InterpreterTestClass()));

        assertTrue(ret instanceof IntValue && ret.asInt() == 1);
    }

    public void test_instanceof2() throws ClassCastException, ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "instanceof_", SignatureDescriptor.create("(Ljava/lang/Object;)Z"),
                                            ReferenceValue.from(new InterpreterTestChildClass()));

        assertTrue(ret instanceof IntValue && ret.asInt() == 1);
    }

    public void test_instanceof3() throws ClassCastException, ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "instanceof_", SignatureDescriptor.create("(Ljava/lang/Object;)Z"),
                                            ReferenceValue.from(new Integer(2)));

        assertTrue(ret instanceof IntValue && ret.asInt() == 0);
    }

    public void test_multianewarray() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "multianewarray", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 13);
    }

    public void test_new1() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "new1", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 0xcafebabe);
    }

    public void test_new2() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "new2", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 0x80081355);
    }

    public void test_println() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(null, "test.com.sun.max.ins.interpreter.InterpreterTestClass",
                                            "println", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 500);
    }
}
