/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.jvmti.JVMTIBreakpoints.EventBreakpointID;
import com.sun.max.vm.jvmti.JVMTIThreadFunctions.FramePopEventData;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * The heart of the Maxine JVMTI implementation.
 * Handles environments, event handling.
 */
public class JVMTI {
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
         * The global event settings for this agent.
         */
        long globalEventSettings;
        /**
         * The per-thread event settings for this agent.
         */
        JVMTIEvent.PerThreadSettings perThreadEventSettings = new JVMTIEvent.PerThreadSettings();
        /**
         *  JVMTI thread local storage.
         */
        JVMTIThreadLocalStorage tls = new JVMTIThreadLocalStorage();
    }

    private static final String AGENT_ONLOAD = "Agent_OnLoad";
    private static final String AGENT_ONUNLOAD = "Agent_OnUnLoad";

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

    static {
        jvmtiEnvs = new Env[MAX_ENVS];
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            Env jvmtiEnv = new Env();
            jvmtiEnvs[i] = jvmtiEnv;
            for (int j = 0; j < jvmtiEnv.bootClassPathAdd.length; j++) {
                jvmtiEnv.bootClassPathAdd[j] = 0;
            }
        }
        VMOptions.addFieldOption("-XX:", "JVMTI_VM", "Include VM classes in JVMTI results.");
    }

    /**
     * Called exactly once during agent startup (strictly speaking during jvmtiEnv creation in jvmti.c)
     * allowing it to be recorded once here to support the handling of callbacks to the agent.
     * @param env the jvmtienv C struct
     */
    public static void setJVMTIEnv(Pointer env) {
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
     * Initial entry from VM at the start of {@code PRISTINE} phase, i.e., before {code
     * VMConfiguration.initializeSchemes(MaxineVM.Phase.PRISTINE)} has been called. We call Agent_OnLoad for all the
     * agents listed in VM startup command.
     */
    public static void initialize() {
        for (int i = 0; i < AgentVMOption.count(); i++) {
            AgentVMOption.Info info = AgentVMOption.getInfo(i);
            Word handle = Word.zero();
            Pointer path = info.libStart;
            if (info.isAbsolute) {
                handle = DynamicLinker.load(path);
            } else {
                handle = JVMTISystem.load(path);
                if (CString.equals(info.libStart, "jdwp")) {
                    if (JVMTIVMOptions.jdwpLogOption.getValue()) {
                        info.optionStart = CString.append(info.optionStart, ",logfile=/tmp/jdwp.log,logflags=255");
                    }
                }
            }

            if (handle.isZero()) {
                initializeFail("failed to load agentlib: ", info.libStart, "");
            }
            Word onLoad = DynamicLinker.lookupSymbol(handle, AGENT_ONLOAD);
            if (onLoad.isZero()) {
                initializeFail("agentlib: ", info.libStart, " does not contain an Agent_OnLoad function");
            }
            int rc = invokeAgentOnLoad(onLoad.asAddress(), info.optionStart);
            if (rc != 0) {
                initializeFail("agentlib: ", info.libStart, " failed to initialize");
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
            Pointer callback = getCallbackForEvent(jvmtiEnvs[i], eventId, VmThread.current());
            if (!callback.isZero()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Are there any agents requesting any events needing compiled code support?
     * @param classMethodActor the method about to be compiled or {@code null} if none.
     * @return
     */
    public static synchronized boolean compiledCodeEventsNeeded(ClassMethodActor classMethodActor) {
        return JVMTIEvent.anyCodeEventsSet() ||
            (classMethodActor == null ? false : JVMTIBreakpoints.hasBreakpoints(classMethodActor));
    }

    /**
     * Support for determining if we need to compile special code to dispatch
     * specific JVMTI events, e.g. METHOD_ENTRY, FIELD_ACCESS.
     * The value -1 is used to indicate METHOD_ENTRY as this is a pseudo bytecode.
     * @return the eventId corresponding to the bytecode or 0 if not needed
     */
    public static synchronized int byteCodeEventNeeded(int opcode) {
        int eventId;
        if (opcode == -1) {
            eventId = JVMTI_EVENT_METHOD_ENTRY;
        } else {
            switch (opcode) {
                case Bytecodes.GETFIELD:
                case Bytecodes.GETSTATIC:
                    eventId = JVMTI_EVENT_FIELD_ACCESS;
                    break;
                case Bytecodes.PUTFIELD:
                case Bytecodes.PUTSTATIC:
                    eventId = JVMTI_EVENT_FIELD_MODIFICATION;
                    break;
                case Bytecodes.IRETURN:
                case Bytecodes.LRETURN:
                case Bytecodes.FRETURN:
                case Bytecodes.DRETURN:
                case Bytecodes.ARETURN:
                case Bytecodes.RETURN:
                    eventId = JVMTI_EVENT_FRAME_POP;
                    break;
                default:
                    return 0;

            }
        }
        return JVMTIEvent.isEventSet(eventId) ? eventId : 0;
    }

    /**
     * Gets the (enabled) callback for given event in given environment.
     * @param jvmtiEnv
     * @param eventId
     * @param vmThread thread generating the event
     * @return the callback address or zero if none or not enabled
     */
    static Pointer getCallbackForEvent(Env jvmtiEnv, int eventId, VmThread vmThread) {
        Pointer env = jvmtiEnv.env;
        if (env.isZero()) {
            return env;
        }
        if (JVMTIEvent.isEventSet(jvmtiEnv, eventId, vmThread)) {
            return getCallBack(CALLBACKS.getPtr(env), eventId);
        }
        return Pointer.zero();

    }

    private static boolean ignoreEvent(int eventId) {
        if (MaxineVM.isHosted()) {
            return true;
        }
        if (jvmtiEnvsIndex == 0) {
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
     * Dispatches the event denoted by {@code eventId} to all environments that have registered and enabled a call back
     * for it.
     *
     * @param eventId
     */
    public static void event(int eventId, Object arg1) {
        boolean ignoring = ignoreEvent(eventId);

        if (ignoring) {
            return;
        }

        JVMTIEvent.logger.logEvent(eventId, arg1);

        // Regardless of interest in these events there are things that must be done
        switch (eventId) {
            case VM_START:
                phase = JVMTI_PHASE_START;
                break;

            case VM_INIT:
                phase = JVMTI_PHASE_LIVE;
                tfed = new ThreadFieldEventData();
                break;

            default:
        }

        // Check that event is enabled and dispatch it to all interested agents
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            Pointer callback = getCallbackForEvent(jvmtiEnvs[i], eventId, VmThread.current());
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

                case BREAKPOINT:
                case SINGLE_STEP:
                    EventBreakpointID id = asEventBreakpointID(arg1);
                    invokeBreakpointCallback(callback, env, currentThreadHandle(), id.methodID, id.location);
                    break;

                case FRAME_POP:
                    FramePopEventData framePopEventData = asFramePopEventData(arg1);
                    invokeFramePopCallback(callback, env, currentThreadHandle(), framePopEventData.methodID,
                                           framePopEventData.wasPoppedByException);
                    break;

            }
        }
        if (eventId == VM_DEATH) {
            phase = JVMTI_PHASE_DEAD;
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
    @INTRINSIC(UNSAFE_CAST) public static FramePopEventData  asFramePopEventData(Object object) { return (FramePopEventData) object; }
    @INTRINSIC(UNSAFE_CAST) public static EventBreakpointID  asEventBreakpointID(Object object) { return (EventBreakpointID) object; }

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
                propValPtr = JVMTISystem.findJavaHome();
                if (propValPtr.isZero()) {
                    Log.println("Environment variable JAVA_HOME not set");
                    MaxineVM.native_exit(-1);
                    return JVMTI_ERROR_NOT_AVAILABLE;
                }
                length = CString.length(propValPtr).toInt();
            } else {
                assert false;
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
        Memory.copyBytes(propValPtr, propValCopyPtr, Size.fromInt(length));
        propValCopyPtr.setByte(length, (byte) 0);
        valuePtr.setWord(propValCopyPtr);
        return JVMTI_ERROR_NONE;
    }

    static int runAgentThread(Pointer env, JniHandle jthread, Address proc, Pointer arg, int priority) {
        /*
         * The JVMTI spec for this says:
         *
         * "The thread group of the thread is ignored -- specifically, the thread is not added to the thread group
         * and the thread is not seen on queries of the thread group at either the Java programming language or JVM TI levels.
         * The thread is not visible to Java programming language queries but is included in JVM TI queries
         * (for example, GetAllThreads and GetAllStackTraces)."
         *
         * Evidently there is no runnable method associated with the thread at this stage, and the default
         * run method just returns. So, knowing the implementation of Thread, we create a runnable and
         * patch the "Runnable target" with an object that will invoke the "proc" native method.
         */
        Thread agentThread = (Thread) jthread.unhand();
        agentThread.setPriority(priority);
        agentThread.setDaemon(true);

        new AgentThreadRunnable(env, agentThread, proc.asPointer(), arg);
        // calling VmThread.start0 instead of Thread.start avoids adding the thread
        // to whatever thread group was set when the constructor was invoked.
        final VmThread vmThread = VmThreadFactory.create(agentThread);
        vmThread.setAsJVMTIAgentThread();
        vmThread.start0();
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

    /**
     * In it's default setting, VM classes are invisible to JVMTI.
     * They don't show up in stack traces, heap traces or the set of loaded classes.
     */
    static boolean JVMTI_VM;

}
