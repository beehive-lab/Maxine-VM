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
import static com.sun.max.vm.jvmti.JVMTIFieldWatch.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import static com.sun.max.unsafe.UnsafeCast.*;

import java.security.*;

import com.sun.cri.bytecode.*;
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

    /**
     * Holds state associated with this side (i.e. Java) of the implementation.
     */
    static class Env {
        /**
         * The C struct used by the native agents. {@see JVMTIEnvNativeStruct}.
         */
        Pointer env = Pointer.zero();
        /**
         * Object tagging.
         */
        JVMTITags tags = new JVMTITags();
        /**
         * additions to the boot classpath by this agent.
         */
        long[] bootClassPathAdd = new long[4];
        /**
         * A cache of the code event settings by this agent.
         */
        long codeEventMask;
        /**
         *  JVMTI thread local storage.
         */
        JVMTIThreadLocalStorage tls = new JVMTIThreadLocalStorage();
    }

    /**
     * Since the agent initialization code happens very early, before we have a functional heap,
     * we static allocate storage for key data structures. The day that someone tries to runs
     * Maxine with more than {@link MAX_ENVS} agents, we will celebrate -;)
     */
    static final int MAX_ENVS = 8;

    /**
     * The record of registered agent environments, used to handle callbacks.
     * A free slot is denoted by {@link Env#env} having a zero value.
     */
    static final Env[] jvmtiEnvs;
    /**
     * The {@link #jvmtiEnvs} array is updated indirectly by the {@link #SetJVMTIEnv(Pointer)} upcall
     * during agent initialization. Hence we use a static variable to index the array during initialization.
     * N.B. after initialization this value should not be used to limit the search of the array as
     * agents may come and go.
     */
    private static int jvmtiEnvsIndex;

    /**
     * The number of active agent environments.
     */
    private static int activeEnvCount;

    /**
     * The phase of execution.
     */
    static int phase = JVMTI_PHASE_ONLOAD;

    /**
     * We do not permit recursive events, which can happen all too easily in a meta-circular VM.
     */
    private static boolean inEvent;

    static {
        jvmtiEnvs = new Env[MAX_ENVS];
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            Env jvmtiEnv = new Env();
            jvmtiEnvs[i] = jvmtiEnv;
            for (int j = 0; j < jvmtiEnv.bootClassPathAdd.length; j++) {
                jvmtiEnv.bootClassPathAdd [j] = 0;
            }
        }
        new CriticalNativeMethod(JVMTI.class, "currentJniEnv");
    }

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

    static Env getEnv(Pointer env) {
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            if (jvmtiEnvs[i].env == env) {
                return jvmtiEnvs[i];
            }
        }
        return null;
    }

    static synchronized int disposeEnv(Pointer env) {
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
    public static synchronized boolean anyActiveAgents() {
        return activeEnvCount > 0;
    }

    /**
     * Initial entry from VM in {@code PRIMORDIAL} phase.
     * We call Agent_OnLoad for all the agents listed in VM startup command.
     */
    public static void initialize() {
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
        Log.printCString(path);
        Log.println(s2);
        MaxineVM.native_exit(-1);
    }

    /* The standard JNI entry prologue uses the fact that the jni env value is a slot in the
     * thread local storage area in order to reset the safepoint latch register
     * on an upcall, by indexing back to the base of the storage area.
     *
     * A jvmti env value is agent-specific and can be used across threads.
     * Therefore it cannot have a stored jni env value since that is thread-specific.
     * So we load the current value from the native thread control control block.
     * One possible problem: if the TLA has been set to triggered or disabled this will be wrong.
     * I believe this could only happen in the case of a callback from such a state
     * and the callback explicitly passes the jni env as well as the jvmti env.
     * TODO We can't change the agent code, but, if this is an issue, there should
     * be some way to cache the jni env value on the way down and use it on any nested upcalls.
     *
     * TODO handle the (error) case of an upcall from an unattached thread
     */

    @C_FUNCTION(noLatch = true)
    private static native Pointer currentJniEnv();

    @INLINE
    static Pointer prologue(Pointer env, String name) {
        return JniFunctions.prologue(currentJniEnv(), name);
    }

    /**
     * Support for avoiding unnecessary work in the VM.
     * Returns {@code true} iff at least one agent wants to handle this event.
     * @param eventId
     * @return
     */
    public static synchronized boolean eventNeeded(int eventId) {
        if (MaxineVM.isHosted()) {
            return false;
        }
        if (activeEnvCount == 0) {
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
     * Are there any agents requesting any events needing compiled code support?
     * @return
     */
    public static synchronized boolean compiledCodeEventsNeeded() {
        return JVMTIEvent.anyCodeEventsSetGlobally();
    }

    /**
     * Support for determining if we need to compile special code to dispatch
     * specific JVMTI events, e.g. METHOD_ENTRY, FIELD_ACCESS.
     * The value -1 is used to indicate METHOD_ENTRY as this is a pseudo bytecode.
     * @return
     */
    public static synchronized boolean byteCodeEventNeeded(int opcode) {
        int eventId;
        if (opcode == -1) {
            eventId = JVMTI_EVENT_METHOD_ENTRY;
        } else {
            if (opcode == Bytecodes.GETFIELD || opcode == Bytecodes.GETSTATIC) {
                eventId = JVMTI_EVENT_FIELD_ACCESS;
            } else if (opcode == Bytecodes.PUTFIELD || opcode == Bytecodes.PUTSTATIC) {
                eventId = JVMTI_EVENT_FIELD_MODIFICATION;
            } else {
                return false;
            }
        }
        return JVMTIEvent.isEventSetGlobally(eventId);
    }

    /**
     * Gets the (enabled) callback for given event in given environment.
     * @param jvmtiEnv
     * @param eventId
     * @return the callback address or zero if none or not enabled
     */
    static Pointer getCallbackForEvent(Env jvmtiEnv, int eventId) {
        Pointer env = jvmtiEnv.env;
        if (env.isZero()) {
            return env;
        }
        // just global for now
        if (JVMTIEvent.isEventSetGlobally(eventId)) {
            return getCallBack(CALLBACKS.getPtr(env), eventId);
        }
        return Pointer.zero();

    }

    private static boolean ignoreEvent(int eventId) {
        if (MaxineVM.isHosted()) {
            return true;
        }
        if (jvmtiEnvsIndex == 0 || inEvent) {
            return true;
        }

        if ((JVMTIEvent.getPhase(eventId) & phase) == 0) {
            return true;
        }
        return false;
    }

    public static void event(int eventId) {
        event(eventId, null);
    }

    /**
     * Dispatches the event denoted by {@code eventId} to all environments that have registered
     * and enabled a call back for it.
     *
     * @param eventId
     */
    public static void event(int eventId, Object arg1) {
        if (ignoreEvent(eventId)) {
            return;
        }

        try {
            inEvent = true;

            // Regardless of interest in these events there are things that must be done
            switch (eventId) {
                case VM_START:
                    phase = JVMTI_PHASE_START;
                    break;

                case VM_INIT:
                    phase = JVMTI_PHASE_LIVE;
                    tfed = new ThreadFieldEventData();
                    break;

                case THREAD_START:
                    // JVMTI_FIELD_EVENT_DATA.store3(Reference.fromJava(new FieldEventData()));
                    break;

                default:
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

                    case VM_INIT:
                    case THREAD_START:
                    case THREAD_END:
                        invokeStartFunction(callback, env, currentThreadHandle());
                        break;

                    case GARBAGE_COLLECTION_START:
                    case GARBAGE_COLLECTION_FINISH:
                        invokeGarbageCollectionCallback(callback, env);
                        break;

                    case METHOD_ENTRY:
                        invokeThreadObjectCallback(callback, env, currentThreadHandle(), MethodID.fromMethodActor(asClassMethodActor(arg1)));
                        break;

                    case CLASS_LOAD:
                    case CLASS_PREPARE:
                        invokeThreadObjectCallback(callback, env, currentThreadHandle(), JniHandles.createLocalHandle(arg1));
                        break;

                    case FIELD_ACCESS:
                    case FIELD_MODIFICATION:
                        invokeFieldAccessCallback(callback, env, currentThreadHandle(), asFieldEventData(arg1));
                        break;

                }
            }
            if (eventId == VM_DEATH) {
                phase = JVMTI_PHASE_DEAD;
            }
        } finally {
            inEvent = false;
        }
    }

    private static class ThreadFieldEventData extends ThreadLocal<FieldEventData> {
        @Override
        public FieldEventData initialValue() {
            return new FieldEventData();
        }
    }

    private static ThreadFieldEventData tfed;

    @INTRINSIC(UNSAFE_CAST) public static FieldEventData  asFieldEventData(Object object) { return (FieldEventData) object; }

    private static FieldEventData checkGetFieldModificationEvent(int eventType, Object object, int offset, boolean isStatic) {
        if (ignoreEvent(eventType)) {
            return null;
        }
        FieldEventData data = tfed.get();
        data.object = object;
        data.offset = offset;
        data.isStatic = isStatic;
        return data;
    }

    // These event methods are used in T1X templates, so must not be inlined.

    @NEVER_INLINE
    public static void fieldAccessEvent(Object object, int offset, boolean isStatic) {
        // FieldEventData data = asFieldEventData(VmThread.currentTLA().getReference(JVMTI_FIELD_EVENT_DATA.index).toJava());
        FieldEventData data = checkGetFieldModificationEvent(JVMTI_EVENT_FIELD_ACCESS, object, offset, isStatic);
        if (data == null) {
            return;
        }
        data.tag = FieldEventData.DATA_NONE;
        event(JVMTI_EVENT_FIELD_ACCESS, data);
    }

    @NEVER_INLINE
    public static void fieldModificationEvent(Object object, int offset, boolean isStatic, long value) {
        FieldEventData data = checkGetFieldModificationEvent(JVMTI_EVENT_FIELD_MODIFICATION, object, offset, isStatic);
        if (data == null) {
            return;
        }
        data.tag = FieldEventData.DATA_LONG;
        data.longValue = value;
        event(JVMTI_EVENT_FIELD_MODIFICATION, data);
    }

    @NEVER_INLINE
    public static void fieldModificationEvent(Object object, int offset, boolean isStatic, float value) {
        FieldEventData data = checkGetFieldModificationEvent(JVMTI_EVENT_FIELD_MODIFICATION, object, offset, isStatic);
        if (data == null) {
            return;
        }
        data.tag = FieldEventData.DATA_FLOAT;
        data.floatValue = value;
        event(JVMTI_EVENT_FIELD_MODIFICATION, data);
    }

    @NEVER_INLINE
    public static void fieldModificationEvent(Object object, int offset, boolean isStatic, double value) {
        FieldEventData data = checkGetFieldModificationEvent(JVMTI_EVENT_FIELD_MODIFICATION, object, offset, isStatic);
        if (data == null) {
            return;
        }
        data.tag = FieldEventData.DATA_DOUBLE;
        data.doubleValue = value;
        event(JVMTI_EVENT_FIELD_MODIFICATION, data);
    }

    @NEVER_INLINE
    public static void fieldModificationEvent(Object object, int offset, boolean isStatic, Object value) {
        FieldEventData data = checkGetFieldModificationEvent(JVMTI_EVENT_FIELD_MODIFICATION, object, offset, isStatic);
        if (data == null) {
            return;
        }
        data.tag = FieldEventData.DATA_OBJECT;
        data.objectValue = value;
        event(JVMTI_EVENT_FIELD_MODIFICATION, data);
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

    static int getPhase(Pointer phasePtr) {
        int result = phase == JVMTI_PHASE_START ? JVMTI_PHASE_START_ORIG : phase;
        phasePtr.setInt(0, result);
        return JVMTI_ERROR_NONE;
    }

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
