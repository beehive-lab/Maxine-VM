/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal.snippets;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.util.*;
import com.oracle.max.vm.ext.graal.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;

/**
 * Rewrites {@link InvokeNode}s to slow path runtime methods in Maxine snippets to {@link RuntimeCall} nodes.
 * Ideally, this would not be necessary
 */
public class MaxSlowpathRewriterPhase extends Phase {

    private final MetaAccessProvider runtime;

    public MaxSlowpathRewriterPhase(MetaAccessProvider metaAccess) {
        this.runtime = metaAccess;
    }

    @Override
    protected void run(StructuredGraph graph) {
        for (Node n : GraphOrder.forwardGraph(graph)) {
            if (n instanceof Invoke) {
                Invoke invoke = (Invoke) n;
                MethodCallTargetNode callTarget = invoke.methodCallTarget();
                ClassMethodActor cma = (ClassMethodActor) MaxJavaMethod.getRiMethod(callTarget.targetMethod());
                MaxRuntimeCall call = MaxRuntimeCallsMap.get(cma);
                ValueNode[] args = new ValueNode[callTarget.arguments().size()];
                RuntimeCallNode runtimeCallNode = new RuntimeCallNode(call, callTarget.arguments().toArray(args));
                RUNTIME_ENTRY runTimeEntry = call.getMethodActor().getAnnotation(RUNTIME_ENTRY.class);
                boolean exactType = runTimeEntry == null ? false : runTimeEntry.exactType();
                boolean nonNull = runTimeEntry == null ? false : runTimeEntry.nonNull();
                runtimeCallNode.setStamp(stampFor(runtime.lookupJavaType(call.getResultType()), exactType, nonNull));
                invoke.intrinsify(invoke.graph().add(runtimeCallNode));
            }
        }
    }

    private static Stamp stampFor(ResolvedJavaType resolvedJavaType, boolean exactType, boolean nonNull) {
        if (resolvedJavaType.isPrimitive()) {
            return StampFactory.forKind(resolvedJavaType.getKind());
        } else {
            if (exactType && nonNull) {
                return StampFactory.exactNonNull(resolvedJavaType);
            } else {
                return StampFactory.declared(resolvedJavaType, nonNull);
            }
        }
    }


}
