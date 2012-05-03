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

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.ext.jvmti.JVMTIBreakpoints.*;
import com.sun.max.vm.heap.Heap;
import com.sun.max.vm.log.*;
import com.sun.max.vm.thread.*;

/**
 * Support for JVMTI event handling.
 *
 * Most of the events can be set globally or per-thread, and are set per-agent.
 *
 * An event is dispatched to an agent if it is either set globally
 * or it is enabled for the generating thread, for that agent.
 *
 * Pan-agent caches are maintained for fast lookup.
 *
 * Certain events, e.g. breakpoints, frame pop, require the method to be specially compiled.
 * To avoid routinely compiling in the support, a pan-agent event setting is maintained
 * that can be consulted quickly.
 *
 * TODO Consider changing this to an enum.
 */
public class JVMTIEvent {

    public static class JVMTIEventLogger extends VMLogger {
        private JVMTIEventLogger() {
            super("JVMTIEvents", EVENT_COUNT, null);
        }

        @Override
        public String operationName(int op) {
            return name(toEventId(op));
        }

        public static int toEventId(int op) {
            return op + JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL;
        }

        void logEvent(int eventId, boolean ignoring, Object arg) {
            int zEventId = eventId - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL;
            switch (eventId) {
                case CLASS_LOAD:
                case CLASS_PREPARE: {
                    log(zEventId, booleanArg(ignoring), classActorArg((ClassActor) arg));
                    break;
                }
                case BREAKPOINT: {
                    EventBreakpointID bptId = (EventBreakpointID) arg;
                    log(zEventId, booleanArg(ignoring), Address.fromLong(bptId.methodID), intArg(bptId.location));
                    break;
                }

                default:
                    log(zEventId, booleanArg(ignoring));
            }
        }

    }

    static final JVMTIEventLogger logger = new JVMTIEventLogger();

    static class MutableLong {
        long value;

        MutableLong(long value) {
            this.value = value;
        }
    }

    private static class GCCallback implements Heap.GCCallback{

        public void gcCallback(Heap.GCCallbackPhase gcCallbackPhase) {
            if (gcCallbackPhase == Heap.GCCallbackPhase.BEFORE) {
                JVMTI.event(JVMTIEvent.GARBAGE_COLLECTION_START);
            } else if (gcCallbackPhase == Heap.GCCallbackPhase.AFTER) {
                JVMTI.event(JVMTIEvent.GARBAGE_COLLECTION_FINISH);
            }
        }
    }

    /**
     * Per-agent, per-thread event settings.
     */
    static class PerThreadSettings {
        private Map<Thread, JVMTIEvent.MutableLong> settings;
        /**
         * per-agent setting across all threads.
         */
        long panThreadSettings;

        long get(Thread thread) {
            checkMap();
            MutableLong ml =  settings.get(thread);
            if (ml == null) {
                return 0;
            } else {
                return ml.value;
            }
        }

        void set(Thread thread, long value) {
            checkMap();
            MutableLong ml = settings.get(thread);
            if (ml == null) {
                ml = new MutableLong(value);
                settings.put(thread, ml);
            } else {
                ml.value = value;
            }
            panThreadSettings = 0;
            for (MutableLong mlx : settings.values()) {
                panThreadSettings |= mlx.value;
            }
        }

        private Map<Thread, JVMTIEvent.MutableLong> checkMap() {
            if (settings == null) {
                settings = new HashMap<Thread, JVMTIEvent.MutableLong>();
            }
            return settings;
        }
    }

    // Event IDs
    public static final int VM_INIT = JVMTIConstants.JVMTI_EVENT_VM_INIT;
    public static final int VM_DEATH = JVMTIConstants.JVMTI_EVENT_VM_DEATH;
    public static final int THREAD_START = JVMTIConstants.JVMTI_EVENT_THREAD_START;
    public static final int THREAD_END = JVMTIConstants.JVMTI_EVENT_THREAD_END;
    public static final int CLASS_FILE_LOAD_HOOK = JVMTIConstants.JVMTI_EVENT_CLASS_FILE_LOAD_HOOK;
    public static final int CLASS_LOAD = JVMTIConstants.JVMTI_EVENT_CLASS_LOAD;
    public static final int CLASS_PREPARE = JVMTIConstants.JVMTI_EVENT_CLASS_PREPARE;
    public static final int VM_START = JVMTIConstants.JVMTI_EVENT_VM_START;
    public static final int EXCEPTION = JVMTIConstants.JVMTI_EVENT_EXCEPTION;
    public static final int EXCEPTION_CATCH = JVMTIConstants.JVMTI_EVENT_EXCEPTION_CATCH;
    public static final int SINGLE_STEP = JVMTIConstants.JVMTI_EVENT_SINGLE_STEP;
    public static final int FRAME_POP = JVMTIConstants.JVMTI_EVENT_FRAME_POP;
    public static final int BREAKPOINT = JVMTIConstants.JVMTI_EVENT_BREAKPOINT;
    public static final int FIELD_ACCESS = JVMTIConstants.JVMTI_EVENT_FIELD_ACCESS;
    public static final int FIELD_MODIFICATION = JVMTIConstants.JVMTI_EVENT_FIELD_MODIFICATION;
    public static final int METHOD_ENTRY = JVMTIConstants.JVMTI_EVENT_METHOD_ENTRY;
    public static final int METHOD_EXIT = JVMTIConstants.JVMTI_EVENT_METHOD_EXIT;
    public static final int NATIVE_METHOD_BIND = JVMTIConstants.JVMTI_EVENT_NATIVE_METHOD_BIND;
    public static final int COMPILED_METHOD_LOAD = JVMTIConstants.JVMTI_EVENT_COMPILED_METHOD_LOAD;
    public static final int COMPILED_METHOD_UNLOAD = JVMTIConstants.JVMTI_EVENT_COMPILED_METHOD_UNLOAD;
    public static final int DYNAMIC_CODE_GENERATED = JVMTIConstants.JVMTI_EVENT_DYNAMIC_CODE_GENERATED;
    public static final int DATA_DUMP_REQUEST = JVMTIConstants.JVMTI_EVENT_DATA_DUMP_REQUEST;
    public static final int MONITOR_WAIT = JVMTIConstants.JVMTI_EVENT_MONITOR_WAIT;
    public static final int MONITOR_WAITED = JVMTIConstants.JVMTI_EVENT_MONITOR_WAITED;
    public static final int MONITOR_CONTENDED_ENTER = JVMTIConstants.JVMTI_EVENT_MONITOR_CONTENDED_ENTER;
    public static final int MONITOR_CONTENDED_ENTERED = JVMTIConstants.JVMTI_EVENT_MONITOR_CONTENDED_ENTERED;
    public static final int RESOURCE_EXHAUSTED = JVMTIConstants.JVMTI_EVENT_RESOURCE_EXHAUSTED;
    public static final int GARBAGE_COLLECTION_START = JVMTIConstants.JVMTI_EVENT_GARBAGE_COLLECTION_START;
    public static final int GARBAGE_COLLECTION_FINISH = JVMTIConstants.JVMTI_EVENT_GARBAGE_COLLECTION_FINISH;
    public static final int OBJECT_FREE = JVMTIConstants.JVMTI_EVENT_OBJECT_FREE;
    public static final int VM_OBJECT_ALLOC = JVMTIConstants.JVMTI_EVENT_VM_OBJECT_ALLOC;

    private static final int EVENT_COUNT = JVMTIConstants.JVMTI_MAX_EVENT_TYPE_VAL - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL + 1;

    /**
     * These events require compiler support.
     */
    static long CODE_EVENTS_SETTING = computeEventBitSetting(FIELD_ACCESS) | computeEventBitSetting(FIELD_MODIFICATION) |
                                   computeEventBitSetting(METHOD_ENTRY) | computeEventBitSetting(METHOD_EXIT) |
                                   computeEventBitSetting(BREAKPOINT) | computeEventBitSetting(SINGLE_STEP) |
                                   computeEventBitSetting(FRAME_POP);

    /**
     * Returns the bit setting for the given event, or -1 if invalid.
     * The bit numbers are zero based, i.e. modulo {@link #JVMTI_MIN_EVENT_TYPE_VAL}.
     */
    private static long computeEventBitSetting(int eventType) {
        return 1L << (eventType - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL);
    }

    /**
     * Pre-computed bit settings for each event.
     */
    private static final long[] bitSettings = new long[EVENT_COUNT];

    /**
     * This provides a fast check for compiled code event checks.
     * It is the union of the event setting for all agents, both global and per-thread
     */
    private static long panAgentEventSettingCache;

    /**
     * Fast check just for global settings across all agents.
     */
    private static long panAgentGlobalEventSettingCache;

    /**
     * Fast check just for per-thread settings across all agents.
     */
    private static long panAgentThreadEventSettingCache;

    /**
     * Checks whether the given event is set for any agent, either globally or for any thread.
     * @param eventType
     * @return
     */
    static boolean isEventSet(int eventType) {
        return (bitSettings[eventType - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL] & panAgentEventSettingCache) != 0;
    }

    /**
     * Checks whether the given event is set for any agent, either globally or for a given thread.
     * @param eventType
     * @param vmThread thread to check
     * @return
     */
    static boolean isEventSet(JVMTI.Env env, int eventType, VmThread vmThread) {
        long setting = bitSettings[eventType - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL];
        if ((setting & env.globalEventSettings) != 0) {
            return true;
        } else {
            return (env.perThreadEventSettings.get(vmThread.javaThread()) & setting) != 0;
        }
    }

    /**
     * Check whether any of the events requiring compiler support are set for any agent, either globally or for any thread.
     * @return
     */
    static boolean anyCodeEventsSet() {
        return (panAgentEventSettingCache & JVMTIEvent.CODE_EVENTS_SETTING) != 0;
    }

    /**
     * A set of bits that correspond to the phases in which it is legal to dispatch the event.
     */
    private static final int[] phases = new int[EVENT_COUNT];

    static {
        Heap.registerGCCallback(new GCCallback());

        for (int i = JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL; i <= JVMTIConstants.JVMTI_MAX_EVENT_TYPE_VAL; i++) {
            int eventPhase = JVMTIConstants.JVMTI_PHASE_LIVE;
            /* N.B. The START phase is considered to have been entered when the VM sends the VM_START event,
               and similarly for VM_INIT. So when JVMTI.event receives these events it is still in the previous
               phase. To avoid a special case we simply add the previous phase to their bit setting.
            */
            switch (i) {
                case VM_INIT:
                case THREAD_START:
                case THREAD_END:
                case CLASS_LOAD:
                case CLASS_PREPARE:
                    eventPhase = JVMTIConstants.JVMTI_PHASE_START | JVMTIConstants.JVMTI_PHASE_LIVE;
                    break;

                case VM_START:
                case CLASS_FILE_LOAD_HOOK:
                case NATIVE_METHOD_BIND:
                case DYNAMIC_CODE_GENERATED:
                    eventPhase = JVMTIConstants.JVMTI_PHASE_PRIMORDIAL | JVMTIConstants.JVMTI_PHASE_START | JVMTIConstants.JVMTI_PHASE_LIVE;
                    break;

                case EXCEPTION:
                case EXCEPTION_CATCH:
                case SINGLE_STEP:
                case FRAME_POP:
                case BREAKPOINT:
                case FIELD_ACCESS:
                case FIELD_MODIFICATION:
                case METHOD_ENTRY:
                case METHOD_EXIT:
                case COMPILED_METHOD_LOAD:
                case COMPILED_METHOD_UNLOAD:
                case DATA_DUMP_REQUEST:
                case MONITOR_WAIT:
                case MONITOR_WAITED:
                case MONITOR_CONTENDED_ENTER:
                case MONITOR_CONTENDED_ENTERED:
                case RESOURCE_EXHAUSTED:
                case GARBAGE_COLLECTION_START:
                case GARBAGE_COLLECTION_FINISH:
                case OBJECT_FREE:
                case VM_OBJECT_ALLOC:
                case VM_DEATH:
                    // LIVE
                    break;
            }
            phases[i - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL] = eventPhase;
            bitSettings[i - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL] = computeEventBitSetting(i);
        }
    }

    /**
     * Gets the bit setting to determine if an event should be delivered based on the phase.
     */
    static int getPhase(int eventType) {
        return phases[eventType - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL];
    }

    /**
     * Implementation of upcall to enable/disable event notification.
     */
    static int setEventNotificationMode(JVMTI.Env jvmtiEnv, int mode, int eventType, Thread thread) {
        if (eventType < JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL || eventType > JVMTIConstants.JVMTI_MAX_EVENT_TYPE_VAL) {
            return JVMTI_ERROR_INVALID_EVENT_TYPE;
        }
        if (eventType == JVMTI_EVENT_EXCEPTION_CATCH) {
            return JVMTI_ERROR_ACCESS_DENIED;
        }
        if (thread == null) {
            // Global
            long newBits = newEventBits(eventType, mode, jvmtiEnv.globalEventSettings);
            if (newBits < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            jvmtiEnv.globalEventSettings = newBits;
        } else {
            // Per-thread
            long newBits = newEventBits(eventType, mode, jvmtiEnv.perThreadEventSettings.get(thread));
            if (newBits < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            jvmtiEnv.perThreadEventSettings.set(thread, newBits);
        }


        // recompute pan agent caches
        panAgentEventSettingCache = 0;
        panAgentThreadEventSettingCache = 0;
        panAgentGlobalEventSettingCache = 0;

        for (int i = 0; i < JVMTI.jvmtiEnvs.length; i++) {
            jvmtiEnv = JVMTI.jvmtiEnvs[i];
            if (jvmtiEnv.env.isZero()) {
                continue;
            }
            panAgentGlobalEventSettingCache |= jvmtiEnv.globalEventSettings;
            panAgentThreadEventSettingCache |= jvmtiEnv.perThreadEventSettings.panThreadSettings;
            panAgentEventSettingCache |= jvmtiEnv.globalEventSettings | jvmtiEnv.perThreadEventSettings.panThreadSettings;
        }

        if (eventType == SINGLE_STEP) {
            JVMTIBreakpoints.setSingleStep(mode == JVMTI_ENABLE);
        }

        return JVMTI_ERROR_NONE;
    }

    static long codeEventSettings(JVMTI.Env jvmtiEnv, VmThread vmThread) {
        long settings;
        if (jvmtiEnv == null || vmThread == null) {
            // called from compiled code on a frame pop
            settings = panAgentEventSettingCache;
        } else {
            settings = jvmtiEnv.perThreadEventSettings.get(vmThread.javaThread());
        }
        return settings & JVMTIEvent.CODE_EVENTS_SETTING;
    }

    public static long bitSetting(int eventType) {
        return bitSettings[eventType - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL];
    }

    private static long newEventBits(int eventType, int mode, long oldBits) {
        long bitSetting = bitSetting(eventType);
        if (mode == JVMTI_ENABLE) {
            return oldBits | bitSetting;
        } else if (mode == JVMTI_DISABLE) {
            return oldBits & ~bitSetting;
        } else {
            return -1;
        }
    }

    @NEVER_INLINE
    private static void debug() {

    }

    /**
     * Event tracing support.
     * @param id
     * @return
     */
    public static String name(int id) {
        switch (id) {
            // Checkstyle: stop
            case JVMTI_EVENT_VM_INIT: return "VM_INIT";
            case JVMTI_EVENT_VM_DEATH: return "VM_DEATH";
            case JVMTI_EVENT_THREAD_START: return "THREAD_START";
            case JVMTI_EVENT_THREAD_END: return "THREAD_END";
            case JVMTI_EVENT_CLASS_FILE_LOAD_HOOK: return "CLASS_FILE_LOAD_HOOK";
            case JVMTI_EVENT_CLASS_LOAD: return "CLASS_LOAD";
            case JVMTI_EVENT_CLASS_PREPARE: return "CLASS_PREPARE";
            case JVMTI_EVENT_VM_START: return "VM_START";
            case JVMTI_EVENT_EXCEPTION: return "EXCEPTION";
            case JVMTI_EVENT_EXCEPTION_CATCH: return "EXCEPTION_CATCH";
            case JVMTI_EVENT_SINGLE_STEP: return "SINGLE_STEP";
            case JVMTI_EVENT_FRAME_POP: return "FRAME_POP";
            case JVMTI_EVENT_BREAKPOINT: return "BREAKPOINT";
            case JVMTI_EVENT_FIELD_ACCESS: return "FIELD_ACCESS";
            case JVMTI_EVENT_FIELD_MODIFICATION: return "FIELD_MODIFICATION";
            case JVMTI_EVENT_METHOD_ENTRY: return "METHOD_ENTRY";
            case JVMTI_EVENT_METHOD_EXIT: return "METHOD_EXIT";
            case JVMTI_EVENT_NATIVE_METHOD_BIND: return "NATIVE_METHOD_BIND";
            case JVMTI_EVENT_COMPILED_METHOD_LOAD: return "COMPILED_METHOD_LOAD";
            case JVMTI_EVENT_COMPILED_METHOD_UNLOAD: return "COMPILED_METHOD_UNLOAD";
            case JVMTI_EVENT_DYNAMIC_CODE_GENERATED: return "DYNAMIC_CODE_GENERATED";
            case JVMTI_EVENT_DATA_DUMP_REQUEST: return "DATA_DUMP_REQUEST";
            case JVMTI_EVENT_MONITOR_WAIT: return "MONITOR_WAIT";
            case JVMTI_EVENT_MONITOR_WAITED: return "MONITOR_WAITED";
            case JVMTI_EVENT_MONITOR_CONTENDED_ENTER: return "MONITOR_CONTENDED_ENTER";
            case JVMTI_EVENT_MONITOR_CONTENDED_ENTERED: return "MONITOR_CONTENDED_ENTERED";
            case JVMTI_EVENT_RESOURCE_EXHAUSTED: return "RESOURCE_EXHAUSTED";
            case JVMTI_EVENT_GARBAGE_COLLECTION_START: return "GARBAGE_COLLECTION_START";
            case JVMTI_EVENT_GARBAGE_COLLECTION_FINISH: return "GARBAGE_COLLECTION_FINISH";
            case JVMTI_EVENT_OBJECT_FREE: return "OBJECT_FREE";
            case JVMTI_EVENT_VM_OBJECT_ALLOC: return "VM_OBJECT_ALLOC";
            default: return "UNKNOWN EVENT";
            // Checkstyle: resume
        }
    }

    @HOSTED_ONLY
    public static String bitToName(long bitSetting) {
        for (int i = 0; i < bitSettings.length; i++) {
            if (bitSettings[i] == bitSetting) {
                return name(i + JVMTI_MIN_EVENT_TYPE_VAL);
            }
        }
        return "???";
    }

    @HOSTED_ONLY
    public static String inspectEventSettings(long settings) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < 63; i++) {
            long bit = 1L << i;
            if ((settings & bit) != 0) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(JVMTIEvent.bitToName(bit));
            }
        }
        return sb.toString();
    }

}
