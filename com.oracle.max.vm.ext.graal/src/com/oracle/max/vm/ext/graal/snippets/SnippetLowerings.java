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
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.SnippetTemplate.*;
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
                replacements.getSnippet(runtime.lookupJavaMethod(method));
            }
        }
    }

    public Map<Node, Node> instantiate(FixedNode node, Arguments args) {
        return template(args).instantiate(runtime, node, SnippetTemplate.DEFAULT_REPLACER, args);
    }

    public abstract void registerLowerings(
                    CodeCacheProvider runtime, Replacements replacements,
                    TargetDescription targetDescription,
                    Map<Class< ? extends Node>, LoweringProvider> lowerings);


}
