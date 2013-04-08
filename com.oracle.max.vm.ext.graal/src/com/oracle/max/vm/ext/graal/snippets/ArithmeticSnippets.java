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

import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.spi.*;
import com.sun.max.annotate.*;

/**
 * We are required to provide a lowering for the div/rem nodes, but we take the default {@link LIRLowering}.
 */
public final class ArithmeticSnippets extends SnippetLowerings {

    @HOSTED_ONLY
    public ArithmeticSnippets(CodeCacheProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, replacements, targetDescription);
    }

    @Override
    @HOSTED_ONLY
    public void registerLowerings(CodeCacheProvider runtime, Replacements replacements,
                    TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        lowerings.put(IntegerDivNode.class, new IdentityLowering());
        lowerings.put(IntegerRemNode.class, new IdentityLowering());
        lowerings.put(UnsignedDivNode.class, new IdentityLowering());
        lowerings.put(UnsignedRemNode.class, new IdentityLowering());
    }

    protected class IdentityLowering implements LoweringProvider<Node> {

        @Override
        public void lower(Node node, LoweringTool tool) {
            // do nothing and leave node unchanged.
        }
    }
}


