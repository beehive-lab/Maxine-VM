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

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jvmti.JVMTIThreadFunctions.*;
import com.sun.max.vm.thread.*;

/**
 * Everything concerned with modifications to compiled code for JVMTI code events.
 */
public class JVMTICode {

    /**
     * Called from {@link JVMTIEvent#setEventNotificationMode} to enable/disable events
     * that require compiled code support. Experimentally, these come in bunches,
     * one at a time, so we defer handling them until the thread is suspended/resumed.
     * @param eventType
     * @param mode
     * @param thread
     */
    static void codeEvent(int eventType, int mode, Thread thread) {
        if (eventType == JVMTI_EVENT_BREAKPOINT || eventType == JVMTI_EVENT_EXCEPTION) {
            // no deopt needed for these
            return;
        }
        assert thread != null; // TODO handle global code events
        // Record the requested event in the {@link JVMTIVmThreadLocal#STATE}
        JVMTIVmThreadLocal.orEventBits(VmThread.fromJava(thread).tla(), JVMTIEvent.bitSetting(eventType));
    }

    /**
     * Handle any change in compiled code for the methods on the (logical) call stack
     * necessary for the pervasive events such as SINGLE_STEP, FRAME_POP, for a single thread.
     */
    private static void checkDeOptForEvent(VmThread vmThread) {
        long codeEventSettings = codeEventSettings(vmThread);
        if (codeEventSettings != 0) {
            SingleThreadStackTraceVmOperation op = new FindAppFramesStackTraceOperation(vmThread).submitOp();
            // we only deopt the top frame, which means we need to handle leaving the frame later
            checkDeOptForMethod(op.stackTraceVisitor.getStackElement(0).classMethodActor, codeEventSettings);
        } else {
            // is it worth reopting? perhaps if we are resuming without, say, single step set and
            // the code contains single step event calls. They won't be delivered but they reduce
            // performance noticeably.
        }
    }

    static long codeEventSettings(VmThread vmThread) {
        return JVMTIVmThreadLocal.getEventBits(vmThread.tla()) & JVMTIEvent.CODE_EVENTS_SETTING;
    }

    static void checkDeOptForMethod(ClassMethodActor classMethodActor, long codeEventSettings) {
        TargetMethod targetMethod = classMethodActor.currentTargetMethod();
        // we check here if the code is already adequate for the settings we want
        if (targetMethod.jvmtiCheck(codeEventSettings, JVMTIBreakpoints.getBreakpoints(targetMethod.classMethodActor))) {
            return;
        }
        ArrayList<TargetMethod> targetMethods = new ArrayList<TargetMethod>();
        targetMethod.finalizeReferenceMaps();
        targetMethods.add(targetMethod);
        // Calling this multiple times for different threads is harmless as it takes care to
        // filter out already invalidated methods.
        new Deoptimization(targetMethods).go();
    }

    /**
     * A new breakpoint is being set in code that is already compiled.
     * @param classMethodActor
     */
    static void deOptForBreakpoint(ClassMethodActor classMethodActor) {
        // Potentially need to deopt the code in all threads, but note that it
        // may not be active on any thread stack, in which case we just need to recompile
        // and patch call sites. Deopt takes care of it either way.
        ArrayList<TargetMethod> targetMethods = new ArrayList<TargetMethod>();
        targetMethods.add(classMethodActor.currentTargetMethod());
        new Deoptimization(targetMethods).go();
    }

    static void resumeThreadNotify(VmThread vmThread) {
        checkDeOptForEvent(vmThread);
    }

    static void suspendThreadNotify(VmThread vmThread) {
        JVMTIVmThreadLocal.clearEventBits(vmThread.tla());
    }

    static void resumeThreadListNotify(Set<VmThread> set) {
        for (VmThread vmThread : set) {
            resumeThreadNotify(vmThread);
        }
    }

    static void suspendThreadListNotify(Set<VmThread> set) {
        for (VmThread vmThread : set) {
            suspendThreadNotify(vmThread);
        }
    }
}
