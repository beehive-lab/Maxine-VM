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
