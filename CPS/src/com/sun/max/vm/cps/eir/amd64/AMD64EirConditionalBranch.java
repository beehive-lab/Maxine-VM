/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.eir.amd64;

import com.sun.max.collect.*;
import com.sun.max.vm.cps.eir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AMD64EirConditionalBranch extends AMD64EirLocalControlTransfer {

    private EirBlock next;

    public EirBlock next() {
        return next;
    }

    AMD64EirConditionalBranch(EirBlock block, EirBlock target, EirBlock next) {
        super(block, target);
        this.next = next;
        next.addPredecessor(block);
    }

    @Override
    public void visitSuccessorBlocks(EirBlock.Procedure procedure) {
        super.visitSuccessorBlocks(procedure);
        if (next != null) {
            procedure.run(next);
        }
    }

    @Override
    public void substituteSuccessorBlocks(Mapping<EirBlock, EirBlock> map) {
        super.substituteSuccessorBlocks(map);
        if (map.containsKey(next)) {
            next = map.get(next);
        }
    }

    @Override
    public EirBlock selectSuccessorBlock(PoolSet<EirBlock> eligibleBlocks) {
        if (eligibleBlocks.contains(next)) {
            return next;
        }
        return super.selectSuccessorBlock(eligibleBlocks);
    }

    @Override
    public String toString() {
        String s = super.toString();
        if (next != null) {
            s += " | #" + next.serial();
        }
        return s;
    }
}
