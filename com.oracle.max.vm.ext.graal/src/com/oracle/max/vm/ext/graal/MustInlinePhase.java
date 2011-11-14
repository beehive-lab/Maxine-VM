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

import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.actor.member.*;


public class MustInlinePhase extends Phase {

    private final MaxRuntime runtime;
    private final RiResolvedType accessor;
    private final Map<RiMethod, StructuredGraph> cache;

    public MustInlinePhase(MaxRuntime runtime, Map<RiMethod, StructuredGraph> cache, RiResolvedType accessor) {
        this.runtime = runtime;
        this.cache = cache;
        this.accessor = accessor;
    }

    @Override
    protected void run(StructuredGraph graph) {
        for (MethodCallTargetNode callTarget : graph.getNodes(MethodCallTargetNode.class)) {
            Invoke invoke = callTarget.invoke();
            RiResolvedMethod method = callTarget.targetMethod();
            if (invoke != null) {
                assert ((MethodActor) method).intrinsic() == null : "Intrinsics must be resolved before this phase " + ((MethodActor) method).intrinsic();
                if (runtime.mustInline(method)) {
                    StructuredGraph inlineGraph = cache.get(method);
                    if (inlineGraph == null) {
                        inlineGraph = new StructuredGraph();
                        new GraphBuilderPhase(runtime, method).apply(inlineGraph);
                        new PhiSimplificationPhase().apply(inlineGraph);
                        RiResolvedType curAccessor = getAccessor(method, accessor);
                        if (curAccessor != null) {
                            new AccessorPhase(runtime, curAccessor).apply(inlineGraph);
                        }
                        new FoldPhase(runtime).apply(inlineGraph);
                        new MaxineIntrinsicsPhase(runtime).apply(inlineGraph);
                        new MustInlinePhase(runtime, cache, curAccessor).apply(inlineGraph, context);
                        cache.put(method, inlineGraph);
                    }
                    InliningUtil.inline(invoke, inlineGraph, false);
                }
            }
        }
    }

    private RiResolvedType getAccessor(RiResolvedMethod method, RiResolvedType accessor) {
        if (method.accessor() != null) {
            return method.accessor();
        }
        return accessor;
    }
}
