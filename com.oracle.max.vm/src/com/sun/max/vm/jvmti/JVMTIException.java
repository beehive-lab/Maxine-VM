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

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

public class JVMTIException {

    /**
     * For communicating the data through to {@link JVMTI#event(int, Object)}.
     */
    static class ExceptionEventData {
        Throwable throwable;
        MethodID methodID;
        int location;
        MethodID catchMethodID;
        int catchLocation;
    }

    static class EventState {
        ExceptionEventData exceptionEventData = new ExceptionEventData();
        FindThrower findThrower = new FindThrower();
        VmStackFrameWalker sfw = new VmStackFrameWalker(VmThread.currentTLA());
    }

    static class EventStateThreadLocal extends ThreadLocal<EventState> {
        @Override
        public EventState initialValue() {
            return new EventState();
        }
    }

    private static EventStateThreadLocal eventStateTL = new EventStateThreadLocal();
    private static Class<?> MAXRUNTIMECALLS;

    static {
        try {
            MAXRUNTIMECALLS = Class.forName("com.oracle.max.vm.ext.maxri.MaxRuntimeCalls");
        } catch (Exception ex) {
            ProgramError.unexpected(ex);
        }
    }

    private static class FindThrower extends SourceFrameVisitor {
        ClassMethodActor methodActor;
        int bci = -1;

        @Override
        public boolean visitSourceFrame(ClassMethodActor methodActor, int bci, boolean trapped, long frameId) {
            Class<?> klass = methodActor.original().holder().toJava();
            if (klass == Throw.class || klass == MAXRUNTIMECALLS) {
                return true;
            } else {
                this.methodActor = methodActor.original();
                this.bci = bci;
                return false;
            }
        }

        void reset() {
            methodActor = null;
            bci = -1;
        }

    }

    private static boolean sendEvent() {
        if (JVMTIVmThreadLocal.bitIsSet(JVMTIVmThreadLocal.IN_UPCALL)) {
            // exception in JVMTI implementation, gets thrown to agent
            return false;
        } else {
            if (VmThread.current() == VmThread.vmOperationThread) {
                if (!JVMTI.JVMTI_VM) {
                    // unless we are debugging the VM itself, this is also fatal.
                    // we are likely in an upcall in an agent thread that submitted a VMOperation.
                    return false;
                }
            }
        }
        // send if any agent wants it
        return JVMTI.eventNeeded(JVMTIEvent.EXCEPTION);
    }

    public static void raiseEvent(Throwable throwable, Pointer sp, Pointer fp, CodePointer ip) {
        if (!sendEvent()) {
            return;
        }
        // Have to determine if this exception is caught (by the application).
        // We use the pre-allocated stack walker as per Throw.raise (which called us).
        final VmStackFrameWalker sfw = VmThread.current().unwindingStackFrameWalker(throwable);
        boolean wasDisabled = SafepointPoll.disable();
        sfw.findHandler(ip.toPointer(), sp, fp, throwable);
        if (!wasDisabled) {
            SafepointPoll.enable();
        }

        EventState eventState = eventStateTL.get();
        ExceptionEventData exceptionEventData = eventState.exceptionEventData;
        exceptionEventData.throwable = throwable;

        StackFrameCursor handlerCursor = sfw.defaultStackUnwindingContext.handlerCursor;
        // there is always a handler in Maxine but it may be unhandled by the application
        assert handlerCursor != null;

        TargetMethod ctm = handlerCursor.ip.targetMethod();
        if (ctm.classMethodActor.holder() == JVMTIThreadFunctions.vmThreadClassActor()) {
            exceptionEventData.catchMethodID = MethodID.fromWord(Word.zero());
            exceptionEventData.catchLocation = 0;
        } else {
            exceptionEventData.catchMethodID = MethodID.fromMethodActor(ctm.classMethodActor);
            exceptionEventData.catchLocation = ctm.posFor(handlerCursor.ip.vmIP());
        }

        sfw.reset();

        // ip doesn't necessarily define the throwing method, as there may be a MaxRuntimeCalls method in between.
        //
        FindThrower findThrower = eventState.findThrower;
        findThrower.reset();
        eventState.sfw.reset();
        findThrower.walk(eventState.sfw, ip.toPointer(), sp, fp);

        assert findThrower.methodActor != null;

        exceptionEventData.location = findThrower.bci;
        exceptionEventData.methodID = MethodID.fromMethodActor(findThrower.methodActor);

        JVMTI.event(JVMTIEvent.EXCEPTION, exceptionEventData);
    }
}
