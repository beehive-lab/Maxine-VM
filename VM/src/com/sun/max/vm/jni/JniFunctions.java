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
package com.sun.max.vm.jni;

import static com.sun.max.vm.classfile.ErrorContext.*;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;
import com.sun.max.vm.object.TupleAccess;

/**
 * Upcalls from C that implement JNI functions.
 *
 *
 * ATTENTION: All the methods annotated by {@link JNI_FUNCTION} must appear in the exact same order as specified in
 * jni.h (and jni.c), see the functions at the bottom.
 *
 * @see JniNativeInterface
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class JniFunctions {

    public static final int JNI_OK = 0;
    public static final int JNI_ERR = -1; /* unknown error */
    public static final int JNI_EDETACHED = -2; /* thread detached from the VM */
    public static final int JNI_EVERSION = -3; /* JNI version error */
    public static final int JNI_ENOMEM = -4; /* not enough memory */
    public static final int JNI_EEXIST = -5; /* VM already created */
    public static final int JNI_EINVAL = -6; /* invalid arguments */

    public static final int JNI_COMMIT = 1;
    public static final int JNI_ABORT = 2;

    private JniFunctions() {
    }

    @JNI_FUNCTION
    private static void reserved0(Pointer env) {
    }

    @JNI_FUNCTION
    private static void reserved1(Pointer env) {
    }

    @JNI_FUNCTION
    private static void reserved2(Pointer env) {
    }

    @JNI_FUNCTION
    private static void reserved3(Pointer env) {
    }

    // Checkstyle: stop method name check

    @JNI_FUNCTION
    private static int GetVersion(Pointer env) {
        FatalError.unimplemented();
        return -1;
    }

    private static String dottify(String slashifiedName) {
        return slashifiedName.replace('/', '.');
    }

    private static void traceReflectiveInvocation(MethodActor methodActor) {
        if (ClassMethodActor.traceJNI()) {
            Log.print("[Thread \"");
            Log.print(VmThread.current().getName());
            Log.print("\" --> JNI invoke: ");
            Log.println(methodActor.format("%H.%n(%p)"));
        }
    }

    private static final Class[] defineClassParameterTypes = {String.class, byte[].class, int.class, int.class};

    @JNI_FUNCTION
    private static JniHandle DefineClass(Pointer env, Pointer slashifiedName, JniHandle classLoader, Pointer buffer, int length) throws ClassFormatError {
        final byte[] bytes = new byte[length];
        Memory.readBytes(buffer, length, bytes);
        try {
            // TODO: find out whether already dottified class names should be rejected by this function
            final Object[] arguments = {dottify(CString.utf8ToJava(slashifiedName)), bytes, 0, length};
            Object cl = classLoader.unhand();
            if (cl == null) {
                cl = VmClassLoader.VM_CLASS_LOADER;
            }
            return JniHandles.createLocalHandle(WithoutAccessCheck.invokeVirtual(cl, "defineClass", defineClassParameterTypes, arguments));
        } catch (Utf8Exception utf8Exception) {
            throw classFormatError("Invalid class name");
        } catch (NoSuchMethodException noSuchMethodException) {
            throw ProgramError.unexpected(noSuchMethodException);
        } catch (IllegalAccessException illegalAccessException) {
            throw ProgramError.unexpected(illegalAccessException);
        } catch (InvocationTargetException invocationTargetException) {
            VmThread.fromJniEnv(env).setPendingException(invocationTargetException.getTargetException());
            return JniHandle.zero();
        }
    }

    private static Class findClass(ClassLoader classLoader, String slashifiedName) throws ClassNotFoundException {
        if (slashifiedName.startsWith("[")) {
            final Class componentClass = findClass(classLoader, slashifiedName.substring(1));
            return ArrayClassActor.forComponentClassActor(ClassActor.fromJava(componentClass)).toJava();
        }
        return classLoader.loadClass(dottify(slashifiedName));
    }

    @JNI_FUNCTION
    private static JniHandle FindClass(Pointer env, Pointer name) throws ClassNotFoundException {
        try {
            final Class javaClass = findClass(Code.codePointerToTargetMethod(VMRegister.getInstructionPointer()).classMethodActor().holder().classLoader, CString.utf8ToJava(name));
            MakeClassInitialized.makeClassInitialized(ClassActor.fromJava(javaClass));
            return JniHandles.createLocalHandle(javaClass);
        } catch (Utf8Exception utf8Exception) {
            throw new ClassNotFoundException();
        }
    }

    @JNI_FUNCTION
    private static MethodID FromReflectedMethod(Pointer env, JniHandle method) {
        final MethodActor methodActor = MethodActor.fromJava((Method) method.unhand());
        return MethodID.fromMethodActor(methodActor);
    }

    @JNI_FUNCTION
    private static FieldID FromReflectedField(Pointer env, JniHandle field) {
        final FieldActor fieldActor = FieldActor.fromJava((Field) field.unhand());
        return FieldID.fromFieldActor(fieldActor);
    }

    private static Method ToReflectedMethod(MethodID methodID, boolean isStatic) throws NoSuchMethodException {
        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null || methodActor.isStatic() != isStatic) {
            throw new NoSuchMethodException();
        }
        return methodActor.toJava();
    }

    @JNI_FUNCTION
    private static JniHandle ToReflectedMethod(Pointer env, JniHandle javaClass, MethodID methodID, boolean isStatic) throws NoSuchMethodException {
        return JniHandles.createLocalHandle(ToReflectedMethod(methodID, isStatic));
    }

    @JNI_FUNCTION
    private static JniHandle GetSuperclass(Pointer env, JniHandle subType) {
        return JniHandles.createLocalHandle(((Class) subType.unhand()).getSuperclass());
    }

    @JNI_FUNCTION
    private static boolean IsAssignableFrom(Pointer env, JniHandle subType, JniHandle superType) {
        return ClassActor.fromJava((Class) superType.unhand()).isAssignableFrom(ClassActor.fromJava((Class) subType.unhand()));
    }

    @JNI_FUNCTION
    private static JniHandle ToReflectedField(Pointer env, JniHandle javaClass, FieldID fieldID, boolean isStatic) {
        final FieldActor fieldActor = FieldID.toFieldActor(fieldID);
        if (fieldActor == null || fieldActor.isStatic() != isStatic) {
            throw new NoSuchFieldError();
        }
        return JniHandles.createLocalHandle(fieldActor.toJava());
    }

    @JNI_FUNCTION
    private static int Throw(Pointer env, JniHandle throwable) {
        VmThread.fromJniEnv(env).setPendingException((Throwable) throwable.unhand());
        return JNI_OK;
    }

    @JNI_FUNCTION
    private static int ThrowNew(Pointer env, JniHandle throwableClass, Pointer message) throws Throwable {
        final Class<Class<? extends Throwable>> type = null;
        Constructor<? extends Throwable> constructor = null;
        Class[] parameterTypes = null;
        if (message.isZero()) {
            parameterTypes = new Class[0];
        } else {
            parameterTypes = new Class[1];
            parameterTypes[0] = String.class;
        }
        constructor = StaticLoophole.cast(type, throwableClass.unhand()).getConstructor(parameterTypes);
        Throwable throwable = message.isZero() ? constructor.newInstance() : constructor.newInstance(CString.utf8ToJava(message));
        VmThread.fromJniEnv(env).setPendingException(throwable);
        return JNI_OK;
    }

    @JNI_FUNCTION
    private static JniHandle ExceptionOccurred(Pointer env) {
        return JniHandles.createLocalHandle(VmThread.fromJniEnv(env).pendingException());
    }

    @JNI_FUNCTION
    private static void ExceptionDescribe(Pointer env) {
        final Throwable exception = VmThread.fromJniEnv(env).pendingException();
        if (exception != null) {
            exception.printStackTrace();
        }
    }

    @JNI_FUNCTION
    private static void ExceptionClear(Pointer env) {
        VmThread.fromJniEnv(env).setPendingException(null);
    }

    @JNI_FUNCTION
    private static void FatalError(Pointer env, Pointer message) {
        try {
            FatalError.unexpected(CString.utf8ToJava(message));
        } catch (Utf8Exception utf8Exception) {
            FatalError.unexpected("fatal error with UTF8 error in message");
        }
    }

    private static int PushLocalFrame0(Pointer env, int capacity) {
        JniHandles.pushLocalFrame(capacity);
        return JNI_OK;
    }

    @JNI_FUNCTION
    private static int PushLocalFrame(Pointer env, int capacity) {
        JniHandles.pushLocalFrame(capacity);
        return JNI_OK;
    }

    @JNI_FUNCTION
    private static JniHandle PopLocalFrame(Pointer env, JniHandle result) {
        return JniHandles.popLocalFrame(result);
    }

    @JNI_FUNCTION
    private static JniHandle NewGlobalRef(Pointer env, JniHandle handle) {
        return JniHandles.createGlobalHandle(handle.unhand());
    }

    @JNI_FUNCTION
    private static void DeleteGlobalRef(Pointer env, JniHandle handle) {
        JniHandles.destroyGlobalHandle(handle);
    }

    @JNI_FUNCTION
    private static void DeleteLocalRef(Pointer env, JniHandle handle) {
        JniHandles.destroyLocalHandle(handle);
    }

    @JNI_FUNCTION
    private static boolean IsSameObject(Pointer env, JniHandle object1, JniHandle object2) {
        return object1.unhand() == object2.unhand();
    }

    @JNI_FUNCTION
    private static JniHandle NewLocalRef(Pointer env, JniHandle object) {
        return JniHandles.createLocalHandle(object.unhand());
    }

    @JNI_FUNCTION
    private static int EnsureLocalCapacity(Pointer env, int capacity) {
        // If this call fails, it will be with an OutOfMemoryError which will be
        // set as the pending exception for the current thread
        JniHandles.ensureLocalHandleCapacity(capacity);
        return JNI_OK;
    }

    private static Object allocObject(Class javaClass) throws InstantiationException {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        if (classActor.isTupleClassActor() && !classActor.isAbstract()) {
            return JniHandles.createLocalHandle(Heap.createTuple(classActor.dynamicHub()));
        }
        throw new InstantiationException();
    }

    @JNI_FUNCTION
    private static JniHandle AllocObject(Pointer env, JniHandle javaClass) throws InstantiationException {
        return JniHandles.createLocalHandle(allocObject((Class) javaClass.unhand()));
    }

    private static void replacedBySubstrate() {
        ProgramError.unexpected("a JNI function should have been replaced by a C function in the substrate");
    }

    /**
     * A Call to this method is delegated by the substrate to {@link #newObjectV(Pointer, JniHandle, MethodID, Pointer)}.
     */
    @JNI_FUNCTION
    private static JniHandle NewObject(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/) throws NoSuchMethodException, InstantiationException, IllegalAccessException,
                    InvocationTargetException {
        replacedBySubstrate();
        return JniHandle.zero();
    }

    @JNI_FUNCTION
    private static JniHandle NewObjectV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, InstantiationException, IllegalAccessException,
                    InvocationTargetException {
        replacedBySubstrate();
        return JniHandle.zero();
    }

    @JNI_FUNCTION
    private static JniHandle NewObjectA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, InstantiationException, IllegalAccessException,
                    InvocationTargetException {

        final ClassActor classActor = ClassActor.fromJava((Class) javaClass.unhand());
        if (!(classActor instanceof TupleClassActor)) {
            throw new NoSuchMethodException();
        }
        final TupleClassActor tupleClassActor = (TupleClassActor) classActor;

        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null || !methodActor.isInitializer()) {
            throw new NoSuchMethodException();
        }
        final VirtualMethodActor dynamicMethodActor = tupleClassActor.findLocalVirtualMethodActor(methodActor);
        if (dynamicMethodActor == null) {
            throw new NoSuchMethodException();
        }

        final SignatureDescriptor signature = dynamicMethodActor.descriptor();
        final Value[] argumentValues = new Value[signature.numberOfParameters()];
        copyJValueArrayToValueArray(arguments, signature, argumentValues, 0);
        traceReflectiveInvocation(dynamicMethodActor);
        return JniHandles.createLocalHandle(dynamicMethodActor.invokeConstructor(argumentValues).asObject());
    }

    @JNI_FUNCTION
    private static JniHandle GetObjectClass(Pointer env, JniHandle object) {
        final Class javaClass = object.unhand().getClass();
        return JniHandles.createLocalHandle(javaClass);
    }

    @JNI_FUNCTION
    private static boolean IsInstanceOf(Pointer env, JniHandle object, JniHandle javaType) {
        return ((Class) javaType.unhand()).isInstance(object.unhand());
    }

    @JNI_FUNCTION
    private static MethodID GetMethodID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        final ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
        MakeClassInitialized.makeClassInitialized(classActor);
        try {
            final Utf8Constant name = SymbolTable.lookupSymbol(CString.utf8ToJava(nameCString));
            final SignatureDescriptor descriptor = SignatureDescriptor.lookup(CString.utf8ToJava(descriptorCString));
            if (name == null || descriptor == null) {
                // The class should have been loaded (we have an instance of the class
                // passed in) so the name and signature should already be in their respective canonicalization
                // tables. If they're not there, the method doesn't exist.
                throw new NoSuchMethodError();
            }
            final MethodActor methodActor = classActor.findMethodActor(name, descriptor);
            if (methodActor == null || methodActor.isStatic()) {
                throw new NoSuchMethodError();
            }
            return MethodID.fromMethodActor(methodActor);
        } catch (Utf8Exception utf8Exception) {
            throw new NoSuchMethodError();
        }
    }

    @JNI_FUNCTION
    private static JniHandle CallObjectMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        replacedBySubstrate();
        return JniHandle.zero();
    }

    @JNI_FUNCTION
    private static JniHandle CallObjectMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer vaList) {
        replacedBySubstrate();
        return JniHandle.zero();
    }

    /**
     * Copies arguments from the native jvalue array at {@code arguments} into {@code argumentValues}. The number of
     * arguments copied is equal to {@code signature.getNumberOfParameters()}.
     *
     * @param signature describes the kind of each parameter
     * @param startIndex the index in {@code argumentValues} to start writing at
     */
    private static void copyJValueArrayToValueArray(Pointer arguments, SignatureDescriptor signature, Value[] argumentValues, int startIndex) {
        Pointer a = arguments;

        // This is equivalent to sizeof(jvalue) in C and gives us the size of each slot in a jvalue array.
        // Note that the size of the data in any given array element will be *at most* this size.
        final int jvalueSize = Kind.LONG.width.numberOfBytes;

        for (int i = 0; i < signature.numberOfParameters(); i++) {
            final int j = startIndex + i;
            switch (signature.parameterDescriptorAt(i).toKind().asEnum) {
                case BYTE: {
                    argumentValues[j] = ByteValue.from((byte) a.readInt(0));
                    break;
                }
                case BOOLEAN: {
                    argumentValues[j] = (a.readInt(0) != 0) ? BooleanValue.TRUE : BooleanValue.FALSE;
                    break;
                }
                case SHORT: {
                    argumentValues[j] = ShortValue.from((short) a.readInt(0));
                    break;
                }
                case CHAR: {
                    argumentValues[j] = CharValue.from((char) a.readInt(0));
                    break;
                }
                case INT: {
                    argumentValues[j] = IntValue.from(a.readInt(0));
                    break;
                }
                case FLOAT: {
                    argumentValues[j] = FloatValue.from(a.readFloat(0));
                    break;
                }
                case LONG: {
                    argumentValues[j] = LongValue.from(a.readLong(0));
                    break;
                }
                case DOUBLE: {
                    argumentValues[j] = DoubleValue.from(a.readDouble(0));
                    break;
                }
                case WORD: {
                    argumentValues[j] = new WordValue(a.readWord(0));
                    break;
                }
                case REFERENCE: {
                    final JniHandle jniHandle = a.readWord(0).asJniHandle();
                    argumentValues[j] = ReferenceValue.from(jniHandle.unhand());
                    break;
                }
                default: {
                    ProgramError.unexpected();
                }
            }
            a = a.plus(jvalueSize);
        }
    }

    private static Value CallValueMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null || methodActor.isStatic() || methodActor.isInitializer() || methodActor instanceof InterfaceMethodActor) {
            throw new NoSuchMethodException();
        }
        final VirtualMethodActor dynamicMethodActor = MethodSelectionSnippet.SelectVirtualMethod.quasiFold(object.unhand(), (VirtualMethodActor) methodActor);

        final SignatureDescriptor signature = dynamicMethodActor.descriptor();
        final Value[] argumentValues = new Value[1 + signature.numberOfParameters()];
        argumentValues[0] = ReferenceValue.from(object.unhand());
        copyJValueArrayToValueArray(arguments, signature, argumentValues, 1);
        traceReflectiveInvocation(dynamicMethodActor);
        return dynamicMethodActor.invoke(argumentValues);

    }

    @JNI_FUNCTION
    private static JniHandle CallObjectMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return JniHandles.createLocalHandle(CallValueMethodA(env, object, methodID, arguments).asObject());
    }

    @JNI_FUNCTION
    private static boolean CallBooleanMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        replacedBySubstrate();
        return false;
    }

    @JNI_FUNCTION
    private static boolean CallBooleanMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer vaList) {
        replacedBySubstrate();
        return false;
    }

    @JNI_FUNCTION
    private static boolean CallBooleanMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallValueMethodA(env, object, methodID, arguments).asBoolean();
    }

    @JNI_FUNCTION
    private static byte CallByteMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        replacedBySubstrate();
        return (byte) 0;
    }

    @JNI_FUNCTION
    private static byte CallByteMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return (byte) 0;
    }

    @JNI_FUNCTION
    private static byte CallByteMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallValueMethodA(env, object, methodID, arguments).asByte();
    }

    @JNI_FUNCTION
    private static char CallCharMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        replacedBySubstrate();
        return '\0';
    }

    @JNI_FUNCTION
    private static char CallCharMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return '\0';
    }

    @JNI_FUNCTION
    private static char CallCharMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallValueMethodA(env, object, methodID, arguments).asChar();
    }

    @JNI_FUNCTION
    private static short CallShortMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        replacedBySubstrate();
        return (short) 0;
    }

    @JNI_FUNCTION
    private static short CallShortMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return (short) 0;
    }

    @JNI_FUNCTION
    private static short CallShortMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallValueMethodA(env, object, methodID, arguments).asShort();
    }

    @JNI_FUNCTION
    private static int CallIntMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        replacedBySubstrate();
        return 0;
    }

    @JNI_FUNCTION
    private static int CallIntMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return 0;
    }

    @JNI_FUNCTION
    private static int CallIntMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallValueMethodA(env, object, methodID, arguments).asInt();
    }

    @JNI_FUNCTION
    private static long CallLongMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        replacedBySubstrate();
        return 0L;
    }

    @JNI_FUNCTION
    private static long CallLongMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return 0L;
    }

    @JNI_FUNCTION
    private static long CallLongMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallValueMethodA(env, object, methodID, arguments).asLong();
    }

    @JNI_FUNCTION
    private static float CallFloatMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        replacedBySubstrate();
        return (float) 0.0;
    }

    @JNI_FUNCTION
    private static float CallFloatMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return (float) 0.0;
    }

    @JNI_FUNCTION
    private static float CallFloatMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallValueMethodA(env, object, methodID, arguments).asFloat();
    }

    @JNI_FUNCTION
    private static double CallDoubleMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        replacedBySubstrate();
        return 0.0;
    }

    @JNI_FUNCTION
    private static double CallDoubleMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return 0.0;
    }

    @JNI_FUNCTION
    private static double CallDoubleMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallValueMethodA(env, object, methodID, arguments).asDouble();
    }

    private static Value CallNonvirtualValueMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        final ClassActor classActor = ClassActor.fromJava((Class) javaClass.unhand());
        if (!(classActor instanceof TupleClassActor)) {
            throw new NoSuchMethodException();
        }
        final TupleClassActor tupleClassActor = (TupleClassActor) classActor;

        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null || methodActor.isStatic() || methodActor.isInitializer() || methodActor instanceof InterfaceMethodActor) {
            throw new NoSuchMethodException();
        }
        final VirtualMethodActor dynamicMethodActor = tupleClassActor.findLocalVirtualMethodActor(methodActor);
        if (dynamicMethodActor == null) {
            throw new NoSuchMethodException();
        }

        final SignatureDescriptor signature = dynamicMethodActor.descriptor();
        final Value[] argumentValues = new Value[1 + signature.numberOfParameters()];
        argumentValues[0] = ReferenceValue.from(object.unhand());
        copyJValueArrayToValueArray(arguments, signature, argumentValues, 1);
        traceReflectiveInvocation(dynamicMethodActor);
        return dynamicMethodActor.invoke(argumentValues);
    }

    @JNI_FUNCTION
    private static void CallVoidMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        replacedBySubstrate();
    }

    @JNI_FUNCTION
    private static void CallVoidMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
    }

    @JNI_FUNCTION
    private static void CallVoidMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        CallValueMethodA(env, object, methodID, arguments);
    }

    @JNI_FUNCTION
    private static JniHandle CallNonvirtualObjectMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/) {
        replacedBySubstrate();
        return JniHandle.zero();
    }

    @JNI_FUNCTION
    private static JniHandle CallNonvirtualObjectMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments) {
        replacedBySubstrate();
        return JniHandle.zero();
    }

    @JNI_FUNCTION
    private static JniHandle CallNonvirtualObjectMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException,
                    IllegalAccessException, InvocationTargetException {
        return JniHandles.createLocalHandle(CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asObject());
    }

    @JNI_FUNCTION
    private static boolean CallNonvirtualBooleanMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/) {
        replacedBySubstrate();
        return false;
    }

    @JNI_FUNCTION
    private static boolean CallNonvirtualBooleanMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments) {
        replacedBySubstrate();
        return false;
    }

    @JNI_FUNCTION
    private static boolean CallNonvirtualBooleanMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asBoolean();
    }

    @JNI_FUNCTION
    private static byte CallNonvirtualByteMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/) {
        replacedBySubstrate();
        return (byte) 0;
    }

    @JNI_FUNCTION
    private static byte CallNonvirtualByteMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments) {
        replacedBySubstrate();
        return (byte) 0;
    }

    @JNI_FUNCTION
    private static byte CallNonvirtualByteMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asByte();
    }

    @JNI_FUNCTION
    private static char CallNonvirtualCharMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/) {
        replacedBySubstrate();
        return '\0';
    }

    @JNI_FUNCTION
    private static char CallNonvirtualCharMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments) {
        replacedBySubstrate();
        return '\0';
    }

    @JNI_FUNCTION
    private static char CallNonvirtualCharMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asChar();
    }

    @JNI_FUNCTION
    private static short CallNonvirtualShortMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/) {
        replacedBySubstrate();
        return (short) 0;
    }

    @JNI_FUNCTION
    private static short CallNonvirtualShortMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments) {
        replacedBySubstrate();
        return (short) 0;
    }

    @JNI_FUNCTION
    private static short CallNonvirtualShortMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asShort();
    }

    @JNI_FUNCTION
    private static int CallNonvirtualIntMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/) {
        replacedBySubstrate();
        return 0;
    }

    @JNI_FUNCTION
    private static int CallNonvirtualIntMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments) {
        replacedBySubstrate();
        return 0;
    }

    @JNI_FUNCTION
    private static int CallNonvirtualIntMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asInt();
    }

    @JNI_FUNCTION
    private static long CallNonvirtualLongMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/) {
        replacedBySubstrate();
        return 0L;
    }

    @JNI_FUNCTION
    private static long CallNonvirtualLongMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments) {
        replacedBySubstrate();
        return 0L;
    }

    @JNI_FUNCTION
    private static long CallNonvirtualLongMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asLong();
    }

    @JNI_FUNCTION
    private static float CallNonvirtualFloatMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/) {
        replacedBySubstrate();
        return (float) 0.0;
    }

    @JNI_FUNCTION
    private static float CallNonvirtualFloatMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments) {
        replacedBySubstrate();
        return (float) 0.0;
    }

    @JNI_FUNCTION
    private static float CallNonvirtualFloatMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asFloat();
    }

    @JNI_FUNCTION
    private static double CallNonvirtualDoubleMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/) {
        replacedBySubstrate();
        return 0.0;
    }

    @JNI_FUNCTION
    private static double CallNonvirtualDoubleMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments) {
        replacedBySubstrate();
        return 0.0;
    }

    @JNI_FUNCTION
    private static double CallNonvirtualDoubleMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asDouble();
    }

    @JNI_FUNCTION
    private static void CallNonvirtualVoidMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/) {
        replacedBySubstrate();
    }

    @JNI_FUNCTION
    private static void CallNonvirtualVoidMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments) {
        replacedBySubstrate();
    }

    @JNI_FUNCTION
    private static void CallNonvirtualVoidMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments);
    }

    @JNI_FUNCTION
    private static FieldID GetFieldID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        final ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
        MakeClassInitialized.makeClassInitialized(classActor);
        try {

            final Utf8Constant name = SymbolTable.lookupSymbol(CString.utf8ToJava(nameCString));
            final TypeDescriptor descriptor = JavaTypeDescriptor.parseTypeDescriptor(CString.utf8ToJava(descriptorCString));
            if (name == null || descriptor == null) {
                // The class should have been loaded (we have an instance of the class
                // passed in) so the name and signature should already be in their respective canonicalization
                // tables. If they're not there, the field doesn't exist.
                throw new NoSuchFieldError();
            }
            final FieldActor fieldActor = classActor.findInstanceFieldActor(name, descriptor);
            if (fieldActor == null) {
                throw new NoSuchFieldError();
            }
            return FieldID.fromFieldActor(fieldActor);
        } catch (Utf8Exception utf8Exception) {
            throw new NoSuchFieldError();
        }
    }

    @JNI_FUNCTION
    private static JniHandle GetObjectField(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor fieldActor = FieldID.toFieldActor(fieldID);
        return JniHandles.createLocalHandle(TupleAccess.readObject(object.unhand(), fieldActor.offset()));
    }

    @JNI_FUNCTION
    private static boolean GetBooleanField(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor booleanFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readBoolean(object.unhand(), booleanFieldActor.offset());
    }

    @JNI_FUNCTION
    private static byte GetByteField(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor byteFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readByte(object.unhand(), byteFieldActor.offset());
    }

    @JNI_FUNCTION
    private static char GetCharField(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor charFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readChar(object.unhand(), charFieldActor.offset());
    }

    @JNI_FUNCTION
    private static short GetShortField(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor shortFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readShort(object.unhand(), shortFieldActor.offset());
    }

    @JNI_FUNCTION
    private static int GetIntField(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor intFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readInt(object.unhand(), intFieldActor.offset());
    }

    @JNI_FUNCTION
    private static long GetLongField(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor longFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readLong(object.unhand(), longFieldActor.offset());
    }

    @JNI_FUNCTION
    private static float GetFloatField(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor floatFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readFloat(object.unhand(), floatFieldActor.offset());
    }

    @JNI_FUNCTION
    private static double GetDoubleField(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor doubleFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readDouble(object.unhand(), doubleFieldActor.offset());
    }

    @JNI_FUNCTION
    private static void SetObjectField(Pointer env, JniHandle object, FieldID fieldID, JniHandle value) {
        final FieldActor referenceFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeObject(object.unhand(), referenceFieldActor.offset(), value.unhand());
    }

    @JNI_FUNCTION
    private static void SetBooleanField(Pointer env, JniHandle object, FieldID fieldID, boolean value) {
        final FieldActor booleanFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeBoolean(object.unhand(), booleanFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetByteField(Pointer env, JniHandle object, FieldID fieldID, byte value) {
        final FieldActor byteFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeByte(object.unhand(), byteFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetCharField(Pointer env, JniHandle object, FieldID fieldID, char value) {
        final FieldActor charFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeChar(object.unhand(), charFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetShortField(Pointer env, JniHandle object, FieldID fieldID, short value) {
        final FieldActor shortFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeShort(object.unhand(), shortFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetIntField(Pointer env, JniHandle object, FieldID fieldID, int value) {
        final FieldActor intFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeInt(object.unhand(), intFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetLongField(Pointer env, JniHandle object, FieldID fieldID, long value) {
        final FieldActor longFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeLong(object.unhand(), longFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetFloatField(Pointer env, JniHandle object, FieldID fieldID, float value) {
        final FieldActor floatFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeFloat(object.unhand(), floatFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetDoubleField(Pointer env, JniHandle object, FieldID fieldID, double value) {
        final FieldActor doubleFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeDouble(object.unhand(), doubleFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static MethodID GetStaticMethodID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        final ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
        MakeClassInitialized.makeClassInitialized(classActor);
        try {
            final Utf8Constant name = SymbolTable.lookupSymbol(CString.utf8ToJava(nameCString));
            final SignatureDescriptor descriptor = SignatureDescriptor.create(CString.utf8ToJava(descriptorCString));
            if (name == null || descriptor == null) {
                // The class should have been loaded (we have an instance of the class
                // passed in) so the name and signature should already be in their respective canonicalization
                // tables. If they're not there, the method doesn't exist.
                throw new NoSuchMethodError();
            }
            final MethodActor methodActor = classActor.findStaticMethodActor(name, descriptor);
            if (methodActor == null) {
                throw new NoSuchMethodError();
            }
            return MethodID.fromMethodActor(methodActor);
        } catch (Utf8Exception utf8Exception) {
            throw new NoSuchMethodError();
        }
    }

    private static Value CallStaticValueMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final ClassActor classActor = ClassActor.fromJava((Class) javaClass.unhand());
        if (!(classActor instanceof TupleClassActor)) {
            throw new NoSuchMethodException();
        }

        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null || !methodActor.isStatic()) {
            throw new NoSuchMethodException();
        }
        if (!javaClass.isZero() && !methodActor.holder().toJava().isAssignableFrom((Class) javaClass.unhand())) {
            throw new NoSuchMethodException();
        }

        final SignatureDescriptor signature = methodActor.descriptor();
        final Value[] argumentValues = new Value[signature.numberOfParameters()];
        copyJValueArrayToValueArray(arguments, signature, argumentValues, 0);
        traceReflectiveInvocation(methodActor);
        return methodActor.invoke(argumentValues);
    }

    @JNI_FUNCTION
    private static JniHandle CallStaticObjectMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/) {
        replacedBySubstrate();
        return JniHandle.zero();
    }

    @JNI_FUNCTION
    private static JniHandle CallStaticObjectMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return JniHandle.zero();
    }

    @JNI_FUNCTION
    private static JniHandle CallStaticObjectMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        return JniHandles.createLocalHandle(CallStaticValueMethodA(env, javaClass, methodID, arguments).asObject());
    }

    @JNI_FUNCTION
    private static boolean CallStaticBooleanMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/) {
        replacedBySubstrate();
        return false;
    }

    @JNI_FUNCTION
    private static boolean CallStaticBooleanMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return false;
    }

    @JNI_FUNCTION
    private static boolean CallStaticBooleanMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asBoolean();
    }

    @JNI_FUNCTION
    private static byte CallStaticByteMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/) {
        replacedBySubstrate();
        return (byte) 0;
    }

    @JNI_FUNCTION
    private static byte CallStaticByteMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return (byte) 0;
    }

    @JNI_FUNCTION
    private static byte CallStaticByteMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asByte();
    }

    @JNI_FUNCTION
    private static char CallStaticCharMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/) {
        replacedBySubstrate();
        return '\0';
    }

    @JNI_FUNCTION
    private static char CallStaticCharMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return '\0';
    }

    @JNI_FUNCTION
    private static char CallStaticCharMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asChar();
    }

    @JNI_FUNCTION
    private static short CallStaticShortMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/) {
        replacedBySubstrate();
        return (short) 0;
    }

    @JNI_FUNCTION
    private static short CallStaticShortMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return (short) 0;
    }

    @JNI_FUNCTION
    private static short CallStaticShortMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asShort();
    }

    @JNI_FUNCTION
    private static int CallStaticIntMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/) {
        replacedBySubstrate();
        return 0;
    }

    @JNI_FUNCTION
    private static int CallStaticIntMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return 0;
    }

    @JNI_FUNCTION
    private static int CallStaticIntMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asInt();
    }

    @JNI_FUNCTION
    private static long CallStaticLongMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/) {
        replacedBySubstrate();
        return 0L;
    }

    @JNI_FUNCTION
    private static long CallStaticLongMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return 0L;
    }

    @JNI_FUNCTION
    private static long CallStaticLongMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asLong();
    }

    @JNI_FUNCTION
    private static float CallStaticFloatMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/) {
        replacedBySubstrate();
        return (float) 0.0;
    }

    @JNI_FUNCTION
    private static float CallStaticFloatMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return (float) 0.0;
    }

    @JNI_FUNCTION
    private static float CallStaticFloatMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asFloat();
    }

    @JNI_FUNCTION
    private static double CallStaticDoubleMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/) {
        replacedBySubstrate();
        return 0.0;
    }

    @JNI_FUNCTION
    private static double CallStaticDoubleMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
        return 0.0;
    }

    @JNI_FUNCTION
    private static double CallStaticDoubleMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asDouble();
    }

    @JNI_FUNCTION
    private static void CallStaticVoidMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/) {
        replacedBySubstrate();
    }

    @JNI_FUNCTION
    private static void CallStaticVoidMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) {
        replacedBySubstrate();
    }

    @JNI_FUNCTION
    private static void CallStaticVoidMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException {
        CallStaticValueMethodA(env, javaClass, methodID, arguments);
    }

    @JNI_FUNCTION
    private static FieldID GetStaticFieldID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        final ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
        MakeClassInitialized.makeClassInitialized(classActor);
        try {
            final Utf8Constant name = SymbolTable.lookupSymbol(CString.utf8ToJava(nameCString));
            final TypeDescriptor descriptor = TypeDescriptor.lookup(CString.utf8ToJava(descriptorCString));
            if (name == null || descriptor == null) {
                // The class should have been loaded (we have an instance of the class
                // passed in) so the name and signature should already be in their respective canonicalization
                // tables. If they're not there, the field doesn't exist.
                throw new NoSuchFieldError();
            }
            final FieldActor fieldActor = classActor.findStaticFieldActor(name, descriptor);
            if (fieldActor == null) {
                throw new NoSuchFieldError();
            }
            return FieldID.fromFieldActor(fieldActor);
        } catch (Utf8Exception utf8Exception) {
            throw new NoSuchFieldError();
        }
    }

    private static Object javaTypeToStaticTuple(JniHandle javaType) {
        final TupleClassActor tupleClassActor = (TupleClassActor) ClassActor.fromJava((Class) javaType.unhand());
        return tupleClassActor.staticTuple();
    }

    @JNI_FUNCTION
    private static JniHandle GetStaticObjectField(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor referenceFieldActor = FieldID.toFieldActor(fieldID);
        return JniHandles.createLocalHandle(TupleAccess.readObject(javaTypeToStaticTuple(javaType), referenceFieldActor.offset()));
    }

    @JNI_FUNCTION
    private static boolean GetStaticBooleanField(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor booleanFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readBoolean(javaTypeToStaticTuple(javaType), booleanFieldActor.offset());
    }

    @JNI_FUNCTION
    private static byte GetStaticByteField(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor byteFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readByte(javaTypeToStaticTuple(javaType), byteFieldActor.offset());
    }

    @JNI_FUNCTION
    private static char GetStaticCharField(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor charFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readChar(javaTypeToStaticTuple(javaType), charFieldActor.offset());
    }

    @JNI_FUNCTION
    private static short GetStaticShortField(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor shortFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readShort(javaTypeToStaticTuple(javaType), shortFieldActor.offset());
    }

    @JNI_FUNCTION
    private static int GetStaticIntField(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor intFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readInt(javaTypeToStaticTuple(javaType), intFieldActor.offset());
    }

    @JNI_FUNCTION
    private static long GetStaticLongField(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor longFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readLong(javaTypeToStaticTuple(javaType), longFieldActor.offset());
    }

    @JNI_FUNCTION
    private static float GetStaticFloatField(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor floatFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readFloat(javaTypeToStaticTuple(javaType), floatFieldActor.offset());
    }

    @JNI_FUNCTION
    private static double GetStaticDoubleField(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor doubleFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readDouble(javaTypeToStaticTuple(javaType), doubleFieldActor.offset());
    }

    @JNI_FUNCTION
    private static void SetStaticObjectField(Pointer env, JniHandle javaType, FieldID fieldID, JniHandle value) {
        final FieldActor referenceFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeObject(javaTypeToStaticTuple(javaType), referenceFieldActor.offset(), value.unhand());
    }

    @JNI_FUNCTION
    private static void SetStaticBooleanField(Pointer env, JniHandle javaType, FieldID fieldID, boolean value) {
        final FieldActor booleanFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeBoolean(javaTypeToStaticTuple(javaType), booleanFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticByteField(Pointer env, JniHandle javaType, FieldID fieldID, byte value) {
        final FieldActor byteFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeByte(javaTypeToStaticTuple(javaType), byteFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticCharField(Pointer env, JniHandle javaType, FieldID fieldID, char value) {
        final FieldActor charFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeChar(javaTypeToStaticTuple(javaType), charFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticShortField(Pointer env, JniHandle javaType, FieldID fieldID, short value) {
        final FieldActor shortFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeShort(javaTypeToStaticTuple(javaType), shortFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticIntField(Pointer env, JniHandle javaType, FieldID fieldID, int value) {
        final FieldActor intFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeInt(javaTypeToStaticTuple(javaType), intFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticLongField(Pointer env, JniHandle javaType, FieldID fieldID, long value) {
        final FieldActor longFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeLong(javaTypeToStaticTuple(javaType), longFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticFloatField(Pointer env, JniHandle javaType, FieldID fieldID, float value) {
        final FieldActor floatFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeFloat(javaTypeToStaticTuple(javaType), floatFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticDoubleField(Pointer env, JniHandle javaType, FieldID fieldID, double value) {
        final FieldActor doubleFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeDouble(javaTypeToStaticTuple(javaType), doubleFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static JniHandle NewString(Pointer env, Pointer chars, int length) {
        final char[] charArray = new char[length];
        for (int i = 0; i < length; i++) {
            charArray[i] = chars.getChar(i);
        }
        return JniHandles.createLocalHandle(new String(charArray));
    }

    @JNI_FUNCTION
    private static int GetStringLength(Pointer env, JniHandle string) {
        return ((String) string.unhand()).length();
    }

    @JNI_FUNCTION
    private static JniHandle GetStringChars(Pointer env, JniHandle string, Pointer isCopy) {
        setCopyPointer(isCopy, true);
        return JniHandles.createLocalHandle(((String) string.unhand()).toCharArray());
    }

    @JNI_FUNCTION
    private static void ReleaseStringChars(Pointer env, JniHandle string, Pointer chars) {
        Memory.deallocate(chars);
    }

    @JNI_FUNCTION
    private static JniHandle NewStringUTF(Pointer env, Pointer utf) {
        try {
            return JniHandles.createLocalHandle(CString.utf8ToJava(utf));
        } catch (Utf8Exception utf8Exception) {
            return null;
        }
    }

    @JNI_FUNCTION
    private static int GetStringUTFLength(Pointer env, JniHandle string) {
        return Utf8.utf8Length((String) string.unhand());
    }

    @JNI_FUNCTION
    private static Pointer GetStringUTFChars(Pointer env, JniHandle string, Pointer isCopy) {
        setCopyPointer(isCopy, true);
        return CString.utf8FromJava((String) string.unhand());
    }

    @JNI_FUNCTION
    private static void ReleaseStringUTFChars(Pointer env, JniHandle string, Pointer chars) {
        Memory.deallocate(chars);
    }

    @JNI_FUNCTION
    private static int GetArrayLength(Pointer env, JniHandle array) {
        return Array.getLength(array.unhand());
    }

    @JNI_FUNCTION
    private static JniHandle NewObjectArray(Pointer env, int length, JniHandle elementType, JniHandle initialElementValue) {
        final Object array = Array.newInstance((Class) elementType.unhand(), length);
        final Object initialValue = initialElementValue.unhand();
        for (int i = 0; i < length; i++) {
            Array.set(array, i, initialValue);
        }
        return JniHandles.createLocalHandle(array);
    }

    @JNI_FUNCTION
    private static JniHandle GetObjectArrayElement(Pointer env, JniHandle array, int index) {
        return JniHandles.createLocalHandle(((Object[]) array.unhand())[index]);
    }

    @JNI_FUNCTION
    private static void SetObjectArrayElement(Pointer env, JniHandle array, int index, JniHandle value) {
        ((Object[]) array.unhand())[index] = value.unhand();
    }

    @JNI_FUNCTION
    private static JniHandle NewBooleanArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new boolean[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewByteArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new byte[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewCharArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new char[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewShortArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new short[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewIntArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new int[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewLongArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new long[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewFloatArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new float[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewDoubleArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new double[length]);
    }

    @JNI_FUNCTION
    private static Pointer GetBooleanArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        return getBooleanArrayElements(array, isCopy);
    }

    private static Pointer getBooleanArrayElements(JniHandle array, Pointer isCopy) throws OutOfMemoryError {
        setCopyPointer(isCopy, true);
        final boolean[] a = (boolean[]) array.unhand();
        final Pointer pointer = Memory.mustAllocate(a.length);
        for (int i = 0; i < a.length; i++) {
            pointer.setBoolean(i, a[i]);
        }
        return pointer;
    }

    @JNI_FUNCTION
    private static Pointer GetByteArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        return getByteArrayElements(array, isCopy);
    }

    private static Pointer getByteArrayElements(JniHandle array, Pointer isCopy) throws OutOfMemoryError {
        setCopyPointer(isCopy, true);
        final byte[] a = (byte[]) array.unhand();
        final Pointer pointer = Memory.mustAllocate(a.length * Kind.BYTE.width.numberOfBytes);
        for (int i = 0; i < a.length; i++) {
            pointer.setByte(i, a[i]);
        }
        return pointer;
    }

    @JNI_FUNCTION
    private static Pointer GetCharArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        return getCharArrayElements(array, isCopy);
    }

    private static Pointer getCharArrayElements(JniHandle array, Pointer isCopy) throws OutOfMemoryError {
        setCopyPointer(isCopy, true);
        final char[] a = (char[]) array.unhand();
        final Pointer pointer = Memory.mustAllocate(a.length * Kind.CHAR.width.numberOfBytes);
        for (int i = 0; i < a.length; i++) {
            pointer.setChar(i, a[i]);
        }
        return pointer;
    }

    @JNI_FUNCTION
    private static Pointer GetShortArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        return getShortArrayElements(array, isCopy);
    }

    private static Pointer getShortArrayElements(JniHandle array, Pointer isCopy) throws OutOfMemoryError {
        setCopyPointer(isCopy, true);
        final short[] a = (short[]) array.unhand();
        final Pointer pointer = Memory.mustAllocate(a.length * Kind.SHORT.width.numberOfBytes);
        for (int i = 0; i < a.length; i++) {
            pointer.setShort(i, a[i]);
        }
        return pointer;
    }

    @JNI_FUNCTION
    private static Pointer GetIntArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        return getIntArrayElements(array, isCopy);
    }

    private static Pointer getIntArrayElements(JniHandle array, Pointer isCopy) throws OutOfMemoryError {
        setCopyPointer(isCopy, true);
        final int[] a = (int[]) array.unhand();
        final Pointer pointer = Memory.mustAllocate(a.length * Kind.INT.width.numberOfBytes);
        for (int i = 0; i < a.length; i++) {
            pointer.setInt(i, a[i]);
        }
        return pointer;
    }

    @JNI_FUNCTION
    private static Pointer GetLongArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        return getLongArrayElements(array, isCopy);
    }

    private static Pointer getLongArrayElements(JniHandle array, Pointer isCopy) throws OutOfMemoryError {
        setCopyPointer(isCopy, true);
        final long[] a = (long[]) array.unhand();
        final Pointer pointer = Memory.mustAllocate(a.length * Kind.LONG.width.numberOfBytes);
        for (int i = 0; i < a.length; i++) {
            pointer.setLong(i, a[i]);
        }
        return pointer;
    }

    @JNI_FUNCTION
    private static Pointer GetFloatArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        return getFloatArrayElements(array, isCopy);
    }

    private static Pointer getFloatArrayElements(JniHandle array, Pointer isCopy) throws OutOfMemoryError {
        setCopyPointer(isCopy, true);
        final float[] a = (float[]) array.unhand();
        final Pointer pointer = Memory.mustAllocate(a.length * Kind.FLOAT.width.numberOfBytes);
        for (int i = 0; i < a.length; i++) {
            pointer.setFloat(i, a[i]);
        }
        return pointer;
    }

    @JNI_FUNCTION
    private static Pointer GetDoubleArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        return getDoubleArrayElements(array, isCopy);
    }

    private static Pointer getDoubleArrayElements(JniHandle array, Pointer isCopy) throws OutOfMemoryError {
        setCopyPointer(isCopy, true);
        final double[] a = (double[]) array.unhand();
        final Pointer pointer = Memory.mustAllocate(a.length * Kind.DOUBLE.width.numberOfBytes);
        for (int i = 0; i < a.length; i++) {
            pointer.setDouble(i, a[i]);
        }
        return pointer;
    }

    @JNI_FUNCTION
    private static void ReleaseBooleanArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        releaseBooleanArrayElements(array, elements, mode);
    }

    private static void releaseBooleanArrayElements(JniHandle array, Pointer elements, int mode) {
        final boolean[] a = (boolean[]) array.unhand();
        if (mode == 0 || mode == JNI_COMMIT) {
            for (int i = 0; i < a.length; i++) {
                a[i] = elements.getBoolean(i);
            }
        }
        releaseElements(elements, mode);
    }

    @JNI_FUNCTION
    private static void ReleaseByteArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        releaseByteArrayElements(array, elements, mode);
    }

    private static void releaseByteArrayElements(JniHandle array, Pointer elements, int mode) {
        final byte[] a = (byte[]) array.unhand();
        if (mode == 0 || mode == JNI_COMMIT) {
            for (int i = 0; i < a.length; i++) {
                a[i] = elements.getByte(i);
            }
        }
        releaseElements(elements, mode);
    }

    @JNI_FUNCTION
    private static void ReleaseCharArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        releaseCharArrayElements(array, elements, mode);
    }

    private static void releaseCharArrayElements(JniHandle array, Pointer elements, int mode) {
        final char[] a = (char[]) array.unhand();
        if (mode == 0 || mode == JNI_COMMIT) {
            for (int i = 0; i < a.length; i++) {
                a[i] = elements.getChar(i);
            }
        }
        releaseElements(elements, mode);
    }

    @JNI_FUNCTION
    private static void ReleaseShortArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        releaseShortArrayElements(array, elements, mode);
    }

    private static void releaseShortArrayElements(JniHandle array, Pointer elements, int mode) {
        final short[] a = (short[]) array.unhand();
        if (mode == 0 || mode == JNI_COMMIT) {
            for (int i = 0; i < a.length; i++) {
                a[i] = elements.getShort(i);
            }
        }
        releaseElements(elements, mode);
    }

    @JNI_FUNCTION
    private static void ReleaseIntArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        releaseIntArrayElements(array, elements, mode);
    }

    private static void releaseIntArrayElements(JniHandle array, Pointer elements, int mode) {
        final int[] a = (int[]) array.unhand();
        if (mode == 0 || mode == JNI_COMMIT) {
            for (int i = 0; i < a.length; i++) {
                a[i] = elements.getInt(i);
            }
        }
        releaseElements(elements, mode);
    }

    @JNI_FUNCTION
    private static void ReleaseLongArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        releaseLongArrayElements(array, elements, mode);
    }

    private static void releaseLongArrayElements(JniHandle array, Pointer elements, int mode) {
        final long[] a = (long[]) array.unhand();
        if (mode == 0 || mode == JNI_COMMIT) {
            for (int i = 0; i < a.length; i++) {
                a[i] = elements.getLong(i);
            }
        }
        releaseElements(elements, mode);
    }

    @JNI_FUNCTION
    private static void ReleaseFloatArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        releaseFloatArrayElements(array, elements, mode);
    }

    private static void releaseFloatArrayElements(JniHandle array, Pointer elements, int mode) {
        final float[] a = (float[]) array.unhand();
        if (mode == 0 || mode == JNI_COMMIT) {
            for (int i = 0; i < a.length; i++) {
                a[i] = elements.getFloat(i);
            }
        }
        releaseElements(elements, mode);
    }

    @JNI_FUNCTION
    private static void ReleaseDoubleArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        releaseDoubleArrayElements(array, elements, mode);
    }

    private static void releaseDoubleArrayElements(JniHandle array, Pointer elements, int mode) {
        final double[] a = (double[]) array.unhand();
        if (mode == 0 || mode == JNI_COMMIT) {
            for (int i = 0; i < a.length; i++) {
                a[i] = elements.getDouble(i);
            }
        }
        releaseElements(elements, mode);
    }

    @JNI_FUNCTION
    private static void GetBooleanArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final boolean[] a = (boolean[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setBoolean(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetByteArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final byte[] a = (byte[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setByte(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetCharArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final char[] a = (char[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setChar(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetShortArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final short[] a = (short[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setShort(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetIntArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final int[] a = (int[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setInt(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetLongArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final long[] a = (long[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setLong(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetFloatArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final float[] a = (float[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setFloat(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetDoubleArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final double[] a = (double[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setDouble(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void SetBooleanArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final boolean[] a = (boolean[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getBoolean(i);
        }
    }

    @JNI_FUNCTION
    private static void SetByteArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final byte[] a = (byte[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getByte(i);
        }
    }

    @JNI_FUNCTION
    private static void SetCharArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final char[] a = (char[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getChar(i);
        }
    }

    @JNI_FUNCTION
    private static void SetShortArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final short[] a = (short[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getShort(i);
        }
    }

    @JNI_FUNCTION
    private static void SetIntArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final int[] a = (int[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getInt(i);
        }
    }

    @JNI_FUNCTION
    private static void SetLongArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final long[] a = (long[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getLong(i);
        }
    }

    @JNI_FUNCTION
    private static void SetFloatArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final float[] a = (float[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getFloat(i);
        }
    }

    @JNI_FUNCTION
    private static void SetDoubleArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final double[] a = (double[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getDouble(i);
        }
    }

    /**
     * Registers a set of native methods.
     *
     * For reference, this code expects the type of the {@code methods} parameter to be {@code JNINativeMethod *methods}
     * where:
     *
     * typedef struct { char *name; char *signature; void *fnPtr; } JNINativeMethod;
     */
    @JNI_FUNCTION
    private static int RegisterNatives(Pointer env, JniHandle javaType, Pointer methods, int numberOfMethods) {
        Pointer a = methods;

        final int pointerSize = Word.size();
        final int NAME = 0 * pointerSize;
        final int SIGNATURE = 1 * pointerSize;
        final int FNPTR = 2 * pointerSize;

        for (int i = 0; i < numberOfMethods; i++) {
            try {
                final Utf8Constant name = SymbolTable.lookupSymbol(CString.utf8ToJava(a.readWord(NAME).asPointer()));
                final SignatureDescriptor descriptor = SignatureDescriptor.lookup(CString.utf8ToJava(a.readWord(SIGNATURE).asPointer()));
                if (name == null || descriptor == null) {
                    // The class should have been loaded (we have an instance of the class
                    // passed in) so the name and signature should already be in their respective canonicalization
                    // tables. If they're not there, the method doesn't exist.
                    throw new NoSuchMethodError();
                }
                final Word fnPtr = a.readWord(FNPTR);

                final ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
                final ClassMethodActor classMethodActor = classActor.findClassMethodActor(name, descriptor);
                if (classMethodActor == null || !classMethodActor.isNative()) {
                    throw new NoSuchMethodError();
                }
                classMethodActor.nativeFunction.setAddress(fnPtr);

            } catch (Utf8Exception e) {
                throw new NoSuchMethodError();
            }

            // advance to next JNINativeMethod struct
            a = a.plus(pointerSize * 3);
        }
        return 0;
    }

    @JNI_FUNCTION
    private static int UnregisterNatives(Pointer env, JniHandle javaType) {
        final ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
        classActor.forAllClassMethodActors(new Procedure<ClassMethodActor>() {
            public void run(ClassMethodActor classMethodActor) {
                classMethodActor.nativeFunction.setAddress(Word.zero());
            }
        });
        return 0;
    }

    @JNI_FUNCTION
    private static int MonitorEnter(Pointer env, JniHandle object) {
        Monitor.enter(object.unhand());
        return 0;
    }

    @JNI_FUNCTION
    private static int MonitorExit(Pointer env, JniHandle object) {
        Monitor.exit(object.unhand());
        return 0;
    }

    @JNI_FUNCTION
    private static int GetJavaVM(Pointer env, Pointer vmPointerPointer) {
        FatalError.unexpected("Unimplemented: GetJavaVM()");
        return 0;
    }

    @JNI_FUNCTION
    private static void GetStringRegion(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        final String s = (String) string.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setChar(i, s.charAt(i + start));
        }
    }

    @JNI_FUNCTION
    private static void GetStringUTFRegion(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        final String s = ((String) string.unhand()).substring(start, start + length);
        final byte[] utf = Utf8.stringToUtf8(s);
        Memory.writeBytes(utf, utf.length, buffer);
        buffer.setByte(utf.length, (byte) 0); // zero termination
    }

    @JNI_FUNCTION
    private static Pointer GetPrimitiveArrayCritical(Pointer env, JniHandle array, Pointer isCopy) {
        final Object arrayObject = array.unhand();
        if (Heap.pin(arrayObject)) {
            setCopyPointer(isCopy, false);
            return Reference.fromJava(arrayObject).toOrigin().plus(Layout.byteArrayLayout().getElementOffsetFromOrigin(0));
        }
        if (arrayObject instanceof boolean[]) {
            return getBooleanArrayElements(array, isCopy);
        }
        if (arrayObject instanceof byte[]) {
            return getByteArrayElements(array, isCopy);
        }
        if (arrayObject instanceof char[]) {
            return getCharArrayElements(array, isCopy);
        }
        if (arrayObject instanceof short[]) {
            return getShortArrayElements(array, isCopy);
        }
        if (arrayObject instanceof int[]) {
            return getIntArrayElements(array, isCopy);
        }
        if (arrayObject instanceof long[]) {
            return getLongArrayElements(array, isCopy);
        }
        if (arrayObject instanceof float[]) {
            return getFloatArrayElements(array, isCopy);
        }
        if (arrayObject instanceof double[]) {
            return getDoubleArrayElements(array, isCopy);
        }
        return null;
    }

    @JNI_FUNCTION
    private static void ReleasePrimitiveArrayCritical(Pointer env, JniHandle array, Pointer elements, int mode) {
        final Object arrayObject = array.unhand();
        if (Heap.isPinned(arrayObject)) {
            Heap.unpin(arrayObject);
        } else {
            if (arrayObject instanceof boolean[]) {
                releaseBooleanArrayElements(array, elements, mode);
            }
            if (arrayObject instanceof byte[]) {
                releaseByteArrayElements(array, elements, mode);
            }
            if (arrayObject instanceof char[]) {
                releaseCharArrayElements(array, elements, mode);
            }
            if (arrayObject instanceof short[]) {
                releaseShortArrayElements(array, elements, mode);
            }
            if (arrayObject instanceof int[]) {
                releaseIntArrayElements(array, elements, mode);
            }
            if (arrayObject instanceof long[]) {
                releaseLongArrayElements(array, elements, mode);
            }
            if (arrayObject instanceof float[]) {
                releaseFloatArrayElements(array, elements, mode);
            }
            if (arrayObject instanceof double[]) {
                releaseDoubleArrayElements(array, elements, mode);
            }
        }
    }

    @JNI_FUNCTION
    private static Pointer GetStringCritical(Pointer env, JniHandle string, Pointer isCopy) {
        FatalError.unexpected("GetStringCritical is unimplemented");
        return null;
    }

    @JNI_FUNCTION
    private static void ReleaseStringCritical(Pointer env, JniHandle string, final Pointer cString) {
        FatalError.unexpected("ReleaseStringCritical is unimplemented");
    }

    @JNI_FUNCTION
    private static JniHandle NewWeakGlobalRef(Pointer env, JniHandle handle) {
        return JniHandles.createWeakGlobalHandle(handle.unhand());
    }

    @JNI_FUNCTION
    private static void DeleteWeakGlobalRef(Pointer env, JniHandle handle) {
        JniHandles.destroyWeakGlobalHandle(handle);
    }

    @JNI_FUNCTION
    private static boolean ExceptionCheck(Pointer env) {
        return VmThread.fromJniEnv(env).pendingException() != null;
    }

    @JNI_FUNCTION
    private static JniHandle NewDirectByteBuffer(Pointer env, Pointer address, long capacity) {
        FatalError.unimplemented();
        return null;
    }

    @JNI_FUNCTION
    private static Pointer GetDirectBufferAddress(Pointer env, JniHandle buffer) {
        FatalError.unimplemented();
        return null;
    }

    @JNI_FUNCTION
    private static long GetDirectBufferCapacity(Pointer env, JniHandle buffer) {
        FatalError.unimplemented();
        return 0L;
    }

    @JNI_FUNCTION
    private static int GetObjectRefType(Pointer env, JniHandle obj) {
        final int tag = JniHandles.tag(obj);
        if (tag == JniHandles.Tag.STACK) {
            return JniHandles.Tag.LOCAL;
        }
        return tag;
    }



    /*
     * Extended JNI native interface, see Native/jni/jni.c:
     */

    @JNI_FUNCTION
    private static int GetNumberOfArguments(Pointer env, MethodID methodID) throws NoSuchMethodException {
        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null) {
            throw new NoSuchMethodException();
        }
        return methodActor.descriptor().numberOfParameters();
    }

    @JNI_FUNCTION
    private static void GetKindsOfArguments(Pointer env, MethodID methodID, Pointer kinds) throws NoSuchMethodException {
        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null) {
            throw new NoSuchMethodException();
        }
        final SignatureDescriptor signature = methodActor.descriptor();
        for (int i = 0; i < signature.numberOfParameters(); ++i) {
            final Kind kind = signature.parameterDescriptorAt(i).toKind();
            kinds.setByte(i, (byte) kind.asEnum.ordinal());
        }
    }

    // Checkstyle: resume method name check

    private static void setCopyPointer(Pointer isCopy, boolean bool) {
        if (!isCopy.isZero()) {
            isCopy.setBoolean(bool);
        }
    }

    private static void releaseElements(Pointer elements, int mode) {
        if (mode == 0 || mode == JNI_ABORT) {
            Memory.deallocate(elements);
        }
        assert mode == 0 || mode == JNI_COMMIT || mode == JNI_ABORT;
    }

}
