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
package com.sun.max.vm.cps.eir.allocate;

import com.sun.max.vm.cps.eir.*;

/**
 * @author Bernd Mathiske
 */
public class EirPosition {

    private final EirBlock block;

    public EirBlock block() {
        return block;
    }

    private int index;
    private int number = -1;

    public int index() {
        return index;
    }

    public int number() {
        return number;
    }

    /**
     * Unique number within whole method.
     * @param instructionNumber the new unique number assigned to this instruction
     */
    public void setNumber(int instructionNumber) {
        number = instructionNumber;
    }

    public void setIndex(int instructionIndex) {
        index = instructionIndex;
    }

    public EirPosition(EirBlock block) {
        this.block = block;
        this.index = -1;
    }

    public EirPosition(EirBlock block, int index) {
        this.block = block;
        this.index = index;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof EirPosition) {
            final EirPosition position = (EirPosition) other;
            return index == position.index && block == position.block;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return index ^ block.hashCode();
    }
}
