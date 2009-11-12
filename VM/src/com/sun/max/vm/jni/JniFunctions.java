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
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.lang.reflect.*;
import java.nio.*;

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
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Upcalls from C that implement the <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jni/spec/jniTOC.html">JNI Interface Functions</a>.
 * <p>
 * <b>DO NOT EDIT CODE BETWEEN "START GENERATED CODE" AND "END GENERATED CODE" IN THIS FILE.</b>
 * <p>
 * Instead, modify the corresponding source in JniFunctionsSource.java denoted by the "// Source: ..." comments.
 * Once finished with editing, execute {@link JniFunctionsGenerator} as a Java application to refresh this file.
 *
 * @see JniNativeInterface
 * @see JniFunctionsSource
 * @see Native/substrate/jni.c
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

    @UNSAFE_CAST public static native JniHandle asJniHandle(int value);
    @UNSAFE_CAST public static native MethodID  asMethodID(int value);
    @UNSAFE_CAST public static native FieldID   asFieldID(int value);
    @UNSAFE_CAST public static native Pointer   asPointer(int value);

    /**
     * This method implements part of the prologue for entering a JNI upcall from native code.
     *
     * @param enabledVmThreadLocals
     */
    @INLINE
    public static Pointer reenterJavaFromNative(Pointer enabledVmThreadLocals) {
        Word previousAnchor = LAST_JAVA_FRAME_ANCHOR.getVariableWord();
        if (previousAnchor.isZero()) {
            FatalError.unexpected("LAST_JAVA_FRAME_ANCHOR is zero");
        }
        Pointer anchor = JavaFrameAnchor.create(Word.zero(), Word.zero(), Word.zero(), previousAnchor);
        // a JNI upcall is similar to a native method returning; reuse the native call epilogue sequence
        NativeCallEpilogue.nativeCallEpilogue0(enabledVmThreadLocals, anchor);
        return anchor;
    }

    @INLINE
    private static Pointer prologue(Pointer env, String name) {
        Safepoint.setLatchRegister(fromJniEnv(env));
        Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer();
        Pointer anchor = reenterJavaFromNative(enabledVmThreadLocals);
        traceEntry(name, anchor);
        return anchor;
    }

    /**
     * This method implements the epilogue for leaving an JNI upcall. The steps performed are to
     * reset the thread-local information which stores the last Java caller SP, FP, and IP, and
     * print a trace if necessary.
     *
     * @param name the method which was called (for tracing only)
     */
    @INLINE
    private static void epilogue(Pointer anchor, String name) {
        traceExit(name);

        // returning from a JNI upcall is similar to a entering a native method returning; reuse the native call prologue sequence
        Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer();
        NativeCallPrologue.nativeCallPrologue0(enabledVmThreadLocals, JavaFrameAnchor.PREVIOUS.get(anchor));
    }

    /**
     * Traces the entry to an upcall if the {@linkplain ClassMethodActor#traceJNI() JNI tracing flag} has been set.
     *
     * @param name the name of the JNI function being entered
     */
    private static void traceEntry(String name, Pointer anchor) {
        if (ClassMethodActor.traceJNI()) {
            boolean lockDisabledSafepoints = Log.lock();
            Log.print("[Thread \"");
            Log.print(VmThread.current().getName());
            Log.print("\" --> JNI upcall: ");
            Log.print(name);
            Pointer jniStubAnchor = JavaFrameAnchor.PREVIOUS.get(anchor);
            final Address jniStubPC = JavaFrameAnchor.PC.get(jniStubAnchor).asAddress();
            final TargetMethod nativeMethod = Code.codePointerToTargetMethod(jniStubPC);
            Log.print(", last down call: ");
            FatalError.check(nativeMethod != null, "Could not find Java down call when entering JNI upcall");
            Log.print(nativeMethod.classMethodActor().name.string);
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    /**
     * Traces the exit from an upcall if the {@linkplain ClassMethodActor#traceJNI() JNI tracing flag} has been set.
     *
     * @param name the name of the JNI function being exited
     */
    private static void traceExit(String name) {
        if (ClassMethodActor.traceJNI()) {
            boolean lockDisabledSafepoints = Log.lock();
            Log.print("[Thread \"");
            Log.print(VmThread.current().getName());
            Log.print("\" <-- JNI upcall: ");
            Log.print(name);
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    // Run the generator for the source of this file. If it differs from the on-disk source, throw an error
    // indicating that this source file needs to be recompiled and the bootstrapping process restarted.
    static {
        try {
            if (JniFunctionsGenerator.generate(true)) {
                String thisFile = JniFunctions.class.getSimpleName() + ".java";
                String sourceFile = JniFunctionsSource.class.getSimpleName() + ".java";
                FatalError.unexpected(String.format("%n%n" + thisFile +
                    " is out of sync with respect to " + sourceFile + ".%n" +
                    "Run " + JniFunctionsGenerator.class.getSimpleName() + ".java (via 'max jnigen'), recompile " + thisFile + " (or refresh it in your IDE)" +
                    " and restart the bootstrapping process.%n%n"));
            }
        } catch (Exception exception) {
            FatalError.unexpected("Error while generating source for " + JniFunctions.class, exception);
        }
    }

    /*
     * DO NOT EDIT CODE BETWEEN "START GENERATED CODE" AND "END GENERATED CODE" IN THIS FILE.
     *
     * Instead, modify the corresponding source in JniFunctionsSource.java denoted by the "// Source: ..." comments.
     * Once finished with editing, execute JniFunctionsGenerator as a Java application to refresh this file.
     */

// START GENERATED CODE

    @JNI_FUNCTION
    private static native void reserved0();

    @JNI_FUNCTION
    private static native void reserved1();

    @JNI_FUNCTION
    private static native void reserved2();

    @JNI_FUNCTION
    private static native void reserved3();

    // Checkstyle: stop method name check

    @JNI_FUNCTION
    private static native int GetVersion(Pointer env);

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
        Pointer anchor = prologue(env, "DefineClass");
        JniHandle result;
        try {
            result = DefineClass_(env, slashifiedName, classLoader, buffer, length);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "DefineClass");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:103
    private static JniHandle DefineClass_(Pointer env, Pointer slashifiedName, JniHandle classLoader, Pointer buffer, int length) throws ClassFormatError {
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
            VmThread.fromJniEnv(env).setPendingException(invocationTargetException.getTargetException());
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

    @JNI_FUNCTION
    private static JniHandle FindClass(Pointer env, Pointer name) throws ClassNotFoundException {
        Pointer anchor = prologue(env, "FindClass");
        JniHandle result;
        try {
            result = FindClass_(env, name);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "FindClass");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:135
    private static JniHandle FindClass_(Pointer env, Pointer name) throws ClassNotFoundException {
        String className;
        try {
            className = CString.utf8ToJava(name);
        } catch (Utf8Exception utf8Exception) {
            throw new ClassNotFoundException();
        }
        final Class javaClass = findClass(VmStackFrameWalker.getCallerClassMethodActor().holder().classLoader, className);
        MakeClassInitialized.makeClassInitialized(ClassActor.fromJava(javaClass));
        return JniHandles.createLocalHandle(javaClass);
    }

    @JNI_FUNCTION
    private static MethodID FromReflectedMethod(Pointer env, JniHandle reflectedMethod) {
        Pointer anchor = prologue(env, "FromReflectedMethod");
        MethodID result;
        try {
            result = FromReflectedMethod_(env, reflectedMethod);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asMethodID(0);
        }
        epilogue(anchor, "FromReflectedMethod");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:148
    private static MethodID FromReflectedMethod_(Pointer env, JniHandle reflectedMethod) {
        final MethodActor methodActor = MethodActor.fromJava((Method) reflectedMethod.unhand());
        return MethodID.fromMethodActor(methodActor);
    }

    @JNI_FUNCTION
    private static FieldID FromReflectedField(Pointer env, JniHandle field) {
        Pointer anchor = prologue(env, "FromReflectedField");
        FieldID result;
        try {
            result = FromReflectedField_(env, field);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asFieldID(0);
        }
        epilogue(anchor, "FromReflectedField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:154
    private static FieldID FromReflectedField_(Pointer env, JniHandle field) {
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
        Pointer anchor = prologue(env, "ToReflectedMethod");
        JniHandle result;
        try {
            result = ToReflectedMethod_(env, javaClass, methodID, isStatic);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "ToReflectedMethod");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:168
    private static JniHandle ToReflectedMethod_(Pointer env, JniHandle javaClass, MethodID methodID, boolean isStatic) throws NoSuchMethodException {
        return JniHandles.createLocalHandle(ToReflectedMethod(methodID, isStatic));
    }

    @JNI_FUNCTION
    private static JniHandle GetSuperclass(Pointer env, JniHandle subType) {
        Pointer anchor = prologue(env, "GetSuperclass");
        JniHandle result;
        try {
            result = GetSuperclass_(env, subType);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "GetSuperclass");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:173
    private static JniHandle GetSuperclass_(Pointer env, JniHandle subType) {
        return JniHandles.createLocalHandle(((Class) subType.unhand()).getSuperclass());
    }

    @JNI_FUNCTION
    private static boolean IsAssignableFrom(Pointer env, JniHandle subType, JniHandle superType) {
        Pointer anchor = prologue(env, "IsAssignableFrom");
        boolean result;
        try {
            result = IsAssignableFrom_(env, subType, superType);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = false;
        }
        epilogue(anchor, "IsAssignableFrom");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:178
    private static boolean IsAssignableFrom_(Pointer env, JniHandle subType, JniHandle superType) {
        return ClassActor.fromJava((Class) superType.unhand()).isAssignableFrom(ClassActor.fromJava((Class) subType.unhand()));
    }

    @JNI_FUNCTION
    private static JniHandle ToReflectedField(Pointer env, JniHandle javaClass, FieldID fieldID, boolean isStatic) {
        Pointer anchor = prologue(env, "ToReflectedField");
        JniHandle result;
        try {
            result = ToReflectedField_(env, javaClass, fieldID, isStatic);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "ToReflectedField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:183
    private static JniHandle ToReflectedField_(Pointer env, JniHandle javaClass, FieldID fieldID, boolean isStatic) {
        final FieldActor fieldActor = FieldID.toFieldActor(fieldID);
        if (fieldActor == null || fieldActor.isStatic() != isStatic) {
            throw new NoSuchFieldError();
        }
        return JniHandles.createLocalHandle(fieldActor.toJava());
    }

    @JNI_FUNCTION
    private static int Throw(Pointer env, JniHandle throwable) {
        Pointer anchor = prologue(env, "Throw");
        int result;
        try {
            result = Throw_(env, throwable);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "Throw");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:192
    private static int Throw_(Pointer env, JniHandle throwable) {
        VmThread.fromJniEnv(env).setPendingException((Throwable) throwable.unhand());
        return JNI_OK;
    }

    @JNI_FUNCTION
    private static int ThrowNew(Pointer env, JniHandle throwableClass, Pointer message) throws Throwable {
        Pointer anchor = prologue(env, "ThrowNew");
        int result;
        try {
            result = ThrowNew_(env, throwableClass, message);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "ThrowNew");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:198
    private static int ThrowNew_(Pointer env, JniHandle throwableClass, Pointer message) throws Throwable {
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
        Pointer anchor = prologue(env, "ExceptionOccurred");
        JniHandle result;
        try {
            result = ExceptionOccurred_(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "ExceptionOccurred");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:215
    private static JniHandle ExceptionOccurred_(Pointer env) {
        return JniHandles.createLocalHandle(VmThread.fromJniEnv(env).pendingException());
    }

    @JNI_FUNCTION
    private static void ExceptionDescribe(Pointer env) {
        Pointer anchor = prologue(env, "ExceptionDescribe");
        try {
            ExceptionDescribe_(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ExceptionDescribe");
    }

    @INLINE
    // Source: JniFunctionsSource.java:220
    private static void ExceptionDescribe_(Pointer env) {
        final Throwable exception = VmThread.fromJniEnv(env).pendingException();
        if (exception != null) {
            exception.printStackTrace();
        }
    }

    @JNI_FUNCTION
    private static void ExceptionClear(Pointer env) {
        Pointer anchor = prologue(env, "ExceptionClear");
        try {
            ExceptionClear_(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ExceptionClear");
    }

    @INLINE
    // Source: JniFunctionsSource.java:228
    private static void ExceptionClear_(Pointer env) {
        VmThread.fromJniEnv(env).setPendingException(null);
    }

    @JNI_FUNCTION
    private static void FatalError(Pointer env, Pointer message) {
        Pointer anchor = prologue(env, "FatalError");
        try {
            FatalError_(env, message);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "FatalError");
    }

    @INLINE
    // Source: JniFunctionsSource.java:233
    private static void FatalError_(Pointer env, Pointer message) {
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
        Pointer anchor = prologue(env, "PushLocalFrame");
        int result;
        try {
            result = PushLocalFrame_(env, capacity);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "PushLocalFrame");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:247
    private static int PushLocalFrame_(Pointer env, int capacity) {
        JniHandles.pushLocalFrame(capacity);
        return JNI_OK;
    }

    @JNI_FUNCTION
    private static JniHandle PopLocalFrame(Pointer env, JniHandle res) {
        Pointer anchor = prologue(env, "PopLocalFrame");
        JniHandle result;
        try {
            result = PopLocalFrame_(env, res);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "PopLocalFrame");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:253
    private static JniHandle PopLocalFrame_(Pointer env, JniHandle res) {
        return JniHandles.popLocalFrame(res);
    }

    @JNI_FUNCTION
    private static JniHandle NewGlobalRef(Pointer env, JniHandle handle) {
        Pointer anchor = prologue(env, "NewGlobalRef");
        JniHandle result;
        try {
            result = NewGlobalRef_(env, handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewGlobalRef");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:258
    private static JniHandle NewGlobalRef_(Pointer env, JniHandle handle) {
        return JniHandles.createGlobalHandle(handle.unhand());
    }

    @JNI_FUNCTION
    private static void DeleteGlobalRef(Pointer env, JniHandle handle) {
        Pointer anchor = prologue(env, "DeleteGlobalRef");
        try {
            DeleteGlobalRef_(env, handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "DeleteGlobalRef");
    }

    @INLINE
    // Source: JniFunctionsSource.java:263
    private static void DeleteGlobalRef_(Pointer env, JniHandle handle) {
        JniHandles.destroyGlobalHandle(handle);
    }

    @JNI_FUNCTION
    private static void DeleteLocalRef(Pointer env, JniHandle handle) {
        Pointer anchor = prologue(env, "DeleteLocalRef");
        try {
            DeleteLocalRef_(env, handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "DeleteLocalRef");
    }

    @INLINE
    // Source: JniFunctionsSource.java:268
    private static void DeleteLocalRef_(Pointer env, JniHandle handle) {
        JniHandles.destroyLocalHandle(handle);
    }

    @JNI_FUNCTION
    private static boolean IsSameObject(Pointer env, JniHandle object1, JniHandle object2) {
        Pointer anchor = prologue(env, "IsSameObject");
        boolean result;
        try {
            result = IsSameObject_(env, object1, object2);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = false;
        }
        epilogue(anchor, "IsSameObject");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:273
    private static boolean IsSameObject_(Pointer env, JniHandle object1, JniHandle object2) {
        return object1.unhand() == object2.unhand();
    }

    @JNI_FUNCTION
    private static JniHandle NewLocalRef(Pointer env, JniHandle object) {
        Pointer anchor = prologue(env, "NewLocalRef");
        JniHandle result;
        try {
            result = NewLocalRef_(env, object);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewLocalRef");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:278
    private static JniHandle NewLocalRef_(Pointer env, JniHandle object) {
        return JniHandles.createLocalHandle(object.unhand());
    }

    @JNI_FUNCTION
    private static int EnsureLocalCapacity(Pointer env, int capacity) {
        Pointer anchor = prologue(env, "EnsureLocalCapacity");
        int result;
        try {
            result = EnsureLocalCapacity_(env, capacity);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "EnsureLocalCapacity");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:283
    private static int EnsureLocalCapacity_(Pointer env, int capacity) {
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
        Pointer anchor = prologue(env, "AllocObject");
        JniHandle result;
        try {
            result = AllocObject_(env, javaClass);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "AllocObject");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:299
    private static JniHandle AllocObject_(Pointer env, JniHandle javaClass) throws InstantiationException {
        return JniHandles.createLocalHandle(allocObject((Class) javaClass.unhand()));
    }

    @JNI_FUNCTION
    private static native JniHandle NewObject(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native JniHandle NewObjectV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static JniHandle NewObjectA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "NewObjectA");
        JniHandle result;
        try {
            result = NewObjectA_(env, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewObjectA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:310
    private static JniHandle NewObjectA_(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {

        final ClassActor classActor = ClassActor.fromJava((Class) javaClass.unhand());
        if (!(classActor instanceof TupleClassActor)) {
            throw new NoSuchMethodException();
        }
        final TupleClassActor tupleClassActor = (TupleClassActor) classActor;

        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null || !methodActor.isInitializer()) {
            throw new NoSuchMethodException();
        }
        final VirtualMethodActor virtualMethodActor = tupleClassActor.findLocalVirtualMethodActor(methodActor);
        if (virtualMethodActor == null) {
            throw new NoSuchMethodException();
        }

        final SignatureDescriptor signature = virtualMethodActor.descriptor();
        final Value[] argumentValues = new Value[signature.numberOfParameters()];
        copyJValueArrayToValueArray(arguments, signature, argumentValues, 0);
        traceReflectiveInvocation(virtualMethodActor);
        return JniHandles.createLocalHandle(virtualMethodActor.invokeConstructor(argumentValues).asObject());
    }

    @JNI_FUNCTION
    private static JniHandle GetObjectClass(Pointer env, JniHandle object) {
        Pointer anchor = prologue(env, "GetObjectClass");
        JniHandle result;
        try {
            result = GetObjectClass_(env, object);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "GetObjectClass");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:335
    private static JniHandle GetObjectClass_(Pointer env, JniHandle object) {
        final Class javaClass = object.unhand().getClass();
        return JniHandles.createLocalHandle(javaClass);
    }

    @JNI_FUNCTION
    private static boolean IsInstanceOf(Pointer env, JniHandle object, JniHandle javaType) {
        Pointer anchor = prologue(env, "IsInstanceOf");
        boolean result;
        try {
            result = IsInstanceOf_(env, object, javaType);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = false;
        }
        epilogue(anchor, "IsInstanceOf");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:341
    private static boolean IsInstanceOf_(Pointer env, JniHandle object, JniHandle javaType) {
        return ((Class) javaType.unhand()).isInstance(object.unhand());
    }

    @JNI_FUNCTION
    private static MethodID GetMethodID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        Pointer anchor = prologue(env, "GetMethodID");
        MethodID result;
        try {
            result = GetMethodID_(env, javaType, nameCString, descriptorCString);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asMethodID(0);
        }
        epilogue(anchor, "GetMethodID");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:346
    private static MethodID GetMethodID_(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
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
    private static native JniHandle CallObjectMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
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
        if (methodActor == null || methodActor.isStatic() || methodActor.isInitializer() || methodActor instanceof InterfaceMethodActor) {
            throw new NoSuchMethodException();
        }
        final VirtualMethodActor virtualMethodActor = MethodSelectionSnippet.SelectVirtualMethod.quasiFold(object.unhand(), (VirtualMethodActor) methodActor);

        final SignatureDescriptor signature = virtualMethodActor.descriptor();
        final Value[] argumentValues = new Value[1 + signature.numberOfParameters()];
        argumentValues[0] = ReferenceValue.from(object.unhand());
        copyJValueArrayToValueArray(arguments, signature, argumentValues, 1);
        traceReflectiveInvocation(virtualMethodActor);
        return virtualMethodActor.invoke(argumentValues);

    }

    @JNI_FUNCTION
    private static JniHandle CallObjectMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallObjectMethodA");
        JniHandle result;
        try {
            result = CallObjectMethodA_(env, object, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "CallObjectMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:457
    private static JniHandle CallObjectMethodA_(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return JniHandles.createLocalHandle(CallValueMethodA(env, object, methodID, arguments).asObject());
    }

    @JNI_FUNCTION
    private static native boolean CallBooleanMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native boolean CallBooleanMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer vaList);

    @JNI_FUNCTION
    private static boolean CallBooleanMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallBooleanMethodA");
        boolean result;
        try {
            result = CallBooleanMethodA_(env, object, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = false;
        }
        epilogue(anchor, "CallBooleanMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:468
    private static boolean CallBooleanMethodA_(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asBoolean();
    }

    @JNI_FUNCTION
    private static native byte CallByteMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native byte CallByteMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static byte CallByteMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallByteMethodA");
        byte result;
        try {
            result = CallByteMethodA_(env, object, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallByteMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:479
    private static byte CallByteMethodA_(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asByte();
    }

    @JNI_FUNCTION
    private static native char CallCharMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native char CallCharMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static char CallCharMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallCharMethodA");
        char result;
        try {
            result = CallCharMethodA_(env, object, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result =  (char) JNI_ERR;
        }
        epilogue(anchor, "CallCharMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:490
    private static char CallCharMethodA_(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asChar();
    }

    @JNI_FUNCTION
    private static native short CallShortMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native short CallShortMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static short CallShortMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallShortMethodA");
        short result;
        try {
            result = CallShortMethodA_(env, object, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallShortMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:501
    private static short CallShortMethodA_(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asShort();
    }

    @JNI_FUNCTION
    private static native int CallIntMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native int CallIntMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static int CallIntMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallIntMethodA");
        int result;
        try {
            result = CallIntMethodA_(env, object, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallIntMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:512
    private static int CallIntMethodA_(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asInt();
    }

    @JNI_FUNCTION
    private static native long CallLongMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native long CallLongMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static long CallLongMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallLongMethodA");
        long result;
        try {
            result = CallLongMethodA_(env, object, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallLongMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:523
    private static long CallLongMethodA_(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asLong();
    }

    @JNI_FUNCTION
    private static native float CallFloatMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native float CallFloatMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static float CallFloatMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallFloatMethodA");
        float result;
        try {
            result = CallFloatMethodA_(env, object, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallFloatMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:534
    private static float CallFloatMethodA_(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        return CallValueMethodA(env, object, methodID, arguments).asFloat();
    }

    @JNI_FUNCTION
    private static native double CallDoubleMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native double CallDoubleMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static double CallDoubleMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallDoubleMethodA");
        double result;
        try {
            result = CallDoubleMethodA_(env, object, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallDoubleMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:545
    private static double CallDoubleMethodA_(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
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
        final VirtualMethodActor virtualMethodActor = tupleClassActor.findLocalVirtualMethodActor(methodActor);
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

    @JNI_FUNCTION
    private static native void CallVoidMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native void CallVoidMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static void CallVoidMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallVoidMethodA");
        try {
            CallVoidMethodA_(env, object, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "CallVoidMethodA");
    }

    @INLINE
    // Source: JniFunctionsSource.java:580
    private static void CallVoidMethodA_(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        CallValueMethodA(env, object, methodID, arguments);
    }

    @JNI_FUNCTION
    private static native JniHandle CallNonvirtualObjectMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @JNI_FUNCTION
    private static native JniHandle CallNonvirtualObjectMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @JNI_FUNCTION
    private static JniHandle CallNonvirtualObjectMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualObjectMethodA");
        JniHandle result;
        try {
            result = CallNonvirtualObjectMethodA_(env, object, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "CallNonvirtualObjectMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:591
    private static JniHandle CallNonvirtualObjectMethodA_(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return JniHandles.createLocalHandle(CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asObject());
    }

    @JNI_FUNCTION
    private static native boolean CallNonvirtualBooleanMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @JNI_FUNCTION
    private static native boolean CallNonvirtualBooleanMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @JNI_FUNCTION
    private static boolean CallNonvirtualBooleanMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualBooleanMethodA");
        boolean result;
        try {
            result = CallNonvirtualBooleanMethodA_(env, object, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = false;
        }
        epilogue(anchor, "CallNonvirtualBooleanMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:602
    private static boolean CallNonvirtualBooleanMethodA_(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asBoolean();
    }

    @JNI_FUNCTION
    private static native byte CallNonvirtualByteMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @JNI_FUNCTION
    private static native byte CallNonvirtualByteMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @JNI_FUNCTION
    private static byte CallNonvirtualByteMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualByteMethodA");
        byte result;
        try {
            result = CallNonvirtualByteMethodA_(env, object, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallNonvirtualByteMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:613
    private static byte CallNonvirtualByteMethodA_(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asByte();
    }

    @JNI_FUNCTION
    private static native char CallNonvirtualCharMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @JNI_FUNCTION
    private static native char CallNonvirtualCharMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @JNI_FUNCTION
    private static char CallNonvirtualCharMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualCharMethodA");
        char result;
        try {
            result = CallNonvirtualCharMethodA_(env, object, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result =  (char) JNI_ERR;
        }
        epilogue(anchor, "CallNonvirtualCharMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:624
    private static char CallNonvirtualCharMethodA_(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asChar();
    }

    @JNI_FUNCTION
    private static native short CallNonvirtualShortMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @JNI_FUNCTION
    private static native short CallNonvirtualShortMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @JNI_FUNCTION
    private static short CallNonvirtualShortMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualShortMethodA");
        short result;
        try {
            result = CallNonvirtualShortMethodA_(env, object, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallNonvirtualShortMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:635
    private static short CallNonvirtualShortMethodA_(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asShort();
    }

    @JNI_FUNCTION
    private static native int CallNonvirtualIntMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @JNI_FUNCTION
    private static native int CallNonvirtualIntMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @JNI_FUNCTION
    private static int CallNonvirtualIntMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualIntMethodA");
        int result;
        try {
            result = CallNonvirtualIntMethodA_(env, object, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallNonvirtualIntMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:646
    private static int CallNonvirtualIntMethodA_(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asInt();
    }

    @JNI_FUNCTION
    private static native long CallNonvirtualLongMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @JNI_FUNCTION
    private static native long CallNonvirtualLongMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @JNI_FUNCTION
    private static long CallNonvirtualLongMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualLongMethodA");
        long result;
        try {
            result = CallNonvirtualLongMethodA_(env, object, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallNonvirtualLongMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:657
    private static long CallNonvirtualLongMethodA_(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asLong();
    }

    @JNI_FUNCTION
    private static native float CallNonvirtualFloatMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @JNI_FUNCTION
    private static native float CallNonvirtualFloatMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @JNI_FUNCTION
    private static float CallNonvirtualFloatMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualFloatMethodA");
        float result;
        try {
            result = CallNonvirtualFloatMethodA_(env, object, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallNonvirtualFloatMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:668
    private static float CallNonvirtualFloatMethodA_(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asFloat();
    }

    @JNI_FUNCTION
    private static native double CallNonvirtualDoubleMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @JNI_FUNCTION
    private static native double CallNonvirtualDoubleMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @JNI_FUNCTION
    private static double CallNonvirtualDoubleMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualDoubleMethodA");
        double result;
        try {
            result = CallNonvirtualDoubleMethodA_(env, object, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallNonvirtualDoubleMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:679
    private static double CallNonvirtualDoubleMethodA_(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asDouble();
    }

    @JNI_FUNCTION
    private static native void CallNonvirtualVoidMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    @JNI_FUNCTION
    private static native void CallNonvirtualVoidMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    @JNI_FUNCTION
    private static void CallNonvirtualVoidMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualVoidMethodA");
        try {
            CallNonvirtualVoidMethodA_(env, object, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "CallNonvirtualVoidMethodA");
    }

    @INLINE
    // Source: JniFunctionsSource.java:690
    private static void CallNonvirtualVoidMethodA_(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments);
    }

    @JNI_FUNCTION
    private static FieldID GetFieldID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        Pointer anchor = prologue(env, "GetFieldID");
        FieldID result;
        try {
            result = GetFieldID_(env, javaType, nameCString, descriptorCString);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asFieldID(0);
        }
        epilogue(anchor, "GetFieldID");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:695
    private static FieldID GetFieldID_(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
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
        Pointer anchor = prologue(env, "GetObjectField");
        JniHandle result;
        try {
            result = GetObjectField_(env, object, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "GetObjectField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:719
    private static JniHandle GetObjectField_(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor fieldActor = FieldID.toFieldActor(fieldID);
        return JniHandles.createLocalHandle(TupleAccess.readObject(object.unhand(), fieldActor.offset()));
    }

    @JNI_FUNCTION
    private static boolean GetBooleanField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetBooleanField");
        boolean result;
        try {
            result = GetBooleanField_(env, object, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = false;
        }
        epilogue(anchor, "GetBooleanField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:725
    private static boolean GetBooleanField_(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor booleanFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readBoolean(object.unhand(), booleanFieldActor.offset());
    }

    @JNI_FUNCTION
    private static byte GetByteField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetByteField");
        byte result;
        try {
            result = GetByteField_(env, object, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetByteField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:731
    private static byte GetByteField_(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor byteFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readByte(object.unhand(), byteFieldActor.offset());
    }

    @JNI_FUNCTION
    private static char GetCharField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetCharField");
        char result;
        try {
            result = GetCharField_(env, object, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result =  (char) JNI_ERR;
        }
        epilogue(anchor, "GetCharField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:737
    private static char GetCharField_(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor charFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readChar(object.unhand(), charFieldActor.offset());
    }

    @JNI_FUNCTION
    private static short GetShortField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetShortField");
        short result;
        try {
            result = GetShortField_(env, object, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetShortField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:743
    private static short GetShortField_(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor shortFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readShort(object.unhand(), shortFieldActor.offset());
    }

    @JNI_FUNCTION
    private static int GetIntField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetIntField");
        int result;
        try {
            result = GetIntField_(env, object, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetIntField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:749
    private static int GetIntField_(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor intFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readInt(object.unhand(), intFieldActor.offset());
    }

    @JNI_FUNCTION
    private static long GetLongField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetLongField");
        long result;
        try {
            result = GetLongField_(env, object, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetLongField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:755
    private static long GetLongField_(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor longFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readLong(object.unhand(), longFieldActor.offset());
    }

    @JNI_FUNCTION
    private static float GetFloatField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetFloatField");
        float result;
        try {
            result = GetFloatField_(env, object, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetFloatField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:761
    private static float GetFloatField_(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor floatFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readFloat(object.unhand(), floatFieldActor.offset());
    }

    @JNI_FUNCTION
    private static double GetDoubleField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetDoubleField");
        double result;
        try {
            result = GetDoubleField_(env, object, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetDoubleField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:767
    private static double GetDoubleField_(Pointer env, JniHandle object, FieldID fieldID) {
        final FieldActor doubleFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readDouble(object.unhand(), doubleFieldActor.offset());
    }

    @JNI_FUNCTION
    private static void SetObjectField(Pointer env, JniHandle object, FieldID fieldID, JniHandle value) {
        Pointer anchor = prologue(env, "SetObjectField");
        try {
            SetObjectField_(env, object, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetObjectField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:773
    private static void SetObjectField_(Pointer env, JniHandle object, FieldID fieldID, JniHandle value) {
        final FieldActor referenceFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeObject(object.unhand(), referenceFieldActor.offset(), value.unhand());
    }

    @JNI_FUNCTION
    private static void SetBooleanField(Pointer env, JniHandle object, FieldID fieldID, boolean value) {
        Pointer anchor = prologue(env, "SetBooleanField");
        try {
            SetBooleanField_(env, object, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetBooleanField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:779
    private static void SetBooleanField_(Pointer env, JniHandle object, FieldID fieldID, boolean value) {
        final FieldActor booleanFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeBoolean(object.unhand(), booleanFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetByteField(Pointer env, JniHandle object, FieldID fieldID, byte value) {
        Pointer anchor = prologue(env, "SetByteField");
        try {
            SetByteField_(env, object, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetByteField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:785
    private static void SetByteField_(Pointer env, JniHandle object, FieldID fieldID, byte value) {
        final FieldActor byteFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeByte(object.unhand(), byteFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetCharField(Pointer env, JniHandle object, FieldID fieldID, char value) {
        Pointer anchor = prologue(env, "SetCharField");
        try {
            SetCharField_(env, object, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetCharField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:791
    private static void SetCharField_(Pointer env, JniHandle object, FieldID fieldID, char value) {
        final FieldActor charFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeChar(object.unhand(), charFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetShortField(Pointer env, JniHandle object, FieldID fieldID, short value) {
        Pointer anchor = prologue(env, "SetShortField");
        try {
            SetShortField_(env, object, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetShortField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:797
    private static void SetShortField_(Pointer env, JniHandle object, FieldID fieldID, short value) {
        final FieldActor shortFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeShort(object.unhand(), shortFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetIntField(Pointer env, JniHandle object, FieldID fieldID, int value) {
        Pointer anchor = prologue(env, "SetIntField");
        try {
            SetIntField_(env, object, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetIntField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:803
    private static void SetIntField_(Pointer env, JniHandle object, FieldID fieldID, int value) {
        final FieldActor intFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeInt(object.unhand(), intFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetLongField(Pointer env, JniHandle object, FieldID fieldID, long value) {
        Pointer anchor = prologue(env, "SetLongField");
        try {
            SetLongField_(env, object, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetLongField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:809
    private static void SetLongField_(Pointer env, JniHandle object, FieldID fieldID, long value) {
        final FieldActor longFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeLong(object.unhand(), longFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetFloatField(Pointer env, JniHandle object, FieldID fieldID, float value) {
        Pointer anchor = prologue(env, "SetFloatField");
        try {
            SetFloatField_(env, object, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetFloatField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:815
    private static void SetFloatField_(Pointer env, JniHandle object, FieldID fieldID, float value) {
        final FieldActor floatFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeFloat(object.unhand(), floatFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetDoubleField(Pointer env, JniHandle object, FieldID fieldID, double value) {
        Pointer anchor = prologue(env, "SetDoubleField");
        try {
            SetDoubleField_(env, object, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetDoubleField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:821
    private static void SetDoubleField_(Pointer env, JniHandle object, FieldID fieldID, double value) {
        final FieldActor doubleFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeDouble(object.unhand(), doubleFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static MethodID GetStaticMethodID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        Pointer anchor = prologue(env, "GetStaticMethodID");
        MethodID result;
        try {
            result = GetStaticMethodID_(env, javaType, nameCString, descriptorCString);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asMethodID(0);
        }
        epilogue(anchor, "GetStaticMethodID");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:827
    private static MethodID GetStaticMethodID_(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
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

    private static Value CallStaticValueMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
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
    private static native JniHandle CallStaticObjectMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native JniHandle CallStaticObjectMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static JniHandle CallStaticObjectMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticObjectMethodA");
        JniHandle result;
        try {
            result = CallStaticObjectMethodA_(env, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "CallStaticObjectMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:877
    private static JniHandle CallStaticObjectMethodA_(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return JniHandles.createLocalHandle(CallStaticValueMethodA(env, javaClass, methodID, arguments).asObject());
    }

    @JNI_FUNCTION
    private static native boolean CallStaticBooleanMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native boolean CallStaticBooleanMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static boolean CallStaticBooleanMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticBooleanMethodA");
        boolean result;
        try {
            result = CallStaticBooleanMethodA_(env, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = false;
        }
        epilogue(anchor, "CallStaticBooleanMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:888
    private static boolean CallStaticBooleanMethodA_(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asBoolean();
    }

    @JNI_FUNCTION
    private static native byte CallStaticByteMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native byte CallStaticByteMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static byte CallStaticByteMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticByteMethodA");
        byte result;
        try {
            result = CallStaticByteMethodA_(env, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallStaticByteMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:899
    private static byte CallStaticByteMethodA_(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asByte();
    }

    @JNI_FUNCTION
    private static native char CallStaticCharMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native char CallStaticCharMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static char CallStaticCharMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticCharMethodA");
        char result;
        try {
            result = CallStaticCharMethodA_(env, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result =  (char) JNI_ERR;
        }
        epilogue(anchor, "CallStaticCharMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:910
    private static char CallStaticCharMethodA_(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asChar();
    }

    @JNI_FUNCTION
    private static native short CallStaticShortMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native short CallStaticShortMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static short CallStaticShortMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticShortMethodA");
        short result;
        try {
            result = CallStaticShortMethodA_(env, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallStaticShortMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:921
    private static short CallStaticShortMethodA_(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asShort();
    }

    @JNI_FUNCTION
    private static native int CallStaticIntMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native int CallStaticIntMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static int CallStaticIntMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticIntMethodA");
        int result;
        try {
            result = CallStaticIntMethodA_(env, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallStaticIntMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:932
    private static int CallStaticIntMethodA_(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asInt();
    }

    @JNI_FUNCTION
    private static native long CallStaticLongMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native long CallStaticLongMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static long CallStaticLongMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticLongMethodA");
        long result;
        try {
            result = CallStaticLongMethodA_(env, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallStaticLongMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:943
    private static long CallStaticLongMethodA_(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asLong();
    }

    @JNI_FUNCTION
    private static native float CallStaticFloatMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native float CallStaticFloatMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static float CallStaticFloatMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticFloatMethodA");
        float result;
        try {
            result = CallStaticFloatMethodA_(env, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallStaticFloatMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:954
    private static float CallStaticFloatMethodA_(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asFloat();
    }

    @JNI_FUNCTION
    private static native double CallStaticDoubleMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native double CallStaticDoubleMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static double CallStaticDoubleMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticDoubleMethodA");
        double result;
        try {
            result = CallStaticDoubleMethodA_(env, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "CallStaticDoubleMethodA");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:965
    private static double CallStaticDoubleMethodA_(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        return CallStaticValueMethodA(env, javaClass, methodID, arguments).asDouble();
    }

    @JNI_FUNCTION
    private static native void CallStaticVoidMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    @JNI_FUNCTION
    private static native void CallStaticVoidMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    @JNI_FUNCTION
    private static void CallStaticVoidMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticVoidMethodA");
        try {
            CallStaticVoidMethodA_(env, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "CallStaticVoidMethodA");
    }

    @INLINE
    // Source: JniFunctionsSource.java:976
    private static void CallStaticVoidMethodA_(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        CallStaticValueMethodA(env, javaClass, methodID, arguments);
    }

    @JNI_FUNCTION
    private static FieldID GetStaticFieldID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        Pointer anchor = prologue(env, "GetStaticFieldID");
        FieldID result;
        try {
            result = GetStaticFieldID_(env, javaType, nameCString, descriptorCString);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asFieldID(0);
        }
        epilogue(anchor, "GetStaticFieldID");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:981
    private static FieldID GetStaticFieldID_(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
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
        Pointer anchor = prologue(env, "GetStaticObjectField");
        JniHandle result;
        try {
            result = GetStaticObjectField_(env, javaType, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "GetStaticObjectField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1009
    private static JniHandle GetStaticObjectField_(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor referenceFieldActor = FieldID.toFieldActor(fieldID);
        return JniHandles.createLocalHandle(TupleAccess.readObject(javaTypeToStaticTuple(javaType), referenceFieldActor.offset()));
    }

    @JNI_FUNCTION
    private static boolean GetStaticBooleanField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticBooleanField");
        boolean result;
        try {
            result = GetStaticBooleanField_(env, javaType, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = false;
        }
        epilogue(anchor, "GetStaticBooleanField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1015
    private static boolean GetStaticBooleanField_(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor booleanFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readBoolean(javaTypeToStaticTuple(javaType), booleanFieldActor.offset());
    }

    @JNI_FUNCTION
    private static byte GetStaticByteField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticByteField");
        byte result;
        try {
            result = GetStaticByteField_(env, javaType, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetStaticByteField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1021
    private static byte GetStaticByteField_(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor byteFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readByte(javaTypeToStaticTuple(javaType), byteFieldActor.offset());
    }

    @JNI_FUNCTION
    private static char GetStaticCharField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticCharField");
        char result;
        try {
            result = GetStaticCharField_(env, javaType, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result =  (char) JNI_ERR;
        }
        epilogue(anchor, "GetStaticCharField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1027
    private static char GetStaticCharField_(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor charFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readChar(javaTypeToStaticTuple(javaType), charFieldActor.offset());
    }

    @JNI_FUNCTION
    private static short GetStaticShortField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticShortField");
        short result;
        try {
            result = GetStaticShortField_(env, javaType, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetStaticShortField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1033
    private static short GetStaticShortField_(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor shortFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readShort(javaTypeToStaticTuple(javaType), shortFieldActor.offset());
    }

    @JNI_FUNCTION
    private static int GetStaticIntField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticIntField");
        int result;
        try {
            result = GetStaticIntField_(env, javaType, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetStaticIntField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1039
    private static int GetStaticIntField_(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor intFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readInt(javaTypeToStaticTuple(javaType), intFieldActor.offset());
    }

    @JNI_FUNCTION
    private static long GetStaticLongField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticLongField");
        long result;
        try {
            result = GetStaticLongField_(env, javaType, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetStaticLongField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1045
    private static long GetStaticLongField_(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor longFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readLong(javaTypeToStaticTuple(javaType), longFieldActor.offset());
    }

    @JNI_FUNCTION
    private static float GetStaticFloatField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticFloatField");
        float result;
        try {
            result = GetStaticFloatField_(env, javaType, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetStaticFloatField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1051
    private static float GetStaticFloatField_(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor floatFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readFloat(javaTypeToStaticTuple(javaType), floatFieldActor.offset());
    }

    @JNI_FUNCTION
    private static double GetStaticDoubleField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticDoubleField");
        double result;
        try {
            result = GetStaticDoubleField_(env, javaType, fieldID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetStaticDoubleField");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1057
    private static double GetStaticDoubleField_(Pointer env, JniHandle javaType, FieldID fieldID) {
        final FieldActor doubleFieldActor = FieldID.toFieldActor(fieldID);
        return TupleAccess.readDouble(javaTypeToStaticTuple(javaType), doubleFieldActor.offset());
    }

    @JNI_FUNCTION
    private static void SetStaticObjectField(Pointer env, JniHandle javaType, FieldID fieldID, JniHandle value) {
        Pointer anchor = prologue(env, "SetStaticObjectField");
        try {
            SetStaticObjectField_(env, javaType, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetStaticObjectField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1063
    private static void SetStaticObjectField_(Pointer env, JniHandle javaType, FieldID fieldID, JniHandle value) {
        final FieldActor referenceFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeObject(javaTypeToStaticTuple(javaType), referenceFieldActor.offset(), value.unhand());
    }

    @JNI_FUNCTION
    private static void SetStaticBooleanField(Pointer env, JniHandle javaType, FieldID fieldID, boolean value) {
        Pointer anchor = prologue(env, "SetStaticBooleanField");
        try {
            SetStaticBooleanField_(env, javaType, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetStaticBooleanField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1069
    private static void SetStaticBooleanField_(Pointer env, JniHandle javaType, FieldID fieldID, boolean value) {
        final FieldActor booleanFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeBoolean(javaTypeToStaticTuple(javaType), booleanFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticByteField(Pointer env, JniHandle javaType, FieldID fieldID, byte value) {
        Pointer anchor = prologue(env, "SetStaticByteField");
        try {
            SetStaticByteField_(env, javaType, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetStaticByteField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1075
    private static void SetStaticByteField_(Pointer env, JniHandle javaType, FieldID fieldID, byte value) {
        final FieldActor byteFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeByte(javaTypeToStaticTuple(javaType), byteFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticCharField(Pointer env, JniHandle javaType, FieldID fieldID, char value) {
        Pointer anchor = prologue(env, "SetStaticCharField");
        try {
            SetStaticCharField_(env, javaType, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetStaticCharField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1081
    private static void SetStaticCharField_(Pointer env, JniHandle javaType, FieldID fieldID, char value) {
        final FieldActor charFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeChar(javaTypeToStaticTuple(javaType), charFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticShortField(Pointer env, JniHandle javaType, FieldID fieldID, short value) {
        Pointer anchor = prologue(env, "SetStaticShortField");
        try {
            SetStaticShortField_(env, javaType, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetStaticShortField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1087
    private static void SetStaticShortField_(Pointer env, JniHandle javaType, FieldID fieldID, short value) {
        final FieldActor shortFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeShort(javaTypeToStaticTuple(javaType), shortFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticIntField(Pointer env, JniHandle javaType, FieldID fieldID, int value) {
        Pointer anchor = prologue(env, "SetStaticIntField");
        try {
            SetStaticIntField_(env, javaType, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetStaticIntField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1093
    private static void SetStaticIntField_(Pointer env, JniHandle javaType, FieldID fieldID, int value) {
        final FieldActor intFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeInt(javaTypeToStaticTuple(javaType), intFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticLongField(Pointer env, JniHandle javaType, FieldID fieldID, long value) {
        Pointer anchor = prologue(env, "SetStaticLongField");
        try {
            SetStaticLongField_(env, javaType, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetStaticLongField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1099
    private static void SetStaticLongField_(Pointer env, JniHandle javaType, FieldID fieldID, long value) {
        final FieldActor longFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeLong(javaTypeToStaticTuple(javaType), longFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticFloatField(Pointer env, JniHandle javaType, FieldID fieldID, float value) {
        Pointer anchor = prologue(env, "SetStaticFloatField");
        try {
            SetStaticFloatField_(env, javaType, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetStaticFloatField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1105
    private static void SetStaticFloatField_(Pointer env, JniHandle javaType, FieldID fieldID, float value) {
        final FieldActor floatFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeFloat(javaTypeToStaticTuple(javaType), floatFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static void SetStaticDoubleField(Pointer env, JniHandle javaType, FieldID fieldID, double value) {
        Pointer anchor = prologue(env, "SetStaticDoubleField");
        try {
            SetStaticDoubleField_(env, javaType, fieldID, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetStaticDoubleField");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1111
    private static void SetStaticDoubleField_(Pointer env, JniHandle javaType, FieldID fieldID, double value) {
        final FieldActor doubleFieldActor = FieldID.toFieldActor(fieldID);
        TupleAccess.writeDouble(javaTypeToStaticTuple(javaType), doubleFieldActor.offset(), value);
    }

    @JNI_FUNCTION
    private static JniHandle NewString(Pointer env, Pointer chars, int length) {
        Pointer anchor = prologue(env, "NewString");
        JniHandle result;
        try {
            result = NewString_(env, chars, length);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewString");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1117
    private static JniHandle NewString_(Pointer env, Pointer chars, int length) {
        final char[] charArray = new char[length];
        for (int i = 0; i < length; i++) {
            charArray[i] = chars.getChar(i);
        }
        return JniHandles.createLocalHandle(new String(charArray));
    }

    @JNI_FUNCTION
    private static int GetStringLength(Pointer env, JniHandle string) {
        Pointer anchor = prologue(env, "GetStringLength");
        int result;
        try {
            result = GetStringLength_(env, string);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetStringLength");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1126
    private static int GetStringLength_(Pointer env, JniHandle string) {
        return ((String) string.unhand()).length();
    }

    @JNI_FUNCTION
    private static JniHandle GetStringChars(Pointer env, JniHandle string, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetStringChars");
        JniHandle result;
        try {
            result = GetStringChars_(env, string, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "GetStringChars");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1131
    private static JniHandle GetStringChars_(Pointer env, JniHandle string, Pointer isCopy) {
        setCopyPointer(isCopy, true);
        return JniHandles.createLocalHandle(((String) string.unhand()).toCharArray());
    }

    @JNI_FUNCTION
    private static void ReleaseStringChars(Pointer env, JniHandle string, Pointer chars) {
        Pointer anchor = prologue(env, "ReleaseStringChars");
        try {
            ReleaseStringChars_(env, string, chars);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ReleaseStringChars");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1137
    private static void ReleaseStringChars_(Pointer env, JniHandle string, Pointer chars) {
        Memory.deallocate(chars);
    }

    @JNI_FUNCTION
    private static JniHandle NewStringUTF(Pointer env, Pointer utf) {
        Pointer anchor = prologue(env, "NewStringUTF");
        JniHandle result;
        try {
            result = NewStringUTF_(env, utf);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewStringUTF");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1142
    private static JniHandle NewStringUTF_(Pointer env, Pointer utf) {
        try {
            return JniHandles.createLocalHandle(CString.utf8ToJava(utf));
        } catch (Utf8Exception utf8Exception) {
            return null;
        }
    }

    @JNI_FUNCTION
    private static int GetStringUTFLength(Pointer env, JniHandle string) {
        Pointer anchor = prologue(env, "GetStringUTFLength");
        int result;
        try {
            result = GetStringUTFLength_(env, string);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetStringUTFLength");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1151
    private static int GetStringUTFLength_(Pointer env, JniHandle string) {
        return Utf8.utf8Length((String) string.unhand());
    }

    @JNI_FUNCTION
    private static Pointer GetStringUTFChars(Pointer env, JniHandle string, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetStringUTFChars");
        Pointer result;
        try {
            result = GetStringUTFChars_(env, string, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asPointer(0);
        }
        epilogue(anchor, "GetStringUTFChars");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1156
    private static Pointer GetStringUTFChars_(Pointer env, JniHandle string, Pointer isCopy) {
        setCopyPointer(isCopy, true);
        return CString.utf8FromJava((String) string.unhand());
    }

    @JNI_FUNCTION
    private static void ReleaseStringUTFChars(Pointer env, JniHandle string, Pointer chars) {
        Pointer anchor = prologue(env, "ReleaseStringUTFChars");
        try {
            ReleaseStringUTFChars_(env, string, chars);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ReleaseStringUTFChars");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1162
    private static void ReleaseStringUTFChars_(Pointer env, JniHandle string, Pointer chars) {
        Memory.deallocate(chars);
    }

    @JNI_FUNCTION
    private static int GetArrayLength(Pointer env, JniHandle array) {
        Pointer anchor = prologue(env, "GetArrayLength");
        int result;
        try {
            result = GetArrayLength_(env, array);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetArrayLength");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1167
    private static int GetArrayLength_(Pointer env, JniHandle array) {
        return Array.getLength(array.unhand());
    }

    @JNI_FUNCTION
    private static JniHandle NewObjectArray(Pointer env, int length, JniHandle elementType, JniHandle initialElementValue) {
        Pointer anchor = prologue(env, "NewObjectArray");
        JniHandle result;
        try {
            result = NewObjectArray_(env, length, elementType, initialElementValue);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewObjectArray");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1172
    private static JniHandle NewObjectArray_(Pointer env, int length, JniHandle elementType, JniHandle initialElementValue) {
        final Object array = Array.newInstance((Class) elementType.unhand(), length);
        final Object initialValue = initialElementValue.unhand();
        for (int i = 0; i < length; i++) {
            Array.set(array, i, initialValue);
        }
        return JniHandles.createLocalHandle(array);
    }

    @JNI_FUNCTION
    private static JniHandle GetObjectArrayElement(Pointer env, JniHandle array, int index) {
        Pointer anchor = prologue(env, "GetObjectArrayElement");
        JniHandle result;
        try {
            result = GetObjectArrayElement_(env, array, index);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "GetObjectArrayElement");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1182
    private static JniHandle GetObjectArrayElement_(Pointer env, JniHandle array, int index) {
        return JniHandles.createLocalHandle(((Object[]) array.unhand())[index]);
    }

    @JNI_FUNCTION
    private static void SetObjectArrayElement(Pointer env, JniHandle array, int index, JniHandle value) {
        Pointer anchor = prologue(env, "SetObjectArrayElement");
        try {
            SetObjectArrayElement_(env, array, index, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetObjectArrayElement");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1187
    private static void SetObjectArrayElement_(Pointer env, JniHandle array, int index, JniHandle value) {
        ((Object[]) array.unhand())[index] = value.unhand();
    }

    @JNI_FUNCTION
    private static JniHandle NewBooleanArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewBooleanArray");
        JniHandle result;
        try {
            result = NewBooleanArray_(env, length);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewBooleanArray");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1192
    private static JniHandle NewBooleanArray_(Pointer env, int length) {
        return JniHandles.createLocalHandle(new boolean[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewByteArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewByteArray");
        JniHandle result;
        try {
            result = NewByteArray_(env, length);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewByteArray");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1197
    private static JniHandle NewByteArray_(Pointer env, int length) {
        return JniHandles.createLocalHandle(new byte[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewCharArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewCharArray");
        JniHandle result;
        try {
            result = NewCharArray_(env, length);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewCharArray");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1202
    private static JniHandle NewCharArray_(Pointer env, int length) {
        return JniHandles.createLocalHandle(new char[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewShortArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewShortArray");
        JniHandle result;
        try {
            result = NewShortArray_(env, length);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewShortArray");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1207
    private static JniHandle NewShortArray_(Pointer env, int length) {
        return JniHandles.createLocalHandle(new short[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewIntArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewIntArray");
        JniHandle result;
        try {
            result = NewIntArray_(env, length);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewIntArray");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1212
    private static JniHandle NewIntArray_(Pointer env, int length) {
        return JniHandles.createLocalHandle(new int[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewLongArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewLongArray");
        JniHandle result;
        try {
            result = NewLongArray_(env, length);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewLongArray");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1217
    private static JniHandle NewLongArray_(Pointer env, int length) {
        return JniHandles.createLocalHandle(new long[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewFloatArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewFloatArray");
        JniHandle result;
        try {
            result = NewFloatArray_(env, length);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewFloatArray");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1222
    private static JniHandle NewFloatArray_(Pointer env, int length) {
        return JniHandles.createLocalHandle(new float[length]);
    }

    @JNI_FUNCTION
    private static JniHandle NewDoubleArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewDoubleArray");
        JniHandle result;
        try {
            result = NewDoubleArray_(env, length);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewDoubleArray");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1227
    private static JniHandle NewDoubleArray_(Pointer env, int length) {
        return JniHandles.createLocalHandle(new double[length]);
    }

    @JNI_FUNCTION
    private static Pointer GetBooleanArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetBooleanArrayElements");
        Pointer result;
        try {
            result = GetBooleanArrayElements_(env, array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asPointer(0);
        }
        epilogue(anchor, "GetBooleanArrayElements");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1232
    private static Pointer GetBooleanArrayElements_(Pointer env, JniHandle array, Pointer isCopy) {
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
        Pointer anchor = prologue(env, "GetByteArrayElements");
        Pointer result;
        try {
            result = GetByteArrayElements_(env, array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asPointer(0);
        }
        epilogue(anchor, "GetByteArrayElements");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1247
    private static Pointer GetByteArrayElements_(Pointer env, JniHandle array, Pointer isCopy) {
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
        Pointer anchor = prologue(env, "GetCharArrayElements");
        Pointer result;
        try {
            result = GetCharArrayElements_(env, array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asPointer(0);
        }
        epilogue(anchor, "GetCharArrayElements");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1262
    private static Pointer GetCharArrayElements_(Pointer env, JniHandle array, Pointer isCopy) {
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
        Pointer anchor = prologue(env, "GetShortArrayElements");
        Pointer result;
        try {
            result = GetShortArrayElements_(env, array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asPointer(0);
        }
        epilogue(anchor, "GetShortArrayElements");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1277
    private static Pointer GetShortArrayElements_(Pointer env, JniHandle array, Pointer isCopy) {
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
        Pointer anchor = prologue(env, "GetIntArrayElements");
        Pointer result;
        try {
            result = GetIntArrayElements_(env, array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asPointer(0);
        }
        epilogue(anchor, "GetIntArrayElements");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1292
    private static Pointer GetIntArrayElements_(Pointer env, JniHandle array, Pointer isCopy) {
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
        Pointer anchor = prologue(env, "GetLongArrayElements");
        Pointer result;
        try {
            result = GetLongArrayElements_(env, array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asPointer(0);
        }
        epilogue(anchor, "GetLongArrayElements");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1307
    private static Pointer GetLongArrayElements_(Pointer env, JniHandle array, Pointer isCopy) {
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
        Pointer anchor = prologue(env, "GetFloatArrayElements");
        Pointer result;
        try {
            result = GetFloatArrayElements_(env, array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asPointer(0);
        }
        epilogue(anchor, "GetFloatArrayElements");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1322
    private static Pointer GetFloatArrayElements_(Pointer env, JniHandle array, Pointer isCopy) {
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
        Pointer anchor = prologue(env, "GetDoubleArrayElements");
        Pointer result;
        try {
            result = GetDoubleArrayElements_(env, array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asPointer(0);
        }
        epilogue(anchor, "GetDoubleArrayElements");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1337
    private static Pointer GetDoubleArrayElements_(Pointer env, JniHandle array, Pointer isCopy) {
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
        Pointer anchor = prologue(env, "ReleaseBooleanArrayElements");
        try {
            ReleaseBooleanArrayElements_(env, array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ReleaseBooleanArrayElements");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1352
    private static void ReleaseBooleanArrayElements_(Pointer env, JniHandle array, Pointer elements, int mode) {
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
        Pointer anchor = prologue(env, "ReleaseByteArrayElements");
        try {
            ReleaseByteArrayElements_(env, array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ReleaseByteArrayElements");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1367
    private static void ReleaseByteArrayElements_(Pointer env, JniHandle array, Pointer elements, int mode) {
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
        Pointer anchor = prologue(env, "ReleaseCharArrayElements");
        try {
            ReleaseCharArrayElements_(env, array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ReleaseCharArrayElements");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1382
    private static void ReleaseCharArrayElements_(Pointer env, JniHandle array, Pointer elements, int mode) {
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
        Pointer anchor = prologue(env, "ReleaseShortArrayElements");
        try {
            ReleaseShortArrayElements_(env, array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ReleaseShortArrayElements");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1397
    private static void ReleaseShortArrayElements_(Pointer env, JniHandle array, Pointer elements, int mode) {
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
        Pointer anchor = prologue(env, "ReleaseIntArrayElements");
        try {
            ReleaseIntArrayElements_(env, array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ReleaseIntArrayElements");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1412
    private static void ReleaseIntArrayElements_(Pointer env, JniHandle array, Pointer elements, int mode) {
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
        Pointer anchor = prologue(env, "ReleaseLongArrayElements");
        try {
            ReleaseLongArrayElements_(env, array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ReleaseLongArrayElements");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1427
    private static void ReleaseLongArrayElements_(Pointer env, JniHandle array, Pointer elements, int mode) {
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
        Pointer anchor = prologue(env, "ReleaseFloatArrayElements");
        try {
            ReleaseFloatArrayElements_(env, array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ReleaseFloatArrayElements");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1442
    private static void ReleaseFloatArrayElements_(Pointer env, JniHandle array, Pointer elements, int mode) {
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
        Pointer anchor = prologue(env, "ReleaseDoubleArrayElements");
        try {
            ReleaseDoubleArrayElements_(env, array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ReleaseDoubleArrayElements");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1457
    private static void ReleaseDoubleArrayElements_(Pointer env, JniHandle array, Pointer elements, int mode) {
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
        Pointer anchor = prologue(env, "GetBooleanArrayRegion");
        try {
            GetBooleanArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "GetBooleanArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1472
    private static void GetBooleanArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final boolean[] a = (boolean[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setBoolean(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetByteArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetByteArrayRegion");
        try {
            GetByteArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "GetByteArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1480
    private static void GetByteArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final byte[] a = (byte[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setByte(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetCharArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetCharArrayRegion");
        try {
            GetCharArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "GetCharArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1488
    private static void GetCharArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final char[] a = (char[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setChar(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetShortArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetShortArrayRegion");
        try {
            GetShortArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "GetShortArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1496
    private static void GetShortArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final short[] a = (short[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setShort(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetIntArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetIntArrayRegion");
        try {
            GetIntArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "GetIntArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1504
    private static void GetIntArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final int[] a = (int[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setInt(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetLongArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetLongArrayRegion");
        try {
            GetLongArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "GetLongArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1512
    private static void GetLongArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final long[] a = (long[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setLong(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetFloatArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetFloatArrayRegion");
        try {
            GetFloatArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "GetFloatArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1520
    private static void GetFloatArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final float[] a = (float[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setFloat(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void GetDoubleArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetDoubleArrayRegion");
        try {
            GetDoubleArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "GetDoubleArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1528
    private static void GetDoubleArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final double[] a = (double[]) array.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setDouble(i, a[start + i]);
        }
    }

    @JNI_FUNCTION
    private static void SetBooleanArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetBooleanArrayRegion");
        try {
            SetBooleanArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetBooleanArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1536
    private static void SetBooleanArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final boolean[] a = (boolean[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getBoolean(i);
        }
    }

    @JNI_FUNCTION
    private static void SetByteArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetByteArrayRegion");
        try {
            SetByteArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetByteArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1544
    private static void SetByteArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final byte[] a = (byte[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getByte(i);
        }
    }

    @JNI_FUNCTION
    private static void SetCharArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetCharArrayRegion");
        try {
            SetCharArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetCharArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1552
    private static void SetCharArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final char[] a = (char[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getChar(i);
        }
    }

    @JNI_FUNCTION
    private static void SetShortArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetShortArrayRegion");
        try {
            SetShortArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetShortArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1560
    private static void SetShortArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final short[] a = (short[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getShort(i);
        }
    }

    @JNI_FUNCTION
    private static void SetIntArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetIntArrayRegion");
        try {
            SetIntArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetIntArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1568
    private static void SetIntArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final int[] a = (int[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getInt(i);
        }
    }

    @JNI_FUNCTION
    private static void SetLongArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetLongArrayRegion");
        try {
            SetLongArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetLongArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1576
    private static void SetLongArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final long[] a = (long[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getLong(i);
        }
    }

    @JNI_FUNCTION
    private static void SetFloatArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetFloatArrayRegion");
        try {
            SetFloatArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetFloatArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1584
    private static void SetFloatArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        final float[] a = (float[]) array.unhand();
        for (int i = 0; i < length; i++) {
            a[start + i] = buffer.getFloat(i);
        }
    }

    @JNI_FUNCTION
    private static void SetDoubleArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetDoubleArrayRegion");
        try {
            SetDoubleArrayRegion_(env, array, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "SetDoubleArrayRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1592
    private static void SetDoubleArrayRegion_(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
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
        Pointer anchor = prologue(env, "RegisterNatives");
        int result;
        try {
            result = RegisterNatives_(env, javaType, methods, numberOfMethods);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "RegisterNatives");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1608
    private static int RegisterNatives_(Pointer env, JniHandle javaType, Pointer methods, int numberOfMethods) {
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
        Pointer anchor = prologue(env, "UnregisterNatives");
        int result;
        try {
            result = UnregisterNatives_(env, javaType);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "UnregisterNatives");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1646
    private static int UnregisterNatives_(Pointer env, JniHandle javaType) {
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
        Pointer anchor = prologue(env, "MonitorEnter");
        int result;
        try {
            result = MonitorEnter_(env, object);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "MonitorEnter");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1657
    private static int MonitorEnter_(Pointer env, JniHandle object) {
        Monitor.enter(object.unhand());
        return 0;
    }

    @JNI_FUNCTION
    private static int MonitorExit(Pointer env, JniHandle object) {
        Pointer anchor = prologue(env, "MonitorExit");
        int result;
        try {
            result = MonitorExit_(env, object);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "MonitorExit");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1663
    private static int MonitorExit_(Pointer env, JniHandle object) {
        Monitor.exit(object.unhand());
        return 0;
    }

    @JNI_FUNCTION
    private static native int GetJavaVM(Pointer env, Pointer vmPointerPointer);

    @JNI_FUNCTION
    private static void GetStringRegion(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetStringRegion");
        try {
            GetStringRegion_(env, string, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "GetStringRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1672
    private static void GetStringRegion_(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        final String s = (String) string.unhand();
        for (int i = 0; i < length; i++) {
            buffer.setChar(i, s.charAt(i + start));
        }
    }

    @JNI_FUNCTION
    private static void GetStringUTFRegion(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetStringUTFRegion");
        try {
            GetStringUTFRegion_(env, string, start, length, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "GetStringUTFRegion");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1680
    private static void GetStringUTFRegion_(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        final String s = ((String) string.unhand()).substring(start, start + length);
        final byte[] utf = Utf8.stringToUtf8(s);
        Memory.writeBytes(utf, utf.length, buffer);
        buffer.setByte(utf.length, (byte) 0); // zero termination
    }

    @JNI_FUNCTION
    private static Pointer GetPrimitiveArrayCritical(Pointer env, JniHandle array, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetPrimitiveArrayCritical");
        Pointer result;
        try {
            result = GetPrimitiveArrayCritical_(env, array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asPointer(0);
        }
        epilogue(anchor, "GetPrimitiveArrayCritical");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1688
    private static Pointer GetPrimitiveArrayCritical_(Pointer env, JniHandle array, Pointer isCopy) {
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
        Pointer anchor = prologue(env, "ReleasePrimitiveArrayCritical");
        try {
            ReleasePrimitiveArrayCritical_(env, array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ReleasePrimitiveArrayCritical");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1722
    private static void ReleasePrimitiveArrayCritical_(Pointer env, JniHandle array, Pointer elements, int mode) {
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
        Pointer anchor = prologue(env, "GetStringCritical");
        Pointer result;
        try {
            result = GetStringCritical_(env, string, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asPointer(0);
        }
        epilogue(anchor, "GetStringCritical");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1755
    private static Pointer GetStringCritical_(Pointer env, JniHandle string, Pointer isCopy) {
        setCopyPointer(isCopy, true);
        final char[] a = ((String) string.unhand()).toCharArray();
        final Pointer pointer = Memory.mustAllocate(a.length * Kind.CHAR.width.numberOfBytes);
        for (int i = 0; i < a.length; i++) {
            pointer.setChar(i, a[i]);
        }
        return pointer;
    }

    @JNI_FUNCTION
    private static void ReleaseStringCritical(Pointer env, JniHandle string, final Pointer chars) {
        Pointer anchor = prologue(env, "ReleaseStringCritical");
        try {
            ReleaseStringCritical_(env, string, chars);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "ReleaseStringCritical");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1766
    private static void ReleaseStringCritical_(Pointer env, JniHandle string, final Pointer chars) {
        Memory.deallocate(chars);
    }

    @JNI_FUNCTION
    private static JniHandle NewWeakGlobalRef(Pointer env, JniHandle handle) {
        Pointer anchor = prologue(env, "NewWeakGlobalRef");
        JniHandle result;
        try {
            result = NewWeakGlobalRef_(env, handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewWeakGlobalRef");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1771
    private static JniHandle NewWeakGlobalRef_(Pointer env, JniHandle handle) {
        return JniHandles.createWeakGlobalHandle(handle.unhand());
    }

    @JNI_FUNCTION
    private static void DeleteWeakGlobalRef(Pointer env, JniHandle handle) {
        Pointer anchor = prologue(env, "DeleteWeakGlobalRef");
        try {
            DeleteWeakGlobalRef_(env, handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "DeleteWeakGlobalRef");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1776
    private static void DeleteWeakGlobalRef_(Pointer env, JniHandle handle) {
        JniHandles.destroyWeakGlobalHandle(handle);
    }

    @JNI_FUNCTION
    private static boolean ExceptionCheck(Pointer env) {
        Pointer anchor = prologue(env, "ExceptionCheck");
        boolean result;
        try {
            result = ExceptionCheck_(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = false;
        }
        epilogue(anchor, "ExceptionCheck");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1781
    private static boolean ExceptionCheck_(Pointer env) {
        return VmThread.fromJniEnv(env).pendingException() != null;
    }

    @FOLD
    private static ClassActor DirectByteBuffer() {
        return ClassActor.fromJava(Classes.forName("java.nio.DirectByteBuffer"));
    }

    @FOLD
    private static MethodActor DirectByteBufferConstructor() {
        return DirectByteBuffer().findClassMethodActor(SymbolTable.INIT, SignatureDescriptor.fromJava(void.class, long.class, int.class));
    }

    @FOLD
    private static int directByteBufferAddressFieldOffset() {
        return ClassActor.fromJava(Buffer.class).findLocalInstanceFieldActor("address").offset();
    }

    @JNI_FUNCTION
    private static JniHandle NewDirectByteBuffer(Pointer env, Pointer address, long capacity) throws Exception {
        Pointer anchor = prologue(env, "NewDirectByteBuffer");
        JniHandle result;
        try {
            result = NewDirectByteBuffer_(env, address, capacity);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asJniHandle(0);
        }
        epilogue(anchor, "NewDirectByteBuffer");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1801
    private static JniHandle NewDirectByteBuffer_(Pointer env, Pointer address, long capacity) throws Exception {
        int cap = (int) capacity;
        return JniHandles.createLocalHandle(DirectByteBufferConstructor().invokeConstructor(LongValue.from(address.toLong()), IntValue.from(cap)).asObject());
    }

    @JNI_FUNCTION
    private static Pointer GetDirectBufferAddress(Pointer env, JniHandle buffer) throws Exception {
        Pointer anchor = prologue(env, "GetDirectBufferAddress");
        Pointer result;
        try {
            result = GetDirectBufferAddress_(env, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = asPointer(0);
        }
        epilogue(anchor, "GetDirectBufferAddress");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1807
    private static Pointer GetDirectBufferAddress_(Pointer env, JniHandle buffer) throws Exception {
        Object buf = buffer.unhand();
        if (DirectByteBuffer().isInstance(buf)) {
            long address = TupleAccess.readLong(buf, directByteBufferAddressFieldOffset());
            return Pointer.fromLong(address);
        }
        return Pointer.zero();
    }

    @JNI_FUNCTION
    private static long GetDirectBufferCapacity(Pointer env, JniHandle buffer) {
        Pointer anchor = prologue(env, "GetDirectBufferCapacity");
        long result;
        try {
            result = GetDirectBufferCapacity_(env, buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetDirectBufferCapacity");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1817
    private static long GetDirectBufferCapacity_(Pointer env, JniHandle buffer) {
        Object buf = buffer.unhand();
        if (DirectByteBuffer().isInstance(buf)) {
            return ((Buffer) buf).capacity();
        }
        return -1;
    }

    @JNI_FUNCTION
    private static int GetObjectRefType(Pointer env, JniHandle obj) {
        Pointer anchor = prologue(env, "GetObjectRefType");
        int result;
        try {
            result = GetObjectRefType_(env, obj);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetObjectRefType");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1826
    private static int GetObjectRefType_(Pointer env, JniHandle obj) {
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
    private static int GetNumberOfArguments(Pointer env, MethodID methodID) throws Exception {
        Pointer anchor = prologue(env, "GetNumberOfArguments");
        int result;
        try {
            result = GetNumberOfArguments_(env, methodID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JNI_ERR;
        }
        epilogue(anchor, "GetNumberOfArguments");
        return result;
    }

    @INLINE
    // Source: JniFunctionsSource.java:1839
    private static int GetNumberOfArguments_(Pointer env, MethodID methodID) throws Exception {
        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null) {
            throw new NoSuchMethodException();
        }
        return methodActor.descriptor().numberOfParameters();
    }

    @JNI_FUNCTION
    private static void GetKindsOfArguments(Pointer env, MethodID methodID, Pointer kinds) throws Exception {
        Pointer anchor = prologue(env, "GetKindsOfArguments");
        try {
            GetKindsOfArguments_(env, methodID, kinds);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        epilogue(anchor, "GetKindsOfArguments");
    }

    @INLINE
    // Source: JniFunctionsSource.java:1848
    private static void GetKindsOfArguments_(Pointer env, MethodID methodID, Pointer kinds) throws Exception {
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
// END GENERATED CODE
}
