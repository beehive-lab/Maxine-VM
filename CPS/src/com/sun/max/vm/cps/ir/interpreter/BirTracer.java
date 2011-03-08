/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir.interpreter;

import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.hotpath.compiler.Console.*;
import com.sun.max.vm.cps.hotpath.state.*;
import com.sun.max.vm.cps.tir.*;
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
        assert MaxineVM.isHosted();
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
