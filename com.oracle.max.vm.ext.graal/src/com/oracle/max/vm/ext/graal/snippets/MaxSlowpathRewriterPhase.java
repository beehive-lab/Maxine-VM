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
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.phases.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.stubs.NativeStubSnippets.NOT_FOREIGN;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;

/**
 * Rewrites {@link InvokeNode}s to slow path runtime methods in Maxine snippets to {@link ForeignCallNode} nodes.
 * Ideally, this would not be necessary, but method calls within snippets are special in the sense that they
 * may not be deoptimization points (snippets as microcode).
 */
public class MaxSlowpathRewriterPhase extends Phase {

    private final MetaAccessProvider runtime;

    public MaxSlowpathRewriterPhase(MetaAccessProvider metaAccess) {
        this.runtime = metaAccess;
    }

    @Override
    protected void run(StructuredGraph graph) {
        for (Invoke invoke : graph.getInvokes()) {
            MethodCallTargetNode callTarget = (MethodCallTargetNode) invoke.callTarget();
            ClassMethodActor cma = (ClassMethodActor) MaxJavaMethod.getRiMethod(callTarget.targetMethod());
            MaxForeignCallDescriptor call = MaxForeignCallsMap.get(cma);
            if (call != null && cma.getAnnotation(NOT_FOREIGN.class) == null) {
                ValueNode[] args = new ValueNode[callTarget.arguments().size()];
                ForeignCallNode foreignCallNode = new ForeignCallNode(runtime, call, callTarget.arguments().toArray(args));
                SNIPPET_SLOWPATH runTimeEntry = call.getMethodActor().getAnnotation(SNIPPET_SLOWPATH.class);
                boolean exactType = runTimeEntry == null ? false : runTimeEntry.exactType();
                boolean nonNull = runTimeEntry == null ? false : runTimeEntry.nonNull();
                foreignCallNode.setStamp(stampFor(runtime.lookupJavaType(call.getResultType()), exactType, nonNull));
                invoke.intrinsify(callTarget.graph().add(foreignCallNode));
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
