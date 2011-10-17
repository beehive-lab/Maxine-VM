/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.phases;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.cri.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.sun.cri.ri.*;

public class IntrinsificationPhase extends Phase {

    private final GraalRuntime runtime;

    public IntrinsificationPhase(GraalContext context, GraalRuntime runtime) {
        super(context);
        this.runtime = runtime;
    }

    @Override
    protected void run(Graph graph) {
        for (InvokeNode invoke : graph.getNodes(InvokeNode.class)) {
            tryIntrinsify(invoke);
        }
    }

    private void tryIntrinsify(InvokeNode invoke) {
        RiMethod target = invoke.target;
        Graph intrinsicGraph = (Graph) target.compilerStorage().get(Graph.class);
        if (intrinsicGraph == null) {
            // TODO (ph) remove once all intrinsics are available via RiMethod
            intrinsicGraph = runtime.intrinsicGraph(invoke.stateAfter().method, invoke.bci, target, invoke.arguments());
        }
        if (intrinsicGraph != null) {
            InliningUtil.inline(invoke, (CompilerGraph) intrinsicGraph, null);
        }
    }
}
