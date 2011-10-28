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
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.lang.reflect.*;
import java.nio.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jdk.*;
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
 * @see NativeInterfaces
 * @see JniFunctionsSource
 * @see Native/substrate/jni.c
 */
public final class JniFunctions {

    @RESET
    public static boolean TraceJNI;
    static {
        VMOptions.addFieldOption("-XX:", "TraceJNI", "Trace JNI calls.");
    }

    public static boolean CheckJNI;
    static {
        VMOptions.register(new VMOption("-Xcheck:jni", "Perform additional checks for JNI functions.") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                CheckJNI = true;
                return true;
            }
        }, Phase.STARTING);
    }

    private static final String JNI = "JNI";
    public static final int JNI_OK = 0;
    public static final int JNI_ERR = -1; /* unknown error */
    public static final int JNI_EDETACHED = -2; /* thread detached from the VM */
    public static final int JNI_EVERSION = -3; /* JNI version error */
    public static final int JNI_ENOMEM = -4; /* not enough memory */
    public static final int JNI_EEXIST = -5; /* VM already created */
    public static final int JNI_EINVAL = -6; /* invalid arguments */

    public static final int JNI_COMMIT = 1;
    public static final int JNI_ABORT = 2;

    @INTRINSIC(UNSAFE_CAST) public static native JniHandle asJniHandle(int value);
    @INTRINSIC(UNSAFE_CAST) public static native MethodID  asMethodID(int value);
    @INTRINSIC(UNSAFE_CAST) public static native FieldID   asFieldID(int value);
    @INTRINSIC(UNSAFE_CAST) public static native Pointer   asPointer(int value);

    /**
     * This method implements part of the prologue for entering a JNI upcall from native code.
     *
     * @param etla
     * @return an anchor for the JNI function frame. The anchor previous to this anchor is either that of the JNI stub
     *         frame that called out to native code or the native anchor of a thread that attached to the VM.
     */
    @INLINE
    public static Pointer reenterJavaFromNative(Pointer etla) {
        Word previousAnchor = LAST_JAVA_FRAME_ANCHOR.load(etla);
        Pointer anchor = JavaFrameAnchor.create(Word.zero(), Word.zero(), Word.zero(), previousAnchor);
        // a JNI upcall is similar to a native method returning; reuse the native call epilogue sequence
        Snippets.nativeCallEpilogue0(etla, anchor);
        return anchor;
    }

    @INLINE
    public static void tracePrologue(String name, Pointer anchor) {
        if (TraceJNI) {
            traceEntry(name, JNI, anchor);
        }
    }

    @INLINE
    public static Pointer prologue(Pointer env) {
        SafepointPoll.setLatchRegister(env.minus(JNI_ENV.offset));
        Pointer etla = ETLA.load(currentTLA());
        Pointer anchor = reenterJavaFromNative(etla);
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
    public static void epilogue(Pointer anchor) {
        // returning from a JNI upcall is similar to a entering a native method returning; reuse the native call prologue sequence
        Pointer etla = ETLA.load(currentTLA());
        Snippets.nativeCallPrologue0(etla, JavaFrameAnchor.PREVIOUS.get(anchor));
    }

    @INLINE
    public static void traceEpilogue(String name) {
        if (TraceJNI) {
            traceExit(name, JNI);
        }
    }

    /**
     * Traces the entry to an upcall if the {@linkplain ClassMethodActor#traceJNI() JNI tracing flag} has been set.
     *
     * @param name the name of the JNI function being entered
     * @param anchor for the JNI function frame. The anchor previous to this anchor is either that of the JNI stub frame
     *            that called out to native code or the native anchor of a thread that attached to the VM.
     */

    public static void traceEntry(String name, String callType, Pointer anchor) {
        if (name != null) {
            boolean lockDisabledSafepoints = Log.lock();
            Log.print("[Thread \"");
            Log.print(VmThread.current().getName());
            Log.print("\" --> "); Log.print(callType); Log.print(" upcall: ");
            Log.print(name);
            Pointer jniStubAnchor = JavaFrameAnchor.PREVIOUS.get(anchor);
            final Address jniStubPC = jniStubAnchor.isZero() ? Address.zero() : JavaFrameAnchor.PC.get(jniStubAnchor).asAddress();
            if (!jniStubPC.isZero()) {
                final TargetMethod nativeMethod = Code.codePointerToTargetMethod(jniStubPC);
                Log.print(", last down call: ");
                FatalError.check(nativeMethod != null, "Could not find Java down call when entering JNI upcall");
                Log.print(nativeMethod.classMethodActor().name.string);
            } else {
                Log.print(", called from attached native thread");
            }
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    /**
     * Traces the exit from an upcall if the {@linkplain ClassMethodActor#traceJNI() JNI tracing flag} has been set.
     *
     * @param name the name of the JNI function being exited
     */
    public static void traceExit(String name, String callType) {
        if (name != null) {
            boolean lockDisabledSafepoints = Log.lock();
            Log.print("[Thread \"");
            Log.print(VmThread.current().getName());
            Log.print("\" <-- "); Log.print(callType); Log.print(" upcall: ");
            Log.print(name);
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }


    private static class Triple implements Comparable<Triple> {
        public long counter;
        public long timer;
        public String methodName;
        @Override
        public int compareTo(Triple o) {
            long diff = o.timer - this.timer;
            return diff < 0 ? -1 : diff > 0 ? 1 : 0;
        }
    }

    /**
     * Print counters and timers for all of the JNI and JMM entrypoints.
     * To generate the necessary instrumentation code, set {@linkplain JniFunctionsGenerator#TIME_JNI_FUNCTIONS}
     * to true and run "max jnigen".
     */
    public static void printJniFunctionTimers() {
        if (INSTRUMENTED) {
            List<Triple> counters = new ArrayList<Triple>();
            try {
                for (Class clazz : new Class[] {JniFunctions.class, JmmFunctions.class}) {
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        if (Modifier.isStatic(field.getModifiers()) && field.getName().startsWith("COUNTER_")) {
                            Triple triple = new Triple();
                            triple.methodName = field.getName().substring("COUNTER_".length());
                            triple.counter = field.getLong(null);
                            triple.timer = clazz.getDeclaredField("TIMER_" + triple.methodName).getLong(null);
                            counters.add(triple);
                        }
                    }
                }
            } catch (Throwable ex) {
                throw ProgramError.unexpected(ex);
            }
            Collections.sort(counters);
            Log.println("JNI Function counters and timers:");
            Log.println("_______count_______ms__us__ns___method________");
            for (Triple triple : counters) {
                Log.println(String.format("%,12d %,16d   %s", triple.counter, triple.timer, triple.methodName));
            }
        }
    }

    /*
     * DO NOT EDIT CODE BETWEEN "START GENERATED CODE" AND "END GENERATED CODE" IN THIS FILE.
     *
     * Instead, modify the corresponding source in JniFunctionsSource.java denoted by the "// Source: ..." comments.
     * Once finished with editing, execute JniFunctionsGenerator as a Java application to refresh this file.
     */

// START GENERATED CODE

    private static final boolean INSTRUMENTED = false;

    @VM_ENTRY_POINT
    private static native void reserved0();
        // Source: JniFunctionsSource.java:71

    @VM_ENTRY_POINT
    private static native void reserved1();
        // Source: JniFunctionsSource.java:74

    @VM_ENTRY_POINT
    private static native void reserved2();
        // Source: JniFunctionsSource.java:77

    @VM_ENTRY_POINT
    private static native void reserved3();
        // Source: JniFunctionsSource.java:80

    // Checkstyle: stop method name check

    @VM_ENTRY_POINT
    private static native int GetVersion(Pointer env);
        // Source: JniFunctionsSource.java:85

    private static String dottify(String slashifiedName) {
        return slashifiedName.replace('/', '.');
    }

    private static void traceReflectiveInvocation(MethodActor methodActor) {
        if (TraceJNI) {
            Log.print("[Thread \"");
            Log.print(VmThread.current().getName());
            Log.print("\" --> JNI invoke: ");
            Log.println(methodActor.format("%H.%n(%p)"));
        }
    }

    private static final Class[] defineClassParameterTypes = {String.class, byte[].class, int.class, int.class};

    @VM_ENTRY_POINT
    private static JniHandle DefineClass(Pointer env, Pointer slashifiedName, JniHandle classLoader, Pointer buffer, int length) throws ClassFormatError {
        // Source: JniFunctionsSource.java:103
        Pointer anchor = prologue(env);
        tracePrologue("DefineClass", anchor);
        try {
            final byte[] bytes = new byte[length];
            Memory.readBytes(buffer, length, bytes);
            try {
                // TODO: find out whether already dottified class names should be rejected by this function
                String name = dottify(CString.utf8ToJava(slashifiedName));
                ClassLoader cl = (ClassLoader) classLoader.unhand();
                if (cl == null) {
                    cl = BootClassLoader.BOOT_CLASS_LOADER;
                }
                Class javaClass = ClassfileReader.defineClassActor(name, cl, bytes, 0, bytes.length, null, null, false).toJava();
                return JniHandles.createLocalHandle(javaClass);
            } catch (Utf8Exception utf8Exception) {
                throw classFormatError("Invalid class name");
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("DefineClass");
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
        // Source: JniFunctionsSource.java:129
        Pointer anchor = prologue(env);
        tracePrologue("FindClass", anchor);
        try {
            String className;
            try {
                className = CString.utf8ToJava(name);
            } catch (Utf8Exception utf8Exception) {
                throw new ClassNotFoundException();
            }
            // Skip our frame
            Class caller = JDK_sun_reflect_Reflection.getCallerClassForFindClass(1);
            ClassLoader classLoader = caller == null ? ClassLoader.getSystemClassLoader() : ClassActor.fromJava(caller).classLoader;
            final Class javaClass = findClass(classLoader, className);
            Snippets.makeClassInitialized(ClassActor.fromJava(javaClass));
            return JniHandles.createLocalHandle(javaClass);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("FindClass");
        }
    }

    @VM_ENTRY_POINT
    private static MethodID FromReflectedMethod(Pointer env, JniHandle reflectedMethod) {
        // Source: JniFunctionsSource.java:145
        Pointer anchor = prologue(env);
        tracePrologue("FromReflectedMethod", anchor);
        try {
            final MethodActor methodActor = MethodActor.fromJava((Method) reflectedMethod.unhand());
            return MethodID.fromMethodActor(methodActor);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asMethodID(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("FromReflectedMethod");
        }
    }

    @VM_ENTRY_POINT
    private static FieldID FromReflectedField(Pointer env, JniHandle field) {
        // Source: JniFunctionsSource.java:151
        Pointer anchor = prologue(env);
        tracePrologue("FromReflectedField", anchor);
        try {
            final FieldActor fieldActor = FieldActor.fromJava((Field) field.unhand());
            return FieldID.fromFieldActor(fieldActor);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asFieldID(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("FromReflectedField");
        }
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
        // Source: JniFunctionsSource.java:165
        Pointer anchor = prologue(env);
        tracePrologue("ToReflectedMethod", anchor);
        try {
            return JniHandles.createLocalHandle(ToReflectedMethod(methodID, isStatic));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("ToReflectedMethod");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetSuperclass(Pointer env, JniHandle subType) {
        // Source: JniFunctionsSource.java:170
        Pointer anchor = prologue(env);
        tracePrologue("GetSuperclass", anchor);
        try {
            return JniHandles.createLocalHandle(((Class) subType.unhand()).getSuperclass());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetSuperclass");
        }
    }

    @VM_ENTRY_POINT
    private static boolean IsAssignableFrom(Pointer env, JniHandle subType, JniHandle superType) {
        // Source: JniFunctionsSource.java:175
        Pointer anchor = prologue(env);
        tracePrologue("IsAssignableFrom", anchor);
        try {
            return ClassActor.fromJava((Class) superType.unhand()).isAssignableFrom(ClassActor.fromJava((Class) subType.unhand()));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            traceEpilogue("IsAssignableFrom");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle ToReflectedField(Pointer env, JniHandle javaClass, FieldID fieldID, boolean isStatic) {
        // Source: JniFunctionsSource.java:180
        Pointer anchor = prologue(env);
        tracePrologue("ToReflectedField", anchor);
        try {
            final FieldActor fieldActor = FieldID.toFieldActor(fieldID);
            if (fieldActor == null || fieldActor.isStatic() != isStatic) {
                throw new NoSuchFieldError();
            }
            return JniHandles.createLocalHandle(fieldActor.toJava());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("ToReflectedField");
        }
    }

    @VM_ENTRY_POINT
    private static int Throw(Pointer env, JniHandle throwable) {
        // Source: JniFunctionsSource.java:189
        Pointer anchor = prologue(env);
        tracePrologue("Throw", anchor);
        try {
            VmThread.fromJniEnv(env).setJniException((Throwable) throwable.unhand());
            return JNI_OK;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("Throw");
        }
    }

    @VM_ENTRY_POINT
    private static int ThrowNew(Pointer env, JniHandle throwableClass, Pointer message) throws Throwable {
        // Source: JniFunctionsSource.java:195
        Pointer anchor = prologue(env);
        tracePrologue("ThrowNew", anchor);
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
            constructor = Utils.cast(type, throwableClass.unhand()).getConstructor(parameterTypes);
            Throwable throwable = message.isZero() ? constructor.newInstance() : constructor.newInstance(CString.utf8ToJava(message));
            VmThread.fromJniEnv(env).setJniException(throwable);
            return JNI_OK;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("ThrowNew");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle ExceptionOccurred(Pointer env) {
        // Source: JniFunctionsSource.java:212
        Pointer anchor = prologue(env);
        tracePrologue("ExceptionOccurred", anchor);
        try {
            return JniHandles.createLocalHandle(VmThread.fromJniEnv(env).jniException());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("ExceptionOccurred");
        }
    }

    @VM_ENTRY_POINT
    private static void ExceptionDescribe(Pointer env) {
        // Source: JniFunctionsSource.java:217
        Pointer anchor = prologue(env);
        tracePrologue("ExceptionDescribe", anchor);
        try {
            final Throwable exception = VmThread.fromJniEnv(env).jniException();
            if (exception != null) {
                exception.printStackTrace();
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ExceptionDescribe");
        }
    }

    @VM_ENTRY_POINT
    private static void ExceptionClear(Pointer env) {
        // Source: JniFunctionsSource.java:225
        Pointer anchor = prologue(env);
        tracePrologue("ExceptionClear", anchor);
        try {
            VmThread.fromJniEnv(env).setJniException(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ExceptionClear");
        }
    }

    @VM_ENTRY_POINT
    private static void FatalError(Pointer env, Pointer message) {
        // Source: JniFunctionsSource.java:230
        Pointer anchor = prologue(env);
        tracePrologue("FatalError", anchor);
        try {
            try {
                FatalError.unexpected(CString.utf8ToJava(message));
            } catch (Utf8Exception utf8Exception) {
                FatalError.unexpected("fatal error with UTF8 error in message");
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("FatalError");
        }
    }

    private static int PushLocalFrame0(Pointer env, int capacity) {
        JniHandles.pushLocalFrame(capacity);
        return JNI_OK;
    }

    @VM_ENTRY_POINT
    private static int PushLocalFrame(Pointer env, int capacity) {
        // Source: JniFunctionsSource.java:244
        Pointer anchor = prologue(env);
        tracePrologue("PushLocalFrame", anchor);
        try {
            JniHandles.pushLocalFrame(capacity);
            return JNI_OK;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("PushLocalFrame");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle PopLocalFrame(Pointer env, JniHandle res) {
        // Source: JniFunctionsSource.java:250
        Pointer anchor = prologue(env);
        tracePrologue("PopLocalFrame", anchor);
        try {
            return JniHandles.popLocalFrame(res);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("PopLocalFrame");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewGlobalRef(Pointer env, JniHandle handle) {
        // Source: JniFunctionsSource.java:255
        Pointer anchor = prologue(env);
        tracePrologue("NewGlobalRef", anchor);
        try {
            return JniHandles.createGlobalHandle(handle.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewGlobalRef");
        }
    }

    @VM_ENTRY_POINT
    private static void DeleteGlobalRef(Pointer env, JniHandle handle) {
        // Source: JniFunctionsSource.java:260
        Pointer anchor = prologue(env);
        tracePrologue("DeleteGlobalRef", anchor);
        try {
            JniHandles.destroyGlobalHandle(handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("DeleteGlobalRef");
        }
    }

    @VM_ENTRY_POINT
    private static void DeleteLocalRef(Pointer env, JniHandle handle) {
        // Source: JniFunctionsSource.java:265
        Pointer anchor = prologue(env);
        tracePrologue("DeleteLocalRef", anchor);
        try {
            JniHandles.destroyLocalHandle(handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("DeleteLocalRef");
        }
    }

    @VM_ENTRY_POINT
    private static boolean IsSameObject(Pointer env, JniHandle object1, JniHandle object2) {
        // Source: JniFunctionsSource.java:270
        Pointer anchor = prologue(env);
        tracePrologue("IsSameObject", anchor);
        try {
            return object1.unhand() == object2.unhand();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            traceEpilogue("IsSameObject");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewLocalRef(Pointer env, JniHandle object) {
        // Source: JniFunctionsSource.java:275
        Pointer anchor = prologue(env);
        tracePrologue("NewLocalRef", anchor);
        try {
            return JniHandles.createLocalHandle(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewLocalRef");
        }
    }

    @VM_ENTRY_POINT
    private static int EnsureLocalCapacity(Pointer env, int capacity) {
        // Source: JniFunctionsSource.java:280
        Pointer anchor = prologue(env);
        tracePrologue("EnsureLocalCapacity", anchor);
        try {
            // If this call fails, it will be with an OutOfMemoryError which will be
            // set as the pending exception for the current thread
            JniHandles.ensureLocalHandleCapacity(capacity);
            return JNI_OK;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("EnsureLocalCapacity");
        }
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
        // Source: JniFunctionsSource.java:296
        Pointer anchor = prologue(env);
        tracePrologue("AllocObject", anchor);
        try {
            return JniHandles.createLocalHandle(allocObject((Class) javaClass.unhand()));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("AllocObject");
        }
    }

    @VM_ENTRY_POINT
    private static native JniHandle NewObject(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:301

    @VM_ENTRY_POINT
    private static native JniHandle NewObjectV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:304

    @VM_ENTRY_POINT
    private static JniHandle NewObjectA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:307
        Pointer anchor = prologue(env);
        tracePrologue("NewObjectA", anchor);
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
            final VirtualMethodActor virtualMethodActor = tupleClassActor.findLocalVirtualMethodActor(methodActor.name, methodActor.descriptor());
            if (virtualMethodActor == null) {
                throw new NoSuchMethodException();
            }

            final SignatureDescriptor signature = virtualMethodActor.descriptor();
            final Value[] argumentValues = new Value[signature.numberOfParameters()];
            copyJValueArrayToValueArray(arguments, signature, argumentValues, 0);
            traceReflectiveInvocation(virtualMethodActor);
            return JniHandles.createLocalHandle(virtualMethodActor.invokeConstructor(argumentValues).asObject());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewObjectA");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetObjectClass(Pointer env, JniHandle object) {
        // Source: JniFunctionsSource.java:332
        Pointer anchor = prologue(env);
        tracePrologue("GetObjectClass", anchor);
        try {
            final Class javaClass = object.unhand().getClass();
            return JniHandles.createLocalHandle(javaClass);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetObjectClass");
        }
    }

    @VM_ENTRY_POINT
    private static boolean IsInstanceOf(Pointer env, JniHandle object, JniHandle javaType) {
        // Source: JniFunctionsSource.java:338
        Pointer anchor = prologue(env);
        tracePrologue("IsInstanceOf", anchor);
        try {
            return ((Class) javaType.unhand()).isInstance(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            traceEpilogue("IsInstanceOf");
        }
    }

    @VM_ENTRY_POINT
    private static MethodID GetMethodID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        // Source: JniFunctionsSource.java:343
        Pointer anchor = prologue(env);
        tracePrologue("GetMethodID", anchor);
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asMethodID(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetMethodID");
        }
    }

    @VM_ENTRY_POINT
    private static native JniHandle CallObjectMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:366

    @VM_ENTRY_POINT
    private static native JniHandle CallObjectMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer vaList);
        // Source: JniFunctionsSource.java:369

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
                    throw ProgramError.unexpected();
                }
            }
            a = a.plus(jvalueSize);
        }
    }

    private static Value checkResult(Kind expectedReturnKind, final MethodActor methodActor, Value result) {
        if (expectedReturnKind != result.kind()) {
            Value zero = expectedReturnKind.zeroValue();
            if (CheckJNI) {
                Log.println("JNI warning: returning " + zero + " for " + expectedReturnKind + " call to " + methodActor);
            }
            result = zero;
        }
        return result;
    }

    private static Value CallValueMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments, Kind expectedReturnKind) throws Exception {
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
        return checkResult(expectedReturnKind, methodActor, selectedMethod.invoke(argumentValues));

    }

    @VM_ENTRY_POINT
    private static JniHandle CallObjectMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:477
        Pointer anchor = prologue(env);
        tracePrologue("CallObjectMethodA", anchor);
        try {
            return JniHandles.createLocalHandle(CallValueMethodA(env, object, methodID, arguments, Kind.REFERENCE).asObject());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("CallObjectMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native boolean CallBooleanMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:482

    @VM_ENTRY_POINT
    private static native boolean CallBooleanMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer vaList);
        // Source: JniFunctionsSource.java:485

    @VM_ENTRY_POINT
    private static boolean CallBooleanMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:488
        Pointer anchor = prologue(env);
        tracePrologue("CallBooleanMethodA", anchor);
        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.BOOLEAN).asBoolean();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallBooleanMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native byte CallByteMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:493

    @VM_ENTRY_POINT
    private static native byte CallByteMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:496

    @VM_ENTRY_POINT
    private static byte CallByteMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:499
        Pointer anchor = prologue(env);
        tracePrologue("CallByteMethodA", anchor);
        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.BYTE).asByte();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallByteMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native char CallCharMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:504

    @VM_ENTRY_POINT
    private static native char CallCharMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:507

    @VM_ENTRY_POINT
    private static char CallCharMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:510
        Pointer anchor = prologue(env);
        tracePrologue("CallCharMethodA", anchor);
        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.CHAR).asChar();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return  (char) JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallCharMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native short CallShortMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:515

    @VM_ENTRY_POINT
    private static native short CallShortMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:518

    @VM_ENTRY_POINT
    private static short CallShortMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:521
        Pointer anchor = prologue(env);
        tracePrologue("CallShortMethodA", anchor);
        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.SHORT).asShort();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallShortMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native int CallIntMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:526

    @VM_ENTRY_POINT
    private static native int CallIntMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:529

    @VM_ENTRY_POINT
    private static int CallIntMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:532
        Pointer anchor = prologue(env);
        tracePrologue("CallIntMethodA", anchor);
        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.INT).asInt();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallIntMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native long CallLongMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:537

    @VM_ENTRY_POINT
    private static native long CallLongMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:540

    @VM_ENTRY_POINT
    private static long CallLongMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:543
        Pointer anchor = prologue(env);
        tracePrologue("CallLongMethodA", anchor);
        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.LONG).asLong();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallLongMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native float CallFloatMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:548

    @VM_ENTRY_POINT
    private static native float CallFloatMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:551

    @VM_ENTRY_POINT
    private static float CallFloatMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:554
        Pointer anchor = prologue(env);
        tracePrologue("CallFloatMethodA", anchor);
        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.FLOAT).asFloat();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallFloatMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native double CallDoubleMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:559

    @VM_ENTRY_POINT
    private static native double CallDoubleMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:562

    @VM_ENTRY_POINT
    private static double CallDoubleMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:565
        Pointer anchor = prologue(env);
        tracePrologue("CallDoubleMethodA", anchor);
        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.DOUBLE).asDouble();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallDoubleMethodA");
        }
    }

    private static Value CallNonvirtualValueMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments, Kind expectedReturnKind) throws Exception {
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
        return checkResult(expectedReturnKind, methodActor, virtualMethodActor.invoke(argumentValues));
    }

    @VM_ENTRY_POINT
    private static native void CallVoidMethod(Pointer env, JniHandle object, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:594

    @VM_ENTRY_POINT
    private static native void CallVoidMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:597

    @VM_ENTRY_POINT
    private static void CallVoidMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:600
        Pointer anchor = prologue(env);
        tracePrologue("CallVoidMethodA", anchor);
        try {
            CallValueMethodA(env, object, methodID, arguments, Kind.VOID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("CallVoidMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native JniHandle CallNonvirtualObjectMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);
        // Source: JniFunctionsSource.java:605

    @VM_ENTRY_POINT
    private static native JniHandle CallNonvirtualObjectMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:608

    @VM_ENTRY_POINT
    private static JniHandle CallNonvirtualObjectMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:611
        Pointer anchor = prologue(env);
        tracePrologue("CallNonvirtualObjectMethodA", anchor);
        try {
            return JniHandles.createLocalHandle(CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.REFERENCE).asObject());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("CallNonvirtualObjectMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native boolean CallNonvirtualBooleanMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);
        // Source: JniFunctionsSource.java:616

    @VM_ENTRY_POINT
    private static native boolean CallNonvirtualBooleanMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:619

    @VM_ENTRY_POINT
    private static boolean CallNonvirtualBooleanMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:622
        Pointer anchor = prologue(env);
        tracePrologue("CallNonvirtualBooleanMethodA", anchor);
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.BOOLEAN).asBoolean();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallNonvirtualBooleanMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native byte CallNonvirtualByteMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);
        // Source: JniFunctionsSource.java:627

    @VM_ENTRY_POINT
    private static native byte CallNonvirtualByteMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:630

    @VM_ENTRY_POINT
    private static byte CallNonvirtualByteMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:633
        Pointer anchor = prologue(env);
        tracePrologue("CallNonvirtualByteMethodA", anchor);
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.BYTE).asByte();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallNonvirtualByteMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native char CallNonvirtualCharMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);
        // Source: JniFunctionsSource.java:638

    @VM_ENTRY_POINT
    private static native char CallNonvirtualCharMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:641

    @VM_ENTRY_POINT
    private static char CallNonvirtualCharMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:644
        Pointer anchor = prologue(env);
        tracePrologue("CallNonvirtualCharMethodA", anchor);
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.CHAR).asChar();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return  (char) JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallNonvirtualCharMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native short CallNonvirtualShortMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);
        // Source: JniFunctionsSource.java:649

    @VM_ENTRY_POINT
    private static native short CallNonvirtualShortMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:652

    @VM_ENTRY_POINT
    private static short CallNonvirtualShortMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:655
        Pointer anchor = prologue(env);
        tracePrologue("CallNonvirtualShortMethodA", anchor);
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.SHORT).asShort();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallNonvirtualShortMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native int CallNonvirtualIntMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);
        // Source: JniFunctionsSource.java:660

    @VM_ENTRY_POINT
    private static native int CallNonvirtualIntMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:663

    @VM_ENTRY_POINT
    private static int CallNonvirtualIntMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:666
        Pointer anchor = prologue(env);
        tracePrologue("CallNonvirtualIntMethodA", anchor);
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.INT).asInt();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallNonvirtualIntMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native long CallNonvirtualLongMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);
        // Source: JniFunctionsSource.java:671

    @VM_ENTRY_POINT
    private static native long CallNonvirtualLongMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:674

    @VM_ENTRY_POINT
    private static long CallNonvirtualLongMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:677
        Pointer anchor = prologue(env);
        tracePrologue("CallNonvirtualLongMethodA", anchor);
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.LONG).asLong();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallNonvirtualLongMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native float CallNonvirtualFloatMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);
        // Source: JniFunctionsSource.java:682

    @VM_ENTRY_POINT
    private static native float CallNonvirtualFloatMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:685

    @VM_ENTRY_POINT
    private static float CallNonvirtualFloatMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:688
        Pointer anchor = prologue(env);
        tracePrologue("CallNonvirtualFloatMethodA", anchor);
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.FLOAT).asFloat();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallNonvirtualFloatMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native double CallNonvirtualDoubleMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);
        // Source: JniFunctionsSource.java:693

    @VM_ENTRY_POINT
    private static native double CallNonvirtualDoubleMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:696

    @VM_ENTRY_POINT
    private static double CallNonvirtualDoubleMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:699
        Pointer anchor = prologue(env);
        tracePrologue("CallNonvirtualDoubleMethodA", anchor);
        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.DOUBLE).asDouble();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallNonvirtualDoubleMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native void CallNonvirtualVoidMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID /*,...*/);
        // Source: JniFunctionsSource.java:704

    @VM_ENTRY_POINT
    private static native void CallNonvirtualVoidMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:707

    @VM_ENTRY_POINT
    private static void CallNonvirtualVoidMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:710
        Pointer anchor = prologue(env);
        tracePrologue("CallNonvirtualVoidMethodA", anchor);
        try {
            CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.VOID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("CallNonvirtualVoidMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static FieldID GetFieldID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        // Source: JniFunctionsSource.java:715
        Pointer anchor = prologue(env);
        tracePrologue("GetFieldID", anchor);
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asFieldID(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetFieldID");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetObjectField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:739
        Pointer anchor = prologue(env);
        tracePrologue("GetObjectField", anchor);
        try {
            return JniHandles.createLocalHandle(FieldID.toFieldActor(fieldID).getObject(object.unhand()));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetObjectField");
        }
    }

    @VM_ENTRY_POINT
    private static boolean GetBooleanField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:744
        Pointer anchor = prologue(env);
        tracePrologue("GetBooleanField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getBoolean(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetBooleanField");
        }
    }

    @VM_ENTRY_POINT
    private static byte GetByteField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:749
        Pointer anchor = prologue(env);
        tracePrologue("GetByteField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getByte(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetByteField");
        }
    }

    @VM_ENTRY_POINT
    private static char GetCharField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:754
        Pointer anchor = prologue(env);
        tracePrologue("GetCharField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getChar(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return  (char) JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetCharField");
        }
    }

    @VM_ENTRY_POINT
    private static short GetShortField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:759
        Pointer anchor = prologue(env);
        tracePrologue("GetShortField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getShort(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetShortField");
        }
    }

    @VM_ENTRY_POINT
    private static int GetIntField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:764
        Pointer anchor = prologue(env);
        tracePrologue("GetIntField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getInt(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetIntField");
        }
    }

    @VM_ENTRY_POINT
    private static long GetLongField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:769
        Pointer anchor = prologue(env);
        tracePrologue("GetLongField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getLong(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetLongField");
        }
    }

    @VM_ENTRY_POINT
    private static float GetFloatField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:774
        Pointer anchor = prologue(env);
        tracePrologue("GetFloatField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getFloat(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetFloatField");
        }
    }

    @VM_ENTRY_POINT
    private static double GetDoubleField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:779
        Pointer anchor = prologue(env);
        tracePrologue("GetDoubleField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getDouble(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetDoubleField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetObjectField(Pointer env, JniHandle object, FieldID fieldID, JniHandle value) {
        // Source: JniFunctionsSource.java:784
        Pointer anchor = prologue(env);
        tracePrologue("SetObjectField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setObject(object.unhand(), value.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetObjectField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetBooleanField(Pointer env, JniHandle object, FieldID fieldID, boolean value) {
        // Source: JniFunctionsSource.java:789
        Pointer anchor = prologue(env);
        tracePrologue("SetBooleanField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setBoolean(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetBooleanField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetByteField(Pointer env, JniHandle object, FieldID fieldID, byte value) {
        // Source: JniFunctionsSource.java:794
        Pointer anchor = prologue(env);
        tracePrologue("SetByteField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setByte(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetByteField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetCharField(Pointer env, JniHandle object, FieldID fieldID, char value) {
        // Source: JniFunctionsSource.java:799
        Pointer anchor = prologue(env);
        tracePrologue("SetCharField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setChar(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetCharField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetShortField(Pointer env, JniHandle object, FieldID fieldID, short value) {
        // Source: JniFunctionsSource.java:804
        Pointer anchor = prologue(env);
        tracePrologue("SetShortField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setShort(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetShortField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetIntField(Pointer env, JniHandle object, FieldID fieldID, int value) {
        // Source: JniFunctionsSource.java:809
        Pointer anchor = prologue(env);
        tracePrologue("SetIntField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setInt(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetIntField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetLongField(Pointer env, JniHandle object, FieldID fieldID, long value) {
        // Source: JniFunctionsSource.java:814
        Pointer anchor = prologue(env);
        tracePrologue("SetLongField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setLong(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetLongField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetFloatField(Pointer env, JniHandle object, FieldID fieldID, float value) {
        // Source: JniFunctionsSource.java:819
        Pointer anchor = prologue(env);
        tracePrologue("SetFloatField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setFloat(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetFloatField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetDoubleField(Pointer env, JniHandle object, FieldID fieldID, double value) {
        // Source: JniFunctionsSource.java:824
        Pointer anchor = prologue(env);
        tracePrologue("SetDoubleField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setDouble(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetDoubleField");
        }
    }

    @VM_ENTRY_POINT
    private static MethodID GetStaticMethodID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        // Source: JniFunctionsSource.java:829
        Pointer anchor = prologue(env);
        tracePrologue("GetStaticMethodID", anchor);
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asMethodID(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStaticMethodID");
        }
    }

    private static Value CallStaticValueMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments, Kind expectedReturnKind) throws Exception {
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
        return checkResult(expectedReturnKind, methodActor, methodActor.invoke(argumentValues));
    }

    @VM_ENTRY_POINT
    private static native JniHandle CallStaticObjectMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:876

    @VM_ENTRY_POINT
    private static native JniHandle CallStaticObjectMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:879

    @VM_ENTRY_POINT
    private static JniHandle CallStaticObjectMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:882
        Pointer anchor = prologue(env);
        tracePrologue("CallStaticObjectMethodA", anchor);
        try {
            return JniHandles.createLocalHandle(CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.REFERENCE).asObject());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("CallStaticObjectMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native boolean CallStaticBooleanMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:887

    @VM_ENTRY_POINT
    private static native boolean CallStaticBooleanMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:890

    @VM_ENTRY_POINT
    private static boolean CallStaticBooleanMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:893
        Pointer anchor = prologue(env);
        tracePrologue("CallStaticBooleanMethodA", anchor);
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.BOOLEAN).asBoolean();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallStaticBooleanMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native byte CallStaticByteMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:898

    @VM_ENTRY_POINT
    private static native byte CallStaticByteMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:901

    @VM_ENTRY_POINT
    private static byte CallStaticByteMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:904
        Pointer anchor = prologue(env);
        tracePrologue("CallStaticByteMethodA", anchor);
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.BYTE).asByte();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallStaticByteMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native char CallStaticCharMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:909

    @VM_ENTRY_POINT
    private static native char CallStaticCharMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:912

    @VM_ENTRY_POINT
    private static char CallStaticCharMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:915
        Pointer anchor = prologue(env);
        tracePrologue("CallStaticCharMethodA", anchor);
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.CHAR).asChar();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return  (char) JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallStaticCharMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native short CallStaticShortMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:920

    @VM_ENTRY_POINT
    private static native short CallStaticShortMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:923

    @VM_ENTRY_POINT
    private static short CallStaticShortMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:926
        Pointer anchor = prologue(env);
        tracePrologue("CallStaticShortMethodA", anchor);
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.SHORT).asShort();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallStaticShortMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native int CallStaticIntMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:931

    @VM_ENTRY_POINT
    private static native int CallStaticIntMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:934

    @VM_ENTRY_POINT
    private static int CallStaticIntMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:937
        Pointer anchor = prologue(env);
        tracePrologue("CallStaticIntMethodA", anchor);
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.INT).asInt();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallStaticIntMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native long CallStaticLongMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:942

    @VM_ENTRY_POINT
    private static native long CallStaticLongMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:945

    @VM_ENTRY_POINT
    private static long CallStaticLongMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:948
        Pointer anchor = prologue(env);
        tracePrologue("CallStaticLongMethodA", anchor);
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.LONG).asLong();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallStaticLongMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native float CallStaticFloatMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:953

    @VM_ENTRY_POINT
    private static native float CallStaticFloatMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:956

    @VM_ENTRY_POINT
    private static float CallStaticFloatMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:959
        Pointer anchor = prologue(env);
        tracePrologue("CallStaticFloatMethodA", anchor);
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.FLOAT).asFloat();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallStaticFloatMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native double CallStaticDoubleMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:964

    @VM_ENTRY_POINT
    private static native double CallStaticDoubleMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:967

    @VM_ENTRY_POINT
    private static double CallStaticDoubleMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:970
        Pointer anchor = prologue(env);
        tracePrologue("CallStaticDoubleMethodA", anchor);
        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.DOUBLE).asDouble();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("CallStaticDoubleMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static native void CallStaticVoidMethod(Pointer env, JniHandle javaClass, MethodID methodID /*, ...*/);
        // Source: JniFunctionsSource.java:975

    @VM_ENTRY_POINT
    private static native void CallStaticVoidMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:978

    @VM_ENTRY_POINT
    private static void CallStaticVoidMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:981
        Pointer anchor = prologue(env);
        tracePrologue("CallStaticVoidMethodA", anchor);
        try {
            CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.VOID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("CallStaticVoidMethodA");
        }
    }

    @VM_ENTRY_POINT
    private static FieldID GetStaticFieldID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        // Source: JniFunctionsSource.java:986
        Pointer anchor = prologue(env);
        tracePrologue("GetStaticFieldID", anchor);
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asFieldID(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStaticFieldID");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetStaticObjectField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1009
        Pointer anchor = prologue(env);
        tracePrologue("GetStaticObjectField", anchor);
        try {
            return JniHandles.createLocalHandle(FieldID.toFieldActor(fieldID).getObject(null));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStaticObjectField");
        }
    }

    @VM_ENTRY_POINT
    private static boolean GetStaticBooleanField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1014
        Pointer anchor = prologue(env);
        tracePrologue("GetStaticBooleanField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getBoolean(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStaticBooleanField");
        }
    }

    @VM_ENTRY_POINT
    private static byte GetStaticByteField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1019
        Pointer anchor = prologue(env);
        tracePrologue("GetStaticByteField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getByte(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStaticByteField");
        }
    }

    @VM_ENTRY_POINT
    private static char GetStaticCharField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1024
        Pointer anchor = prologue(env);
        tracePrologue("GetStaticCharField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getChar(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return  (char) JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStaticCharField");
        }
    }

    @VM_ENTRY_POINT
    private static short GetStaticShortField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1029
        Pointer anchor = prologue(env);
        tracePrologue("GetStaticShortField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getShort(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStaticShortField");
        }
    }

    @VM_ENTRY_POINT
    private static int GetStaticIntField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1034
        Pointer anchor = prologue(env);
        tracePrologue("GetStaticIntField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getInt(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStaticIntField");
        }
    }

    @VM_ENTRY_POINT
    private static long GetStaticLongField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1039
        Pointer anchor = prologue(env);
        tracePrologue("GetStaticLongField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getLong(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStaticLongField");
        }
    }

    @VM_ENTRY_POINT
    private static float GetStaticFloatField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1044
        Pointer anchor = prologue(env);
        tracePrologue("GetStaticFloatField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getFloat(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStaticFloatField");
        }
    }

    @VM_ENTRY_POINT
    private static double GetStaticDoubleField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1049
        Pointer anchor = prologue(env);
        tracePrologue("GetStaticDoubleField", anchor);
        try {
            return FieldID.toFieldActor(fieldID).getDouble(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStaticDoubleField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticObjectField(Pointer env, JniHandle javaType, FieldID fieldID, JniHandle value) {
        // Source: JniFunctionsSource.java:1054
        Pointer anchor = prologue(env);
        tracePrologue("SetStaticObjectField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setObject(null, value.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetStaticObjectField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticBooleanField(Pointer env, JniHandle javaType, FieldID fieldID, boolean value) {
        // Source: JniFunctionsSource.java:1059
        Pointer anchor = prologue(env);
        tracePrologue("SetStaticBooleanField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setBoolean(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetStaticBooleanField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticByteField(Pointer env, JniHandle javaType, FieldID fieldID, byte value) {
        // Source: JniFunctionsSource.java:1064
        Pointer anchor = prologue(env);
        tracePrologue("SetStaticByteField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setByte(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetStaticByteField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticCharField(Pointer env, JniHandle javaType, FieldID fieldID, char value) {
        // Source: JniFunctionsSource.java:1069
        Pointer anchor = prologue(env);
        tracePrologue("SetStaticCharField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setChar(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetStaticCharField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticShortField(Pointer env, JniHandle javaType, FieldID fieldID, short value) {
        // Source: JniFunctionsSource.java:1074
        Pointer anchor = prologue(env);
        tracePrologue("SetStaticShortField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setShort(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetStaticShortField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticIntField(Pointer env, JniHandle javaType, FieldID fieldID, int value) {
        // Source: JniFunctionsSource.java:1079
        Pointer anchor = prologue(env);
        tracePrologue("SetStaticIntField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setInt(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetStaticIntField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticLongField(Pointer env, JniHandle javaType, FieldID fieldID, long value) {
        // Source: JniFunctionsSource.java:1084
        Pointer anchor = prologue(env);
        tracePrologue("SetStaticLongField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setLong(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetStaticLongField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticFloatField(Pointer env, JniHandle javaType, FieldID fieldID, float value) {
        // Source: JniFunctionsSource.java:1089
        Pointer anchor = prologue(env);
        tracePrologue("SetStaticFloatField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setFloat(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetStaticFloatField");
        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticDoubleField(Pointer env, JniHandle javaType, FieldID fieldID, double value) {
        // Source: JniFunctionsSource.java:1094
        Pointer anchor = prologue(env);
        tracePrologue("SetStaticDoubleField", anchor);
        try {
            FieldID.toFieldActor(fieldID).setDouble(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetStaticDoubleField");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewString(Pointer env, Pointer chars, int length) {
        // Source: JniFunctionsSource.java:1099
        Pointer anchor = prologue(env);
        tracePrologue("NewString", anchor);
        try {
            final char[] charArray = new char[length];
            for (int i = 0; i < length; i++) {
                charArray[i] = chars.getChar(i);
            }
            return JniHandles.createLocalHandle(new String(charArray));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewString");
        }
    }

    @VM_ENTRY_POINT
    private static int GetStringLength(Pointer env, JniHandle string) {
        // Source: JniFunctionsSource.java:1108
        Pointer anchor = prologue(env);
        tracePrologue("GetStringLength", anchor);
        try {
            return ((String) string.unhand()).length();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStringLength");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetStringChars(Pointer env, JniHandle string, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1113
        Pointer anchor = prologue(env);
        tracePrologue("GetStringChars", anchor);
        try {
            setCopyPointer(isCopy, true);
            return JniHandles.createLocalHandle(((String) string.unhand()).toCharArray());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStringChars");
        }
    }

    @VM_ENTRY_POINT
    private static void ReleaseStringChars(Pointer env, JniHandle string, Pointer chars) {
        // Source: JniFunctionsSource.java:1119
        Pointer anchor = prologue(env);
        tracePrologue("ReleaseStringChars", anchor);
        try {
            Memory.deallocate(chars);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ReleaseStringChars");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewStringUTF(Pointer env, Pointer utf) {
        // Source: JniFunctionsSource.java:1124
        Pointer anchor = prologue(env);
        tracePrologue("NewStringUTF", anchor);
        try {
            try {
                return JniHandles.createLocalHandle(CString.utf8ToJava(utf));
            } catch (Utf8Exception utf8Exception) {
                return JniHandle.zero();
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewStringUTF");
        }
    }

    @VM_ENTRY_POINT
    private static int GetStringUTFLength(Pointer env, JniHandle string) {
        // Source: JniFunctionsSource.java:1133
        Pointer anchor = prologue(env);
        tracePrologue("GetStringUTFLength", anchor);
        try {
            return Utf8.utf8Length((String) string.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStringUTFLength");
        }
    }

    @VM_ENTRY_POINT
    private static Pointer GetStringUTFChars(Pointer env, JniHandle string, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1138
        Pointer anchor = prologue(env);
        tracePrologue("GetStringUTFChars", anchor);
        try {
            setCopyPointer(isCopy, true);
            return CString.utf8FromJava((String) string.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStringUTFChars");
        }
    }

    @VM_ENTRY_POINT
    private static void ReleaseStringUTFChars(Pointer env, JniHandle string, Pointer chars) {
        // Source: JniFunctionsSource.java:1144
        Pointer anchor = prologue(env);
        tracePrologue("ReleaseStringUTFChars", anchor);
        try {
            Memory.deallocate(chars);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ReleaseStringUTFChars");
        }
    }

    @VM_ENTRY_POINT
    private static int GetArrayLength(Pointer env, JniHandle array) {
        // Source: JniFunctionsSource.java:1149
        Pointer anchor = prologue(env);
        tracePrologue("GetArrayLength", anchor);
        try {
            return Array.getLength(array.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetArrayLength");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewObjectArray(Pointer env, int length, JniHandle elementType, JniHandle initialElementValue) {
        // Source: JniFunctionsSource.java:1154
        Pointer anchor = prologue(env);
        tracePrologue("NewObjectArray", anchor);
        try {
            final Object array = Array.newInstance((Class) elementType.unhand(), length);
            final Object initialValue = initialElementValue.unhand();
            for (int i = 0; i < length; i++) {
                Array.set(array, i, initialValue);
            }
            return JniHandles.createLocalHandle(array);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewObjectArray");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetObjectArrayElement(Pointer env, JniHandle array, int index) {
        // Source: JniFunctionsSource.java:1164
        Pointer anchor = prologue(env);
        tracePrologue("GetObjectArrayElement", anchor);
        try {
            return JniHandles.createLocalHandle(((Object[]) array.unhand())[index]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetObjectArrayElement");
        }
    }

    @VM_ENTRY_POINT
    private static void SetObjectArrayElement(Pointer env, JniHandle array, int index, JniHandle value) {
        // Source: JniFunctionsSource.java:1169
        Pointer anchor = prologue(env);
        tracePrologue("SetObjectArrayElement", anchor);
        try {
            ((Object[]) array.unhand())[index] = value.unhand();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetObjectArrayElement");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewBooleanArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1174
        Pointer anchor = prologue(env);
        tracePrologue("NewBooleanArray", anchor);
        try {
            return JniHandles.createLocalHandle(new boolean[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewBooleanArray");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewByteArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1179
        Pointer anchor = prologue(env);
        tracePrologue("NewByteArray", anchor);
        try {
            return JniHandles.createLocalHandle(new byte[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewByteArray");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewCharArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1184
        Pointer anchor = prologue(env);
        tracePrologue("NewCharArray", anchor);
        try {
            return JniHandles.createLocalHandle(new char[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewCharArray");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewShortArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1189
        Pointer anchor = prologue(env);
        tracePrologue("NewShortArray", anchor);
        try {
            return JniHandles.createLocalHandle(new short[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewShortArray");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewIntArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1194
        Pointer anchor = prologue(env);
        tracePrologue("NewIntArray", anchor);
        try {
            return JniHandles.createLocalHandle(new int[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewIntArray");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewLongArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1199
        Pointer anchor = prologue(env);
        tracePrologue("NewLongArray", anchor);
        try {
            return JniHandles.createLocalHandle(new long[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewLongArray");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewFloatArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1204
        Pointer anchor = prologue(env);
        tracePrologue("NewFloatArray", anchor);
        try {
            return JniHandles.createLocalHandle(new float[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewFloatArray");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewDoubleArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1209
        Pointer anchor = prologue(env);
        tracePrologue("NewDoubleArray", anchor);
        try {
            return JniHandles.createLocalHandle(new double[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewDoubleArray");
        }
    }

    @VM_ENTRY_POINT
    private static Pointer GetBooleanArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1214
        Pointer anchor = prologue(env);
        tracePrologue("GetBooleanArrayElements", anchor);
        try {
            return getBooleanArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetBooleanArrayElements");
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

    @VM_ENTRY_POINT
    private static Pointer GetByteArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1229
        Pointer anchor = prologue(env);
        tracePrologue("GetByteArrayElements", anchor);
        try {
            return getByteArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetByteArrayElements");
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

    @VM_ENTRY_POINT
    private static Pointer GetCharArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1244
        Pointer anchor = prologue(env);
        tracePrologue("GetCharArrayElements", anchor);
        try {
            return getCharArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetCharArrayElements");
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

    @VM_ENTRY_POINT
    private static Pointer GetShortArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1259
        Pointer anchor = prologue(env);
        tracePrologue("GetShortArrayElements", anchor);
        try {
            return getShortArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetShortArrayElements");
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

    @VM_ENTRY_POINT
    private static Pointer GetIntArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1274
        Pointer anchor = prologue(env);
        tracePrologue("GetIntArrayElements", anchor);
        try {
            return getIntArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetIntArrayElements");
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

    @VM_ENTRY_POINT
    private static Pointer GetLongArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1289
        Pointer anchor = prologue(env);
        tracePrologue("GetLongArrayElements", anchor);
        try {
            return getLongArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetLongArrayElements");
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

    @VM_ENTRY_POINT
    private static Pointer GetFloatArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1304
        Pointer anchor = prologue(env);
        tracePrologue("GetFloatArrayElements", anchor);
        try {
            return getFloatArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetFloatArrayElements");
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

    @VM_ENTRY_POINT
    private static Pointer GetDoubleArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1319
        Pointer anchor = prologue(env);
        tracePrologue("GetDoubleArrayElements", anchor);
        try {
            return getDoubleArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetDoubleArrayElements");
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

    @VM_ENTRY_POINT
    private static void ReleaseBooleanArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        // Source: JniFunctionsSource.java:1334
        Pointer anchor = prologue(env);
        tracePrologue("ReleaseBooleanArrayElements", anchor);
        try {
            releaseBooleanArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ReleaseBooleanArrayElements");
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

    @VM_ENTRY_POINT
    private static void ReleaseByteArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        // Source: JniFunctionsSource.java:1349
        Pointer anchor = prologue(env);
        tracePrologue("ReleaseByteArrayElements", anchor);
        try {
            releaseByteArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ReleaseByteArrayElements");
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

    @VM_ENTRY_POINT
    private static void ReleaseCharArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        // Source: JniFunctionsSource.java:1364
        Pointer anchor = prologue(env);
        tracePrologue("ReleaseCharArrayElements", anchor);
        try {
            releaseCharArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ReleaseCharArrayElements");
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

    @VM_ENTRY_POINT
    private static void ReleaseShortArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        // Source: JniFunctionsSource.java:1379
        Pointer anchor = prologue(env);
        tracePrologue("ReleaseShortArrayElements", anchor);
        try {
            releaseShortArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ReleaseShortArrayElements");
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

    @VM_ENTRY_POINT
    private static void ReleaseIntArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        // Source: JniFunctionsSource.java:1394
        Pointer anchor = prologue(env);
        tracePrologue("ReleaseIntArrayElements", anchor);
        try {
            releaseIntArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ReleaseIntArrayElements");
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

    @VM_ENTRY_POINT
    private static void ReleaseLongArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        // Source: JniFunctionsSource.java:1409
        Pointer anchor = prologue(env);
        tracePrologue("ReleaseLongArrayElements", anchor);
        try {
            releaseLongArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ReleaseLongArrayElements");
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

    @VM_ENTRY_POINT
    private static void ReleaseFloatArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        // Source: JniFunctionsSource.java:1424
        Pointer anchor = prologue(env);
        tracePrologue("ReleaseFloatArrayElements", anchor);
        try {
            releaseFloatArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ReleaseFloatArrayElements");
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

    @VM_ENTRY_POINT
    private static void ReleaseDoubleArrayElements(Pointer env, JniHandle array, Pointer elements, int mode) {
        // Source: JniFunctionsSource.java:1439
        Pointer anchor = prologue(env);
        tracePrologue("ReleaseDoubleArrayElements", anchor);
        try {
            releaseDoubleArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ReleaseDoubleArrayElements");
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

    @VM_ENTRY_POINT
    private static void GetBooleanArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1454
        Pointer anchor = prologue(env);
        tracePrologue("GetBooleanArrayRegion", anchor);
        try {
            final boolean[] a = (boolean[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setBoolean(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetBooleanArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void GetByteArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1462
        Pointer anchor = prologue(env);
        tracePrologue("GetByteArrayRegion", anchor);
        try {
            final byte[] a = (byte[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setByte(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetByteArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void GetCharArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1470
        Pointer anchor = prologue(env);
        tracePrologue("GetCharArrayRegion", anchor);
        try {
            final char[] a = (char[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setChar(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetCharArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void GetShortArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1478
        Pointer anchor = prologue(env);
        tracePrologue("GetShortArrayRegion", anchor);
        try {
            final short[] a = (short[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setShort(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetShortArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void GetIntArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1486
        Pointer anchor = prologue(env);
        tracePrologue("GetIntArrayRegion", anchor);
        try {
            final int[] a = (int[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setInt(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetIntArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void GetLongArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1494
        Pointer anchor = prologue(env);
        tracePrologue("GetLongArrayRegion", anchor);
        try {
            final long[] a = (long[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setLong(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetLongArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void GetFloatArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1502
        Pointer anchor = prologue(env);
        tracePrologue("GetFloatArrayRegion", anchor);
        try {
            final float[] a = (float[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setFloat(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetFloatArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void GetDoubleArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1510
        Pointer anchor = prologue(env);
        tracePrologue("GetDoubleArrayRegion", anchor);
        try {
            final double[] a = (double[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setDouble(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetDoubleArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void SetBooleanArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1518
        Pointer anchor = prologue(env);
        tracePrologue("SetBooleanArrayRegion", anchor);
        try {
            final boolean[] a = (boolean[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getBoolean(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetBooleanArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void SetByteArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1526
        Pointer anchor = prologue(env);
        tracePrologue("SetByteArrayRegion", anchor);
        try {
            final byte[] a = (byte[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getByte(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetByteArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void SetCharArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1534
        Pointer anchor = prologue(env);
        tracePrologue("SetCharArrayRegion", anchor);
        try {
            final char[] a = (char[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getChar(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetCharArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void SetShortArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1542
        Pointer anchor = prologue(env);
        tracePrologue("SetShortArrayRegion", anchor);
        try {
            final short[] a = (short[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getShort(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetShortArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void SetIntArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1550
        Pointer anchor = prologue(env);
        tracePrologue("SetIntArrayRegion", anchor);
        try {
            final int[] a = (int[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getInt(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetIntArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void SetLongArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1558
        Pointer anchor = prologue(env);
        tracePrologue("SetLongArrayRegion", anchor);
        try {
            final long[] a = (long[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getLong(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetLongArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void SetFloatArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1566
        Pointer anchor = prologue(env);
        tracePrologue("SetFloatArrayRegion", anchor);
        try {
            final float[] a = (float[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getFloat(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetFloatArrayRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void SetDoubleArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1574
        Pointer anchor = prologue(env);
        tracePrologue("SetDoubleArrayRegion", anchor);
        try {
            final double[] a = (double[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getDouble(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("SetDoubleArrayRegion");
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
        // Source: JniFunctionsSource.java:1590
        Pointer anchor = prologue(env);
        tracePrologue("RegisterNatives", anchor);
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
                    final Address fnPtr = a.readWord(FNPTR).asAddress();

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
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("RegisterNatives");
        }
    }

    @VM_ENTRY_POINT
    private static int UnregisterNatives(Pointer env, JniHandle javaType) {
        // Source: JniFunctionsSource.java:1628
        Pointer anchor = prologue(env);
        tracePrologue("UnregisterNatives", anchor);
        try {
            ClassActor classActor = ClassActor.fromJava((Class) javaType.unhand());
            for (VirtualMethodActor method : classActor.allVirtualMethodActors()) {
                method.nativeFunction.setAddress(Address.zero());
            }
            do {
                for (StaticMethodActor method : classActor.localStaticMethodActors()) {
                    method.nativeFunction.setAddress(Address.zero());
                }
                classActor = classActor.superClassActor;
            } while (classActor != null);
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("UnregisterNatives");
        }
    }

    @VM_ENTRY_POINT
    private static int MonitorEnter(Pointer env, JniHandle object) {
        // Source: JniFunctionsSource.java:1643
        Pointer anchor = prologue(env);
        tracePrologue("MonitorEnter", anchor);
        try {
            Monitor.enter(object.unhand());
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("MonitorEnter");
        }
    }

    @VM_ENTRY_POINT
    private static int MonitorExit(Pointer env, JniHandle object) {
        // Source: JniFunctionsSource.java:1649
        Pointer anchor = prologue(env);
        tracePrologue("MonitorExit", anchor);
        try {
            Monitor.exit(object.unhand());
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("MonitorExit");
        }
    }

    @VM_ENTRY_POINT
    private static native int GetJavaVM(Pointer env, Pointer vmPointerPointer);
        // Source: JniFunctionsSource.java:1655

    @VM_ENTRY_POINT
    private static void GetStringRegion(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1658
        Pointer anchor = prologue(env);
        tracePrologue("GetStringRegion", anchor);
        try {
            final String s = (String) string.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setChar(i, s.charAt(i + start));
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStringRegion");
        }
    }

    @VM_ENTRY_POINT
    private static void GetStringUTFRegion(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1666
        Pointer anchor = prologue(env);
        tracePrologue("GetStringUTFRegion", anchor);
        try {
            final String s = ((String) string.unhand()).substring(start, start + length);
            final byte[] utf = Utf8.stringToUtf8(s);
            Memory.writeBytes(utf, utf.length, buffer);
            buffer.setByte(utf.length, (byte) 0); // zero termination
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStringUTFRegion");
        }
    }

    @VM_ENTRY_POINT
    private static Pointer GetPrimitiveArrayCritical(Pointer env, JniHandle array, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1674
        Pointer anchor = prologue(env);
        tracePrologue("GetPrimitiveArrayCritical", anchor);
        try {
            final Object arrayObject = array.unhand();
            if (Heap.useDirectPointer(arrayObject)) {
                setCopyPointer(isCopy, false);
                return Reference.fromJava(arrayObject).toOrigin().plus(Layout.byteArrayLayout().getElementOffsetFromOrigin(0));
            }

            if (arrayObject instanceof boolean[]) {
                return getBooleanArrayElements(array, isCopy);
            } else if (arrayObject instanceof byte[]) {
                return getByteArrayElements(array, isCopy);
            } else if (arrayObject instanceof char[]) {
                return getCharArrayElements(array, isCopy);
            } else if (arrayObject instanceof short[]) {
                return getShortArrayElements(array, isCopy);
            } else if (arrayObject instanceof int[]) {
                return getIntArrayElements(array, isCopy);
            } else if (arrayObject instanceof long[]) {
                return getLongArrayElements(array, isCopy);
            } else if (arrayObject instanceof float[]) {
                return getFloatArrayElements(array, isCopy);
            } else if (arrayObject instanceof double[]) {
                return getDoubleArrayElements(array, isCopy);
            }
            return Pointer.zero();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetPrimitiveArrayCritical");
        }
    }

    @VM_ENTRY_POINT
    private static void ReleasePrimitiveArrayCritical(Pointer env, JniHandle array, Pointer elements, int mode) {
        // Source: JniFunctionsSource.java:1702
        Pointer anchor = prologue(env);
        tracePrologue("ReleasePrimitiveArrayCritical", anchor);
        try {
            final Object arrayObject = array.unhand();
            if (Heap.releasedDirectPointer(arrayObject)) {
                return;
            }
            if (arrayObject instanceof boolean[]) {
                releaseBooleanArrayElements(array, elements, mode);
            } else if (arrayObject instanceof byte[]) {
                releaseByteArrayElements(array, elements, mode);
            } else if (arrayObject instanceof char[]) {
                releaseCharArrayElements(array, elements, mode);
            } else if (arrayObject instanceof short[]) {
                releaseShortArrayElements(array, elements, mode);
            } else if (arrayObject instanceof int[]) {
                releaseIntArrayElements(array, elements, mode);
            } else if (arrayObject instanceof long[]) {
                releaseLongArrayElements(array, elements, mode);
            } else if (arrayObject instanceof float[]) {
                releaseFloatArrayElements(array, elements, mode);
            } else if (arrayObject instanceof double[]) {
                releaseDoubleArrayElements(array, elements, mode);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ReleasePrimitiveArrayCritical");
        }
    }

    @VM_ENTRY_POINT
    private static Pointer GetStringCritical(Pointer env, JniHandle string, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1727
        Pointer anchor = prologue(env);
        tracePrologue("GetStringCritical", anchor);
        try {
            // TODO(cwi): Implement optimized version for OptimizeJNICritical if a benchmark uses it frequently
            setCopyPointer(isCopy, true);
            final char[] a = ((String) string.unhand()).toCharArray();
            final Pointer pointer = Memory.mustAllocate(a.length * Kind.CHAR.width.numberOfBytes);
            for (int i = 0; i < a.length; i++) {
                pointer.setChar(i, a[i]);
            }
            return pointer;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetStringCritical");
        }
    }

    @VM_ENTRY_POINT
    private static void ReleaseStringCritical(Pointer env, JniHandle string, final Pointer chars) {
        // Source: JniFunctionsSource.java:1739
        Pointer anchor = prologue(env);
        tracePrologue("ReleaseStringCritical", anchor);
        try {
            Memory.deallocate(chars);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("ReleaseStringCritical");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewWeakGlobalRef(Pointer env, JniHandle handle) {
        // Source: JniFunctionsSource.java:1744
        Pointer anchor = prologue(env);
        tracePrologue("NewWeakGlobalRef", anchor);
        try {
            return JniHandles.createWeakGlobalHandle(handle.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewWeakGlobalRef");
        }
    }

    @VM_ENTRY_POINT
    private static void DeleteWeakGlobalRef(Pointer env, JniHandle handle) {
        // Source: JniFunctionsSource.java:1749
        Pointer anchor = prologue(env);
        tracePrologue("DeleteWeakGlobalRef", anchor);
        try {
            JniHandles.destroyWeakGlobalHandle(handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("DeleteWeakGlobalRef");
        }
    }

    @VM_ENTRY_POINT
    private static boolean ExceptionCheck(Pointer env) {
        // Source: JniFunctionsSource.java:1754
        Pointer anchor = prologue(env);
        tracePrologue("ExceptionCheck", anchor);
        try {
            return VmThread.fromJniEnv(env).jniException() != null;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            traceEpilogue("ExceptionCheck");
        }
    }

    private static final ClassActor DirectByteBuffer = ClassActor.fromJava(Classes.forName("java.nio.DirectByteBuffer"));

    @VM_ENTRY_POINT
    private static JniHandle NewDirectByteBuffer(Pointer env, Pointer address, long capacity) throws Exception {
        // Source: JniFunctionsSource.java:1761
        Pointer anchor = prologue(env);
        tracePrologue("NewDirectByteBuffer", anchor);
        try {
            ByteBuffer buffer = ObjectAccess.createDirectByteBuffer(address.toLong(), (int) capacity);
            return JniHandles.createLocalHandle(buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("NewDirectByteBuffer");
        }
    }

    @VM_ENTRY_POINT
    private static Pointer GetDirectBufferAddress(Pointer env, JniHandle buffer) throws Exception {
        // Source: JniFunctionsSource.java:1767
        Pointer anchor = prologue(env);
        tracePrologue("GetDirectBufferAddress", anchor);
        try {
            Object buf = buffer.unhand();
            if (DirectByteBuffer.isInstance(buf)) {
                long address = ClassRegistry.Buffer_address.getLong(buf);
                return Pointer.fromLong(address);
            }
            return Pointer.zero();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetDirectBufferAddress");
        }
    }

    @VM_ENTRY_POINT
    private static long GetDirectBufferCapacity(Pointer env, JniHandle buffer) {
        // Source: JniFunctionsSource.java:1777
        Pointer anchor = prologue(env);
        tracePrologue("GetDirectBufferCapacity", anchor);
        try {
            Object buf = buffer.unhand();
            if (DirectByteBuffer.isInstance(buf)) {
                return ((Buffer) buf).capacity();
            }
            return -1;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetDirectBufferCapacity");
        }
    }

    @VM_ENTRY_POINT
    private static int GetObjectRefType(Pointer env, JniHandle obj) {
        // Source: JniFunctionsSource.java:1786
        Pointer anchor = prologue(env);
        tracePrologue("GetObjectRefType", anchor);
        try {
            final int tag = JniHandles.tag(obj);
            if (tag == JniHandles.Tag.STACK) {
                return JniHandles.Tag.LOCAL;
            }
            return tag;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetObjectRefType");
        }
    }

    /*
     * Extended JNI native interface, see Native/jni/jni.c:
     */

    @VM_ENTRY_POINT
    private static int GetNumberOfArguments(Pointer env, MethodID methodID) throws Exception {
        // Source: JniFunctionsSource.java:1799
        Pointer anchor = prologue(env);
        tracePrologue("GetNumberOfArguments", anchor);
        try {
            final MethodActor methodActor = MethodID.toMethodActor(methodID);
            if (methodActor == null) {
                throw new NoSuchMethodException();
            }
            return methodActor.descriptor().numberOfParameters();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            traceEpilogue("GetNumberOfArguments");
        }
    }

    @VM_ENTRY_POINT
    private static void GetKindsOfArguments(Pointer env, MethodID methodID, Pointer kinds) throws Exception {
        // Source: JniFunctionsSource.java:1808
        Pointer anchor = prologue(env);
        tracePrologue("GetKindsOfArguments", anchor);
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
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            traceEpilogue("GetKindsOfArguments");
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
