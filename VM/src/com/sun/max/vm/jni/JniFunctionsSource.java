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
package com.sun.max.vm.jni;

import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.jni.JniFunctions.*;

import java.lang.reflect.*;
import java.nio.*;

import com.sun.max.*;
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
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Template from which (parts of) {@link JniFunctions} is generated. The static initializer of
 * {@link JniFunctions} includes a call to {@link JniFunctionsGenerator#generate(boolean, Class, Class)}
 * to double-check that the source is up-to-date with respect to any edits made to this class.
 *
 * All the methods annotated by {@link VM_ENTRY_POINT} appear in the exact same order as specified in
 * jni.h. In addition, any methods annotated by {@link VM_ENTRY_POINT} that are declared
 * {@code native} have implementations in jni.c and their entries in the JNI function table
 * are initialized in jni.c.
 *
 * @author Doug Simon
 */
@HOSTED_ONLY
public final class JniFunctionsSource {

    private JniFunctionsSource() {
    }

    @VM_ENTRY_POINT
    private static native void reserved0();

    @VM_ENTRY_POINT
    private static native void reserved1();

    @VM_ENTRY_POINT
    private static native void reserved2();

    @VM_ENTRY_POINT
    private static native void reserved3();

    // Checkstyle: stop method name check

    @VM_ENTRY_POINT
    private static native int GetVersion(Pointer env);

    private static String dottify(String slashifiedName) {
        return slashifiedName.replace('/', '.');
    }

    private static void traceReflectiveInvocation(MethodActor methodActor) {
        if (ClassMethodActor.TraceJNI) {
            Log.print("[Thread \"");
            Log.print(VmThread.current().getName());
            Log.print("\" --> JNI invoke: ");
            Log.println(methodActor.format("%H.%n(%p)"));
        }
    }

    private static final Class[] defineClassParameterTypes = {String.class, byte[].class, int.class, int.class};

    @VM_ENTRY_POINT
    private static JniHandle DefineClass(Pointer env, Pointer slashifiedName, JniHandle classLoader, Pointer buffer, int length) throws ClassFormatError {
        final byte[] bytes = new byte[length];
        Memory.readBytes(buffer, length, bytes);
        try {
            // TODO: find out whether already dottified class names should be rejected by this function
            final Object[] arguments = {dottify(CString.utf8ToJava(slashifiedName)), bytes, 0, length};
            Object cl = classLoader.unhand();
            if (cl == null) {
                cl = BootClassLoader.BOOT_CLASS_LOADER;
            }
            return JniHandles.createLocalHandle(WithoutAccessCheck.invokeVirtual(cl, "defineClass", defineClassParameterTypes, arguments));
        } catch (Utf8Exception utf8Exception) {
            throw classFormatError("Invalid class name");
        } catch (NoSuchMethodException noSuchMethodException) {
            throw ProgramError.unexpected(noSuchMethodException);
        } catch (IllegalAccessException illegalAccessException) {
            throw ProgramError.unexpected(illegalAccessException);
        } catch (InvocationTargetException invocationTargetException) {
            VmThread.fromJniEnv(env).setJniException(invocationTargetException.getTargetException());
            return JniHandle.zero();
        }
    }

    private static Class findClass(ClassLoader classLoader, String slashifiedName) throws ClassNotFoundException {
        if (slashifiedName.startsWith("[")) {
            TypeDescriptor descriptor = JavaTypeDescriptor.parseTypeDescriptor(slashifiedName);
            return descriptor.resolve(classLoader).toJava();
        }
        return classLoader.loadClass(dottify(slashifiedName));
    }

    @VM_ENTRY_POINT
    private static JniHandle FindClass(Pointer env, Pointer name) throws ClassNotFoundException {
        String className;
        try {
            className = CString.utf8ToJava(name);
        } catch (Utf8Exception utf8Exception) {
            throw new ClassNotFoundException();
        }
        // Skip frames for 'getCallerClass' and 'FindClass'
        int realFramesToSkip = 2;
        ClassActor caller = ClassActor.fromJava(JDK_sun_reflect_Reflection.getCallerClass(realFramesToSkip));
        ClassLoader classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.classLoader;
        final Class javaClass = findClass(classLoader, className);
        Snippets.makeClassInitialized(ClassActor.fromJava(javaClass));
        return JniHandles.createLocalHandle(javaClass);
    }

    @VM_ENTRY_POINT
    private static MethodID FromReflectedMethod(Pointer env, JniHandle reflectedMethod) {
        final MethodActor methodActor = MethodActor.fromJava((Method) reflectedMethod.unhand());
        return MethodID.fromMethodActor(methodActor);
    }

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
    private static JniHandle ToReflectedMethod(Pointer env, JniHandle javaClass, MethodID methodID, boolean isStatic) throws NoSuchMethodException {
        return JniHandles.createLocalHandle(ToReflectedMethod(methodID, isStatic));
    }

    @VM_ENTRY_POINT
    private static JniHandle GetSuperclass(Pointer env, JniHandle subType) {
        return JniHandles.createLocalHandle(((Class) subType.unhand()).getSuperclass());
    }

    @VM_ENTRY_POINT
    private static boolean IsAssignableFrom(Pointer env, JniHandle subType, JniHandle superType) {
        return ClassActor.fromJava((Class) superType.unhand()).isAssignableFrom(ClassActor.fromJava((Class) subType.unhand()));
    }

    @VM_ENTRY_POINT
    private static JniHandle ToReflectedField(Pointer env, JniHandle javaClass, FieldID fieldID, boolean isStatic) {
        final FieldActor fieldActor = FieldID.toFieldActor(fieldID);
        if (fieldActor == null || fieldActor.isStatic() != isStatic) {
            throw new NoSuchFieldError();
        }
        return JniHandles.createLocalHandle(fieldActor.toJava());
    }

    @VM_ENTRY_POINT
    private static int Throw(Pointer env, JniHandle throwable) {
        VmThread.fromJniEnv(env).setJniException((Throwable) throwable.unhand());
        return JNI_OK;
    }

    @VM_ENTRY_POINT
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
        constructor = Utils.cast(type, throwableClass.unhand()).getConstructor(parameterTypes);
        Throwable throwable = message.isZero() ? constructor.newInstance() : constructor.newInstance(CString.utf8ToJava(message));
        VmThread.fromJniEnv(env).setJniException(throwable);
        return JNI_OK;
    }

    @VM_ENTRY_POINT
    private static JniHandle ExceptionOccurred(Pointer env) {
        return JniHandles.createLocalHandle(VmThread.fromJniEnv(env).jniException());
    }

    @VM_ENTRY_POINT
    private static void ExceptionDescribe(Pointer env) {
        final Throwable exception = VmThread.fromJniEnv(env).jniException();
        if (exception != null) {
            exception.printStackTrace();
        }
    }

    @VM_ENTRY_POINT
    private static void ExceptionClear(Pointer env) {
        VmThread.fromJniEnv(env).setJniException(null);
    }

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
    private static int PushLocalFrame(Pointer env, int capacity) {
        JniHandles.pushLocalFrame(capacity);
        return JNI_OK;
    }

    @VM_ENTRY_POINT
    private static JniHandle PopLocalFrame(Pointer env, JniHandle res) {
        return JniHandles.popLocalFrame(res);
    }

    @VM_ENTRY_POINT
    private static JniHandle NewGlobalRef(Pointer env, JniHandle handle) {
        return JniHandles.createGlobalHandle(handle.unhand());
    }

    @VM_ENTRY_POINT
    private static void DeleteGlobalRef(Pointer env, JniHandle handle) {
        JniHandles.destroyGlobalHandle(handle);
    }

    @VM_ENTRY_POINT
    private static void DeleteLocalRef(Pointer env, JniHandle handle) {
        JniHandles.destroyLocalHandle(handle);
    }

    @VM_ENTRY_POINT
    private static boolean IsSameObject(Pointer env, JniHandle object1, JniHandle object2) {
        return object1.unhand() == object2.unhand();
    }

    @VM_ENTRY_POINT
    private static JniHandle NewLocalRef(Pointer env, JniHandle object) {
        return JniHandles.createLocalHandle(object.unhand());
    }

    @VM_ENTRY_POINT
    private static int EnsureLocalCapacity(Pointer env, int capacity) {
        // If this call fails, it will be with an OutOfMemoryError which will be
        // set as the pending exception for the current thread
        JniHandles.ensureLocalHandleCapacity(capacity);
        return JNI_OK;
    }

    private static Object allocObject(Class javaClass) throws InstantiationException {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        if (classActor.isTupleClass() && !classActor.isAbstract()) {
            return Heap.createTuple(classActor.dynamicHub());
        }
        throw new InstantiationException();
    }

    @VM_ENTRY_POINT
    private static JniHandle AllocObject(Pointer env, JniHandle javaClass) throws InstantiationException {
        return JniHandles.createLocalHandle(allocObject((Class) javaClass.unhand()));
    }

    @VM_ENTRY_POINT
    private static native JniHandle NewObject(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native JniHandle NewObjectV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static JniHandle NewObjectA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {

        final ClassActor classActor = ClassActor.fromJava((Class) javaClass.unhand());
        if (!(classActor instanceof TupleClassActor)) {
            throw new NoSuchMethodException();
        }
        final TupleClassActor tupleClassActor = (TupleClassActor) classActor;

        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null || !methodActor.isInitializer()) {
            throw new NoSuchMethodException();
        }
        final VirtualMethodActor virtualMethodActor = tupleClassActor.findLocalVirtualMethodActor(methodActor.name, methodActor.descriptor());
        if (virtualMethodActor == null) {
            throw new NoSuchMethodException();
        }

        final SignatureDescriptor signature = virtualMethodActor.descriptor();
        final Value[] argumentValues = new Value[signature.numberOfParameters()];
        copyJValueArrayToValueArray(arguments, signature, argumentValues, 0);
        traceReflectiveInvocation(virtualMethodActor);
        return JniHandles.createLocalHandle(virtualMethodActor.invokeConstructor(argumentValues).asObject());
    }

    @VM_ENTRY_POINT
    private static JniHandle GetObjectClass(Pointer env, JniHandle object) {
        final Class javaClass = object.unhand().getClass();
        return JniHandles.createLocalHandle(javaClass);
    }

    @VM_ENTRY_POINT
    private static boolean IsInstanceOf(Pointer env, JniHandle object, JniHandle javaType) {
        return ((Class) javaType.unhand()).isInstance(object.unhand());
    }

    @VM_ENTRY_POINT
    private static MethodID GetMethodID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        final ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
        Snippets.makeClassInitialized(classActor);
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

    @VM_ENTRY_POINT
    private static native JniHandle CallObjectMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native JniHandle CallObjectMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer vaList);

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

    private static Value CallValueMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null) {
            throw new NoSuchMethodException("Invalid method ID " + methodID.asAddress().toLong());
        }
        if (methodActor.isStatic()) {
            throw new NoSuchMethodException(methodActor.toString() + " is static");
        }
        if (methodActor.isInitializer()) {
            throw new NoSuchMethodException(methodActor.toString() + " is an initializer");
        }
        final MethodActor selectedMethod;
        Object receiver = object.unhand();
        ClassActor holder = ObjectAccess.readClassActor(receiver);

        if (!methodActor.holder().isAssignableFrom(holder)) {
            throw new NoSuchMethodException(holder + " is not an instance of " + methodActor.holder());
        }
        selectedMethod = (MethodActor) holder.resolveMethodImpl(methodActor);
        final SignatureDescriptor signature = selectedMethod.descriptor();
        final Value[] argumentValues = new Value[1 + signature.numberOfParameters()];
        argumentValues[0] = ReferenceValue.from(object.unhand());
        copyJValueArrayToValueArray(arguments, signature, argumentValues, 1);
        traceReflectiveInvocation(selectedMethod);
        return selectedMethod.invoke(argumentValues);

    }

    @VM_ENTRY_POINT
    private static JniHandle CallObjectMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return JniHandles.createLocalHandle(CallValueMethodA(env, object, methodID, arguments).asObject());
    }

    @VM_ENTRY_POINT
    private static native boolean CallBooleanMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native boolean CallBooleanMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer vaList);

    @VM_ENTRY_POINT
    private static boolean CallBooleanMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asBoolean();
    }

    @VM_ENTRY_POINT
    private static native byte CallByteMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native byte CallByteMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static byte CallByteMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asByte();
    }

    @VM_ENTRY_POINT
    private static native char CallCharMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native char CallCharMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static char CallCharMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asChar();
    }

    @VM_ENTRY_POINT
    private static native short CallShortMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native short CallShortMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static short CallShortMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asShort();
    }

    @VM_ENTRY_POINT
    private static native int CallIntMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native int CallIntMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static int CallIntMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asInt();
    }

    @VM_ENTRY_POINT
    private static native long CallLongMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native long CallLongMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static long CallLongMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asLong();
    }

    @VM_ENTRY_POINT
    private static native float CallFloatMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native float CallFloatMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static float CallFloatMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asFloat();
    }

    @VM_ENTRY_POINT
    private static native double CallDoubleMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native double CallDoubleMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static double CallDoubleMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asDouble();
    }

    private static Value CallNonvirtualValueMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        final ClassActor classActor = ClassActor.fromJava((Class) javaClass.unhand());
        if (!(classActor instanceof TupleClassActor)) {
            throw new NoSuchMethodException();
        }
        final TupleClassActor tupleClassActor = (TupleClassActor) classActor;

        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null || methodActor.isStatic() || methodActor.isInitializer() || methodActor instanceof InterfaceMethodActor) {
            throw new NoSuchMethodException();
        }
        final VirtualMethodActor virtualMethodActor = tupleClassActor.findLocalVirtualMethodActor(methodActor.name, methodActor.descriptor());
        if (virtualMethodActor == null) {
            throw new NoSuchMethodException();
        }

        final SignatureDescriptor signature = virtualMethodActor.descriptor();
        final Value[] argumentValues = new Value[1 + signature.numberOfParameters()];
        argumentValues[0] = ReferenceValue.from(object.unhand());
        copyJValueArrayToValueArray(arguments, signature, argumentValues, 1);
        traceReflectiveInvocation(virtualMethodActor);
        return virtualMethodActor.invoke(argumentValues);
    }

    @VM_ENTRY_POINT
    private static native void CallVoidMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native void CallVoidMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static void CallVoidMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        CallValueMethodA(env, object, methodID, arguments);
    }

    @VM_ENTRY_POINT
    private static native JniHandle CallNonvirtualObjectMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @VM_ENTRY_POINT
    private static native JniHandle CallNonvirtualObjectMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @VM_ENTRY_POINT
    private static JniHandle CallNonvirtualObjectMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return JniHandles.createLocalHandle(CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asObject());
    }

    @VM_ENTRY_POINT
    private static native boolean CallNonvirtualBooleanMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @VM_ENTRY_POINT
    private static native boolean CallNonvirtualBooleanMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @VM_ENTRY_POINT
    private static boolean CallNonvirtualBooleanMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asBoolean();
    }

    @VM_ENTRY_POINT
    private static native byte CallNonvirtualByteMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @VM_ENTRY_POINT
    private static native byte CallNonvirtualByteMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @VM_ENTRY_POINT
    private static byte CallNonvirtualByteMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asByte();
    }

    @VM_ENTRY_POINT
    private static native char CallNonvirtualCharMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @VM_ENTRY_POINT
    private static native char CallNonvirtualCharMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @VM_ENTRY_POINT
    private static char CallNonvirtualCharMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asChar();
    }

    @VM_ENTRY_POINT
    private static native short CallNonvirtualShortMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @VM_ENTRY_POINT
    private static native short CallNonvirtualShortMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @VM_ENTRY_POINT
    private static short CallNonvirtualShortMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asShort();
    }

    @VM_ENTRY_POINT
    private static native int CallNonvirtualIntMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @VM_ENTRY_POINT
    private static native int CallNonvirtualIntMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @VM_ENTRY_POINT
    private static int CallNonvirtualIntMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asInt();
    }

    @VM_ENTRY_POINT
    private static native long CallNonvirtualLongMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @VM_ENTRY_POINT
    private static native long CallNonvirtualLongMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @VM_ENTRY_POINT
    private static long CallNonvirtualLongMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asLong();
    }

    @VM_ENTRY_POINT
    private static native float CallNonvirtualFloatMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @VM_ENTRY_POINT
    private static native float CallNonvirtualFloatMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @VM_ENTRY_POINT
    private static float CallNonvirtualFloatMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asFloat();
    }

    @VM_ENTRY_POINT
    private static native double CallNonvirtualDoubleMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @VM_ENTRY_POINT
    private static native double CallNonvirtualDoubleMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @VM_ENTRY_POINT
    private static double CallNonvirtualDoubleMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asDouble();
    }

    @VM_ENTRY_POINT
    private static native void CallNonvirtualVoidMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @VM_ENTRY_POINT
    private static native void CallNonvirtualVoidMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @VM_ENTRY_POINT
    private static void CallNonvirtualVoidMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments);
    }

    @VM_ENTRY_POINT
    private static FieldID GetFieldID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        final ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
        Snippets.makeClassInitialized(classActor);
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
                throw new NoSuchFieldError(name.string);
            }
            return FieldID.fromFieldActor(fieldActor);
        } catch (Utf8Exception utf8Exception) {
            throw new NoSuchFieldError();
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetObjectField(Pointer env, JniHandle object, FieldID fieldID) {
        return JniHandles.createLocalHandle(FieldID.toFieldActor(fieldID).getObject(object.unhand()));
    }

    @VM_ENTRY_POINT
    private static boolean GetBooleanField(Pointer env, JniHandle object, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getBoolean(object.unhand());
    }

    @VM_ENTRY_POINT
    private static byte GetByteField(Pointer env, JniHandle object, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getByte(object.unhand());
    }

    @VM_ENTRY_POINT
    private static char GetCharField(Pointer env, JniHandle object, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getChar(object.unhand());
    }

    @VM_ENTRY_POINT
    private static short GetShortField(Pointer env, JniHandle object, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getShort(object.unhand());
    }

    @VM_ENTRY_POINT
    private static int GetIntField(Pointer env, JniHandle object, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getInt(object.unhand());
    }

    @VM_ENTRY_POINT
    private static long GetLongField(Pointer env, JniHandle object, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getLong(object.unhand());
    }

    @VM_ENTRY_POINT
    private static float GetFloatField(Pointer env, JniHandle object, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getFloat(object.unhand());
    }

    @VM_ENTRY_POINT
    private static double GetDoubleField(Pointer env, JniHandle object, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getDouble(object.unhand());
    }

    @VM_ENTRY_POINT
    private static void SetObjectField(Pointer env, JniHandle object, FieldID fieldID, JniHandle value) {
        FieldID.toFieldActor(fieldID).setObject(object.unhand(), value.unhand());
    }

    @VM_ENTRY_POINT
    private static void SetBooleanField(Pointer env, JniHandle object, FieldID fieldID, boolean value) {
        FieldID.toFieldActor(fieldID).setBoolean(object.unhand(), value);
    }

    @VM_ENTRY_POINT
    private static void SetByteField(Pointer env, JniHandle object, FieldID fieldID, byte value) {
        FieldID.toFieldActor(fieldID).setByte(object.unhand(), value);
    }

    @VM_ENTRY_POINT
    private static void SetCharField(Pointer env, JniHandle object, FieldID fieldID, char value) {
        FieldID.toFieldActor(fieldID).setChar(object.unhand(), value);
    }

    @VM_ENTRY_POINT
    private static void SetShortField(Pointer env, JniHandle object, FieldID fieldID, short value) {
        FieldID.toFieldActor(fieldID).setShort(object.unhand(), value);
    }

    @VM_ENTRY_POINT
    private static void SetIntField(Pointer env, JniHandle object, FieldID fieldID, int value) {
        FieldID.toFieldActor(fieldID).setInt(object.unhand(), value);
    }

    @VM_ENTRY_POINT
    private static void SetLongField(Pointer env, JniHandle object, FieldID fieldID, long value) {
        FieldID.toFieldActor(fieldID).setLong(object.unhand(), value);
    }

    @VM_ENTRY_POINT
    private static void SetFloatField(Pointer env, JniHandle object, FieldID fieldID, float value) {
        FieldID.toFieldActor(fieldID).setFloat(object.unhand(), value);
    }

    @VM_ENTRY_POINT
    private static void SetDoubleField(Pointer env, JniHandle object, FieldID fieldID, double value) {
        FieldID.toFieldActor(fieldID).setDouble(object.unhand(), value);
    }

    @VM_ENTRY_POINT
    private static MethodID GetStaticMethodID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        final ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
        Snippets.makeClassInitialized(classActor);
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
                throw new NoSuchMethodError(classActor + "." + name.string);
            }
            return MethodID.fromMethodActor(methodActor);
        } catch (Utf8Exception utf8Exception) {
            throw new NoSuchMethodError();
        }
    }

    private static Value CallStaticValueMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        final ClassActor classActor = ClassActor.fromJava((Class) javaClass.unhand());
        if (!(classActor instanceof TupleClassActor)) {
            throw new NoSuchMethodException(classActor + " is not a class with static methods");
        }

        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null) {
            throw new NoSuchMethodException("Invalid method ID " + methodID.asAddress().toLong());
        }
        if (!methodActor.isStatic()) {
            throw new NoSuchMethodException(methodActor + " is not static");
        }
        if (!javaClass.isZero() && !methodActor.holder().toJava().isAssignableFrom((Class) javaClass.unhand())) {
            throw new NoSuchMethodException(javaClass.unhand() + " is not a subclass of " + methodActor.holder());
        }

        final SignatureDescriptor signature = methodActor.descriptor();
        final Value[] argumentValues = new Value[signature.numberOfParameters()];
        copyJValueArrayToValueArray(arguments, signature, argumentValues, 0);
        traceReflectiveInvocation(methodActor);
        return methodActor.invoke(argumentValues);
    }

    @VM_ENTRY_POINT
    private static native JniHandle CallStaticObjectMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native JniHandle CallStaticObjectMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static JniHandle CallStaticObjectMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return JniHandles.createLocalHandle(CallStaticValueMethodA(env, javaClass, methodID, arguments).asObject());
    }

    @VM_ENTRY_POINT
    private static native boolean CallStaticBooleanMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native boolean CallStaticBooleanMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static boolean CallStaticBooleanMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asBoolean();
    }

    @VM_ENTRY_POINT
    private static native byte CallStaticByteMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native byte CallStaticByteMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static byte CallStaticByteMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asByte();
    }

    @VM_ENTRY_POINT
    private static native char CallStaticCharMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native char CallStaticCharMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static char CallStaticCharMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asChar();
    }

    @VM_ENTRY_POINT
    private static native short CallStaticShortMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native short CallStaticShortMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static short CallStaticShortMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asShort();
    }

    @VM_ENTRY_POINT
    private static native int CallStaticIntMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native int CallStaticIntMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static int CallStaticIntMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asInt();
    }

    @VM_ENTRY_POINT
    private static native long CallStaticLongMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native long CallStaticLongMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static long CallStaticLongMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asLong();
    }

    @VM_ENTRY_POINT
    private static native float CallStaticFloatMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native float CallStaticFloatMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static float CallStaticFloatMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asFloat();
    }

    @VM_ENTRY_POINT
    private static native double CallStaticDoubleMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native double CallStaticDoubleMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static double CallStaticDoubleMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asDouble();
    }

    @VM_ENTRY_POINT
    private static native void CallStaticVoidMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @VM_ENTRY_POINT
    private static native void CallStaticVoidMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @VM_ENTRY_POINT
    private static void CallStaticVoidMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        CallStaticValueMethodA(env, javaClass, methodID, arguments);
    }

    @VM_ENTRY_POINT
    private static FieldID GetStaticFieldID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        final ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
        Snippets.makeClassInitialized(classActor);
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

    @VM_ENTRY_POINT
    private static JniHandle GetStaticObjectField(Pointer env, JniHandle javaType, FieldID fieldID) {
        return JniHandles.createLocalHandle(FieldID.toFieldActor(fieldID).getObject(null));
    }

    @VM_ENTRY_POINT
    private static boolean GetStaticBooleanField(Pointer env, JniHandle javaType, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getBoolean(null);
    }

    @VM_ENTRY_POINT
    private static byte GetStaticByteField(Pointer env, JniHandle javaType, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getByte(null);
    }

    @VM_ENTRY_POINT
    private static char GetStaticCharField(Pointer env, JniHandle javaType, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getChar(null);
    }

    @VM_ENTRY_POINT
    private static short GetStaticShortField(Pointer env, JniHandle javaType, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getShort(null);
    }

    @VM_ENTRY_POINT
    private static int GetStaticIntField(Pointer env, JniHandle javaType, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getInt(null);
    }

    @VM_ENTRY_POINT
    private static long GetStaticLongField(Pointer env, JniHandle javaType, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getLong(null);
    }

    @VM_ENTRY_POINT
    private static float GetStaticFloatField(Pointer env, JniHandle javaType, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getFloat(null);
    }

    @VM_ENTRY_POINT
    private static double GetStaticDoubleField(Pointer env, JniHandle javaType, FieldID fieldID) {
        return FieldID.toFieldActor(fieldID).getDouble(null);
    }

    @VM_ENTRY_POINT
    private static void SetStaticObjectField(Pointer env, JniHandle javaType, FieldID fieldID, JniHandle value) {
        FieldID.toFieldActor(fieldID).setObject(null, value.unhand());
    }

    @VM_ENTRY_POINT
    private static void SetStaticBooleanField(Pointer env, JniHandle javaType, FieldID fieldID, boolean value) {
        FieldID.toFieldActor(fieldID).setBoolean(null, value);
    }

    @VM_ENTRY_POINT
    private static void SetStaticByteField(Pointer env, JniHandle javaType, FieldID fieldID, byte value) {
        FieldID.toFieldActor(fieldID).setByte(null, value);
    }

    @VM_ENTRY_POINT
    private static void SetStaticCharField(Pointer env, JniHandle javaType, FieldID fieldID, char value) {
        FieldID.toFieldActor(fieldID).setChar(null, value);
    }

    @VM_ENTRY_POINT
    private static void SetStaticShortField(Pointer env, JniHandle javaType, FieldID fieldID, short value) {
        FieldID.toFieldActor(fieldID).setShort(null, value);
    }

    @VM_ENTRY_POINT
    private static void SetStaticIntField(Pointer env, JniHandle javaType, FieldID fieldID, int value) {
        FieldID.toFieldActor(fieldID).setInt(null, value);
    }

    @VM_ENTRY_POINT
    private static void SetStaticLongField(Pointer env, JniHandle javaType, FieldID fieldID, long value) {
        FieldID.toFieldActor(fieldID).setLong(null, value);
    }

    @VM_ENTRY_POINT
    private static void SetStaticFloatField(Pointer env, JniHandle javaType, FieldID fieldID, float value) {
        FieldID.toFieldActor(fieldID).setFloat(null, value);
    }

    @VM_ENTRY_POINT
    private static void SetStaticDoubleField(Pointer env, JniHandle javaType, FieldID fieldID, double value) {
        FieldID.toFieldActor(fieldID).setDouble(null, value);
    }

    @VM_ENTRY_POINT
    private static JniHandle NewString(Pointer env, Pointer chars, int length) {
        final char[] charArray = new char[length];
        for (int i = 0; i < length; i++) {
            charArray[i] = chars.getChar(i);
        }
        return JniHandles.createLocalHandle(new String(charArray));
    }

    @VM_ENTRY_POINT
    private static int GetStringLength(Pointer env, JniHandle string) {
        return ((String) string.unhand()).length();
    }

    @VM_ENTRY_POINT
    private static JniHandle GetStringChars(Pointer env, JniHandle string, Pointer isCopy) {
        setCopyPointer(isCopy, true);
        return JniHandles.createLocalHandle(((String) string.unhand()).toCharArray());
    }

    @VM_ENTRY_POINT
    private static void ReleaseStringChars(Pointer env, JniHandle string, Pointer chars) {
        Memory.deallocate(chars);
    }

    @VM_ENTRY_POINT
    private static JniHandle NewStringUTF(Pointer env, Pointer utf) {
        try {
            return JniHandles.createLocalHandle(CString.utf8ToJava(utf));
        } catch (Utf8Exception utf8Exception) {
            return JniHandle.zero();
        }
    }

    @VM_ENTRY_POINT
    private static int GetStringUTFLength(Pointer env, JniHandle string) {
        return Utf8.utf8Length((String) string.unhand());
    }

    @VM_ENTRY_POINT
    private static Pointer GetStringUTFChars(Pointer env, JniHandle string, Pointer isCopy) {
        setCopyPointer(isCopy, true);
        return CString.utf8FromJava((String) string.unhand());
    }

    @VM_ENTRY_POINT
    private static void ReleaseStringUTFChars(Pointer env, JniHandle string, Pointer chars) {
        Memory.deallocate(chars);
    }

    @VM_ENTRY_POINT
    private static int GetArrayLength(Pointer env, JniHandle array) {
        return Array.getLength(array.unhand());
    }

    @VM_ENTRY_POINT
    private static JniHandle NewObjectArray(Pointer env, int length, JniHandle elementType, JniHandle initialElementValue) {
        final Object array = Array.newInstance((Class) elementType.unhand(), length);
        final Object initialValue = initialElementValue.unhand();
        for (int i = 0; i < length; i++) {
            Array.set(array, i, initialValue);
        }
        return JniHandles.createLocalHandle(array);
    }

    @VM_ENTRY_POINT
    private static JniHandle GetObjectArrayElement(Pointer env, JniHandle array, int index) {
        return JniHandles.createLocalHandle(((Object[]) array.unhand())[index]);
    }

    @VM_ENTRY_POINT
    private static void SetObjectArrayElement(Pointer env, JniHandle array, int index, JniHandle value) {
        ((Object[]) array.unhand())[index] = value.unhand();
    }

    @VM_ENTRY_POINT
    private static JniHandle NewBooleanArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new boolean[length]);
    }

    @VM_ENTRY_POINT
    private static JniHandle NewByteArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new byte[length]);
    }

    @VM_ENTRY_POINT
    private static JniHandle NewCharArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new char[length]);
    }

    @VM_ENTRY_POINT
    private static JniHandle NewShortArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new short[length]);
    }

    @VM_ENTRY_POINT
    private static JniHandle NewIntArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new int[length]);
    }

    @VM_ENTRY_POINT
    private static JniHandle NewLongArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new long[length]);
    }

    @VM_ENTRY_POINT
    private static JniHandle NewFloatArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new float[length]);
    }

    @VM_ENTRY_POINT
    private static JniHandle NewDoubleArray(Pointer env, int length) {
        return JniHandles.createLocalHandle(new double[length]);
    }

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
    private static void GetBooleanArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final boolean[] a = (boolean[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setBoolean(i, a[start + i]);
        }
    }

    @VM_ENTRY_POINT
    private static void GetByteArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final byte[] a = (byte[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setByte(i, a[start + i]);
        }
    }

    @VM_ENTRY_POINT
    private static void GetCharArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final char[] a = (char[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setChar(i, a[start + i]);
        }
    }

    @VM_ENTRY_POINT
    private static void GetShortArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final short[] a = (short[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setShort(i, a[start + i]);
        }
    }

    @VM_ENTRY_POINT
    private static void GetIntArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final int[] a = (int[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setInt(i, a[start + i]);
        }
    }

    @VM_ENTRY_POINT
    private static void GetLongArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final long[] a = (long[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setLong(i, a[start + i]);
        }
    }

    @VM_ENTRY_POINT
    private static void GetFloatArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final float[] a = (float[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setFloat(i, a[start + i]);
        }
    }

    @VM_ENTRY_POINT
    private static void GetDoubleArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final double[] a = (double[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setDouble(i, a[start + i]);
        }
    }

    @VM_ENTRY_POINT
    private static void SetBooleanArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final boolean[] a = (boolean[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getBoolean(i);
        }
    }

    @VM_ENTRY_POINT
    private static void SetByteArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final byte[] a = (byte[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getByte(i);
        }
    }

    @VM_ENTRY_POINT
    private static void SetCharArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final char[] a = (char[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getChar(i);
        }
    }

    @VM_ENTRY_POINT
    private static void SetShortArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final short[] a = (short[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getShort(i);
        }
    }

    @VM_ENTRY_POINT
    private static void SetIntArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final int[] a = (int[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getInt(i);
        }
    }

    @VM_ENTRY_POINT
    private static void SetLongArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final long[] a = (long[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getLong(i);
        }
    }

    @VM_ENTRY_POINT
    private static void SetFloatArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final float[] a = (float[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getFloat(i);
        }
    }

    @VM_ENTRY_POINT
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
    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
    private static int UnregisterNatives(Pointer env, JniHandle javaType) {
        ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
        for (VirtualMethodActor method : classActor.allVirtualMethodActors()) {
            method.nativeFunction.setAddress(Word.zero());
        }
        do {
            for (StaticMethodActor method : classActor.localStaticMethodActors()) {
                method.nativeFunction.setAddress(Word.zero());
            }
            classActor = classActor.superClassActor;
        } while (classActor != null);
        return 0;
    }

    @VM_ENTRY_POINT
    private static int MonitorEnter(Pointer env, JniHandle object) {
        Monitor.enter(object.unhand());
        return 0;
    }

    @VM_ENTRY_POINT
    private static int MonitorExit(Pointer env, JniHandle object) {
        Monitor.exit(object.unhand());
        return 0;
    }

    @VM_ENTRY_POINT
    private static native int GetJavaVM(Pointer env, Pointer vmPointerPointer);

    @VM_ENTRY_POINT
    private static void GetStringRegion(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        final String s = (String) string.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setChar(i, s.charAt(i + start));
        }
    }

    @VM_ENTRY_POINT
    private static void GetStringUTFRegion(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        final String s = ((String) string.unhand()).substring(start, start + length);
        final byte[] utf = Utf8.stringToUtf8(s);
        Memory.writeBytes(utf, utf.length, buffer);
        buffer.setByte(utf.length, (byte) 0); // zero termination
    }

    @VM_ENTRY_POINT
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
        return Pointer.zero();
    }

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
    private static Pointer GetStringCritical(Pointer env, JniHandle string, Pointer isCopy) {
        setCopyPointer(isCopy, true);
        final char[] a = ((String) string.unhand()).toCharArray();
        final Pointer pointer = Memory.mustAllocate(a.length * Kind.CHAR.width.numberOfBytes);
        for (int i = 0; i < a.length; i++) {
            pointer.setChar(i, a[i]);
        }
        return pointer;
    }

    @VM_ENTRY_POINT
    private static void ReleaseStringCritical(Pointer env, JniHandle string, final Pointer chars) {
        Memory.deallocate(chars);
    }

    @VM_ENTRY_POINT
    private static JniHandle NewWeakGlobalRef(Pointer env, JniHandle handle) {
        return JniHandles.createWeakGlobalHandle(handle.unhand());
    }

    @VM_ENTRY_POINT
    private static void DeleteWeakGlobalRef(Pointer env, JniHandle handle) {
        JniHandles.destroyWeakGlobalHandle(handle);
    }

    @VM_ENTRY_POINT
    private static boolean ExceptionCheck(Pointer env) {
        return VmThread.fromJniEnv(env).jniException() != null;
    }

    private static final ClassActor DirectByteBuffer = ClassActor.fromJava(Classes.forName("java.nio.DirectByteBuffer"));

    @VM_ENTRY_POINT
    private static JniHandle NewDirectByteBuffer(Pointer env, Pointer address, long capacity) throws Exception {
        ByteBuffer buffer = ObjectAccess.createDirectByteBuffer(address.toLong(), (int) capacity);
        return JniHandles.createLocalHandle(buffer);
    }

    @VM_ENTRY_POINT
    private static Pointer GetDirectBufferAddress(Pointer env, JniHandle buffer) throws Exception {
        Object buf = buffer.unhand();
        if (DirectByteBuffer.isInstance(buf)) {
            long address = ClassRegistry.Buffer_address.getLong(buf);
            return Pointer.fromLong(address);
        }
        return Pointer.zero();
    }

    @VM_ENTRY_POINT
    private static long GetDirectBufferCapacity(Pointer env, JniHandle buffer) {
        Object buf = buffer.unhand();
        if (DirectByteBuffer.isInstance(buf)) {
            return ((Buffer) buf).capacity();
        }
        return -1;
    }

    @VM_ENTRY_POINT
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

    @VM_ENTRY_POINT
    private static int GetNumberOfArguments(Pointer env, MethodID methodID) throws Exception {
        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null) {
            throw new NoSuchMethodException();
        }
        return methodActor.descriptor().numberOfParameters();
    }

    @VM_ENTRY_POINT
    private static void GetKindsOfArguments(Pointer env, MethodID methodID, Pointer kinds) throws Exception {
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
