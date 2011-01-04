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
package test.com.sun.max.vm.cps;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public abstract class CompilerTest_comparisons<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected CompilerTest_comparisons(String name) {
        super(name);
    }

    private static boolean useAddressGreaterThanDirectly(Address a, Address b) {
        return a.greaterThan(b);
    }

    private static boolean useAddressGreaterThanEqualDirectly(Address a, Address b) {
        return a.greaterEqual(b);
    }

    private static boolean useAddressLessThanDirectly(Address a, Address b) {
        return a.lessThan(b);
    }

    private static boolean useAddressLessThanEqualDirectly(Address a, Address b) {
        return a.lessEqual(b);
    }

    private static boolean useUnsignedIntGreaterEqualDirectly(int a, int b) {
        return SpecialBuiltin.aboveEqual(a, b);
    }

    public void test_forceAddressGreaterThan() {
        final Method_Type method = compileMethod(CompilerTest_comparisons.class, "useAddressGreaterThanDirectly");
        assertTrue(method.contains(AddressBuiltin.GreaterThan.BUILTIN, true));

        Value result = execute(method, new WordValue(Address.fromInt(3)), new WordValue(Address.fromInt(4)));
        assertFalse(result.asBoolean());

        result = execute(method, new WordValue(Address.fromInt(7)), new WordValue(Address.fromInt(5)));
        assertTrue(result.asBoolean());

        result = execute(method, new WordValue(Address.fromInt(88)), new WordValue(Address.fromInt(88)));
        assertFalse(result.asBoolean());

        result = execute(method, new WordValue(Address.fromLong(0xfff3ff1fff2fff4fL)), new WordValue(Address.fromLong(0x0003ff00000fff4fL)));
        assertTrue(result.asBoolean());

        result = execute(method, new WordValue(Address.fromLong(0x0003ff00000fff4fL)), new WordValue(Address.fromLong(0xfff3ff1fff2fff4fL)));
        assertFalse(result.asBoolean());
    }

    public void test_forceAddressGreaterEqual() {
        final Method_Type method = compileMethod(CompilerTest_comparisons.class, "useAddressGreaterThanEqualDirectly");
        assertTrue(method.contains(AddressBuiltin.GreaterEqual.BUILTIN, true));

        Value result = execute(method, new WordValue(Address.fromInt(3)), new WordValue(Address.fromInt(4)));
        assertFalse(result.asBoolean());

        result = execute(method, new WordValue(Address.fromInt(7)), new WordValue(Address.fromInt(5)));
        assertTrue(result.asBoolean());

        result = execute(method, new WordValue(Address.fromInt(88)), new WordValue(Address.fromInt(88)));
        assertTrue(result.asBoolean());

        result = execute(method, new WordValue(Address.fromLong(0xfff3ff1fff2fff4fL)), new WordValue(Address.fromLong(0x0003ff00000fff4fL)));
        assertTrue(result.asBoolean());

        result = execute(method, new WordValue(Address.fromLong(0x0003ff00000fff4fL)), new WordValue(Address.fromLong(0xfff3ff1fff2fff4fL)));
        assertFalse(result.asBoolean());
    }

    public void test_forceUnsignedIntGreaterEqual() {
        //Trace.on(3);
        final Method_Type method = compileMethod(CompilerTest_comparisons.class, "useUnsignedIntGreaterEqualDirectly");
        //Trace.on(1);
        assertTrue(method.contains(SpecialBuiltin.AboveEqual.BUILTIN, true));

        Value result = execute(method, IntValue.from(3), IntValue.from(4));
        assertFalse(result.asBoolean());

        result = execute(method, IntValue.from(5), IntValue.from(7));
        assertFalse(result.asBoolean());

        result = execute(method, IntValue.from(88), IntValue.from(88));
        assertTrue(result.asBoolean());
    }

    public void test_forceAddressLessThan() {
        final Method_Type method = compileMethod(CompilerTest_comparisons.class, "useAddressLessThanDirectly");
        assertTrue(method.contains(AddressBuiltin.LessThan.BUILTIN, true));

        Value result = execute(method, new WordValue(Address.fromInt(4)), new WordValue(Address.fromInt(3)));
        assertFalse(result.asBoolean());

        result = execute(method, new WordValue(Address.fromInt(5)), new WordValue(Address.fromInt(7)));
        assertTrue(result.asBoolean());

        result = execute(method, new WordValue(Address.fromInt(88)), new WordValue(Address.fromInt(88)));
        assertFalse(result.asBoolean());

        result = execute(method, new WordValue(Address.fromLong(0xfff3ff1fff2fff4fL)), new WordValue(Address.fromLong(0x0003ff00000fff4fL)));
        assertFalse(result.asBoolean());

        result = execute(method, new WordValue(Address.fromLong(0x0003ff00000fff4fL)), new WordValue(Address.fromLong(0xfff3ff1fff2fff4fL)));
        assertTrue(result.asBoolean());

    }

    public void test_forceAddressLessEqual() {
        // Trace.on(5);
        final Method_Type method = compileMethod(CompilerTest_comparisons.class, "useAddressLessThanEqualDirectly");
        assertTrue(method.contains(AddressBuiltin.LessEqual.BUILTIN, true));

        Value result = execute(method, new WordValue(Address.fromInt(4)), new WordValue(Address.fromInt(3)));
        assertFalse(result.asBoolean());

        result = execute(method, new WordValue(Address.fromInt(5)), new WordValue(Address.fromInt(7)));
        assertTrue(result.asBoolean());

        result = execute(method, new WordValue(Address.fromInt(88)), new WordValue(Address.fromInt(88)));
        assertTrue(result.asBoolean());

        result = execute(method, new WordValue(Address.fromLong(0xfff3ff1fff2fff4fL)), new WordValue(Address.fromLong(0x0003ff00000fff4fL)));
        assertFalse(result.asBoolean());

        result = execute(method, new WordValue(Address.fromLong(0x0003ff00000fff4fL)), new WordValue(Address.fromLong(0xfff3ff1fff2fff4fL)));
        assertTrue(result.asBoolean());
    }

    private static int addressGreaterThan(Address a, Address b) {
        if (a.greaterThan(b)) {
            return 10;
        }
        return 20;
    }

    private static boolean addressGreaterThanMax(Address a) {
        return a.greaterThan(Address.max());
    }

    private static boolean addressZeroGreaterThan(Address b) {
        return Address.zero().greaterThan(b);
    }

    public void test_addressGreaterThan() {
        Method_Type method = compileMethod(CompilerTest_comparisons.class, "addressGreaterThan");
        assertFalse(method.contains(AddressBuiltin.GreaterThan.BUILTIN, false));

        Value result = execute(method, new WordValue(Address.fromInt(3)), new WordValue(Address.fromInt(4)));
        assertTrue(result.asInt() == 20);

        result = execute(method, new WordValue(Address.fromInt(7)), new WordValue(Address.fromInt(5)));
        assertTrue(result.asInt() == 10);

        result = execute(method, new WordValue(Address.fromLong(-1L)), new WordValue(Address.fromInt(5)));
        assertTrue(result.asInt() == 10);

        method = compileMethod(CompilerTest_comparisons.class, "addressGreaterThanMax");
        assertFalse(method.contains(AddressBuiltin.GreaterThan.BUILTIN, false));
        result = execute(method, new WordValue(Address.fromInt(5)));
        assertFalse(result.asBoolean());

        method = compileMethod(CompilerTest_comparisons.class, "addressZeroGreaterThan");
        assertFalse(method.contains(AddressBuiltin.GreaterThan.BUILTIN, false));
        result = execute(method, new WordValue(Address.fromInt(5)));
        assertFalse(result.asBoolean());
    }

    private static int addressGreaterEqual(Address a, Address b) {
        if (a.greaterEqual(b)) {
            return 10;
        }
        return 20;
    }

    private static boolean addressGreaterEqualZero(Address a) {
        return a.greaterEqual(Address.zero());
    }

    private static boolean addressMaxGreaterEqual(Address b) {
        return Address.max().greaterEqual(b);
    }

    public void test_addressGreaterEqual() {
        Method_Type method = compileMethod(CompilerTest_comparisons.class, "addressGreaterEqual");
        assertFalse(method.contains(AddressBuiltin.GreaterEqual.BUILTIN, false));

        Value result = execute(method, new WordValue(Address.fromInt(3)), new WordValue(Address.fromInt(4)));
        assertTrue(result.asInt() == 20);

        result = execute(method, new WordValue(Address.fromInt(7)), new WordValue(Address.fromInt(5)));
        assertTrue(result.asInt() == 10);

        result = execute(method, new WordValue(Address.fromLong(-1L)), new WordValue(Address.fromInt(5)));
        assertTrue(result.asInt() == 10);

        method = compileMethod(CompilerTest_comparisons.class, "addressGreaterEqualZero");
        assertFalse(method.contains(AddressBuiltin.GreaterEqual.BUILTIN, false));
        result = execute(method, new WordValue(Address.fromInt(5)));
        assertTrue(result.asBoolean());

        method = compileMethod(CompilerTest_comparisons.class, "addressMaxGreaterEqual");
        assertFalse(method.contains(AddressBuiltin.GreaterEqual.BUILTIN, false));
        result = execute(method, new WordValue(Address.fromInt(5)));
        assertTrue(result.asBoolean());
    }

    private static int unsignedIntGreaterEqual(int a, int b) {
        if (SpecialBuiltin.aboveEqual(a, b)) {
            return 10;
        }
        return 20;
    }

    private static boolean unsignedIntGreaterEqualZero(int a) {
        return SpecialBuiltin.aboveEqual(a, 0);
    }

    private static boolean unsignedIntMaxGreaterEqual(int b) {
        return SpecialBuiltin.aboveEqual(0xffffffff, b);
    }

    public void test_unsignedIntGreaterEqual() {
        Method_Type method = compileMethod(CompilerTest_comparisons.class, "unsignedIntGreaterEqual");
        assertFalse(method.contains(AddressBuiltin.GreaterEqual.BUILTIN, false));

        Value result = execute(method, IntValue.from(-3), IntValue.from(4));
        assertEquals(result.asInt(), 10);

        result = execute(method, IntValue.from(5), IntValue.from(5));
        assertTrue(result.asInt() == 10);

        result = execute(method, IntValue.from(7), IntValue.from(5));
        assertTrue(result.asInt() == 10);

        method = compileMethod(CompilerTest_comparisons.class, "unsignedIntGreaterEqualZero");
        assertFalse(method.contains(AddressBuiltin.GreaterEqual.BUILTIN, false));
        result = execute(method, IntValue.from(6));
        assertTrue(result.asBoolean());

        method = compileMethod(CompilerTest_comparisons.class, "unsignedIntMaxGreaterEqual");
        assertFalse(method.contains(AddressBuiltin.GreaterEqual.BUILTIN, false));
        result = execute(method, IntValue.from(6));
        assertTrue(result.asBoolean());
    }

    private static int addressLessThan(Address a, Address b) {
        if (a.lessThan(b)) {
            return 10;
        }
        return 20;
    }

    private static boolean addressLessThanZero(Address a) {
        return a.lessThan(Address.zero());
    }

    private static boolean addressMaxLessThan(Address b) {
        return Address.max().lessThan(b);
    }

    public void test_addressLessThan() {
        Method_Type method = compileMethod(CompilerTest_comparisons.class, "addressLessThan");
        assertFalse(method.contains(AddressBuiltin.LessThan.BUILTIN, false));

        Value result = execute(method, new WordValue(Address.fromInt(3)), new WordValue(Address.fromInt(4)));
        assertTrue(result.asInt() == 10);

        result = execute(method, new WordValue(Address.fromInt(7)), new WordValue(Address.fromInt(5)));
        assertTrue(result.asInt() == 20);

        result = execute(method, new WordValue(Address.fromLong(-1L)), new WordValue(Address.fromInt(5)));
        assertTrue(result.asInt() == 20);

        method = compileMethod(CompilerTest_comparisons.class, "addressLessThanZero");
        assertFalse(method.contains(AddressBuiltin.LessThan.BUILTIN, false));
        result = execute(method, new WordValue(Address.fromInt(5)));
        assertFalse(result.asBoolean());

        method = compileMethod(CompilerTest_comparisons.class, "addressMaxLessThan");
        assertFalse(method.contains(AddressBuiltin.LessThan.BUILTIN, false));
        result = execute(method, new WordValue(Address.fromInt(5)));
        assertFalse(result.asBoolean());
    }

    private static int addressLessEqual(Address a, Address b) {
        if (a.lessEqual(b)) {
            return 10;
        }
        return 20;
    }

    private static boolean addressLessEqualMax(Address a) {
        return a.lessEqual(Address.max());
    }

    private static boolean addressZeroLessEqual(Address b) {
        return Address.zero().lessEqual(b);
    }

    public void test_addressLessEqual() {
        Method_Type method = compileMethod(CompilerTest_comparisons.class, "addressLessEqual");
        assertFalse(method.contains(AddressBuiltin.LessEqual.BUILTIN, false));

        Value result = execute(method, new WordValue(Address.fromInt(3)), new WordValue(Address.fromInt(4)));
        assertTrue(result.asInt() == 10);

        result = execute(method, new WordValue(Address.fromInt(7)), new WordValue(Address.fromInt(5)));
        assertTrue(result.asInt() == 20);

        result = execute(method, new WordValue(Address.fromLong(-1L)), new WordValue(Address.fromInt(5)));
        assertTrue(result.asInt() == 20);

        method = compileMethod(CompilerTest_comparisons.class, "addressLessEqualMax");
        assertFalse(method.contains(AddressBuiltin.LessEqual.BUILTIN, false));
        result = execute(method, new WordValue(Address.fromInt(5)));
        assertTrue(result.asBoolean());

        method = compileMethod(CompilerTest_comparisons.class, "addressZeroLessEqual");
        assertFalse(method.contains(AddressBuiltin.LessEqual.BUILTIN, false));
        result = execute(method, new WordValue(Address.fromInt(5)));
        assertTrue(result.asBoolean());
    }

    private static int perform_offsetGreaterThan(Offset a, Offset b) {
        if (a.greaterThan(b)) {
            return 10;
        }
        return 20;
    }

    public void test_offsetComparison() {
        final Method_Type method = compileMethod(CompilerTest_comparisons.class, "perform_offsetGreaterThan");
        assertFalse(method.contains(JavaBuiltin.LongCompare.BUILTIN, false));

        Value result = execute(method, new WordValue(Offset.fromInt(3)), new WordValue(Offset.fromInt(4)));
        assertTrue(result.asInt() == 20);

        result = execute(method, new WordValue(Offset.fromInt(7)), new WordValue(Offset.fromInt(5)));
        assertTrue(result.asInt() == 10);

        result = execute(method, new WordValue(Offset.fromLong(-1L)), new WordValue(Offset.fromInt(5)));
        assertTrue(result.asInt() == 20);
    }

    private static int perform_longComparison(long a, long b) {
        if (a > b) {
            return 10;
        }
        return 20;
    }

    public void test_longComparison() {
        final Method_Type method = compileMethod(CompilerTest_comparisons.class, "perform_longComparison");
        assertFalse(method.contains(JavaBuiltin.LongCompare.BUILTIN, false));

        Value result = execute(method, LongValue.from(3L), LongValue.from(4L));
        assertTrue(result.asInt() == 20);

        result = execute(method, LongValue.from(7L), LongValue.from(5L));
        assertTrue(result.asInt() == 10);
    }

}
