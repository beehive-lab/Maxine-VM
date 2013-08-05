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
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.SnippetTemplate.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;


public class MaxMiscLowerings extends SnippetLowerings {

    @HOSTED_ONLY
    public MaxMiscLowerings(MetaAccessProvider runtime, Replacements replacements, TargetDescription target,
                    Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, replacements, target);
    }

    @Override
    @HOSTED_ONLY
    public void registerLowerings(MetaAccessProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        lowerings.put(LoadExceptionObjectNode.class, new LoadExceptionObjectLowering(this));
        lowerings.put(UnwindNode.class, new UnwindLowering(this));
        lowerings.put(DeoptimizeNode.class, new DeoptimizeLowering());
    }

    protected class DeoptimizeLowering extends Lowering implements LoweringProvider<DeoptimizeNode> {

        @Override
        public void lower(DeoptimizeNode node, LoweringTool tool) {
            // currently handled by LIR
        }

    }

    protected class LoadExceptionObjectLowering extends Lowering implements LoweringProvider<LoadExceptionObjectNode> {

        LoadExceptionObjectLowering(MaxMiscLowerings invokeSnippets) {
            super(invokeSnippets, "loadExceptionObjectSnippet");
        }

        @Override
        public void lower(LoadExceptionObjectNode node, LoweringTool tool) {
            Arguments args = new Arguments(snippet);
            instantiate(node, args);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static Throwable loadExceptionObjectSnippet() {
        return loadExceptionForHandler();
    }

    @SNIPPET_SLOWPATH(nonNull = true)
    private static Throwable loadExceptionForHandler() {
        // this aborts the VM if the stored exception object is null
        return VmThread.current().loadExceptionForHandler();
    }

    protected class UnwindLowering extends Lowering implements LoweringProvider<UnwindNode> {

        UnwindLowering(MaxMiscLowerings invokeSnippets) {
            super(invokeSnippets, "throwExceptionSnippet");
        }

        @Override
        public void lower(UnwindNode node, LoweringTool tool) {
            Arguments args = new Arguments(snippet);
            args.add("throwable", node.exception());
            instantiate(node, args);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void throwExceptionSnippet(Throwable throwable) {
        throwException(throwable);
        throw UnreachableNode.unreachable();
    }

    @SNIPPET_SLOWPATH
    private static void throwException(Throwable throwable) {
        Throw.raise(throwable);
    }


}
