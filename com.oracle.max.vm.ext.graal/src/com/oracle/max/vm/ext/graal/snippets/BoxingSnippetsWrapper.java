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
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.replacements.*;


public class BoxingSnippetsWrapper extends SnippetLowerings {

    public BoxingSnippetsWrapper(MetaAccessProvider runtime, Replacements replacements, TargetDescription target, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, replacements, target);
        getSnippetGraphs(BoxingSnippets.class);
    }

    @Override
    public void registerLowerings(MetaAccessProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        lowerings.put(BoxNode.class, new BoxingLowering(new BoxingSnippets.Templates(runtime, replacements, targetDescription)));
        lowerings.put(UnboxNode.class, new UnboxingLowering(new BoxingSnippets.Templates(runtime, replacements, targetDescription)));

    }

    protected class BoxingLowering extends Lowering implements LoweringProvider<BoxNode> {
        private final BoxingSnippets.Templates snippets;

        BoxingLowering(BoxingSnippets.Templates snippets) {
            this.snippets = snippets;
        }

        @Override
        public void lower(BoxNode node, LoweringTool tool) {
            snippets.lower(node, tool);

        }
    }

    protected class UnboxingLowering extends Lowering implements LoweringProvider<UnboxNode> {
        private final BoxingSnippets.Templates snippets;

        UnboxingLowering(BoxingSnippets.Templates snippets) {
            this.snippets = snippets;
        }

        @Override
        public void lower(UnboxNode node, LoweringTool tool) {
            snippets.lower(node, tool);

        }
    }

}
