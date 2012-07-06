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

import static com.sun.max.vm.ext.jvmti.JVMTIEvents.*;
import static com.sun.max.vm.ext.jvmti.JVMTIVmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.ext.jvmti.JVMTIThreadFunctions.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

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
        StackAnalyzer stackAnalyser = new StackAnalyzer();
        VmStackFrameWalker sfw = new VmStackFrameWalker(VmThread.currentTLA());
        boolean inProcess;
    }

    static class EventStateThreadLocal extends ThreadLocal<EventState> {
        @Override
        public EventState initialValue() {
            return new EventState();
        }
    }

    /**
     * A pre-thread value that holds the state necessary to analyze and dispatch and exception event (and it's catch if any).
     */
    private static EventStateThreadLocal eventStateTL = new EventStateThreadLocal();

    /**
     * VM implementation methods that may be on the stack above the method that actually threw the exception.
     * This is rather ad hoc and compiler dependent, unfortunately.
     */
    private static final MethodActor[] throwImplMethods = new ClassMethodActor[5];

    @CONSTANT_WHEN_NOT_ZERO
    private static ClassActor throwClassActor;

    @HOSTED_ONLY
    private static class InitializationCompleteCallback implements JavaPrototype.InitializationCompleteCallback {

        @Override
        public void initializationComplete() {
            try {
                throwClassActor = ClassActor.fromJava(Throw.class);
                int i = 0;
                throwImplMethods[i++] = MethodActor.fromJava(VmThread.class.getDeclaredMethod("throwJniException"));
                Class<?> maxRuntimeCalls = Class.forName("com.oracle.max.vm.ext.maxri.MaxRuntimeCalls");
                throwImplMethods[i++] = MethodActor.fromJava(maxRuntimeCalls.getDeclaredMethod("runtimeHandleException", Throwable.class));
                throwImplMethods[i++] = MethodActor.fromJava(maxRuntimeCalls.getDeclaredMethod("runtimeUnwindException", Throwable.class));
            } catch (Exception ex) {
                ProgramError.unexpected(ex);
            }
        }
    }

    static {
        JavaPrototype.registerInitializationCompleteCallback(new InitializationCompleteCallback());
    }

    /**
     * A variant of {@link FindAppFramesStackTraceVisitor} that locates the
     * actual frame that threw the exception and whether or not the exception was
     * caught. Owing to the meta-circular nature of Maxine, neither question
     * is entirely trivial to answer.
     *
     */
    private static class StackAnalyzer extends FindAppFramesStackTraceVisitor {
        Throwable throwable;
        ClassMethodActor throwingMethodActor;
        TargetMethod catchTargetMethod;
        int stackElementSizeAtCatch;
        int throwingBci = -1;
        TargetMethod.CatchExceptionInfo catchInfo = new TargetMethod.CatchExceptionInfo();

        @Override
        public boolean visitSourceFrame(ClassMethodActor methodActor, int bci, boolean trapped, long frameId) {
            if (throwingMethodActor == null) {
                ClassMethodActor methodActorOriginal = methodActor.original();
                if (!(methodActorOriginal.holder() == throwClassActor || isThrowImplMethod(methodActor))) {
                    this.throwingMethodActor = methodActorOriginal;
                    this.throwingBci = bci;
                }
            }
            return super.visitSourceFrame(methodActor, bci, trapped, frameId);
        }

        private boolean isThrowImplMethod(ClassMethodActor methodActor) {
            for (int i = 0; i < throwImplMethods.length; i++) {
                if (methodActor == throwImplMethods[i]) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean visitFrame(StackFrameCursor current, StackFrameCursor callee) {
            // raw frame visit with the info we need to check for a catch of throwable
            if (catchTargetMethod == null) {
                TargetMethod tm = current.targetMethod();
                if (tm != null) {
                    boolean isCaught = tm.catchExceptionInfo(current, throwable, catchInfo);
                    // The way Maxine handles synchronized methods introduces an additional catch (Throwable)
                    // which results in a negative bci value for the handler (since there is no user written
                    // handler in the method). So we ignore this one and keep looking.
                    if (isCaught && catchInfo.bci >= 0) {
                        stackElementSizeAtCatch = stackElements.size() + 1; // this frame is added in the super call
                        catchTargetMethod = tm;
                    }
                }
            }
            return super.visitFrame(current, callee);
        }

        void walk(StackFrameWalker walker, Pointer ip, Pointer sp, Pointer fp, Throwable throwable) {
            walker.reset();
            this.throwable = throwable;
            walkRaw(walker, ip, sp, fp);
        }

        /**
         * Resets state if the visitor is being reused.
         */
        @Override
        void reset() {
            super.reset();
            throwable = null;
            throwingMethodActor = null;
            catchTargetMethod = null;
            throwingBci = -1;
            stackElementSizeAtCatch = 0;
        }

        @NEVER_INLINE
        boolean uncaughtByApplication() {
            if (JVMTI.JVMTI_VM) {
                return false;
            }
            int catchCallerIndex = stackElements.size() - stackElementSizeAtCatch;
            for (int i = 0; i < catchCallerIndex; i++) {
                ClassMethodActor classMethodActor = stackElements.get(i).classMethodActor;
                if (!isVmStartup(classMethodActor)) {
                    // application class lower than handler so its's caught
                    return false;
                }
            }
            return true;
        }

        private boolean isVmStartup(ClassMethodActor classMethodActor) {
            ClassActor holder = classMethodActor.holder();
            return holder.classLoader == VMClassLoader.VM_CLASS_LOADER ||
                   holder == JVMTIThreadFunctions.methodClassActor();
        }

        boolean thrownInVmStartup() {
            if (JVMTI.JVMTI_VM) {
                return false;
            }
            for (int i = 0; i < stackElements.size(); i++) {
                ClassMethodActor classMethodActor = stackElements.get(i).classMethodActor;
                // check for non-VM class, but not the invocation stub for main which rethrows an unhandled exception
                if (!(isVmStartup(classMethodActor) || classMethodActor.holder().isReflectionStub())) {
                    // genuine app class
                    return false;
                }
            }
            // no app classes, in VM startup
            return true;

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
        return JVMTI.eventNeeded(E.EXCEPTION) || JVMTI.eventNeeded(E.EXCEPTION_CATCH)  ||
                  JVMTI.eventNeeded(E.METHOD_EXIT) ||
                  JVMTIVmThreadLocal.bitIsSet(VmThread.currentTLA(), JVMTI_FRAME_POP) || vmaHandler != null;
    }

    /**
     * Used by {@link EXCEPTION_CATCH} events to access the information need to dispatch the event.
     */
    static ExceptionEventData getExceptionEventData() {
        return eventStateTL.get().exceptionEventData;
    }

    public static void raiseEvent(Throwable throwable, Pointer sp, Pointer fp, CodePointer ip) {
        EventState eventState = eventStateTL.get();
        try {
            if (eventState.inProcess) {
                FatalError.unexpected("jvmti: exception while analyzing exception", throwable);
            }
            eventState.inProcess = true;

            if (!sendEvent()) {
                return;
            }
            ExceptionEventData exceptionEventData = eventState.exceptionEventData;
            exceptionEventData.throwable = throwable;

            StackAnalyzer stackAnalyser = eventState.stackAnalyser;
            stackAnalyser.reset();
            stackAnalyser.walk(eventState.sfw, ip.toPointer(), sp, fp, throwable);

            // There is always a handler in Maxine but it may be unhandled by the application
            // and deciding this requires some detective work. It may also have been rethrown in the
            // VM startup sequence and we want to ignore that (since we previously decided it was uncaught)
            if (stackAnalyser.thrownInVmStartup()) {
                return;
            }
            TargetMethod ctm = stackAnalyser.catchTargetMethod;
            assert ctm != null;
            boolean uncaught = stackAnalyser.uncaughtByApplication();
            if (uncaught) {
                exceptionEventData.catchMethodID = MethodID.fromWord(Word.zero());
                exceptionEventData.catchLocation = 0;
            } else {
                exceptionEventData.catchMethodID = MethodID.fromMethodActor(ctm.classMethodActor);
                exceptionEventData.catchLocation = stackAnalyser.catchInfo.bci;
            }

            assert stackAnalyser.throwingMethodActor != null;

            exceptionEventData.location = stackAnalyser.throwingBci;
            exceptionEventData.methodID = MethodID.fromMethodActor(stackAnalyser.throwingMethodActor);

            if (JVMTI.eventNeeded(JVMTIEvents.E.EXCEPTION)) {
                JVMTI.event(JVMTIEvents.E.EXCEPTION, exceptionEventData);
            }

            if (!uncaught) {
                // send a POP_FRAME unless caught in throwing method
                if (ctm.classMethodActor != stackAnalyser.throwingMethodActor) {
                    FramePopEventData framePopEventData = JVMTIThreadFunctions.getFramePopEventData(exceptionEventData.methodID, true, null);
                    if (JVMTIVmThreadLocal.bitIsSet(VmThread.currentTLA(), JVMTI_FRAME_POP)) {
                        JVMTI.event(E.FRAME_POP, framePopEventData);
                    }

                    if (JVMTIEvents.isEventSet(E.METHOD_EXIT)) {
                        JVMTI.event(E.METHOD_EXIT, framePopEventData);
                    }
                    if (vmaHandler != null) {
                        vmaHandler.exceptionRaised(stackAnalyser.throwingMethodActor, throwable, stackAnalyser.stackElementSizeAtCatch - 1);
                    }
                }
                // if an agent wants the exception catch event, we need to ensure that the compiled code for the catch method
                // has that capability, or deopt it and generate the relevant event code
                if (JVMTI.eventNeeded(JVMTIEvents.E.EXCEPTION_CATCH)) {
                    JVMTICode.checkDeOptForMethod(ctm.classMethodActor, JVMTIEvents.codeEventSettings(null, VmThread.current()));
                }
            }
        } finally {
            eventState.inProcess = false;
        }
    }

    // VMA support

    public interface VMAHandler {
        void exceptionRaised(ClassMethodActor throwingActor, Throwable throwable, int poppedFrameCount);
    }

    private static VMAHandler vmaHandler;

    public static void registerVMAHAndler(VMAHandler handler) {
        vmaHandler = handler;
    }

}
