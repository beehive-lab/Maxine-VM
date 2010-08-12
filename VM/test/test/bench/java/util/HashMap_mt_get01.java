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
package test.bench.java.util;

import java.util.*;
import test.bench.util.*;

/**
 * Multi-threaded version of {@link HashMap_get01}.
 * This uses the {@link ThreadCounter} framework to spawn multiple threads per invocation of {@link MicroBenchmark#run}.
 * The number of threads and iteration count are passed in as arguments. Each thread performs 1 / numThreads of the iteration count.
 *
 * N.B. Although the thread creation is factored out into the {@link MicroBenchmark#prerun} method, the overhead of synchronizing
 * the threads at the start/end barriers is significant. This should be factored out by the encapsulating benchmark {@link ThreadCounter.Bench}.
 *
 * Note also that results from this variant are not directly comparable to {@link HashMap_get01}. However, assuming that the
 * loopcount for {@link HashMap_get01} is the same as the iteration count for this variant, scaling the operations/ms of this
 * vaariant by loopcount should be comparable.
 *
 * @author Mick Jordan
 */

public class HashMap_mt_get01 extends RunBench {

    protected HashMap_mt_get01(MicroBenchmark bench, int t, long c) {
        super(bench, new ThreadCounter.Bench(t, c));
    }

    public static boolean test(int t, int c) {
        final boolean result = new HashMap_mt_get01(new Bench(t, c), t, c).runBench(true);
        return result;
    }

    public static class Bench extends ThreadCounter.Bench {

        public Bench(int threadCount, long countDown) {
            this(new HashGetRunnerFactory(), threadCount, countDown);
        }

        public Bench(HashGetRunnerFactory factory, int threadCount, long countDown) {
            super(factory, threadCount, countDown);
        }

    }

    public static class HashGetRunner extends ThreadCounter.BaseRunner implements Runnable {
        protected HashMapSetup mapSetup;

        public HashGetRunner(int threadCount, long count) {
            super(threadCount, count);
            mapSetup = new HashMapSetup();
        }

        public HashGetRunner(int threadCount, long count, Map<Integer, Integer> map) {
            super(threadCount, count);
            mapSetup = new HashMapSetup(map);
        }

        @Override
        public void run() {
            ThreadCounter.startBarrier.waitForRelease();
            while (count > 0) {
                mapSetup.map.get(mapSetup.key);
                count--;
            }
        }
    }

    public static class HashGetRunnerFactory extends ThreadCounter.RunnerFactory {

        @Override
        public Runnable createRunner(int threadCount, long count)  {
            return new HashGetRunner(threadCount, count);
        }
    }

    // for running stand-alone
    public static void main(String[] args) {
        RunBench.runTest(HashMap_mt_get01.class, args);
    }
}
