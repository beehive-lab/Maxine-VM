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
package test.bench.util;

/**
 * This provides a simple framework for benchmarks that want to do a similar operation
 * in multiple threads, typically for some number of iterations. The threads are created in the
 * {@link Bench#prerun} method, so this is very thread-creation intensive if the number
 * of {@link Bench#run} invocations is high. Typically this class is used in benchmarks
 * that do a lot of iterations in the thread itself.
 *
 * TODO Consider supporting multi-thread benchmarks directly in {@link RunBench), i.e. adding this code.
 *
 * @author Mick Jordan
 */
public class ThreadCounter {

    public static final int DEFAULT_COUNT = 100000;
    public static Barrier startBarrier;

    public static class Bench extends RunBench.MicroBenchmark {

        private RunnerFactory runnerFactory;
        private Thread[] threads;
        private volatile int started;
        protected int threadCount;
        protected long countDown;

        /**
         * Create a multi-threaded benchmark.
         *
         * @param runnerFactory factory that creates the {@link CountingRunner} that does the work of the benchmark in one thread
         * @param threadCount the number of threads to create
         * @param countDown the count; typically the {@link CountingRunner} counts it down to zero then terminates
         */
        public Bench(RunnerFactory runnerFactory, int threadCount, long countDown) {
            this.runnerFactory = runnerFactory;
            this.threadCount = threadCount;
            this.countDown = countDown;
        }

        public Bench(int threadCount, long countDown) {
            this(new EmptyRunnerFactory(), threadCount, countDown);
        }

        @Override
        public void prerun() {
            threads = new Thread[threadCount];
            final Runnable[] counter = new Runnable[threadCount];
            for (int i = 0; i < threadCount; i++) {
                counter[i] = runnerFactory.createRunner(threadCount, countDown);
                threads[i] = new Thread(counter[i]);
            }
            startBarrier = new Barrier(threadCount + 1);
            // start the threads which will wait at the barrier
            for (int i = 0; i < threadCount; i++) {
                threads[i].start();
            }
        }

        @Override
        public long run() {
            startBarrier.waitForRelease();
            for (int i = 0; i < threadCount; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException ex) {
                }
            }
            return defaultResult;
        }
    }

    /**
     * Convenience base class that records thread/count and pre-thread count info.
     */
    public static class BaseRunner {
        protected int threadCount;
        protected long totalCount;
        protected long count;

        protected BaseRunner(int threadCount, long totalCount) {
            this.threadCount = threadCount;
            this.totalCount = totalCount;
            this.count = totalCount / threadCount;
        }
    }

    public abstract static class RunnerFactory {
        public abstract Runnable createRunner(int threadCount, long count);
    }

    public static class EmptyRunner implements Runnable {
        @Override
        public void run() {
            startBarrier.waitForRelease();
        }
    }

    public static class EmptyRunnerFactory extends RunnerFactory {
        @Override
        public Runnable createRunner(int threadCount, long count)  {
            return new EmptyRunner();
        }
    }

}
