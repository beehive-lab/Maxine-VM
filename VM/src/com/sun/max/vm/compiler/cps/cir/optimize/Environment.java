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

package com.sun.max.vm.compiler.cps.cir.optimize;

/**
 * Maps keys to values.
 *
 * @author Aziz Ghuloum
 */
public class Environment<KeyType, BasicType> {

    public Environment() {
    }

    public BasicType lookup(KeyType x) {
        return null;
    }

    public Environment<KeyType, BasicType> extend(KeyType key, BasicType val) {
        return new ExtendedEnvironment<KeyType, BasicType>(key, val, null);
    }

    /**
     * Extends an environment by adding a new <key,value> pair to an existing environment.
     *
     * Using a subclass for a non-empty environment optimizes the space requirement for empty
     * environments (which are presumed to be common).
     */
    static final class ExtendedEnvironment<KeyType, BasicType> extends Environment<KeyType, BasicType> {

        private final int depth;
        private final KeyType key;
        private final BasicType val;
        private final ExtendedEnvironment<KeyType, BasicType> next;

        private ExtendedEnvironment(KeyType key, BasicType val, ExtendedEnvironment<KeyType, BasicType> next) {
            this.key = key;
            this.val = val;
            this.next = next;
            this.depth = (next == null) ? 0 : next.depth + 1;
        }

        @Override
        public BasicType lookup(KeyType x) {
            if (x == key) {
                return val;
            }
            ExtendedEnvironment<KeyType, BasicType> e = next;
            while (e != null) {
                if (e.key == x) {
                    return e.val;
                }
                e = e.next;
            }
            return null;
        }

        @Override
        public Environment<KeyType, BasicType> extend(KeyType k, BasicType v) {
            return new ExtendedEnvironment<KeyType, BasicType>(k, v, this);
        }
    }
}
