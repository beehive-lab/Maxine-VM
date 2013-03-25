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
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.SnippetTemplate.Arguments;
import com.oracle.graal.replacements.SnippetTemplate.Cache;
import com.oracle.graal.replacements.SnippetTemplate.Key;
import com.sun.max.annotate.*;
import com.sun.max.vm.runtime.CriticalMethod;

public abstract class SnippetLowerings implements Snippets {

    protected final MetaAccessProvider runtime;
    protected final Assumptions assumptions;
    protected final Cache cache;

    public abstract static class Lowering {
        protected final ResolvedJavaMethod snippet;

        protected Lowering(SnippetLowerings snippetLowererings, String methodName) {
            this.snippet = snippetLowererings.findSnippet(snippetLowererings.getClass(), methodName);
        }

        protected Lowering() {
            snippet = null;
        }
    }

    public SnippetLowerings(MetaAccessProvider runtime, Assumptions assumptions, TargetDescription target) {
        this.runtime = runtime;
        this.assumptions = assumptions;
        this.cache = new Cache(runtime, target);

        // All the RUNTIME_ENTRY methods are critical
        for (Method method : getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(RUNTIME_ENTRY.class)) {
                new CriticalMethod(method);
            }
        }
    }

    public ResolvedJavaMethod findSnippet(Class< ? extends SnippetLowerings> clazz, String name) {
        Method found = null;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                assert found == null : "found more than one method " + name;
                found = method;
            }
        }
        assert found != null : "did not find method " + name;
        assert found.getAnnotation(Snippet.class) != null;
        return runtime.lookupJavaMethod(found);
    }

    public Map<Node, Node> instantiate(FixedNode node, Key key, Arguments args) {
        return cache.get(key, assumptions).instantiate(runtime, node, SnippetTemplate.DEFAULT_REPLACER, args);
    }

    public abstract void registerLowerings(
                    CodeCacheProvider runtime, TargetDescription targetDescription,
                    Assumptions assumptions,
                    Map<Class< ? extends Node>, LoweringProvider> lowerings);


}
