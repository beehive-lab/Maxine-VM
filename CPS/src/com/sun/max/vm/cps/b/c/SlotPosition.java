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

import com.sun.max.vm.type.*;

/**
 * Position in a Java frame or stack.
 *
 * If in the sense of the JVM spec, the same slot is occupied by items of different type, we instead reserve a separate
 * slot for each type. This disambiguation simplifies stack merging at basic block boundaries.
 *
 * @author Bernd Mathiske
 */
class SlotPosition {

    private final Kind kind;
    private final int slotIndex;

    SlotPosition(Kind kind, int slotIndex) {
        this.kind = kind.stackKind;
        this.slotIndex = slotIndex;
    }

    public Kind getKind() {
        return kind;
    }

    public int getSlot() {
        return slotIndex;
    }

    @Override
    public int hashCode() {
        return (kind.hashCode() ^ slotIndex) + slotIndex;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SlotPosition)) {
            return false;
        }
        final SlotPosition position = (SlotPosition) other;
        return kind == position.kind && slotIndex == position.slotIndex;
    }

}
