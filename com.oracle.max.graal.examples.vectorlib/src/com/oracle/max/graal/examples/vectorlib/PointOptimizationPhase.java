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
package com.oracle.max.graal.examples.vectorlib;

import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;


public class PointOptimizationPhase extends Phase {
    @Override
    protected void run(Graph<EntryPointNode> graph) {
        for (InvokeNode invoke : graph.getNodes(InvokeNode.class)) {
            System.out.println("Invoke: " + invoke.target.name());
            if (invoke.target.name().equals("same")) {
                ValueNode arg0 = invoke.arguments().get(0);
                ValueNode arg1 = invoke.arguments().get(1);
                if (arg0 == arg1) {
                    invoke.replaceWithFloating(ConstantNode.forBoolean(true, graph));
                }
            }
        }
    }
}
