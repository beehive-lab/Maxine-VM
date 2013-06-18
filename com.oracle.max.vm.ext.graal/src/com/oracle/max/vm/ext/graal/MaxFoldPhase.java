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
package com.oracle.max.vm.ext.graal;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.Snippet.*;

/**
 * Maxine boot image compilation needs {@link @Fold} support, which is normally
 * restricted to snippets in Graal. We can't just use the snippets {@link NodeIntrinsification}
 * phase because there can be unresolved {@link HOSTED_ONLY} methods in the graph.
 */
public class MaxFoldPhase extends NodeIntrinsificationPhase {

    public MaxFoldPhase(MetaAccessProvider runtime) {
        super(runtime);
    }

    @Override
    protected void run(StructuredGraph graph) {
        // Identical to NodeIntrinsificationPhase.run, save for the check on unresolved methods
        ArrayList<Node> cleanUpReturnList = new ArrayList<>();
        for (MethodCallTargetNode node : graph.getNodes(MethodCallTargetNode.class)) {
            if (node.isResolved() && node.targetMethod().getAnnotation(Fold.class) != null) {
                try {
                tryIntrinsify(node, cleanUpReturnList);
                } catch (Exception ex) {
                   System.console();
                }
            }
        }

        for (Node node : cleanUpReturnList) {
            cleanUpReturnCheckCast(node);
        }
    }

}
