/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.max.vm.compiler;

import static com.sun.max.vm.VMOptions.*;

import java.util.*;

import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.Log;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.ti.*;

/**
 * This class implements a thread pool that maintains a variable number of compilation threads.
 */
public class CompilationThreadPool {

    /**
     * A queue of pending compilations.
     */
    private final LinkedList<Compilation> pending = new LinkedList<Compilation>();

    private CompilationThread[] threadPool;

    /**
     * Default size of compilation thread pool.  
     */
    private static int CTPS = 4;

    private static boolean GCOnRecompilation;

    static {
        addFieldOption("-XX:", "GCOnRecompilation", CompilationThreadPool.class, "Force GC before every re-compilation.");
        addFieldOption("-XX:", "CTPS", CompilationThreadPool.class, "Compilation threadpool size (Default: 4)");
    }

    public CompilationThreadPool() {
        threadPool = new CompilationThread[CTPS];
        for (int i = 0; i < CTPS; i++) {
            threadPool[i] = new CompilationThread();
        }
    }

    public void setDaemon(boolean on) {
        for (int i = 0; i < CTPS; i++) {
            threadPool[i].setDaemon(on);
        }
    }

    public void startThreads() {
        for (int i = 0; i < CTPS; i++) {
            threadPool[i].start();
        }
    }

    public void addCompilationToQueue(Compilation compilation) {
        synchronized (pending) {
            pending.add(compilation);
            pending.notify();
        }
    }

    /**
     * This class implements a daemon thread that performs compilations in the background. Depending on the compiler
     * configuration, multiple compilation threads may be working in parallel.
     */
    protected class CompilationThread extends Thread {

        protected CompilationThread() {
            super("compile");
        }

        /**
         * The current compilation being performed by this thread.
         */
        Compilation compilation;

        /**
         * Continuously polls the compilation queue for work, performing compilations as they are removed from the
         * queue.
         */
        @Override
        public void run() {
            while (true) {
                try {
                    compileOne();
                } catch (InterruptedException e) {
                    // do nothing.  
                } catch (Throwable t) {
                    logCompilationError(compilation.classMethodActor, t);
                }
            }
        }

        /**
         * Polls the compilation queue and performs a single compilation.
         * @throws InterruptedException if the thread was interrupted waiting on the queue
         */
        void compileOne() throws InterruptedException {
            compilation = null;
            synchronized (pending) {
                while (compilation == null) {
                    compilation = pending.poll();
                    if (compilation == null) {
                        pending.wait();
                    }
                }
            }
            compilation.compilingThread = Thread.currentThread();
            if (GCOnRecompilation) {
                System.gc();
            }
            TargetMethod tm = compilation.compile();
            VMTI.handler().methodCompiled(tm.classMethodActor);
        }
    }

    private void logCompilationError(ClassMethodActor cma, Throwable t) {
        if (VMOptions.verboseOption.verboseCompilation) {
            boolean lockDisabledSafepoints = Log.lock();
            Log.printCurrentThread(false);
            Log.println(": Exception during compilation of " + cma);
            t.printStackTrace(Log.out);
            Log.unlock(lockDisabledSafepoints);
        }
    }
}

