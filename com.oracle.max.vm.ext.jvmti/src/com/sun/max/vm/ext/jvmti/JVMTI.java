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
package com.sun.max.vm.ext.jvmti;

import static com.sun.max.vm.ext.jvmti.JVMTICallbacks.*;
import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.ext.jvmti.JVMTIEnvNativeStruct.*;
import static com.sun.max.vm.ext.jvmti.JVMTIEvents.*;
import static com.sun.max.vm.ext.jvmti.JVMTIFieldWatch.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import static com.sun.max.unsafe.UnsafeCast.*;

import java.security.*;
import java.util.*;

import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.jvmti.*;
import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.ext.jvmti.JVMTIUtil.ModeUnion;
import com.sun.max.vm.ext.jvmti.JVMTIBreakpoints.*;
import com.sun.max.vm.ext.jvmti.JVMTIException.*;
import com.sun.max.vm.ext.jvmti.JVMTIThreadFunctions.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.ti.*;

/**
 * The heart of the Maxine JVMTI implementation.
 * Handles environments, event handling.
 * Supports both standard (native) agents and the Java agents written to {@link JJVMTI}.
 */
public class JVMTI {
    /**
     * Holds state associated with this side (i.e. Java) of the implementation that is independent
     * of whether this is a native or a Java agent.
     */
    static abstract class Env {
        /**
         * Object tagging.
         */
        JVMTITags tags = new JVMTITags();
        /**
         * The global event settings for this agent.
         */
        long globalEventSettings;
        /**
         * The per-thread event settings for this agent.
         */
        JVMTIEvents.PerThreadSettings perThreadEventSettings = new JVMTIEvents.PerThreadSettings();

        boolean isFree() {
            return false;
        }

    }

    /**
     * Subclass used for traditional native agents.
     */
    static class NativeEnv extends Env {
        /**
         * The C struct used by the native agents. {@see JVMTIEnvNativeStruct}.
         */
        Pointer cstruct;

        /**
         * Support for {@code Get/SetEnvironmentLocalStorage}.
         */
        Pointer envStorage;

        /**
         * additions to the boot classpath by this agent.
         */
        long[] bootClassPathAdd = new long[4];

        @Override
        boolean isFree() {
            return cstruct.isZero();
        }

    }

    /**
     * Abstract subclass used for agents that use {@link JJVMTI}.
     */
    public static class JavaEnv extends Env {
        final EnumSet<JVMTICapabilities.E> capabilities = EnumSet.noneOf(JVMTICapabilities.E.class);
        final JJVMTI.EventCallbacks callbackHandler;
        protected JavaEnv(JJVMTI.EventCallbacks callbackHandler) {
            this.callbackHandler = callbackHandler;
        }

    }

    static class JVMTIHandler extends NullVMTIHandler implements VMTIHandler {

        @Override
        public void initialize() {
            JVMTI.initialize();
        }

        @Override
        public void vmInitialized() {
            /*
             * JVMTI has a VM_START and a VM_INIT event, but the distinction
             * is minimal and only relates to which JVMTI functions may be called.
             * The gating function for dispatching either event this late
             * is that any JNI function can be called from VM_START, e.g, FindClass,
             * which requires that essentially all of the VM machinery is working.
             */
            JVMTI.event(E.VM_START);
            JVMTI.event(E.VM_INIT);
        }

        @Override
        public void vmDeath() {
            JVMTI.event(E.VM_DEATH);
        }

        @Override
        public void threadStart(VmThread vmThread) {
            JVMTI.event(E.THREAD_START, vmThread);
        }

        @Override
        public void threadEnd(VmThread vmThread) {
            JVMTI.event(E.THREAD_END, vmThread);
        }

        @Override
        public boolean classFileLoadHookHandled() {
            return JVMTI.eventNeeded(E.CLASS_FILE_LOAD_HOOK);
        }

        @Override
        public byte[] classFileLoadHook(ClassLoader classLoader, String className, ProtectionDomain protectionDomain, byte[] classfileBytes) {
            return JVMTI.classFileLoadHook(classLoader, className, protectionDomain, classfileBytes);
        }

        @Override
        public void classLoad(ClassActor classActor) {
            // Have to send both events
            JVMTI.event(E.CLASS_LOAD, classActor);
            JVMTI.event(E.CLASS_PREPARE, classActor);
        }

        @Override
        public void methodCompiled(ClassMethodActor classMethodActor) {
            JVMTI.event(E.COMPILED_METHOD_LOAD, classMethodActor);
        }

        @Override
        public void methodUnloaded(ClassMethodActor classMethodActor, Pointer codeAddr) {
            MethodUnloadEventData methodUnloadEventData = threadMethodUnloadEventData.get();
            methodUnloadEventData.classMethodActor = classMethodActor;
            methodUnloadEventData.codeAddr = codeAddr;
            JVMTI.event(E.COMPILED_METHOD_UNLOAD, methodUnloadEventData);
        }

        @Override
        public boolean hasBreakpoints(ClassMethodActor classMethodActor) {
            return JVMTIBreakpoints.hasBreakpoints(classMethodActor);
        }

        @Override
        public String bootclassPathExtension() {
            return JVMTIClassFunctions.getAddedBootClassPath();
        }

        @Override
        public void beginUpcallVM() {
            JVMTIVmThreadLocal.setBit(VmThread.currentTLA(), JVMTIVmThreadLocal.IN_UPCALL);
        }

        @Override
        public void endUpcallVM() {
            JVMTIVmThreadLocal.unsetBit(VmThread.currentTLA(), JVMTIVmThreadLocal.IN_UPCALL);
        }

        @Override
        public void beginGC() {
            JVMTI.event(E.GARBAGE_COLLECTION_START);
        }

        @Override
        public void endGC() {
            JVMTI.event(E.GARBAGE_COLLECTION_FINISH);
        }

        @Override
        public boolean nativeCallNeedsPrologueAndEpilogue(MethodActor ma) {
            return ma != JVMTIFunctions.currentJniEnv;
        }

        @Override
        public void registerAgent(Word agentHandle) {
            JVMTI.setJVMTIEnv(agentHandle);
        }

        @Override
        public int activeAgents() {
            return JVMTI.activeEnvCount;
        }

        @Override
        public void raise(Throwable throwable, Pointer sp, Pointer fp, CodePointer ip) {
            JVMTIException.raiseEvent(throwable, sp, fp, ip);
        }

        @Override
        public RuntimeCompiler runtimeCompiler(RuntimeCompiler stdRuntimeCompiler) {
            return new T1X(JVMTI_T1XTemplateSource.class, new JVMTI_T1XCompilationFactory());
        }

        @Override
        public boolean needsVMTICompilation(ClassMethodActor classMethodActor) {
            // N.B. We do not instrument reflection stubs. Amongst other reasons they
            // can be generated by upcalls from JVMTI agents using JNI, e.g. Class.getName,
            // causing runaway recursion.
            ClassActor holder = classMethodActor.holder();
            return !holder.isReflectionStub() && JVMTI.compiledCodeEventsNeeded(classMethodActor);
        }

        @Override
        public boolean needsSpecialGetCallerClass() {
            return true;
        }

        @Override
        @NEVER_INLINE
        public Class getCallerClassForFindClass(int realFramesToSkip) {
            return JVMTIClassLoader.getCallerClassForFindClass(realFramesToSkip++);
        }

    }

    static {
        VMTI.registerEventHandler(new JVMTIHandler());
    }

    private static final String AGENT_ONLOAD = "Agent_OnLoad";
    private static final String AGENT_ONUNLOAD = "Agent_OnUnLoad";

    /**
     * Since the agent initialization code happens very early, before we have a functional heap,
     * we static allocate storage for key data structures. The day that someone tries to runs
     * Maxine with more than {@link MAX_ENVS} agents, we will celebrate -;)
     */
    static final int MAX_ENVS = 8;
    static final int MAX_NATIVE_ENVS = 6;

    /**
     * The record of registered agent environments, used to handle callbacks.
     * A free slot is denoted by {@link Env#cstruct} having a zero value.
     */
    static final Env[] jvmtiEnvs;
    /**
     * The {@link #jvmtiEnvs} array is updated indirectly by the {@link #SetJVMTIEnv(Pointer)} upcall
     * during agent initialization. Hence we use a static variable to index the array during initialization.
     * N.B. after initialization this value should not be used to limit the search of the array as
     * agents may come and go.
     */
    private static int nativeEnvsIndex;

    /**
     * The number of active agent environments.
     */
    static int activeEnvCount;

    /**
     * The phase of execution.
     */
    static int phase = JVMTI_PHASE_ONLOAD;

    static {
        jvmtiEnvs = new Env[MAX_ENVS];
        for (int i = 0; i < MAX_NATIVE_ENVS; i++) {
            NativeEnv jvmtiEnv = new NativeEnv();
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
     * @param agentHandle the jvmtienv C struct
     */
    public static void setJVMTIEnv(Word agentHandle) {
        if (nativeEnvsIndex >= MAX_NATIVE_ENVS) {
            Log.println("too many JVMTI agents");
            MaxineVM.native_exit(1);
        }
        NativeEnv nativeEnv = (NativeEnv) jvmtiEnvs[nativeEnvsIndex++];
        nativeEnv.cstruct = agentHandle.asPointer();
        activeEnvCount++;
    }

    /**
     * Called to register the environment for a Java JVMTI agent.
     * @param env
     */
    public static synchronized void setJVMTIJavaEnv(Env env) {
        for (int i = MAX_NATIVE_ENVS; i < MAX_ENVS; i++) {
            if (jvmtiEnvs[i] == null) {
                jvmtiEnvs[i] = env;
                activeEnvCount++;
                return;
            }
        }
        Log.println("too many JVMTI agents");
        MaxineVM.native_exit(1);
    }

    public static synchronized int disposeJVMTIJavaEnv(Env env) {
        for (int i = MAX_NATIVE_ENVS; i < MAX_ENVS; i++) {
            if (jvmtiEnvs[i] == env) {
                jvmtiEnvs[i] = null;
                activeEnvCount--;
                return JVMTI_ERROR_NONE;
            }
        }
        return JVMTI_ERROR_INVALID_ENVIRONMENT;
    }

    static Env getEnv(Pointer env) {
        for (int i = 0; i < MAX_NATIVE_ENVS; i++) {
            NativeEnv nativeEnv = (NativeEnv) jvmtiEnvs[i];
            if (nativeEnv.cstruct == env) {
                return jvmtiEnvs[i];
            }
        }
        return null;
    }

    static synchronized int disposeEnv(Pointer env) {
        for (int i = 0; i < MAX_NATIVE_ENVS; i++) {
            NativeEnv nativeEnv = (NativeEnv) jvmtiEnvs[i];
            if (nativeEnv.cstruct == env) {
                // TODO cleanup
                nativeEnv.cstruct = Pointer.zero();
                activeEnvCount--;
                return JVMTI_ERROR_NONE;
            }
        }
        return JVMTI_ERROR_INVALID_ENVIRONMENT;
    }

    /**
     * Initial entry from VM at the start of {@code PRISTINE} phase, i.e., before {code
     * VMConfiguration.initializeSchemes(MaxineVM.Phase.PRISTINE)} has been called. We call Agent_OnLoad for all the
     * agents listed in VM startup command.
     */
    public static void initialize() {
        NativeInterfaces.initFunctionTable(getJVMTIInterface(-1), JVMTIFunctions.jvmtiFunctions, JVMTIFunctions.jvmtiFunctionActors);
        JVMTISystem.initSystemProperties();

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

        // Invoke onBoot for any Java agents built into the image
        for (int i = MAX_NATIVE_ENVS; i < MAX_ENVS; i++) {
            JavaEnv javaEnv = (JavaEnv) jvmtiEnvs[i];
            if (javaEnv != null) {
                javaEnv.callbackHandler.onBoot();
            }
        }
        phase = JVMTI_PHASE_PRIMORDIAL;
    }

    static {
        new CriticalNativeMethod(JVMTI.class, "getJVMTIInterface");
    }

    @C_FUNCTION
    private static native Pointer getJVMTIInterface(int version);

    private static void initializeFail(String s1, Pointer path, String s2) {
        Log.print(s1);
        Log.printCString(path);
        Log.println(s2);
        MaxineVM.native_exit(1);
    }

   /**
     * Support for avoiding unnecessary work in the VM.
     * Returns {@code true} iff at least one agent wants to handle this event.
     * @param eventId
     */
    public static synchronized boolean eventNeeded(JVMTIEvents.E event) {
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
            if (hasCallbackForEvent(jvmtiEnvs[i], event, VmThread.current())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Are there any agents requesting any events needing compiled code support?
     * @param classMethodActor the method about to be compiled or {@code null} if none.
     */
    public static synchronized boolean compiledCodeEventsNeeded(ClassMethodActor classMethodActor) {
        return JVMTIEvents.anyCodeEventsSet() ||
            (classMethodActor == null ? false : JVMTIBreakpoints.hasBreakpoints(classMethodActor));
    }

    /**
     * Returns the {@link JVMTIEvents.E event} that corresponds to a given bytecode or null if none.
     * should it be instrumented.
     * @param opcode
     */
    public static JVMTIEvents.E eventForBytecode(int opcode) {
        if (opcode == -1) {
            return E.METHOD_ENTRY;
        } else if (opcode == -2) {
            return E.METHOD_EXIT;
        } else {
            switch (opcode) {
                case Bytecodes.GETFIELD:
                case Bytecodes.GETSTATIC:
                    return E.FIELD_ACCESS;
                case Bytecodes.PUTFIELD:
                case Bytecodes.PUTSTATIC:
                    return E.FIELD_MODIFICATION;
                case Bytecodes.IRETURN:
                case Bytecodes.LRETURN:
                case Bytecodes.FRETURN:
                case Bytecodes.DRETURN:
                case Bytecodes.ARETURN:
                case Bytecodes.RETURN:
                    return E.FRAME_POP;
                default:
                    return null;

            }
        }
    }

    /**
     * Support for determining if we need to compile special code to dispatch
     * specific JVMTI events, e.g. METHOD_ENTRY, FIELD_ACCESS.
     * The value -1 is used to indicate METHOD_ENTRY as this is a pseudo bytecode.
     * @return the {@link JVMTIEvents.E event} corresponding to the bytecode or null if no associated
     *              event or not set for an agent
     */
    public static synchronized E byteCodeEventNeeded(int opcode) {
        E event =  eventForBytecode(opcode);
        if (event != null) {
            if (!JVMTIEvents.isEventSet(event)) {
                event = null;
            }
        }
        return event;
    }

    /**
     * Gets the (enabled) callback for given event in given environment.
     * @param jvmtiEnv
     * @param eventId
     * @param vmThread thread generating the event
     * @return the callback address or zero if none or not enabled
     */
    static Pointer getCallbackForEvent(NativeEnv jvmtiEnv, JVMTIEvents.E event, VmThread vmThread) {
        if (jvmtiEnv.isFree()) {
            return Pointer.zero();
        }
        if (JVMTIEvents.isEventSet(jvmtiEnv, event, vmThread)) {
            return getCallBack(CALLBACKS.getPtr(jvmtiEnv.cstruct), event);
        }
        return Pointer.zero();

    }

    static boolean hasCallbackForEvent(Env jvmtiEnv, JVMTIEvents.E event, VmThread vmThread) {
        if (jvmtiEnv == null || jvmtiEnv.isFree()) {
            return false;
        }
        return JVMTIEvents.isEventSet(jvmtiEnv, event, vmThread);
    }

    /**
     * Invoked from T1X templates, so no inline.
     */
    @NEVER_INLINE
    public static void methodEntryEvent(MethodActor methodActor) {
        event(E.METHOD_ENTRY, methodActor);
    }

    /**
     * Invoked from T1X templates, so no inline.
     */
    @NEVER_INLINE
    public static void exceptionCatchEvent(Object exception) {
        event(E.EXCEPTION_CATCH, JVMTIException.getExceptionEventData());
    }

    private static void logEvent(JVMTIEvents.E event, JVMTI.Env env, int status, Object arg1) {
        if (JVMTIEvents.logger.enabled()) {
            JVMTIEvents.logger.logEvent(event, status, env, arg1);
        }
    }

    public static void event(JVMTIEvents.E event) {
        event(event, null);
    }

    /**
     * Dispatches the event denoted by {@code eventId} to all environments that have registered and enabled a call back
     * for it.
     *
     * @param eventId
     */
    public static void event(JVMTIEvents.E event, Object arg1) {
        if (MaxineVM.isHosted()) {
            return;
        }
        if (phase == JVMTI_PHASE_LIVE && activeEnvCount == 0) {
            logEvent(event, null, JVMTIEventLogger.NO_INTEREST, arg1);
            return;
        }

        // Regardless of interest in these events we must track the phase.
        switch (event) {
            case VM_START:
                phase = JVMTI_PHASE_START;
                break;

            case VM_INIT:
                phase = JVMTI_PHASE_LIVE;
                break;

            default:
        }

        if ((JVMTIEvents.getPhases(event) & phase) == 0) {
            // VM has sent an event that is not supposed to be delivered in the current phase
            logEvent(event, null, JVMTIEventLogger.WRONG_PHASE, arg1);
        } else {
            // fast check if anyone is interested
            boolean interest = JVMTIEvents.isEventSet(event);
            if (interest) {
                interest = dispatchEvent(event, arg1); // did anyone actually get it?
            }
            if (!interest) {
                logEvent(event, null, JVMTIEventLogger.NO_INTEREST, arg1);
            }

        }


        if (event == E.VM_DEATH) {
            // Now the event has (possibly) been delivered change the phase.
            // This will suppress the deliver of any future events as the phase check above will not match.
            phase = JVMTI_PHASE_DEAD;
        }
    }

    private static boolean dispatchEvent(JVMTIEvents.E event, Object arg1) {
        // Dispatch event to all interested agents
        boolean interest = false;
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            if (i < MAX_NATIVE_ENVS) {
                NativeEnv nativeEnv = (NativeEnv) jvmtiEnvs[i];
                Pointer callback = getCallbackForEvent(nativeEnv, event, VmThread.current());
                if (callback.isZero()) {
                    continue;
                }
                interest = true;
                logEvent(event, nativeEnv, JVMTIEventLogger.DELIVERED, arg1);
                Pointer cstruct = nativeEnv.cstruct;
                switch (event) {
                    case VM_START:
                    case VM_DEATH:
                        invokeStartFunctionNoArg(callback, cstruct);
                        break;

                    case VM_INIT:
                    case THREAD_START:
                    case THREAD_END:
                        invokeStartFunction(callback, cstruct, currentThreadHandle());
                        break;

                    case GARBAGE_COLLECTION_START:
                    case GARBAGE_COLLECTION_FINISH:
                        invokeGarbageCollectionCallback(callback, cstruct);
                        break;

                    case METHOD_ENTRY:
                        invokeThreadObjectCallback(callback, cstruct, currentThreadHandle(), MethodID.fromMethodActor(asClassMethodActor(arg1)));
                        break;

                    case CLASS_LOAD:
                    case CLASS_PREPARE:
                        invokeThreadObjectCallback(callback, cstruct, currentThreadHandle(), JniHandles.createLocalHandle(asClassActor(arg1).javaClass()));
                        break;

                    case COMPILED_METHOD_LOAD: {
                        ClassMethodActor cma = asClassMethodActor(arg1);
                        TargetMethod tm = cma.currentTargetMethod();
                        invokeCompiledMethodLoadCallback(callback, cstruct, MethodID.fromMethodActor(cma),
                                        tm.codeLength(), tm.start(), 0, Pointer.zero(), Pointer.zero());
                        break;
                    }

                    case COMPILED_METHOD_UNLOAD: {
                        MethodUnloadEventData methodUnloadEventData = asMethodUnloadEventData(arg1);
                        invokeCompiledMethodUnloadCallback(callback, cstruct,
                                        MethodID.fromMethodActor(methodUnloadEventData.classMethodActor),
                                        methodUnloadEventData.codeAddr.asAddress());
                        break;
                    }

                    case FIELD_ACCESS:
                    case FIELD_MODIFICATION:
                        invokeFieldAccessCallback(callback, cstruct, currentThreadHandle(), asFieldEventData(arg1));
                        break;

                    case BREAKPOINT:
                    case SINGLE_STEP:
                        EventBreakpointID id = asEventBreakpointID(arg1);
                        invokeBreakpointCallback(callback, cstruct, currentThreadHandle(), id.methodID, id.location);
                        break;

                    case FRAME_POP:
                        FramePopEventData framePopEventData = asFramePopEventData(arg1);
                        invokeFramePopCallback(callback, cstruct, currentThreadHandle(), framePopEventData.methodID, framePopEventData.wasPoppedByException);
                        break;

                    case EXCEPTION:
                    case EXCEPTION_CATCH:
                        ExceptionEventData exceptionEventData = asExceptionEventData(arg1);
                        invokeExceptionCallback(callback, cstruct, event == E.EXCEPTION_CATCH, currentThreadHandle(), exceptionEventData.methodID, exceptionEventData.location,
                                        JniHandles.createLocalHandle(exceptionEventData.throwable), exceptionEventData.catchMethodID, exceptionEventData.catchLocation);
                        break;
                }
            } else {
                JavaEnv javaEnv = (JavaEnv) jvmtiEnvs[i];
                if (javaEnv == null || !JVMTIEvents.isEventSet(javaEnv, event, VmThread.current())) {
                    continue;
                }
                interest = true;
                logEvent(event, javaEnv, JVMTIEventLogger.DELIVERED, arg1);
                Thread currentThread = Thread.currentThread();
                switch (event) {
                    case VM_INIT:
                        javaEnv.callbackHandler.vmInit();
                        break;

                    case VM_DEATH:
                        javaEnv.callbackHandler.vmDeath();
                        break;

                    case THREAD_START:
                        javaEnv.callbackHandler.threadStart(currentThread);
                        break;

                    case THREAD_END:
                        javaEnv.callbackHandler.threadEnd(currentThread);
                        break;

                    case GARBAGE_COLLECTION_START:
                        javaEnv.callbackHandler.garbageCollectionStart();
                        break;

                    case GARBAGE_COLLECTION_FINISH:
                        javaEnv.callbackHandler.garbageCollectionFinish();
                        break;

                    case CLASS_LOAD:
                        javaEnv.callbackHandler.classLoad(currentThread, asClassActor(arg1));
                        break;

                    case COMPILED_METHOD_LOAD: {
                        ClassMethodActor cma = asClassMethodActor(arg1);
                        TargetMethod tm = cma.currentTargetMethod();
                        javaEnv.callbackHandler.compiledMethodLoad(cma, tm.codeLength(), tm.start(), null, null);
                        break;
                    }

                    case COMPILED_METHOD_UNLOAD: {
                        MethodUnloadEventData methodUnloadEventData = asMethodUnloadEventData(arg1);
                        javaEnv.callbackHandler.compiledMethodUnload(methodUnloadEventData.classMethodActor, methodUnloadEventData.codeAddr);
                        break;
                    }

                    case METHOD_ENTRY: {
                        javaEnv.callbackHandler.methodEntry(currentThread, asClassMethodActor(arg1));
                        break;
                    }

                    case METHOD_EXIT: {
                        FramePopEventData framePopEventData = asFramePopEventData(arg1);
                        javaEnv.callbackHandler.methodExit(currentThread, MethodID.toMethodActor(framePopEventData.methodID),
                                            framePopEventData.wasPoppedByException, framePopEventData.value);
                        break;
                    }

                    case FIELD_ACCESS:
                    case FIELD_MODIFICATION: {
                        invokeFieldAccessCallback(javaEnv.callbackHandler, currentThread, asFieldEventData(arg1));
                        break;
                    }

                    case SINGLE_STEP:
                    case BREAKPOINT:
                        EventBreakpointID id = asEventBreakpointID(arg1);
                        MethodID methodId = MethodID.fromWord(Address.fromLong(id.methodID));
                        javaEnv.callbackHandler.breakpoint(currentThread, MethodID.toMethodActor(methodId), id.location);
                        break;

                    case FRAME_POP:
                        FramePopEventData framePopEventData = asFramePopEventData(arg1);
                        javaEnv.callbackHandler.framePop(currentThread, MethodID.toMethodActor(framePopEventData.methodID), framePopEventData.wasPoppedByException);
                        break;

                    case EXCEPTION: {
                        ExceptionEventData exceptionEventData = asExceptionEventData(arg1);
                        MethodActor catchMethod = exceptionEventData.catchMethodID.isZero() ? null : MethodID.toMethodActor(exceptionEventData.catchMethodID);
                        javaEnv.callbackHandler.exception(currentThread,
                                        MethodID.toMethodActor(exceptionEventData.methodID), exceptionEventData.location,
                                        exceptionEventData.throwable,
                                        catchMethod, exceptionEventData.catchLocation);
                        break;
                    }

                    case EXCEPTION_CATCH: {
                        ExceptionEventData exceptionEventData = asExceptionEventData(arg1);
                        javaEnv.callbackHandler.exceptionCatch(currentThread,
                                        MethodID.toMethodActor(exceptionEventData.methodID), exceptionEventData.location,
                                        exceptionEventData.throwable);
                        break;
                    }

                    default:
                        assert false;
                }
            }
        }

        return interest; // at least one agent was (really) interested.
    }

    private static class ThreadFieldEventData extends ThreadLocal<FieldEventData> {
        @Override
        public FieldEventData initialValue() {
            return new FieldEventData();
        }
    }

    private static class MethodUnloadEventData  {
        ClassMethodActor classMethodActor;
        Pointer codeAddr;
    }

    private static class ThreadMethodUnloadEventData extends ThreadLocal<MethodUnloadEventData> {
        @Override
        public MethodUnloadEventData initialValue() {
            return new MethodUnloadEventData();
        }
    }

    private static final ThreadFieldEventData threadFieldEventData = new ThreadFieldEventData();
    private static final ThreadMethodUnloadEventData threadMethodUnloadEventData = new ThreadMethodUnloadEventData();

    @INTRINSIC(UNSAFE_CAST) public static FieldEventData  asFieldEventData(Object object) { return (FieldEventData) object; }
    @INTRINSIC(UNSAFE_CAST) public static MethodUnloadEventData  asMethodUnloadEventData(Object object) { return (MethodUnloadEventData) object; }
    @INTRINSIC(UNSAFE_CAST) public static FramePopEventData  asFramePopEventData(Object object) { return (FramePopEventData) object; }
    @INTRINSIC(UNSAFE_CAST) public static EventBreakpointID  asEventBreakpointID(Object object) { return (EventBreakpointID) object; }
    @INTRINSIC(UNSAFE_CAST) public static ExceptionEventData  asExceptionEventData(Object object) { return (ExceptionEventData) object; }

    private static FieldEventData setFieldEventData(JVMTIEvents.E event, Object object, int offset, boolean isStatic) {
        FieldEventData data = threadFieldEventData.get();
        data.object = object;
        data.offset = offset;
        data.isStatic = isStatic;
        return data;
    }

    // These event methods are used in T1X templates, so must not be inlined.

    @NEVER_INLINE
    public static void fieldAccessEvent(Object object, int offset, boolean isStatic) {
        FieldEventData data = setFieldEventData(E.FIELD_ACCESS, object, offset, isStatic);
        data.tag = FieldEventData.DATA_NONE;
        event(E.FIELD_ACCESS, data);
    }

    @NEVER_INLINE
    public static void fieldModificationEvent(Object object, int offset, boolean isStatic, long value) {
        FieldEventData data = setFieldEventData(E.FIELD_MODIFICATION, object, offset, isStatic);
        data.tag = FieldEventData.DATA_LONG;
        data.longValue = value;
        event(E.FIELD_MODIFICATION, data);
    }

    @NEVER_INLINE
    public static void fieldModificationEvent(Object object, int offset, boolean isStatic, float value) {
        FieldEventData data = setFieldEventData(E.FIELD_MODIFICATION, object, offset, isStatic);
        data.tag = FieldEventData.DATA_FLOAT;
        data.floatValue = value;
        event(E.FIELD_MODIFICATION, data);
    }

    @NEVER_INLINE
    public static void fieldModificationEvent(Object object, int offset, boolean isStatic, double value) {
        FieldEventData data = setFieldEventData(E.FIELD_MODIFICATION, object, offset, isStatic);
        data.tag = FieldEventData.DATA_DOUBLE;
        data.doubleValue = value;
        event(E.FIELD_MODIFICATION, data);
    }

    @NEVER_INLINE
    public static void fieldModificationEvent(Object object, int offset, boolean isStatic, Object value) {
        FieldEventData data = setFieldEventData(E.FIELD_MODIFICATION, object, offset, isStatic);
        data.tag = FieldEventData.DATA_OBJECT;
        data.objectValue = value;
        event(E.FIELD_MODIFICATION, data);
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

    private static Pointer getCallBack(Pointer callbacks, JVMTIEvents.E event) {
        int index = event.ordinal();
        return callbacks.readWord(index * Pointer.size()).asPointer();
    }

    private static final String JAVA_HOME = "java.home";
    private static final byte[] JAVA_HOME_BYTES = "JAVA_HOME".getBytes();

    static int getPhase(Pointer phasePtr) {
        int result = getPhase();
        phasePtr.setInt(0, result);
        return JVMTI_ERROR_NONE;
    }

    static int getPhase() {
        return phase == JVMTI_PHASE_START ? JVMTI_PHASE_START_ORIG : phase;
    }

    private static class AgentThreadUnion extends ModeUnion {
        final Thread thread;
        final int priority;
        // native
        Pointer env;
        Address proc;
        Pointer arg;

        AgentThreadUnion(boolean isNative, Thread thread, int priority) {
            super(isNative);
            this.thread = thread;
            this.priority = priority;
        }
    }

    static void runAgentThread(Thread thread, int priority) {
        AgentThreadUnion agu = new AgentThreadUnion(false, thread, priority);
        runAgentThread(agu);
    }

    static int runAgentThread(Pointer env, JniHandle jthread, Address proc, Pointer arg, int priority) {
        AgentThreadUnion agu = new AgentThreadUnion(true, (Thread) jthread.unhand(), priority);
        agu.env = env;
        agu.proc = proc;
        agu.arg = arg;
        return runAgentThread(agu);
    }

    static int runAgentThread(AgentThreadUnion agu) {
        /*
         * The JVMTI spec for this says:
         *
         * "The thread group of the thread is ignored -- specifically, the thread is not added to the thread group
         * and the thread is not seen on queries of the thread group at either the Java programming language or JVM TI levels.
         * The thread is not visible to Java programming language queries but is included in JVM TI queries
         * (for example, GetAllThreads and GetAllStackTraces)."
         *
         * For the native variant, there is no runnable method associated with the thread at this stage, and the default
         * run method just returns. So, knowing the implementation of Thread, we create a runnable and
         * patch the "Runnable target" with an object that will invoke the "proc" native method.
         */
        agu.thread.setPriority(agu.priority);
        agu.thread.setDaemon(true);

        if (agu.isNative) {
            new AgentThreadRunnable(agu.env, agu.thread, agu.proc.asPointer(), agu.arg);
        }
        // calling VmThread.start0 instead of Thread.start avoids adding the thread
        // to whatever thread group was set when the constructor was invoked.
        final VmThread vmThread = VmThreadFactory.create(agu.thread);
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
