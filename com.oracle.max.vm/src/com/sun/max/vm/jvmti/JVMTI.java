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

import static com.sun.max.vm.jvmti.JJJCallbacks.*;
import static com.sun.max.vm.jvmti.JJJConstants.*;
import static com.sun.max.vm.jvmti.JJJEnvImplFields.*;
import static com.sun.max.vm.jvmti.JJJVMOptions.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * The heart of the Maxine JVMTI implementation.
 */
public class JVMTI {
    private static final String AGENT_ONLOAD = "Agent_OnLoad";
    private static final String AGENT_ONUNLOAD = "Agent_OnUnLoad";

    private static class JVMTIEnv {
        Pointer env;
    }

    /**
     * Since the agent initialization code happens very early, before we have a function heap,
     * we static allocate storage for key data structures. The day that someone tries to runs
     * Maxine with more than {@link MAX_ENVS} agents, we will celebrate -;)
     */
    static final int MAX_ENVS = 8;

    /**
     * The record of registered agent environments, used to handle callbacks.
     */
    private static final JVMTIEnv[] jvmtiEnvs;
    /**
     * The {@link #jvmtiEnvs} array is updated indirectly by the {@link #SetJVMTIEnv(Pointer)} upcall
     * during agent initialization. Hence we need a static variable to index the array.
     */
    private static int jvmtiEnvsIndex;

    /**
     * The phase of execution.
     */
    static int phase = JVMTI_PHASE_ONLOAD;

    static {
        jvmtiEnvs = new JVMTIEnv[MAX_ENVS];
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            jvmtiEnvs[i] = new JVMTIEnv();
        }
        new CriticalNativeMethod(JVMTI.class, "jvmtiCurrentJniEnv");
    }

    @C_FUNCTION
    static native void log_print_buffer(Address val);

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
    }

    public static void initialize() {
        JJJRawMonitor.initialize();
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
    public static Pointer prologue(Pointer env, String name) {
        return JniFunctions.prologue(jvmtiCurrentJniEnv(), name);
    }

    public static void vmEvent(int eventId) {
        if (phase == JVMTI_PHASE_DEAD) {
            // shouldn't happen but just in case, spec says no delivery.
            return;
        }

        if (eventId == JVMTI_EVENT_VM_START) {
            phase = JVMTI_PHASE_START;
        } else if (eventId == JVMTI_EVENT_VM_INIT) {
            phase = JVMTI_PHASE_LIVE;
        }

        // Check that event is enabled and dispatch it to all registered agents
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            Pointer jvmtiEnv = jvmtiEnvs[i].env;
            if (jvmtiEnv.isZero()) {
                continue;
            }
            // just global for now
            long envMask = EVENTMASK.get(jvmtiEnv).asAddress().toLong();
            long maskBit = getEventBitMask(eventId);
            if ((envMask & maskBit) != 0) {
                Pointer callback = getCallBack(CALLBACKS.getPtr(jvmtiEnv), eventId);

                if (callback.isZero()) {
                    continue;
                }
                switch (eventId) {
                    case JVMTI_EVENT_VM_START:
                    case JVMTI_EVENT_VM_DEATH:
                        invokeStartFunctionNoArg(callback, jvmtiEnv);
                        break;
                    case JVMTI_EVENT_VM_INIT:
                        invokeStartFunction(callback, jvmtiEnv, JniHandles.createLocalHandle(VmThread.current().javaThread()));
                        break;

                    case JVMTI_EVENT_GARBAGE_COLLECTION_START:
                    case JVMTI_EVENT_GARBAGE_COLLECTION_FINISH:
                        invokeGarbageCollectionCallback(callback, jvmtiEnv);
                        break;
                }
            }
        }
        if (eventId == JVMTI_EVENT_VM_DEATH) {
            phase = JVMTI_PHASE_DEAD;
        }
    }

    private static Pointer getCallBack(Pointer callbacks, int eventId) {
        int index = eventId - JVMTI_MIN_EVENT_TYPE_VAL;
        return callbacks.readWord(index * Pointer.size()).asPointer();
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
