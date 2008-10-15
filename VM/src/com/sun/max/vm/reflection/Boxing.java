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
package com.sun.max.vm.reflection;

import static com.sun.max.lang.Classes.*;
import static com.sun.max.vm.reflection.InvocationStubGenerator.*;

import java.lang.reflect.*;

import com.sun.max.lang.*;
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

        private final SignatureDescriptor _invokeSignature = SignatureDescriptor.fromJava(getDeclaredMethod(Method.class, "invoke", Object.class, Object[].class));
        private final SignatureDescriptor _newInstanceSignature = SignatureDescriptor.fromJava(getDeclaredMethod(Constructor.class, "newInstance", Object[].class));

        @Override
        public SignatureDescriptor invokeSignature() {
            return _invokeSignature;
        }

        @Override
        public SignatureDescriptor newInstanceSignature() {
            return _newInstanceSignature;
        }

        @Override
        Class[] runtimeParameterTypes(Class[] parameterTypes, Class declaringClass, boolean isStatic, boolean isConstructor) {
            return parameterTypes;
        }

        @Override
        void box(ByteArrayBytecodeAssembler asm, boolean isConstructor, Kind returnKind) {
            if (!isConstructor) {
                if (returnKind == Kind.VOID) {
                    asm.aconst_null();
                } else if (returnKind != Kind.REFERENCE) {
                    if (returnKind == Kind.WORD) {
                        throw new IllegalArgumentException("cannot reflectively invoke method with a Word return type");
                    }
                    asm.invokestatic(JAVA_BOX_PRIMITIVE.get(returnKind.asEnum()), returnKind.stackSlots(), 1);
                }
            }
        }

        @Override
        void unbox(ByteArrayBytecodeAssembler asm, Class parameterType, int parameterTypePoolConstantIndex) {
            final Kind parameterKind = Kind.fromJava(parameterType);
            if (parameterKind == Kind.REFERENCE) {
                asm.checkcast(parameterTypePoolConstantIndex);
            } else {
                if (parameterKind == Kind.WORD) {
                    throw new IllegalArgumentException("cannot reflectively invoke method with Word type parameter");
                }
                asm.invokestatic(JAVA_UNBOX_PRIMITIVE.get(parameterKind.asEnum()), 1, parameterKind.stackSlots());
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
                    if (returnKind == Kind.WORD) {
                        throw new IllegalArgumentException("cannot reflectively invoke method with a Word return type");
                    }

                    final ClassMethodRefConstant boxMethod = pool.classMethodAt(JAVA_BOX_PRIMITIVE.get(returnKind.asEnum()));
                    return boxMethod.holder(pool).toJavaString(false) + "." + boxMethod.name(pool) + "(" + value + ")";
                }
            }
            return value;
        }

        @Override
        String sourceUnbox(ConstantPool pool, Class parameterType, String parameter) {
            final Kind parameterKind = Kind.fromJava(parameterType);
            if (parameterKind == Kind.REFERENCE) {
                return "(" + parameterType.getSimpleName() + ") " + parameter;
            }
            if (parameterKind == Kind.WORD) {
                throw new IllegalArgumentException("cannot reflectively invoke method with Word type parameter");
            }

            final ClassMethodRefConstant unboxMethod = pool.classMethodAt(JAVA_UNBOX_PRIMITIVE.get(parameterKind.asEnum()));
            return unboxMethod.holder(pool).toJavaString(false) + "." + unboxMethod.name(pool) + "(" + parameter + ")";
        }
    },

    /**
     * Parameters and return values are boxed in {@link Value} instances.
     */
    VALUE {

        private final SignatureDescriptor _invokeSignature = SignatureDescriptor.fromJava(getDeclaredMethod(GeneratedMethodStub.class, "invoke", Value[].class));
        private final SignatureDescriptor _newInstanceSignature = SignatureDescriptor.fromJava(getDeclaredMethod(GeneratedConstructorStub.class, "newInstance", Value[].class));

        @Override
        public SignatureDescriptor invokeSignature() {
            return _invokeSignature;
        }

        @Override
        public SignatureDescriptor newInstanceSignature() {
            return _newInstanceSignature;
        }

        @Override
        Class[] runtimeParameterTypes(Class[] parameterTypes, Class declaringClass, boolean isStatic, boolean isConstructor) {
            return (isStatic || isConstructor) ? parameterTypes : Arrays.prepend(parameterTypes, declaringClass);
        }

        @Override
        void box(ByteArrayBytecodeAssembler asm, boolean isConstructor, Kind returnKind) {
            if (!isConstructor) {
                if (returnKind == Kind.VOID) {
                    asm.getstatic(VoidValue_VOID);
                } else {
                    asm.invokestatic(VALUE_BOX.get(returnKind.asEnum()), returnKind.stackSlots(), 1);
                }
            } else {
                asm.invokestatic(VALUE_BOX.get(Kind.REFERENCE.asEnum()), 1, 1);
            }
        }

        @Override
        void unbox(ByteArrayBytecodeAssembler asm, Class parameterType, int parameterTypePoolConstantIndex) {
            final Kind parameterKind = Kind.fromJava(parameterType);
            asm.invokevirtual(VALUE_UNBOX.get(parameterKind.asEnum()), 1, parameterKind.stackSlots());
            if (parameterKind == Kind.REFERENCE) {
                // cast against Object is unneccessary AND causes problems with values that are StaticTuples
                if (parameterType != Object.class) {
                    asm.checkcast(parameterTypePoolConstantIndex);
                }
            } else if (parameterKind == Kind.WORD) {
                /*
                 * The optimizing compiler is supposed to eliminate all this:
                 */
                asm.ldc(parameterTypePoolConstantIndex);
                asm.swap();
                asm.invokestatic(UnsafeLoophole_castWord_Class_Word, 2, 1);
                asm.checkcast(parameterTypePoolConstantIndex);
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

                final ClassMethodRefConstant boxMethod = pool.classMethodAt(VALUE_BOX.get(returnKind.asEnum()));
                return boxMethod.holder(pool).toJavaString(false) + "." + boxMethod.name(pool) + "(" + value + ")";
            }
            final ClassMethodRefConstant boxMethod = pool.classMethodAt(VALUE_BOX.get(Kind.REFERENCE.asEnum()));
            return boxMethod.holder(pool).toJavaString(false) + "." + boxMethod.name(pool) + "(" + value + ")";
        }

        @Override
        String sourceUnbox(ConstantPool pool, Class parameterType, String parameter) {
            final Kind parameterKind = Kind.fromJava(parameterType);

            final ClassMethodRefConstant unboxMethod = pool.classMethodAt(VALUE_UNBOX.get(parameterKind.asEnum()));
            final String unboxedParameter = parameter + "." + unboxMethod.name(pool) + "()";
            if (parameterKind == Kind.REFERENCE) {
                return "(" + parameterType.getSimpleName() + ") " + unboxedParameter;
            } else if (parameterKind == Kind.WORD) {
                // The following instructions will be optimized away to nothing
                return "UnsafeLoophole.castWord(" + parameterType.getSimpleName() + ", " + unboxedParameter + ")";
            }
            return unboxedParameter;
        }
    };

    public abstract SignatureDescriptor invokeSignature();
    public abstract SignatureDescriptor newInstanceSignature();

    abstract void box(ByteArrayBytecodeAssembler asm, boolean isConstructor, Kind returnKind);
    abstract Class[] runtimeParameterTypes(Class[] parameterTypes, Class declaringClass, boolean isStatic, boolean isConstructor);
    abstract void unbox(ByteArrayBytecodeAssembler asm, Class parameterType, int parameterTypePoolConstantIndex);

    abstract String sourceDeclaration(boolean isConstructor);
    abstract String sourceBox(ConstantPool pool, boolean isConstructor, Kind returnKind, String value);
    abstract String sourceUnbox(ConstantPool pool, Class parameterType, String parameter);
}
