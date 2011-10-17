/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.bench.java.util;

import java.util.*;

import test.bench.util.*;

/**
 * Utility class for HashMap benchmarks.
 */

public abstract class HashMapBase  extends RunBench.MicroBenchmark {

    public final Map<Integer, Integer> map;
    public final Random random = new Random(46763);
    public final Integer value = new Integer(0);
    public final Integer key = random.nextInt();

    public HashMapBase() {
        this(new HashMap<Integer, Integer>());
    }

    public HashMapBase(Map<Integer, Integer> map) {
        this.map = map;
        for (int i = 0; i < 1000; i++) {
            map.put(random.nextInt(), value);
        }
    }
}
