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
package com.sun.max.vm.reflection;

import static com.sun.max.lang.Classes.*;
import static com.sun.max.vm.reflection.InvocationStubGenerator.*;

import java.lang.reflect.*;

import com.sun.max.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.graft.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * An enumerated type to distinguish the style of boxing performed by a stub.
 *
 *  @author Doug Simon
 */
public enum Boxing {
    /**
     * Parameters and return values are boxed in {@link Object} instances.
     */
    JAVA {

        private final SignatureDescriptor invokeSignature = SignatureDescriptor.fromJava(getDeclaredMethod(Method.class, "invoke", Object.class, Object[].class));
        private final SignatureDescriptor newInstanceSignature = SignatureDescriptor.fromJava(getDeclaredMethod(Constructor.class, "newInstance", Object[].class));

        @Override
        public SignatureDescriptor invokeSignature() {
            return invokeSignature;
        }

        @Override
        public SignatureDescriptor newInstanceSignature() {
            return newInstanceSignature;
        }

        @Override
        Class[] runtimeParameterTypes(Class[] parameterTypes, Class declaringClass, boolean isStatic, boolean isConstructor) {
            return parameterTypes;
        }

        @Override
        void box(BytecodeAssembler asm, boolean isConstructor, Kind returnKind) {
            if (!isConstructor) {
                if (returnKind == Kind.VOID) {
                    asm.aconst_null();
                } else if (returnKind != Kind.REFERENCE) {
                    if (returnKind.isWord) {
                        throw new IllegalArgumentException("cannot reflectively invoke method with a Word return type");
                    }
                    asm.invokestatic(JAVA_BOX_PRIMITIVE.get(returnKind.asEnum), returnKind.stackSlots, 1);
                }
            }
        }

        @Override
        void unbox(BytecodeAssembler asm, Class parameterType, int parameterTypePoolConstantIndex) {
            final Kind parameterKind = Kind.fromJava(parameterType);
            if (parameterKind.isReference) {
                asm.checkcast(parameterTypePoolConstantIndex);
            } else {
                if (parameterKind.isWord) {
                    throw new IllegalArgumentException("cannot reflectively invoke method with Word type parameter");
                }
                asm.invokestatic(JAVA_UNBOX_PRIMITIVE.get(parameterKind.asEnum), 1, parameterKind.stackSlots);
            }
        }

        @Override
        String sourceDeclaration(boolean isConstructor) {
            if (isConstructor) {
                return "public Object newInstance(Object... args) throws InstantiationException, IllegalArgumentException, InvocationTargetException";
            }
            return "public Object invoke(Object obj, Object... args) throws IllegalArgumentException, InvocationTargetException";
        }

        @Override
        String sourceBox(ConstantPool pool, boolean isConstructor, Kind returnKind, String value) {
            if (!isConstructor) {
                if (returnKind == Kind.VOID) {
                    return "null";
                } else if (returnKind != Kind.REFERENCE) {
                    if (returnKind.isWord) {
                        throw new IllegalArgumentException("cannot reflectively invoke method with a Word return type");
                    }

                    final ClassMethodRefConstant boxMethod = pool.classMethodAt(JAVA_BOX_PRIMITIVE.get(returnKind.asEnum));
                    return boxMethod.holder(pool).toJavaString(false) + "." + boxMethod.name(pool) + "(" + value + ")";
                }
            }
            return value;
        }

        @Override
        String sourceUnbox(ConstantPool pool, Class parameterType, String parameter) {
            final Kind parameterKind = Kind.fromJava(parameterType);
            if (parameterKind.isReference) {
                return "(" + parameterType.getSimpleName() + ") " + parameter;
            }
            if (parameterKind.isWord) {
                throw new IllegalArgumentException("cannot reflectively invoke method with Word type parameter");
            }

            final ClassMethodRefConstant unboxMethod = pool.classMethodAt(JAVA_UNBOX_PRIMITIVE.get(parameterKind.asEnum));
            return unboxMethod.holder(pool).toJavaString(false) + "." + unboxMethod.name(pool) + "(" + parameter + ")";
        }
    },

    /**
     * Parameters and return values are boxed in {@link Value} instances.
     */
    VALUE {

        private final SignatureDescriptor invokeSignature = SignatureDescriptor.fromJava(getDeclaredMethod(MethodInvocationStub.class, "invoke", Value[].class));
        private final SignatureDescriptor newInstanceSignature = SignatureDescriptor.fromJava(getDeclaredMethod(ConstructorInvocationStub.class, "newInstance", Value[].class));

        @Override
        public SignatureDescriptor invokeSignature() {
            return invokeSignature;
        }

        @Override
        public SignatureDescriptor newInstanceSignature() {
            return newInstanceSignature;
        }

        @Override
        Class[] runtimeParameterTypes(Class[] parameterTypes, Class declaringClass, boolean isStatic, boolean isConstructor) {
            return (isStatic || isConstructor) ? parameterTypes : Utils.prepend(parameterTypes, declaringClass);
        }

        @Override
        void box(BytecodeAssembler asm, boolean isConstructor, Kind returnKind) {
            if (!isConstructor) {
                if (returnKind == Kind.VOID) {
                    asm.getstatic(VoidValue_VOID);
                } else {
                    asm.invokestatic(VALUE_BOX.get(returnKind.asEnum), returnKind.stackSlots, 1);
                }
            } else {
                asm.invokestatic(VALUE_BOX.get(Kind.REFERENCE.asEnum), 1, 1);
            }
        }

        @Override
        void unbox(BytecodeAssembler asm, Class parameterType, int parameterTypePoolConstantIndex) {
            final Kind parameterKind = Kind.fromJava(parameterType);
            asm.invokevirtual(VALUE_UNBOX.get(parameterKind.asEnum), 1, parameterKind.stackSlots);
            if (parameterKind.isReference) {
                // cast against Object is unnecessary AND causes problems with values that are StaticTuples
                if (parameterType != Object.class) {
                    asm.checkcast(parameterTypePoolConstantIndex);
                }
            } else if (parameterKind.isWord) {
                if (parameterType != Word.class) {
                    Integer methodRefIndex = CAST_WORD.get(parameterType);
                    asm.invokevirtual(methodRefIndex, 1, 1);
                }
            }
        }

        @Override
        String sourceDeclaration(boolean isConstructor) {
            if (isConstructor) {
                return "public Value newInstance(Value... args) throws InstantiationException, IllegalArgumentException, InvocationTargetException";
            }
            return "public Value invoke(Value... args) throws IllegalArgumentException, InvocationTargetException";
        }

        @Override
        String sourceBox(ConstantPool pool, boolean isConstructor, Kind returnKind, String value) {
            if (!isConstructor) {
                if (returnKind == Kind.VOID) {
                    return "VoidValue.VOID";
                }

                final ClassMethodRefConstant boxMethod = pool.classMethodAt(VALUE_BOX.get(returnKind.asEnum));
                return boxMethod.holder(pool).toJavaString(false) + "." + boxMethod.name(pool) + "(" + value + ")";
            }
            final ClassMethodRefConstant boxMethod = pool.classMethodAt(VALUE_BOX.get(Kind.REFERENCE.asEnum));
            return boxMethod.holder(pool).toJavaString(false) + "." + boxMethod.name(pool) + "(" + value + ")";
        }

        @Override
        String sourceUnbox(ConstantPool pool, Class parameterType, String parameter) {
            final Kind parameterKind = Kind.fromJava(parameterType);

            final ClassMethodRefConstant unboxMethod = pool.classMethodAt(VALUE_UNBOX.get(parameterKind.asEnum));
            final String unboxedParameter = parameter + "." + unboxMethod.name(pool) + "()";
            if (parameterKind.isReference) {
                return "(" + parameterType.getSimpleName() + ") " + unboxedParameter;
            } else if (parameterKind.isWord) {
                // The following instructions will be optimized away to nothing
                return "Boxing.castWord(" + parameterType.getSimpleName() + ", " + unboxedParameter + ")";
            }
            return unboxedParameter;
        }
    };

    public abstract SignatureDescriptor invokeSignature();
    public abstract SignatureDescriptor newInstanceSignature();

    abstract void box(BytecodeAssembler asm, boolean isConstructor, Kind returnKind);
    abstract Class[] runtimeParameterTypes(Class[] parameterTypes, Class declaringClass, boolean isStatic, boolean isConstructor);
    abstract void unbox(BytecodeAssembler asm, Class parameterType, int parameterTypePoolConstantIndex);

    abstract String sourceDeclaration(boolean isConstructor);
    abstract String sourceBox(ConstantPool pool, boolean isConstructor, Kind returnKind, String value);
    abstract String sourceUnbox(ConstantPool pool, Class parameterType, String parameter);
}
