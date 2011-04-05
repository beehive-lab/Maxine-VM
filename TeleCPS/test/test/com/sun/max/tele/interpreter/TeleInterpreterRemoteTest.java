/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.tele.interpreter;

import junit.framework.*;

import org.junit.runner.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.Options;
import com.sun.max.vm.hosted.*;
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
    protected TeleIrInterpreter createInterpreter() {
        return new TeleIrInterpreter(teleVM());
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
                    teleVM.updateVMCaches(0L);
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
