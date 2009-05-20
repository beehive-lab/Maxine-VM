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
package com.sun.max.collect;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;

/**
 * A compact and efficient implementation of a {@link PoolSet} for pools with 128 objects or less.
 *
 * While it's possible to create an instance of this class directly, the preferred way to create a pool set is described
 * {@linkplain PoolSet here}.
 *
 * @author Doug Simon
 */
public class PoolSet128<PoolObject_Type extends PoolObject> extends PoolSet<PoolObject_Type> {

    public static final int MAX_POOL_SIZE = 128;

    /**
     * Bit vector representation of the low part of this set. The 2^k bit indicates the presence of _pool.get(k) in this set.
     */
    private long _setLow;

    /**
     * Bit vector representation of the low part of this set. The 2^k bit indicates the presence of _pool.get(k + 64) in this set.
     */
    private long _setHigh;

    /**
     * Creates an empty pool set for a pool with 128 objects or less.
     */
    public PoolSet128(Pool<PoolObject_Type> pool) {
        super(pool);
        assert pool.length() <= MAX_POOL_SIZE : pool.length() + " > " + MAX_POOL_SIZE;
    }

    @Override
    public int length() {
        return Long.bitCount(_setLow) + Long.bitCount(_setHigh);
    }

    @Override
    public void clear() {
        _setLow = 0L;
        _setHigh = 0L;
    }

    @Override
    public boolean isEmpty() {
        return _setLow == 0L && _setHigh == 0L;
    }

    @INLINE
    private static int bitToSerial(long bit) {
        assert bit != 0 && Longs.isPowerOfTwoOrZero(bit);
        return Long.numberOfTrailingZeros(bit);
    }

    @INLINE
    private static long serialToBit(int serial) {
        assert serial >= 0 && serial < MAX_POOL_SIZE;
        return 1L << serial;
    }

    @Override
    public boolean contains(PoolObject_Type value) {
        if (value == null) {
            return false;
        }

        final int serial = value.serial();
        assert _pool.get(serial) == value;
        if (serial < 64) {
            return (serialToBit(serial) & _setLow) != 0;
        }
        return (serialToBit(serial - 64) & _setHigh) != 0;
    }

    @Override
    public void add(PoolObject_Type value) {
        final int serial = value.serial();
        assert _pool.get(serial) == value;
        if (serial < 64) {
            _setLow |= serialToBit(serial);
        } else {
            _setHigh |= serialToBit(serial - 64);
        }
    }

    @Override
    public PoolSet128<PoolObject_Type> addAll() {
        final int poolLength = _pool.length();
        if (poolLength == 0) {
            return this;
        }
        if (poolLength <= 64) {
            final long highestBit = 1L << poolLength - 1;
            _setLow = highestBit | (highestBit - 1);
        } else {
            final long highestBit = 1L << (poolLength - 64) - 1;
            _setHigh = highestBit | (highestBit - 1);
            _setLow = -1L;
        }
        assert length() == poolLength;
        return this;
    }

    @Override
    public void or(PoolSet<PoolObject_Type> others) {
        if (others instanceof PoolSet128) {
            final PoolSet128 poolSet128 = (PoolSet128) others;
            _setLow |= poolSet128._setLow;
            _setHigh |= poolSet128._setHigh;
        } else {
            if (!others.isEmpty()) {
                for (PoolObject_Type element : others) {
                    add(element);
                }
            }
        }
    }

    @Override
    public boolean remove(PoolObject_Type value) {
        if (!isEmpty()) {
            final int serial = value.serial();
            assert _pool.get(serial) == value;
            final boolean present;
            if (serial < 64) {
                final long bit = serialToBit(serial);
                present = (bit & _setLow) != 0;
                _setLow &= ~bit;
            } else {
                final long bit = serialToBit(serial - 64);
                present = (bit & _setHigh) != 0;
                _setHigh &= ~bit;
            }
            return present;
        }
        return false;
    }

    @Override
    public PoolObject_Type removeOne() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        long bit = Long.lowestOneBit(_setLow);
        if (bit != 0) {
            _setLow &= ~bit;
            return _pool.get(bitToSerial(bit));
        }
        bit = Long.lowestOneBit(_setHigh);
        assert bit != 0;
        _setHigh &= ~bit;
        return _pool.get(bitToSerial(bit) + 64);
    }

    @Override
    public void and(PoolSet<PoolObject_Type> others) {
        if (others instanceof PoolSet128) {
            final PoolSet128 poolSet128 = (PoolSet128) others;
            _setLow &= poolSet128._setLow;
            _setHigh &= poolSet128._setHigh;
        } else {
            long setLow = _setLow;
            while (setLow != 0) {
                final long bit = Long.lowestOneBit(setLow);
                final int serial = bitToSerial(bit);
                setLow &= ~bit;
                if (!others.contains(_pool.get(serial))) {
                    _setLow &= ~bit;
                }
            }
            long setHigh = _setHigh;
            while (setHigh != 0) {
                final long bit = Long.lowestOneBit(setHigh);
                final int serial = bitToSerial(bit) + 64;
                setHigh &= ~bit;
                if (!others.contains(_pool.get(serial))) {
                    _setHigh &= ~bit;
                }
            }
        }
    }

    @Override
    public boolean containsAll(PoolSet<PoolObject_Type> others) {
        if (others instanceof PoolSet128) {
            final PoolSet128 poolSet128 = (PoolSet128) others;
            return (_setLow & poolSet128._setLow) == poolSet128._setLow &&
                   (_setHigh & poolSet128._setHigh) == poolSet128._setHigh;
        }
        return super.containsAll(others);
    }

    @Override
    public PoolSet<PoolObject_Type> clone() {
        final PoolSet128<PoolObject_Type> poolSet = new PoolSet128<PoolObject_Type>(_pool);
        poolSet._setLow = _setLow;
        poolSet._setHigh = _setHigh;
        return poolSet;
    }

    /**
     * Gets an iterator over all the values in this set.
     */
    public Iterator<PoolObject_Type> iterator() {
        return new Iterator<PoolObject_Type>() {

            private int _count = length();
            private boolean _inHighSet;
            private long _current = _setLow;
            private long _currentBit = -1L;
            private long _nextSetBit = Long.lowestOneBit(_setLow);

            public boolean hasNext() {
                return _count != 0;
            }

            public PoolObject_Type next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                _currentBit = Long.lowestOneBit(_current);
                if (_currentBit == 0) {
                    assert !_inHighSet;
                    _inHighSet = true;
                    _current = _setHigh;
                    _currentBit = Long.lowestOneBit(_current);
                }

                final int serial = bitToSerial(_currentBit);
                _current &= ~_currentBit;
                _count--;
                if (_inHighSet) {
                    return _pool.get(serial + 64);
                }
                return _pool.get(serial);
            }

            public void remove() {
                if (_currentBit == -1L) {
                    throw new IllegalStateException();
                }
                if (_inHighSet) {
                    _setHigh &= ~_currentBit;
                } else {
                    _setLow &= ~_currentBit;
                }
                _currentBit = -1;
            }
        };
    }
}
