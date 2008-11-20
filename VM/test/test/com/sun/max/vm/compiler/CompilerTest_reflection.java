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
package test.com.sun.max.vm.compiler;

import java.lang.reflect.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Tests for reflection.
 *
 * @author Doug Simon
 */
public abstract class CompilerTest_reflection<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    public CompilerTest_reflection(String name) {
        super(name);
    }

    public static class Instance {
        Word _word;
        int _int;
        String _string;
        byte _byte;
        Object[] _array;

        public Instance() {
            this(Address.fromInt(1), 1, "1", (byte) 1, new Object[1]);
        }

        public Instance(int i, String s, byte b, Object[] array) {
            this(Address.fromInt(2), i, s, b, array);
        }

        public Instance(Word w, int i, String s, byte b, Object[] array) {
            _word = w;
            _int = i;
            _string = s;
            _byte = b;
            _array = array;
        }

        @Override
        public String toString() {
            return "_word=" + _word + ", _int=" + _int + ", _string=" + _string + ", _byte=" + _byte + ", _array=[" + Arrays.toString(_array, ",") + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Instance) {
                final Instance instance = (Instance) o;
                return instance._byte == _byte && instance._word.equals(_word) && instance._string.equals(_string) && Arrays.equals(instance._array, _array);
            }
            return false;
        }
    }

    public void test_Instance() throws SecurityException, NoSuchMethodException {
        final Constructor javaConstructor = Instance.class.getDeclaredConstructor();
        final ClassMethodActor classMethodActor = getClassMethodActor(Instance.class, SymbolTable.INIT.toString(), SignatureDescriptor.fromJava(javaConstructor));

        final Instance instance = new Instance();

        final Method_Type compiledMethod = compileMethod(classMethodActor);
        if (!(compiledMethod instanceof TargetMethod)) {
            final Value result2 = execute(compiledMethod);
            assertEquals(result2.asObject(), instance);
        }
    }

    public void test_Instance2() throws SecurityException, NoSuchMethodException {
        final Constructor javaConstructor = Instance.class.getDeclaredConstructor(int.class, String.class, byte.class, Object[].class);
        final ClassMethodActor classMethodActor = getClassMethodActor(Instance.class, SymbolTable.INIT.toString(), SignatureDescriptor.fromJava(javaConstructor));

        final Instance instance = new Instance(2, "2", (byte) 2, new Object[]{2, 2});
        final Value[] arguments = new Value[]{IntValue.from(2), ReferenceValue.from("2"), ByteValue.from((byte) 2), ReferenceValue.from(new Object[]{2, 2})};

        final Method_Type compiledMethod = compileMethod(classMethodActor);
        if (!(compiledMethod instanceof TargetMethod)) {
            final Value result2 = execute(compiledMethod, arguments);
            assertEquals(result2.asObject(), instance);
        }
    }

    public void test_Instance3() throws SecurityException, NoSuchMethodException {
        final Constructor javaConstructor = Instance.class.getDeclaredConstructor(int.class, String.class, byte.class, Object[].class);
        final ClassMethodActor classMethodActor = getClassMethodActor(Instance.class, SymbolTable.INIT.toString(), SignatureDescriptor.fromJava(javaConstructor));

        final Instance instance = new Instance(2, "2", (byte) 2, new Object[]{2, 2});
        final Value[] arguments = new Value[]{IntValue.from(2), ReferenceValue.from("2"), ByteValue.from((byte) 2), ReferenceValue.from(new Object[]{2, 2})};

        final Method_Type compiledMethod = compileMethod(classMethodActor);
        if (!(compiledMethod instanceof TargetMethod)) {
            final Value result2 = execute(compiledMethod, arguments);
            assertEquals(result2.asObject(), instance);
        }
    }
}
