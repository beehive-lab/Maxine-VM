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

import com.oracle.max.graal.extensions.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.sun.cri.bytecode.*;
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
                    Graph<EntryPointNode> graph = new Graph<EntryPointNode>(new EntryPointNode());
                    CiKind returnKind = accessorMethod.signature().returnKind(false);
                    RiType returnType = accessorMethod.signature().returnType(accessor);
                    InvokeNode invoke = graph.add(new InvokeNode(callerPos.bci, Bytecodes.INVOKESPECIAL, returnKind, parameters.toArray(new ValueNode[0]), accessorMethod, returnType));
                    ReturnNode ret = graph.add(new ReturnNode(invoke));
                    graph.start().setNext(invoke);
                    invoke.setNext(ret);
                    return graph;
                }
                pos = pos.caller;
            }
        }
        return null;
    }

}
