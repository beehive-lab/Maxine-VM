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
 * An implementation of a pool based on an array.
 *
 * @author Doug Simon
 */
public class ArrayPool<PoolObject_Type extends PoolObject> extends Pool<PoolObject_Type> {

    protected final PoolObject_Type[] objects;

    public ArrayPool(PoolObject_Type... objects) {
        this.objects = objects;
    }

    @Override
    public PoolObject_Type get(int serial) {
        return objects[serial];
    }

    @Override
    public int length() {
        return objects.length;
    }

    public Iterator<PoolObject_Type> iterator() {
        return Iterators.iterator(objects);
    }
}
