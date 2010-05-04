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
package com.sun.max.vm.compiler.snippet;

import java.lang.reflect.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.runtime.*;

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
                ProgramError.check(Arrays.equals(expectedSignature, method.getParameterTypes()));
            }
        }
    }

    /**
     * Creates a new guard for the resolution and/or initialization performed by this snippet.
     */
    public final ResolutionGuard createGuard(ConstantPool constantPool, int constantPoolIndex) {
        return new ResolutionGuard(constantPool, constantPoolIndex);
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
        private static void resolve(ResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool;
            final int index = guard.constantPoolIndex;
            final ClassActor classActor = constantPool.classAt(index).resolve(constantPool, index);
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
            final ConstantPool constantPool = guard.constantPool;
            final int index = guard.constantPoolIndex;
            final ArrayClassActor arrayClassActor = ArrayClassActor.forComponentClassActor(constantPool.classAt(index).resolve(constantPool, index));
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
            final ConstantPool constantPool = guard.constantPool;
            final int index = guard.constantPoolIndex;
            final ClassActor classActor = constantPool.classAt(index).resolve(constantPool, index);
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
        private static void resolve(ResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool;
            final int index = guard.constantPoolIndex;
            final FieldActor fieldActor = constantPool.fieldAt(index).resolve(constantPool, index);
            if (!fieldActor.isStatic()) {
                throw new IncompatibleClassChangeError();
            }
            guard.value = fieldActor;
        }

        @SNIPPET
        @INLINE
        public static FieldActor resolveStaticFieldForReading(ResolutionGuard guard) {
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
        private static void resolve(ResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool;
            final int index = guard.constantPoolIndex;
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
        public static FieldActor resolveStaticFieldForWriting(ResolutionGuard guard) {
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
        private static void resolve(ResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool;
            final int index = guard.constantPoolIndex;
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
        public static FieldActor resolveInstanceFieldForReading(ResolutionGuard guard) {
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
        private static void resolve(ResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool;
            final int index = guard.constantPoolIndex;
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
        public static FieldActor resolveInstanceFieldForWriting(ResolutionGuard guard) {
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
        private static void resolve(ResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool;
            final int index = guard.constantPoolIndex;
            final StaticMethodActor staticMethodActor = constantPool.classMethodAt(index).resolveStatic(constantPool, index);
            if (staticMethodActor.isInitializer()) {
                throw new VerifyError("<init> must be invoked with invokespecial");
            }
            guard.value = staticMethodActor;
        }

        @SNIPPET
        @INLINE(afterSnippetsAreCompiled = true)
        public static StaticMethodActor resolveStaticMethod(ResolutionGuard guard) {
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
        private static void resolve(ResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool;
            final int index = guard.constantPoolIndex;
            VirtualMethodActor virtualMethodActor = constantPool.classMethodAt(index).resolveVirtual(constantPool, index);
            if (isSpecial(virtualMethodActor, constantPool.holder())) {
                virtualMethodActor = constantPool.holder().superClassActor.findVirtualMethodActor(virtualMethodActor);
                if (virtualMethodActor == null) {
                    throw new AbstractMethodError();
                }
            }
            if (virtualMethodActor.isAbstract()) {
                throw new AbstractMethodError();
            }
            guard.value = virtualMethodActor;
        }

        @SNIPPET
        @INLINE
        public static VirtualMethodActor resolveSpecialMethod(ResolutionGuard guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asVirtualMethodActor(guard.value);
        }

        public static final ResolveSpecialMethod SNIPPET = new ResolveSpecialMethod();
    }

    public static final class ResolveVirtualMethod extends ResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool;
            final int index = guard.constantPoolIndex;
            final VirtualMethodActor virtualMethodActor = constantPool.classMethodAt(index).resolveVirtual(constantPool, index);
            if (virtualMethodActor.isInitializer()) {
                throw new VerifyError("<init> must be invoked with invokespecial");
            }
            guard.value = virtualMethodActor;
        }

        @SNIPPET
        @INLINE(afterSnippetsAreCompiled = true)
        public static VirtualMethodActor resolveVirtualMethod(ResolutionGuard guard) {
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
        public static void resolve(ResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool;
            final int index = guard.constantPoolIndex;
            final InterfaceMethodRefConstant methodConstant = constantPool.interfaceMethodAt(index);
            final MethodActor declaredMethod = methodConstant.resolve(constantPool, index);
            if (!(declaredMethod instanceof InterfaceMethodActor)) {
                throw new InternalError("Use of INVOKEINTERFACE for a method in java.lang.Object should have been rewritten");
            }
            guard.value = declaredMethod;
        }

        @SNIPPET
        @INLINE
        public static InterfaceMethodActor resolveInterfaceMethod(ResolutionGuard guard) {
            if (guard.value == null) {
                resolve(guard);
            }
            return UnsafeCast.asInterfaceMethodActor(guard.value);
        }

        public static final ResolveInterfaceMethod SNIPPET = new ResolveInterfaceMethod();
    }
}
