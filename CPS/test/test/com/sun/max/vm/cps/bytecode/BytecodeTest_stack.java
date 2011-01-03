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

import java.lang.reflect.*;

import test.com.sun.max.vm.cps.*;

import com.sun.max.vm.classfile.create.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Unit testing translation of mere stack manipulation byte codes.
 * 
 */
public abstract class BytecodeTest_stack<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_stack(String name) {
        super(name);
    }

    public void test_pop() {
        final String className = getClass().getName() + "_test_pop";

        final int a = 111;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(0, 3);
        code.bipush(a);
        code.bipush(a / 2);
        code.bipush(a / 3);
        code.pop();
        code.pop();
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_pop", SignatureDescriptor.create(int.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_pop", SignatureDescriptor.create(int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void pop() {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertEquals(result.asInt(), a);
    }

    public void test_pop2() {
        final String className = getClass().getName() + "_test_pop2";

        final long a = 1234567812345678L;
        final double b = a / 2;
        final long c = a / 3;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(6, 6);
        code.lload(0); // a
        code.dload(2); // a, b
        code.lload(4); // a, b, c
        code.pop2(); // a, b
        code.pop2(); // a
        code.lreturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_pop2", SignatureDescriptor.create(long.class, long.class, double.class, long.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_pop2", SignatureDescriptor.create(long.class, long.class, double.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void pop2() {
                confirmPresence();
            }
        };
        final Value result = execute(method, LongValue.from(a), DoubleValue.from(b), LongValue.from(c));
        assertEquals(result.asLong(), a);
    }

    public void test_dup() {
        final String className = getClass().getName() + "_test_dup";

        final int a = 77;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(1, 2);
        code.bipush(a); // a
        code.dup(); // a, a
        code.istore(0); // a
        code.pop(); //
        code.iload(0); // a
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dup", SignatureDescriptor.create(int.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_dup", SignatureDescriptor.create(int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dup() {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertEquals(result.asInt(), a);
    }

    public void test_dup_x1() {
        final String className = getClass().getName() + "_test_dup_x1";

        final int a = 2 * 3 * 5;
        final int b = 7 * 11 * 19;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(2, 3);
        code.iload(0); // a
        code.iload(1); // a, b
        code.dup_x1(); // b, a, b
        code.isub(); // b, (a - b)
        code.imul(); // (b * (a - b))
        code.ireturn(); //
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dup_x1", SignatureDescriptor.create(int.class, int.class, int.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_dup_x1", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dup_x1() {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(a), IntValue.from(b));
        assertEquals(result.asInt(), b * (a - b));
    }

    public void test_dup_x2_12() {
        final String className = getClass().getName() + "_test_dup_x2_12";

        final float a1 = (float) 123.456;
        final double b2 = 123123543.2342342354;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(3, 5);
        code.dload(1); // b2
        code.fload(0); // b2, a1
        code.dup_x2(); // a1, b2, a1
        code.f2d(); // a1, b2, a2
        code.dsub(); // a1, (b2 - a2)
        code.dstore(2); // a1
        code.f2d(); // a2
        code.dload(2); // a2, (b2 - a2)
        code.dmul(); // (a2 * (b2 - a2))
        code.dreturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dup_x2_12", SignatureDescriptor.create(double.class, float.class, double.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_dup_x2_12", SignatureDescriptor.create(double.class, float.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dup_x2() {
                confirmPresence();
            }
        };
        final Value result = execute(method, FloatValue.from(a1), DoubleValue.from(b2));
        assertEquals(result.asDouble(), a1 * (b2 - a1));
    }

    public void test_dup_x2_111() {
        final String className = getClass().getName() + "_test_dup_x2_111";

        final int a = 2;
        final int b = 41;
        final int c = 2003;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(3, 4);
        code.iload(2); // c
        code.iload(1); // c, b
        code.iload(0); // c, b, a
        code.dup_x2(); // a, c, b, a
        code.isub(); // a, c, (b - a)
        code.isub(); // a, (c - (b - a))
        code.imul(); // (a * (c - (b - a)))
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dup_x2_111", SignatureDescriptor.create(int.class, int.class, int.class, int.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_dup_x2_111", SignatureDescriptor.create(int.class, int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dup_x2() {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(a), IntValue.from(b), IntValue.from(c));
        assertEquals(result.asInt(), a * (c - (b - a)));
    }

    public void test_dup2_2() {
        final String className = getClass().getName() + "_test_dup2_2";

        final double a = 22424563453.24243234234;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(4, 4);
        code.dload(0); // a
        code.dup2(); // a, a
        code.dstore(2); // a
        code.pop2(); //
        code.dload(2); // a
        code.dreturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dup2_2", SignatureDescriptor.create(double.class, double.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_dup2_2", SignatureDescriptor.create(double.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dup2() {
                confirmPresence();
            }
        };
        final Value result = execute(method, DoubleValue.from(a));
        assertEquals(result.asDouble(), a);
    }

    public void test_dup2_11() {
        final String className = getClass().getName() + "_test_dup2_11";

        final int a = 123;
        final int b = 474;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(4, 4);
        code.iload(0); // a
        code.iload(1); // a, b
        code.dup2(); // a, b, a, b
        code.istore(3); // a, b, a
        code.istore(2); // a, b
        code.pop2(); //
        code.iload(2); // a
        code.iload(3); // a, b
        code.isub(); // (a - b)
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dup2_11", SignatureDescriptor.create(int.class, int.class, int.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_dup2_11", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dup2() {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(a), IntValue.from(b));
        assertEquals(result.asInt(), a - b);
    }

    public void test_dup2_x1_21() {
        final String className = getClass().getName() + "_test_dup2_x1_21";

        final long a2 = 1000000000L;
        final int b1 = -7;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(5, 6);
        code.iload(2); // b1
        code.lload(0); // b1, a2
        code.dup2_x1(); // a2, b1, a2
        code.lstore(3); // a2, b1
        code.i2l(); // a2, b2
        code.lload(3); // a2, b2, a2
        code.lsub(); // a2, (b2 - a2)
        code.lmul(); // (a2 * (b2 - a2))
        code.lreturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dup2_x1_21", SignatureDescriptor.create(long.class, long.class, int.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_dup2_x1_21", SignatureDescriptor.create(long.class, long.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dup2_x1() {
                confirmPresence();
            }
        };
        final Value result = execute(method, LongValue.from(a2), IntValue.from(b1));
        assertEquals(result.asLong(), a2 * (b1 - a2));
    }

    public void test_dup2_x1_111() {
        final String className = getClass().getName() + "_test_dup2_x1_111";

        final int a = 10;
        final int b = 7893;
        final int c = 123456;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(3, 5);
        code.iload(0); // a
        code.iload(1); // a, b
        code.iload(2); // a, b, c
        code.dup2_x1(); // b, c, a, b, c
        code.isub(); // b, c, a, (b - c)
        code.imul(); // b, c, (a * (b - c))
        code.iadd(); // b, (c + (a * (b - c)))
        code.imul(); // (b * (c + (a * (b - c))))
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dup2_x1_111", SignatureDescriptor.create(int.class, int.class, int.class, int.class), code);
        final byte[] classfileBytes = millClass.generate();
        final Method_Type method = compileMethod(className, classfileBytes, "perform_dup2_x1_111", SignatureDescriptor.create(int.class, int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dup2_x1() {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(a), IntValue.from(b), IntValue.from(c));
        assertEquals(result.asInt(), b * (c + (a * (b - c))));
    }

    public void test_dup2_x2_22() {
        final String className = getClass().getName() + "_test_dup2_x2_22";

        final long a = 173527L;
        final long b = 892L;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(4, 6);
        code.lload(0); // a
        code.lload(2); // a, b
        code.dup2_x2(); // b, a, b
        code.lsub(); // b, (a - b)
        code.lmul(); // (b * (a - b))
        code.lreturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dup2_x2_22", SignatureDescriptor.create(long.class, long.class, long.class), code);
        final byte[] classfileBytes = millClass.generate();
        final Method_Type method = compileMethod(className, classfileBytes, "perform_dup2_x2_22", SignatureDescriptor.create(long.class, long.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dup2_x2() {
                confirmPresence();
            }
        };
        final Value result = execute(method, LongValue.from(a), LongValue.from(b));
        assertEquals(result.asLong(), b * (a - b));
    }

    public void test_dup2_x2_211() {
        final String className = getClass().getName() + "_test_dup2_x2_211";
        final long a2 = 86762L;
        final int b1 = 3;
        final int c1 = -343;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(6, 7);
        code.iload(3); // c1
        code.iload(2); // c1, b1
        code.lload(0); // c1, b1, a2
        code.dup2_x2(); // a2, c1, b1, a2
        code.lstore(4); // a2, c1, b1
        code.i2l(); // a2, c1, b2
        code.lload(4); // a2, c1, b2, a2
        code.lsub(); // a2, c1, (b2 - a2)
        code.lstore(4); // a2, c1
        code.i2l(); // a2, c2
        code.lmul(); // (a2 * c2)
        code.lload(4); // (a2 * c2), (b2 - a2)
        code.ldiv(); // ((a2 * c2) / (b2 - a2))
        code.lreturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dup2_x2_211", SignatureDescriptor.create(long.class, long.class, int.class, int.class), code);
        final byte[] classfileBytes = millClass.generate();
        final Method_Type method = compileMethod(className, classfileBytes, "perform_dup2_x2_211", SignatureDescriptor.create(long.class, long.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dup2_x2() {
                confirmPresence();
            }
        };
        final Value result = execute(method, LongValue.from(a2), IntValue.from(b1), IntValue.from(c1));
        assertEquals(result.asLong(), (a2 * c1) / (b1 - a2));
    }

    public void test_dup2_x2_112() {
        final String className = getClass().getName() + "_test_dup2_x2_112";
        final int a1 = 5;
        final int b1 = -10099;
        final long c2 = 28282872L;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(4, 8);
        code.lload(2); // c2
        code.iload(1); // c2, b1
        code.iload(0); // c2, b1, a1
        code.dup2_x2(); // b1, a1, c2, b1, a1
        code.isub(); // b1, a1, c2, (b1 - a1)
        code.i2l(); // b1, a1, c2, (b2 - a2)
        code.lmul(); // b1, a1, (c2 * (b2 - a2))
        code.lstore(2); // b1, a1
        code.idiv(); // (b1 / a1)
        code.i2l(); // (b2 / a2)
        code.lload(2); // (b2 / a2), (c2 * (b2 - a2))
        code.lsub(); // ((b2 / a2) - (c2 * (b2 - a2)))
        code.lreturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dup2_x2_112", SignatureDescriptor.create(long.class, int.class, int.class, long.class), code);
        final byte[] classfileBytes = millClass.generate();
        final Method_Type method = compileMethod(className, classfileBytes, "perform_dup2_x2_112", SignatureDescriptor.create(long.class, int.class, int.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dup2_x2() {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(a1), IntValue.from(b1), LongValue.from(c2));
        assertEquals(result.asLong(), (b1 / a1) - (c2 * (b1 - a1)));
    }

    public void test_dup2_x2_1111() {
        final String className = getClass().getName() + "_test_dup2_x2_1111";
        final int a = 23;
        final int b = 111;
        final int c = 1009;
        final int d = 12345;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(4, 6);
        code.iload(0); // a
        code.iload(1); // a, b
        code.iload(2); // a, b, c
        code.iload(3); // a, b, c, d
        code.dup2_x2(); // c, d, a, b, c, d
        code.isub(); // c, d, a, b, (c - d)
        code.imul(); // c, d, a, (b * (c - d))
        code.isub(); // c, d, (a - (b * (c - d)))
        code.imul(); // c, (d * (a - (b * (c - d))))
        code.isub(); // (c - (d * (a - (b * (c - d)))))
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dup2_x2_1111", SignatureDescriptor.create(int.class, int.class, int.class, int.class, int.class), code);
        final byte[] classfileBytes = millClass.generate();
        final Method_Type method = compileMethod(className, classfileBytes, "perform_dup2_x2_1111", SignatureDescriptor.create(int.class, int.class, int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dup2_x2() {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(a), IntValue.from(b), IntValue.from(c), IntValue.from(d));
        assertEquals(result.asInt(), c - (d * (a - (b * (c - d)))));
    }

    public void test_swap() {
        final String className = getClass().getName() + "_test_swap";

        final int a = 40;
        final int b = 17;
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(0, 2);
        code.bipush(a); // a
        code.bipush(b); // a, b
        code.swap(); // b, a
        code.isub(); // (b - a)
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_swap", SignatureDescriptor.create(int.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_swap", SignatureDescriptor.create(int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void swap() {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertEquals(result.asInt(), b - a);
    }
}
