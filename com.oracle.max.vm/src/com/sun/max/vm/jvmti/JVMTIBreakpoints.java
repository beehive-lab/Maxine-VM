/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.jvmti.JVMTIConstants.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
//import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;

/**
 * Breakpoint function and event handling.
 *
 * Breakpoints are encoded as a 64-bit long value. The low 32 bits are from the MethodID
 * and the high 16 bits are the location in the method.
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

    private static long[] table;
    private static EventArgThreadLocal eventArg = new EventArgThreadLocal();

    /**
     * Used in T1X template, so no inlining.
     * @param id
     */
    @NEVER_INLINE
    public static void event(long id) {
        EventBreakpointID eventID = eventArg.get();
        eventID.methodID = getMethodID(id);
        eventID.location = getLocation(id);
        JVMTI.event(JVMTI_EVENT_BREAKPOINT, eventID);
    }

    static int setBreakpoint(ClassMethodActor classMethodActor, MethodID methodID, long location) {
        long id = createBreakpointID(methodID, location);
//        TargetMethod targetMethod = classMethodActor.currentTargetMethod();
        int index = tryRecordBreakpoint(id);
        if (index >= 0) {
            return JVMTI_ERROR_DUPLICATE;
        }
        return JVMTI_ERROR_NONE;
    }

    static int clearBreakpoint(ClassMethodActor classMethodActor, MethodID methodID, long location) {
        return JVMTI_ERROR_NONE;
    }

    /**
     * Return the list of breakpoints set in the given method, as a sorted array.
     * Since the low 32 bits are all the same we are effectively sorting by location.
     * @param classMethodActor
     * @return
     */
    public static long[] getBreakpoints(ClassMethodActor classMethodActor)  {
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
        long[] result = new long[count];
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
        return (int) (breakpointID >> LOCATION_SHIFT);
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
