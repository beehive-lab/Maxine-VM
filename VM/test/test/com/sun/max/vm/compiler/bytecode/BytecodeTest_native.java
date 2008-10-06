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
/*VCSID=e5918051-7e73-48da-acbb-2568d180527d*/
package test.com.sun.max.vm.compiler.bytecode;

import java.lang.reflect.*;
import java.util.Arrays;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_native<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_native(String name) {
        super(name);
    }

    private static native void nop();

    public void test_nop() {
        final Method_Type method = compileMethod("nop", SignatureDescriptor.create(Void.TYPE));
        if (hasInterpreter()) {
            final Value result = execute(method);
            assertTrue(result.toString().equals("void"));
        }
    }

    public void test_JniFunctions() {
        for (Method method : JniFunctions.class.getDeclaredMethods()) {
            final MethodActor methodActor = MethodActor.fromJava(method);
            if (methodActor.isJniFunction()) {
                compileMethod((ClassMethodActor) methodActor);
            }
        }
    }

    @C_FUNCTION
    private static native void nop_cfunction();

    public void test_nop_cfunction() {
        final Method_Type method = compileMethod("nop_cfunction", SignatureDescriptor.create(Void.TYPE));
        if (hasInterpreter()) {
            final Value result = execute(method);
            assertTrue(result.toString().equals("void"));
        }
    }

    // "identity" functions to test simple parameter passing and returning
    private static native boolean booleanIdentity(boolean value);
    private static native byte byteIdentity(byte value);
    private static native char charIdentity(char value);
    private static native short shortIdentity(short value);
    private static native int intIdentity(int value);
    private static native long longIdentity(long value);
    private static native float floatIdentity(float value);
    private static native double doubleIdentity(double value);
    private static native Object referenceIdentity(Object value);

    public static class TestValuesMapping implements KindMapping<Value[]> {

        public Value[] mapBoolean() {
            return BooleanValue.arrayFrom(true, false);
        }

        public Value[] mapByte() {
            return ByteValue.arrayFrom(Byte.MIN_VALUE, (byte) (Byte.MIN_VALUE + 1), (byte) -1, (byte) 0, (byte) 1, (byte) (Byte.MAX_VALUE - 1), Byte.MAX_VALUE);
        }

        public Value[] mapChar() {
            return CharValue.arrayFrom((char) 0, (char) 1, (char)  (Character.MAX_VALUE - 1), Character.MAX_VALUE);
        }

        public Value[] mapDouble() {
            return DoubleValue.arrayFrom(Double.MIN_VALUE, Double.MIN_VALUE + 1, -1D, 0D, 1D, Double.MAX_VALUE - 1, Double.MAX_VALUE);
        }

        public Value[] mapFloat() {
            return FloatValue.arrayFrom(Float.MIN_VALUE, Float.MIN_VALUE + 1, -1F, 0F, 1F, Float.MAX_VALUE - 1, Float.MAX_VALUE);
        }

        public Value[] mapInt() {
            return IntValue.arrayFrom(Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1, 0, 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        }

        public Value[] mapLong() {
            return LongValue.arrayFrom(Long.MIN_VALUE, Long.MIN_VALUE + 1, -1, 0, 1, Long.MAX_VALUE - 1, Long.MAX_VALUE);
        }

        public Value[] mapReference() {
            return new ReferenceValue[]{ReferenceValue.NULL, ReferenceValue.from(this)};
        }

        public Value[] mapShort() {
            return ShortValue.arrayFrom(Short.MIN_VALUE, (short) (Short.MIN_VALUE + 1), (short) -1, (short) 0, (short) 1, (short) (Short.MAX_VALUE - 1), Short.MAX_VALUE);
        }

        public Value[] mapVoid() {
            return new Value[]{};
        }

        public Value[] mapWord() {
            return new Value[]{};
        }

    }

    public void test_reference_identity() {
        final Method_Type method = compileMethod("referenceIdentity", SignatureDescriptor.create(Object.class, Object.class));
        if (hasInterpreter()) {
            final Object object = "XXX";
            final Value value = ReferenceValue.from(object);
            final Value returnedValue = execute(method, value);
            assertTrue(returnedValue.equals(value));
        }
    }

    public void test_primitive_identity() {
        final TestValuesMapping mapping = new TestValuesMapping();
        for (Kind<?> kind : Kind.VALUES) {
            @JavacSyntax("Incomparable types bug")
            final Kind rawKind = kind;
            if (rawKind == Kind.WORD || rawKind == Kind.REFERENCE) {
                continue;
            }
            final String kindName = kind.name().toString();
            final String name = kindName.toLowerCase();
            final Method_Type method = compileMethod(name + "Identity", SignatureDescriptor.create(kind.toJava(), kind.toJava()));
            if (hasInterpreter()) {
                for (Value value : kind.acceptMapping(mapping)) {
                    final Value returnedValue = execute(method, value);
                    assertTrue(returnedValue.equals(value));
                }
            }
        }
    }

    private static void parametersAsString(StringBuilder sb, Object rdi, int rsi, long rdx, short rcx, char r8, Object r9, int sp24, long sp16, short sp8, char sp0) {
        sb.append("" + rdi + rsi + rdx + rcx + r8 + r9 + sp24 + sp16 + sp8 + sp0);
    }

    @STUB_TEST_PROPERTIES(execute = false)
    public static native void manyParameters(Method reflectedMethod, StringBuilder sb, Object rdi, int rsi, long rdx, short rcx, char r8, Object r9, int sp24, long sp16, short sp8, char sp0);

    public void test_manyParameters() {
        final Method reflectedMethod = Classes.getDeclaredMethod(BytecodeTest_native.class, "parametersAsString", StringBuilder.class,
                        Object.class, int.class, long.class, short.class, char.class,
                        Object.class, int.class, long.class, short.class, char.class);
        final Method_Type method = compileMethod("manyParameters", SignatureDescriptor.create(Void.TYPE, Method.class, StringBuilder.class,
                        Object.class, int.class, long.class, short.class, char.class,
                        Object.class, int.class, long.class, short.class, char.class));
        if (hasInterpreter()) {
            final StringBuilder sb = new StringBuilder();
            final Value value = ReferenceValue.from("Hello Native");
            final Value[] parameterValues = {ReferenceValue.from(reflectedMethod), ReferenceValue.from(sb),
                value,               IntValue.from(1), LongValue.from(2), ShortValue.from((short) 3), CharValue.from((char) 4),
                ReferenceValue.NULL, IntValue.from(5), LongValue.from(6), ShortValue.from((short) 7), CharValue.from((char) 8)};

            execute(method, parameterValues);
            final String returnString = sb.toString();

            try {
                sb.delete(0, sb.length());
                MethodActor.fromJava(reflectedMethod).invoke(com.sun.max.lang.Arrays.subArray(parameterValues, 1));
                final String expectedReturnString = sb.toString();
                assertTrue(returnString.equals(expectedReturnString));
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                fail();
            } catch (IllegalAccessException e) {
                fail();
            }
        }
    }

    public static native Object[] manyObjectParameters(
                    Object[] array,
                    Object object0,
                    Object object1,
                    Object object2,
                    Object object3,
                    Object object4,
                    Object object5,
                    Object object6,
                    Object object7,
                    Object object8,
                    Object object9,
                    Object object10,
                    Object object11,
                    Object object12,
                    Object object13,
                    Object object14,
                    Object object15,
                    Object object16,
                    Object object17,
                    Object object18,
                    Object object19,
                    Object object20,
                    Object object21,
                    Object object22,
                    Object object23,
                    Object object24,
                    Object object25,
                    Object object26,
                    Object object27,
                    Object object28,
                    Object object29,
                    Object object30,
                    Object object31,
                    Object object32,
                    Object object33,
                    Object object34,
                    Object object35,
                    Object object36,
                    Object object37,
                    Object object38,
                    Object object39,
                    Object object40,
                    Object object41,
                    Object object42,
                    Object object43,
                    Object object44,
                    Object object45,
                    Object object46,
                    Object object47,
                    Object object48,
                    Object object49,
                    Object object50,
                    Object object51,
                    Object object52,
                    Object object53,
                    Object object54,
                    Object object55);

    public void notest_manyObjectParameters() {
        // This must be the number of parameters in the 'manyObjectParameters' native method
        final int numberOfParameters = 56;

        final Class[] parameterTypes = new Class[numberOfParameters + 1];
        parameterTypes[0] = Object[].class;
        for (int i = 0; i != numberOfParameters; ++i) {
            parameterTypes[i + 1] = Object.class;
        }
        final Value[] parameterValues = new Value[numberOfParameters + 1];
        final Object[] array = new Object[numberOfParameters];
        parameterValues[0] = ReferenceValue.from(array);
        for (int i = 0; i != numberOfParameters; ++i) {
            parameterValues[i + 1] = ReferenceValue.from("object" + i);
        }

        final Method_Type method = compileMethod("manyObjectParameters", SignatureDescriptor.create(Object[].class, parameterTypes));
        if (hasInterpreter()) {
            final Value returnValue = execute(method, parameterValues);
            final Object[] returnArray = (Object[]) returnValue.asObject();
            assertTrue(Arrays.equals(returnArray, array));
        }
    }
}
