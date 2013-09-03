/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.spi.Lowerable.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.SnippetTemplate.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.runtime.CriticalMethod;

public abstract class SnippetLowerings extends AbstractTemplates implements Snippets {

    public abstract static class Lowering {
        protected final SnippetInfo snippet;

        protected Lowering(SnippetLowerings snippetLowererings, String methodName) {
            this.snippet = snippetLowererings.snippet(snippetLowererings.getClass(), methodName);
        }

        protected Lowering() {
            snippet = null;
        }
    }

    public SnippetLowerings(MetaAccessProvider runtime, Replacements replacements, TargetDescription target) {
        super(runtime, replacements, target);

        // All the SNIPPET_SLOWPATH methods are critical
        for (Method method : getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(SNIPPET_SLOWPATH.class)) {
                new CriticalMethod(method);
            }
        }
        getSnippetGraphs(getClass());

    }

    /**
     * Forces the graphs for all @Snippet methods in the given class to be built (at image build time).
     * @param snippetClass
     */
    @HOSTED_ONLY
    protected void getSnippetGraphs(Class<?> snippetClass) {
        for (Method method : snippetClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Snippet.class)) {
                snippetGraph(method, replacements.getSnippet(runtime.lookupJavaMethod(method)));
            }
        }
    }

    /**
     * Informs a subclass of the graph for snippet method {@code m}.
     */
    protected void snippetGraph(Method m, StructuredGraph graph) {

    }

    public Map<Node, Node> instantiate(FixedNode node, Arguments args, LoweringTool tool) {
        StructuredGraph graph = node.graph();
        SnippetTemplate template = template(args);
        Map<Node, Node> result = template.instantiate(runtime, node, SnippetTemplate.DEFAULT_REPLACER, args);
        // We handle the immediate lowering of the MaxNullCheck nodes here
        boolean hadCheck = false;
        for (Node rNode : result.values()) {
            if (rNode instanceof MaxCheckNode) {
                ((MaxCheckNode) rNode).lower(tool, LoweringType.BEFORE_GUARDS);
                hadCheck = true;
            }
        }
        if (hadCheck) {
            Debug.dump(graph, "After lowering MaxCheck");
        }
        return result;
    }

    public abstract void registerLowerings(
                    MetaAccessProvider runtime, Replacements replacements,
                    TargetDescription targetDescription,
                    Map<Class< ? extends Node>, LoweringProvider> lowerings);


}
