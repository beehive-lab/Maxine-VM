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

/**
 * A sequence of {@linkplain PoolObject objects} that are uniquely identified <i>within a sequence</i> by a
 * {@linkplain PoolObject#serial() serial number} (i.e. an index into the sequence). A subset of the objects
 * within a pool can be efficiently represented by a {@link PoolSet}.
 * 
 * @author Doug Simon
 */
public abstract class Pool<PoolObject_Type extends PoolObject> implements Iterable<PoolObject_Type> {

    /**
     * Gets the object in the pool identified by a given serial number.
     */
    public abstract PoolObject_Type get(int serial);

    /**
     * The number of objects in this pool. All objects in the pool must have a unique serial number in the range
     * {@code [0 .. length() - 1]}.
     */
    public abstract int length();

    /**
     * Determines if this pool is empty.
     */
    public boolean isEmpty() {
        return length() == 0;
    }
}
