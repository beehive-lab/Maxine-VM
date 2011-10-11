/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.actor.member;

import static com.sun.max.vm.actor.member.InjectedReferenceFieldActor.*;
import static com.sun.max.vm.type.ClassRegistry.Property.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import sun.reflect.*;

import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.reflection.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Internal representations of Java methods.
 */
public abstract class MethodActor extends MemberActor implements RiMethod {

    /**
     * Extended {@linkplain Bytecodes#isStandard(int) opcode} for an {@linkplain INTRINSIC intrinsic} method.
     * A value of 0 means this method is not an intrinsic method.
     */
    private final String intrinsic;

    public static final MethodActor[] NONE = {};

    public static final TypeDescriptor[] NO_CHECKED_EXCEPTIONS = {};
    public static final byte[] NO_ANNOTATION_DEFAULT_BYTES = null;
    public static final byte[] NO_RUNTIME_VISIBLE_PARAMETER_ANNOTATION_BYTES = null;

    public MethodActor(Utf8Constant name,
                       SignatureDescriptor descriptor,
                       int flags,
                       String intrinsic) {
        super(name, descriptor, flags);
        this.intrinsic = intrinsic;
    }

    /**
     * Gets the intrinsic id of this method.
     */
    public final String intrinsic() {
        return intrinsic;
    }

    @INLINE
    public final boolean isSynchronized() {
        return isSynchronized(flags());
    }

    @INLINE
    public final boolean isNative() {
        return isNative(flags());
    }

    @INLINE
    public final boolean isStrict() {
        return isStrict(flags());
    }

    @INLINE
    public final boolean isClassInitializer() {
        return isClassInitializer(flags());
    }

    @INLINE
    public final boolean isInstanceInitializer() {
        return isInstanceInitializer(flags());
    }

    @INLINE
    public final boolean isInitializer() {
        return isInitializer(flags());
    }

    @INLINE
    public final boolean isCFunction() {
        return isCFunction(flags());
    }

    @INLINE
    public final boolean isCFunctionNoLatch() {
        return isCFunctionNoLatch(flags());
    }

    @INLINE
    public final boolean isVmEntryPoint() {
        return isVmEntryPoint(flags());
    }

    @INLINE
    public final boolean isIntrinsic() {
        return intrinsic != null;
    }

    @INLINE
    public final boolean isTemplate() {
        return isTemplate(flags());
    }

    @INLINE
    public final boolean isLocalSubstitute() {
        return isLocalSubstitute(flags());
    }

    @INLINE
    public final boolean isUnsafe() {
        return isUnsafe(flags());
    }

    @INLINE
    public final boolean isInline() {
        return isInline(flags());
    }

    @INLINE
    public final boolean isNeverInline() {
        return isNeverInline(flags());
    }

    @INLINE
    public final boolean noSafepointPolls() {
        return noSafepointPolls(flags()) || isTemplate();
    }

    public boolean minimalDebugInfo() {
        // Code pos and frame info are not needed for templates
        return isTemplate();
    }

    /**
     * @return whether this method was generated merely to provide an entry in a vtable slot that would otherwise be
     *         empty.
     *
     * @see MirandaMethodActor
     */
    public boolean isMiranda() {
        return false;
    }

    @Override
    public final boolean isHiddenToReflection() {
        return isInitializer() || isMiranda();
    }

    /**
     * Gets the bytes of the RuntimeVisibleParameterAnnotations class file attribute associated with this method actor.
     *
     * @return null if there is no RuntimeVisibleParameterAnnotations attribute associated with this method actor
     */
    public final byte[] runtimeVisibleParameterAnnotationsBytes() {
        return holder().classRegistry().get(RUNTIME_VISIBLE_PARAMETER_ANNOTATION_BYTES, this);
    }

    /**
     * Gets the bytes of the AnnotationDefault class file attribute associated with this method actor.
     *
     * @return null if there is no AnnotationDefault attribute associated with this method actor
     */
    public final byte[] annotationDefaultBytes() {
        return holder().classRegistry().get(ANNOTATION_DEFAULT_BYTES, this);
    }

    @INLINE
    public final SignatureDescriptor descriptor() {
        return (SignatureDescriptor) descriptor;
    }

    /**
     * Gets the array of checked exceptions declared by this method actor.
     *
     * @return a zero-length array if there are no checked exceptions declared by this method actor
     */
    public final TypeDescriptor[] checkedExceptions() {
        return holder().classRegistry().get(CHECKED_EXCEPTIONS, this);
    }

    /**
     * Gets the value of the {@link ACCESSOR} annotation that was applied to this method.
     *
     * @return {@code null} if this method has no {@link ACCESSOR} annotation
     */
    public final RiType accessor() {
        Class<?> accessorClass = holder().classRegistry().get(ACCESSOR, this);
        return accessorClass == null ? null : ClassActor.fromJava(accessorClass);
    }

    public static MethodActor fromJava(Method javaMethod) {
        if (MaxineVM.isHosted()) {
            return JavaPrototype.javaPrototype().toMethodActor(javaMethod);
        }
        // The injected field in a Method object that is used to speed up this translation is lazily initialized.
        MethodActor methodActor = (MethodActor) Method_methodActor.getObject(javaMethod);
        if (methodActor == null) {
            final String name = javaMethod.getName();
            methodActor = findMethodActor(ClassActor.fromJava(javaMethod.getDeclaringClass()), SymbolTable.makeSymbol(name), SignatureDescriptor.fromJava(javaMethod));
            Method_methodActor.setObject(javaMethod, methodActor);
        }
        return methodActor;
    }

    @Override
    public String toString() {
        return format("%H.%n(%p)");
    }

    public static MethodActor fromJavaConstructor(Constructor javaConstructor) {
        if (MaxineVM.isHosted()) {
            return JavaPrototype.javaPrototype().toMethodActor(javaConstructor);
        }
        // The injected field in a Constructor object that is used to speed up this translation is lazily initialized.
        MethodActor methodActor = (MethodActor) Constructor_methodActor.getObject(javaConstructor);
        if (methodActor == null) {
            methodActor = findMethodActor(ClassActor.fromJava(javaConstructor.getDeclaringClass()), SymbolTable.INIT, SignatureDescriptor.fromJava(javaConstructor));
            Constructor_methodActor.setObject(javaConstructor, methodActor);
        }
        return methodActor;
    }

    private static MethodActor findMethodActor(ClassActor holder, Utf8Constant name, SignatureDescriptor signature) {
        final MethodActor methodActor = holder.findLocalMethodActor(name, signature);
        ProgramError.check(methodActor != null, "Could not find " + name + signature + " in " + holder);
        return methodActor;
    }

    public final Method toJava() {
        assert !isInstanceInitializer();
        if (MaxineVM.isHosted()) {
            return JavaPrototype.javaPrototype().toJava(this);
        }
        final Class<?> javaHolder = holder().toJava();
        final ClassLoader holderClassLoader = javaHolder.getClassLoader();
        final Class[] parameterTypes = descriptor().resolveParameterTypes(holderClassLoader);
        final Class returnType = descriptor().resultDescriptor().resolveType(holderClassLoader);
        final TypeDescriptor[] checkedExceptions = checkedExceptions();
        final Class[] checkedExceptionTypes = checkedExceptions == null ? new Class[0] : JavaTypeDescriptor.resolveToJavaClasses(checkedExceptions, holderClassLoader);
        final Method javaMethod = ReflectionFactory.getReflectionFactory().newMethod(
                        javaHolder,
                        name.toString(),
                        parameterTypes,
                        returnType,
                        checkedExceptionTypes,
                        flags(),
                        memberIndex(),
                        genericSignatureString(),
                        runtimeVisibleAnnotationsBytes(),
                        runtimeVisibleParameterAnnotationsBytes(),
                        annotationDefaultBytes());
        Method_methodActor.setObject(javaMethod, this);
        return javaMethod;
    }

    public final Constructor<?> toJavaConstructor() {
        assert isInstanceInitializer();
        if (MaxineVM.isHosted()) {
            return JavaPrototype.javaPrototype().toJavaConstructor(this);
        }
        final Class<?> javaHolder = holder().toJava();
        final Class[] parameterTypes = descriptor().resolveParameterTypes(javaHolder.getClassLoader());
        final TypeDescriptor[] checkedExceptions = checkedExceptions();
        final Class[] checkedExceptionTypes = checkedExceptions == null ? new Class[0] : JavaTypeDescriptor.resolveToJavaClasses(checkedExceptions, holder().classLoader);
        final Constructor javaConstructor = ReflectionFactory.getReflectionFactory().newConstructor(
                        javaHolder,
                        parameterTypes,
                        checkedExceptionTypes,
                        flags(),
                        -1, // "java.lang.reflect.Constructor.slot", (apparently) not used throughout the JDK
                        genericSignatureString(),
                        runtimeVisibleAnnotationsBytes(),
                        runtimeVisibleParameterAnnotationsBytes());
        Constructor_methodActor.setObject(javaConstructor, this);
        return javaConstructor;
    }

    @Override
    public final <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        if (isInstanceInitializer()) {
            return toJavaConstructor().getAnnotation(annotationClass);
        }
        if (isClassInitializer() || isMiranda()) {
            return null;
        }
        try {
            return toJava().getAnnotation(annotationClass);
        } catch (NoSuchMethodError e) {
            // Handles methods that are hidden to reflection (e.g. Unsage.getUnsafe())
            return null;
        }
    }

    @Override
    public final String javaSignature(boolean qualified) {
        if (qualified) {
            return format("%R %n(%P)");
        }
        return format("%r %n(%p)");
    }

    /**
     * Gets the invocation stub for this method actor, creating it first if necessary.
     */
    public final InvocationStub makeInvocationStub() {
        ClassRegistry classRegistry = holder().classRegistry();
        InvocationStub invocationStub = classRegistry.get(INVOCATION_STUB, this);
        if (invocationStub == null) {
            if (isInstanceInitializer()) {
                invocationStub = InvocationStub.newConstructorStub(toJavaConstructor(), null, Boxing.VALUE);
            } else {
                invocationStub = InvocationStub.newMethodStub(toJava(), Boxing.VALUE);
            }
            classRegistry.set(INVOCATION_STUB, this, invocationStub);
        }
        return invocationStub;
    }

    public static boolean containWord(Value[] values) {
        for (Value value : values) {
            if (value.kind().isWord) {
                return true;
            }
        }
        return false;
    }

    /**
     * Invokes the method represented by this method actor with the given parameter values.
     *
     * This is akin to the standard Java reflective method {@linkplain Method#invoke(Object, Object...) invocation}
     * except that the parameter values are boxed in {@link Value} objects and the
     * receiver object for a non-static method is in element 0 of {@code argumentValues} (as opposed to
     * being a separate parameter).
     *
     * This method throws the same exceptions as {@link Method#invoke(Object, Object...)}.
     *
     * NOTE: Reflection invocation should not be used for accessing otherwise hidden methods in the JDK.
     * Instead, method {@linkplain ALIAS aliasing} mechanism should be used.
     *
     * @param argumentValues the values to be passed as the arguments of the invocation
     */
    public final Value invoke(Value... argumentValues) throws InvocationTargetException, IllegalAccessException {
        assert !isInstanceInitializer();
        if (MaxineVM.isHosted()) {
            // When running hosted, the generated stub cannot be executed, because it does not verify.
            // In this situation we simply use normal Java reflection.
            final Method javaMethod = toJava();
            final Kind resultKind = Kind.fromJava(javaMethod.getReturnType());
            final Class[] parameterTypes = javaMethod.getParameterTypes();
            javaMethod.setAccessible(true);
            if ((javaMethod.getModifiers() & Modifier.STATIC) != 0) {
                final Object[] arguments = getBoxedJavaValues(argumentValues, parameterTypes);
                return resultKind.asValue(javaMethod.invoke(null, arguments));
            }
            final Object receiver = getBoxedJavaValue(argumentValues[0], javaMethod.getDeclaringClass());
            final Object[] arguments = getBoxedJavaValues(Arrays.copyOfRange(argumentValues, 1, argumentValues.length), parameterTypes);
            return resultKind.asValue(javaMethod.invoke(receiver, arguments));
        }
        final MethodInvocationStub stub = UnsafeCast.asMethodInvocationStub(makeInvocationStub());
        return stub.invoke(argumentValues);
    }

    /**
     * Invokes the method represented by this method actor with the given parameter values.
     *
     * This is akin to the standard Java reflective constructor {@linkplain Constructor#newInstance(Object...)
     * invocation} except that the parameter values are boxed in {@link Value} objects.
     *
     * This method throws the same exceptions as {@link Constructor#newInstance(Object...)}.
     *
     * NOTE: Reflection invocation should not be used for accessing otherwise hidden constructors in the JDK.
     * Instead, method {@linkplain ALIAS aliasing} mechanism should be used.
     *
     * @param argumentValues the values to be passed as the arguments of the invocation. Note that this does not include
     *            the uninitialized object as it is created by this invocation.
     */
    public final Value invokeConstructor(Value... argumentValues) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        assert isInstanceInitializer();
        final ConstructorInvocationStub stub = UnsafeCast.asConstructorInvocationStub(makeInvocationStub());
        if (MaxineVM.isHosted()) {
            // When running hosted by HotSpot, the generated stub cannot be executed if the target method is inaccessible.
            // In this situation we simply use normal Java reflection.
            final Constructor javaConstructor = toJavaConstructor();
            final Class stubClass = stub.getClass();
            if (!Reflection.verifyMemberAccess(stubClass, holder().toJava(), null, flags()) || !stub.canAccess(javaConstructor.getParameterTypes())) {
                final Kind resultKind = Kind.fromJava(javaConstructor.getDeclaringClass());
                final Class[] parameterTypes = javaConstructor.getParameterTypes();
                javaConstructor.setAccessible(true);
                final Object[] arguments = getBoxedJavaValues(argumentValues, parameterTypes);
                return resultKind.asValue(javaConstructor.newInstance(arguments));
            }
        }
        return stub.newInstance(argumentValues);
    }

    @HOSTED_ONLY
    private static Object getBoxedJavaValue(Value value, Class<?> parameterType) {
        final Kind parameterKind = Kind.fromJava(parameterType);
        if (parameterKind.isWord) {
            if (MaxineVM.isHosted()) {
                final Word word = value.unboxWord();
                final Class<Class<? extends Word>> type = null;
                final Class<? extends Word> wordType = Utils.cast(type, parameterType);
                return word.as(wordType);
            }
            throw ProgramError.unexpected();
        }
        try {
            return InvocationStubGenerator.findValueUnboxMethod(parameterKind).invoke(value);
        } catch (Exception e) {
            throw ProgramError.unexpected(e);
        }
    }

    /**
     * Converts an array of values boxed as {@link Value}s to an array of values boxed as Objects as
     * expected by {@link Method#invoke} or {@link Constructor#newInstance}. In addition, an extra
     * argument of the appropriate array type is appended as necessary if the method or constructor
     * to be invoked was declared to take a variable number of arguments (i.e. if
     * {@code isVarArgs == true && parameterTypes.length == values.length + 1}).
     * @param values
     * @param parameterTypes
     *
     * @return
     */
    @HOSTED_ONLY
    private static Object[] getBoxedJavaValues(Value[] values, Class[] parameterTypes) {
        final Object[] boxedJavaValues = new Object[parameterTypes.length];
        assert values.length == parameterTypes.length;
        for (int i = 0; i != values.length; ++i) {
            boxedJavaValues[i] = getBoxedJavaValue(values[i], parameterTypes[i]);
        }
        return boxedJavaValues;
    }

    public Kind resultKind() {
        return descriptor().resultKind();
    }

    /**
     * Gets the {@link Kind kinds} of the runtime parameters taken by this method. The returned array includes an entry
     * at index 0 for the receiver kind if this is not a static method.
     */
    public Kind[] getParameterKinds() {
        if (isStatic()) {
            return descriptor().copyParameterKinds(null, 0);
        }
        final Kind[] kinds = descriptor().copyParameterKinds(null, 1);
        kinds[0] = holder().kind.isWord ? Kind.WORD : Kind.REFERENCE;
        return kinds;
    }

    public final int accessFlags() {
        return flags() & JAVA_METHOD_FLAGS;
    }

    public final boolean canBeStaticallyBound() {
        return (flags() & (ACC_STATIC | ACC_PRIVATE | ACC_FINAL)) != 0;
    }

    public byte[] code() {
        return null;
    }

    public CiExceptionHandler[] exceptionHandlers() {
        return CiExceptionHandler.NONE;
    }

    public boolean hasBalancedMonitors() {
        return true; // TODO: do the required analysis
    }

    public final boolean isConstructor() {
        return isInstanceInitializer();
    }

    public final boolean isLeafMethod() {
        return isStatic() || isPrivate() || holder().isFinal() || isFinal();
    }

    public final boolean isOverridden() {
        // TODO: do more sophisticated leaf method check than just if class has subclass
        // Currently unused, so doesn't matter.
        return !canBeStaticallyBound() && holder().hasSubclass(); // TODO what about interfaces?
    }

    public final boolean isResolved() {
        return true;
    }

    public String jniSymbol() {
        return null;
    }

    public CiBitMap[] livenessMap() {
        return null;
    }

    public int maxLocals() {
        return 0;
    }

    public int maxStackSize() {
        return 0;
    }

    public RiMethodProfile methodData() {
        return null;
    }

    public String name() {
        return name.string;
    }

    public RiSignature signature() {
        return descriptor();
    }
}
