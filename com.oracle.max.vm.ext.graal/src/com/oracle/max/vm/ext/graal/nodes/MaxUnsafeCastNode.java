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
package com.oracle.max.vm.ext.graal.nodes;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.max.vm.ext.graal.phases.*;

/**
 * Handles Maxine's {@link UNSAFE_CAST} annotation. There is no value, and lots of issues, in trying to
 * canonicalize/infer the stamp given that almost all uses are on {@link Word} types, which have special semantics.
 *
 * N.B. Setting {@code exactType == true} for {@link Word} and {@link Reference} allows normal canonicalization of
 * {@link MethodCallTargetNode method calls} to resolve methods in the {@link Accessor} interface.
 *
 */
public class MaxUnsafeCastNode extends UnsafeCastNode {

    public MaxUnsafeCastNode(ValueNode object, ResolvedJavaType toType) {
        super(object, toType, MaxWordType.isWordOrReference(toType), false);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool) {
        return this;
    }

    @Override
    public boolean inferStamp() {
        return false;
    }
}
