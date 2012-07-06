/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.JVMTIEvents.E;
import com.sun.max.vm.jni.*;

/**
 * Breakpoint function and event handling.
 *
 * Breakpoints are encoded as a 64-bit long value. The low 32 bits are from the MethodID
 * and the high 16 bits are the location in the method.
 *
 * Single stepping is handled as a pseudo-breakpoint that has bit {@value JVMTIBreakpoints#SINGLE_STEP} set.
 * Single stepping at a breakpoint is denoted by setting bit {@value JVMTIBreakpoints#SINGLE_STEP_AND_BREAK} as well.
 */
public class JVMTIBreakpoints {

    static class EventBreakpointID {
        long methodID;
        int location;
    }

    static class EventArgThreadLocal extends ThreadLocal<EventBreakpointID> {
        @Override
        public EventBreakpointID initialValue() {
            return new EventBreakpointID();
        }
    }

    private static final int NUMBER_OF_MEMBERID_BITS = 32;
    private static final long METHOD_ID_MASK = 0xffffffffL;
    private static final int LOCATION_SHIFT = 32;
    private static final int LOCATION_MASK = 0x0000ffff;
    private static final int DEFAULT_INITIAL_TABLE_SIZE = 16;
    private static final long UNSET = -1;
    private static final long SINGLE_STEP = 1L << 63;
    public static final long SINGLE_STEP_AND_BREAK = 1L << 62;
    private static final long ID_MASK = 0x0000ffffffffffffL;

    /**
     * Table of all breakpoints, encoded by {@link #createBreakpointID(MethodID, long)}.
     */
    private static long[] table;
    private static EventArgThreadLocal eventArg = new EventArgThreadLocal();
    private static boolean singleStep;

    private static final long[] SENTINEL = new long[0];
    /**
     * Lazily computed map of breakpoints for a specific {@link ClassMethodActor}.
     * Setting a breakpoint in the method causes a zero length array to be installed,
     * which allows a fast check on any breakpoints being set in {@link #hasBreakpoints(ClassMethodActor)}.
     * When {@link #getBreakpoints(ClassMethodActor)} is called, the actual array is filled in.
     */
    private static Map<ClassMethodActor, long[]> methodBreakpointsMap = new HashMap<ClassMethodActor, long[]>();

    /**
     * Used in T1X template and cut-off point for analysing stacks (when debugging the VM), so no inlining.
     * @param id
     */
    @JVMTI_STACKBASE
    @NEVER_INLINE
    public static void event(long id) {
        if (JVMTIVmThreadLocal.bitIsSet(JVMTIVmThreadLocal.IN_UPCALL)) {
            // already in an agent callback : VM breakpoint
            return;
        }

        // if single step and breakpoint deliver both, single step first (see spec)
        if ((id & SINGLE_STEP) != 0) {
            event(JVMTI_EVENT_SINGLE_STEP, id);
            if ((id & SINGLE_STEP_AND_BREAK) == 0) {
                return;
            }
        }
        // Since we do not aggressively recompile when a breakpoint is cleared
        // it is possible this breakpoint has been cleared and therefore the event
        // should not be delivered.
        long idMasked = id & ID_MASK;
        for (int i = 0; i < table().length; i++) {
            if (table[i] == idMasked) {
                event(JVMTI_EVENT_BREAKPOINT, id);
                return;
            }
        }
    }

    private static void event(int eventId, long id) {
        EventBreakpointID eventBptID = eventArg.get();
        eventBptID.methodID = getMethodID(id);
        eventBptID.location = getLocation(id);
        JVMTI.event(E.fromEventId(eventId), eventBptID);
    }

    static void setSingleStep(boolean setting) {
        singleStep = setting;
    }

    public static boolean isSingleStepEnabled() {
        return singleStep;
    }

    public static long createSingleStepId(MethodID methodID, int location) {
        return createBreakpointID(methodID, location) | SINGLE_STEP;
    }

    static void setBreakpoint(ClassMethodActor classMethodActor, long location) {
        int error = setBreakpoint(classMethodActor, MethodID.fromMethodActor(classMethodActor), location);
        if (error != JVMTI_ERROR_NONE) {
            throw new JJVMTI.JJVMTIException(error);
        }
    }

    static int setBreakpoint(ClassMethodActor classMethodActor, MethodID methodID, long location) {
        long id = createBreakpointID(methodID, location);
        int index = tryRecordBreakpoint(id);
        if (index < 0) {
            return JVMTI_ERROR_DUPLICATE;
        }
        methodBreakpointsMap.put(classMethodActor, SENTINEL);
        JVMTICode.deOptForNewBreakpoint(classMethodActor);
        return JVMTI_ERROR_NONE;
    }

    static void clearBreakpoint(ClassMethodActor classMethodActor, long location) {
        int error = clearBreakpoint(classMethodActor, MethodID.fromMethodActor(classMethodActor), location);
        if (error != JVMTI_ERROR_NONE) {
            throw new JJVMTI.JJVMTIException(error);
        }
    }

    static int clearBreakpoint(ClassMethodActor classMethodActor, MethodID methodID, long location) {
        long id = createBreakpointID(methodID, location);
        long m = methodID.asAddress().toLong();
        boolean found = false;
        int count = 0;
        for (int i = 0; i < table().length; i++) {
            if (table[i] == id) {
                table[i] = UNSET;
                found = true;
            } else if (getMethodID(id) == m) {
                // another bpt in same method
                count++;
            }
        }
        if (found) {
            if (count == 0) {
                // last breakpoint
                methodBreakpointsMap.put(classMethodActor, null);
                // TODO check if we should allow this to re-opt (no other events set)
            }
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_NOT_FOUND;
        }
    }

    public static boolean hasBreakpoints(ClassMethodActor classMethodActor) {
        return methodBreakpointsMap.get(classMethodActor) != null;
    }

    /**
     * Return the list of breakpoints set in the given method, as a sorted array. Since the low 32 bits are all the same
     * we are effectively sorting by location.
     *
     * @param classMethodActor
     * @return array of breakpoint info or null if there are none.
     */
    public static long[] getBreakpoints(ClassMethodActor classMethodActor) {
        long[] result = methodBreakpointsMap.get(classMethodActor);
        if (result == SENTINEL) {
            int count = 0;
            long methodID = MethodID.fromMethodActor(classMethodActor).asAddress().toLong();
            for (int i = 0; i < table().length; i++) {
                if (getMethodID(table[i]) == methodID) {
                    count++;
                }
            }
            if (count == 0) {
                return null;
            }
            result = new long[count];
            count = 0;
            for (int i = 0; i < table.length; i++) {
                if (getMethodID(table[i]) == methodID) {
                    result[count++] = table[i];
                }
            }
            switch (count) {
                case 1:
                    break;
                case 2:
                    if (result[0] > result[1]) {
                        long temp = result[0];
                        result[0] = result[1];
                        result[1] = temp;
                    }
                    break;
                default:
                    Arrays.sort(result);
            }
            methodBreakpointsMap.put(classMethodActor, result);
        }
        return result;
    }

    private static long[] table() {
        if (table == null) {
            table = new long[DEFAULT_INITIAL_TABLE_SIZE];
            Arrays.fill(table, UNSET);
        }
        return table;
    }

    private static long createBreakpointID(MethodID methodID, long location) {
        long m = methodID.asAddress().toLong();
        assert 0 <= m && m <= METHOD_ID_MASK;
        return (location << LOCATION_SHIFT) | m;
    }

    private static long getMethodID(long breakpointID) {
        return breakpointID & METHOD_ID_MASK;
    }

    public static int getLocation(long breakpointID) {
        return (int) (breakpointID >> LOCATION_SHIFT) & LOCATION_MASK;
    }

    static private int tryRecordBreakpoint(long id) {
        boolean duplicate = false;
        int firstFreeIndex = -1;
        for (int i = 0; i < table().length; i++) {
            if (table[i] == id) {
                duplicate = true;
                break;
            } else if (table[i] == UNSET && firstFreeIndex < 0) {
                firstFreeIndex = i;
                break;
            }
        }
        if (duplicate) {
            return -1;
        }
        if (firstFreeIndex < 0) {
            long[] newTable = new long[table.length * 2];
            System.arraycopy(table, 0, newTable, 0, table.length);
            firstFreeIndex = table.length;
            Arrays.fill(newTable, table.length, newTable.length - 1, UNSET);
            table = newTable;
        }
        table[firstFreeIndex] = id;
        return firstFreeIndex;
    }
}
