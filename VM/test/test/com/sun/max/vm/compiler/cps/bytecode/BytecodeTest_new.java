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
package test.com.sun.max.vm.compiler.cps.bytecode;

import test.com.sun.max.vm.compiler.cps.*;

import com.sun.max.vm.compiler.cps.ir.*;
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
