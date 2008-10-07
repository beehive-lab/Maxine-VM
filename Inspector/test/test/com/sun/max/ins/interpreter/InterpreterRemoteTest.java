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
/*VCSID=839113a5-5d3a-4004-ae64-a706ec8b2783*/
package test.com.sun.max.ins.interpreter;

import junit.framework.*;

import org.junit.runner.*;

import com.sun.max.ins.*;
import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.value.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Interpreter tests on a tele VM.
 * @author Athul Acharya
 */

@RunWith(org.junit.runners.AllTests.class)
public class InterpreterRemoteTest extends TestCase {

    private TeleVM _teleVM;

    public InterpreterRemoteTest() {
    }

    public InterpreterRemoteTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(InterpreterRemoteTest.class);
    }

    @Override
    protected final void setUp() throws InterruptedException {
        _teleVM = MaxineInspector.test(new String[0]);

        synchronized (InspectorInterpreter._inspectionLock) {
            InspectorInterpreter._inspectionLock.wait();
        }
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(InterpreterRemoteTest.class.getSimpleName());
        //$JUnit-BEGIN$
        suite.addTestSuite(InterpreterRemoteTest.class);
        //$JUnit-END$
        return suite;
    }

    //this test is just here as a sanity check that starting the inspector etc works
    public void test_iadd() throws ClassNotFoundException {
        final int a = 1;
        final int b = 2;
        final Value ret = InspectorInterpreter.start(_teleVM, "util.InterpreterRemoteTestClass",
                                            "iadd", SignatureDescriptor.create("(II)I"),
                                             IntValue.from(a), IntValue.from(b));

        assertTrue(ret instanceof IntValue && ret.asInt() == 3);
    }

    public void test_getstatic() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(_teleVM, "util.InterpreterRemoteTestClass",
                                            "getstatic", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 0xdeadbeef);
    }

    public void test_getfield() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(_teleVM, "util.InterpreterRemoteTestClass",
                                            "getfield", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 0xcafebabe);
    }

    public void test_arraylength() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(_teleVM, "util.InterpreterRemoteTestClass",
                                            "arraylength", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 2);
    }

    public void test_iaload_aaload() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(_teleVM, "util.InterpreterRemoteTestClass",
                                            "iaload_aaload", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 0xba5eba11);
    }

    public void test_invokevirtual2() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(_teleVM, "util.InterpreterRemoteTestClass",
                                            "invokevirtual2", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 3);
    }

    public void test_invokevirtual3() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(_teleVM, "util.InterpreterRemoteTestClass",
                                            "invokevirtual3", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 6);
    }

    public void test_new1() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(_teleVM, "util.InterpreterRemoteTestClass",
                                            "new1", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 0xcafebabe);
    }

    public void test_new2() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(_teleVM, "util.InterpreterRemoteTestClass",
                                            "new2", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 0x80081355);
    }

    public void test_putfield() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(_teleVM, "util.InterpreterRemoteTestClass",
                                            "putfield", SignatureDescriptor.create("()I"));

        assertTrue(ret instanceof IntValue && ret.asInt() == 666);
    }

    public void test_stringnew() throws ClassNotFoundException {
        final Value ret = InspectorInterpreter.start(_teleVM, "util.InterpreterRemoteTestClass",
                                            "stringnew", SignatureDescriptor.create("()Lutil/InterpreterRemoteTestClass;"));

        assertTrue(ret instanceof TeleReferenceValue && ret.unboxObject().toString().equals("test"));
    }
}
