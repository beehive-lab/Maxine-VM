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
package com.sun.max.util;

import java.util.concurrent.*;

import com.sun.max.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * Compute given functions in always one and the same single thread.
 *
 * This is for example necessary in Linux where one and
 * the same thread needs to be reused to represent the whole process in certain contexts,
 * e.g. when using the ptrace interface.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class SingleThread extends Thread {

    private static Thread worker;
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable runnable) {
            ProgramError.check(worker == null, "Single worker thread died unexpectedly");
            worker = new Thread(runnable);
            worker.setName("SingleThread");
            return worker;
        }
    });

    private static final boolean disabled = false;

    public static <V> V execute(Function<V> function) {
        if (disabled || Thread.currentThread() == worker) {
            try {
                return function.call();
            } catch (Exception exception) {
                ProgramError.unexpected(exception);
            }
        }
        synchronized (executorService) {
            final Future<V> future = executorService.submit(function);
            while (true) {
                try {
                    return future.get();
                } catch (ExecutionException e) {
                    throw Utils.cast(RuntimeException.class, e.getCause());
                } catch (InterruptedException exception) {
                    // continue
                }
            }
        }
    }

    public static <V> V executeWithException(Function<V> function) throws Exception {
        if (disabled || Thread.currentThread() == worker) {
            return function.call();
        }
        synchronized (executorService) {
            final Future<V> future = executorService.submit(function);
            while (true) {
                try {
                    return future.get();
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof Exception) {
                        throw (Exception) cause;
                    }
                    ProgramError.unexpected(cause);
                } catch (InterruptedException exception) {
                    // continue
                }
            }
        }
    }

    public static void execute(final Runnable runnable) {
        if (disabled || Thread.currentThread() == worker) {
            runnable.run();
        }
        synchronized (executorService) {
            final Future<?> future = executorService.submit(runnable);
            while (true) {
                try {
                    future.get();
                    return;
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause();
                    ProgramError.unexpected(cause);

                } catch (InterruptedException exception) {
                    // continue
                }
            }
        }
    }
}
