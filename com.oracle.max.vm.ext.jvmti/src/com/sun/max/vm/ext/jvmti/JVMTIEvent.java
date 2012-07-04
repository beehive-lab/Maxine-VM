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
 * Pan-agent caches are maintained for fast lookup. Event sets are represented as
 * little-endian bitset values where the bit is the ordinal value of the enum.
 * TODO Consider using EnumSet.
 *
 * Certain events, e.g. breakpoints, frame pop, require the method to be specially compiled.
 * To avoid routinely compiling in the support, a pan-agent event setting is maintained
 * that can be consulted quickly.
 *
 */
public class JVMTIEvent {

    /**
     * Enum form of int constants.
     * These are ordered the same as the constants so that their ordinal value is the correct zero-based
     * value for a bit mask.
     */
    public enum E {
        VM_INIT(JVMTIConstants.JVMTI_EVENT_VM_INIT),
        VM_DEATH(JVMTIConstants.JVMTI_EVENT_VM_DEATH),
        THREAD_START(JVMTIConstants.JVMTI_EVENT_THREAD_START),
        THREAD_END(JVMTIConstants.JVMTI_EVENT_THREAD_END),
        CLASS_FILE_LOAD_HOOK(JVMTIConstants.JVMTI_EVENT_CLASS_FILE_LOAD_HOOK),
        CLASS_LOAD(JVMTIConstants.JVMTI_EVENT_CLASS_LOAD),
        CLASS_PREPARE(JVMTIConstants.JVMTI_EVENT_CLASS_PREPARE),
        VM_START(JVMTIConstants.JVMTI_EVENT_VM_START),
        EXCEPTION(JVMTIConstants.JVMTI_EVENT_EXCEPTION),
        EXCEPTION_CATCH(JVMTIConstants.JVMTI_EVENT_EXCEPTION_CATCH),
        SINGLE_STEP(JVMTIConstants.JVMTI_EVENT_SINGLE_STEP),
        FRAME_POP(JVMTIConstants.JVMTI_EVENT_FRAME_POP),
        BREAKPOINT(JVMTIConstants.JVMTI_EVENT_BREAKPOINT),
        FIELD_ACCESS(JVMTIConstants.JVMTI_EVENT_FIELD_ACCESS),
        FIELD_MODIFICATION(JVMTIConstants.JVMTI_EVENT_FIELD_MODIFICATION),
        METHOD_ENTRY(JVMTIConstants.JVMTI_EVENT_METHOD_ENTRY),
        METHOD_EXIT(JVMTIConstants.JVMTI_EVENT_METHOD_EXIT),
        NATIVE_METHOD_BIND(JVMTIConstants.JVMTI_EVENT_NATIVE_METHOD_BIND),
        COMPILED_METHOD_LOAD(JVMTIConstants.JVMTI_EVENT_COMPILED_METHOD_LOAD),
        COMPILED_METHOD_UNLOAD(JVMTIConstants.JVMTI_EVENT_COMPILED_METHOD_UNLOAD),
        DYNAMIC_CODE_GENERATED(JVMTIConstants.JVMTI_EVENT_DYNAMIC_CODE_GENERATED),
        DATA_DUMP_REQUEST(JVMTIConstants.JVMTI_EVENT_DATA_DUMP_REQUEST),
        MONITOR_WAIT(JVMTIConstants.JVMTI_EVENT_MONITOR_WAIT),
        MONITOR_WAITED(JVMTIConstants.JVMTI_EVENT_MONITOR_WAITED),
        MONITOR_CONTENDED_ENTER(JVMTIConstants.JVMTI_EVENT_MONITOR_CONTENDED_ENTER),
        MONITOR_CONTENDED_ENTERED(JVMTIConstants.JVMTI_EVENT_MONITOR_CONTENDED_ENTERED),
        RESOURCE_EXHAUSTED(JVMTIConstants.JVMTI_EVENT_RESOURCE_EXHAUSTED),
        GARBAGE_COLLECTION_START(JVMTIConstants.JVMTI_EVENT_GARBAGE_COLLECTION_START),
        GARBAGE_COLLECTION_FINISH(JVMTIConstants.JVMTI_EVENT_GARBAGE_COLLECTION_FINISH),
        OBJECT_FREE(JVMTIConstants.JVMTI_EVENT_OBJECT_FREE),
        VM_OBJECT_ALLOC(JVMTIConstants.JVMTI_EVENT_VM_OBJECT_ALLOC);

        public final int code;

        public static final E[] VALUES = values();

        private static final int EVENT_COUNT = VALUES.length;

        public static E fromEventId(int eventId) {
            if (eventId < JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL || eventId > JVMTIConstants.JVMTI_MAX_EVENT_TYPE_VAL) {
                return null;
            }
            return VALUES[eventId - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL];
        }

        private E(int code) {
            this.code = code;
        }


    }
    public static class JVMTIEventLogger extends VMLogger {
        private JVMTIEventLogger() {
            super("JVMTIEvents", E.EVENT_COUNT, "log JVMTI events");
        }

        @Override
        public String operationName(int op) {
            return E.VALUES[op].name();
        }

        void logEvent(E event, boolean ignoring, Object arg) {
            switch (event) {
                case CLASS_LOAD:
                case CLASS_PREPARE: {
                    log(event.ordinal(), booleanArg(ignoring), classActorArg((ClassActor) arg));
                    break;
                }
                case BREAKPOINT: {
                    EventBreakpointID bptId = (EventBreakpointID) arg;
                    log(event.ordinal(), booleanArg(ignoring), Address.fromLong(bptId.methodID), intArg(bptId.location));
                    break;
                }

                default:
                    log(event.ordinal(), booleanArg(ignoring));
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


    /**
     * These events require compiler support.
     */
    static long CODE_EVENTS_SETTING = computeEventBitSetting(E.FIELD_ACCESS) | computeEventBitSetting(E.FIELD_MODIFICATION) |
                                   computeEventBitSetting(E.METHOD_ENTRY) | computeEventBitSetting(E.METHOD_EXIT) |
                                   computeEventBitSetting(E.BREAKPOINT) | computeEventBitSetting(E.SINGLE_STEP) |
                                   computeEventBitSetting(E.FRAME_POP);

    /**
     * Returns the bit setting for the given event, or -1 if invalid.
     * The bit numbers are zero based, i.e. modulo {@link #JVMTI_MIN_EVENT_TYPE_VAL}.
     */
    private static long computeEventBitSetting(E event) {
        return 1L << event.ordinal();
    }

    /**
     * Pre-computed bit settings for each event.
     */
    private static final long[] bitSettings = new long[E.EVENT_COUNT];

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
    static boolean isEventSet(JVMTIEvent.E event) {
        return (bitSettings[event.ordinal()] & panAgentEventSettingCache) != 0;
    }

    /**
     * Checks whether the given event is set for any agent, either globally or for a given thread.
     * @param eventType
     * @param vmThread thread to check
     * @return
     */
    static boolean isEventSet(JVMTI.Env env, JVMTIEvent.E event, VmThread vmThread) {
        long setting = bitSettings[event.ordinal()];
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
    private static final int[] phases = new int[E.EVENT_COUNT];

    static {
        Heap.registerGCCallback(new GCCallback());

        for (int i = 0; i < E.VALUES.length; i++) {
            E event = E.VALUES[i];
            int eventPhase = JVMTIConstants.JVMTI_PHASE_LIVE;
            /* N.B. The START phase is considered to have been entered when the VM sends the VM_START event,
               and similarly for VM_INIT. So when JVMTI.event receives these events it is still in the previous
               phase. To avoid a special case we simply add the previous phase to their bit setting.
            */
            switch (event) {
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
            phases[event.ordinal()] = eventPhase;
            bitSettings[event.ordinal()] = computeEventBitSetting(event);
        }
    }

    /**
     * Gets the bit setting to determine if an event should be delivered based on the phase.
     */
    static int getPhase(E event) {
        return phases[event.ordinal()];
    }

    /**
     * Implementation of upcall to enable/disable event notification.
     */
    static int setEventNotificationMode(JVMTI.Env jvmtiEnv, int mode, int eventId, Thread thread) {
        E event = E.fromEventId(eventId);
        if (event == null) {
            return JVMTI_ERROR_INVALID_EVENT_TYPE;
        }

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

        if (eventId == E.SINGLE_STEP.code) {
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

    public static long bitSetting(E event) {
        return bitSettings[event.ordinal()];
    }

    private static long newEventBits(E event, int mode, long oldBits) {
        long bitSetting = bitSetting(event);
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

    @HOSTED_ONLY
    public static String bitToName(long bitSetting) {
        for (int i = 0; i < bitSettings.length; i++) {
            if (bitSettings[i] == bitSetting) {
                return E.VALUES[i].name();
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
