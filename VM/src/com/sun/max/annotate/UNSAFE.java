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
package com.sun.max.annotate;

import java.lang.annotation.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;

/**
 * Methods with this annotation must be compiled with the optimizing compiler, not the JIT.
 * Neither can they be interpreted.
 * <p>
 * This is the case when a method uses values of type {@link Word} or
 * any constant folding or dead code elimination must take place
 * before the code makes sense in the target VM.
 * <p>
 * Most of these methods are recognized automatically.
 * Only those not captured by the {@linkplain Static#determineMethods() below algorithm}
 * need to be annotated.
 * <p>
 * Some other annotations imply UNSAFE:
 * <ul>
 * <li>{@link BUILTIN}</li>
 * <li>{@link C_FUNCTION}</li>
 * <li>{@link JNI_FUNCTION}</li>
 * <li>{@link SUBSTITUTE}: the substitutee is unsafe</li>
 * <li>{@link SURROGATE}: the substitutee is unsafe</li>
 * </ul>
 * <p>
 * However, some must be pointed out manually with this annotation.
 * In addition to the above, any method that calls a method with this annotation is regarded as unsafe, too.
 * But this property is not transitive.
 * It only applies when a method manually declared UNSAFE is found.
 *
 * @see ClassfileReader
 *
 * @author Bernd Mathiske
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface UNSAFE {

    @PROTOTYPE_ONLY
    public final class Static {

        private Static() {
        }

        private static boolean isUnsafeType(TypeDescriptor type, ClassActor classActor, ClassLoader classLoader) {
            if (KindTypeDescriptor.isWord(type)) {
                return true;
            }
            if (type.isResolvableWithoutClassLoading(classActor, classLoader)) {
                return Accessor.class.isAssignableFrom(type.resolve(classLoader).toJava());
            }
            return false;
        }

        private static boolean isUnsafeSignature(SignatureDescriptor descriptor, ClassActor classActor, ClassLoader classLoader) {
            if (isUnsafeType(descriptor.resultDescriptor(), classActor, classLoader)) {
                return true;
            }
            for (int i = 0; i < descriptor.numberOfParameters(); i++) {
                if (isUnsafeType(descriptor.parameterDescriptorAt(i), classActor, classLoader)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean hasUnsafeSignature(MethodActor methodActor, ClassActor classActor, ClassLoader classLoader) {
            if (methodActor instanceof VirtualMethodActor && isUnsafeType(methodActor.holder().typeDescriptor(), classActor, classLoader)) {
                return true;
            }
            return isUnsafeSignature(methodActor.descriptor(), classActor, classLoader);
        }

        /**
         * Find out whether the given method must be compiled by the optimizing compiler only, without resolving any constant pool constants.
         */
        private static boolean isUnsafe(ClassMethodActor classMethodActor) {
            if (classMethodActor.isUnsafe()) {
                return true;
            }
            final CodeAttribute codeAttribute = classMethodActor.compilee().codeAttribute();
            if (codeAttribute == null) {
                return false;
            }
            if (classMethodActor.isCFunction() || classMethodActor.isJniFunction()) {
                return true;
            }
            if (classMethodActor.isClassInitializer()) { // TODO: check whether the latter is always correct
                return false;
            }
            final ClassActor classActor = classMethodActor.holder();
            final ClassLoader classLoader = classActor.classLoader();
            if (hasUnsafeSignature(classMethodActor, classActor, classLoader)) {
                return true;
            }

            final ConstantPool pool = codeAttribute.constantPool();
            final MutableInnerClassGlobal<Boolean> isUnsafe = new MutableInnerClassGlobal<Boolean>(false);

            final BytecodeVisitor bytecodeVisitor = new BytecodeAdapter() {
                private void checkField(int index) {
                    final FieldRefConstant fieldRefConstant = (FieldRefConstant) pool.at(index);
                    if (isUnsafeType(fieldRefConstant.type(pool), classActor, classLoader)) {
                        isUnsafe.setValue(true);
                    }
                }

                @Override
                protected void getfield(int index) {
                    checkField(index);
                }

                @Override
                protected void putfield(int index) {
                    checkField(index);
                }

                @Override
                protected void getstatic(int index) {
                    checkField(index);
                }

                @Override
                protected void putstatic(int index) {
                    checkField(index);
                }

                private void checkMethod(int index, boolean hasReceiver) {
                    final MethodRefConstant methodRefConstant = (MethodRefConstant) pool.at(index);
                    if (isUnsafeSignature(methodRefConstant.signature(pool), classActor, classLoader)) {
                        isUnsafe.setValue(true);
                    }
                    if (hasReceiver && isUnsafeType(methodRefConstant.holder(pool), classActor, classLoader)) {
                        isUnsafe.setValue(true);
                    }
                    if (methodRefConstant.holder(pool).toJavaString().startsWith(_vmPackageName) && methodRefConstant.isResolvableWithoutClassLoading(pool)) {
                        try {
                            final MethodActor methodActor = methodRefConstant.resolve(pool, index);
                            if (methodActor.isUnsafe()) {
                                isUnsafe.setValue(true);
                            }
                        } catch (NoSuchMethodError noSuchMethodError) {
                            // much likely a reference to a @PROTOTYPE_ONLY method - do nothing
                        }
                    }
                }

                @Override
                protected void invokestatic(int index) {
                    checkMethod(index, false);
                }

                @Override
                protected void invokespecial(int index) {
                    checkMethod(index, true);
                }

                @Override
                protected void invokevirtual(int index) {
                    checkMethod(index, true);
                }

                @Override
                protected void invokeinterface(int index, int count) {
                    checkMethod(index, true);
                }

            };
            final BytecodeScanner bytecodeScanner = new BytecodeScanner(bytecodeVisitor);
            bytecodeScanner.scan(classMethodActor);
            return isUnsafe.value();
        }

        private static final AppendableSequence<ClassMethodActor> _list = new ArrayListSequence<ClassMethodActor>();

        public static Sequence<ClassMethodActor> methods() {
            return _list;
        }

        private static void determine(ClassMethodActor classMethodActor) {
            if (isUnsafe(classMethodActor)) {
                _list.append(classMethodActor);
            }
        }

        private static final String _vmPackageName = new com.sun.max.vm.Package().name();
        private static final String _asmPackageName = new com.sun.max.vm.asm.Package().name();

        /**
         * Find all unsafe methods and mark them with the UNSAFE flag.
         * As a side-effect add them to the list for use of the {@link CompiledPrototype}.
         */
        public static void determineMethods() {
            Trace.begin(1, "determining unsafe methods");
            for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
                if (classActor.packageName().startsWith(_vmPackageName) && !classActor.packageName().startsWith(_asmPackageName)) {
                    for (ClassMethodActor classMethodActor : classActor.localStaticMethodActors()) {
                        determine(classMethodActor);
                    }
                    for (ClassMethodActor classMethodActor : classActor.localVirtualMethodActors()) {
                        determine(classMethodActor);
                    }
                }
            }
            // Only now set the flag, because it is NOT transitive that a method that calls another unsafe one is also unsafe:
            for (ClassMethodActor classMethodActor : _list) {
                classMethodActor.beUnsafe();
            }
            Trace.end(1, "determining unsafe methods");
        }
    }
}
