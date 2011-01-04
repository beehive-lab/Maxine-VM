/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.b.c;

import java.util.*;

import com.sun.max.*;
import com.sun.max.vm.*;
import com.sun.max.vm.cps.bir.*;
import com.sun.max.vm.cps.cir.*;

/**
 * Abstract interpretation state associated with a block.
 *
 * @author Bernd Mathiske
 */
public class BlockState {

    private final BirBlock birBlock;
    private List<CirBlock> cirBlockList;
    private JavaFrame frame = null;
    private JavaStack stack = null;

    BlockState(BirBlock birBlock) {
        this.birBlock = birBlock;
    }

    public BirBlock birBlock() {
        return birBlock;
    }

    /**
     * Determines if a CIR block has been created for the BIR block.
     */
    public boolean hasCirBlock() {
        return cirBlockList != null;
    }

    /**
     * Gets the CIR block for the BIR block, creating it first if necessary. This method should
     * only be called if the BIR block is known to be reachable otherwise CIR blocks will be created
     * for unreachable code, inevitably causing problems for subsequent CIR compilation.
     */
    public CirBlock cirBlock() {
        return Utils.first(cirBlockList());
    }

    public List<CirBlock>  cirBlockList() {
        if (cirBlockList == null) {
            cirBlockList = new LinkedList<CirBlock>();
            cirBlockList.add(new CirBlock(birBlock.role()));
        }
        return cirBlockList;
    }

    public void addCirBlock(CirBlock cirBlock) {
        if (cirBlockList == null) {
            Log.println("cirBlockList is null");
            assert false;
        }
        cirBlockList.add(cirBlock);
    }

    public JavaFrame frame() {
        return frame;
    }

    public void setFrame(JavaFrame frame) {
        this.frame = frame;
    }

    public JavaStack stack() {
        return stack;
    }

    public void setStack(JavaStack stack) {
        this.stack = stack;
    }
}
