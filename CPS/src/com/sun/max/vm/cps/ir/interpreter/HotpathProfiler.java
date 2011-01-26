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
package com.sun.max.vm.cps.ir.interpreter;

import com.sun.max.collect.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.hotpath.compiler.Console.*;
import com.sun.max.vm.cps.hotpath.state.*;
import com.sun.max.vm.cps.ir.interpreter.BirInterpreter.*;
import com.sun.max.vm.value.*;

public class HotpathProfiler implements Profiler {

    public static OptionSet optionSet = new OptionSet();
    public static Option<Boolean> printState = optionSet.newBooleanOption("PP", false, "(P)rints the Profiler's Execution (S)tate.");

    private Mapping<BytecodeLocation, TreeAnchor> anchors = new OpenAddressingHashMapping<BytecodeLocation, TreeAnchor>();

    private boolean isTracing = false;

    public void jump(BytecodeLocation fromlocation, BytecodeLocation toLocation, BirState state) {
        assert fromlocation.classMethodActor == toLocation.classMethodActor;
        if (toLocation.bytecodePosition < fromlocation.bytecodePosition) {
            backwardJump(fromlocation, toLocation, state);
        }
    }

    public void trace(BytecodeLocation location, BirState state) {
        if (isTracing) {
            isTracing = BirTracer.current().visitBytecode(location, state);
            if (printState.getValue()) {
                Console.println(Color.LIGHTRED, "tracing loc: " + location.toString());
            }
        }
    }

    public void invoke(ClassMethodActor target, BirState state) {
        if (isTracing) {
            isTracing = BirTracer.current().visitInvoke(target, state);
        }
    }

    private void backwardJump(BytecodeLocation fromlocation, BytecodeLocation toLocation, State<Value> state) {
        if (state.last().isEmpty() == false) {
            // Only record traces at anchors that have empty stacks. This should almost always be the case for java,
            // however due to bytecode rewriting for exception handlers this is not the case in Maxine.
            return;
        }

        final TreeAnchor hotpathAnchor = lookupAnchor(toLocation);
        final boolean isTracing = BirTracer.current().visitAnchor(hotpathAnchor, state);
        if (isTracing) {
            this.isTracing = true;
            if (printState.getValue()) {
                Console.println(Color.RED, "tracing starting at: " + hotpathAnchor);
            }
        } else {
            if (this.isTracing == true) {
                this.isTracing = false;
                if (printState.getValue()) {
                    Console.println(Color.RED, "tracing stopping at: " + hotpathAnchor);
                }
            }
        }
    }

    private TreeAnchor lookupAnchor(BytecodeLocation location) {
        TreeAnchor hotpathAnchor = anchors.get(location);
        if (hotpathAnchor == null) {
            hotpathAnchor = new TreeAnchor(location, 5);
            anchors.put(location, hotpathAnchor);
        }
        return hotpathAnchor;
    }
}
