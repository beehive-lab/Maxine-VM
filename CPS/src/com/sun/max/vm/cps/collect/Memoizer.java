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
package com.sun.max.vm.cps.collect;

import com.sun.max.collect.*;
import com.sun.max.unsafe.*;

/**
 * Memoization of function results with adaptive caching.
 *
 * Typically used to implement infrequent functional attributes.
 *
 * @author Bernd Mathiske
 */
public final class Memoizer {
    private Memoizer() {
    }

    public interface Function<Key_Type, Value_Type> {

        public static final class Result<Value_Type> {

            private final Value_Type value;

            public Value_Type value() {
                return value;
            }

            private final Size size;

            public Result(Value_Type value, Size size) {
                this.value = value;
                this.size = size;
            }
        }

        Result<Value_Type> create(Key_Type key);
    }

    public static <Key_Type, Value_Type> Mapping<Key_Type, Value_Type> create(Function<Key_Type, Value_Type> function) {
        return new FunctionResultCache<Key_Type, Value_Type>(function);
    }

    private static final class FunctionResultCache<Key_Type, Value_Type> implements Mapping<Key_Type, Value_Type> {

        private final Function<Key_Type, Value_Type> function;

        private FunctionResultCache(Function<Key_Type, Value_Type> function) {
            this.function = function;
        }

        private final Cache<Key_Type, Value_Type> cache = Cache.createIdentityCache();

        public boolean containsKey(Key_Type key) {
            return true;
        }

        public Value_Type get(Key_Type key) {
            Value_Type value = cache.get(key);
            if (value == null) {
                final long milliSecondsBefore = System.currentTimeMillis();
                final Function.Result<Value_Type> result = function.create(key);
                final long costInMilliSeconds = System.currentTimeMillis() - milliSecondsBefore;
                value = result.value;
                cache.put(key, value, result.size, Size.fromLong(costInMilliSeconds));
            }
            return value;
        }

        public int length() {
            return cache.length();
        }

        public IterableWithLength<Key_Type> keys() {
            return cache.keys();
        }

        public IterableWithLength<Value_Type> values() {
            return cache.values();
        }

        public Value_Type put(Key_Type key, Value_Type value) {
            throw new UnsupportedOperationException();
        }

        public Value_Type remove(Key_Type key) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }
    }
}
