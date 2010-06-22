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

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.test.*;
import com.sun.max.vm.value.*;

/**
 * Interpreter tests on a tele VM.
 * @author Athul Acharya
 */

@RunWith(org.junit.runners.AllTests.class)
public class TeleInterpreterRemoteTest extends TeleInterpreterTestCase {

    private static TeleVM teleVM;

    public TeleInterpreterRemoteTest() {
    }

    public TeleInterpreterRemoteTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TeleInterpreterRemoteTest.class);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(TeleInterpreterRemoteTest.class.getSimpleName());
        suite.addTestSuite(TeleInterpreterRemoteTest.class);
        return new TeleInterpreterTestSetup(suite);
    }

    @Override
    protected TeleInterpreter createInterpreter() {
        return new TeleInterpreter(teleVM());
    }

    @Override
    protected Class defaultDeclaringClass() {
        return TeleInterpreterRemoteTestClass.class;
    }

    @Override
    protected TeleVM teleVM() {
        synchronized (getClass()) {
            if (teleVM == null) {
                try {
                    teleVM = TeleVM.create(new Options());
                    teleVM.refresh(0);
                } catch (BootImageException e) {
                    throw ProgramError.unexpected(e);
                }
            }
        }
        return teleVM;
    }

    //this test is just here as a sanity check that starting the inspector etc works
    public void test_iadd() {
        final int a = 1;
        final int b = 2;
        final Value ret = execute("iadd", IntValue.from(a), IntValue.from(b));
        assertTrue(ret.asInt() == 3);
    }

    public void test_getstatic() {
        final Value ret = execute("getstatic");
        assertTrue(ret.asInt() == 0xdeadbeef);
    }

    public void test_getfield() {
        final Value ret = execute("getfield");
        assertTrue(ret.asInt() == 0xcafebabe);
    }

    public void test_arraylength() {
        final Value ret = execute("arraylength");
        assertTrue(ret.asInt() == 2);
    }

    public void test_iaload_aaload() {
        final Value ret = execute("iaload_aaload");
        assertTrue(ret.asInt() == 0xba5eba11);
    }

    public void test_invokevirtual2() {
        final Value ret = execute("invokevirtual2");
        assertTrue(ret.asInt() == 3);
    }

    public void test_invokevirtual3() {
        final Value ret = execute("invokevirtual3");
        assertTrue(ret.asInt() == 6);
    }

    public void test_new1() {
        final Value ret = execute("new1");

        assertTrue(ret.asInt() == 0xcafebabe);
    }

    public void test_new2() {
        final Value ret = execute("new2");
        assertTrue(ret.asInt() == 0x80081355);
    }

    public void test_putfield() {
        final Value ret = execute("putfield");
        assertTrue(ret.asInt() == 666);
    }

    public void test_stringnew() {
        final Value ret = execute("stringnew");
        assertTrue(ret.unboxObject().toString().equals("test"));
    }
}
