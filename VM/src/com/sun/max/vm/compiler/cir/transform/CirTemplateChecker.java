/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.compiler.cir.transform;

import static com.sun.max.vm.compiler.cir.CirTraceObserver.Transformation.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.transform.CirDepthFirstTraversal.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.runtime.VMRegister.*;

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
@PROTOTYPE_ONLY
public final class CirTemplateChecker extends CirVisitor {

    private final MethodActor _classMethodActor;

    /**
     * Maps a closure/block/call to a stop instruction reachable from the node.
     */
    private final VariableMapping<CirNode, CirCall> _stops = new ChainedHashMapping<CirNode, CirCall>();

    private boolean _changed;

    private CirTemplateChecker(MethodActor classMethodActor) {
        _classMethodActor = classMethodActor;
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
        sb.append("[in " + _classMethodActor.format("%H.%n(%p)") + " ]\n");
        sb.append("Stack/local update: " + call.procedure() + "(" + Arrays.toString(call.arguments(), ", ") + ")\n");
        sb.append("    at " + call.bytecodeLocation().toSourcePositionString() + "\n");
        sb.append("Safepoint/call:     " + stop.procedure() + "(" + Arrays.toString(stop.arguments(), ", ") + ")\n");
        sb.append("    at " + stop.bytecodeLocation().toSourcePositionString());
        ProgramError.unexpected(sb.toString());
    }

    @Override
    public void visitBlock(CirBlock block) {
        CirCall stop = _stops.get(block);
        if (stop == null) {
            stop = _stops.get(block.closure());
            if (stop != null) {
                _changed = true;
                _stops.put(block, stop);
            }
        }
    }

    @Override
    public void visitClosure(CirClosure cirClosure) {
        CirCall stop = _stops.get(cirClosure);
        if (stop == null) {
            stop = _stops.get(cirClosure.body());
            if (stop != null) {
                _stops.put(cirClosure, stop);
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
                final Builtin builtin = cirBuiltin.builtin();
                if (builtin == SpecialBuiltin.Push.BUILTIN || builtin == SpecialBuiltin.Pop.BUILTIN) {
                    return true;
                }
                if (builtin == GetIntegerRegister.BUILTIN) {
                    final CirValue[] arguments = call.arguments();
                    final Role role = (Role) arguments[0].value().asObject();
                    if (role == Role.ABI_STACK_POINTER || role == Role.ABI_FRAME_POINTER || role == Role.CPU_STACK_POINTER || role == Role.CPU_FRAME_POINTER) {
                        final CirContinuation cont = (CirContinuation) arguments[1];
                        final CirValue nextCall = cont.body().procedure();
                        if (nextCall instanceof CirBuiltin) {
                            final CirBuiltin nextCirBuiltin = (CirBuiltin) nextCall;
                            if (nextCirBuiltin.builtin() instanceof PointerStoreBuiltin) {
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
            return routine.needsJavaFrameDescriptor();
        }
        return false;
    }

    @Override
    public void visitCall(CirCall call) {
        final CirValue procedure = call.procedure();
        final CirValue[] arguments = call.arguments();

        CirCall stop = _stops.get(call);
        if (stop == null) {
            stop = _stops.get(procedure);
            if (stop == null) {
                for (CirValue argument : arguments) {
                    stop = _stops.get(argument);
                    if (stop != null) {
                        _stops.put(call, stop);
                        break;
                    }
                }
            }
            if (stop != null) {
                _stops.put(call, stop);
            }
        }

        if (stop != null) {
            if (updatesJavaFrameOrStack(call)) {
                reportError(call, stop);
            }
        } else {
            if (isStop(call)) {
                _stops.put(call, call);
            }
        }
    }

    public static void apply(CirGenerator cirGenerator, CirMethod cirMethod, final CirClosure closure) {
        cirGenerator.notifyBeforeTransformation(cirMethod, cirMethod, TEMPLATE_CHECKING);
        final DefaultBlockSet blockSet = new DefaultBlockSet();
        final CirDepthFirstTraversal depthFirstTraversal = new CirDepthFirstTraversal(blockSet);
        final CirTemplateChecker checker = new CirTemplateChecker(cirMethod.classMethodActor());
        do {
            checker._changed = false;
            depthFirstTraversal.run(closure, checker);
            blockSet.clear();
        } while (checker._changed);
        cirGenerator.notifyAfterTransformation(cirMethod, cirMethod, TEMPLATE_CHECKING);
    }
}
