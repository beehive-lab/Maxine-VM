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
package com.sun.max.vm.compiler.ir.interpreter;

import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.profile.TreeAnchor;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.tir.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.hotpath.compiler.*;
import com.sun.max.vm.hotpath.compiler.Console.*;
import com.sun.max.vm.hotpath.state.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class BirTracer extends Tracer {

    public static OptionSet birTracerOptionSet = new OptionSet();
    public static Option<Boolean> interpretEir = birTracerOptionSet.newBooleanOption("EIR", false, "Interpret EIR");

    private static BirTracer tracer = new BirTracer();

    public static BirTracer current() {
        return tracer;
    }

    /**
     * Visits a {@link TreeAnchor}.
     * @return {@code true} if tracing should commence / continue, or {@code false} otherwise.
     */
    public boolean visitAnchor(TreeAnchor anchor, State<Value> state) {
        anchor.setStackHeight(state.last().stackHeight());
        visitAnchor(anchor);
        return isTracing();
    }

    /**
     * Visits an invoke.
     * @return {@code true} if tracing should commence / continue, or {@code false} otherwise.
     */
    public boolean visitInvoke(ClassMethodActor target, BirState state) {
        return true;
    }

    /**
     * Visits a bytecode instruction.
     * @return {@code true} of tracing should continue, or {@code false} otherwise.
     */
    public boolean visitBytecode(BytecodeLocation location, BirState state) {
        assert MaxineVM.isPrototyping();
        assert isTracing();
        saveState(state);
        visitBytecode(location);
        return isTracing();
    }

    private BirState state;

    private void saveState(BirState state) {
        this.state = state;
    }

    @Override
    protected Bailout evaluateTree(TirTree tree) {
        if (printState.getValue()) {
            Console.print(Color.YELLOW, "EXECUTING  TREE:  " + NameMap.nameOf(tree) + " ");
            state.println();
        }

        TirGuard guard = null;

        if (interpretEir.getValue()) {
            final AMD64TreeEirInterpreter interpreter = new AMD64TreeEirInterpreter();
            guard = interpreter.execute(tree, state);
        } else {
            final TirInterpreter interpreter = new TirInterpreter();
            guard = interpreter.execute(tree, state);
        }

        if (printState.getValue()) {
            Console.print(Color.YELLOW, "EXITED AT GUARD: #" + tree.getNumber(guard) + " ");
            state.println();
        }
        return new Bailout(guard);
    }

    @Override
    protected boolean evaluateAcmpBranch(BranchCondition condition) {
        final ReferenceValue b = (ReferenceValue) state.peek(Kind.REFERENCE);
        final ReferenceValue a = (ReferenceValue) state.peek(Kind.REFERENCE, 1);
        return condition.evaluate(a, b);
    }

    @Override
    protected boolean evaluateBranch(BranchCondition condition) {
        final Value a = state.peek(Kind.INT);
        return condition.evaluate(a, IntValue.ZERO);
    }

    @Override
    protected boolean evaluateIcmpBranch(BranchCondition condition) {
        final Value b = state.peek(Kind.INT);
        final Value a = state.peek(Kind.INT, 1);
        return condition.evaluate(a, b);
    }

    @Override
    protected boolean evaluateNullBranch(BranchCondition condition) {
        final ReferenceValue a = (ReferenceValue) state.peek(Kind.REFERENCE);
        final ReferenceValue b = ReferenceValue.NULL;
        return condition.evaluate(a, b);
    }

    @Override
    protected Object evaluateObject(int stackDepth) {
        return state.peek(Kind.REFERENCE, stackDepth).asObject();
    }

    @Override
    protected int evaluateInt(int stackDepth) {
        return state.peek(Kind.INT, stackDepth).asInt();
    }
}
