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

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.ir.*;
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
        Word word;
        int i;
        String string;
        byte b;
        Object[] array;

        public Instance() {
            this(Address.fromInt(1), 1, "1", (byte) 1, new Object[1]);
        }

        public Instance(int i, String s, byte b, Object[] array) {
            this(Address.fromInt(2), i, s, b, array);
        }

        public Instance(Word w, int i, String s, byte b, Object[] array) {
            this.word = w;
            this.i = i;
            this.string = s;
            this.b = b;
            this.array = array;
        }

        @Override
        public String toString() {
            return "_word=" + word + ", _int=" + i + ", _string=" + string + ", _byte=" + b + ", _array=[" + Utils.toString(array, ",") + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Instance) {
                final Instance instance = (Instance) o;
                return instance.b == b && instance.word.equals(word) && instance.string.equals(string) && Arrays.equals(instance.array, array);
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
            assertEquals(instance, result2.asObject());
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
