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
package com.sun.max.vm.cps.bir;

import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.ir.*;

/**
 * A basic block is a control flow unit, a linear sequence of operations without any control flow.
 *
 * @author Bernd Mathiske
 */
public class BirBlock implements IrBlock {

    private BytecodeBlock bytecodeBlock;
    private IrBlock.Role role;
    private LinkedIdentityHashSet<BirBlock> predecessors = new LinkedIdentityHashSet<BirBlock>();
    private LinkedIdentityHashSet<BirBlock> successors = new LinkedIdentityHashSet<BirBlock>();
    private boolean hasSafepoint;

    public BytecodeBlock bytecodeBlock() {
        return bytecodeBlock;
    }

    public IrBlock.Role role() {
        return role;
    }

    public void setRole(IrBlock.Role role) {
        this.role = role;
    }

    public boolean hasSafepoint() {
        return hasSafepoint;
    }

    public void haveSafepoint() {
        hasSafepoint = true;
    }

    public BirBlock(BytecodeBlock bytecodeBlock) {
        this.bytecodeBlock = bytecodeBlock;
        this.role = IrBlock.Role.NORMAL;
    }

    @Override
    public int hashCode() {
        return bytecodeBlock.start * bytecodeBlock.end;
    }

    public void addPredecessor(BirBlock predecessor) {
        predecessors.add(predecessor);
    }

    public LinkedIdentityHashSet<BirBlock> predecessors() {
        return predecessors;
    }

    public void addSuccessor(BirBlock successor) {
        successors.add(successor);
    }

    public LinkedIdentityHashSet<BirBlock> successors() {
        return successors;
    }

    public int serial() {
        return bytecodeBlock.start;
    }

    @Override
    public String toString() {
        return "<" + role + ": " + bytecodeBlock + ">";
    }

    public boolean isReachable() {
        return !predecessors().isEmpty() || bytecodeBlock.start == 0 || role == IrBlock.Role.EXCEPTION_DISPATCHER;
    }
}
