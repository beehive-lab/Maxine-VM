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
/*VCSID=c3ee7fcb-60fe-4505-930f-8051f66bfbf6*/
package com.sun.max.util;

import java.io.*;
import java.util.*;
import java.util.Map.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * This is a class that provides support for timing computations (at millisecond granularity),
 * including nested computations. The timing data gathered for each computation includes
 * both flat time (time not spent inside timed inner computations) and total time.
 *
 * The {@link NanoTimer} class provides similiar functionality but at nanosecond
 * granularity and no support for total times.
 * 
 * @author  Doug Simon
 */
public final class Timer<Key_Type> {

    /**
     * A computation to be timed that does not throw a checked exception.
     */
    public abstract static class Computation<Result_Type> {
        /**
         * Performs the computation that will be timed.
         *
         * @return   the result of the computation
         */
        public abstract Result_Type run();
    }

    /**
     * A computation to be timed that throws an exception.
     */
    public abstract static class ComputationWithException<Result_Type, Exception_Type extends Throwable> {

        protected final Class<? extends Throwable> _exceptionType;

        public ComputationWithException(Class<? extends Throwable> exceptionType) {
            _exceptionType = exceptionType;
        }

        /**
         * Performs the computation that will be timed.
         *
         * @return   the result of the computation
         */
        public abstract Result_Type run() throws Exception_Type;
    }

    /**
     * An instance of {@code Execution} encapsulates the state of a
     * computation including its duration, result and any exception thrown.
     */
    static class Execution<Result_Type, Exception_Type> {
        /**
         * The duration of the computation inclusing any nested compuatutions.
         */
        protected long _nestedTimes;

        /**
         * The result of the computation.
         */
        protected Result_Type _result;

        /**
         * The exception (if any) thrown by the computation.
         */
        protected Exception_Type _exception;
    }

    /**
     * The collected flat times.
     */
    private final Map<Key_Type, Long> _flatTimes = new HashMap<Key_Type, Long>();

    /**
     * The collected nested times.
     */
    private final Map<Key_Type, Long> _totalTimes = new HashMap<Key_Type, Long>();

    /**
     * A stack to model the nesting of computations.
     */
    private final Stack<Execution> _executions = new Stack<Execution>();

    /**
     * Executes a computation.
     *
     * @param   id  the identifier of the computation
     * @param   c   the {@code Computation} or
     *              {@code ComputationException} instance representing
     *              the computation to be executed
     * @return  the dynamic state of the computation's execution
     */
    private <Result_Type, Exception_Type extends Throwable> Execution<Result_Type, Exception_Type> execute(Key_Type id, Object c) {
        final long start = System.currentTimeMillis();
        final Execution<Result_Type, Exception_Type> e = new Execution<Result_Type, Exception_Type>();
        _executions.push(e);
        final Long currentTotal = _totalTimes.get(id);
        try {
            if (c instanceof Computation) {
                final Class<Computation<Result_Type>> type = null;
                e._result = StaticLoophole.cast(type, c).run();
            } else {
                final Class<ComputationWithException<Result_Type, Exception_Type>> type = null;
                e._result = StaticLoophole.cast(type, c).run();
            }
        } catch (Throwable ex) {
            if (c instanceof ComputationWithException) {
                final Class<ComputationWithException<Result_Type, Exception_Type>> type = null;
                final Class<? extends Throwable> exceptionType = StaticLoophole.cast(type, c)._exceptionType;
                if (ex instanceof Error) {
                    throw (Error) ex;
                }
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }
                if (exceptionType.isInstance(ex)) {
                    final Class<Exception_Type> t = null;
                    e._exception = StaticLoophole.cast(t, ex);
                } else {
                    throw new RuntimeException("computation threw " + ex.getClass().getName() + ", expected " + exceptionType.getName(), ex);
                }
            } else {
                // Must be an unchecked exception (RuntimeException or Error subclass)
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }
                if (ex instanceof Error) {
                    throw (Error) ex;
                }
                ProgramError.unexpected();
            }
        } finally {
            _executions.pop();
            final long time = System.currentTimeMillis() - start;
            if (!_executions.isEmpty()) {
                final Execution execution = _executions.peek();
                execution._nestedTimes += time;
            }
            _totalTimes.put(id, time + (currentTotal == null ? 0L : currentTotal));

            final Long flatTime = _flatTimes.get(id);
            if (flatTime == null) {
                _flatTimes.put(id, time - e._nestedTimes);
            } else {
                _flatTimes.put(id, flatTime + (time - e._nestedTimes));
            }
        }
        return e;
    }

    /**
     * Time a specified computation denoted by a specified identifier. The
     * time taken to perform the computation is added to the accumulative
     * time to perform all computations with the same identifier.
     *
     * @param   id           the identifier for the computation
     * @param   computation  the computation to be performed and timed
     * @return  the result of the computation
     */
    public <Result_Type> Result_Type time(Key_Type id, Computation<Result_Type> computation) {
        final Execution<Result_Type, Throwable> e = execute(id, computation);
        if (e._exception != null) {
            if (e._exception instanceof RuntimeException) {
                throw (RuntimeException) e._exception;
            }
            assert e._exception instanceof Error;
            throw (Error) e._exception;
        }
        return e._result;
    }

    /**
     * Time a specified computation denoted by a specified identifier. The
     * time taken to perform the computation is added to the accumulative
     * time to perform all computations with the same identifier.
     *
     * @param   id           the identifier for the computation
     * @param   computation  the computation to be performed and timed
     * @return  the result of the computation.
     */
    public <Result_Type, Exception_Type extends Throwable> Result_Type time(Key_Type id, ComputationWithException<Result_Type, Exception_Type> computation) throws Exception_Type {
        final Execution<Result_Type, Exception_Type> e = execute(id, computation);
        if (e._exception != null) {
            throw e._exception;
        }
        return e._result;
    }

    /**
     * Gets an iterator over the identifiers of computations for which
     * times were collected.
     *
     * @return  an iterator over the identifiers of computations for which
     *          times were collected
     */
    public Iterable<Key_Type> computations() {
        return _flatTimes.keySet();
    }

    /**
     * Gets an iterator over the collected flat times.
     *
     * @return  an iterator over the collected flat times
     */
    public Iterable<Entry<Key_Type, Long>> flatTimes() {
        return _flatTimes.entrySet();
    }

    /**
     * Gets an iterator over the collected accumulative times.
     *
     * @return  an iterator over the collected accumulative times
     */
    public Iterable<Entry<Key_Type, Long>> totalTimes() {
        return _totalTimes.entrySet();
    }

    /**
     * Resets all the data gathered by the timer.
     *
     * @throws IllegalStateException if there is an execution currently being timed
     */
    public void reset() {
        if (!_executions.isEmpty()) {
            throw new IllegalStateException();
        }
        _flatTimes.clear();
        _totalTimes.clear();
    }

    /**
     * Returns a string representation of the times accumulated by the timer
     * in the form of a set of entries, enclosed in braces and separated
     * by the ASCII characters ", " (comma and space). Each entry is rendered
     * as the computation identifier, a colon sign ':', the total time
     * associated with the computation, a colon sign ':' and the flat time
     * associated with the computation.
     *
     * @return a string representation of the collected times
     */
    public String timesAsString() {
        final StringBuilder sb = new StringBuilder("{ ");
        final Iterator<Key_Type> keys = _flatTimes.keySet().iterator();
        final Iterator<Long> ftimes = _flatTimes.values().iterator();
        final Iterator<Long> ttimes = _totalTimes.values().iterator();
        while (keys.hasNext()) {
            sb.append(keys.next()).append(":").append(ttimes.next()).append(":").append(ftimes.next());
            if (keys.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.append(" }").toString();
    }

    /**
     * Print a summary of the times.
     *
     * @param out PrintStream
     */
    public void dump(PrintStream out) {
        out.println("Times: flat | total | computation");
        final Iterator<Key_Type> keys = _flatTimes.keySet().iterator();
        final Iterator<Long> ftimes = _flatTimes.values().iterator();
        final Iterator<Long> ttimes = _totalTimes.values().iterator();
        while (keys.hasNext()) {
            out.println("" + ftimes.next() + '\t' + ttimes.next() + '\t' + keys.next());
        }
    }
}
