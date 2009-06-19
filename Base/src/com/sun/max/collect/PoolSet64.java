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
 * A compact and efficient implementation of a {@link PoolSet} for pools with 64 objects or less.
 *
 * While it's possible to create an instance of this class directly, the preferred way to create a pool set is described
 * {@linkplain PoolSet here}.
 *
 * @author Doug Simon
 */
public class PoolSet64<PoolObject_Type extends PoolObject> extends PoolSet<PoolObject_Type> {

    public static final int MAX_POOL_SIZE = 64;

    /**
     * Bit vector representation of this set. The 2^k bit indicates the presence of _pool.get(k) in this set.
     */
    private long set;

    /**
     * Creates an empty pool set for a pool with 64objects or less.
     */
    public PoolSet64(Pool<PoolObject_Type> pool) {
        super(pool);
        assert pool.length() <= MAX_POOL_SIZE : pool.length() + " > " + MAX_POOL_SIZE;
    }

    @Override
    public int length() {
        return Long.bitCount(set);
    }

    @Override
    public void clear() {
        set = 0L;
    }

    @Override
    public boolean isEmpty() {
        return set == 0L;
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
        assert pool.get(serial) == value;
        return (serialToBit(serial) & set) != 0;
    }

    @Override
    public void add(PoolObject_Type value) {
        final int serial = value.serial();
        assert pool.get(serial) == value;
        set |= serialToBit(serial);
    }

    @Override
    public PoolSet64<PoolObject_Type> addAll() {
        final int poolLength = pool.length();
        if (poolLength != 0) {
            final long highestBit = 1L << poolLength - 1;
            set = highestBit | (highestBit - 1);
            assert length() == poolLength;
        }
        return this;
    }

    @Override
    public void or(PoolSet<PoolObject_Type> others) {
        if (others instanceof PoolSet64) {
            final PoolSet64 poolSet64 = (PoolSet64) others;
            set |= poolSet64.set;
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
            assert pool.get(serial) == value;
            final long bit = serialToBit(serial);
            final boolean present = (set & bit) != 0;
            set &= ~bit;
            return present;
        }
        return false;
    }

    @Override
    public PoolObject_Type removeOne() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        final long bit = Long.lowestOneBit(set);
        set &= ~bit;
        return pool.get(bitToSerial(bit));
    }

    @Override
    public void and(PoolSet<PoolObject_Type> others) {
        if (others instanceof PoolSet64) {
            final PoolSet64 poolSet64 = (PoolSet64) others;
            set &= poolSet64.set;
        } else {
            long oldSet = this.set;
            while (oldSet != 0) {
                final long bit = Long.lowestOneBit(oldSet);
                final int serial = bitToSerial(bit);
                oldSet &= ~bit;
                if (!others.contains(pool.get(serial))) {
                    this.set &= ~bit;
                }
            }
        }
    }

    @Override
    public boolean containsAll(PoolSet<PoolObject_Type> others) {
        if (others instanceof PoolSet64) {
            final PoolSet64 poolSet64 = (PoolSet64) others;
            return (set & poolSet64.set) == poolSet64.set;
        }
        return super.containsAll(others);
    }

    @Override
    public PoolSet<PoolObject_Type> clone() {
        final PoolSet64<PoolObject_Type> clone = new PoolSet64<PoolObject_Type>(pool);
        clone.set = set;
        return clone;
    }

    /**
     * Gets an iterator over all the values in this set.
     */
    public Iterator<PoolObject_Type> iterator() {
        return new Iterator<PoolObject_Type>() {

            private long current = set;
            private long currentBit = -1L;
            private long nextSetBit = Long.lowestOneBit(set);

            public boolean hasNext() {
                return current != 0;
            }

            public PoolObject_Type next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                currentBit = Long.lowestOneBit(current);
                final int serial = bitToSerial(currentBit);
                current &= ~currentBit;
                return pool.get(serial);
            }

            public void remove() {
                if (currentBit == -1L) {
                    throw new IllegalStateException();
                }

                set &= ~currentBit;
                currentBit = -1;
            }
        };
    }
}
