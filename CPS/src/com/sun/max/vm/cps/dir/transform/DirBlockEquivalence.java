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
package com.sun.max.vm.cps.dir.transform;

import java.util.*;

import com.sun.max.vm.cps.dir.*;

/**
 * A equivalence relation that can be used to determine whether two DIR blocks have the same computational semantics and therefore can be merged into one.
 *
 * @author Bernd Mathiske
 */
public final class DirBlockEquivalence {

    private class Pair {
        final DirBlock a;
        final DirBlock b;

        Pair(DirBlock a, DirBlock b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(a) ^ System.identityHashCode(b);
        }

        @Override
        public boolean equals(Object other) {
            final Pair pair = (Pair) other;
            return a == pair.a && b == pair.b;
        }

        boolean makeFalse() {
            maybePairs.remove(this);
            falsePairs.add(this);
            return false;
        }

        boolean makeTrue() {
            maybePairs.remove(this);
            truePairs.add(this);
            return true;
        }
    }

    private final Set<Pair> maybePairs = new HashSet<Pair>();
    private final Set<Pair> falsePairs = new HashSet<Pair>();
    private final Set<Pair> truePairs = new HashSet<Pair>();

    public DirBlockEquivalence() {
    }

    public boolean evaluate(DirBlock a, DirBlock b) {
        if (a == b) {
            return true;
        }
        final Pair pair = new Pair(a, b);
        if (falsePairs.contains(pair)) {
            return false;
        }
        if (truePairs.contains(pair) || maybePairs.contains(pair)) {
            return true;
        }
        maybePairs.add(pair);
        if (a.role() != b.role()) {
            return pair.makeFalse();
        }
        if (a.instructions().size() != b.instructions().size()) {
            return pair.makeFalse();
        }
        for (int i = 0; i < a.instructions().size(); i++) {
            if (!a.instructions().get(i).isEquivalentTo(b.instructions().get(i), this)) {
                return pair.makeFalse();
            }
        }
        return pair.makeTrue();
    }
}
