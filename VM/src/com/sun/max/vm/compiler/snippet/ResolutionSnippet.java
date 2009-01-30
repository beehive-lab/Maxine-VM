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

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.runtime.*;

/**
 * Snippets that perform constant pool resolution. Each resolution snippet type has a corresponding
 * {@link ResolutionGuard resolution guard type}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class ResolutionSnippet extends Snippet {

    protected <ResolutionGuard_Type extends ResolutionGuard> ResolutionSnippet(Class<ResolutionGuard_Type> resolutionGuardType) {
        ProgramError.check((serial() & 0xffff) == serial());
        final Class[] expectedSignature = {resolutionGuardType};
        for (Method method : getClass().getDeclaredMethods()) {
            if (method.getAnnotation(SNIPPET.class) != null) {
                ProgramError.check(Arrays.equals(expectedSignature, method.getParameterTypes()));
            }
        }
    }

    /**
     * Creates a new guard for the resolution and/or initialization performed by this snippet.
     */
    public abstract ResolutionGuard createGuard(ConstantPool constantPool, int constantPoolIndex);

    /**
     * All resolution snippets that do not override {@link ResolutionSnippet#isFoldable(IrValue[])} must conform with
     * this parameter list.
     */
    static enum Parameter {
        guard;

        static final Sequence<Parameter> VALUES = new ArraySequence<Parameter>(values());
    }

    @Override
    public boolean isFoldable(IrValue[] arguments) {
        assert arguments.length == Parameter.VALUES.length() : arguments.length;
        if (!super.isFoldable(arguments)) {
            // This occurs when compiling the stub for folding a snippet
            return false;
        }
        final ResolutionGuard guard = (ResolutionGuard) arguments[Parameter.guard.ordinal()].value().asObject();
        if (!guard.isClear()) {
            return true;
        }
        final ConstantPool constantPool = guard.constantPool();
        final ResolvableConstant resolvableConstant = constantPool.resolvableAt(guard.constantPoolIndex());
        if (resolvableConstant.isResolvableWithoutClassLoading(constantPool)) {
            try {
                resolvableConstant.resolve(constantPool, guard.constantPoolIndex());
                return true;
            } catch (LinkageError linkageError) {
                // Whatever went wrong here is supposed to show up at runtime, too.
                // So we just don't fold here and move on.
            }
        }
        return false;
    }

    abstract static class ReferenceResolutionSnippet extends ResolutionSnippet {

        ReferenceResolutionSnippet() {
            super(ReferenceResolutionGuard.class);
        }

        @Override
        public final ResolutionGuard createGuard(ConstantPool constantPool, int constantPoolIndex) {
            return new ReferenceResolutionGuard(constantPool, constantPoolIndex);
        }
    }

    abstract static class EntrypointResolutionSnippet extends ResolutionSnippet {
        EntrypointResolutionSnippet() {
            super(EntrypointResolutionGuard.class);
        }

        @Override
        public final ResolutionGuard createGuard(ConstantPool constantPool, int constantPoolIndex) {
            return new EntrypointResolutionGuard(constantPool, constantPoolIndex);
        }
    }

    public static final class ResolveClass extends ReferenceResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ReferenceResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            final ClassActor classActor = constantPool.classAt(index).resolve(constantPool, index);
            guard.set(classActor);
        }

        @SNIPPET
        @INLINE(afterSnippetsAreCompiled = true)
        public static Object resolveClass(ReferenceResolutionGuard guard) {
            if (guard.inlinedIsClear()) {
                resolve(guard);
            }
            return guard.value();
        }

        public static final ResolveClass SNIPPET = new ResolveClass();
    }

    public static final class ResolveArrayClass extends ReferenceResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ReferenceResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            final ArrayClassActor arrayClassActor = ArrayClassActor.forComponentClassActor(constantPool.classAt(index).resolve(constantPool, index));
            guard.set(arrayClassActor);
        }

        @SNIPPET
        @INLINE(afterSnippetsAreCompiled = true)
        public static Object resolveArrayClass(ReferenceResolutionGuard guard) {
            if (guard.inlinedIsClear()) {
                resolve(guard);
            }
            return guard.value();
        }

        public static final ResolveArrayClass SNIPPET = new ResolveArrayClass();
    }

    public static final class ResolveClassForNew extends ReferenceResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ReferenceResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            final ClassActor classActor = constantPool.classAt(index).resolve(constantPool, index);
            if (classActor.isAbstract()) {
                // Covers abstract classes and interfaces
                throw new InstantiationError();
            }
            classActor.makeInitialized();
            guard.set(classActor);
        }

        @SNIPPET
        @INLINE
        public static Object resolveClassForNew(ReferenceResolutionGuard guard) {
            if (guard.inlinedIsClear()) {
                resolve(guard);
            }
            return guard.value();
        }

        public static final ResolveClassForNew SNIPPET = new ResolveClassForNew();
    }


    public static final class ResolveStaticFieldForReading extends ReferenceResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ReferenceResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            final FieldActor fieldActor = constantPool.fieldAt(index).resolve(constantPool, index);
            if (!fieldActor.isStatic()) {
                throw new IncompatibleClassChangeError();
            }
            fieldActor.holder().makeInitialized();
            guard.set(fieldActor);
        }

        @SNIPPET
        @INLINE
        public static Object resolveStaticFieldForReading(ReferenceResolutionGuard guard) {
            if (guard.inlinedIsClear()) {
                resolve(guard);
            }
            return guard.value();
        }

        public static final ResolveStaticFieldForReading SNIPPET = new ResolveStaticFieldForReading();
    }

    public static final class ResolveStaticFieldForWriting extends ReferenceResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ReferenceResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            final FieldActor fieldActor = constantPool.fieldAt(index).resolve(constantPool, index);
            if (!fieldActor.isStatic()) {
                throw new IncompatibleClassChangeError();
            }
            if (fieldActor.isFinal() && fieldActor.holder() != constantPool.holder()) {
                throw new IllegalAccessError();
            }
            fieldActor.holder().makeInitialized();
            guard.set(fieldActor);
        }

        @SNIPPET
        @INLINE
        public static Object resolveStaticFieldForWriting(ReferenceResolutionGuard guard) {
            if (guard.inlinedIsClear()) {
                resolve(guard);
            }
            return guard.value();
        }

        public static final ResolveStaticFieldForWriting SNIPPET = new ResolveStaticFieldForWriting();
    }

    public static final class ResolveInstanceFieldForReading extends ReferenceResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ReferenceResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            final FieldActor fieldActor = constantPool.fieldAt(index).resolve(constantPool, index);
            if (fieldActor.isStatic()) {
                throw new IncompatibleClassChangeError();
            }
            guard.set(fieldActor);
        }

        /**
         * This is annotated with both SNIPPET and INLINE so that if folding fails during compilation (because
         * the field isn't resolved at compiled time), the guarding code gets inlined in the user code.
         */
        @SNIPPET
        @INLINE
        public static Object resolveInstanceFieldForReading(ReferenceResolutionGuard guard) {
            if (guard.inlinedIsClear()) {
                resolve(guard);
            }
            return guard.value();
        }

        public static final ResolveInstanceFieldForReading SNIPPET = new ResolveInstanceFieldForReading();
    }

    public static final class ResolveInstanceFieldForWriting extends ReferenceResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ReferenceResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            final FieldActor fieldActor = constantPool.fieldAt(index).resolve(constantPool, index);
            if (fieldActor.isStatic()) {
                throw new IncompatibleClassChangeError();
            }
            if (fieldActor.isFinal() && fieldActor.holder() != constantPool.holder()) {
                throw new IllegalAccessError();
            }
            guard.set(fieldActor);

        }

        @SNIPPET
        @INLINE
        public static Object resolveInstanceFieldForWriting(ReferenceResolutionGuard guard) {
            if (guard.inlinedIsClear()) {
                resolve(guard);
            }
            return guard.value();
        }

        public static final ResolveInstanceFieldForWriting SNIPPET = new ResolveInstanceFieldForWriting();
    }

    public static final class ResolveStaticMethod extends EntrypointResolutionSnippet {
        @NEVER_INLINE
        private static void resolve(EntrypointResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            guard.set(CompilationScheme.Static.compile(quasiFold(constantPool, index), CallEntryPoint.OPTIMIZED_ENTRY_POINT, CompilationDirective.DEFAULT));
        }

        /**
         * Folds a static method constant pool reference to ClassMethodActor as opposed to a target method.
         * This is used when folding the {@link CirResolveStaticMethod CIR version of this snippet}.
         */
        public static StaticMethodActor quasiFold(ConstantPool constantPool, int index) {
            final StaticMethodActor staticMethodActor = constantPool.classMethodAt(index).resolveStatic(constantPool, index);
            if (staticMethodActor.isInitializer()) {
                throw new VerifyError("<init> must be invoked with invokespecial");
            }
            staticMethodActor.holder().makeInitialized();
            return staticMethodActor;
        }

        @SNIPPET
        @INLINE(afterSnippetsAreCompiled = true)
        public static Word resolveStaticMethod(EntrypointResolutionGuard guard) {
            if (guard.inlinedIsClear()) {
                resolve(guard);
            }
            return guard.value();
        }

        @NEVER_INLINE
        public static Word resolveTracedStaticMethod(EntrypointResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            return CompilationScheme.Static.compile(quasiFold(constantPool, index), CallEntryPoint.OPTIMIZED_ENTRY_POINT, CompilationDirective.TRACE_JIT);
        }

        public static final ResolveStaticMethod SNIPPET = new ResolveStaticMethod();
    }

    public static boolean isSpecial(MethodActor declaredMethod, ClassActor currentClassActor) {
        final ClassActor holder = declaredMethod.holder();
        return (currentClassActor.flags() & Actor.ACC_SUPER) != 0 && currentClassActor != holder && currentClassActor.hasSuperClass(holder) && !declaredMethod.isInstanceInitializer();
    }

    public static final class ResolveSpecialMethod extends EntrypointResolutionSnippet {
        public static VirtualMethodActor quasiFold(ConstantPool pool, int index) {
            VirtualMethodActor dynamicMethodActor = pool.classMethodAt(index).resolveVirtual(pool, index);
            if (isSpecial(dynamicMethodActor, pool.holder())) {
                dynamicMethodActor = pool.holder().superClassActor().findVirtualMethodActor(dynamicMethodActor);
                if (dynamicMethodActor == null) {
                    throw new AbstractMethodError();
                }
            }
            if (dynamicMethodActor.isAbstract()) {
                throw new AbstractMethodError();
            }
            return dynamicMethodActor;
        }

        @NEVER_INLINE
        public static void resolve(EntrypointResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            final VirtualMethodActor selectedMethod = quasiFold(constantPool, index);
            guard.set(CompilationScheme.Static.compile(selectedMethod, CallEntryPoint.OPTIMIZED_ENTRY_POINT, CompilationDirective.DEFAULT));
        }

        @SNIPPET
        @INLINE
        public static Word resolveSpecialMethod(EntrypointResolutionGuard guard) {
            if (guard.inlinedIsClear()) {
                resolve(guard);
            }
            return guard.value();
        }

        public static Word resolveTracedStaticMethod(EntrypointResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            final VirtualMethodActor selectedMethod = quasiFold(constantPool, index);
            return CompilationScheme.Static.compile(selectedMethod, CallEntryPoint.OPTIMIZED_ENTRY_POINT, CompilationDirective.TRACE_JIT);
        }

        public static final ResolveSpecialMethod SNIPPET = new ResolveSpecialMethod();
    }


    public static final class ResolveVirtualMethod extends ReferenceResolutionSnippet {

        @NEVER_INLINE
        private static void resolve(ReferenceResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            final VirtualMethodActor dynamicMethodActor = constantPool.classMethodAt(index).resolveVirtual(constantPool, index);
            if (dynamicMethodActor.isInitializer()) {
                throw new VerifyError("<init> must be invoked with invokespecial");
            }
            guard.set(dynamicMethodActor);
        }

        @SNIPPET
        @INLINE(afterSnippetsAreCompiled = true)
        public static Object resolveVirtualMethod(ReferenceResolutionGuard guard) {
            if (guard.inlinedIsClear()) {
                resolve(guard);
            }
            return guard.value();
        }

        public static final ResolveVirtualMethod SNIPPET = new ResolveVirtualMethod();
    }

    public static final class ResolveInterfaceMethod extends ReferenceResolutionSnippet {

        @NEVER_INLINE
        public static void resolve(ReferenceResolutionGuard guard) {
            final ConstantPool constantPool = guard.constantPool();
            final int index = guard.constantPoolIndex();
            final MethodRefConstant methodConstant = constantPool.interfaceMethodAt(index);
            final MethodActor declaredMethod = methodConstant.resolve(constantPool, index);
            guard.set(declaredMethod);
        }

        @SNIPPET
        @INLINE
        public static Object resolveInterfaceMethod(ReferenceResolutionGuard guard) {
            if (guard.inlinedIsClear()) {
                resolve(guard);
            }
            return guard.value();
        }

        public static final ResolveInterfaceMethod SNIPPET = new ResolveInterfaceMethod();
    }
}
