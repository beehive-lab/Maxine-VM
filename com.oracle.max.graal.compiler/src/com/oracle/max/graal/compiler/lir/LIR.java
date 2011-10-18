/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.lir;

import java.util.*;

import com.oracle.max.graal.graph.*;

/**
 * This class implements the overall container for the LIR graph
 * and directs its construction, optimization, and finalization.
 */
public class LIR {

    /**
     * The start block of this LIR.
     */
    private final LIRBlock startBlock;

    /**
     * The linear-scan ordered list of blocks.
     */
    private final List<LIRBlock> linearScanOrder;

    /**
     * The order in which the code is emitted.
     */
    private final List<LIRBlock> codeEmittingOrder;

    private final NodeMap<LIRBlock> valueToBlock;

    /**
     * Creates a new LIR instance for the specified compilation.
     * @param compilation the compilation
     */
    public LIR(LIRBlock startBlock, List<LIRBlock> linearScanOrder, List<LIRBlock> codeEmittingOrder, NodeMap<LIRBlock> valueToBlock) {
        this.codeEmittingOrder = codeEmittingOrder;
        this.linearScanOrder = linearScanOrder;
        this.startBlock = startBlock;
        this.valueToBlock = valueToBlock;
    }

    /**
     * Gets the linear scan ordering of blocks as a list.
     * @return the blocks in linear scan order
     */
    public List<LIRBlock> linearScanOrder() {
        return linearScanOrder;
    }

    public List<LIRBlock> codeEmittingOrder() {
        return codeEmittingOrder;
    }

    public LIRBlock startBlock() {
        return startBlock;
    }

    public NodeMap<LIRBlock> valueToBlock() {
        return valueToBlock;
    }
}
