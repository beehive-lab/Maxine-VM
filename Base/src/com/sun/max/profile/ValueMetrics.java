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
package com.sun.max.profile;

import java.util.*;
import java.util.Arrays;

import com.sun.max.lang.*;
import com.sun.max.profile.Metrics.*;
import com.sun.max.program.*;

/**
 * This class is a container for a number of inner classes that support recording
 * metrics about the distribution (i.e. frequency) of values. For example,
 * these utilities can be used to gather the distribution of compilation times,
 * object sizes, heap allocations, etc. Several different distribution implementations
 * are available, with different time and space tradeoffs and different approximations,
 * ranging from exact distributions to only distributions within a limited range.
 *
 * @author Ben L. Titzer
 */
public class ValueMetrics {

    public static final Approximation EXACT = new Approximation();
    public static final Approximation TRACE = new IntegerTraceApproximation(1024);

    public static class Approximation {
    }

    public static class FixedApproximation extends Approximation {
        protected final Object[] _values;
        public FixedApproximation(Object... values) {
            this._values = values.clone();
        }
    }

    public static class IntegerRangeApproximation extends Approximation {
        protected final int _lowValue;
        protected final int _highValue;
        public IntegerRangeApproximation(int low, int high) {
            _lowValue = low;
            _highValue = high;
        }
    }

    public static class IntegerTraceApproximation extends Approximation {
        protected final int _bufferSize;
        public IntegerTraceApproximation(int bufferSize) {
            _bufferSize = bufferSize;
        }
    }

    /**
     * This class and its descendants allow recording of a distribution of integer values.
     * Various implementations implement different approximations with different time and
     * space tradeoffs. Because this class and its descendants record distributions only
     * for integers, it can be more efficient than using boxed (e.g. {@code java.lang.Integer}) values.
     *
     * @author Ben L. Titzer
     */
    public abstract static class IntegerDistribution extends Distribution<Integer> {

        public abstract void record(int value);
    }

    public static class FixedRangeIntegerDistribution extends IntegerDistribution {
        protected final int _lowValue;
        protected final int[] _counts;
        protected int _missed;

        public FixedRangeIntegerDistribution(int low, int high) {
            _lowValue = low;
            _counts = new int[high - low];
        }

        @Override
        public void record(int value) {
            _total++;
            final int index = value - _lowValue;
            if (index >= 0 && index < _counts.length) {
                _counts[index]++;
            } else {
                _missed++;
            }
        }

        @Override
        public int getCount(Integer value) {
            final int index = value - _lowValue;
            if (index >= 0 && index < _counts.length) {
                return _counts[index];
            }
            return _missed > 0 ? -1 : 0;
        }

        @Override
        public Map<Integer, Integer> asMap() {
            final Map<Integer, Integer> map = new HashMap<Integer, Integer>();
            for (int i = 0; i != _counts.length; ++i) {
                map.put(_lowValue + i, _counts[i]);
            }
            return map;
        }
    }

    public static class FixedSetIntegerDistribution extends IntegerDistribution {

        private final int[] _set;
        private final int[] _count;
        protected int _missed;

        public FixedSetIntegerDistribution(int[] set) {
            // TODO: sort() and binarySearch for large sets.
            _set = set.clone();
            _count = new int[set.length];
        }

        @Override
        public void record(int value) {
            _total++;
            for (int i = 0; i < _set.length; i++) {
                if (_set[i] == value) {
                    _count[i]++;
                    return;
                }
            }
            _missed++;
        }

        @Override
        public int getCount(Integer value) {
            final int val = value;
            for (int i = 0; i < _set.length; i++) {
                if (_set[i] == val) {
                    return _count[i];
                }
            }
            return _missed > 0 ? -1 : 0;
        }

        @Override
        public Map<Integer, Integer> asMap() {
            final Map<Integer, Integer> map = new HashMap<Integer, Integer>();
            for (int i = 0; i != _count.length; ++i) {
                map.put(_set[i], _count[i]);
            }
            return map;
        }
    }

    /**
     * The trace integer distribution class collects an exact distribution of integer
     * values by internally recording every integer value in order in a fixed size buffer.
     * When the buffer fills up, its contents are sorted and then reduced into a
     * sorted list of value/count pairs. This results in an exact distribution
     * with a tunable size/performance tradeoff, since a larger buffer means
     * fewer reduction steps.
     * 
     * This implementation is <b>not thread safe</b>. It may lose updates and potentially
     * generate exceptions if used in a multi-threaded scenario. To make updates
     * to this distribution thread safe, {@linkplain ValueMetrics#threadSafe(IntegerDistribution) wrap}
     * it with a {@link ThreadsafeIntegerDistribution}.
     *
     * @author Ben L. Titzer
     */
    public static class TraceIntegerDistribution extends IntegerDistribution {
        private final int[] _buffer;
        private int _cursor;

        private int[] _values;
        private int[] _counts;

        public TraceIntegerDistribution(int bufferSize) {
            assert bufferSize > 0;
            _buffer = new int[bufferSize];
        }

        @Override
        public void record(int value) {
            _total++;
            _buffer[_cursor++] = value;
            if (_cursor == _buffer.length) {
                reduce();
            }
        }

        @Override
        public int getCount(Integer value) {
            reduce();
            final int index = Arrays.binarySearch(_values, value);
            if (index < 0) {
                return 0;
            }
            return _counts[index];
        }

        @Override
        public Map<Integer, Integer> asMap() {
            reduce();
            // TODO: another map implementation might be better.
            final Map<Integer, Integer> map = new HashMap<Integer, Integer>();
            if (_values != null) {
                for (int i = 0; i < _values.length; i++) {
                    map.put(_values[i], _counts[i]);
                }
            }
            return map;
        }

        private void reduce() {
            if (_cursor == 0) {
                // no recorded values to reduce
                return;
            }
            // compression needed.
            Arrays.sort(_buffer, 0, _cursor);
            if (_values != null) {
                // there are already some entries. need to merge counts
                removeExistingValues();
                if (_cursor > 0) {
                    final int[] ovalues = _values;
                    final int[] ocounts = _counts;
                    reduceBuffer(_buffer);
                    mergeValues(ovalues, ocounts, _values, _counts);
                }
            } else {
                // there currently aren't any entry arrays. reduce the buffer to generate the first ones.
                reduceBuffer(_buffer);
            }
            _cursor = 0;
        }

        private void removeExistingValues() {
            // increment counts for values that already occur in the entry arrays
            // and leave leftover values in the buffer
            int valuesPos = 0;
            final int ocursor = _cursor;
            _cursor = 0;
            for (int i = 0; i < ocursor; i++) {
                final int nvalue = _buffer[i];
                while (valuesPos < _values.length && _values[valuesPos] < nvalue) {
                    valuesPos++;
                }
                if (valuesPos < _values.length && _values[valuesPos] == nvalue) {
                    _counts[valuesPos]++;
                } else {
                    // retain the new values in the buffer
                    _buffer[_cursor++] = nvalue;
                }
            }
        }

        private void mergeValues(int[] avalues, int[] acounts, int[] bvalues, int[] bcounts) {
            final int[] nvalues = new int[avalues.length + _values.length];
            final int[] ncounts = new int[acounts.length + _counts.length];

            int a = 0;
            int b = 0;
            int n = 0;
            while (n < nvalues.length) {
                while (a < avalues.length && (b == bvalues.length || avalues[a] < bvalues[b])) {
                    nvalues[n] = avalues[a];
                    ncounts[n] = acounts[a];
                    n++;
                    a++;
                }
                while (b < bvalues.length && (a == avalues.length || bvalues[b] < avalues[a])) {
                    nvalues[n] = bvalues[b];
                    ncounts[n] = bcounts[b];
                    n++;
                    b++;
                }
            }
            assert n == nvalues.length && a == avalues.length && b == bvalues.length;
            _values = nvalues;
            _counts = ncounts;
        }

        private void reduceBuffer(int[] buffer) {
            // count the unique values
            int last1 = buffer[0];
            int unique = 1;
            for (int i1 = 1; i1 < _cursor; i1++) {
                if (_buffer[i1] != last1) {
                    unique++;
                    last1 = _buffer[i1];
                }
            }
            // create the values arrays and populate them
            _values = new int[unique];
            _counts = new int[unique];
            int last = _buffer[0];
            int count = 1;
            int pos = 0;
            for (int i = 1; i < _cursor; i++) {
                if (_buffer[i] != last) {
                    _values[pos] = last;
                    _counts[pos] = count;
                    pos++;
                    count = 1;
                    last = _buffer[i];
                } else {
                    count++;
                }
            }
            assert pos == _values.length - 1;
            _values[pos] = last;
            _counts[pos] = count;
        }
    }

    public static class HashedIntegerDistribution extends IntegerDistribution {
        private final Map<Integer, Distribution> _map = new HashMap<Integer, Distribution>();

        @Override
        public void record(int value) {
            _total++;
            final Integer integer = value;
            Distribution distribution = _map.get(integer);
            if (distribution == null) {
                distribution = new Distribution();
                _map.put(integer, distribution);
            }
            distribution._total++;
        }
        @Override
        public int getCount(Integer value) {
            final Distribution distribution = _map.get(value);
            if (distribution != null) {
                return distribution._total;
            }
            return 0;
        }

        @Override
        public Map<Integer, Integer> asMap() {
            final Map<Integer, Integer> map = new HashMap<Integer, Integer>();
            for (Map.Entry<Integer, Distribution> entry : _map.entrySet()) {
                map.put(entry.getKey(), entry.getValue()._total);
            }
            return map;
        }
    }

    /**
     * This class and its descendants support profiling of individual objects. Reference
     * equality is used here exclusively. Various implementations with different size and space
     * tradeoffs offer various levels of accuracy.
     *
     * @author Ben L. Titzer
     */
    public abstract static class ObjectDistribution<Value_Type> extends Distribution<Value_Type> {
        public abstract void record(Value_Type value);
    }

    public static class HashedObjectDistribution<Value_Type> extends ObjectDistribution<Value_Type> {
        private final Map<Value_Type, Distribution> _map;

        public HashedObjectDistribution() {
            _map = new IdentityHashMap<Value_Type, Distribution>();
        }

        @Override
        public void record(Value_Type value) {
            _total++;
            Distribution distribution = _map.get(value);
            if (distribution == null) {
                distribution = new Distribution();
                _map.put(value, distribution);
            }
            distribution._total++;
        }
        @Override
        public int getCount(Value_Type value) {
            final Distribution distribution = _map.get(value);
            if (distribution != null) {
                return distribution._total;
            }
            return 0;
        }

        @Override
        public Map<Value_Type, Integer> asMap() {
            final Map<Value_Type, Integer> map = new IdentityHashMap<Value_Type, Integer>();
            for (Map.Entry<Value_Type, Distribution> entry : _map.entrySet()) {
                map.put(entry.getKey(), entry.getValue()._total);
            }
            return map;
        }
    }

    public static class FixedSetObjectDistribution<Value_Type> extends ObjectDistribution<Value_Type> {

        private final Value_Type[] _set;
        private final int[] _count;
        private int _missed;

        public FixedSetObjectDistribution(Value_Type[] set) {
            _set = set.clone();
            _count = new int[set.length];
        }

        @Override
        public void record(Value_Type value) {
            _total++;
            for (int i = 0; i < _set.length; i++) {
                if (_set[i] == value) {
                    _count[i]++;
                    return;
                }
            }
            _missed++;
        }
        @Override
        public int getCount(Value_Type value) {
            for (int i = 0; i < _set.length; i++) {
                if (_set[i] == value) {
                    return _count[i];
                }
            }
            return _missed > 0 ? -1 : 0;
        }

        @Override
        public Map<Value_Type, Integer> asMap() {
            final Map<Value_Type, Integer> map = new IdentityHashMap<Value_Type, Integer>();
            for (int i = 0; i != _count.length; ++i) {
                map.put(_set[i], _count[i]);
            }
            return map;
        }
    }

    /**
     * This method creates a new distribution for the occurrence of integer values.
     * 
     * @param name the name of the metric; if non-null, then a global metric of the specified
     * name will be returned {@see GlobalMetrics}
     * @param approx the approximation level that is requested. Different approximation levels
     * have different time and space tradeoffs versus accuracy.
     * @return a new integer distribution object that can be used to record occurrences of integers
     */
    public static IntegerDistribution newIntegerDistribution(String name, Approximation approx) {
        if (name != null) {
            final IntegerDistribution prev = GlobalMetrics.getMetric(name, IntegerDistribution.class);
            if (prev != null) {
                return prev;
            }
            return GlobalMetrics.setMetric(name, IntegerDistribution.class, createIntegerDistribution(approx));
        }
        return createIntegerDistribution(approx);
    }

    private static IntegerDistribution createIntegerDistribution(Approximation approx) throws ProgramError {
        if (approx instanceof FixedApproximation) {
            final FixedApproximation fixedApprox = (FixedApproximation) approx;
            final int[] values = new int[fixedApprox._values.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = (Integer) fixedApprox._values[i];
            }
            return new FixedSetIntegerDistribution(values);
        }
        if (approx instanceof IntegerRangeApproximation) {
            final IntegerRangeApproximation fixedApprox = (IntegerRangeApproximation) approx;
            return new FixedRangeIntegerDistribution(fixedApprox._lowValue, fixedApprox._highValue);
        }
        if (approx == EXACT) {
            return new HashedIntegerDistribution();
        }
        if (approx instanceof IntegerTraceApproximation) {
            final IntegerTraceApproximation traceApprox = (IntegerTraceApproximation) approx;
            return new TraceIntegerDistribution(traceApprox._bufferSize);
        }
        return new HashedIntegerDistribution();
    }

    /**
     * This is a utility method to create an integer distribution over a range of specified integers.
     * 
     * @param name the name of the metric
     * @param low the lowest value to be recorded in the range (inclusive)
     * @param high the highest value to be recorded (inclusive)
     * @return a new integer distribution object that will record exact profiles for the integers in the
     * specified range
     */
    public static IntegerDistribution newIntegerDistribution(String name, int low, int high) {
        return newIntegerDistribution(name, new IntegerRangeApproximation(low, high));
    }

    /**
     * This utility method creates a new integer distribution with the {@link #EXACT} approximation.
     * Note that the implementation of this integer distribution may consume excessive time and/or
     * space for unstructured distributions.
     * 
     * @param name the name of the metric; if non-null, then a shared, global metric of the specified name will be returned
     * @return a new integer distribution that records an exact profile
     */
    public static IntegerDistribution newIntegerDistribution(String name) {
        return newIntegerDistribution(name, EXACT);
    }

    /**
     * This utility method creates an integer distribution that records only the specified set of
     * values, with all other values being not recorded.
     * 
     * @param name the name of the metric; if non-null, then a shared, global metric of the specified name will be returned
     * @param values the set of integer values to be recored
     * @return a new integer distribution recorder
     */
    public static IntegerDistribution newIntegerDistribution(String name, int[] values) {
        final Object[] vals = new Object[values.length];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = values[i];
        }
        return newIntegerDistribution(name, new FixedApproximation(vals));
    }

    /**
     * This method creates a new distribution capable of recording individual objects.
     * 
     * @param <Value_Type> the type of objects being profiled
     * @param name the name of the metric; if non-null, then a shared, global metric of the specified name will be
     * returned
     * @param approx the approximation for the distribution
     * @return a new distribution capable of profiling the occurrence of objects
     */
    public static <Value_Type> ObjectDistribution<Value_Type> newObjectDistribution(String name, Approximation approx) {
        if (name != null) {
            final ObjectDistribution<Value_Type> prev = StaticLoophole.cast(GlobalMetrics.getMetric(name, ObjectDistribution.class));
            if (prev != null) {
                return prev;
            }
            return StaticLoophole.cast(GlobalMetrics.setMetric(name, ObjectDistribution.class, createObjectDistribution(approx)));
        }
        return createObjectDistribution(approx);
    }

    private static <Value_Type> ObjectDistribution<Value_Type> createObjectDistribution(Approximation approx) {
        if (approx instanceof FixedApproximation) {
            final FixedApproximation fixedApprox = (FixedApproximation) approx;
            final Value_Type[] values = StaticLoophole.cast(fixedApprox._values);
            return new FixedSetObjectDistribution<Value_Type>(values);
        }
        if (approx == EXACT) {
            return new HashedObjectDistribution<Value_Type>();
        }
        // default is to use the hashed object distribution
        return new HashedObjectDistribution<Value_Type>();
    }

    /**
     * This is a utility method to create a new object distribution that only records occurrences of
     * objects in the specified set.
     * 
     * @param <Value_Type> the type of the objects being profiled
     * @param name the name of the metric
     * @param set the set of objects for which to record exact profiling information
     * @return a new distribution capable of producing an exact profile of the occurence of the specified objects
     */
    public static <Value_Type> ObjectDistribution<Value_Type> newObjectDistribution(String name, Value_Type... set) {
        return newObjectDistribution(name, new FixedApproximation(set));
    }

    /**
     * This is utility method to create a new object distribution with an exact profile.
     * 
     * @param <Value_Type> the type of the objects being profiled
     * @param name the name of metric
     * @return a new distribution capable of producing an exact profile of the occurrences of all of the specified
     * objects.
     */
    public static <Value_Type> ObjectDistribution<Value_Type> newObjectDistribution(String name) {
        return newObjectDistribution(name, EXACT);
    }

    public static class ThreadsafeIntegerDistribution extends IntegerDistribution {

        private final IntegerDistribution _distribution;

        public ThreadsafeIntegerDistribution(IntegerDistribution distribution) {
            this._distribution = distribution;
        }
        @Override
        public void record(int value) {
            synchronized (_distribution) {
                _distribution.record(value);
            }
        }
        @Override
        public int getCount(Integer value) {
            synchronized (_distribution) {
                return _distribution.getCount(value);
            }
        }

        @Override
        public Map<Integer, Integer> asMap() {
            synchronized (_distribution) {
                return _distribution.asMap();
            }
        }
    }

    private static class ThreadsafeObjectDistribution<Value_Type> extends ObjectDistribution<Value_Type> {

        private final ObjectDistribution<Value_Type> _distribution;

        ThreadsafeObjectDistribution(ObjectDistribution<Value_Type> distribution) {
            this._distribution = distribution;
        }
        @Override
        public void record(Value_Type value) {
            synchronized (_distribution) {
                _distribution.record(value);
            }
        }
        @Override
        public int getCount(Value_Type value) {
            synchronized (_distribution) {
                return _distribution.getCount(value);
            }
        }

        @Override
        public Map<Value_Type, Integer> asMap() {
            synchronized (_distribution) {
                return _distribution.asMap();
            }
        }
    }

    /**
     * This method creates a wrapper around the specified integer distribution that ensures
     * access to the distribution is synchronized.
     * 
     * @param distribution the distribution to wrap in a synchronization
     * @return a synchronized view of the distribution
     */
    public static IntegerDistribution threadSafe(IntegerDistribution distribution) {
        return new ThreadsafeIntegerDistribution(distribution);
    }

    /**
     * This method creates a wrapper around the specified integer distribution that ensures
     * access to the distribution is synchronized.
     * 
     * @param distribution the distribution to wrap in a synchronization
     * @return a synchronized view of the distribution
     */
    public static <Value_Type> ObjectDistribution<Value_Type> threadSafe(ObjectDistribution<Value_Type> distribution) {
        return new ThreadsafeObjectDistribution<Value_Type>(distribution);
    }
}
