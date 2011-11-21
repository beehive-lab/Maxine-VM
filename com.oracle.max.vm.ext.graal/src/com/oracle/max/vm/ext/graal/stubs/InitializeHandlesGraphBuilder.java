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
package com.oracle.max.vm.ext.graal.stubs;

import static com.sun.max.vm.jni.JniHandles.*;

import java.util.*;

import com.oracle.max.graal.nodes.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;

/**
 * Builds the graph that will allocate and initialize the handles
 * in a native method stub.
 */
class InitializeHandlesGraphBuilder extends AbstractGraphBuilder {

    private ValueNode handles;
    private int handleOffset;

    public InitializeHandlesGraphBuilder(ClassMethodActor nativeMethod) {
        super(nativeMethod);
        setGraph(new StructuredGraph());
        SignatureDescriptor sig = nativeMethod.descriptor();

        List<LocalNode> nativeMethodArgs = createLocals(0, sig, nativeMethod.isStatic());
        Iterator<LocalNode> nativeMethodArg = nativeMethodArgs.iterator();

        int handlesSize = (handlesCount(sig) + 1) * Word.size();
        handles = append(invoke(Intrinsics_alloca, iconst(handlesSize), zconst(true)));

        if (nativeMethod.isStatic()) {
            append(invoke(Pointer_writeObject, handles, iconst(0), oconst(nativeMethod.holder().toJava())));
        } else {
            append(invoke(Pointer_writeObject, handles, iconst(0), nativeMethodArg.next()));
            handleOffset += Word.size();
        }

        // Load the remaining parameters, handlizing object parameters
        while (nativeMethodArg.hasNext()) {
            LocalNode arg = nativeMethodArg.next();
            if (arg.kind().isObject()) {
                append(invoke(Pointer_writeObject, handles, iconst(0), arg));
                handleOffset += Word.size();
            }
        }

        append(graph.add(new ReturnNode(handles)));
    }
}
