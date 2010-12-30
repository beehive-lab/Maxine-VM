/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.cps.bytecode;

import test.com.sun.max.vm.cps.*;

import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_new<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_new(String name) {
        super(name);
    }

    public static class CreationTest {

        public CreationTest() {
        }
    }

    @STUB_TEST_PROPERTIES(compareResult = false)
    private static CreationTest perform_new() {
        return new CreationTest();
    }

    public void test_new() {
        final Method_Type method = compileMethod("perform_new", SignatureDescriptor.create(CreationTest.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void new_(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asObject() instanceof CreationTest);
    }

    @STUB_TEST_PROPERTIES(compareResult = false)
    private static int[] perform_newarray() {
        return new int[7];
    }

    public void test_newarray() {
        final Method_Type method = compileMethod("perform_newarray", SignatureDescriptor.create(int[].class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void newarray(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asObject() instanceof int[]);
        assertTrue(((int[]) result.asObject()).length == 7);
    }

    @STUB_TEST_PROPERTIES(compareResult = false)
    private static CreationTest[] perform_anewarray() {
        return new CreationTest[8];
    }

    public void test_anewarray() {
        final Method_Type method = compileMethod("perform_anewarray", SignatureDescriptor.create(CreationTest[].class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void anewarray(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asObject() instanceof CreationTest[]);
        assertTrue(((CreationTest[]) result.asObject()).length == 8);
    }

    @STUB_TEST_PROPERTIES(compareResult = false)
    private static CreationTest[][] perform_multianewarray_2() {
        return new CreationTest[3][2];
    }

    public void test_multianewarray_2() {
        final Method_Type method = compileMethod("perform_multianewarray_2", SignatureDescriptor.create(CreationTest[][].class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void multianewarray(int index, int nDimensions) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asObject() instanceof CreationTest[][]);
        assertTrue(((CreationTest[][]) result.asObject()).length == 3);
        assertTrue(((CreationTest[][]) result.asObject())[0].length == 2);
    }

}
