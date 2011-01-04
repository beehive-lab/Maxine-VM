/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.transform;

import static com.sun.max.vm.cps.cir.CirTraceObserver.TransformationType.*;

import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.GetIntegerRegister;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.transform.CirDepthFirstTraversal.DefaultBlockSet;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.cps.template.*;
import com.sun.max.vm.runtime.VMRegister.Role;

/**
 * Tests the CIR for a {@linkplain BYTECODE_TEMPLATE bytecode template} to ensure it does not write to a Java stack or
 * local slot before any {@linkplain Stop stop} instruction. A stop instruction is any point where the execution of the
 * code may be stopped (such as a safepoint or call) and have its execution state (i.e. the values of the Java stack and
 * locals) inspected.
 *
 * The invariant tested for by this class simplifies root scanning of a frame compiled by a template based JIT. If the
 * invariant holds for all templates, then a reference map derived from the Java state at the start of a bytecode
 * template will be valid for all execution paths up until the last stop on the path. After that point, the state is not
 * required for a root scanner as it can never be stopped at.
 *
 * @author Doug Simon
 */
@HOSTED_ONLY
public final class CirTemplateChecker extends CirVisitor {

    private final MethodActor classMethodActor;

    /**
     * Maps a closure/block/call to a stop instruction reachable from the node.
     */
    private final HashMap<CirNode, CirCall> stops = new HashMap<CirNode, CirCall>();

    private boolean changed;

    private CirTemplateChecker(MethodActor classMethodActor) {
        this.classMethodActor = classMethodActor;
    }

    /**
     * Reports a violation of the invariant tested by this class. The error message trys to give useful source position
     * information regarding the pair of instructions that violate the invariant. However, when the necessary bytecode
     * mappings are missing, it may be a little misleading.
     *
     * @param call a call representing an instruction that updates the Java stack or locals array
     * @param stop a call representing a stop instruction
     */
    private void reportError(CirCall call, CirCall stop) {
        final StringBuilder sb = new StringBuilder("In a bytecode template, the Java stack or locals array must not be updated before a safepoint or call.\n");
        sb.append("[in " + classMethodActor.format("%H.%n(%p)") + " ]\n");
        sb.append("Stack/local update: " + call.procedure() + "(" + Utils.toString(call.arguments(), ", ") + ")\n");
        sb.append("    at " + call.javaFrameDescriptor().toStackTraceElement() + "\n");
        sb.append("Safepoint/call:     " + stop.procedure() + "(" + Utils.toString(stop.arguments(), ", ") + ")\n");
        sb.append("    at " + stop.javaFrameDescriptor().toStackTraceElement() + "\n");
        sb.append("Stop reason(s): " + Stoppable.Static.reasonsMayStopToString((Stoppable) stop.procedure()));
        ProgramError.unexpected(sb.toString());
    }

    @Override
    public void visitBlock(CirBlock block) {
        CirCall stop = stops.get(block);
        if (stop == null) {
            stop = stops.get(block.closure());
            if (stop != null) {
                changed = true;
                stops.put(block, stop);
            }
        }
    }

    @Override
    public void visitClosure(CirClosure cirClosure) {
        CirCall stop = stops.get(cirClosure);
        if (stop == null) {
            stop = stops.get(cirClosure.body());
            if (stop != null) {
                stops.put(cirClosure, stop);
            }
        }
    }

    /**
     * Determines if a given CIR call represents an instruction that updates the Java stack or locals array.
     */
    private boolean updatesJavaFrameOrStack(CirCall call) {
        final CirValue procedure = call.procedure();
        if (procedure instanceof CirRoutine) {
            final CirRoutine routine = (CirRoutine) procedure;
            if (routine instanceof CirBuiltin) {
                final CirBuiltin cirBuiltin = (CirBuiltin) routine;
                final Builtin builtin = cirBuiltin.builtin;
                if (builtin == GetIntegerRegister.BUILTIN) {
                    final CirValue[] arguments = call.arguments();
                    final Role role = (Role) arguments[0].value().asObject();
                    if (role == Role.ABI_STACK_POINTER || role == Role.ABI_FRAME_POINTER || role == Role.CPU_STACK_POINTER || role == Role.CPU_FRAME_POINTER) {
                        final CirContinuation cont = (CirContinuation) arguments[1];
                        final CirValue nextCall = cont.body().procedure();
                        if (nextCall instanceof CirBuiltin) {
                            final CirBuiltin nextCirBuiltin = (CirBuiltin) nextCall;
                            if (nextCirBuiltin.builtin instanceof PointerStoreBuiltin) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Determines if a given CIR call represents a stop instruction.
     */
    private boolean isStop(CirCall call) {
        final CirValue procedure = call.procedure();
        if (procedure instanceof CirRoutine) {
            final CirRoutine routine = (CirRoutine) procedure;
            return Stoppable.Static.canStop(routine);
        }
        return false;
    }

    @Override
    public void visitCall(CirCall call) {
        final CirValue procedure = call.procedure();
        final CirValue[] arguments = call.arguments();

        CirCall stop = stops.get(call);
        if (stop == null) {
            stop = stops.get(procedure);
            if (stop == null) {
                for (CirValue argument : arguments) {
                    stop = stops.get(argument);
                    if (stop != null) {
                        stops.put(call, stop);
                        break;
                    }
                }
            }
            if (stop != null) {
                stops.put(call, stop);
            }
        }

        if (stop != null) {
            if (updatesJavaFrameOrStack(call)) {
                reportError(call, stop);
            }
        } else {
            if (isStop(call)) {
                stops.put(call, call);
            }
        }
    }

    public static void apply(CirGenerator cirGenerator, CirMethod cirMethod, final CirClosure closure) {
        cirGenerator.notifyBeforeTransformation(cirMethod, cirMethod, TEMPLATE_CHECKING);
        final DefaultBlockSet blockSet = new DefaultBlockSet();
        final CirDepthFirstTraversal depthFirstTraversal = new CirDepthFirstTraversal(blockSet);
        final CirTemplateChecker checker = new CirTemplateChecker(cirMethod.classMethodActor());
        do {
            checker.changed = false;
            depthFirstTraversal.run(closure, checker);
            blockSet.clear();
        } while (checker.changed);
        cirGenerator.notifyAfterTransformation(cirMethod, cirMethod, TEMPLATE_CHECKING);
    }
}
