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

    // Source: JniFunctionsSource.java:70
    @JNI_FUNCTION
    private static native void reserved0();

    // Source: JniFunctionsSource.java:73
    @JNI_FUNCTION
    private static native void reserved1();

    // Source: JniFunctionsSource.java:76
    @JNI_FUNCTION
    private static native void reserved2();

    // Source: JniFunctionsSource.java:79
    @JNI_FUNCTION
    private static native void reserved3();

    // Checkstyle: stop method name check

    // Source: JniFunctionsSource.java:84
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

    // Source: JniFunctionsSource.java:102
    @JNI_FUNCTION
    private static JniHandle DefineClass(Pointer env, Pointer slashifiedName, JniHandle classLoader, Pointer buffer, int length) throws ClassFormatError {
        Pointer anchor = prologue(env, "DefineClass");
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "DefineClass");
        }
    }

    private static Class findClass(ClassLoader classLoader, String slashifiedName) throws ClassNotFoundException {
        if (slashifiedName.startsWith("[")) {
            TypeDescriptor descriptor = JavaTypeDescriptor.parseTypeDescriptor(slashifiedName);
            return descriptor.resolve(classLoader).toJava();
        }
        return classLoader.loadClass(dottify(slashifiedName));
    }

    // Source: JniFunctionsSource.java:134
    @JNI_FUNCTION
    private static JniHandle FindClass(Pointer env, Pointer name) throws ClassNotFoundException {
        Pointer anchor = prologue(env, "FindClass");
        try {
            String className;
            try {
                className = CString.utf8ToJava(name);
            } catch (Utf8Exception utf8Exception) {
                throw new ClassNotFoundException();
            }
            final Class javaClass = findClass(VmStackFrameWalker.getCallerClassMethodActor().holder().classLoader, className);
            MakeClassInitialized.makeClassInitialized(ClassActor.fromJava(javaClass));
            return JniHandles.createLocalHandle(javaClass);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "FindClass");
        }
    }

    // Source: JniFunctionsSource.java:147
    @JNI_FUNCTION
    private static MethodID FromReflectedMethod(Pointer env, JniHandle reflectedMethod) {
        Pointer anchor = prologue(env, "FromReflectedMethod");
        try {
            final MethodActor methodActor = MethodActor.fromJava((Method) reflectedMethod.unhand());
            return MethodID.fromMethodActor(methodActor);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asMethodID(0);
        } finally {
            epilogue(anchor, "FromReflectedMethod");
        }
    }

    // Source: JniFunctionsSource.java:153
    @JNI_FUNCTION
    private static FieldID FromReflectedField(Pointer env, JniHandle field) {
        Pointer anchor = prologue(env, "FromReflectedField");
        try {
            final FieldActor fieldActor = FieldActor.fromJava((Field) field.unhand());
            return FieldID.fromFieldActor(fieldActor);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asFieldID(0);
        } finally {
            epilogue(anchor, "FromReflectedField");
        }
    }

    private static Method ToReflectedMethod(MethodID methodID, boolean isStatic) throws NoSuchMethodException {
        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null || methodActor.isStatic() != isStatic) {
            throw new NoSuchMethodException();
        }
        return methodActor.toJava();
    }

    // Source: JniFunctionsSource.java:167
    @JNI_FUNCTION
    private static JniHandle ToReflectedMethod(Pointer env, JniHandle javaClass, MethodID methodID, boolean isStatic) throws NoSuchMethodException {
        Pointer anchor = prologue(env, "ToReflectedMethod");
        try {
            return JniHandles.createLocalHandle(ToReflectedMethod(methodID, isStatic));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "ToReflectedMethod");
        }
    }

    // Source: JniFunctionsSource.java:172
    @JNI_FUNCTION
    private static JniHandle GetSuperclass(Pointer env, JniHandle subType) {
        Pointer anchor = prologue(env, "GetSuperclass");
        try {
            return JniHandles.createLocalHandle(((Class) subType.unhand()).getSuperclass());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetSuperclass");
        }
    }

    // Source: JniFunctionsSource.java:177
    @JNI_FUNCTION
    private static boolean IsAssignableFrom(Pointer env, JniHandle subType, JniHandle superType) {
        Pointer anchor = prologue(env, "IsAssignableFrom");
        try {
            return ClassActor.fromJava((Class) superType.unhand()).isAssignableFrom(ClassActor.fromJava((Class) subType.unhand()));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return false;
        } finally {
            epilogue(anchor, "IsAssignableFrom");
        }
    }

    // Source: JniFunctionsSource.java:182
    @JNI_FUNCTION
    private static JniHandle ToReflectedField(Pointer env, JniHandle javaClass, FieldID fieldID, boolean isStatic) {
        Pointer anchor = prologue(env, "ToReflectedField");
        try {
            final FieldActor fieldActor = FieldID.toFieldActor(fieldID);
            if (fieldActor == null || fieldActor.isStatic() != isStatic) {
                throw new NoSuchFieldError();
            }
            return JniHandles.createLocalHandle(fieldActor.toJava());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "ToReflectedField");
        }
    }

    // Source: JniFunctionsSource.java:191
    @JNI_FUNCTION
    private static int Throw(Pointer env, JniHandle throwable) {
        Pointer anchor = prologue(env, "Throw");
        try {
            VmThread.fromJniEnv(env).setPendingException((Throwable) throwable.unhand());
            return JNI_OK;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "Throw");
        }
    }

    // Source: JniFunctionsSource.java:197
    @JNI_FUNCTION
    private static int ThrowNew(Pointer env, JniHandle throwableClass, Pointer message) throws Throwable {
        Pointer anchor = prologue(env, "ThrowNew");
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ThrowNew");
        }
    }

    // Source: JniFunctionsSource.java:214
    @JNI_FUNCTION
    private static JniHandle ExceptionOccurred(Pointer env) {
        Pointer anchor = prologue(env, "ExceptionOccurred");
        try {
            return JniHandles.createLocalHandle(VmThread.fromJniEnv(env).pendingException());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "ExceptionOccurred");
        }
    }

    // Source: JniFunctionsSource.java:219
    @JNI_FUNCTION
    private static void ExceptionDescribe(Pointer env) {
        Pointer anchor = prologue(env, "ExceptionDescribe");
        try {
            final Throwable exception = VmThread.fromJniEnv(env).pendingException();
            if (exception != null) {
                exception.printStackTrace();
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ExceptionDescribe");
        }
    }

    // Source: JniFunctionsSource.java:227
    @JNI_FUNCTION
    private static void ExceptionClear(Pointer env) {
        Pointer anchor = prologue(env, "ExceptionClear");
        try {
            VmThread.fromJniEnv(env).setPendingException(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ExceptionClear");
        }
    }

    // Source: JniFunctionsSource.java:232
    @JNI_FUNCTION
    private static void FatalError(Pointer env, Pointer message) {
        Pointer anchor = prologue(env, "FatalError");
        try {
            try {
                FatalError.unexpected(CString.utf8ToJava(message));
            } catch (Utf8Exception utf8Exception) {
                FatalError.unexpected("fatal error with UTF8 error in message");
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "FatalError");
        }
    }

    private static int PushLocalFrame0(Pointer env, int capacity) {
        JniHandles.pushLocalFrame(capacity);
        return JNI_OK;
    }

    // Source: JniFunctionsSource.java:246
    @JNI_FUNCTION
    private static int PushLocalFrame(Pointer env, int capacity) {
        Pointer anchor = prologue(env, "PushLocalFrame");
        try {
            JniHandles.pushLocalFrame(capacity);
            return JNI_OK;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "PushLocalFrame");
        }
    }

    // Source: JniFunctionsSource.java:252
    @JNI_FUNCTION
    private static JniHandle PopLocalFrame(Pointer env, JniHandle result) {
        Pointer anchor = prologue(env, "PopLocalFrame");
        try {
            return JniHandles.popLocalFrame(result);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "PopLocalFrame");
        }
    }

    // Source: JniFunctionsSource.java:257
    @JNI_FUNCTION
    private static JniHandle NewGlobalRef(Pointer env, JniHandle handle) {
        Pointer anchor = prologue(env, "NewGlobalRef");
        try {
            return JniHandles.createGlobalHandle(handle.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewGlobalRef");
        }
    }

    // Source: JniFunctionsSource.java:262
    @JNI_FUNCTION
    private static void DeleteGlobalRef(Pointer env, JniHandle handle) {
        Pointer anchor = prologue(env, "DeleteGlobalRef");
        try {
            JniHandles.destroyGlobalHandle(handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "DeleteGlobalRef");
        }
    }

    // Source: JniFunctionsSource.java:267
    @JNI_FUNCTION
    private static void DeleteLocalRef(Pointer env, JniHandle handle) {
        Pointer anchor = prologue(env, "DeleteLocalRef");
        try {
            JniHandles.destroyLocalHandle(handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "DeleteLocalRef");
        }
    }

    // Source: JniFunctionsSource.java:272
    @JNI_FUNCTION
    private static boolean IsSameObject(Pointer env, JniHandle object1, JniHandle object2) {
        Pointer anchor = prologue(env, "IsSameObject");
        try {
            return object1.unhand() == object2.unhand();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return false;
        } finally {
            epilogue(anchor, "IsSameObject");
        }
    }

    // Source: JniFunctionsSource.java:277
    @JNI_FUNCTION
    private static JniHandle NewLocalRef(Pointer env, JniHandle object) {
        Pointer anchor = prologue(env, "NewLocalRef");
        try {
            return JniHandles.createLocalHandle(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewLocalRef");
        }
    }

    // Source: JniFunctionsSource.java:282
    @JNI_FUNCTION
    private static int EnsureLocalCapacity(Pointer env, int capacity) {
        Pointer anchor = prologue(env, "EnsureLocalCapacity");
        try {
            // If this call fails, it will be with an OutOfMemoryError which will be
            // set as the pending exception for the current thread
            JniHandles.ensureLocalHandleCapacity(capacity);
            return JNI_OK;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "EnsureLocalCapacity");
        }
    }

    private static Object allocObject(Class javaClass) throws InstantiationException {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        if (classActor.isTupleClassActor() && !classActor.isAbstract()) {
            return JniHandles.createLocalHandle(Heap.createTuple(classActor.dynamicHub()));
        }
        throw new InstantiationException();
    }

    // Source: JniFunctionsSource.java:298
    @JNI_FUNCTION
    private static JniHandle AllocObject(Pointer env, JniHandle javaClass) throws InstantiationException {
        Pointer anchor = prologue(env, "AllocObject");
        try {
            return JniHandles.createLocalHandle(allocObject((Class) javaClass.unhand()));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "AllocObject");
        }
    }

    // Source: JniFunctionsSource.java:303
    @JNI_FUNCTION
    private static native JniHandle NewObject(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:306
    @JNI_FUNCTION
    private static native JniHandle NewObjectV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:309
    @JNI_FUNCTION
    private static JniHandle NewObjectA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "NewObjectA");
        try {
    
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewObjectA");
        }
    }

    // Source: JniFunctionsSource.java:334
    @JNI_FUNCTION
    private static JniHandle GetObjectClass(Pointer env, JniHandle object) {
        Pointer anchor = prologue(env, "GetObjectClass");
        try {
            final Class javaClass = object.unhand().getClass();
            return JniHandles.createLocalHandle(javaClass);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetObjectClass");
        }
    }

    // Source: JniFunctionsSource.java:340
    @JNI_FUNCTION
    private static boolean IsInstanceOf(Pointer env, JniHandle object, JniHandle javaType) {
        Pointer anchor = prologue(env, "IsInstanceOf");
        try {
            return ((Class) javaType.unhand()).isInstance(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return false;
        } finally {
            epilogue(anchor, "IsInstanceOf");
        }
    }

    // Source: JniFunctionsSource.java:345
    @JNI_FUNCTION
    private static MethodID GetMethodID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        Pointer anchor = prologue(env, "GetMethodID");
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asMethodID(0);
        } finally {
            epilogue(anchor, "GetMethodID");
        }
    }

    // Source: JniFunctionsSource.java:368
    @JNI_FUNCTION
    private static native JniHandle CallObjectMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:371
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

    // Source: JniFunctionsSource.java:456
    @JNI_FUNCTION
    private static JniHandle CallObjectMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallObjectMethodA");
        try {
            return JniHandles.createLocalHandle(CallValueMethodA(env, object, methodID, arguments).asObject());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "CallObjectMethodA");
        }
    }

    // Source: JniFunctionsSource.java:461
    @JNI_FUNCTION
    private static native boolean CallBooleanMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:464
    @JNI_FUNCTION
    private static native boolean CallBooleanMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer vaList);

    // Source: JniFunctionsSource.java:467
    @JNI_FUNCTION
    private static boolean CallBooleanMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallBooleanMethodA");
        try {
            return CallValueMethodA(env, object, methodID, arguments).asBoolean();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return false;
        } finally {
            epilogue(anchor, "CallBooleanMethodA");
        }
    }

    // Source: JniFunctionsSource.java:472
    @JNI_FUNCTION
    private static native byte CallByteMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:475
    @JNI_FUNCTION
    private static native byte CallByteMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:478
    @JNI_FUNCTION
    private static byte CallByteMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallByteMethodA");
        try {
            return CallValueMethodA(env, object, methodID, arguments).asByte();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallByteMethodA");
        }
    }

    // Source: JniFunctionsSource.java:483
    @JNI_FUNCTION
    private static native char CallCharMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:486
    @JNI_FUNCTION
    private static native char CallCharMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:489
    @JNI_FUNCTION
    private static char CallCharMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallCharMethodA");
        try {
            return CallValueMethodA(env, object, methodID, arguments).asChar();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return (char) JNI_ERR;
        } finally {
            epilogue(anchor, "CallCharMethodA");
        }
    }

    // Source: JniFunctionsSource.java:494
    @JNI_FUNCTION
    private static native short CallShortMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:497
    @JNI_FUNCTION
    private static native short CallShortMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:500
    @JNI_FUNCTION
    private static short CallShortMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallShortMethodA");
        try {
            return CallValueMethodA(env, object, methodID, arguments).asShort();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallShortMethodA");
        }
    }

    // Source: JniFunctionsSource.java:505
    @JNI_FUNCTION
    private static native int CallIntMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:508
    @JNI_FUNCTION
    private static native int CallIntMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:511
    @JNI_FUNCTION
    private static int CallIntMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallIntMethodA");
        try {
            return CallValueMethodA(env, object, methodID, arguments).asInt();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallIntMethodA");
        }
    }

    // Source: JniFunctionsSource.java:516
    @JNI_FUNCTION
    private static native long CallLongMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:519
    @JNI_FUNCTION
    private static native long CallLongMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:522
    @JNI_FUNCTION
    private static long CallLongMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallLongMethodA");
        try {
            return CallValueMethodA(env, object, methodID, arguments).asLong();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallLongMethodA");
        }
    }

    // Source: JniFunctionsSource.java:527
    @JNI_FUNCTION
    private static native float CallFloatMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:530
    @JNI_FUNCTION
    private static native float CallFloatMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:533
    @JNI_FUNCTION
    private static float CallFloatMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallFloatMethodA");
        try {
            return CallValueMethodA(env, object, methodID, arguments).asFloat();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallFloatMethodA");
        }
    }

    // Source: JniFunctionsSource.java:538
    @JNI_FUNCTION
    private static native double CallDoubleMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:541
    @JNI_FUNCTION
    private static native double CallDoubleMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:544
    @JNI_FUNCTION
    private static double CallDoubleMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallDoubleMethodA");
        try {
            return CallValueMethodA(env, object, methodID, arguments).asDouble();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallDoubleMethodA");
        }
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

    // Source: JniFunctionsSource.java:573
    @JNI_FUNCTION
    private static native void CallVoidMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:576
    @JNI_FUNCTION
    private static native void CallVoidMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:579
    @JNI_FUNCTION
    private static void CallVoidMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallVoidMethodA");
        try {
            CallValueMethodA(env, object, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "CallVoidMethodA");
        }
    }

    // Source: JniFunctionsSource.java:584
    @JNI_FUNCTION
    private static native JniHandle CallNonvirtualObjectMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    // Source: JniFunctionsSource.java:587
    @JNI_FUNCTION
    private static native JniHandle CallNonvirtualObjectMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    // Source: JniFunctionsSource.java:590
    @JNI_FUNCTION
    private static JniHandle CallNonvirtualObjectMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualObjectMethodA");
        try {
            return JniHandles.createLocalHandle(CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asObject());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "CallNonvirtualObjectMethodA");
        }
    }

    // Source: JniFunctionsSource.java:595
    @JNI_FUNCTION
    private static native boolean CallNonvirtualBooleanMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    // Source: JniFunctionsSource.java:598
    @JNI_FUNCTION
    private static native boolean CallNonvirtualBooleanMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    // Source: JniFunctionsSource.java:601
    @JNI_FUNCTION
    private static boolean CallNonvirtualBooleanMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualBooleanMethodA");
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asBoolean();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return false;
        } finally {
            epilogue(anchor, "CallNonvirtualBooleanMethodA");
        }
    }

    // Source: JniFunctionsSource.java:606
    @JNI_FUNCTION
    private static native byte CallNonvirtualByteMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    // Source: JniFunctionsSource.java:609
    @JNI_FUNCTION
    private static native byte CallNonvirtualByteMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    // Source: JniFunctionsSource.java:612
    @JNI_FUNCTION
    private static byte CallNonvirtualByteMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualByteMethodA");
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asByte();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallNonvirtualByteMethodA");
        }
    }

    // Source: JniFunctionsSource.java:617
    @JNI_FUNCTION
    private static native char CallNonvirtualCharMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    // Source: JniFunctionsSource.java:620
    @JNI_FUNCTION
    private static native char CallNonvirtualCharMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    // Source: JniFunctionsSource.java:623
    @JNI_FUNCTION
    private static char CallNonvirtualCharMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualCharMethodA");
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asChar();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return (char) JNI_ERR;
        } finally {
            epilogue(anchor, "CallNonvirtualCharMethodA");
        }
    }

    // Source: JniFunctionsSource.java:628
    @JNI_FUNCTION
    private static native short CallNonvirtualShortMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    // Source: JniFunctionsSource.java:631
    @JNI_FUNCTION
    private static native short CallNonvirtualShortMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    // Source: JniFunctionsSource.java:634
    @JNI_FUNCTION
    private static short CallNonvirtualShortMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualShortMethodA");
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asShort();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallNonvirtualShortMethodA");
        }
    }

    // Source: JniFunctionsSource.java:639
    @JNI_FUNCTION
    private static native int CallNonvirtualIntMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    // Source: JniFunctionsSource.java:642
    @JNI_FUNCTION
    private static native int CallNonvirtualIntMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    // Source: JniFunctionsSource.java:645
    @JNI_FUNCTION
    private static int CallNonvirtualIntMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualIntMethodA");
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asInt();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallNonvirtualIntMethodA");
        }
    }

    // Source: JniFunctionsSource.java:650
    @JNI_FUNCTION
    private static native long CallNonvirtualLongMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    // Source: JniFunctionsSource.java:653
    @JNI_FUNCTION
    private static native long CallNonvirtualLongMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    // Source: JniFunctionsSource.java:656
    @JNI_FUNCTION
    private static long CallNonvirtualLongMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualLongMethodA");
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asLong();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallNonvirtualLongMethodA");
        }
    }

    // Source: JniFunctionsSource.java:661
    @JNI_FUNCTION
    private static native float CallNonvirtualFloatMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    // Source: JniFunctionsSource.java:664
    @JNI_FUNCTION
    private static native float CallNonvirtualFloatMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    // Source: JniFunctionsSource.java:667
    @JNI_FUNCTION
    private static float CallNonvirtualFloatMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualFloatMethodA");
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asFloat();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallNonvirtualFloatMethodA");
        }
    }

    // Source: JniFunctionsSource.java:672
    @JNI_FUNCTION
    private static native double CallNonvirtualDoubleMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    // Source: JniFunctionsSource.java:675
    @JNI_FUNCTION
    private static native double CallNonvirtualDoubleMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    // Source: JniFunctionsSource.java:678
    @JNI_FUNCTION
    private static double CallNonvirtualDoubleMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualDoubleMethodA");
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments).asDouble();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallNonvirtualDoubleMethodA");
        }
    }

    // Source: JniFunctionsSource.java:683
    @JNI_FUNCTION
    private static native void CallNonvirtualVoidMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);

    // Source: JniFunctionsSource.java:686
    @JNI_FUNCTION
    private static native void CallNonvirtualVoidMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);

    // Source: JniFunctionsSource.java:689
    @JNI_FUNCTION
    private static void CallNonvirtualVoidMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallNonvirtualVoidMethodA");
        try {
            CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "CallNonvirtualVoidMethodA");
        }
    }

    // Source: JniFunctionsSource.java:694
    @JNI_FUNCTION
    private static FieldID GetFieldID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        Pointer anchor = prologue(env, "GetFieldID");
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asFieldID(0);
        } finally {
            epilogue(anchor, "GetFieldID");
        }
    }

    // Source: JniFunctionsSource.java:718
    @JNI_FUNCTION
    private static JniHandle GetObjectField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetObjectField");
        try {
            final FieldActor fieldActor = FieldID.toFieldActor(fieldID);
            return JniHandles.createLocalHandle(TupleAccess.readObject(object.unhand(), fieldActor.offset()));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetObjectField");
        }
    }

    // Source: JniFunctionsSource.java:724
    @JNI_FUNCTION
    private static boolean GetBooleanField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetBooleanField");
        try {
            final FieldActor booleanFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readBoolean(object.unhand(), booleanFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return false;
        } finally {
            epilogue(anchor, "GetBooleanField");
        }
    }

    // Source: JniFunctionsSource.java:730
    @JNI_FUNCTION
    private static byte GetByteField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetByteField");
        try {
            final FieldActor byteFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readByte(object.unhand(), byteFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetByteField");
        }
    }

    // Source: JniFunctionsSource.java:736
    @JNI_FUNCTION
    private static char GetCharField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetCharField");
        try {
            final FieldActor charFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readChar(object.unhand(), charFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return (char) JNI_ERR;
        } finally {
            epilogue(anchor, "GetCharField");
        }
    }

    // Source: JniFunctionsSource.java:742
    @JNI_FUNCTION
    private static short GetShortField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetShortField");
        try {
            final FieldActor shortFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readShort(object.unhand(), shortFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetShortField");
        }
    }

    // Source: JniFunctionsSource.java:748
    @JNI_FUNCTION
    private static int GetIntField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetIntField");
        try {
            final FieldActor intFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readInt(object.unhand(), intFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetIntField");
        }
    }

    // Source: JniFunctionsSource.java:754
    @JNI_FUNCTION
    private static long GetLongField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetLongField");
        try {
            final FieldActor longFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readLong(object.unhand(), longFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetLongField");
        }
    }

    // Source: JniFunctionsSource.java:760
    @JNI_FUNCTION
    private static float GetFloatField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetFloatField");
        try {
            final FieldActor floatFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readFloat(object.unhand(), floatFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetFloatField");
        }
    }

    // Source: JniFunctionsSource.java:766
    @JNI_FUNCTION
    private static double GetDoubleField(Pointer env, JniHandle object, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetDoubleField");
        try {
            final FieldActor doubleFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readDouble(object.unhand(), doubleFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetDoubleField");
        }
    }

    // Source: JniFunctionsSource.java:772
    @JNI_FUNCTION
    private static void SetObjectField(Pointer env, JniHandle object, FieldID fieldID, JniHandle value) {
        Pointer anchor = prologue(env, "SetObjectField");
        try {
            final FieldActor referenceFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeObject(object.unhand(), referenceFieldActor.offset(), value.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetObjectField");
        }
    }

    // Source: JniFunctionsSource.java:778
    @JNI_FUNCTION
    private static void SetBooleanField(Pointer env, JniHandle object, FieldID fieldID, boolean value) {
        Pointer anchor = prologue(env, "SetBooleanField");
        try {
            final FieldActor booleanFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeBoolean(object.unhand(), booleanFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetBooleanField");
        }
    }

    // Source: JniFunctionsSource.java:784
    @JNI_FUNCTION
    private static void SetByteField(Pointer env, JniHandle object, FieldID fieldID, byte value) {
        Pointer anchor = prologue(env, "SetByteField");
        try {
            final FieldActor byteFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeByte(object.unhand(), byteFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetByteField");
        }
    }

    // Source: JniFunctionsSource.java:790
    @JNI_FUNCTION
    private static void SetCharField(Pointer env, JniHandle object, FieldID fieldID, char value) {
        Pointer anchor = prologue(env, "SetCharField");
        try {
            final FieldActor charFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeChar(object.unhand(), charFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetCharField");
        }
    }

    // Source: JniFunctionsSource.java:796
    @JNI_FUNCTION
    private static void SetShortField(Pointer env, JniHandle object, FieldID fieldID, short value) {
        Pointer anchor = prologue(env, "SetShortField");
        try {
            final FieldActor shortFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeShort(object.unhand(), shortFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetShortField");
        }
    }

    // Source: JniFunctionsSource.java:802
    @JNI_FUNCTION
    private static void SetIntField(Pointer env, JniHandle object, FieldID fieldID, int value) {
        Pointer anchor = prologue(env, "SetIntField");
        try {
            final FieldActor intFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeInt(object.unhand(), intFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetIntField");
        }
    }

    // Source: JniFunctionsSource.java:808
    @JNI_FUNCTION
    private static void SetLongField(Pointer env, JniHandle object, FieldID fieldID, long value) {
        Pointer anchor = prologue(env, "SetLongField");
        try {
            final FieldActor longFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeLong(object.unhand(), longFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetLongField");
        }
    }

    // Source: JniFunctionsSource.java:814
    @JNI_FUNCTION
    private static void SetFloatField(Pointer env, JniHandle object, FieldID fieldID, float value) {
        Pointer anchor = prologue(env, "SetFloatField");
        try {
            final FieldActor floatFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeFloat(object.unhand(), floatFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetFloatField");
        }
    }

    // Source: JniFunctionsSource.java:820
    @JNI_FUNCTION
    private static void SetDoubleField(Pointer env, JniHandle object, FieldID fieldID, double value) {
        Pointer anchor = prologue(env, "SetDoubleField");
        try {
            final FieldActor doubleFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeDouble(object.unhand(), doubleFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetDoubleField");
        }
    }

    // Source: JniFunctionsSource.java:826
    @JNI_FUNCTION
    private static MethodID GetStaticMethodID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        Pointer anchor = prologue(env, "GetStaticMethodID");
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asMethodID(0);
        } finally {
            epilogue(anchor, "GetStaticMethodID");
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

    // Source: JniFunctionsSource.java:870
    @JNI_FUNCTION
    private static native JniHandle CallStaticObjectMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:873
    @JNI_FUNCTION
    private static native JniHandle CallStaticObjectMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:876
    @JNI_FUNCTION
    private static JniHandle CallStaticObjectMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticObjectMethodA");
        try {
            return JniHandles.createLocalHandle(CallStaticValueMethodA(env, javaClass, methodID, arguments).asObject());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "CallStaticObjectMethodA");
        }
    }

    // Source: JniFunctionsSource.java:881
    @JNI_FUNCTION
    private static native boolean CallStaticBooleanMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:884
    @JNI_FUNCTION
    private static native boolean CallStaticBooleanMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:887
    @JNI_FUNCTION
    private static boolean CallStaticBooleanMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticBooleanMethodA");
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments).asBoolean();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return false;
        } finally {
            epilogue(anchor, "CallStaticBooleanMethodA");
        }
    }

    // Source: JniFunctionsSource.java:892
    @JNI_FUNCTION
    private static native byte CallStaticByteMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:895
    @JNI_FUNCTION
    private static native byte CallStaticByteMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:898
    @JNI_FUNCTION
    private static byte CallStaticByteMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticByteMethodA");
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments).asByte();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallStaticByteMethodA");
        }
    }

    // Source: JniFunctionsSource.java:903
    @JNI_FUNCTION
    private static native char CallStaticCharMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:906
    @JNI_FUNCTION
    private static native char CallStaticCharMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:909
    @JNI_FUNCTION
    private static char CallStaticCharMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticCharMethodA");
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments).asChar();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return (char) JNI_ERR;
        } finally {
            epilogue(anchor, "CallStaticCharMethodA");
        }
    }

    // Source: JniFunctionsSource.java:914
    @JNI_FUNCTION
    private static native short CallStaticShortMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:917
    @JNI_FUNCTION
    private static native short CallStaticShortMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:920
    @JNI_FUNCTION
    private static short CallStaticShortMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticShortMethodA");
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments).asShort();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallStaticShortMethodA");
        }
    }

    // Source: JniFunctionsSource.java:925
    @JNI_FUNCTION
    private static native int CallStaticIntMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:928
    @JNI_FUNCTION
    private static native int CallStaticIntMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:931
    @JNI_FUNCTION
    private static int CallStaticIntMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticIntMethodA");
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments).asInt();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallStaticIntMethodA");
        }
    }

    // Source: JniFunctionsSource.java:936
    @JNI_FUNCTION
    private static native long CallStaticLongMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:939
    @JNI_FUNCTION
    private static native long CallStaticLongMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:942
    @JNI_FUNCTION
    private static long CallStaticLongMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticLongMethodA");
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments).asLong();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallStaticLongMethodA");
        }
    }

    // Source: JniFunctionsSource.java:947
    @JNI_FUNCTION
    private static native float CallStaticFloatMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:950
    @JNI_FUNCTION
    private static native float CallStaticFloatMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:953
    @JNI_FUNCTION
    private static float CallStaticFloatMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticFloatMethodA");
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments).asFloat();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallStaticFloatMethodA");
        }
    }

    // Source: JniFunctionsSource.java:958
    @JNI_FUNCTION
    private static native double CallStaticDoubleMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:961
    @JNI_FUNCTION
    private static native double CallStaticDoubleMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:964
    @JNI_FUNCTION
    private static double CallStaticDoubleMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticDoubleMethodA");
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments).asDouble();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CallStaticDoubleMethodA");
        }
    }

    // Source: JniFunctionsSource.java:969
    @JNI_FUNCTION
    private static native void CallStaticVoidMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);

    // Source: JniFunctionsSource.java:972
    @JNI_FUNCTION
    private static native void CallStaticVoidMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);

    // Source: JniFunctionsSource.java:975
    @JNI_FUNCTION
    private static void CallStaticVoidMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        Pointer anchor = prologue(env, "CallStaticVoidMethodA");
        try {
            CallStaticValueMethodA(env, javaClass, methodID, arguments);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "CallStaticVoidMethodA");
        }
    }

    // Source: JniFunctionsSource.java:980
    @JNI_FUNCTION
    private static FieldID GetStaticFieldID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        Pointer anchor = prologue(env, "GetStaticFieldID");
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asFieldID(0);
        } finally {
            epilogue(anchor, "GetStaticFieldID");
        }
    }

    private static Object javaTypeToStaticTuple(JniHandle javaType) {
        final TupleClassActor tupleClassActor = (TupleClassActor) ClassActor.fromJava((Class) javaType.unhand());
        return tupleClassActor.staticTuple();
    }

    // Source: JniFunctionsSource.java:1008
    @JNI_FUNCTION
    private static JniHandle GetStaticObjectField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticObjectField");
        try {
            final FieldActor referenceFieldActor = FieldID.toFieldActor(fieldID);
            return JniHandles.createLocalHandle(TupleAccess.readObject(javaTypeToStaticTuple(javaType), referenceFieldActor.offset()));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetStaticObjectField");
        }
    }

    // Source: JniFunctionsSource.java:1014
    @JNI_FUNCTION
    private static boolean GetStaticBooleanField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticBooleanField");
        try {
            final FieldActor booleanFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readBoolean(javaTypeToStaticTuple(javaType), booleanFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return false;
        } finally {
            epilogue(anchor, "GetStaticBooleanField");
        }
    }

    // Source: JniFunctionsSource.java:1020
    @JNI_FUNCTION
    private static byte GetStaticByteField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticByteField");
        try {
            final FieldActor byteFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readByte(javaTypeToStaticTuple(javaType), byteFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetStaticByteField");
        }
    }

    // Source: JniFunctionsSource.java:1026
    @JNI_FUNCTION
    private static char GetStaticCharField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticCharField");
        try {
            final FieldActor charFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readChar(javaTypeToStaticTuple(javaType), charFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return (char) JNI_ERR;
        } finally {
            epilogue(anchor, "GetStaticCharField");
        }
    }

    // Source: JniFunctionsSource.java:1032
    @JNI_FUNCTION
    private static short GetStaticShortField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticShortField");
        try {
            final FieldActor shortFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readShort(javaTypeToStaticTuple(javaType), shortFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetStaticShortField");
        }
    }

    // Source: JniFunctionsSource.java:1038
    @JNI_FUNCTION
    private static int GetStaticIntField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticIntField");
        try {
            final FieldActor intFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readInt(javaTypeToStaticTuple(javaType), intFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetStaticIntField");
        }
    }

    // Source: JniFunctionsSource.java:1044
    @JNI_FUNCTION
    private static long GetStaticLongField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticLongField");
        try {
            final FieldActor longFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readLong(javaTypeToStaticTuple(javaType), longFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetStaticLongField");
        }
    }

    // Source: JniFunctionsSource.java:1050
    @JNI_FUNCTION
    private static float GetStaticFloatField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticFloatField");
        try {
            final FieldActor floatFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readFloat(javaTypeToStaticTuple(javaType), floatFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetStaticFloatField");
        }
    }

    // Source: JniFunctionsSource.java:1056
    @JNI_FUNCTION
    private static double GetStaticDoubleField(Pointer env, JniHandle javaType, FieldID fieldID) {
        Pointer anchor = prologue(env, "GetStaticDoubleField");
        try {
            final FieldActor doubleFieldActor = FieldID.toFieldActor(fieldID);
            return TupleAccess.readDouble(javaTypeToStaticTuple(javaType), doubleFieldActor.offset());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetStaticDoubleField");
        }
    }

    // Source: JniFunctionsSource.java:1062
    @JNI_FUNCTION
    private static void SetStaticObjectField(Pointer env, JniHandle javaType, FieldID fieldID, JniHandle value) {
        Pointer anchor = prologue(env, "SetStaticObjectField");
        try {
            final FieldActor referenceFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeObject(javaTypeToStaticTuple(javaType), referenceFieldActor.offset(), value.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetStaticObjectField");
        }
    }

    // Source: JniFunctionsSource.java:1068
    @JNI_FUNCTION
    private static void SetStaticBooleanField(Pointer env, JniHandle javaType, FieldID fieldID, boolean value) {
        Pointer anchor = prologue(env, "SetStaticBooleanField");
        try {
            final FieldActor booleanFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeBoolean(javaTypeToStaticTuple(javaType), booleanFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetStaticBooleanField");
        }
    }

    // Source: JniFunctionsSource.java:1074
    @JNI_FUNCTION
    private static void SetStaticByteField(Pointer env, JniHandle javaType, FieldID fieldID, byte value) {
        Pointer anchor = prologue(env, "SetStaticByteField");
        try {
            final FieldActor byteFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeByte(javaTypeToStaticTuple(javaType), byteFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetStaticByteField");
        }
    }

    // Source: JniFunctionsSource.java:1080
    @JNI_FUNCTION
    private static void SetStaticCharField(Pointer env, JniHandle javaType, FieldID fieldID, char value) {
        Pointer anchor = prologue(env, "SetStaticCharField");
        try {
            final FieldActor charFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeChar(javaTypeToStaticTuple(javaType), charFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetStaticCharField");
        }
    }

    // Source: JniFunctionsSource.java:1086
    @JNI_FUNCTION
    private static void SetStaticShortField(Pointer env, JniHandle javaType, FieldID fieldID, short value) {
        Pointer anchor = prologue(env, "SetStaticShortField");
        try {
            final FieldActor shortFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeShort(javaTypeToStaticTuple(javaType), shortFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetStaticShortField");
        }
    }

    // Source: JniFunctionsSource.java:1092
    @JNI_FUNCTION
    private static void SetStaticIntField(Pointer env, JniHandle javaType, FieldID fieldID, int value) {
        Pointer anchor = prologue(env, "SetStaticIntField");
        try {
            final FieldActor intFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeInt(javaTypeToStaticTuple(javaType), intFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetStaticIntField");
        }
    }

    // Source: JniFunctionsSource.java:1098
    @JNI_FUNCTION
    private static void SetStaticLongField(Pointer env, JniHandle javaType, FieldID fieldID, long value) {
        Pointer anchor = prologue(env, "SetStaticLongField");
        try {
            final FieldActor longFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeLong(javaTypeToStaticTuple(javaType), longFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetStaticLongField");
        }
    }

    // Source: JniFunctionsSource.java:1104
    @JNI_FUNCTION
    private static void SetStaticFloatField(Pointer env, JniHandle javaType, FieldID fieldID, float value) {
        Pointer anchor = prologue(env, "SetStaticFloatField");
        try {
            final FieldActor floatFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeFloat(javaTypeToStaticTuple(javaType), floatFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetStaticFloatField");
        }
    }

    // Source: JniFunctionsSource.java:1110
    @JNI_FUNCTION
    private static void SetStaticDoubleField(Pointer env, JniHandle javaType, FieldID fieldID, double value) {
        Pointer anchor = prologue(env, "SetStaticDoubleField");
        try {
            final FieldActor doubleFieldActor = FieldID.toFieldActor(fieldID);
            TupleAccess.writeDouble(javaTypeToStaticTuple(javaType), doubleFieldActor.offset(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetStaticDoubleField");
        }
    }

    // Source: JniFunctionsSource.java:1116
    @JNI_FUNCTION
    private static JniHandle NewString(Pointer env, Pointer chars, int length) {
        Pointer anchor = prologue(env, "NewString");
        try {
            final char[] charArray = new char[length];
            for (int i = 0; i < length; i++) {
                charArray[i] = chars.getChar(i);
            }
            return JniHandles.createLocalHandle(new String(charArray));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewString");
        }
    }

    // Source: JniFunctionsSource.java:1125
    @JNI_FUNCTION
    private static int GetStringLength(Pointer env, JniHandle string) {
        Pointer anchor = prologue(env, "GetStringLength");
        try {
            return ((String) string.unhand()).length();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetStringLength");
        }
    }

    // Source: JniFunctionsSource.java:1130
    @JNI_FUNCTION
    private static JniHandle GetStringChars(Pointer env, JniHandle string, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetStringChars");
        try {
            setCopyPointer(isCopy, true);
            return JniHandles.createLocalHandle(((String) string.unhand()).toCharArray());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetStringChars");
        }
    }

    // Source: JniFunctionsSource.java:1136
    @JNI_FUNCTION
    private static void ReleaseStringChars(Pointer env, JniHandle string, Pointer chars) {
        Pointer anchor = prologue(env, "ReleaseStringChars");
        try {
            Memory.deallocate(chars);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ReleaseStringChars");
        }
    }

    // Source: JniFunctionsSource.java:1141
    @JNI_FUNCTION
    private static JniHandle NewStringUTF(Pointer env, Pointer utf) {
        Pointer anchor = prologue(env, "NewStringUTF");
        try {
            try {
                return JniHandles.createLocalHandle(CString.utf8ToJava(utf));
            } catch (Utf8Exception utf8Exception) {
                return null;
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewStringUTF");
        }
    }

    // Source: JniFunctionsSource.java:1150
    @JNI_FUNCTION
    private static int GetStringUTFLength(Pointer env, JniHandle string) {
        Pointer anchor = prologue(env, "GetStringUTFLength");
        try {
            return Utf8.utf8Length((String) string.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetStringUTFLength");
        }
    }

    // Source: JniFunctionsSource.java:1155
    @JNI_FUNCTION
    private static Pointer GetStringUTFChars(Pointer env, JniHandle string, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetStringUTFChars");
        try {
            setCopyPointer(isCopy, true);
            return CString.utf8FromJava((String) string.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor, "GetStringUTFChars");
        }
    }

    // Source: JniFunctionsSource.java:1161
    @JNI_FUNCTION
    private static void ReleaseStringUTFChars(Pointer env, JniHandle string, Pointer chars) {
        Pointer anchor = prologue(env, "ReleaseStringUTFChars");
        try {
            Memory.deallocate(chars);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ReleaseStringUTFChars");
        }
    }

    // Source: JniFunctionsSource.java:1166
    @JNI_FUNCTION
    private static int GetArrayLength(Pointer env, JniHandle array) {
        Pointer anchor = prologue(env, "GetArrayLength");
        try {
            return Array.getLength(array.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetArrayLength");
        }
    }

    // Source: JniFunctionsSource.java:1171
    @JNI_FUNCTION
    private static JniHandle NewObjectArray(Pointer env, int length, JniHandle elementType, JniHandle initialElementValue) {
        Pointer anchor = prologue(env, "NewObjectArray");
        try {
            final Object array = Array.newInstance((Class) elementType.unhand(), length);
            final Object initialValue = initialElementValue.unhand();
            for (int i = 0; i < length; i++) {
                Array.set(array, i, initialValue);
            }
            return JniHandles.createLocalHandle(array);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewObjectArray");
        }
    }

    // Source: JniFunctionsSource.java:1181
    @JNI_FUNCTION
    private static JniHandle GetObjectArrayElement(Pointer env, JniHandle array, int index) {
        Pointer anchor = prologue(env, "GetObjectArrayElement");
        try {
            return JniHandles.createLocalHandle(((Object[]) array.unhand())[index]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetObjectArrayElement");
        }
    }

    // Source: JniFunctionsSource.java:1186
    @JNI_FUNCTION
    private static void SetObjectArrayElement(Pointer env, JniHandle array, int index, JniHandle value) {
        Pointer anchor = prologue(env, "SetObjectArrayElement");
        try {
            ((Object[]) array.unhand())[index] = value.unhand();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetObjectArrayElement");
        }
    }

    // Source: JniFunctionsSource.java:1191
    @JNI_FUNCTION
    private static JniHandle NewBooleanArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewBooleanArray");
        try {
            return JniHandles.createLocalHandle(new boolean[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewBooleanArray");
        }
    }

    // Source: JniFunctionsSource.java:1196
    @JNI_FUNCTION
    private static JniHandle NewByteArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewByteArray");
        try {
            return JniHandles.createLocalHandle(new byte[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewByteArray");
        }
    }

    // Source: JniFunctionsSource.java:1201
    @JNI_FUNCTION
    private static JniHandle NewCharArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewCharArray");
        try {
            return JniHandles.createLocalHandle(new char[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewCharArray");
        }
    }

    // Source: JniFunctionsSource.java:1206
    @JNI_FUNCTION
    private static JniHandle NewShortArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewShortArray");
        try {
            return JniHandles.createLocalHandle(new short[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewShortArray");
        }
    }

    // Source: JniFunctionsSource.java:1211
    @JNI_FUNCTION
    private static JniHandle NewIntArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewIntArray");
        try {
            return JniHandles.createLocalHandle(new int[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewIntArray");
        }
    }

    // Source: JniFunctionsSource.java:1216
    @JNI_FUNCTION
    private static JniHandle NewLongArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewLongArray");
        try {
            return JniHandles.createLocalHandle(new long[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewLongArray");
        }
    }

    // Source: JniFunctionsSource.java:1221
    @JNI_FUNCTION
    private static JniHandle NewFloatArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewFloatArray");
        try {
            return JniHandles.createLocalHandle(new float[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewFloatArray");
        }
    }

    // Source: JniFunctionsSource.java:1226
    @JNI_FUNCTION
    private static JniHandle NewDoubleArray(Pointer env, int length) {
        Pointer anchor = prologue(env, "NewDoubleArray");
        try {
            return JniHandles.createLocalHandle(new double[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewDoubleArray");
        }
    }

    // Source: JniFunctionsSource.java:1231
    @JNI_FUNCTION
    private static Pointer GetBooleanArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetBooleanArrayElements");
        try {
            return getBooleanArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor, "GetBooleanArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1246
    @JNI_FUNCTION
    private static Pointer GetByteArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetByteArrayElements");
        try {
            return getByteArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor, "GetByteArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1261
    @JNI_FUNCTION
    private static Pointer GetCharArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetCharArrayElements");
        try {
            return getCharArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor, "GetCharArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1276
    @JNI_FUNCTION
    private static Pointer GetShortArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetShortArrayElements");
        try {
            return getShortArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor, "GetShortArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1291
    @JNI_FUNCTION
    private static Pointer GetIntArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetIntArrayElements");
        try {
            return getIntArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor, "GetIntArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1306
    @JNI_FUNCTION
    private static Pointer GetLongArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetLongArrayElements");
        try {
            return getLongArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor, "GetLongArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1321
    @JNI_FUNCTION
    private static Pointer GetFloatArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetFloatArrayElements");
        try {
            return getFloatArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor, "GetFloatArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1336
    @JNI_FUNCTION
    private static Pointer GetDoubleArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetDoubleArrayElements");
        try {
            return getDoubleArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor, "GetDoubleArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1351
    @JNI_FUNCTION
    private static void ReleaseBooleanArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        Pointer anchor = prologue(env, "ReleaseBooleanArrayElements");
        try {
            releaseBooleanArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ReleaseBooleanArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1366
    @JNI_FUNCTION
    private static void ReleaseByteArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        Pointer anchor = prologue(env, "ReleaseByteArrayElements");
        try {
            releaseByteArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ReleaseByteArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1381
    @JNI_FUNCTION
    private static void ReleaseCharArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        Pointer anchor = prologue(env, "ReleaseCharArrayElements");
        try {
            releaseCharArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ReleaseCharArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1396
    @JNI_FUNCTION
    private static void ReleaseShortArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        Pointer anchor = prologue(env, "ReleaseShortArrayElements");
        try {
            releaseShortArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ReleaseShortArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1411
    @JNI_FUNCTION
    private static void ReleaseIntArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        Pointer anchor = prologue(env, "ReleaseIntArrayElements");
        try {
            releaseIntArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ReleaseIntArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1426
    @JNI_FUNCTION
    private static void ReleaseLongArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        Pointer anchor = prologue(env, "ReleaseLongArrayElements");
        try {
            releaseLongArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ReleaseLongArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1441
    @JNI_FUNCTION
    private static void ReleaseFloatArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        Pointer anchor = prologue(env, "ReleaseFloatArrayElements");
        try {
            releaseFloatArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ReleaseFloatArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1456
    @JNI_FUNCTION
    private static void ReleaseDoubleArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        Pointer anchor = prologue(env, "ReleaseDoubleArrayElements");
        try {
            releaseDoubleArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ReleaseDoubleArrayElements");
        }
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

    // Source: JniFunctionsSource.java:1471
    @JNI_FUNCTION
    private static void GetBooleanArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetBooleanArrayRegion");
        try {
            final boolean[] a = (boolean[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setBoolean(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "GetBooleanArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1479
    @JNI_FUNCTION
    private static void GetByteArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetByteArrayRegion");
        try {
            final byte[] a = (byte[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setByte(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "GetByteArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1487
    @JNI_FUNCTION
    private static void GetCharArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetCharArrayRegion");
        try {
            final char[] a = (char[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setChar(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "GetCharArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1495
    @JNI_FUNCTION
    private static void GetShortArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetShortArrayRegion");
        try {
            final short[] a = (short[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setShort(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "GetShortArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1503
    @JNI_FUNCTION
    private static void GetIntArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetIntArrayRegion");
        try {
            final int[] a = (int[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setInt(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "GetIntArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1511
    @JNI_FUNCTION
    private static void GetLongArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetLongArrayRegion");
        try {
            final long[] a = (long[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setLong(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "GetLongArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1519
    @JNI_FUNCTION
    private static void GetFloatArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetFloatArrayRegion");
        try {
            final float[] a = (float[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setFloat(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "GetFloatArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1527
    @JNI_FUNCTION
    private static void GetDoubleArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetDoubleArrayRegion");
        try {
            final double[] a = (double[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setDouble(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "GetDoubleArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1535
    @JNI_FUNCTION
    private static void SetBooleanArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetBooleanArrayRegion");
        try {
            final boolean[] a = (boolean[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getBoolean(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetBooleanArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1543
    @JNI_FUNCTION
    private static void SetByteArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetByteArrayRegion");
        try {
            final byte[] a = (byte[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getByte(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetByteArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1551
    @JNI_FUNCTION
    private static void SetCharArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetCharArrayRegion");
        try {
            final char[] a = (char[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getChar(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetCharArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1559
    @JNI_FUNCTION
    private static void SetShortArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetShortArrayRegion");
        try {
            final short[] a = (short[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getShort(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetShortArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1567
    @JNI_FUNCTION
    private static void SetIntArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetIntArrayRegion");
        try {
            final int[] a = (int[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getInt(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetIntArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1575
    @JNI_FUNCTION
    private static void SetLongArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetLongArrayRegion");
        try {
            final long[] a = (long[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getLong(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetLongArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1583
    @JNI_FUNCTION
    private static void SetFloatArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetFloatArrayRegion");
        try {
            final float[] a = (float[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getFloat(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetFloatArrayRegion");
        }
    }

    // Source: JniFunctionsSource.java:1591
    @JNI_FUNCTION
    private static void SetDoubleArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "SetDoubleArrayRegion");
        try {
            final double[] a = (double[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getDouble(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetDoubleArrayRegion");
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
    // Source: JniFunctionsSource.java:1607
    @JNI_FUNCTION
    private static int RegisterNatives(Pointer env, JniHandle javaType, Pointer methods, int numberOfMethods) {
        Pointer anchor = prologue(env, "RegisterNatives");
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "RegisterNatives");
        }
    }

    // Source: JniFunctionsSource.java:1645
    @JNI_FUNCTION
    private static int UnregisterNatives(Pointer env, JniHandle javaType) {
        Pointer anchor = prologue(env, "UnregisterNatives");
        try {
            final ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
            classActor.forAllClassMethodActors(new Procedure<ClassMethodActor>() {
                public void run(ClassMethodActor classMethodActor) {
                    classMethodActor.nativeFunction.setAddress(Word.zero());
                }
            });
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "UnregisterNatives");
        }
    }

    // Source: JniFunctionsSource.java:1656
    @JNI_FUNCTION
    private static int MonitorEnter(Pointer env, JniHandle object) {
        Pointer anchor = prologue(env, "MonitorEnter");
        try {
            Monitor.enter(object.unhand());
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "MonitorEnter");
        }
    }

    // Source: JniFunctionsSource.java:1662
    @JNI_FUNCTION
    private static int MonitorExit(Pointer env, JniHandle object) {
        Pointer anchor = prologue(env, "MonitorExit");
        try {
            Monitor.exit(object.unhand());
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "MonitorExit");
        }
    }

    // Source: JniFunctionsSource.java:1668
    @JNI_FUNCTION
    private static native int GetJavaVM(Pointer env, Pointer vmPointerPointer);

    // Source: JniFunctionsSource.java:1671
    @JNI_FUNCTION
    private static void GetStringRegion(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetStringRegion");
        try {
            final String s = (String) string.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setChar(i, s.charAt(i + start));
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "GetStringRegion");
        }
    }

    // Source: JniFunctionsSource.java:1679
    @JNI_FUNCTION
    private static void GetStringUTFRegion(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        Pointer anchor = prologue(env, "GetStringUTFRegion");
        try {
            final String s = ((String) string.unhand()).substring(start, start + length);
            final byte[] utf = Utf8.stringToUtf8(s);
            Memory.writeBytes(utf, utf.length, buffer);
            buffer.setByte(utf.length, (byte) 0); // zero termination
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "GetStringUTFRegion");
        }
    }

    // Source: JniFunctionsSource.java:1687
    @JNI_FUNCTION
    private static Pointer GetPrimitiveArrayCritical(Pointer env, JniHandle array, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetPrimitiveArrayCritical");
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor, "GetPrimitiveArrayCritical");
        }
    }

    // Source: JniFunctionsSource.java:1721
    @JNI_FUNCTION
    private static void ReleasePrimitiveArrayCritical(Pointer env, JniHandle array, Pointer elements, int mode) {
        Pointer anchor = prologue(env, "ReleasePrimitiveArrayCritical");
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ReleasePrimitiveArrayCritical");
        }
    }

    // Source: JniFunctionsSource.java:1754
    @JNI_FUNCTION
    private static Pointer GetStringCritical(Pointer env, JniHandle string, Pointer isCopy) {
        Pointer anchor = prologue(env, "GetStringCritical");
        try {
            setCopyPointer(isCopy, true);
            final char[] a = ((String) string.unhand()).toCharArray();
            final Pointer pointer = Memory.mustAllocate(a.length * Kind.CHAR.width.numberOfBytes);
            for (int i = 0; i < a.length; i++) {
                pointer.setChar(i, a[i]);
            }
            return pointer;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor, "GetStringCritical");
        }
    }

    // Source: JniFunctionsSource.java:1765
    @JNI_FUNCTION
    private static void ReleaseStringCritical(Pointer env, JniHandle string, final Pointer chars) {
        Pointer anchor = prologue(env, "ReleaseStringCritical");
        try {
            Memory.deallocate(chars);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "ReleaseStringCritical");
        }
    }

    // Source: JniFunctionsSource.java:1770
    @JNI_FUNCTION
    private static JniHandle NewWeakGlobalRef(Pointer env, JniHandle handle) {
        Pointer anchor = prologue(env, "NewWeakGlobalRef");
        try {
            return JniHandles.createWeakGlobalHandle(handle.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewWeakGlobalRef");
        }
    }

    // Source: JniFunctionsSource.java:1775
    @JNI_FUNCTION
    private static void DeleteWeakGlobalRef(Pointer env, JniHandle handle) {
        Pointer anchor = prologue(env, "DeleteWeakGlobalRef");
        try {
            JniHandles.destroyWeakGlobalHandle(handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "DeleteWeakGlobalRef");
        }
    }

    // Source: JniFunctionsSource.java:1780
    @JNI_FUNCTION
    private static boolean ExceptionCheck(Pointer env) {
        Pointer anchor = prologue(env, "ExceptionCheck");
        try {
            return VmThread.fromJniEnv(env).pendingException() != null;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return false;
        } finally {
            epilogue(anchor, "ExceptionCheck");
        }
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

    // Source: JniFunctionsSource.java:1800
    @JNI_FUNCTION
    private static JniHandle NewDirectByteBuffer(Pointer env, Pointer address, long capacity) throws Exception {
        Pointer anchor = prologue(env, "NewDirectByteBuffer");
        try {
            int cap = (int) capacity;
            return JniHandles.createLocalHandle(DirectByteBufferConstructor().invokeConstructor(LongValue.from(address.toLong()), IntValue.from(cap)).asObject());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "NewDirectByteBuffer");
        }
    }

    // Source: JniFunctionsSource.java:1806
    @JNI_FUNCTION
    private static Pointer GetDirectBufferAddress(Pointer env, JniHandle buffer) throws Exception {
        Pointer anchor = prologue(env, "GetDirectBufferAddress");
        try {
            Object buf = buffer.unhand();
            if (DirectByteBuffer().isInstance(buf)) {
                long address = TupleAccess.readLong(buf, directByteBufferAddressFieldOffset());
                return Pointer.fromLong(address);
            }
            return Pointer.zero();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor, "GetDirectBufferAddress");
        }
    }

    // Source: JniFunctionsSource.java:1816
    @JNI_FUNCTION
    private static long GetDirectBufferCapacity(Pointer env, JniHandle buffer) {
        Pointer anchor = prologue(env, "GetDirectBufferCapacity");
        try {
            Object buf = buffer.unhand();
            if (DirectByteBuffer().isInstance(buf)) {
                return ((Buffer) buf).capacity();
            }
            return -1;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetDirectBufferCapacity");
        }
    }

    // Source: JniFunctionsSource.java:1825
    @JNI_FUNCTION
    private static int GetObjectRefType(Pointer env, JniHandle obj) {
        Pointer anchor = prologue(env, "GetObjectRefType");
        try {
            final int tag = JniHandles.tag(obj);
            if (tag == JniHandles.Tag.STACK) {
                return JniHandles.Tag.LOCAL;
            }
            return tag;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetObjectRefType");
        }
    }

    /*
     * Extended JNI native interface, see Native/jni/jni.c:
     */

    // Source: JniFunctionsSource.java:1838
    @JNI_FUNCTION
    private static int GetNumberOfArguments(Pointer env, MethodID methodID) throws Exception {
        Pointer anchor = prologue(env, "GetNumberOfArguments");
        try {
            final MethodActor methodActor = MethodID.toMethodActor(methodID);
            if (methodActor == null) {
                throw new NoSuchMethodException();
            }
            return methodActor.descriptor().numberOfParameters();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetNumberOfArguments");
        }
    }

    // Source: JniFunctionsSource.java:1847
    @JNI_FUNCTION
    private static void GetKindsOfArguments(Pointer env, MethodID methodID, Pointer kinds) throws Exception {
        Pointer anchor = prologue(env, "GetKindsOfArguments");
        try {
            final MethodActor methodActor = MethodID.toMethodActor(methodID);
            if (methodActor == null) {
                throw new NoSuchMethodException();
            }
            final SignatureDescriptor signature = methodActor.descriptor();
            for (int i = 0; i < signature.numberOfParameters(); ++i) {
                final Kind kind = signature.parameterDescriptorAt(i).toKind();
                kinds.setByte(i, (byte) kind.asEnum.ordinal());
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "GetKindsOfArguments");
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
