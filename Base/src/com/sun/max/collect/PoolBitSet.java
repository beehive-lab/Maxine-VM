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

/**
 * An implementation of a {@link PoolSet} based on a {@link BitSet}.
 *
 * While it's possible to create an instance of this class directly, the preferred way to create a pool set is described
 * {@linkplain PoolSet here}.
 *
 * @author Doug Simon
 */
public class PoolBitSet<T extends PoolObject> extends PoolSet<T> {

    private final BitSet set;

    /**
     * Creates an empty pool bit set.
     */
    public PoolBitSet(Pool<T> pool) {
        super(pool);
        set = new BitSet(pool.length());
    }

    /**
     * Constructor to be used by subclasses for implementing {@link #clone()}.
     */
    protected PoolBitSet(PoolBitSet<T> toBeCloned) {
        super(toBeCloned.pool());
        set = (BitSet) toBeCloned.set.clone();
    }

    @Override
    public boolean contains(T value) {
        if (value == null) {
            return false;
        }
        assert pool.get(value.serial()) == value;
        return set.get(value.serial());
    }

    @Override
    public int size() {
        return set.cardinality();
    }

    @Override
    public void clear() {
        set.clear();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public void add(T value) {
        assert pool.get(value.serial()) == value;
        set.set(value.serial());
    }

    @Override
    public PoolBitSet<T> addAll() {
        set.set(0, pool.length());
        return this;
    }

    @Override
    public void or(PoolSet<T> others) {
        if (others instanceof PoolBitSet) {
            final PoolBitSet otherPoolBitSet = (PoolBitSet) others;
            set.or(otherPoolBitSet.set);
        } else {
            for (T element : others) {
                add(element);
            }
        }
    }

    @Override
    public boolean remove(T value) {
        assert pool.get(value.serial()) == value;
        final boolean present = set.get(value.serial());
        set.clear(value.serial());
        return present;
    }

    @Override
    public T removeOne() {
        final int index = set.nextSetBit(0);
        if (index < 0) {
            throw new NoSuchElementException();
        }
        set.clear(index);
        return pool.get(index);
    }

    @Override
    public void and(PoolSet<T> others) {
        if (others instanceof PoolBitSet) {
            final PoolBitSet otherPoolBitSet = (PoolBitSet) others;
            set.and(otherPoolBitSet.set);
        } else {
            for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
                if (!others.contains(pool.get(i))) {
                    set.clear(i);
                }
            }
        }
    }

    @Override
    public boolean containsAll(PoolSet<T> others) {
        for (T value : others) {
            if (!contains(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public PoolSet<T> clone() {
        return new PoolBitSet<T>(this);
    }

    /**
     * Gets an iterator over all the values in this set.
     */
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private int currentBit = -1;
            private int nextSetBit = set.nextSetBit(0);

            public boolean hasNext() {
                return nextSetBit != -1;
            }

            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                currentBit = nextSetBit;
                nextSetBit = set.nextSetBit(nextSetBit + 1);
                return pool.get(currentBit);
            }

            public void remove() {
                if (currentBit == -1) {
                    throw new IllegalStateException();
                }
                set.clear(currentBit);
                currentBit = -1;
            }
        };
    }
}
