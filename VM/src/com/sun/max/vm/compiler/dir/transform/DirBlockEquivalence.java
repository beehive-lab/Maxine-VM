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
package com.sun.max.vm.compiler.dir.transform;

import java.util.*;

import com.sun.max.vm.compiler.dir.*;

/**
 * A equivalence relation that can be used to determine whether two DIR blocks have the same computational semantics and therefore can be merged into one.
 * 
 * @author Bernd Mathiske
 */
public final class DirBlockEquivalence {

    private class Pair {
        final DirBlock _a;
        final DirBlock _b;

        Pair(DirBlock a, DirBlock b) {
            _a = a;
            _b = b;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(_a) ^ System.identityHashCode(_b);
        }

        @Override
        public boolean equals(Object other) {
            final Pair pair = (Pair) other;
            return _a == pair._a && _b == pair._b;
        }

        boolean makeFalse() {
            _maybe.remove(this);
            _false.add(this);
            return false;
        }

        boolean makeTrue() {
            _maybe.remove(this);
            _true.add(this);
            return true;
        }
    }

    private final Set<Pair> _maybe = new HashSet<Pair>();
    private final Set<Pair> _false = new HashSet<Pair>();
    private final Set<Pair> _true = new HashSet<Pair>();

    public DirBlockEquivalence() {
    }

    public boolean evaluate(DirBlock a, DirBlock b) {
        if (a == b) {
            return true;
        }
        final Pair pair = new Pair(a, b);
        if (_false.contains(pair)) {
            return false;
        }
        if (_true.contains(pair) || _maybe.contains(pair)) {
            return true;
        }
        _maybe.add(pair);
        if (a.instructions().length() != b.instructions().length()) {
            return pair.makeFalse();
        }
        for (int i = 0; i < a.instructions().length(); i++) {
            if (!a.instructions().get(i).isEquivalentTo(b.instructions().get(i), this)) {
                return pair.makeFalse();
            }
        }
        return pair.makeTrue();
    }
}
