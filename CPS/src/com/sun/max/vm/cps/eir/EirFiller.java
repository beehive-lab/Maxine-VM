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
package com.sun.max.vm.cps.eir;

/**
 * This instruction is used to effectively delete another instruction by overwriting it,
 * sparing the effort to restructure the block it is placed into.
 * No filler is supposed to have any effect.
 *
 * @author Bernd Mathiske
 */
public final class EirFiller extends EirInstruction {

    public EirFiller(EirBlock block) {
        super(block);
    }

    @Override
    public boolean isRedundant() {
        return true;
    }

    @Override
    public void addLiveVariable(EirVariable variable) {
        // Do nothing: this type of instruction does not record variable liveness as it is simply a place holder for an
        // instruction deleted by the register allocator
    }

    @Override
    public void removeLiveVariable(EirVariable variable) {
        // Do nothing: this type of instruction does not record variable liveness as it is simply a place holder for an
        // instruction deleted by the register allocator
    }

    @Override
    public void emit(EirTargetEmitter emitter) {
    }

    @Override
    public void acceptVisitor(EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "<filler>";
    }

}
