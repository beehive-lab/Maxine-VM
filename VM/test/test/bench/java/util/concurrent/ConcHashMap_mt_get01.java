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
 * @Runs: (1, 100000) = true;
 */
package test.bench.java.util.concurrent;

import java.util.concurrent.*;
import test.bench.util.*;
import test.bench.java.util.*;

/**
 * Variant of {@link Hashmap_mt_get01} that uses a {@link ConcurrentHashMap concurrent map}.
 *
 * @author Mick Jordan
 */

public class ConcHashMap_mt_get01 extends RunBench {

    protected ConcHashMap_mt_get01(MicroBenchmark bench, int t, long c) {
        super(bench, new ThreadCounter.Bench(t, c));
    }

    public static boolean test(int t, int c) {
        final boolean result = new ConcHashMap_mt_get01(new Bench(t, c), t, c).runBench(true);
        return result;
    }

    static class Bench extends HashMap_mt_get01.Bench {
        Bench(int threadCount, long countDown) {
            super(new ConcHashGetRunnerFactory(), threadCount, countDown);
        }

    }

    static class ConcHashGetRunnerFactory extends  HashMap_mt_get01.HashGetRunnerFactory {

        @Override
        public Runnable createRunner(int threadCount, long count) {
            return new HashMap_mt_get01.HashGetRunner(threadCount, count, new ConcurrentHashMap<Integer, Integer>());
        }
    }

    // for running stand-alone
    public static void main(String[] args) {
        RunBench.runTest(ConcHashMap_mt_get01.class, args);
    }
}
