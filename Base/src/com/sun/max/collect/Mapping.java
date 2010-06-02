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
 * An object that maps keys to values. A map cannot contain duplicate keys; each key can map to at most one value.
 * <p>
 * The implementations of this interface provide the following behavior be default:
 * <ul>
 * <li>{@code null} keys are illegal in Mapping.</li>
 * <li>The iterators derived from {@link #keys()} and {@link #values()} do not support
 * {@linkplain Iterator#remove() removal} and are <b>not</b> fail-fast (see {@link HashMap} for a description of fail-fast).
 * </ul>
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public interface Mapping<K, V> {

    V get(K key);

    V put(K key, V value);

    V remove(K key);

    void clear();

    boolean containsKey(K key);

    int length();

    /**
     * Gets an iterable view of the keys in this map.
     */
    IterableWithLength<K> keys();

    /**
     * Gets an iterable view of the values in this map.
     */
    IterableWithLength<V> values();

}
