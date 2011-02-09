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
package com.sun.max.vm.cps.cir.transform;

import com.sun.max.vm.cps.cir.variable.*;

/**
 * Simple variable environment for renaming purposes.
 *
 * @author Bernd Mathiske
 */
public final class CirVariableRenaming {
    private final CirVariableRenaming parent;

    public CirVariableRenaming parent() {
        return parent;
    }

    private final CirVariable from;

    public CirVariable from() {
        return from;
    }

    private final CirVariable to;

    public CirVariable to() {
        return to;
    }

    public CirVariableRenaming(CirVariableRenaming parent, CirVariable from, CirVariable to) {
        this.parent = parent;
        this.from = from;
        this.to = to;
    }

    public CirVariable find(CirVariable from) {
        CirVariableRenaming r = this;
        do {
            if (from == r.from()) {
                return r.to();
            }
            r = r.parent();
        } while (r != null);
        return null;
    }

    @Override
    public String toString() {
        String s = "";
        String separator = "";
        CirVariableRenaming renaming = this;
        do {
            s += separator + renaming.from + "->" + renaming.to;
            separator = " ";
            renaming = renaming.parent;
        } while (renaming != null);
        return s;
    }
}
