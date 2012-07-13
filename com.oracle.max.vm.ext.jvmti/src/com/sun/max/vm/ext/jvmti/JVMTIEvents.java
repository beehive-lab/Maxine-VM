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
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.JVMTIBreakpoints.*;
import com.sun.max.vm.heap.Heap;
import com.sun.max.vm.log.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Support for JVMTI event handling.
 *
 * Most of the events can be set globally or per-thread, and are set per-agent.
 *
 * An event is dispatched to an agent if it is either set globally
 * or it is enabled for the generating thread, for that agent.
 *
 * Pan-agent caches are maintained for fast lookup. Event sets are represented as
 * little-endian bitset values where the bit is the ordinal value of the enum.
 * TODO Consider using EnumSet.
 *
 * Certain events, e.g. breakpoints, frame pop, require the method to be specially compiled.
 * To avoid routinely compiling in the support, a pan-agent event setting is maintained
 * that can be consulted quickly.
 *
 */
public class JVMTIEvents {

    /* N.B. The START phase is considered to have been entered when the VM sends the VM_START event,
       and similarly for VM_INIT. So when JVMTI.event receives these events it is still in the previous
       phase. To avoid a special case we simply add the previous phase to their bit setting.
     */
    private static final int LIVE_PHASE = JVMTIConstants.JVMTI_PHASE_LIVE;
    private static final int START_LIVE_PHASES = JVMTIConstants.JVMTI_PHASE_START | JVMTIConstants.JVMTI_PHASE_LIVE;
    private static final int PRIMORDIAL_START_LIVE_PHASES = JVMTIConstants.JVMTI_PHASE_PRIMORDIAL | JVMTIConstants.JVMTI_PHASE_START | JVMTIConstants.JVMTI_PHASE_LIVE;

    /**
     * Enum form of int constants.
     * Note that there are gaps in the integer values defined by JVMTI, so in order to maintain
     * the relationship based on {@code ordinal} we define the missing values as {@code MISSINGn}.
     */
    public enum E {
        VM_INIT(JVMTIConstants.JVMTI_EVENT_VM_INIT, START_LIVE_PHASES),
        VM_DEATH(JVMTIConstants.JVMTI_EVENT_VM_DEATH, LIVE_PHASE),
        THREAD_START(JVMTIConstants.JVMTI_EVENT_THREAD_START, START_LIVE_PHASES),
        THREAD_END(JVMTIConstants.JVMTI_EVENT_THREAD_END, LIVE_PHASE),
        CLASS_FILE_LOAD_HOOK(JVMTIConstants.JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, PRIMORDIAL_START_LIVE_PHASES),
        CLASS_LOAD(JVMTIConstants.JVMTI_EVENT_CLASS_LOAD, START_LIVE_PHASES),
        CLASS_PREPARE(JVMTIConstants.JVMTI_EVENT_CLASS_PREPARE, START_LIVE_PHASES),
        VM_START(JVMTIConstants.JVMTI_EVENT_VM_START, PRIMORDIAL_START_LIVE_PHASES),
        EXCEPTION(JVMTIConstants.JVMTI_EVENT_EXCEPTION, LIVE_PHASE),
        EXCEPTION_CATCH(JVMTIConstants.JVMTI_EVENT_EXCEPTION_CATCH, LIVE_PHASE),
        SINGLE_STEP(JVMTIConstants.JVMTI_EVENT_SINGLE_STEP, LIVE_PHASE),
        FRAME_POP(JVMTIConstants.JVMTI_EVENT_FRAME_POP, LIVE_PHASE),
        BREAKPOINT(JVMTIConstants.JVMTI_EVENT_BREAKPOINT, LIVE_PHASE),
        FIELD_ACCESS(JVMTIConstants.JVMTI_EVENT_FIELD_ACCESS, LIVE_PHASE),
        FIELD_MODIFICATION(JVMTIConstants.JVMTI_EVENT_FIELD_MODIFICATION, LIVE_PHASE),
        METHOD_ENTRY(JVMTIConstants.JVMTI_EVENT_METHOD_ENTRY, LIVE_PHASE),
        METHOD_EXIT(JVMTIConstants.JVMTI_EVENT_METHOD_EXIT, LIVE_PHASE),
        NATIVE_METHOD_BIND(JVMTIConstants.JVMTI_EVENT_NATIVE_METHOD_BIND, PRIMORDIAL_START_LIVE_PHASES),
        COMPILED_METHOD_LOAD(JVMTIConstants.JVMTI_EVENT_COMPILED_METHOD_LOAD, LIVE_PHASE),
        COMPILED_METHOD_UNLOAD(JVMTIConstants.JVMTI_EVENT_COMPILED_METHOD_UNLOAD, LIVE_PHASE),
        DYNAMIC_CODE_GENERATED(JVMTIConstants.JVMTI_EVENT_DYNAMIC_CODE_GENERATED, PRIMORDIAL_START_LIVE_PHASES),
        DATA_DUMP_REQUEST(JVMTIConstants.JVMTI_EVENT_DATA_DUMP_REQUEST, LIVE_PHASE),
        MISSING1(-1, 0),
        MONITOR_WAIT(JVMTIConstants.JVMTI_EVENT_MONITOR_WAIT, LIVE_PHASE),
        MONITOR_WAITED(JVMTIConstants.JVMTI_EVENT_MONITOR_WAITED, LIVE_PHASE),
        MONITOR_CONTENDED_ENTER(JVMTIConstants.JVMTI_EVENT_MONITOR_CONTENDED_ENTER, LIVE_PHASE),
        MONITOR_CONTENDED_ENTERED(JVMTIConstants.JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, LIVE_PHASE),
        MISSING2(-2, 0),
        MISSING3(-3, 0),
        MISSING4(-4, 0),
        RESOURCE_EXHAUSTED(JVMTIConstants.JVMTI_EVENT_RESOURCE_EXHAUSTED, LIVE_PHASE),
        GARBAGE_COLLECTION_START(JVMTIConstants.JVMTI_EVENT_GARBAGE_COLLECTION_START, LIVE_PHASE),
        GARBAGE_COLLECTION_FINISH(JVMTIConstants.JVMTI_EVENT_GARBAGE_COLLECTION_FINISH, LIVE_PHASE),
        OBJECT_FREE(JVMTIConstants.JVMTI_EVENT_OBJECT_FREE, LIVE_PHASE),
        VM_OBJECT_ALLOC(JVMTIConstants.JVMTI_EVENT_VM_OBJECT_ALLOC, LIVE_PHASE);

        /**
         * The JVMTI code for this event.
         */
        public final int code;

        /**
         * The zero based bit for bitsets containing this event.
         */
        public final long bit;

        /**
         * The phases, as bit set, in which this event can be delivered.
         */
        public final int phases;

        public static final E[] VALUES = values();

        private static final int EVENT_COUNT = VALUES.length;

        public static E fromEventId(int eventId) {
            if (eventId < JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL || eventId > JVMTIConstants.JVMTI_MAX_EVENT_TYPE_VAL) {
                return null;
            }
            return VALUES[eventId - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL];
        }

        @HOSTED_ONLY
        private E(int code, int phases) {
            if (code > 0) {
                FatalError.check(code - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL == ordinal(), "JVMTIEvent code mismatch");
            }
            this.code = code;
            this.phases = phases;
            bit = 1L << ordinal();
        }

    }

    public static class JVMTIEventLogger extends VMLogger {
        public static final int SUPPRESSED = 1;
        public static final int DELIVERED = 0;

        private JVMTIEventLogger() {
            super("JVMTIEvents", E.EVENT_COUNT, "log JVMTI events");
        }

        @Override
        public String operationName(int op) {
            return E.VALUES[op].name();
        }

        void logSuppressedEvent(E event) {
            log(event.ordinal(), intArg(SUPPRESSED));
        }

        void logEvent(E event, JVMTI.Env env, Object arg) {
            int ord = event.ordinal();
            Word envArg = objectArg(env);
            Word delivered = intArg(DELIVERED);
            switch (event) {
                case CLASS_LOAD:
                case CLASS_PREPARE: {
                    log(ord, envArg, delivered, classActorArg((ClassActor) arg));
                    break;
                }
                case BREAKPOINT: {
                    EventBreakpointID bptId = (EventBreakpointID) arg;
                    log(ord, envArg, delivered, Address.fromLong(bptId.methodID), intArg(bptId.location));
                    break;
                }
                case COMPILED_METHOD_LOAD: {
                    log(ord, envArg, delivered, methodActorArg((MethodActor) arg));
                    break;
                }

                default:
                    log(ord, envArg, delivered);
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
                JVMTI.event(E.GARBAGE_COLLECTION_START);
            } else if (gcCallbackPhase == Heap.GCCallbackPhase.AFTER) {
                JVMTI.event(E.GARBAGE_COLLECTION_FINISH);
            }
        }
    }

    /**
     * Per-agent, per-thread event settings.
     */
    static class PerThreadSettings {
        private Map<Thread, JVMTIEvents.MutableLong> settings;
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

        private Map<Thread, JVMTIEvents.MutableLong> checkMap() {
            if (settings == null) {
                settings = new HashMap<Thread, JVMTIEvents.MutableLong>();
            }
            return settings;
        }
    }


    /**
     * These events require compiler support.
     */
    static long CODE_EVENTS_SETTING =
                                   E.FIELD_ACCESS.bit | E.FIELD_MODIFICATION.bit |
                                   E.METHOD_ENTRY.bit | E.METHOD_EXIT.bit |
                                   E.BREAKPOINT.bit | E.SINGLE_STEP.bit |
                                   E.FRAME_POP.bit | E.EXCEPTION_CATCH.bit;

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
     */
    public static boolean isEventSet(JVMTIEvents.E event) {
        return (event.bit & panAgentEventSettingCache) != 0;
    }

    /**
     * Checks whether the given event is set for given agent, either globally or for a given thread.
     * @param env environment to check
     * @param event
     * @param vmThread thread to check
     */
    static boolean isEventSet(JVMTI.Env env, JVMTIEvents.E event, VmThread vmThread) {
        long setting = event.bit;
        if ((setting & env.globalEventSettings) != 0) {
            return true;
        } else {
            return (env.perThreadEventSettings.get(vmThread.javaThread()) & setting) != 0;
        }
    }

    /**
     * Check whether any of the events requiring compiler support are set for any agent, either globally or for any thread.
     */
    static boolean anyCodeEventsSet() {
        return (panAgentEventSettingCache & JVMTIEvents.CODE_EVENTS_SETTING) != 0;
    }

    static {
        Heap.registerGCCallback(new GCCallback());
    }

    /**
     * Gets the bit setting to determine if an event should be delivered based on the phase.
     */
    static int getPhases(E event) {
        return event.phases;
    }

    @NEVER_INLINE
    private static void debug() {

    }

    static int setEventNotificationMode(JVMTI.Env jvmtiEnv, int mode, E event, Thread thread) {
        if (thread == null) {
            // Global
            long newBits = newEventBits(event, mode, jvmtiEnv.globalEventSettings);
            if (newBits < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            jvmtiEnv.globalEventSettings = newBits;
        } else {
            // Per-thread
            if (JVMTIThreadFunctions.checkThread(thread) == null) {
                return JVMTI_ERROR_THREAD_NOT_ALIVE;
            }
            long newBits = newEventBits(event, mode, jvmtiEnv.perThreadEventSettings.get(thread));
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
            if (jvmtiEnv == null || jvmtiEnv.isFree()) {
                continue;
            }
            panAgentGlobalEventSettingCache |= jvmtiEnv.globalEventSettings;
            panAgentThreadEventSettingCache |= jvmtiEnv.perThreadEventSettings.panThreadSettings;
            panAgentEventSettingCache |= jvmtiEnv.globalEventSettings | jvmtiEnv.perThreadEventSettings.panThreadSettings;
        }

        if (event == E.SINGLE_STEP) {
            JVMTIBreakpoints.setSingleStep(mode == JVMTI_ENABLE);
        }

        return JVMTI_ERROR_NONE;
    }

    /**
     * Implementation of upcall to enable/disable event notification.
     */
    static int setEventNotificationMode(JVMTI.Env jvmtiEnv, int mode, int eventId, Thread thread) {
        if (eventId == JVMTI_EVENT_METHOD_ENTRY) {
            debug();
        }
        E event = E.fromEventId(eventId);
        if (event == null) {
            return JVMTI_ERROR_INVALID_EVENT_TYPE;
        }
        return setEventNotificationMode(jvmtiEnv, mode, event, thread);
    }

    static long codeEventSettings(JVMTI.Env jvmtiEnv, VmThread vmThread) {
        long settings;
        if (jvmtiEnv == null || vmThread == null) {
            // called from compiled code on a frame pop
            settings = panAgentEventSettingCache;
        } else {
            settings = jvmtiEnv.perThreadEventSettings.get(vmThread.javaThread());
        }
        return settings & JVMTIEvents.CODE_EVENTS_SETTING;
    }

    private static long newEventBits(E event, int mode, long oldBits) {
        long bitSetting = event.bit;
        if (mode == JVMTI_ENABLE) {
            return oldBits | bitSetting;
        } else if (mode == JVMTI_DISABLE) {
            return oldBits & ~bitSetting;
        } else {
            return -1;
        }
    }

    @HOSTED_ONLY
    public static String bitToName(long bitSetting) {
        for (E event : E.VALUES) {
            if (bitSetting == event.bit) {
                return event.name();
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
                sb.append(JVMTIEvents.bitToName(bit));
            }
        }
        return sb.toString();
    }

}
