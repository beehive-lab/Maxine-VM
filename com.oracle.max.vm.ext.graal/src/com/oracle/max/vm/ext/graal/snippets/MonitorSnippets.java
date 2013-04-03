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

import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.Snippet.*;
import com.oracle.graal.replacements.SnippetTemplate.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.runtime.*;

/**
 * Maxine's monitor implementation is controlled by the {@link MonitorScheme} that is defined at boot image
 * time. All the details are encapsulated inside the scheme. In principle the snippets to effect the lowering of
 * the {@link MonitorEnterNode} and {@link MonitorExitNode} are extremely simple, similar to those for
 * {@link NewSnippets allocation}, consisting of calls to {@link Monitor#enter(Object)} and {@link Monitor#exit(Object)},
 * respectively. In practice, life is complicated by the need to maintain a {@link FrameState} that reflects the
 * monitor depth, because Graal assumes that locks are "allocated" on the stack. Maxine currently allocates
 * monitor objects (when needed) from a heap allocated pool, so this is redundant. Unfortunately Graal breaks
 * if the stack based locking info is not properly maintained. This is complicated by the fact that the
 * monitor depth recording has to take place <i>after</i> any null check. Currently, the latter is hidden
 * inside Maxine's monitor implementation and we cannot trivially insert code that creates Graal nodes there.
 * For now we do the null check in the snippet, then introduce a {@link BeginLockScopeNode} and then invoke
 * {@link Monitor#enter}.
  *
 */
public class MonitorSnippets extends SnippetLowerings {

    @HOSTED_ONLY
    public MonitorSnippets(CodeCacheProvider runtime, TargetDescription targetDescription, Assumptions assumptions, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, assumptions, targetDescription);
    }

    @Override
    @HOSTED_ONLY
    public void registerLowerings(CodeCacheProvider runtime, TargetDescription targetDescription, Assumptions assumptions, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        lowerings.put(MonitorEnterNode.class, new MonitorEnterLowering(this));
        lowerings.put(MonitorExitNode.class, new MonitorExitLowering(this));
    }

    protected class MonitorLowering extends Lowering {
        private final ResolvedJavaMethod monitorEliminatedSnippet;

        MonitorLowering(MonitorSnippets snippets, String snippetName, ResolvedJavaMethod monitorEliminatedSnippet) {
            super(snippets, snippetName);
            this.monitorEliminatedSnippet = monitorEliminatedSnippet;
        }

        void lower(AccessMonitorNode node) {
            FrameState stateAfter = node.stateAfter();
            boolean eliminated = node.eliminated();
            Key key = new Key(eliminated ? monitorEliminatedSnippet : snippet);
            boolean checkNull = !node.object().stamp().nonNull();
            Arguments args = new Arguments();
            if (!eliminated) {
                args.add("receiver", node.object());
            }
            if (node instanceof MonitorEnterNode && !eliminated) {
                key.add("checkNull", checkNull);
            }
            Map<Node, Node> nodes = instantiate(node, key, args);
            for (Node n : nodes.values()) {
                if (n instanceof LockScopeNode) {
                    LockScopeNode end = (LockScopeNode) n;
                    end.setStateAfter(stateAfter);
                }
            }
        }
    }

    protected class MonitorEnterLowering extends MonitorLowering implements LoweringProvider<MonitorEnterNode> {

        MonitorEnterLowering(MonitorSnippets snippets) {
            super(snippets, "monitorEnterSnippet", snippets.findSnippet(MonitorSnippets.class, "monitorEnterEliminated"));
        }

        @Override
        public void lower(MonitorEnterNode node, LoweringTool tool) {
            super.lower(node);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void monitorEnterSnippet(@Parameter("receiver") Object receiver, @ConstantParameter("checkNull") boolean checkNull) {
        if (checkNull && receiver == null) {
            Throw.throwNullPointerException();
            throw UnreachableNode.unreachable();
        }
        BeginLockScopeNode.beginLockScope(true);
        Monitor.enter(receiver);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void monitorEnterEliminated() {
        BeginLockScopeNode.beginLockScope(true);
    }

    protected class MonitorExitLowering extends MonitorLowering implements LoweringProvider<MonitorExitNode> {

        MonitorExitLowering(MonitorSnippets snippets) {
            super(snippets, "monitorExitSnippet", snippets.findSnippet(MonitorSnippets.class, "monitorExitEliminated"));
        }

        @Override
        public void lower(MonitorExitNode node, LoweringTool tool) {
            super.lower(node);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void monitorExitSnippet(@Parameter("receiver") Object receiver) {
        Monitor.exit(receiver);
        EndLockScopeNode.endLockScope();
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void monitorExitEliminated() {
        EndLockScopeNode.endLockScope();
    }

}
