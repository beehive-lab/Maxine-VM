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
package test.com.sun.max.vm.bytecode;

import java.util.Arrays;

import junit.framework.*;
import test.com.sun.max.vm.*;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.graft.*;
import com.sun.max.vm.classfile.*;

@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class BytecodePreprocessorTest extends VmTestCase {

    public BytecodePreprocessorTest(String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(BytecodePreprocessorTest.class.getName());
        suite.addTestSuite(BytecodePreprocessorTest.class);
        return new VmTestSetup(suite);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BytecodePreprocessorTest.suite());
    }

    private synchronized float perform_virtualSync(int v) {
        if (v < 0) {
            return -1.0f;
        } else if (v > 0) {
            return 1.0f;
        } else {
            return 0.0f;
        }
    }

    private static synchronized String perform_staticSync(int v) {
        if (v < 0) {
            return "-1";
        } else if (v > 0) {
            return "1";
        } else {
            return "0";
        }
    }

    private void assertProcessedCodeIsDifferent(String methodName) {
        ExceptionDispatchingPreprocessor.REQUIRED = true;
        SynchronizedMethodPreprocessor.REQUIRED = true;
        final ClassMethodActor classMethodActor = ClassMethodActor.fromJava(Classes.getDeclaredMethod(getClass(), methodName, int.class));
        final CodeAttribute originalCodeAttribute = classMethodActor.originalCodeAttribute(true);
        assertFalse(Arrays.equals(originalCodeAttribute.code(), classMethodActor.compilee().codeAttribute().code()));
    }

    public void test_virtualSync() {
        assertProcessedCodeIsDifferent("perform_virtualSync");
    }

    public void test_staticSync() {
        assertProcessedCodeIsDifferent("perform_staticSync");
    }
}
