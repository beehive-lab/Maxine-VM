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
/*
 * @Harness: java
 * @Runs: 0 = true
 */
package test.bench.java.util;

import java.util.*;

/**
 * Simple test of {@link HashMap#get}. This class is used as a base for {@link SyncHashMap_get01} and {@link ConcHashMap_get01}.
 * To simplify support for the multi-threaded variants we delegate rather than inherit the shared hashmap setup code. This does
 * result in one extra object indirection, but it is consistent across all versions.
 *
 * @author Mick Jordan
 */

import test.bench.util.*;

public class HashMap_get01 extends RunBench {
    HashMap_get01() {
        super(new Bench());
    }

    public static boolean test(int i) {
        return new HashMap_get01().runBench(true);
    }

    public static class Bench extends MicroBenchmark {
        protected HashMapSetup mapSetup;
        protected Bench() {
            mapSetup = new HashMapSetup();
            init();
        }

        protected Bench(Map<Integer, Integer> map) {
            mapSetup = new HashMapSetup(map);
            init();
        }

        protected void init() {
            mapSetup.map.put(mapSetup.key, mapSetup.value);
        }

        @Override
        public long run() {
            return mapSetup.map.get(mapSetup.key);
        }

    }

    public static void main(String[] args) {
        RunBench.runTest(HashMap_get01.class, args);
    }

}
