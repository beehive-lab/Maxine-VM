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
/*VCSID=53d3b722-539c-4c11-b1d0-28f92079d66a*/

package com.sun.max.vm.compiler.cir.optimize;

/**
 * Maps keys to values.
 *
 * @author Aziz Ghuloum
 */
public class Environment<KeyType, ValueType> {

    public Environment() {
    }

    public ValueType lookup(KeyType x) {
        return null;
    }

    public Environment<KeyType, ValueType> extend(KeyType key, ValueType val) {
        return new ExtendedEnvironment<KeyType, ValueType>(key, val, null);
    }

    /**
     * Extends an environment by adding a new <key,value> pair to an existing environment.
     *
     * Using a subclass for a non-empty environment optimizes the space requirement for empty
     * environments (which are presumed to be common).
     */
    static final class ExtendedEnvironment<KeyType, ValueType> extends Environment<KeyType, ValueType> {

        private final int _depth;
        private final KeyType _key;
        private final ValueType _val;
        private final ExtendedEnvironment<KeyType, ValueType> _next;

        private ExtendedEnvironment(KeyType key, ValueType val, ExtendedEnvironment<KeyType, ValueType> next) {
            _key = key;
            _val = val;
            _next = next;
            _depth = (next == null) ? 0 : next._depth + 1;
        }

        @Override
        public ValueType lookup(KeyType x) {
            if (x == _key) {
                return _val;
            }
            ExtendedEnvironment<KeyType, ValueType> e = _next;
            while (e != null) {
                if (e._key == x) {
                    return e._val;
                }
                e = e._next;
            }
            return null;
        }

        @Override
        public Environment<KeyType, ValueType> extend(KeyType key, ValueType val) {
            return new ExtendedEnvironment<KeyType, ValueType>(key, val, this);
        }
    }
}
