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

package test.com.sun.max.vm.cps;

import com.sun.max.annotate.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class CompilerTest_controlFlow<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected CompilerTest_controlFlow(String name) {
        super(name);
    }

    private void foo5(int i) {
    }

    public int undefinedLocalVariable(Object object) {
        if (object == null) {
            final int i = 123; // local 1 is defined here
            foo5(i);
        } else {
            // local 1 is undefined here
            foo1();
        }
        // local 1 may be undefined here
        foo2();
        return 0;
    }

    public void test_javaFrameDescriptorWithLocalVariable() {
        compileMethod(CompilerTest_controlFlow.class, "undefinedLocalVariable");
    }

    private static int perform_forLoop(int m) {
        int s = 0;
        for (int i = 0; i <= m; i++) {
            s += i;
        }
        return s;
    }

    public void test_forLoop() {
        final Method_Type method = compileMethod(CompilerTest_controlFlow.class, "perform_forLoop");
        Value result = execute(method, IntValue.from(0));
        assertTrue(result.asInt() == 0);
        result = execute(method, IntValue.from(3));
        assertTrue(result.asInt() == 6);
    }

    private static int perform_ifThenElseForLoop(int m) {
        int s = 0;
        if (m > 0) {
            for (int i = 0; i <= m; i++) {
                s += i;
            }
        } else {
            for (int i = 0; i <= m; i++) {
                s += i;
            }
        }
        if (m > 0) {
            for (int i = 0; i <= m; i++) {
                s += i;
            }
        } else {
            for (int i = 0; i <= m; i++) {
                s += i;
            }
        }
        return s;
    }

    public void test_ifThenElseForLoop() {
        final Method_Type method = compileMethod(CompilerTest_controlFlow.class, "perform_ifThenElseForLoop");
        Value result = execute(method, IntValue.from(0));
        assertTrue(result.asInt() == 0);
        result = execute(method, IntValue.from(3));
        assertTrue(result.asInt() == 12);
    }

    @INLINE
    private static int inlinedLoop(int m, int s) {
        int k = s;
        for (int i = 0; i <= m; i++) {
            k += i;
        }
        return k;
    }

    private static int perform_inlinedForLoop(int m) {
        int s = 0;
        if (m > 0) {
            s = inlinedLoop(m, s);
        } else {
            s = inlinedLoop(m, s);
        }
        if (m > 0) {
            s = inlinedLoop(m, s);
        } else {
            s = inlinedLoop(m, s);
        }
        return s;
    }

    public void test_inlinedForLoop() {
        final Method_Type method = compileMethod(CompilerTest_controlFlow.class, "perform_inlinedForLoop");
        Value result = execute(method, IntValue.from(0));
        assertTrue(result.asInt() == 0);
        result = execute(method, IntValue.from(3));
        assertTrue(result.asInt() == 12);
    }

    private static boolean condition = true;

    private static void foo1() {
    }

    private static void foo2() {
    }

    private static void foo3() {
    }

    public static int ifThenElse() {
        if (condition) {
            if (condition) {
                foo1();
            } else {
                foo2();
            }
        }
        return also();
    }

    public void test_ifThenElse() {
        final Method_Type method = compileMethod("ifThenElse", SignatureDescriptor.create(int.class));
        final Value result = execute(method);
        assertTrue(result.asInt() == 10);
    }

    @INLINE
    private static void inlinedChain() {
        if (condition) {
            foo1();
        }
        if (condition) {
            foo2();
        }
        foo3();
    }

    public static int also() {
        return 10;
    }

    public static int nestedIf() {
        inlinedChain();
        return also();
    }

    public void test_nestedIf() {
        final Method_Type method = compileMethod("nestedIf", SignatureDescriptor.create(int.class));
        final Value result = execute(method);
        assertTrue(result.asInt() == 10);
    }

    @INLINE
    private boolean inlinedIf(int i) {
        if (i >= 4 && i <= 10) {
            return true;
        }
        return false;
    }

    public void inlinedIfThenThrow(int n) {
        if (inlinedIf(n)) {
            throw new ClassFormatError();
        }
    }

    public void test_inlinedIfThenThrow() {
        final Method_Type method = compileMethod("inlinedIfThenThrow", SignatureDescriptor.create(void.class, int.class));
        executeWithReceiverAndExpectedException(method, ClassFormatError.class, IntValue.from(5));
        executeWithReceiver(method, IntValue.from(11));
    }

    private static String join(String s1, int i1, String s2, int i2, String s3, int i3, String s4, int i4) {
        return s1 + i1 + s2 + i2 + s3 + i3 + s4 + i4;
    }

    private static String perform_ternaries(Object o1, int i1, Object o2, int i2, Object o3, int i3, Object o4, int i4) {
        final String result = join(
               o1 == null ? "null" : o1.toString(), i1,
               o2 == null ? "null" : o2.toString(), i2,
               o3 == null ? "null" : o3.toString(), i3,
               o4 == null ? "null" : o4.toString(), i4);
        return result == null ? "null" : result;
    }

    public void test_ternaries() {
        final Method_Type method = compileMethod("perform_ternaries", SignatureDescriptor.create(String.class, Object.class, int.class, Object.class, int.class, Object.class, int.class, Object.class, int.class));
        final Value value = ReferenceValue.from("0");
        final Value intValue = IntValue.from(1);
        final Value result = execute(method, value, intValue, value, intValue, value, intValue, value, intValue);
        assertTrue(result.asObject().equals("01010101"));
    }

    @CONSTANT
    private static int one = 1;

    public static int propagateConstantBlockArgument() {
        // This constant is supposed to be propagated into all the basic blocks below:
        final int a = one;

        int b = 2;
        if (condition) {
            b += a;
        } else {
            b += a;
        }
        if (condition) {
            b += a;
        } else {
            b += a;
        }
        return b;
    }

    public void test_constantBlockArgumentPropagation() {
        final Method_Type method = compileMethod("propagateConstantBlockArgument", SignatureDescriptor.create(int.class));
        // TODO: verify that constant propagation does actually occur (for now we print the IR and look)
        final Value result = execute(method);
        assertTrue(result.asInt() == 4);
    }

}
