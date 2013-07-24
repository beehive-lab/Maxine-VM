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
import com.oracle.graal.nodes.type.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.phases.*;

/**
 * Handles Maxine's {@link UNSAFE_CAST} annotation; slight difference to {@link UnsafeCastNode regarding {@link Word} types
 * and an important check to prevent the cast from {@link Reference} to {@link Object} from being removed during
 * canonicalization, which would cause havoc in Graal's type-based optimization phases.
 */
public class MaxUnsafeCastNode extends UnsafeCastNode {

    public MaxUnsafeCastNode(ValueNode object, ResolvedJavaType toType) {
        // The Reference type is most certainly exact; recording that is important to allow
        // method calls through the Accessor interface to be converted (and inlined) correctly.
        // This is really a hack and should be done differently in a special phase.
        super(object, toType, MaxWordType.isWordOrReference(toType), false);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool) {
        if (kind() != object().kind()) {
            return this;
        }

        // Both same Kind
        if (kind() == Kind.Object && objectStamp().type() == MaxResolvedJavaType.getJavaLangObject()) {
            ResolvedJavaType otherType = object().objectStamp().type();
            if (otherType == MaxResolvedJavaType.getReferenceType()) {
                // the default canonicalization remove the cast; we don't want that!
                return this;
            }
        }
        return super.canonical(tool);
    }

    @Override
    public boolean inferStamp() {
        if (kind() != Kind.Object || object().kind() != Kind.Object) {
            return false;
        }
        if (stamp() == StampFactory.forNodeIntrinsic()) {
            return false;
        }
        if (objectStamp().type() == MaxResolvedJavaType.getJavaLangObject() &&
                        object().objectStamp().type() ==  MaxResolvedJavaType.getReferenceType()) {
            return false;
        }
        return super.inferStamp();
    }
}
