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
package com.oracle.max.graal.nodes.extended;

import java.util.*;

import com.oracle.max.graal.nodes.*;
import com.sun.cri.ci.*;


public final class ReadVectorNode extends AccessVectorNode {

    public ReadVectorNode(AbstractVectorNode vector, ValueNode object, LocationNode location) {
        super(CiKind.Illegal, vector, object, location);
    }

    @Override
    public void addToLoop(LoopBeginNode loop, IdentityHashMap<AbstractVectorNode, ValueNode> nodes) {
        ValueNode index = nodes.get(vector());
        LocationNode newLocation = IndexedLocationNode.create(LocationNode.getArrayLocation(location().getValueKind()), location().getValueKind(), location().displacement(), index, graph());
        assert index != null;
        ReadNode readNode = graph().unique(new ReadNode(location().getValueKind().stackKind(), object(), newLocation));
        loop.loopEnd().replaceAtPredecessors(readNode);
        readNode.setNext(loop.loopEnd());
        nodes.put(this, readNode);
    }
}
