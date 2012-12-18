/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.snippets.*;
import com.oracle.graal.snippets.SnippetTemplate.Cache;

public abstract class SnippetLowerings {

    protected final MetaAccessProvider runtime;
    protected final Assumptions assumptions;
    protected final Cache cache;

    public SnippetLowerings(MetaAccessProvider runtime, Assumptions assumptions, TargetDescription target) {
        this.runtime = runtime;
        this.assumptions = assumptions;
        this.cache = new Cache(runtime, target);
    }

    public ResolvedJavaMethod findSnippet(Class< ? extends SnippetsInterface> clazz, String name) {
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
}
