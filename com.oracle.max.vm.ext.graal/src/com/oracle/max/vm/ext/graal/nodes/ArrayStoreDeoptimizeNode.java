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

/**
 * Carries state appropriate for throwing an exception when compiling boot image code.
 *
 */
public class ArrayStoreDeoptimizeNode extends BootCheckDeoptimizeNode {
    @Input private ValueNode array;
    @Input private ValueNode object;

    public ArrayStoreDeoptimizeNode(ValueNode array, ValueNode object) {
        super(DeoptimizationReason.ArrayStoreException);
        this.array = array;
        this.object = object;
    }

    public ValueNode array() {
        return array;
    }

    public ValueNode object() {
        return object;
    }

    @NodeIntrinsic
    public static native void deopt(Object array, Object object);

}
