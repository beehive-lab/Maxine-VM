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
package com.oracle.max.vm.ext.t1x.jvmti.amd64;

import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;

import java.util.*;

import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.amd64.*;
import com.oracle.max.vm.ext.t1x.jvmti.*;
import com.sun.cri.bytecode.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.deps.*;
import com.sun.max.vm.ext.jvmti.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.profile.*;

/**
 * Custom compilation class for generating JVMTI code-related events.
 *
 * When any JVMTI agents have requested <i>any</i> of the events that require compiled code modifications, an instance
 * of this class is used to compile a method.
 *
 * This class is capable of compiling code that is an exact match for the set of events that are needed. However, for
 * the debugging events, it can also operate in a mode where all possible events are compiled in from the
 * outset. Since single-step is one such event this effectively puts the code in interpreted mode, as the event delivery
 * code will be called on every bytecode. Evidently, it is the responsibility of the event delivery code to suppress
 * delivery of unwanted events. The remainder of the discussion assumes the strict mode.
 *
 * Prior to compilation, in {@link #initCompile}, a check is made for which kinds of events are required. This results
 * in {@link #eventSettings} having those bits sets, where the bit numbers correspond to the {@link JVMTIEvents} values.
 *
 * Some events, e.g., field access/modification events, require that modified templates be used to translate the
 * bytecode. Prior to each bytecode translation, the templates are set appropriately in {@link #setTemplates(boolean)}.
 *
 * Breakpoints are handled by checking every bytecode location against the list of set breakpoints and on a match,
 * generating the code for the breakpoint event (via a template for {@link Bytecodes#BREAKPOINT) before the code for the
 * actual bytecode (before advice essentially). There is no compelling need to recompile to remove a breakpoint, we just
 * don't deliver the event.
 *
 * TODO: Since field events are specified per field by the agent, a further optimization would be to determine whether
 * the specific field being accessed is subject to a watch.
 *
 * All per-compilation fields are reset in {@link #initCompile} so no extra {@link #cleanup() cleanup} is needed.
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
    /**
     * The breakpoints set in this method, or {@code null} if none. Sorted by location.
     */
    private long[] breakpoints;
    /**
     * The index of the next breakpoint (if any) to check for.
     */
    private int breakpointIndex;
    /**
     * {@link MethodID} for the method.
     */
    private MethodID methodID;
    /**
     * Indicates whether a bytecode has an associated event.
     */
    private BitSet eventBci;
    /**
     * The settings as a {@link JVMTIEvents} bitmask in effect for this compilation.
     */
    private long eventSettings;

    private static final JVMTIEvents.E GETFIELD_EVENT = JVMTI.eventForBytecode(Bytecodes.GETFIELD);
    private static final JVMTIEvents.E PUTFIELD_EVENT = JVMTI.eventForBytecode(Bytecodes.PUTFIELD);
    private static final JVMTIEvents.E RETURN_EVENT = JVMTI.eventForBytecode(Bytecodes.RETURN);

    private static final long DEBUG_EVENTS = JVMTIEvents.E.BREAKPOINT.bit |
                                             JVMTIEvents.E.SINGLE_STEP.bit |
                                             JVMTIEvents.E.FRAME_POP.bit |
                                             JVMTIEvents.E.EXCEPTION_CATCH.bit;

    /**
     * If {@code true}, all debugging-related events will compiled in from the get go.
     */
    private static boolean JVMTI_CDE;

    static {
        VMOptions.addFieldOption("-XX:", "JVMTI_CDE", "Compile for all debugging events");
    }

    private final T1X defaultT1X;

    public JVMTI_AMD64T1XCompilation(T1X compiler) {
        super(compiler);
        defaultT1X = T1X.stdT1X;
        templates = defaultT1X.templates;
    }

    @Override
    protected T1XTargetMethod newT1XTargetMethod(T1XCompilation comp, boolean install) {
        // if we compiled any event calls create a JVMTI_T1XTargetMethod, otherwise a vanilla one
        if (!eventBci.isEmpty()) {
            // disable the method profiler
            MethodProfile methodProfile = methodProfileBuilder.methodProfileObject();
            methodProfile.compilationDisabled = true;
            methodProfile.entryCount = Integer.MAX_VALUE;
            // register the dependency
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
        eventSettings = 0;
        eventBci = new BitSet(bciToPos.length);
        breakpoints = JVMTIBreakpoints.getBreakpoints(method);
        // N.B. Just because there are breakpoints defined, doesn't mean that
        // breakpoint events are currently enabled by the agent.
        // For simplicity, we make the connection in this code.
        if (breakpoints != null) {
            eventSettings |= JVMTIEvents.E.BREAKPOINT.bit;
        }
        breakpointIndex = 0;
        if (JVMTIBreakpoints.isSingleStepEnabled()) {
            eventSettings |= JVMTIEvents.E.SINGLE_STEP.bit;
        }
        methodID = MethodID.fromMethodActor(method);
        checkByteCodeEventNeeded(-1);  // METHOD_ENTRY
        checkByteCodeEventNeeded(-2);  // METHOD_EXIT
        checkByteCodeEventNeeded(Bytecodes.GETFIELD);
        checkByteCodeEventNeeded(Bytecodes.PUTFIELD);
        checkByteCodeEventNeeded(Bytecodes.RETURN);

        if (JVMTIEvents.isEventSet(JVMTIEvents.E.EXCEPTION_CATCH)) {
            eventSettings |= JVMTIEvents.E.EXCEPTION_CATCH.bit;
        }

        if (JVMTI_CDE) {
            // any debug events => all debug events
            if ((eventSettings & DEBUG_EVENTS) != 0) {
                eventSettings = DEBUG_EVENTS;
            }
        }
    }

    /**
     * Check whether instrumentation is needed for given bytecode, and set {@link #eventSettings} if so.
     * @param bytecode
     * @return the corresponding eventId or zero if not needed.
     */
    private void checkByteCodeEventNeeded(int bytecode) {
        JVMTIEvents.E event = JVMTI.byteCodeEventNeeded(bytecode);
        if (event != null) {
            eventSettings |= event.bit;
        }
    }

    @Override
    protected void do_methodTraceEntry() {
        if ((eventSettings & JVMTIEvents.E.METHOD_ENTRY.bit) != 0) {
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
        super.beginBytecode(opcode); // may invoke emitLoadException() if at handler
        int currentBCI = stream.currentBCI();
        long id = 0;
        boolean eventCall = false;
        boolean singleStep = (eventSettings & JVMTIEvents.E.SINGLE_STEP.bit) != 0;
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

        boolean bytecodeEvent = false;
        switch (opcode) {
            case Bytecodes.GETFIELD:
            case Bytecodes.GETSTATIC:
                bytecodeEvent = (eventSettings & GETFIELD_EVENT.bit) != 0;
                break;
            case Bytecodes.PUTFIELD:
            case Bytecodes.PUTSTATIC:
                bytecodeEvent = (eventSettings & PUTFIELD_EVENT.bit) != 0;
                break;
            case Bytecodes.IRETURN:
            case Bytecodes.LRETURN:
            case Bytecodes.FRETURN:
            case Bytecodes.DRETURN:
            case Bytecodes.ARETURN:
            case Bytecodes.RETURN:
                bytecodeEvent = ((eventSettings & RETURN_EVENT.bit) != 0) ||
                                ((eventSettings & JVMTIEvents.E.METHOD_EXIT.bit) != 0);
                break;

            default:
        }

        if (bytecodeEvent) {
            eventCall = true;
            setTemplates(eventCall);
        }

        if (eventCall) {
            eventBci.set(currentBCI);
        }
    }

    @Override
    protected void emitLoadException(int bci) {
        if ((eventSettings & JVMTIEvents.E.EXCEPTION_CATCH.bit) != 0) {
            setTemplates(true);
            eventBci.set(bci);
        }
        super.emitLoadException(bci);
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
