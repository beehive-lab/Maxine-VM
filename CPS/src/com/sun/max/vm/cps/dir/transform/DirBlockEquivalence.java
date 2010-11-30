/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
