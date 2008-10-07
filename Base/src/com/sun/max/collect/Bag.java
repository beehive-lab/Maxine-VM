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
 * A mapping of keys to one or more values.
 * 
 * @param <Key_Type>        the type of keys maintained by this bag
 * @param <Value_Type>      the type of mapped values
 * @param <Collection_Type> the type of collection used to store the values mapped to a given key. Whether or not
 *                          multiple <i>identical</i> values can be associated with a given key depends on what identical
 *                          means for this collection type and whether or not this collection type supports containing
 *                          identical values.
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public interface Bag<Key_Type, Value_Type, Collection_Type extends Iterable<Value_Type>> extends Iterable<Value_Type> {

    /**
     * Gets the values in this map associated with the given key. This will return a zero length collection if
     * there are no values associated with {@code key}.
     */
    Collection_Type get(Key_Type key);

    /**
     * Adds a specified value under a specified key in this bag.
     * Whether or not the number of values in this map associated with {@code key} changes
     * depends on the {@linkplain Collection_Type collection type} used to store the values.
     */
    void add(Key_Type key, Value_Type value);

    void addAll(Key_Type key, Collection_Type values);

    void remove(Key_Type key, Value_Type value);

    boolean containsKey(Key_Type key);

    Set<Key_Type> keys();

    Iterable<Collection_Type> collections();
}
