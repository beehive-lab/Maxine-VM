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
package com.oracle.max.vm.ext.t1x.jvmti.amd64;

import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;

import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.amd64.*;
import com.oracle.max.vm.ext.t1x.jvmti.*;
import com.sun.cri.bytecode.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.deps.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.jvmti.*;

/**
 * Custom compilation class for generating JVMTI code-related events.
 *
 * When there are any JVMTI agents that have requested <i>any</i> of the events that require
 * compiled code modifications, an instance of this class is used to compile any non-VM
 * class. During the compilation each bytecode that can generate an event (including the
 * pseudo-bytecode for method entry) is checked explicitly. If the event is not needed
 * the default compiler templates and behavior are used, otherwise the
 * JVMTI templates and behavior defined here are used.
 *
 * Breakpoints are handled by checking every bytecode location against
 * the list of set breakpoints and on a match, generating the code
 * for the breakpoint event (via a template for {@link Bytecodes#BREAKPOINT)
 * before the code for the actual bytecode (before advice essentially).
 * There is no compelling need to recompile to remove a breakpoint, we just
 * don't deliver the event.
 *
 * TODO: Since field events are specified per field by the agent, a further optimization
 * would be to determine whether the specific field being accessed is subject to a watch.
 *
 */
public class JVMTI_AMD64T1XCompilation extends AMD64T1XCompilation {
    /**
     * The specific templates to be used for processing the current bytecode,
     * based on whether there are any agents registered for associated events.
     *
     * The choice is between {@link #compiler#templates} which are JVMTI enabled
     * and (@link altT1X#compiler#templates} which are the standard templates.
     */
    private T1XTemplate[] templates;
    private long[] breakpoints;
    private int breakpointIndex;
    private MethodID methodID;
    private boolean[] eventBci;
    private boolean anyEventCalls;
    private long eventSettings;
    private int doMethodEntry;
    private int doMethodExit;
    private int doFieldAccess;
    private int doFieldModification;
    private int doPopFrame;

    private final T1X defaultT1X;

    public JVMTI_AMD64T1XCompilation(T1X compiler) {
        super(compiler);
        defaultT1X = compiler.altT1X;
        templates = defaultT1X.templates;
    }

    @Override
    protected T1XTargetMethod newT1XTargetMethod(T1XCompilation comp, boolean install) {
        // if we compiled any event calls create a JVMTI_T1XTargetMethod, otherwise a vanilla one
        if (anyEventCalls) {
            Dependencies deps = JVMTI_DependencyProcessor.recordInstrumentation(method.holder(), eventSettings, breakpoints);
            assert deps != null;
            T1XTargetMethod targetMethod = new JVMTI_T1XTargetMethod(comp, install, eventBci);
            Dependencies.registerValidatedTarget(deps, targetMethod);
            return targetMethod;
        } else {
            return new T1XTargetMethod(comp, install);
        }
    }

    @Override
    protected void initCompile(ClassMethodActor method, CodeAttribute codeAttribute) {
        super.initCompile(method, codeAttribute);
        eventBci = new boolean[bciToPos.length];
        breakpoints = JVMTIBreakpoints.getBreakpoints(method);
        if (breakpoints != null) {
            eventSettings |= JVMTIEvent.bitSetting(JVMTIEvent.BREAKPOINT);
        }
        breakpointIndex = 0;
        if (JVMTIBreakpoints.isSingleStepEnabled()) {
            eventSettings |= JVMTIEvent.bitSetting(JVMTIEvent.SINGLE_STEP);
        }
        methodID = MethodID.fromMethodActor(method);
        doMethodEntry = byteCodeEventNeededAndSet(-1);
        doMethodExit = 0; // TODO
        doFieldAccess = byteCodeEventNeededAndSet(Bytecodes.GETFIELD);
        doFieldModification = byteCodeEventNeededAndSet(Bytecodes.PUTFIELD);
        doPopFrame = byteCodeEventNeededAndSet(Bytecodes.RETURN);
        // turn off recompiling to optimized code.
        if (breakpoints != null || singleStep() || doMethodEntry != 0 || doMethodExit != 0 || doFieldAccess != 0 ||
                        doFieldModification != 0 || doPopFrame  != 0) {
            methodProfileBuilder = null;
        }
    }

    /**
     * Check whether instrumentation is needed for given bytecode, and set {@link #eventSettings} if so.
     * @param bytecode
     * @return the corresponding eventId or zero if not needed.
     */
    private int byteCodeEventNeededAndSet(int bytecode) {
        int eventId = JVMTI.byteCodeEventNeeded(bytecode);
        if (eventId != 0) {
            eventSettings |= JVMTIEvent.bitSetting(eventId);
        }
        return eventId;
    }

    @Override
    protected void do_methodTraceEntry() {
        if (doMethodEntry != 0) {
            templates = compiler.templates;
            start(TRACE_METHOD_ENTRY);
            assignObject(0, "methodActor", method);
            finish();
        } else {
            super.do_methodTraceEntry();
        }
    }

    @Override
    protected void beginBytecode(int opcode) {
        super.beginBytecode(opcode);
        int currentBCI = stream.currentBCI();
        long id = 0;
        boolean eventCall = false;
        boolean singleStep = singleStep();
        boolean breakPossible = breakpoints != null && breakpointIndex < breakpoints.length;
        if (singleStep || breakPossible) {
            if (breakPossible && JVMTIBreakpoints.getLocation(breakpoints[breakpointIndex]) == currentBCI) {
                id = breakpoints[breakpointIndex++];
                if (singleStep) {
                    id |= JVMTIBreakpoints.SINGLE_STEP_AND_BREAK;
                }
            } else {
                if (singleStep) {
                    id = JVMTIBreakpoints.createSingleStepId(methodID, currentBCI);
                }
            }
            if (id != 0) {
                templates = compiler.templates;
                start(BREAKPOINT);
                assignLong(0, "id", id);
                finish();
                eventCall = true;
            }
        }

        int eventId = 0;
        switch (opcode) {
            case Bytecodes.GETFIELD:
            case Bytecodes.GETSTATIC:
                eventId = doFieldAccess;
                break;
            case Bytecodes.PUTFIELD:
            case Bytecodes.PUTSTATIC:
                eventId = doFieldModification;
                break;
            case Bytecodes.IRETURN:
            case Bytecodes.LRETURN:
            case Bytecodes.FRETURN:
            case Bytecodes.DRETURN:
            case Bytecodes.ARETURN:
            case Bytecodes.RETURN:
                eventId = doPopFrame;
                break;

            default:
        }

        if (eventId != 0) {
            eventCall = true;
            setTemplates(eventCall);
            eventSettings |= JVMTIEvent.bitSetting(eventId);
        }
        eventBci[currentBCI] = eventCall;
        if (eventCall) {
            anyEventCalls = true;
        }
    }

    private boolean singleStep() {
        return (eventSettings & JVMTIEvent.bitSetting(JVMTIEvent.SINGLE_STEP)) != 0;
    }

    private void setTemplates(boolean jvmti) {
        templates = jvmti ? compiler.templates : defaultT1X.templates;
    }

    @Override
    protected T1XTemplate getTemplate(T1XTemplateTag tag) {
        // Use the templates chosen in beginBytecode/do_methodTraceEntry
        T1XTemplate tx1Template = templates[tag.ordinal()];
        // reset to default
        templates = defaultT1X.templates;
        return tx1Template;
    }


}
