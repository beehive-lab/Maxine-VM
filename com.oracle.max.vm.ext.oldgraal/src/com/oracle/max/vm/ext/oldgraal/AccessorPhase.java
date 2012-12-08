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
package com.oracle.max.vm.ext.oldgraal;

import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.graal.nodes.java.MethodCallTargetNode.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ri.*;
import com.sun.max.unsafe.*;


public class AccessorPhase extends Phase {

    private final MaxRuntime runtime;
    private final RiResolvedType accessor;

    public AccessorPhase(MaxRuntime runtime, RiResolvedType accessor) {
        this.runtime = runtime;
        this.accessor = accessor;
    }

    @Override
    protected void run(StructuredGraph graph) {
        RiResolvedType accessorType = runtime.getType(Accessor.class);
        for (Invoke invoke : graph.getInvokes()) {
            MethodCallTargetNode callTarget = invoke.callTarget();
            RiResolvedMethod method = callTarget.targetMethod();
            if (method.holder().equals(accessorType)) {
                callTarget.setTargetMethod(accessor.resolveMethodImpl(method));
                callTarget.setInvokeKind(InvokeKind.Special);
            }
        }
    }
}
