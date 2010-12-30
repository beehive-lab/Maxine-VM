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
package com.sun.max.vm.cps.cir.optimize;

import com.sun.max.vm.cps.cir.*;

/**
 * The meet-semilattice abstract class for dataflow analysis.
 *
 * @author Yi Guo
 */
public abstract class AbstractValueDomain<T extends AbstractValue<T>> {
    public final T meet(T a, T b) {
        if (a == b) {
            return a;
        }
        if (a.isBottom() || b.isBottom()) {
            return getBottom();
        }
        if (a.isTop()) {
            return b;
        }
        if (b.isTop()) {
            return a;
        }
        return a.meetNontrivial(b);
    }

    public final boolean equal(T a, T b) {
        if (a == b) {
            return true;
        }
        if (a.isTop() || a.isBottom() || b.isTop() || b.isBottom()) {
            return false;
        }
        return a.eqNontrivial(b);
    }

    public final boolean lessOrEqual(T a, T b) {
        if (a.isBottom()) {
            return true;
        }
        if (a.isTop()) {
            return b.isTop();
        }
        if (b.isBottom()) {
            return false;
        }
        if (b.isTop()) {
            return true;
        }
        if (a == b) {
            return true;
        }
        return a.lessOrEqualNontrivial(b);
    }

    public abstract T fromConstant(CirConstant c);

    public abstract T getTop();
    public abstract T getBottom();
}
