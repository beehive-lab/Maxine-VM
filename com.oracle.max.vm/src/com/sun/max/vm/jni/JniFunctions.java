/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
import static com.sun.max.vm.jni.JniFunctions.JxxFunctionsLogger.*;

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
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.layout.*;
//import com.sun.max.vm.log.*;  // see comment on JxxFunctionsLogger
import com.sun.max.vm.log.VMLog.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.ti.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Upcalls from C that implement the <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jni/spec/jniTOC.html">JNI Interface Functions</a>.
 * <p>
 * <b>DO NOT EDIT CODE BETWEEN "START GENERATED CODE" AND "END GENERATED CODE" IN THIS FILE.</b>
 * <p>
 * Instead, modify the corresponding source in JniFunctionsSource.java denoted by the "// Source: ..." comments.
 * Once finished with editing, execute 'mx jnigen' to refresh this file.
 *
 * @see NativeInterfaces
 * @see JniFunctionsSource
 * @see Native/substrate/jni.c
 */
public final class JniFunctions {

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
        Pointer anchor = JavaFrameAnchor.create(Word.zero(), Word.zero(), CodePointer.zero(), previousAnchor);
        // a JNI upcall is similar to a native method returning; reuse the native call epilogue sequence
        Snippets.nativeCallEpilogue0(etla, anchor);
        return anchor;
    }

    @INLINE
    public static Pointer prologue(Pointer env) {
        SafepointPoll.setLatchRegister(env.minus(JNI_ENV.offset));
        Pointer etla = ETLA.load(currentTLA());
        VMTI.handler().beginUpcallVM();
        Pointer anchor = reenterJavaFromNative(etla);
        return anchor;
    }

    /**
     * This method implements the epilogue for leaving an JNI upcall. The steps performed are to
     * reset the thread-local information which stores the last Java caller SP, FP, and IP.     *
     */
    @INLINE
    public static void epilogue(Pointer anchor) {
        // returning from a JNI upcall is similar to a entering a native method returning; reuse the native call prologue sequence
        Pointer etla = ETLA.load(currentTLA());
        VMTI.handler().endUpcallVM();
        Snippets.nativeCallPrologue0(etla, JavaFrameAnchor.PREVIOUS.get(anchor));
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

    /**
     * Logging/Tracing of JNI/JMM entry/exit.
     * TODO javac cannot resolve VMLogger if the symbol is unqualified (why?)
     */
    public static abstract class JxxFunctionsLogger extends com.sun.max.vm.log.VMLogger {
        public static final int ENTRY_BIT = 1;
        public static final int DOWNCALL_BIT = 2;
        public static final int INVOKE_BIT = 4;
        public static final int LINK_BIT = 8;
        public static final int REGISTER_BIT = 16;
        public static final Word UPCALL_ENTRY = Address.fromInt(1);
        public static final Word UPCALL_EXIT = Address.fromInt(0);
        public static final Word DOWNCALL_ENTRY = Address.fromInt(3);
        public static final Word DOWNCALL_EXIT = Address.fromInt(2);
        public static final Word INVOKE_ENTRY = Address.fromInt(5);
        public static final Word LINK_ENTRY = Address.fromInt(9);
        public static final Word REGISTER_ENTRY = Address.fromInt(17);

        JxxFunctionsLogger(String name, int entryPointsLength) {
            super(name, entryPointsLength, "log JNI/JMM upcalls");
        }

        /**
         * Recreates the original style of trace output that was explicitly generated prior to {@link VMLogger}. The
         * different trace modes are encoded in log argument 1. Downcall, Invoke, DynamicLink and RegisterNativeMethod
         * operations log the {@link MethodID} of the method in log argument 2. This is converted back to a
         * {@link String} here.
         */
        @Override
        protected void trace(Record r) {
            int op = r.getOperation();
            int mode = r.getArg(1).asAddress().toInt();
            boolean entry = (mode & ENTRY_BIT) != 0;
            Log.print("[Thread \"");
            Log.print(toVmThreadName(r.getThreadId()));
            Log.print("\" ");
            Log.print(entry ? "-->" : "<--");
            Log.print(" JNI ");
            if (mode <= UPCALL_ENTRY.asAddress().toInt()) {
                Log.print("upcall: ");
                Log.print(operationName(op));
                if (entry) {
                    Pointer anchor = r.getArg(2).asPointer();
                    Pointer jniStubAnchor = JavaFrameAnchor.PREVIOUS.get(anchor);
                    final Address jniStubPC = jniStubAnchor.isZero() ? Address.zero() : JavaFrameAnchor.PC.get(jniStubAnchor).asAddress();
                    if (!jniStubPC.isZero()) {
                        final TargetMethod nativeMethod = CodePointer.from(jniStubPC).toTargetMethod();
                        Log.print(", last down call: ");
                        FatalError.check(nativeMethod != null, "Could not find Java down call when entering JNI upcall");
                        Log.print(nativeMethod.classMethodActor().name.string);
                    } else {
                        Log.print(", called from attached native thread");
                    }
                }
            } else {
                String opName = null;
                Address address = Address.zero();
                if ((mode & INVOKE_BIT) != 0) {
                    opName = "invoke";
                } else if ((mode & DOWNCALL_BIT) != 0) {
                    opName = "downcall";
                } else if ((mode & LINK_BIT) != 0) {
                    address = r.getArg(3).asAddress();
                    opName = "dynamic-link";
                } else if ((mode & REGISTER_BIT) != 0) {
                    address = r.getArg(3).asAddress();
                    opName = address.isZero() ? "unregister" : "register";
                }

                Log.print(opName);
                Log.print(": ");
                MethodActor methodActor = MethodID.toMethodActor(MethodID.fromWord(r.getArg(2)));
                // Note: do not use "format" method, since we might be at a place where memory allocation is not possible
                // (for example, at the JNI call to synchronize before a GC)
                Log.print(methodActor.holder().name);
                Log.print('.');
                Log.print(methodActor.name);

                if (address.isNotZero()) {
                    Log.print(" = ");
                    Log.printSymbol(address);
                }
            }
            Log.println("]");
        }
    }

    /**
     * Logging/Tracing of JNI entry/exit.
     */
    public static class JniFunctionsLogger extends JxxFunctionsLogger {
        private static final LogOperations[] logOperations = LogOperations.values();

        private JniFunctionsLogger() {
            super("JNI", logOperations.length);
        }

        @Override
        public String operationName(int op) {
            return logOperations[op].name();
        }

    }

    public static final JniFunctionsLogger logger = new JniFunctionsLogger();

    static String dottify(String slashifiedName) {
        return slashifiedName.replace('/', '.');
    }

    static void logReflectiveInvocation(MethodActor methodActor) {
        if (logger.enabled()) {
            logger.log(LogOperations.ReflectiveInvocation.ordinal(), JxxFunctionsLogger.INVOKE_ENTRY, MethodID.fromMethodActor(methodActor));
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

    private static final Class[] defineClassParameterTypes = {String.class, byte[].class, int.class, int.class};

    @VM_ENTRY_POINT
    private static JniHandle DefineClass(Pointer env, Pointer slashifiedName, JniHandle classLoader, Pointer buffer, int length) throws ClassFormatError {
        // Source: JniFunctionsSource.java:90
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.DefineClass.ordinal(), UPCALL_ENTRY, anchor, env, slashifiedName, classLoader, buffer, Address.fromInt(length));
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.DefineClass.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:116
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.FindClass.ordinal(), UPCALL_ENTRY, anchor, env, name);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.FindClass.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static MethodID FromReflectedMethod(Pointer env, JniHandle reflectedMethod) {
        // Source: JniFunctionsSource.java:132
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.FromReflectedMethod.ordinal(), UPCALL_ENTRY, anchor, env, reflectedMethod);
        }

        try {
            final MethodActor methodActor = MethodActor.fromJava((Method) reflectedMethod.unhand());
            return MethodID.fromMethodActor(methodActor);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asMethodID(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.FromReflectedMethod.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static FieldID FromReflectedField(Pointer env, JniHandle field) {
        // Source: JniFunctionsSource.java:138
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.FromReflectedField.ordinal(), UPCALL_ENTRY, anchor, env, field);
        }

        try {
            final FieldActor fieldActor = FieldActor.fromJava((Field) field.unhand());
            return FieldID.fromFieldActor(fieldActor);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asFieldID(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.FromReflectedField.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:152
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ToReflectedMethod.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, methodID, Address.fromInt(isStatic ? 1 : 0));
        }

        try {
            return JniHandles.createLocalHandle(ToReflectedMethod(methodID, isStatic));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ToReflectedMethod.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetSuperclass(Pointer env, JniHandle subType) {
        // Source: JniFunctionsSource.java:157
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetSuperclass.ordinal(), UPCALL_ENTRY, anchor, env, subType);
        }

        try {
            return JniHandles.createLocalHandle(((Class) subType.unhand()).getSuperclass());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetSuperclass.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static boolean IsAssignableFrom(Pointer env, JniHandle subType, JniHandle superType) {
        // Source: JniFunctionsSource.java:162
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IsAssignableFrom.ordinal(), UPCALL_ENTRY, anchor, env, subType, superType);
        }

        try {
            return ClassActor.fromJava((Class) superType.unhand()).isAssignableFrom(ClassActor.fromJava((Class) subType.unhand()));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.IsAssignableFrom.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle ToReflectedField(Pointer env, JniHandle javaClass, FieldID fieldID, boolean isStatic) {
        // Source: JniFunctionsSource.java:167
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ToReflectedField.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, fieldID, Address.fromInt(isStatic ? 1 : 0));
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.ToReflectedField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static int Throw(Pointer env, JniHandle throwable) {
        // Source: JniFunctionsSource.java:176
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.Throw.ordinal(), UPCALL_ENTRY, anchor, env, throwable);
        }

        try {
            VmThread.fromJniEnv(env).setJniException((Throwable) throwable.unhand());
            return JNI_OK;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.Throw.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static int ThrowNew(Pointer env, JniHandle throwableClass, Pointer message) throws Throwable {
        // Source: JniFunctionsSource.java:182
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ThrowNew.ordinal(), UPCALL_ENTRY, anchor, env, throwableClass, message);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.ThrowNew.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle ExceptionOccurred(Pointer env) {
        // Source: JniFunctionsSource.java:199
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ExceptionOccurred.ordinal(), UPCALL_ENTRY, anchor, env);
        }

        try {
            return JniHandles.createLocalHandle(VmThread.fromJniEnv(env).jniException());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ExceptionOccurred.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void ExceptionDescribe(Pointer env) {
        // Source: JniFunctionsSource.java:204
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ExceptionDescribe.ordinal(), UPCALL_ENTRY, anchor, env);
        }

        try {
            final Throwable exception = VmThread.fromJniEnv(env).jniException();
            if (exception != null) {
                exception.printStackTrace();
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ExceptionDescribe.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void ExceptionClear(Pointer env) {
        // Source: JniFunctionsSource.java:212
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ExceptionClear.ordinal(), UPCALL_ENTRY, anchor, env);
        }

        try {
            VmThread.fromJniEnv(env).setJniException(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ExceptionClear.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void FatalError(Pointer env, Pointer message) {
        // Source: JniFunctionsSource.java:217
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.FatalError.ordinal(), UPCALL_ENTRY, anchor, env, message);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.FatalError.ordinal(), UPCALL_EXIT);
            }

        }
    }

    private static int PushLocalFrame0(Pointer env, int capacity) {
        JniHandles.pushLocalFrame(capacity);
        return JNI_OK;
    }

    @VM_ENTRY_POINT
    private static int PushLocalFrame(Pointer env, int capacity) {
        // Source: JniFunctionsSource.java:231
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.PushLocalFrame.ordinal(), UPCALL_ENTRY, anchor, env, Address.fromInt(capacity));
        }

        try {
            JniHandles.pushLocalFrame(capacity);
            return JNI_OK;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.PushLocalFrame.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle PopLocalFrame(Pointer env, JniHandle res) {
        // Source: JniFunctionsSource.java:237
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.PopLocalFrame.ordinal(), UPCALL_ENTRY, anchor, env, res);
        }

        try {
            return JniHandles.popLocalFrame(res);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.PopLocalFrame.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewGlobalRef(Pointer env, JniHandle handle) {
        // Source: JniFunctionsSource.java:242
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewGlobalRef.ordinal(), UPCALL_ENTRY, anchor, env, handle);
        }

        try {
            return JniHandles.createGlobalHandle(handle.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewGlobalRef.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void DeleteGlobalRef(Pointer env, JniHandle handle) {
        // Source: JniFunctionsSource.java:247
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.DeleteGlobalRef.ordinal(), UPCALL_ENTRY, anchor, env, handle);
        }

        try {
            JniHandles.destroyGlobalHandle(handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.DeleteGlobalRef.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void DeleteLocalRef(Pointer env, JniHandle handle) {
        // Source: JniFunctionsSource.java:252
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.DeleteLocalRef.ordinal(), UPCALL_ENTRY, anchor, env, handle);
        }

        try {
            JniHandles.destroyLocalHandle(handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.DeleteLocalRef.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static boolean IsSameObject(Pointer env, JniHandle object1, JniHandle object2) {
        // Source: JniFunctionsSource.java:257
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IsSameObject.ordinal(), UPCALL_ENTRY, anchor, env, object1, object2);
        }

        try {
            return object1.unhand() == object2.unhand();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.IsSameObject.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewLocalRef(Pointer env, JniHandle object) {
        // Source: JniFunctionsSource.java:262
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewLocalRef.ordinal(), UPCALL_ENTRY, anchor, env, object);
        }

        try {
            return JniHandles.createLocalHandle(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewLocalRef.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static int EnsureLocalCapacity(Pointer env, int capacity) {
        // Source: JniFunctionsSource.java:267
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.EnsureLocalCapacity.ordinal(), UPCALL_ENTRY, anchor, env, Address.fromInt(capacity));
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.EnsureLocalCapacity.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:283
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.AllocObject.ordinal(), UPCALL_ENTRY, anchor, env, javaClass);
        }

        try {
            return JniHandles.createLocalHandle(allocObject((Class) javaClass.unhand()));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.AllocObject.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native JniHandle NewObject(Pointer env, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:288

    @VM_ENTRY_POINT
    private static native JniHandle NewObjectV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:291

    @VM_ENTRY_POINT
    private static JniHandle NewObjectA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:294
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewObjectA.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, methodID, arguments);
        }

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
            logReflectiveInvocation(virtualMethodActor);
            return JniHandles.createLocalHandle(virtualMethodActor.invokeConstructor(argumentValues).asObject());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewObjectA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetObjectClass(Pointer env, JniHandle object) {
        // Source: JniFunctionsSource.java:319
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetObjectClass.ordinal(), UPCALL_ENTRY, anchor, env, object);
        }

        try {
            final Class javaClass = object.unhand().getClass();
            return JniHandles.createLocalHandle(javaClass);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetObjectClass.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static boolean IsInstanceOf(Pointer env, JniHandle object, JniHandle javaType) {
        // Source: JniFunctionsSource.java:325
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IsInstanceOf.ordinal(), UPCALL_ENTRY, anchor, env, object, javaType);
        }

        try {
            return ((Class) javaType.unhand()).isInstance(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.IsInstanceOf.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static MethodID GetMethodID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        // Source: JniFunctionsSource.java:330
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetMethodID.ordinal(), UPCALL_ENTRY, anchor, env, javaType, nameCString, descriptorCString);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.GetMethodID.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native JniHandle CallObjectMethod(Pointer env, JniHandle object, MethodID methodID);
        // Source: JniFunctionsSource.java:353

    @VM_ENTRY_POINT
    private static native JniHandle CallObjectMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer vaList);
        // Source: JniFunctionsSource.java:356

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
        logReflectiveInvocation(selectedMethod);
        return checkResult(expectedReturnKind, methodActor, selectedMethod.invoke(argumentValues));

    }

    @VM_ENTRY_POINT
    private static JniHandle CallObjectMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:464
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallObjectMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, methodID, arguments);
        }

        try {
            return JniHandles.createLocalHandle(CallValueMethodA(env, object, methodID, arguments, Kind.REFERENCE).asObject());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallObjectMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native boolean CallBooleanMethod(Pointer env, JniHandle object, MethodID methodID);
        // Source: JniFunctionsSource.java:469

    @VM_ENTRY_POINT
    private static native boolean CallBooleanMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer vaList);
        // Source: JniFunctionsSource.java:472

    @VM_ENTRY_POINT
    private static boolean CallBooleanMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:475
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallBooleanMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, methodID, arguments);
        }

        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.BOOLEAN).asBoolean();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallBooleanMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native byte CallByteMethod(Pointer env, JniHandle object, MethodID methodID);
        // Source: JniFunctionsSource.java:480

    @VM_ENTRY_POINT
    private static native byte CallByteMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:483

    @VM_ENTRY_POINT
    private static byte CallByteMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:486
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallByteMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, methodID, arguments);
        }

        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.BYTE).asByte();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallByteMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native char CallCharMethod(Pointer env, JniHandle object, MethodID methodID);
        // Source: JniFunctionsSource.java:491

    @VM_ENTRY_POINT
    private static native char CallCharMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:494

    @VM_ENTRY_POINT
    private static char CallCharMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:497
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallCharMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, methodID, arguments);
        }

        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.CHAR).asChar();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return  (char) JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallCharMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native short CallShortMethod(Pointer env, JniHandle object, MethodID methodID);
        // Source: JniFunctionsSource.java:502

    @VM_ENTRY_POINT
    private static native short CallShortMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:505

    @VM_ENTRY_POINT
    private static short CallShortMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:508
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallShortMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, methodID, arguments);
        }

        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.SHORT).asShort();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallShortMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native int CallIntMethod(Pointer env, JniHandle object, MethodID methodID);
        // Source: JniFunctionsSource.java:513

    @VM_ENTRY_POINT
    private static native int CallIntMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:516

    @VM_ENTRY_POINT
    private static int CallIntMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:519
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallIntMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, methodID, arguments);
        }

        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.INT).asInt();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallIntMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native long CallLongMethod(Pointer env, JniHandle object, MethodID methodID);
        // Source: JniFunctionsSource.java:524

    @VM_ENTRY_POINT
    private static native long CallLongMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:527

    @VM_ENTRY_POINT
    private static long CallLongMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:530
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallLongMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, methodID, arguments);
        }

        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.LONG).asLong();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallLongMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native float CallFloatMethod(Pointer env, JniHandle object, MethodID methodID);
        // Source: JniFunctionsSource.java:535

    @VM_ENTRY_POINT
    private static native float CallFloatMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:538

    @VM_ENTRY_POINT
    private static float CallFloatMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:541
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallFloatMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, methodID, arguments);
        }

        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.FLOAT).asFloat();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallFloatMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native double CallDoubleMethod(Pointer env, JniHandle object, MethodID methodID);
        // Source: JniFunctionsSource.java:546

    @VM_ENTRY_POINT
    private static native double CallDoubleMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:549

    @VM_ENTRY_POINT
    private static double CallDoubleMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:552
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallDoubleMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, methodID, arguments);
        }

        try {
            return CallValueMethodA(env, object, methodID, arguments, Kind.DOUBLE).asDouble();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallDoubleMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    private static Value CallNonvirtualValueMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments, Kind expectedReturnKind) throws Exception {
        // Following Hotspot, the javaClass argument is ignored; we only need the methodId
        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null || methodActor.isStatic() || methodActor.isInitializer()) {
            throw new NoSuchMethodException();
        }
        VirtualMethodActor virtualMethodActor;
        try {
            virtualMethodActor = (VirtualMethodActor) methodActor;
        } catch (ClassCastException ex) {
            throw new NoSuchMethodException();
        }
        final SignatureDescriptor signature = virtualMethodActor.descriptor();
        final Value[] argumentValues = new Value[1 + signature.numberOfParameters()];
        argumentValues[0] = ReferenceValue.from(object.unhand());
        copyJValueArrayToValueArray(arguments, signature, argumentValues, 1);
        logReflectiveInvocation(virtualMethodActor);
        return checkResult(expectedReturnKind, methodActor, virtualMethodActor.invoke(argumentValues));
    }

    @VM_ENTRY_POINT
    private static native void CallVoidMethod(Pointer env, JniHandle object, MethodID methodID);
        // Source: JniFunctionsSource.java:577

    @VM_ENTRY_POINT
    private static native void CallVoidMethodV(Pointer env, JniHandle object, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:580

    @VM_ENTRY_POINT
    private static void CallVoidMethodA(Pointer env, JniHandle object, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:583
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallVoidMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, methodID, arguments);
        }

        try {
            CallValueMethodA(env, object, methodID, arguments, Kind.VOID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallVoidMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native JniHandle CallNonvirtualObjectMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:588

    @VM_ENTRY_POINT
    private static native JniHandle CallNonvirtualObjectMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:591

    @VM_ENTRY_POINT
    private static JniHandle CallNonvirtualObjectMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:594
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallNonvirtualObjectMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, javaClass, methodID, arguments);
        }

        try {
            return JniHandles.createLocalHandle(CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.REFERENCE).asObject());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallNonvirtualObjectMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native boolean CallNonvirtualBooleanMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:599

    @VM_ENTRY_POINT
    private static native boolean CallNonvirtualBooleanMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:602

    @VM_ENTRY_POINT
    private static boolean CallNonvirtualBooleanMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:605
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallNonvirtualBooleanMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, javaClass, methodID, arguments);
        }

        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.BOOLEAN).asBoolean();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallNonvirtualBooleanMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native byte CallNonvirtualByteMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:610

    @VM_ENTRY_POINT
    private static native byte CallNonvirtualByteMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:613

    @VM_ENTRY_POINT
    private static byte CallNonvirtualByteMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:616
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallNonvirtualByteMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, javaClass, methodID, arguments);
        }

        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.BYTE).asByte();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallNonvirtualByteMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native char CallNonvirtualCharMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:621

    @VM_ENTRY_POINT
    private static native char CallNonvirtualCharMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:624

    @VM_ENTRY_POINT
    private static char CallNonvirtualCharMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:627
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallNonvirtualCharMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, javaClass, methodID, arguments);
        }

        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.CHAR).asChar();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return  (char) JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallNonvirtualCharMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native short CallNonvirtualShortMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:632

    @VM_ENTRY_POINT
    private static native short CallNonvirtualShortMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:635

    @VM_ENTRY_POINT
    private static short CallNonvirtualShortMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:638
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallNonvirtualShortMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, javaClass, methodID, arguments);
        }

        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.SHORT).asShort();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallNonvirtualShortMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native int CallNonvirtualIntMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:643

    @VM_ENTRY_POINT
    private static native int CallNonvirtualIntMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:646

    @VM_ENTRY_POINT
    private static int CallNonvirtualIntMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:649
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallNonvirtualIntMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, javaClass, methodID, arguments);
        }

        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.INT).asInt();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallNonvirtualIntMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native long CallNonvirtualLongMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:654

    @VM_ENTRY_POINT
    private static native long CallNonvirtualLongMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:657

    @VM_ENTRY_POINT
    private static long CallNonvirtualLongMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:660
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallNonvirtualLongMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, javaClass, methodID, arguments);
        }

        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.LONG).asLong();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallNonvirtualLongMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native float CallNonvirtualFloatMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:665

    @VM_ENTRY_POINT
    private static native float CallNonvirtualFloatMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:668

    @VM_ENTRY_POINT
    private static float CallNonvirtualFloatMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:671
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallNonvirtualFloatMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, javaClass, methodID, arguments);
        }

        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.FLOAT).asFloat();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallNonvirtualFloatMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native double CallNonvirtualDoubleMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:676

    @VM_ENTRY_POINT
    private static native double CallNonvirtualDoubleMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:679

    @VM_ENTRY_POINT
    private static double CallNonvirtualDoubleMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:682
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallNonvirtualDoubleMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, javaClass, methodID, arguments);
        }

        try {
            return CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.DOUBLE).asDouble();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallNonvirtualDoubleMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native void CallNonvirtualVoidMethod(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:687

    @VM_ENTRY_POINT
    private static native void CallNonvirtualVoidMethodV(Pointer env, JniHandle object, JniHandle javaClass, Pointer arguments);
        // Source: JniFunctionsSource.java:690

    @VM_ENTRY_POINT
    private static void CallNonvirtualVoidMethodA(Pointer env, JniHandle object, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:693
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallNonvirtualVoidMethodA.ordinal(), UPCALL_ENTRY, anchor, env, object, javaClass, methodID, arguments);
        }

        try {
            CallNonvirtualValueMethodA(env, object, javaClass, methodID, arguments, Kind.VOID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallNonvirtualVoidMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static FieldID GetFieldID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        // Source: JniFunctionsSource.java:698
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetFieldID.ordinal(), UPCALL_ENTRY, anchor, env, javaType, nameCString, descriptorCString);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.GetFieldID.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetObjectField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:722
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetObjectField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID);
        }

        try {
            return JniHandles.createLocalHandle(FieldID.toFieldActor(fieldID).getObject(object.unhand()));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetObjectField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static boolean GetBooleanField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:727
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetBooleanField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getBoolean(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetBooleanField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static byte GetByteField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:732
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetByteField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getByte(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetByteField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static char GetCharField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:737
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetCharField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getChar(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return  (char) JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetCharField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static short GetShortField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:742
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetShortField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getShort(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetShortField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static int GetIntField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:747
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetIntField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getInt(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetIntField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static long GetLongField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:752
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetLongField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getLong(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetLongField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static float GetFloatField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:757
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetFloatField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getFloat(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetFloatField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static double GetDoubleField(Pointer env, JniHandle object, FieldID fieldID) {
        // Source: JniFunctionsSource.java:762
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetDoubleField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getDouble(object.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetDoubleField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetObjectField(Pointer env, JniHandle object, FieldID fieldID, JniHandle value) {
        // Source: JniFunctionsSource.java:767
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetObjectField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID, value);
        }

        try {
            FieldID.toFieldActor(fieldID).setObject(object.unhand(), value.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetObjectField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetBooleanField(Pointer env, JniHandle object, FieldID fieldID, boolean value) {
        // Source: JniFunctionsSource.java:772
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetBooleanField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID, Address.fromInt(value ? 1 : 0));
        }

        try {
            FieldID.toFieldActor(fieldID).setBoolean(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetBooleanField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetByteField(Pointer env, JniHandle object, FieldID fieldID, byte value) {
        // Source: JniFunctionsSource.java:777
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetByteField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID, Address.fromInt(value));
        }

        try {
            FieldID.toFieldActor(fieldID).setByte(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetByteField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetCharField(Pointer env, JniHandle object, FieldID fieldID, char value) {
        // Source: JniFunctionsSource.java:782
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetCharField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID, Address.fromInt(value));
        }

        try {
            FieldID.toFieldActor(fieldID).setChar(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetCharField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetShortField(Pointer env, JniHandle object, FieldID fieldID, short value) {
        // Source: JniFunctionsSource.java:787
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetShortField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID, Address.fromInt(value));
        }

        try {
            FieldID.toFieldActor(fieldID).setShort(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetShortField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetIntField(Pointer env, JniHandle object, FieldID fieldID, int value) {
        // Source: JniFunctionsSource.java:792
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetIntField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID, Address.fromInt(value));
        }

        try {
            FieldID.toFieldActor(fieldID).setInt(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetIntField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetLongField(Pointer env, JniHandle object, FieldID fieldID, long value) {
        // Source: JniFunctionsSource.java:797
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetLongField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID, Address.fromLong(value));
        }

        try {
            FieldID.toFieldActor(fieldID).setLong(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetLongField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetFloatField(Pointer env, JniHandle object, FieldID fieldID, float value) {
        // Source: JniFunctionsSource.java:802
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetFloatField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID, Address.fromInt(Float.floatToRawIntBits(value)));
        }

        try {
            FieldID.toFieldActor(fieldID).setFloat(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetFloatField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetDoubleField(Pointer env, JniHandle object, FieldID fieldID, double value) {
        // Source: JniFunctionsSource.java:807
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetDoubleField.ordinal(), UPCALL_ENTRY, anchor, env, object, fieldID, Address.fromLong(Double.doubleToRawLongBits(value)));
        }

        try {
            FieldID.toFieldActor(fieldID).setDouble(object.unhand(), value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetDoubleField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static MethodID GetStaticMethodID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        // Source: JniFunctionsSource.java:812
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStaticMethodID.ordinal(), UPCALL_ENTRY, anchor, env, javaType, nameCString, descriptorCString);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.GetStaticMethodID.ordinal(), UPCALL_EXIT);
            }

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
        logReflectiveInvocation(methodActor);
        return checkResult(expectedReturnKind, methodActor, methodActor.invoke(argumentValues));
    }

    @VM_ENTRY_POINT
    private static native JniHandle CallStaticObjectMethod(Pointer env, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:859

    @VM_ENTRY_POINT
    private static native JniHandle CallStaticObjectMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:862

    @VM_ENTRY_POINT
    private static JniHandle CallStaticObjectMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:865
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallStaticObjectMethodA.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, methodID, arguments);
        }

        try {
            return JniHandles.createLocalHandle(CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.REFERENCE).asObject());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallStaticObjectMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native boolean CallStaticBooleanMethod(Pointer env, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:870

    @VM_ENTRY_POINT
    private static native boolean CallStaticBooleanMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:873

    @VM_ENTRY_POINT
    private static boolean CallStaticBooleanMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:876
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallStaticBooleanMethodA.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, methodID, arguments);
        }

        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.BOOLEAN).asBoolean();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallStaticBooleanMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native byte CallStaticByteMethod(Pointer env, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:881

    @VM_ENTRY_POINT
    private static native byte CallStaticByteMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:884

    @VM_ENTRY_POINT
    private static byte CallStaticByteMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:887
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallStaticByteMethodA.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, methodID, arguments);
        }

        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.BYTE).asByte();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallStaticByteMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native char CallStaticCharMethod(Pointer env, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:892

    @VM_ENTRY_POINT
    private static native char CallStaticCharMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:895

    @VM_ENTRY_POINT
    private static char CallStaticCharMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:898
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallStaticCharMethodA.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, methodID, arguments);
        }

        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.CHAR).asChar();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return  (char) JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallStaticCharMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native short CallStaticShortMethod(Pointer env, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:903

    @VM_ENTRY_POINT
    private static native short CallStaticShortMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:906

    @VM_ENTRY_POINT
    private static short CallStaticShortMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:909
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallStaticShortMethodA.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, methodID, arguments);
        }

        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.SHORT).asShort();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallStaticShortMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native int CallStaticIntMethod(Pointer env, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:914

    @VM_ENTRY_POINT
    private static native int CallStaticIntMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:917

    @VM_ENTRY_POINT
    private static int CallStaticIntMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:920
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallStaticIntMethodA.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, methodID, arguments);
        }

        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.INT).asInt();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallStaticIntMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native long CallStaticLongMethod(Pointer env, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:925

    @VM_ENTRY_POINT
    private static native long CallStaticLongMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:928

    @VM_ENTRY_POINT
    private static long CallStaticLongMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:931
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallStaticLongMethodA.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, methodID, arguments);
        }

        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.LONG).asLong();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallStaticLongMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native float CallStaticFloatMethod(Pointer env, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:936

    @VM_ENTRY_POINT
    private static native float CallStaticFloatMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:939

    @VM_ENTRY_POINT
    private static float CallStaticFloatMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:942
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallStaticFloatMethodA.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, methodID, arguments);
        }

        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.FLOAT).asFloat();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallStaticFloatMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native double CallStaticDoubleMethod(Pointer env, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:947

    @VM_ENTRY_POINT
    private static native double CallStaticDoubleMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:950

    @VM_ENTRY_POINT
    private static double CallStaticDoubleMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:953
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallStaticDoubleMethodA.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, methodID, arguments);
        }

        try {
            return CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.DOUBLE).asDouble();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallStaticDoubleMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native void CallStaticVoidMethod(Pointer env, JniHandle javaClass, MethodID methodID);
        // Source: JniFunctionsSource.java:958

    @VM_ENTRY_POINT
    private static native void CallStaticVoidMethodV(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments);
        // Source: JniFunctionsSource.java:961

    @VM_ENTRY_POINT
    private static void CallStaticVoidMethodA(Pointer env, JniHandle javaClass, MethodID methodID, Pointer arguments) throws Exception {
        // Source: JniFunctionsSource.java:964
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CallStaticVoidMethodA.ordinal(), UPCALL_ENTRY, anchor, env, javaClass, methodID, arguments);
        }

        try {
            CallStaticValueMethodA(env, javaClass, methodID, arguments, Kind.VOID);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.CallStaticVoidMethodA.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static FieldID GetStaticFieldID(Pointer env, JniHandle javaType, Pointer nameCString, Pointer descriptorCString) {
        // Source: JniFunctionsSource.java:969
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStaticFieldID.ordinal(), UPCALL_ENTRY, anchor, env, javaType, nameCString, descriptorCString);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.GetStaticFieldID.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetStaticObjectField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:992
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStaticObjectField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID);
        }

        try {
            return JniHandles.createLocalHandle(FieldID.toFieldActor(fieldID).getObject(null));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStaticObjectField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static boolean GetStaticBooleanField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:997
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStaticBooleanField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getBoolean(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStaticBooleanField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static byte GetStaticByteField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1002
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStaticByteField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getByte(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStaticByteField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static char GetStaticCharField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1007
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStaticCharField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getChar(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return  (char) JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStaticCharField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static short GetStaticShortField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1012
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStaticShortField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getShort(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStaticShortField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static int GetStaticIntField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1017
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStaticIntField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getInt(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStaticIntField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static long GetStaticLongField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1022
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStaticLongField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getLong(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStaticLongField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static float GetStaticFloatField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1027
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStaticFloatField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getFloat(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStaticFloatField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static double GetStaticDoubleField(Pointer env, JniHandle javaType, FieldID fieldID) {
        // Source: JniFunctionsSource.java:1032
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStaticDoubleField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID);
        }

        try {
            return FieldID.toFieldActor(fieldID).getDouble(null);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStaticDoubleField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticObjectField(Pointer env, JniHandle javaType, FieldID fieldID, JniHandle value) {
        // Source: JniFunctionsSource.java:1037
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetStaticObjectField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID, value);
        }

        try {
            FieldID.toFieldActor(fieldID).setObject(null, value.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetStaticObjectField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticBooleanField(Pointer env, JniHandle javaType, FieldID fieldID, boolean value) {
        // Source: JniFunctionsSource.java:1042
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetStaticBooleanField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID, Address.fromInt(value ? 1 : 0));
        }

        try {
            FieldID.toFieldActor(fieldID).setBoolean(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetStaticBooleanField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticByteField(Pointer env, JniHandle javaType, FieldID fieldID, byte value) {
        // Source: JniFunctionsSource.java:1047
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetStaticByteField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID, Address.fromInt(value));
        }

        try {
            FieldID.toFieldActor(fieldID).setByte(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetStaticByteField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticCharField(Pointer env, JniHandle javaType, FieldID fieldID, char value) {
        // Source: JniFunctionsSource.java:1052
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetStaticCharField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID, Address.fromInt(value));
        }

        try {
            FieldID.toFieldActor(fieldID).setChar(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetStaticCharField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticShortField(Pointer env, JniHandle javaType, FieldID fieldID, short value) {
        // Source: JniFunctionsSource.java:1057
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetStaticShortField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID, Address.fromInt(value));
        }

        try {
            FieldID.toFieldActor(fieldID).setShort(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetStaticShortField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticIntField(Pointer env, JniHandle javaType, FieldID fieldID, int value) {
        // Source: JniFunctionsSource.java:1062
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetStaticIntField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID, Address.fromInt(value));
        }

        try {
            FieldID.toFieldActor(fieldID).setInt(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetStaticIntField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticLongField(Pointer env, JniHandle javaType, FieldID fieldID, long value) {
        // Source: JniFunctionsSource.java:1067
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetStaticLongField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID, Address.fromLong(value));
        }

        try {
            FieldID.toFieldActor(fieldID).setLong(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetStaticLongField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticFloatField(Pointer env, JniHandle javaType, FieldID fieldID, float value) {
        // Source: JniFunctionsSource.java:1072
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetStaticFloatField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID, Address.fromInt(Float.floatToRawIntBits(value)));
        }

        try {
            FieldID.toFieldActor(fieldID).setFloat(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetStaticFloatField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetStaticDoubleField(Pointer env, JniHandle javaType, FieldID fieldID, double value) {
        // Source: JniFunctionsSource.java:1077
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetStaticDoubleField.ordinal(), UPCALL_ENTRY, anchor, env, javaType, fieldID, Address.fromLong(Double.doubleToRawLongBits(value)));
        }

        try {
            FieldID.toFieldActor(fieldID).setDouble(null, value);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetStaticDoubleField.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewString(Pointer env, Pointer chars, int length) {
        // Source: JniFunctionsSource.java:1082
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewString.ordinal(), UPCALL_ENTRY, anchor, env, chars, Address.fromInt(length));
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.NewString.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static int GetStringLength(Pointer env, JniHandle string) {
        // Source: JniFunctionsSource.java:1091
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStringLength.ordinal(), UPCALL_ENTRY, anchor, env, string);
        }

        try {
            return ((String) string.unhand()).length();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStringLength.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static Pointer GetStringChars(Pointer env, JniHandle string, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1096
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStringChars.ordinal(), UPCALL_ENTRY, anchor, env, string, isCopy);
        }

        try {
            setCopyPointer(isCopy, true);
            return copyString((String) string.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStringChars.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void ReleaseStringChars(Pointer env, JniHandle string, Pointer chars) {
        // Source: JniFunctionsSource.java:1102
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ReleaseStringChars.ordinal(), UPCALL_ENTRY, anchor, env, string, chars);
        }

        try {
            Memory.deallocate(chars);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ReleaseStringChars.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewStringUTF(Pointer env, Pointer utf) {
        // Source: JniFunctionsSource.java:1107
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewStringUTF.ordinal(), UPCALL_ENTRY, anchor, env, utf);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.NewStringUTF.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static int GetStringUTFLength(Pointer env, JniHandle string) {
        // Source: JniFunctionsSource.java:1116
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStringUTFLength.ordinal(), UPCALL_ENTRY, anchor, env, string);
        }

        try {
            return Utf8.utf8Length((String) string.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStringUTFLength.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static Pointer GetStringUTFChars(Pointer env, JniHandle string, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1121
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStringUTFChars.ordinal(), UPCALL_ENTRY, anchor, env, string, isCopy);
        }

        try {
            setCopyPointer(isCopy, true);
            return CString.utf8FromJava((String) string.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStringUTFChars.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void ReleaseStringUTFChars(Pointer env, JniHandle string, Pointer chars) {
        // Source: JniFunctionsSource.java:1127
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ReleaseStringUTFChars.ordinal(), UPCALL_ENTRY, anchor, env, string, chars);
        }

        try {
            Memory.deallocate(chars);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ReleaseStringUTFChars.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static int GetArrayLength(Pointer env, JniHandle array) {
        // Source: JniFunctionsSource.java:1132
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetArrayLength.ordinal(), UPCALL_ENTRY, anchor, env, array);
        }

        try {
            return Array.getLength(array.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetArrayLength.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewObjectArray(Pointer env, int length, JniHandle elementType, JniHandle initialElementValue) {
        // Source: JniFunctionsSource.java:1137
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewObjectArray.ordinal(), UPCALL_ENTRY, anchor, env, Address.fromInt(length), elementType, initialElementValue);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.NewObjectArray.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetObjectArrayElement(Pointer env, JniHandle array, int index) {
        // Source: JniFunctionsSource.java:1147
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetObjectArrayElement.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(index));
        }

        try {
            return JniHandles.createLocalHandle(((Object[]) array.unhand())[index]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetObjectArrayElement.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetObjectArrayElement(Pointer env, JniHandle array, int index, JniHandle value) {
        // Source: JniFunctionsSource.java:1152
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetObjectArrayElement.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(index), value);
        }

        try {
            ((Object[]) array.unhand())[index] = value.unhand();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetObjectArrayElement.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewBooleanArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1157
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewBooleanArray.ordinal(), UPCALL_ENTRY, anchor, env, Address.fromInt(length));
        }

        try {
            return JniHandles.createLocalHandle(new boolean[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewBooleanArray.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewByteArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1162
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewByteArray.ordinal(), UPCALL_ENTRY, anchor, env, Address.fromInt(length));
        }

        try {
            return JniHandles.createLocalHandle(new byte[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewByteArray.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewCharArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1167
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewCharArray.ordinal(), UPCALL_ENTRY, anchor, env, Address.fromInt(length));
        }

        try {
            return JniHandles.createLocalHandle(new char[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewCharArray.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewShortArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1172
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewShortArray.ordinal(), UPCALL_ENTRY, anchor, env, Address.fromInt(length));
        }

        try {
            return JniHandles.createLocalHandle(new short[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewShortArray.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewIntArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1177
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewIntArray.ordinal(), UPCALL_ENTRY, anchor, env, Address.fromInt(length));
        }

        try {
            return JniHandles.createLocalHandle(new int[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewIntArray.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewLongArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1182
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewLongArray.ordinal(), UPCALL_ENTRY, anchor, env, Address.fromInt(length));
        }

        try {
            return JniHandles.createLocalHandle(new long[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewLongArray.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewFloatArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1187
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewFloatArray.ordinal(), UPCALL_ENTRY, anchor, env, Address.fromInt(length));
        }

        try {
            return JniHandles.createLocalHandle(new float[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewFloatArray.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewDoubleArray(Pointer env, int length) {
        // Source: JniFunctionsSource.java:1192
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewDoubleArray.ordinal(), UPCALL_ENTRY, anchor, env, Address.fromInt(length));
        }

        try {
            return JniHandles.createLocalHandle(new double[length]);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewDoubleArray.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static Pointer GetBooleanArrayElements(Pointer env, JniHandle array, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1197
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetBooleanArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, isCopy);
        }

        try {
            return getBooleanArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetBooleanArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1212
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetByteArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, isCopy);
        }

        try {
            return getByteArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetByteArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1227
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetCharArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, isCopy);
        }

        try {
            return getCharArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetCharArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1242
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetShortArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, isCopy);
        }

        try {
            return getShortArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetShortArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1257
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetIntArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, isCopy);
        }

        try {
            return getIntArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetIntArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1272
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetLongArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, isCopy);
        }

        try {
            return getLongArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetLongArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1287
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetFloatArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, isCopy);
        }

        try {
            return getFloatArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetFloatArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1302
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetDoubleArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, isCopy);
        }

        try {
            return getDoubleArrayElements(array, isCopy);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetDoubleArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1317
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ReleaseBooleanArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, elements, Address.fromInt(mode));
        }

        try {
            releaseBooleanArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ReleaseBooleanArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1332
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ReleaseByteArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, elements, Address.fromInt(mode));
        }

        try {
            releaseByteArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ReleaseByteArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1347
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ReleaseCharArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, elements, Address.fromInt(mode));
        }

        try {
            releaseCharArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ReleaseCharArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1362
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ReleaseShortArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, elements, Address.fromInt(mode));
        }

        try {
            releaseShortArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ReleaseShortArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1377
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ReleaseIntArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, elements, Address.fromInt(mode));
        }

        try {
            releaseIntArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ReleaseIntArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1392
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ReleaseLongArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, elements, Address.fromInt(mode));
        }

        try {
            releaseLongArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ReleaseLongArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1407
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ReleaseFloatArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, elements, Address.fromInt(mode));
        }

        try {
            releaseFloatArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ReleaseFloatArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1422
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ReleaseDoubleArrayElements.ordinal(), UPCALL_ENTRY, anchor, env, array, elements, Address.fromInt(mode));
        }

        try {
            releaseDoubleArrayElements(array, elements, mode);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ReleaseDoubleArrayElements.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1437
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetBooleanArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final boolean[] a = (boolean[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setBoolean(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetBooleanArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void GetByteArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1445
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetByteArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final byte[] a = (byte[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setByte(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetByteArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void GetCharArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1453
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetCharArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final char[] a = (char[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setChar(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetCharArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void GetShortArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1461
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetShortArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final short[] a = (short[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setShort(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetShortArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void GetIntArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1469
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetIntArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final int[] a = (int[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setInt(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetIntArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void GetLongArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1477
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetLongArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final long[] a = (long[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setLong(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetLongArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void GetFloatArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1485
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetFloatArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final float[] a = (float[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setFloat(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetFloatArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void GetDoubleArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1493
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetDoubleArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final double[] a = (double[]) array.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setDouble(i, a[start + i]);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetDoubleArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetBooleanArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1501
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetBooleanArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final boolean[] a = (boolean[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getBoolean(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetBooleanArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetByteArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1509
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetByteArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final byte[] a = (byte[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getByte(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetByteArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetCharArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1517
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetCharArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final char[] a = (char[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getChar(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetCharArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetShortArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1525
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetShortArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final short[] a = (short[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getShort(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetShortArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetIntArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1533
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetIntArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final int[] a = (int[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getInt(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetIntArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetLongArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1541
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetLongArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final long[] a = (long[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getLong(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetLongArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetFloatArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1549
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetFloatArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final float[] a = (float[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getFloat(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetFloatArrayRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void SetDoubleArrayRegion(Pointer env, JniHandle array, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1557
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetDoubleArrayRegion.ordinal(), UPCALL_ENTRY, anchor, env, array, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final double[] a = (double[]) array.unhand();
            for (int i = 0; i < length; i++) {
                a[start + i] = buffer.getDouble(i);
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.SetDoubleArrayRegion.ordinal(), UPCALL_EXIT);
            }

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
        // Source: JniFunctionsSource.java:1573
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.RegisterNatives.ordinal(), UPCALL_ENTRY, anchor, env, javaType, methods, Address.fromInt(numberOfMethods));
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.RegisterNatives.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static int UnregisterNatives(Pointer env, JniHandle javaType) {
        // Source: JniFunctionsSource.java:1611
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.UnregisterNatives.ordinal(), UPCALL_ENTRY, anchor, env, javaType);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.UnregisterNatives.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static int MonitorEnter(Pointer env, JniHandle object) {
        // Source: JniFunctionsSource.java:1626
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.MonitorEnter.ordinal(), UPCALL_ENTRY, anchor, env, object);
        }

        try {
            Monitor.enter(object.unhand());
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.MonitorEnter.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static int MonitorExit(Pointer env, JniHandle object) {
        // Source: JniFunctionsSource.java:1632
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.MonitorExit.ordinal(), UPCALL_ENTRY, anchor, env, object);
        }

        try {
            Monitor.exit(object.unhand());
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.MonitorExit.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static native int GetJavaVM(Pointer env, Pointer vmPointerPointer);
        // Source: JniFunctionsSource.java:1638

    @VM_ENTRY_POINT
    private static void GetStringRegion(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1641
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStringRegion.ordinal(), UPCALL_ENTRY, anchor, env, string, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final String s = (String) string.unhand();
            for (int i = 0; i < length; i++) {
                buffer.setChar(i, s.charAt(i + start));
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStringRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void GetStringUTFRegion(Pointer env, JniHandle string, int start, int length, Pointer buffer) {
        // Source: JniFunctionsSource.java:1649
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStringUTFRegion.ordinal(), UPCALL_ENTRY, anchor, env, string, Address.fromInt(start), Address.fromInt(length), buffer);
        }

        try {
            final String s = ((String) string.unhand()).substring(start, start + length);
            final byte[] utf = Utf8.stringToUtf8(s);
            Memory.writeBytes(utf, utf.length, buffer);
            buffer.setByte(utf.length, (byte) 0); // zero termination
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStringUTFRegion.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static Pointer GetPrimitiveArrayCritical(Pointer env, JniHandle array, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1657
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetPrimitiveArrayCritical.ordinal(), UPCALL_ENTRY, anchor, env, array, isCopy);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.GetPrimitiveArrayCritical.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void ReleasePrimitiveArrayCritical(Pointer env, JniHandle array, Pointer elements, int mode) {
        // Source: JniFunctionsSource.java:1685
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ReleasePrimitiveArrayCritical.ordinal(), UPCALL_ENTRY, anchor, env, array, elements, Address.fromInt(mode));
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.ReleasePrimitiveArrayCritical.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static Pointer GetStringCritical(Pointer env, JniHandle string, Pointer isCopy) {
        // Source: JniFunctionsSource.java:1710
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStringCritical.ordinal(), UPCALL_ENTRY, anchor, env, string, isCopy);
        }

        try {
            // TODO(cwi): Implement optimized version for OptimizeJNICritical if a benchmark uses it frequently
            setCopyPointer(isCopy, true);
            return copyString((String) string.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asPointer(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.GetStringCritical.ordinal(), UPCALL_EXIT);
            }

        }
    }

    private static Pointer copyString(String string) {
        final Pointer pointer = Memory.mustAllocate(string.length() * Kind.CHAR.width.numberOfBytes);
        for (int i = 0; i < string.length(); i++) {
            pointer.setChar(i, string.charAt(i));
        }
        return pointer;
    }

    @VM_ENTRY_POINT
    private static void ReleaseStringCritical(Pointer env, JniHandle string, Pointer chars) {
        // Source: JniFunctionsSource.java:1725
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ReleaseStringCritical.ordinal(), UPCALL_ENTRY, anchor, env, string, chars);
        }

        try {
            Memory.deallocate(chars);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ReleaseStringCritical.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static JniHandle NewWeakGlobalRef(Pointer env, JniHandle handle) {
        // Source: JniFunctionsSource.java:1730
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewWeakGlobalRef.ordinal(), UPCALL_ENTRY, anchor, env, handle);
        }

        try {
            return JniHandles.createWeakGlobalHandle(handle.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewWeakGlobalRef.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static void DeleteWeakGlobalRef(Pointer env, JniHandle handle) {
        // Source: JniFunctionsSource.java:1735
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.DeleteWeakGlobalRef.ordinal(), UPCALL_ENTRY, anchor, env, handle);
        }

        try {
            JniHandles.destroyWeakGlobalHandle(handle);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.DeleteWeakGlobalRef.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static boolean ExceptionCheck(Pointer env) {
        // Source: JniFunctionsSource.java:1740
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ExceptionCheck.ordinal(), UPCALL_ENTRY, anchor, env);
        }

        try {
            return VmThread.fromJniEnv(env).jniException() != null;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.ExceptionCheck.ordinal(), UPCALL_EXIT);
            }

        }
    }

    private static final ClassActor DirectByteBuffer = ClassActor.fromJava(Classes.forName("java.nio.DirectByteBuffer"));

    @VM_ENTRY_POINT
    private static JniHandle NewDirectByteBuffer(Pointer env, Pointer address, long capacity) throws Exception {
        // Source: JniFunctionsSource.java:1747
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NewDirectByteBuffer.ordinal(), UPCALL_ENTRY, anchor, env, address, Address.fromLong(capacity));
        }

        try {
            ByteBuffer buffer = ObjectAccess.createDirectByteBuffer(address.toLong(), (int) capacity);
            return JniHandles.createLocalHandle(buffer);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
            if (logger.enabled()) {
                logger.log(LogOperations.NewDirectByteBuffer.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static Pointer GetDirectBufferAddress(Pointer env, JniHandle buffer) throws Exception {
        // Source: JniFunctionsSource.java:1753
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetDirectBufferAddress.ordinal(), UPCALL_ENTRY, anchor, env, buffer);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.GetDirectBufferAddress.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static long GetDirectBufferCapacity(Pointer env, JniHandle buffer) {
        // Source: JniFunctionsSource.java:1763
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetDirectBufferCapacity.ordinal(), UPCALL_ENTRY, anchor, env, buffer);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.GetDirectBufferCapacity.ordinal(), UPCALL_EXIT);
            }

        }
    }

    @VM_ENTRY_POINT
    private static int GetObjectRefType(Pointer env, JniHandle obj) {
        // Source: JniFunctionsSource.java:1772
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetObjectRefType.ordinal(), UPCALL_ENTRY, anchor, env, obj);
        }

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
            if (logger.enabled()) {
                logger.log(LogOperations.GetObjectRefType.ordinal(), UPCALL_EXIT);
            }

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

    public static enum LogOperations {
        /* 0 */ DefineClass,
        /* 1 */ FindClass,
        /* 2 */ FromReflectedMethod,
        /* 3 */ FromReflectedField,
        /* 4 */ ToReflectedMethod,
        /* 5 */ GetSuperclass,
        /* 6 */ IsAssignableFrom,
        /* 7 */ ToReflectedField,
        /* 8 */ Throw,
        /* 9 */ ThrowNew,
        /* 10 */ ExceptionOccurred,
        /* 11 */ ExceptionDescribe,
        /* 12 */ ExceptionClear,
        /* 13 */ FatalError,
        /* 14 */ PushLocalFrame,
        /* 15 */ PopLocalFrame,
        /* 16 */ NewGlobalRef,
        /* 17 */ DeleteGlobalRef,
        /* 18 */ DeleteLocalRef,
        /* 19 */ IsSameObject,
        /* 20 */ NewLocalRef,
        /* 21 */ EnsureLocalCapacity,
        /* 22 */ AllocObject,
        /* 23 */ NewObjectA,
        /* 24 */ GetObjectClass,
        /* 25 */ IsInstanceOf,
        /* 26 */ GetMethodID,
        /* 27 */ CallObjectMethodA,
        /* 28 */ CallBooleanMethodA,
        /* 29 */ CallByteMethodA,
        /* 30 */ CallCharMethodA,
        /* 31 */ CallShortMethodA,
        /* 32 */ CallIntMethodA,
        /* 33 */ CallLongMethodA,
        /* 34 */ CallFloatMethodA,
        /* 35 */ CallDoubleMethodA,
        /* 36 */ CallVoidMethodA,
        /* 37 */ CallNonvirtualObjectMethodA,
        /* 38 */ CallNonvirtualBooleanMethodA,
        /* 39 */ CallNonvirtualByteMethodA,
        /* 40 */ CallNonvirtualCharMethodA,
        /* 41 */ CallNonvirtualShortMethodA,
        /* 42 */ CallNonvirtualIntMethodA,
        /* 43 */ CallNonvirtualLongMethodA,
        /* 44 */ CallNonvirtualFloatMethodA,
        /* 45 */ CallNonvirtualDoubleMethodA,
        /* 46 */ CallNonvirtualVoidMethodA,
        /* 47 */ GetFieldID,
        /* 48 */ GetObjectField,
        /* 49 */ GetBooleanField,
        /* 50 */ GetByteField,
        /* 51 */ GetCharField,
        /* 52 */ GetShortField,
        /* 53 */ GetIntField,
        /* 54 */ GetLongField,
        /* 55 */ GetFloatField,
        /* 56 */ GetDoubleField,
        /* 57 */ SetObjectField,
        /* 58 */ SetBooleanField,
        /* 59 */ SetByteField,
        /* 60 */ SetCharField,
        /* 61 */ SetShortField,
        /* 62 */ SetIntField,
        /* 63 */ SetLongField,
        /* 64 */ SetFloatField,
        /* 65 */ SetDoubleField,
        /* 66 */ GetStaticMethodID,
        /* 67 */ CallStaticObjectMethodA,
        /* 68 */ CallStaticBooleanMethodA,
        /* 69 */ CallStaticByteMethodA,
        /* 70 */ CallStaticCharMethodA,
        /* 71 */ CallStaticShortMethodA,
        /* 72 */ CallStaticIntMethodA,
        /* 73 */ CallStaticLongMethodA,
        /* 74 */ CallStaticFloatMethodA,
        /* 75 */ CallStaticDoubleMethodA,
        /* 76 */ CallStaticVoidMethodA,
        /* 77 */ GetStaticFieldID,
        /* 78 */ GetStaticObjectField,
        /* 79 */ GetStaticBooleanField,
        /* 80 */ GetStaticByteField,
        /* 81 */ GetStaticCharField,
        /* 82 */ GetStaticShortField,
        /* 83 */ GetStaticIntField,
        /* 84 */ GetStaticLongField,
        /* 85 */ GetStaticFloatField,
        /* 86 */ GetStaticDoubleField,
        /* 87 */ SetStaticObjectField,
        /* 88 */ SetStaticBooleanField,
        /* 89 */ SetStaticByteField,
        /* 90 */ SetStaticCharField,
        /* 91 */ SetStaticShortField,
        /* 92 */ SetStaticIntField,
        /* 93 */ SetStaticLongField,
        /* 94 */ SetStaticFloatField,
        /* 95 */ SetStaticDoubleField,
        /* 96 */ NewString,
        /* 97 */ GetStringLength,
        /* 98 */ GetStringChars,
        /* 99 */ ReleaseStringChars,
        /* 100 */ NewStringUTF,
        /* 101 */ GetStringUTFLength,
        /* 102 */ GetStringUTFChars,
        /* 103 */ ReleaseStringUTFChars,
        /* 104 */ GetArrayLength,
        /* 105 */ NewObjectArray,
        /* 106 */ GetObjectArrayElement,
        /* 107 */ SetObjectArrayElement,
        /* 108 */ NewBooleanArray,
        /* 109 */ NewByteArray,
        /* 110 */ NewCharArray,
        /* 111 */ NewShortArray,
        /* 112 */ NewIntArray,
        /* 113 */ NewLongArray,
        /* 114 */ NewFloatArray,
        /* 115 */ NewDoubleArray,
        /* 116 */ GetBooleanArrayElements,
        /* 117 */ GetByteArrayElements,
        /* 118 */ GetCharArrayElements,
        /* 119 */ GetShortArrayElements,
        /* 120 */ GetIntArrayElements,
        /* 121 */ GetLongArrayElements,
        /* 122 */ GetFloatArrayElements,
        /* 123 */ GetDoubleArrayElements,
        /* 124 */ ReleaseBooleanArrayElements,
        /* 125 */ ReleaseByteArrayElements,
        /* 126 */ ReleaseCharArrayElements,
        /* 127 */ ReleaseShortArrayElements,
        /* 128 */ ReleaseIntArrayElements,
        /* 129 */ ReleaseLongArrayElements,
        /* 130 */ ReleaseFloatArrayElements,
        /* 131 */ ReleaseDoubleArrayElements,
        /* 132 */ GetBooleanArrayRegion,
        /* 133 */ GetByteArrayRegion,
        /* 134 */ GetCharArrayRegion,
        /* 135 */ GetShortArrayRegion,
        /* 136 */ GetIntArrayRegion,
        /* 137 */ GetLongArrayRegion,
        /* 138 */ GetFloatArrayRegion,
        /* 139 */ GetDoubleArrayRegion,
        /* 140 */ SetBooleanArrayRegion,
        /* 141 */ SetByteArrayRegion,
        /* 142 */ SetCharArrayRegion,
        /* 143 */ SetShortArrayRegion,
        /* 144 */ SetIntArrayRegion,
        /* 145 */ SetLongArrayRegion,
        /* 146 */ SetFloatArrayRegion,
        /* 147 */ SetDoubleArrayRegion,
        /* 148 */ RegisterNatives,
        /* 149 */ UnregisterNatives,
        /* 150 */ MonitorEnter,
        /* 151 */ MonitorExit,
        /* 152 */ GetStringRegion,
        /* 153 */ GetStringUTFRegion,
        /* 154 */ GetPrimitiveArrayCritical,
        /* 155 */ ReleasePrimitiveArrayCritical,
        /* 156 */ GetStringCritical,
        /* 157 */ ReleaseStringCritical,
        /* 158 */ NewWeakGlobalRef,
        /* 159 */ DeleteWeakGlobalRef,
        /* 160 */ ExceptionCheck,
        /* 161 */ NewDirectByteBuffer,
        /* 162 */ GetDirectBufferAddress,
        /* 163 */ GetDirectBufferCapacity,
        /* 164 */ GetObjectRefType,
        // operation for logging native method down call
        /* 165 */ NativeMethodCall,
        // operation for logging reflective invocation
        /* 166 */ ReflectiveInvocation,
        // operation for logging dynamic linking
        /* 167 */ DynamicLink,
        // operation for logging native method registration
        /* 168 */ RegisterNativeMethod;

    }
// END GENERATED CODE
}
