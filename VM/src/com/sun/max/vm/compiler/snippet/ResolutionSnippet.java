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
package com.sun.max.vm.compiler.snippet;

import java.lang.reflect.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.ResolutionGuard.*;

/**
 * Snippets that perform constant pool resolution. Each resolution snippet type has a corresponding
 * {@link ResolutionGuard resolution guard type}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class ResolutionSnippet extends Snippet {

    @HOSTED_ONLY
    protected ResolutionSnippet() {
        ProgramError.check((serial() & 0xffff) == serial());
        final Class[] expectedSignature = {ResolutionGuard.class};
        for (Method method : getClass().getDeclaredMethods()) {
            if (method.getAnnotation(SNIPPET.class) != null) {
                Class<?>[] signature = method.getParameterTypes();
                ProgramError.check(expectedSignature.length == signature.length);
                for (int i = 0; i < expectedSignature.length; i++) {
                    Class<?> expected = expectedSignature[i];
                    ProgramError.check(expected.isAssignableFrom(signature[i]));
                }
            }
        }
    }

    /**
     * Creates a new guard for the resolution and/or initialization performed by this snippet.
     */
    public final ResolutionGuard.InPool createGuard(ConstantPool constantPool, int constantPoolIndex) {
        return new ResolutionGuard.InPool(constantPool, constantPoolIndex);
    }

    /**
     * Resolves a constant pool entry denoting a class that will be resolved to any arbitrary class type. That is, the
     * entry can denote an array type, interface or normal class.
     *
     * This snippet is used when translating the {@link Bytecodes#INSTANCEOF}, {@link Bytecodes#CHECKCAST},
     * {@link Bytecodes#LDC} and {@link Bytecodes#MULTIANEWARRAY} instructions.
     */
    public static final class ResolveClass extends ResolutionSnippet {

        @NEVER_INLINE
        public static void resolve(ResolutionGuard guard) {
            final ClassActor classActor;
            if (guard instanceof InPool) {
                InPool guardInPool = (InPool) guard;
                final ConstantPool constantPool = guardInPool.pool;
                final int index = guardInPool.cpi;
                classActor = constantPool.classAt(index).resolve(constantPool, index);
            } else {
                InAccessingClass guardInClass = (InAccessingClass) guard;
                classActor = guardInClass.resolve();
            }
            guard.value = classActor;
        }

        @SNIPPET
        @INLINE(afterSnippetsAreCompiled = true)
        public static ClassActor resolveClass(ResolutionGuard guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asClassActor(guard.value);
        }

        public static final ResolveClass SNIPPET = new ResolveClass();
    }

    /**
     * Resolves a constant pool entry denoting a class that specifies the component type of an object.
     * The resolved value returned by this snippet is the array type derived from the component type.
     *
     * This snippet is used when translating the {@link Bytecodes#ANEWARRAY} instruction.
     */
    public static final class ResolveArrayClass extends ResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ResolutionGuard guard) {
            final ClassActor arrayClassActor;
            if (guard instanceof InPool) {
                InPool guardInPool = (InPool) guard;
                final ConstantPool constantPool = guardInPool.pool;
                final int index = guardInPool.cpi;
                arrayClassActor = ArrayClassActor.forComponentClassActor(constantPool.classAt(index).resolve(constantPool, index));
            } else {
                arrayClassActor = ((InAccessingClass) guard).resolve();
            }
            guard.value = arrayClassActor;
        }

        @SNIPPET
        @INLINE(afterSnippetsAreCompiled = true)
        public static ArrayClassActor resolveArrayClass(ResolutionGuard guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asArrayClassActor(guard.value);
        }

        public static final ResolveArrayClass SNIPPET = new ResolveArrayClass();
    }

    /**
     * Resolves a constant pool entry denoting a class that specifies a class type that can be instantiated.
     *
     * This snippet is used when translating the {@link Bytecodes#NEW} instruction.
     */
    public static final class ResolveClassForNew extends ResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ResolutionGuard guard) {
            final ClassActor classActor;
            if (guard instanceof InPool) {
                InPool guardInPool = (InPool) guard;
                final ConstantPool constantPool = guardInPool.pool;
                final int index = guardInPool.cpi;
                classActor = constantPool.classAt(index).resolve(constantPool, index);
            } else {
                classActor = ((InAccessingClass) guard).resolve();
            }
            if (classActor.isAbstract() || classActor.isArrayClass()) {
                // Covers abstract classes and interfaces
                throw new InstantiationError();
            }
            guard.value = classActor;
        }

        @SNIPPET
        @INLINE
        public static ClassActor resolveClassForNew(ResolutionGuard guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asClassActor(guard.value);
        }

        public static final ResolveClassForNew SNIPPET = new ResolveClassForNew();
    }

    /**
     * Resolves a constant pool entry denoting a static field that is being read.
     *
     * This snippet is used when translating the {@link Bytecodes#GETSTATIC} instruction.
     */
    public static final class ResolveStaticFieldForReading extends ResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ResolutionGuard.InPool guard) {
            final ConstantPool constantPool = guard.pool;
            final int index = guard.cpi;
            final FieldActor fieldActor = constantPool.fieldAt(index).resolve(constantPool, index);
            if (!fieldActor.isStatic()) {
                throw new IncompatibleClassChangeError();
            }
            guard.value = fieldActor;
        }

        @SNIPPET
        @INLINE
        public static FieldActor resolveStaticFieldForReading(ResolutionGuard.InPool guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asFieldActor(guard.value);
        }

        public static final ResolveStaticFieldForReading SNIPPET = new ResolveStaticFieldForReading();
    }

    /**
     * Resolves a constant pool entry denoting a static field that is being written to.
     *
     * This snippet is used when translating the {@link Bytecodes#PUTSTATIC} instruction.
     */
    public static final class ResolveStaticFieldForWriting extends ResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ResolutionGuard.InPool guard) {
            final ConstantPool constantPool = guard.pool;
            final int index = guard.cpi;
            final FieldActor fieldActor = constantPool.fieldAt(index).resolve(constantPool, index);
            if (!fieldActor.isStatic()) {
                throw new IncompatibleClassChangeError();
            }
            if (fieldActor.isFinal() && fieldActor.holder() != constantPool.holder()) {
                throw new IllegalAccessError();
            }
            guard.value = fieldActor;
        }

        @SNIPPET
        @INLINE
        public static FieldActor resolveStaticFieldForWriting(ResolutionGuard.InPool guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asFieldActor(guard.value);
        }

        public static final ResolveStaticFieldForWriting SNIPPET = new ResolveStaticFieldForWriting();
    }

    /**
     * Resolves a constant pool entry denoting an instance field that is being read.
     *
     * This snippet is used when translating the {@link Bytecodes#GETFIELD} instruction.
     */
    public static final class ResolveInstanceFieldForReading extends ResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ResolutionGuard.InPool guard) {
            final ConstantPool constantPool = guard.pool;
            final int index = guard.cpi;
            final FieldActor fieldActor = constantPool.fieldAt(index).resolve(constantPool, index);
            if (fieldActor.isStatic()) {
                throw new IncompatibleClassChangeError();
            }
            guard.value = fieldActor;
        }

        /**
         * This is annotated with both SNIPPET and INLINE so that if folding fails during compilation (because
         * the field isn't resolved at compiled time), the guarding code gets inlined in the user code.
         */
        @SNIPPET
        @INLINE
        public static FieldActor resolveInstanceFieldForReading(ResolutionGuard.InPool guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asFieldActor(guard.value);
        }

        public static final ResolveInstanceFieldForReading SNIPPET = new ResolveInstanceFieldForReading();
    }

    /**
     * Resolves a constant pool entry denoting an instance field that is being written to.
     *
     * This snippet is used when translating the {@link Bytecodes#PUTFIELD} instruction.
     */
    public static final class ResolveInstanceFieldForWriting extends ResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ResolutionGuard.InPool guard) {
            final ConstantPool constantPool = guard.pool;
            final int index = guard.cpi;
            final FieldActor fieldActor = constantPool.fieldAt(index).resolve(constantPool, index);
            if (fieldActor.isStatic()) {
                throw new IncompatibleClassChangeError();
            }
            if (fieldActor.isFinal() && fieldActor.holder() != constantPool.holder()) {
                throw new IllegalAccessError();
            }
            guard.value = fieldActor;

        }

        @SNIPPET
        @INLINE
        public static FieldActor resolveInstanceFieldForWriting(ResolutionGuard.InPool guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asFieldActor(guard.value);
        }

        public static final ResolveInstanceFieldForWriting SNIPPET = new ResolveInstanceFieldForWriting();
    }

    /**
     * Resolves a constant pool entry denoting a static method that is being invoked.
     *
     * This snippet is used when translating the {@link Bytecodes#INVOKESTATIC} instruction.
     */
    public static final class ResolveStaticMethod extends ResolutionSnippet {
        @NEVER_INLINE
        private static void resolve(ResolutionGuard.InPool guard) {
            final ConstantPool constantPool = guard.pool;
            final int index = guard.cpi;
            guard.value = constantPool.resolveInvokeStatic(index);
        }

        @SNIPPET
        @INLINE(afterSnippetsAreCompiled = true)
        public static StaticMethodActor resolveStaticMethod(ResolutionGuard.InPool guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asStaticMethodActor(guard.value);
        }

        public static final ResolveStaticMethod SNIPPET = new ResolveStaticMethod();
    }

    public static boolean isSpecial(MethodActor declaredMethod, ClassActor currentClassActor) {
        final ClassActor holder = declaredMethod.holder();
        return (currentClassActor.flags() & Actor.ACC_SUPER) != 0 && currentClassActor != holder && currentClassActor.hasSuperClass(holder) && !declaredMethod.isInstanceInitializer();
    }

    public static final class ResolveSpecialMethod extends ResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ResolutionGuard.InPool guard) {
            final ConstantPool constantPool = guard.pool;
            final int index = guard.cpi;
            guard.value = constantPool.resolveInvokeSpecial(index);
        }

        @SNIPPET
        @INLINE
        public static VirtualMethodActor resolveSpecialMethod(ResolutionGuard.InPool guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asVirtualMethodActor(guard.value);
        }

        public static final ResolveSpecialMethod SNIPPET = new ResolveSpecialMethod();
    }

    public static final class ResolveVirtualMethod extends ResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ResolutionGuard.InPool guard) {
            final ConstantPool constantPool = guard.pool;
            final int index = guard.cpi;

            final MethodActor methodActor = constantPool.resolveInvokeVirtual(index);
            if (methodActor.isInitializer()) {
                throw new VerifyError("<init> must be invoked with invokespecial");
            }
            guard.value = methodActor;
        }

        @SNIPPET
        @INLINE(afterSnippetsAreCompiled = true)
        public static VirtualMethodActor resolveVirtualMethod(ResolutionGuard.InPool guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asVirtualMethodActor(guard.value);
        }

        public static final ResolveVirtualMethod SNIPPET = new ResolveVirtualMethod();
    }

    /**
     * Resolves a constant pool entry denoting a method that is being invoked "interfacially".
     *
     * This snippet is used when translating the {@link Bytecodes#INVOKEINTERFACE} instruction.
     *
     * Special note: While it is legal to invoke a virtual method declared in java.lang.Object with invokeinterface, an
     * assumption is made by this snippet that the verifier detects such cases and rewrites the instruction to use
     * invokevirtual instead.
     */
    public static final class ResolveInterfaceMethod extends ResolutionSnippet {

        @NEVER_INLINE
        public static void resolve(ResolutionGuard.InPool guard) {
            final ConstantPool constantPool = guard.pool;
            final int index = guard.cpi;
            guard.value = constantPool.resolveInvokeInterface(index);
        }

        @SNIPPET
        @INLINE
        public static InterfaceMethodActor resolveInterfaceMethod(ResolutionGuard.InPool guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asInterfaceMethodActor(guard.value);
        }

        public static final ResolveInterfaceMethod SNIPPET = new ResolveInterfaceMethod();
    }
}
