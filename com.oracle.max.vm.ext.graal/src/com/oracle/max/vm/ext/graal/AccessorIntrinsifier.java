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
package com.oracle.max.vm.ext.graal;

import java.util.*;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.extensions.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.extended.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.unsafe.*;


/**
 *
 */
public class AccessorIntrinsifier implements Intrinsifier {

    @Override
    public Graph<?> intrinsicGraph(RiRuntime runtime, CiCodePos callerPos, RiResolvedMethod method, List< ? extends Node> parameters) {
        if (method.holder().equals(runtime.getType(Accessor.class))) {
            CiCodePos pos = callerPos;
            while (pos != null) {
                RiResolvedType accessor = pos.method.accessor();
                if (accessor != null) {
                    RiResolvedMethod accessorMethod = accessor.resolveMethodImpl(method);

                    // TODO (gd) move this to a graph buidling utility when GBP is moved to its own project
                    Graph<EntryPointNode> graph = GraphBuilderPhase.cachedGraphs.get(accessorMethod);
                    if (graph != null) {
                        Graph<EntryPointNode> duplicate = new Graph<EntryPointNode>(new EntryPointNode(null));
                        Map<Node, Node> replacements = new IdentityHashMap<Node, Node>();
                        replacements.put(graph.start(), duplicate.start());
                        duplicate.addDuplicate(graph.getNodes(), replacements);
                        graph = duplicate;
                    } else {
                        graph = new Graph<EntryPointNode>(new EntryPointNode(runtime));
                        new GraphBuilderPhase(GraalContext.EMPTY_CONTEXT, runtime, accessorMethod, null).apply(graph, true, false);
                        if (GraalOptions.ProbabilityAnalysis) {
                            new DeadCodeEliminationPhase(GraalContext.EMPTY_CONTEXT).apply(graph, true, false);
                            new ComputeProbabilityPhase(GraalContext.EMPTY_CONTEXT).apply(graph, true, false);
                        }
                    }

                    for (LocalNode l : graph.getNodes(LocalNode.class)) {
                        if (l.index() == 0) {
                            UnsafeCastNode cast = graph.add(new UnsafeCastNode(l, accessor));
                            l.replaceAtUsages(cast);
                            break;
                        }
                    }
                    return graph;
                }
                pos = pos.caller;
            }
        }
        return null;
    }

}
