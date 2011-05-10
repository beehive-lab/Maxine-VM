/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
/*
 * @Harness: java
 * @Runs: 0 = true
 */
package test.bench.java.util;

import java.util.*;

import test.bench.util.*;

/**
 *
 * Simple test of {@link HashMap#put}. This class is used as a base for {@link SyncHashMap_put01} and {@link ConcHashMap_put01}.
 * It actually inherits from {@link HashMap_get01.Bench} and just overrides the {@link MicroBenchmark#run} method.
 *
 * @author Mick Jordan
 */

public class HashMap_put01  extends RunBench {

    HashMap_put01() {
        super(new Bench());
    }

    public static boolean test(int i) {
        return new HashMap_put01().runBench();
    }

    public static class Bench extends HashMap_get01.Bench {

        protected Bench() {
            super();
        }

        protected Bench(Map<Integer, Integer> map) {
            super(map);
        }

        @Override
        protected void init() {
            // Hashmap_get01 puts the key into the map in this method
            // we do it in the run method and remove it in the postrun method.
        }

        @Override
        public long run() {
            map.put(key, value);
            return defaultResult;
        }

        @Override
        public void postrun() {
            map.remove(key);
        }
    }

    public static void main(String[] args) {
        RunBench.runTest(HashMap_put01.class, args);
    }

}
