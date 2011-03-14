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
package test.com.sun.max.vm.cps.bytecode;

import java.lang.reflect.*;

import test.com.sun.max.vm.cps.*;
import test.com.sun.max.vm.cps.bytecode.create.*;

import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_wide<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_wide(String name) {
        super(name);
    }

    public void test_wide_iload() {
        final String className = getClass().getName() + "_test_wide_iload";

        final int a = 10;
        final int b = 3;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(2, 2);
        code.wide_iload(0);
        code.iload(1);
        code.isub();
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_wide_iload", SignatureDescriptor.create(int.class, int.class, int.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_wide_iload", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void wide() {
                confirmPresence();
            }

            @Override
            public void iload(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(a), IntValue.from(b));
        assertEquals(result.asInt(), a - b);
    }

    public void test_wide_lload() {
        final String className = getClass().getName() + "_test_wide_lload";

        final long a = 10;
        final long b = 3;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(4, 4);
        code.lload(0);
        code.wide_lload(2);
        code.lsub();
        code.lreturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_wide_lload", SignatureDescriptor.create(long.class, long.class, long.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_wide_lload", SignatureDescriptor.create(long.class, long.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void wide() {
                confirmPresence();
            }

            @Override
            public void lload(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, LongValue.from(a), LongValue.from(b));
        assertEquals(result.asLong(), a - b);
    }

    public void test_wide_fload() {
        final String className = getClass().getName() + "_test_wide_fload";

        final float a = 10;
        final float b = 3;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(2, 2);
        code.fload(0);
        code.wide_fload(1);
        code.fsub();
        code.freturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_wide_fload", SignatureDescriptor.create(float.class, float.class, float.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_wide_fload", SignatureDescriptor.create(float.class, float.class, float.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void wide() {
                confirmPresence();
            }

            @Override
            public void fload(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, FloatValue.from(a), FloatValue.from(b));
        assertEquals(result.asFloat(), a - b);
    }

    public void test_wide_dload() {
        final String className = getClass().getName() + "_test_wide_dload";

        final double a = 10;
        final double b = 3;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(4, 4);
        code.wide_dload(0);
        code.dload(2);
        code.dsub();
        code.dreturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_wide_dload", SignatureDescriptor.create(double.class, double.class, double.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_wide_dload", SignatureDescriptor.create(double.class, double.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void wide() {
                confirmPresence();
            }

            @Override
            public void dload(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, DoubleValue.from(a), DoubleValue.from(b));
        assertEquals(result.asDouble(), a - b);
    }

    public void test_wide_aload() {
        final String className = getClass().getName() + "_test_wide_aload";

        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(1, 1);
        code.wide_aload(0);
        code.areturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_wide_aload", SignatureDescriptor.create(Object.class, Object.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_wide_aload", SignatureDescriptor.create(Object.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void wide() {
                confirmPresence();
            }

            @Override
            public void aload(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, ReferenceValue.from(method));
        assertTrue(result.asObject() == method);
    }

    public void test_wide_istore() {
        final String className = getClass().getName() + "_test_wide_istore";

        final int a = 5;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(1, 1);
        code.bipush(a);
        code.wide_istore(0);
        code.iload(0);
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_wide_istore", SignatureDescriptor.create(int.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_wide_istore", SignatureDescriptor.create(int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void wide() {
                confirmPresence();
            }

            @Override
            public void istore(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertEquals(result.asInt(), a);
    }

    public void test_wide_lstore() {
        final String className = getClass().getName() + "_test_wide_lstore";

        final long a = 12345678;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(4, 2);
        code.lload(0);
        code.wide_lstore(2);
        code.lload(2);
        code.lreturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_wide_lstore", SignatureDescriptor.create(long.class, long.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_wide_lstore", SignatureDescriptor.create(long.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void wide() {
                confirmPresence();
            }

            @Override
            public void lstore(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, LongValue.from(a));
        assertEquals(result.asLong(), a);
    }

    public void test_wide_fstore() {
        final String className = getClass().getName() + "_test_wide_fstore";

        final float a = 100987;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(2, 1);
        code.fload(0);
        code.wide_fstore(1);
        code.fload(1);
        code.freturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_wide_fstore", SignatureDescriptor.create(float.class, float.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_wide_fstore", SignatureDescriptor.create(float.class, float.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void wide() {
                confirmPresence();
            }

            @Override
            public void fstore(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, FloatValue.from(a));
        assertEquals(result.asFloat(), a);
    }

    public void test_wide_dstore() {
        final String className = getClass().getName() + "_test_wide_dstore";

        final double a = 534532;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(4, 2);
        code.dload(0);
        code.wide_dstore(2);
        code.dload(2);
        code.dreturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_wide_dstore", SignatureDescriptor.create(double.class, double.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_wide_dstore", SignatureDescriptor.create(double.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void wide() {
                confirmPresence();
            }

            @Override
            public void dstore(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, DoubleValue.from(a));
        assertEquals(result.asDouble(), a);
    }

    public void test_wide_astore() {
        final String className = getClass().getName() + "_test_wide_astore";

        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(2, 1);
        code.aload(0);
        code.wide_astore(1);
        code.aload(1);
        code.areturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_wide_astore", SignatureDescriptor.create(Object.class, Object.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_wide_astore", SignatureDescriptor.create(Object.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void wide() {
                confirmPresence();
            }

            @Override
            public void astore(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, ReferenceValue.from(method));
        assertTrue(result.asObject() == method);
    }

    public void test_wide_iinc() {
        final String className = getClass().getName() + "_test_wide_iinc";

        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(1, 1);
        code.wide_iinc(0, 10);
        code.iload(0);
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_wide_iinc", SignatureDescriptor.create(int.class, int.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_wide_iinc", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void wide() {
                confirmPresence();
            }

            @Override
            public void iinc(int index, int addend) {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(8));
        assertEquals(result.asInt(), 18);
    }
}
