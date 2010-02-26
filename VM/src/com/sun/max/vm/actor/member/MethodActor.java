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
package com.sun.max.vm.actor.member;

import static com.sun.max.vm.actor.member.InjectedReferenceFieldActor.*;
import static com.sun.max.vm.type.ClassRegistry.Property.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

import sun.reflect.*;

import com.sun.c1x.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.profile.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reflection.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Internal representations of Java methods.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class MethodActor extends MemberActor {

    /**
     * Extended {@linkplain Bytecodes#isExtension(int) opcode} for an {@linkplain INTRINSIC intrinsic} method.
     * A value of 0 means this method is not an intrinsic method.
     */
    private final char intrinsic;

    public static final MethodActor[] NONE = {};

    public static final TypeDescriptor[] NO_CHECKED_EXCEPTIONS = {};
    public static final byte[] NO_ANNOTATION_DEFAULT_BYTES = null;
    public static final byte[] NO_RUNTIME_VISIBLE_PARAMETER_ANNOTATION_BYTES = null;

    public MethodActor(Utf8Constant name,
                       SignatureDescriptor descriptor,
                       int flags,
                       int intrinsic) {
        super(name, descriptor, flags);
        assert (char) intrinsic == intrinsic;
        this.intrinsic = (char) intrinsic;
    }

    /**
     * Gets the {@linkplain Bytecodes#isExtension(int) extended opcode} of this method if
     * it is an {@link INTRINSIC} method.
     *
     * @return 0 if this method is not an intrinsic method
     */
    public final int intrinsic() {
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
    public final boolean isVmEntryPoint() {
        return isVmEntryPoint(flags());
    }

    @INLINE
    public final boolean isIntrinsic() {
        return intrinsic != 0;
    }

    @INLINE
    public final boolean isTrapStub() {
        return Trap.isTrapStub(this);
    }

    @INLINE
    public final boolean isTemplate() {
        return isTemplate(flags());
    }

    @INLINE
    public final boolean isBuiltin() {
        return isBuiltin(flags());
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
    public final boolean isInlineAfterSnippetsAreCompiled() {
        return isInlineAfterSnippetsAreCompiled(flags());
    }

    @INLINE
    public final boolean isNeverInline() {
        return isNeverInline(flags());
    }

    @INLINE
    public final boolean isStaticTrampoline() {
        return isStaticTrampoline(flags());
    }

    @INLINE
    public final boolean isTrampoline() {
        return isTrampoline(flags());
    }

    @INLINE
    public final boolean isVirtualTrampoline() {
        return isVirtualTrampoline(flags());
    }

    @INLINE
    public final boolean isInterfaceTrampoline() {
        return isInterfaceTrampoline(flags());
    }

    public final boolean isApplicationVisible() {
        return !(isNative() || holder().isGenerated());
    }

    /**
     * @return whether this method was generated merely to provide an entry in a vtable slot
     * that would otherwise be empty.
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

    public static MethodActor fromJava(Method javaMethod) {
        if (MaxineVM.isHosted()) {
            return JavaPrototype.javaPrototype().toMethodActor(javaMethod);
        }
        // The injected field in a Method object that is used to speed up this translation is lazily initialized.
        MethodActor methodActor = (MethodActor) TupleAccess.readObject(javaMethod, Method_methodActor.offset());
        if (methodActor == null) {
            final String name = javaMethod.getName();
            methodActor = findMethodActor(ClassActor.fromJava(javaMethod.getDeclaringClass()), SymbolTable.makeSymbol(name), SignatureDescriptor.fromJava(javaMethod));
            TupleAccess.writeObject(javaMethod, Method_methodActor.offset(), methodActor);
        }
        return methodActor;
    }

    public static MethodActor fromJavaConstructor(Constructor javaConstructor) {
        if (MaxineVM.isHosted()) {
            return JavaPrototype.javaPrototype().toMethodActor(javaConstructor);
        }
        // The injected field in a Constructor object that is used to speed up this translation is lazily initialized.
        MethodActor methodActor = (MethodActor) TupleAccess.readObject(javaConstructor, Constructor_methodActor.offset());
        if (methodActor == null) {
            methodActor = findMethodActor(ClassActor.fromJava(javaConstructor.getDeclaringClass()), SymbolTable.INIT, SignatureDescriptor.fromJava(javaConstructor));
            TupleAccess.writeObject(javaConstructor, Constructor_methodActor.offset(), methodActor);
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
        Metrics.increment("MethodActor.toJava()");
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
        TupleAccess.writeObject(javaMethod, Method_methodActor.offset(), this);
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
        TupleAccess.writeObject(javaConstructor, Constructor_methodActor.offset(), this);
        return javaConstructor;
    }

    @Override
    public final <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        final TypeDescriptor annotationTypeDescriptor = JavaTypeDescriptor.forJavaClass(annotationClass);
        if (MaxineVM.isMaxineClass(annotationTypeDescriptor) && !MaxineVM.isMaxineClass(holder())) {
            return null;
        }
        if (isInstanceInitializer()) {
            return toJavaConstructor().getAnnotation(annotationClass);
        }
        if (isClassInitializer() || isMiranda()) {
            return null;
        }
        return toJava().getAnnotation(annotationClass);
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
    public GeneratedStub makeInvocationStub() {
        ClassRegistry classRegistry = holder().classRegistry();
        GeneratedStub invocationStub = classRegistry.get(INVOCATION_STUB, this);
        if (invocationStub == null) {
            if (isInstanceInitializer()) {
                invocationStub = GeneratedStub.newConstructorStub(toJavaConstructor(), null, Boxing.VALUE);
            } else {
                invocationStub = GeneratedStub.newMethodStub(toJava(), Boxing.VALUE);
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
     * @param argumentValues the values to be passed as the arguments of the invocation
     */
    public Value invoke(Value... argumentValues) throws InvocationTargetException, IllegalAccessException {
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
            final Object[] arguments = getBoxedJavaValues(Arrays.subArray(argumentValues, 1), parameterTypes);
            return resultKind.asValue(javaMethod.invoke(receiver, arguments));
        }
        final GeneratedMethodStub stub = UnsafeCast.asGeneratedMethodStub(makeInvocationStub());
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
     * @param argumentValues the values to be passed as the arguments of the invocation. Note that this does not include
     *            the uninitialized object as it is created by this invocation.
     */
    public Value invokeConstructor(Value... argumentValues) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        assert isInstanceInitializer();
        final GeneratedConstructorStub stub = UnsafeCast.asGeneratedConstructorStub(makeInvocationStub());
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
                final Class<? extends Word> wordType = StaticLoophole.cast(type, parameterType);
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

    public void write(DataOutput stream) throws IOException {
        MethodID.fromMethodActor(this).write(stream);
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

    public static MethodActor read(DataInput stream) throws IOException {
        return MethodID.toMethodActor(MethodID.fromWord(Word.read(stream)));
    }

    @FOLD
    public static VirtualMethodActor findVirtual(ClassActor classActor, String name) {
        return classActor.findLocalVirtualMethodActor(name);
    }

    @FOLD
    public static StaticMethodActor findStatic(Class javaClass, String name) {
        return ClassActor.fromJava(javaClass).findLocalStaticMethodActor(name);
    }
}
