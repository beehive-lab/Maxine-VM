/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.max.vm.ext.graal.amd64.MaxAMD64Backend.MaxAMD64LIRGenerator;

/**
 * Maxine a different style of compare and swap to the default Graal node, which returns {@code boolean),,
 * and instead returns either the expected value or the old value, depending on the success of the operation.
 */
public class MaxCompareAndSwapNode extends CompareAndSwapNode {

    public MaxCompareAndSwapNode(ValueNode object, int displacement, ValueNode offset, ValueNode expected, ValueNode newValue) {
        super(object, displacement, offset, expected, newValue);
        setStamp(expected.stamp());
    }

    @Override
    public void generate(LIRGeneratorTool gen) {
        MaxAMD64LIRGenerator maxGen = (MaxAMD64LIRGenerator) gen;
        maxGen.visitMaxCompareAndSwap(this);
    }



}
