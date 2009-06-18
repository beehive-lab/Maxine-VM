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

import com.sun.max.lang.Arrays;

/**
 * A representation for a subset of objects in a {@linkplain Pool pool}. The recommended mechanism for creating a pool
 * set is by calling {@link #noneOf(Pool)}, {@link #allOf(Pool)}, {@link #of(Pool, PoolObject[])} or
 * {@link #of(Pool, PoolObject, PoolObject...)}. These methods ensure that the most efficient and compact pool set
 * object is created based on the length of the underlying pool.
 *
 * @author Doug Simon
 */
public abstract class PoolSet<PoolObject_Type extends PoolObject> implements Cloneable, IterableWithLength<PoolObject_Type> {

    protected final Pool<PoolObject_Type> pool;

    protected PoolSet(Pool<PoolObject_Type> pool) {
        this.pool = pool;
    }

    /**
     * Gets the number of objects in this set.
     */
    public abstract int length();

    /**
     * Adds a value to this set. The value must be in the {@linkplain #pool() underlying pool}.
     */
    public abstract void add(PoolObject_Type value);

    /**
     * Determines if a given value is in this set. The value must be in the {@linkplain #pool() underlying pool}.
     */
    public abstract boolean contains(PoolObject_Type value);

    /**
     * Determines if this set contains all the values present in a given set.
     */
    public boolean containsAll(PoolSet<PoolObject_Type> others) {
        for (PoolObject_Type value : others) {
            if (!contains(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes a given value from this set. The value must be in the {@linkplain #pool() underlying pool}.
     *
     * @return true if this set contained {@code value}
     */
    public abstract boolean remove(PoolObject_Type value);

    /**
     * Removes an arbitrary value from this set. The value must be in the {@linkplain #pool() underlying pool}.
     *
     * @throws NoSuchElementException if this set is empty
     */
    public abstract PoolObject_Type removeOne() throws NoSuchElementException;

    /**
     * Gets the pool containing the values that are in or may be added to this set.
     */
    public Pool<PoolObject_Type> pool() {
        return pool;
    }

    /**
     * Clears all entries from this set.
     */
    public abstract void clear();

    /**
     * Adds all entries from the pool to this set.
     *
     * @return this set
     */
    public abstract PoolSet<PoolObject_Type> addAll();

    /**
     * Adds all entries from another pool set to this set.
     */
    public void or(PoolSet<PoolObject_Type> others) {
        for (PoolObject_Type element : others) {
            add(element);
        }
    }

    /**
     * Removes all the entries from this set that are not in a given set.
     */
    public abstract void and(PoolSet<PoolObject_Type> others);

    /**
     * Creates a copy of this pool set. The copy has the same pool object as this pool set.
     */
    @Override
    public abstract PoolSet<PoolObject_Type> clone();

    /**
     * @return true if there are no values in this set
     */
    public abstract boolean isEmpty();

    /**
     * @see #toString(PoolSet)
     */
    @Override
    public String toString() {
        return toString(this);
    }

    /**
     * Creates an empty pool set for a given pool.
     *
     * @param <PoolObject_Type> the type of objects in {@code pool}
     * @param pool the pool of objects that the returned set provides a view upon
     * @return an empty pool set that can be subsequently modified to contain objects from {@code pool}
     */
    public static <PoolObject_Type extends PoolObject> PoolSet<PoolObject_Type> noneOf(Pool<PoolObject_Type> pool) {
        if (pool.length() <= PoolSet64.MAX_POOL_SIZE) {
            return new PoolSet64<PoolObject_Type>(pool);
        }
        if (pool.length() <= PoolSet128.MAX_POOL_SIZE) {
            return new PoolSet128<PoolObject_Type>(pool);
        }
        return new PoolBitSet<PoolObject_Type>(pool);
    }

    /**
     * Creates a pool set initially containing all the objects in a given pool.
     *
     * @param <PoolObject_Type> the type of objects in {@code pool}
     * @param pool the pool of objects that the returned set provides a view upon
     * @return a pool set containing all the objects in {@code pool}
     */
    public static <PoolObject_Type extends PoolObject> PoolSet<PoolObject_Type> allOf(Pool<PoolObject_Type> pool) {
        return noneOf(pool).addAll();
    }

    /**
     * Creates a pool set initially containing one or more objects from a given pool.
     *
     * @param <PoolObject_Type> the type of objects in {@code pool}
     * @param <PoolObject_SubType> the type of objects that can be added to the pool by this method
     * @param pool the pool of objects that the returned set provides a view upon
     * @param first an object that will be in the returned set
     * @param rest zero or more objects that will be in the returned set
     * @return a pool set containing {@code first} and all the objects in {@code rest}
     */
    public static <PoolObject_Type extends PoolObject, PoolObject_SubType extends PoolObject_Type> PoolSet<PoolObject_Type> of(Pool<PoolObject_Type> pool, PoolObject_SubType first, PoolObject_SubType... rest) {
        final PoolSet<PoolObject_Type> poolSet = noneOf(pool);
        poolSet.add(first);
        for (PoolObject_Type object : rest) {
            poolSet.add(object);
        }
        return poolSet;
    }

    /**
     * Creates a pool set initially containing all the objects specified by a given array that are also in a given pool.
     *
     * @param <PoolObject_Type> the type of objects in {@code pool}
     * @param <PoolObject_SubType> the type of objects that can be added to the pool by this method
     * @param pool the pool of objects that the returned set provides a view upon
     * @param objects zero or more objects that will be in the returned set
     * @return a pool set containing all the objects in {@code objects}
     */
    public static <PoolObject_Type extends PoolObject, PoolObject_SubType extends PoolObject_Type> PoolSet<PoolObject_Type> of(Pool<PoolObject_Type> pool, PoolObject_SubType[] objects) {
        final PoolSet<PoolObject_Type> poolSet = noneOf(pool);
        for (PoolObject_Type object : objects) {
            poolSet.add(object);
        }
        return poolSet;
    }

    /**
     * Adds all objects returned by a given iterable's iterator to a given pool set.
     *
     * @param <PoolObject_Type> the type of objects in {@code poolSet}
     * @param poolSet the set to which the objects are added
     * @param elements a collection of objects
     */
    public static <PoolObject_Type extends PoolObject> void addAll(PoolSet<PoolObject_Type> poolSet, Iterable<PoolObject_Type> elements) {
        for (PoolObject_Type element : elements) {
            poolSet.add(element);
        }
    }

    /**
     * Copies the objects in a given pool set into an array.
     *
     * @param <PoolObject_Type> the type of objects in {@code poolSet}
     * @param poolSet a set of objects in a pool
     * @param elementType the class literal for the type of objects in {@code poolSet}
     * @return an array of the objects in {@code poolSet}, ordered by their serial numbers
     */
    public static <PoolObject_Type extends PoolObject> PoolObject_Type[] toArray(PoolSet<PoolObject_Type> poolSet, Class<PoolObject_Type> elementType) {
        final PoolObject_Type[] array = Arrays.newInstance(elementType, poolSet.length());
        int i = 0;
        for (PoolObject_Type element : poolSet) {
            array[i++] = element;
        }
        return array;
    }

    public static <PoolObject_Type extends PoolObject> boolean match(PoolSet<PoolObject_Type> poolSet1, PoolSet<PoolObject_Type> poolSet2) {
        if (!poolSet1.pool().equals(poolSet2.pool())) {
            return false;
        }
        final Iterator<PoolObject_Type> iterator1 = poolSet1.iterator();
        final Iterator<PoolObject_Type> iterator2 = poolSet2.iterator();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            if (!iterator1.next().equals(iterator2.next())) {
                return false;
            }
        }
        return iterator1.hasNext() == iterator2.hasNext();
    }

    /**
     * Gets a string representation of a given pool set. The returned string is delimited by "{" and "}". For each
     * object in the given pool set, its {@code toString()} representation is added to the string followed by ", " if it
     * is not the last object in the set.
     *
     * @param poolSet a pool set for which a string representation is required
     */
    public static String toString(PoolSet poolSet) {
        String s = "{";
        String delimiter = "";
        for (Object value : poolSet) {
            s += delimiter + value;
            delimiter = ", ";
        }
        s += "}";
        return s;
    }
}
