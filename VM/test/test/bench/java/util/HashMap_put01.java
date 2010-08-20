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
