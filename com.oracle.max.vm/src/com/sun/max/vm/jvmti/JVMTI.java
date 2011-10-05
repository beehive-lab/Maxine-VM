/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jvmti;

import static com.sun.max.vm.jvmti.JVMTICallbacks.*;
import static com.sun.max.vm.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.jvmti.JVMTIEvent.*;
import static com.sun.max.vm.jvmti.JVMTIEnvNativeStruct.*;
import static com.sun.max.vm.jvmti.JVMTIVMOptions.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import java.security.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * The heart of the Maxine JVMTI implementation.
 */
public class JVMTI {
    private static final String AGENT_ONLOAD = "Agent_OnLoad";
    private static final String AGENT_ONUNLOAD = "Agent_OnUnLoad";

    static class JVMTIEnv {
        JVMTITags tags = new JVMTITags();
        Pointer env = Pointer.zero();
        long[] bootClassPathAdd = new long[4];
    }

    /**
     * Since the agent initialization code happens very early, before we have a functional heap,
     * we static allocate storage for key data structures. The day that someone tries to runs
     * Maxine with more than {@link MAX_ENVS} agents, we will celebrate -;)
     */
    static final int MAX_ENVS = 8;

    /**
     * The record of registered agent environments, used to handle callbacks.
     */
    static final JVMTIEnv[] jvmtiEnvs;
    /**
     * The {@link #jvmtiEnvs} array is updated indirectly by the {@link #SetJVMTIEnv(Pointer)} upcall
     * during agent initialization. Hence we need a static variable to index the array.
     */
    private static int jvmtiEnvsIndex;

    /**
     * The number of active agent environments.
     */
    static int activeEnvCount;

    /**
     * The phase of execution.
     */
    static int phase = JVMTI_PHASE_ONLOAD;

    static {
        jvmtiEnvs = new JVMTIEnv[MAX_ENVS];
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            JVMTIEnv jvmtiEnv = new JVMTIEnv();
            jvmtiEnvs[i] = jvmtiEnv;
            for (int j = 0; j < jvmtiEnv.bootClassPathAdd.length; j++) {
                jvmtiEnv.bootClassPathAdd [j] = 0;
            }
        }
        new CriticalNativeMethod(JVMTI.class, "jvmtiCurrentJniEnv");
    }

    @C_FUNCTION
    static native void log_print_buffer(Address val);

    @NEVER_INLINE
    static void debug(Object cap) {

    }

    /**
     * Called exactly once during agent startup (strictly speaking during jvmtiEnv creation in jvmti.c)
     * allowing it to be recorded once here to support the handling of callbacks to the agent.
     * @param env the jvmtienv C struct
     */
    static void setJVMTIEnv(Pointer env) {
        if (jvmtiEnvsIndex >= jvmtiEnvs.length) {
            Log.println("too many JVMTI agents");
            MaxineVM.native_exit(-1);
        }
        jvmtiEnvs[jvmtiEnvsIndex++].env = env;
        activeEnvCount++;
    }

    static JVMTIEnv getEnv(Pointer env) {
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            if (jvmtiEnvs[i].env == env) {
                return jvmtiEnvs[i];
            }
        }
        return null;
    }

    static int disposeEnv(Pointer env) {
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            if (jvmtiEnvs[i].env == env) {
                // TODO cleanup
                jvmtiEnvs[i].env = Pointer.zero();
                activeEnvCount--;
                return JVMTI_ERROR_NONE;
            }
        }
        return JVMTI_ERROR_INVALID_ENVIRONMENT;
    }

    /**
     * A guard to avoid any work when there are no active agents.
     * @return
     */
    public static boolean anyActiveAgents() {
        return activeEnvCount > 0;
    }

    public static void initialize() {
        JVMTIRawMonitor.initialize();
        // TODO agentLibOption variant
        for (int i = 0; i < agentPathOption.count(); i++) {
            Pointer path = agentPathOption.getLibStart(i);
            Word handle = DynamicLinker.load(path);
            if (handle.isZero()) {
                initializeFail("failed to load agentlib: ", path, "");
            }
            Word onLoad = DynamicLinker.lookupSymbol(handle, AGENT_ONLOAD);
            if (onLoad.isZero()) {
                initializeFail("agentlib: ", path, " does not contain an Agent_OnLoad function");
            }
            int rc = invokeAgentOnLoad(onLoad.asAddress(), agentPathOption.getOptionStart(i));
            if (rc != 0) {
                initializeFail("agentlib: ", path, " failed to initialize");
            }
        }
        phase = JVMTI_PHASE_PRIMORDIAL;
    }

    private static void initializeFail(String s1, Pointer path, String s2) {
        Log.print(s1);
        log_print_buffer(path.asAddress());
        Log.println(s2);
        MaxineVM.native_exit(-1);
    }

    /* The standard prologue uses the fact that the jni env value is a slot in the
     * thread local storage area in order to reset the safepoint latch register
     * on an upcall, by indexing back to the base of the storage area.
     *
     * A jvmti env value is agent-specific and can be used across threads.
     * Therefore it cannot have a stored jni env value since that is thread-specific.
     * So we load the current value from the native thread control control block.
     * One problem: if the TLA has been set to triggered or disabled this will be wrong.
     * I believe this could only happen in the case of a callback from such a state
     * and the callback explicitly passes the jni env as well as the jvmti env.
     * TODO We can't change the agent code, but, if this is an issue, there should
     * be some way to cache the jni env value on the way down and use it on any nested upcalls.
     *
     * TODO handle the (error) case of an upcall from an unattached thread
     */

    @C_FUNCTION(noLatch = true)
    private static native Pointer jvmtiCurrentJniEnv();

    @INLINE
    static Pointer prologue(Pointer env, String name) {
        return JniFunctions.prologue(jvmtiCurrentJniEnv(), name);
    }

    /**
     * Support for avoiding unnecessary work in the VM.
     * Returns {@code true} iff at least one agent wants to handle this event.
     * @param eventId
     * @return
     */
    public static boolean eventNeeded(int eventId) {
        if (MaxineVM.isHosted()) {
            return false;
        }
        if (jvmtiEnvsIndex == 0) {
            return false;
        }
        if (phase == JVMTI_PHASE_DEAD) {
            return false;
        }
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            Pointer callback = getCallbackForEvent(jvmtiEnvs[i], eventId);
            if (!callback.isZero()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the (enabled) callback for given event in given environment.
     * @param jvmtiEnv
     * @param eventId
     * @return the callback address or zero if none or not enabled
     */
    static Pointer getCallbackForEvent(JVMTIEnv jvmtiEnv, int eventId) {
        Pointer env = jvmtiEnv.env;
        if (env.isZero()) {
            return env;
        }
        // just global for now
        long envMask = EVENTMASK.get(env).asAddress().toLong();
        long maskBit = getEventBitMask(eventId);
        if ((envMask & maskBit) != 0) {
            return getCallBack(CALLBACKS.getPtr(env), eventId);
        }
        return Pointer.zero();

    }

    public static void event(int eventId) {
        event(eventId, null);
    }

    /**
     * Dispatches the event denoted by {@code eventId} to all environments that have registered and enabled call back
     * for it.
     *
     * @param eventId
     */
    public static void event(int eventId, Object arg1) {
        if (MaxineVM.isHosted()) {
            return;
        }
        if (jvmtiEnvsIndex == 0) {
            return;
        }
        if (phase == JVMTI_PHASE_DEAD) {
            // shouldn't happen but just in case, spec says no delivery.
            return;
        }

        if (eventId == VM_START) {
            phase = JVMTI_PHASE_START;
        } else if (eventId == VM_INIT) {
            phase = JVMTI_PHASE_LIVE;
        }

        // Check that event is enabled and dispatch it to all registered agents
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            Pointer callback = getCallbackForEvent(jvmtiEnvs[i], eventId);
            if (callback.isZero()) {
                continue;
            }
            Pointer env = jvmtiEnvs[i].env;
            switch (eventId) {
                case VM_START:
                case VM_DEATH:
                    invokeStartFunctionNoArg(callback, env);
                    break;
                case THREAD_START:
                case THREAD_END:
                case VM_INIT:
                    invokeStartFunction(callback, env, currentThreadHandle());
                    break;

                case GARBAGE_COLLECTION_START:
                case GARBAGE_COLLECTION_FINISH:
                    invokeGarbageCollectionCallback(callback, env);
                    break;

                case CLASS_LOAD:
                case CLASS_PREPARE:
                    invokeThreadObjectCallback(callback, env, currentThreadHandle(), JniHandles.createLocalHandle(arg1));
                    break;

            }
        }
        if (eventId == VM_DEATH) {
            phase = JVMTI_PHASE_DEAD;
        }
    }

    public static byte[] classFileLoadHook(ClassLoader classLoader, String className, ProtectionDomain protectionDomain,
                    byte[] classfileBytes) {
        return JVMTIClassFunctions.classFileLoadHook(classLoader, className, protectionDomain, classfileBytes);
    }

    public static String getAddedBootClassPath() {
        return JVMTIClassFunctions.getAddedBootClassPath();
    }

    private static JniHandle currentThreadHandle() {
        return JniHandles.createLocalHandle(VmThread.current().javaThread());
    }

    private static Pointer getCallBack(Pointer callbacks, int eventId) {
        int index = eventId - JVMTI_MIN_EVENT_TYPE_VAL;
        return callbacks.readWord(index * Pointer.size()).asPointer();
    }

    private static final String JAVA_HOME = "java.home";
    private static final byte[] JAVA_HOME_BYTES = "JAVA_HOME".getBytes();

    static int getSystemProperty(Pointer env, Pointer property, Pointer valuePtr) {
        int length = 0;
        Pointer propValPtr = Pointer.zero();
        if (MaxineVM.isPristine()) {
            // If we are in the PRISTINE phase the majority of the system properties are not available
            // in any easy way, as they are not setup until the STARTING phase. So we have to hand craft
            // the implementation for a specific set of properties. Yuk.
            if (CString.equals(property, JAVA_HOME)) {
                propValPtr = JVMTIEnvVar.getValue(Reference.fromJava(JAVA_HOME_BYTES).toOrigin().plus(JVMTIUtil.byteDataOffset));
                if (propValPtr.isZero()) {
                    Log.println("Environment variable JAVA_HOME not set");
                    MaxineVM.native_exit(-1);
                    return JVMTI_ERROR_NOT_AVAILABLE;
                }
                length = CString.length(propValPtr).toInt();
            }
        } else {
            try {
                String keyString = CString.utf8ToJava(property);
                String propVal = System.getProperty(keyString);
                length = propVal.length();
                byte[] propValBytes = propVal.getBytes();
                propValPtr = Reference.fromJava(propValBytes).toOrigin().plus(JVMTIUtil.byteDataOffset);
            } catch (Utf8Exception ex) {
                return JVMTI_ERROR_NOT_AVAILABLE;
            }
        }
        Pointer propValCopyPtr = Memory.allocate(Size.fromInt(length + 1));
        if (propValCopyPtr.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        Memory.copyBytes(propValPtr, propValCopyPtr, Size.fromInt(length + 1));
        valuePtr.setWord(propValCopyPtr);
        return JVMTI_ERROR_NONE;
    }

    static int runAgentThread(Pointer env, JniHandle jthread, Address proc, Pointer arg, int priority) {
        /* TODO: Fully implement the specification:
         * The JVMTI spec for this says:
         *
         * "The thread group of the thread is ignored -- specifically, the thread is not added to the thread group
         * and the thread is not seen on queries of the thread group at either the Java programming language or JVM TI levels.
         * The thread is not visible to Java programming language queries but is included in JVM TI queries
         * (for example, GetAllThreads and GetAllStackTraces)."
         *
         * However, the agent has created the Thread instance and, at least based on the gctest demo,
         * invoked the <init> method via JNI, which has already done things like adding to a ThreadGroup.
         * It's not clear how to undo these actions so for now we just let it go.
         *
         * Evidently there is no runnable method associated with the thread at this stage, and the default
         * run method just returns. So, knowing the implementation of Thread, we create a runnable and
         * patch the "Runnable target" with an object that will invoke the "proc" native method.
         */
        Thread agentThread = (Thread) jthread.unhand();
        agentThread.setPriority(priority);
        agentThread.setDaemon(true);

        new AgentThreadRunnable(env, agentThread, proc.asPointer(), arg);
        agentThread.start();
        return JVMTI_ERROR_NONE;
    }

    /**
     * Provides the {@link Runnable} to plug into the the agent thread's "target" field so that when
     * it is started it will invoke the {@code run} method defined here, which invokes the native
     * agent thread method.
     */
    static class AgentThreadRunnable implements Runnable {
        // pseudo field to access the Thread.target field
        @ALIAS(declaringClass = Thread.class)
        Runnable target;

        @INTRINSIC(UNSAFE_CAST)
        static native AgentThreadRunnable asAgentThreadRunnable(Object object);

        Pointer jvmtiEnv;
        Pointer nativeProc;
        Pointer arg;

        AgentThreadRunnable(Pointer env, Thread agentThread, Pointer proc, Pointer arg) {
            jvmtiEnv = env;
            nativeProc = proc;
            this.arg = arg;
            AgentThreadRunnable proxy = asAgentThreadRunnable(agentThread);
            proxy.target = this;
        }

        public void run() {
            invokeStartFunction(nativeProc, jvmtiEnv, arg);
        }

    }

}
