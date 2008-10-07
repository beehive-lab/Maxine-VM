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
/*VCSID=f2b96038-8d56-4135-b871-5ea6550bc7e2*/
package com.sun.max.collect;

import java.util.*;

/**
 * An implementation of a pool that wraps an {@link IndexedSequence}.
 *
 * @author Doug Simon
 */
public class IndexedSequencePool<PoolObject_Type extends PoolObject> extends Pool<PoolObject_Type> {

    protected final IndexedSequence<PoolObject_Type> _indexedSequence;
    private final int _length;

    public IndexedSequencePool(IndexedSequence<PoolObject_Type> indexedSequence) {
        _indexedSequence = indexedSequence;
        _length = indexedSequence.length();
    }

    @Override
    public PoolObject_Type get(int serial) {
        assert _indexedSequence.length() == _length;
        return _indexedSequence.get(serial);
    }

    @Override
    public int length() {
        assert _indexedSequence.length() == _length;
        return _indexedSequence.length();
    }

    @Override
    public Iterator<PoolObject_Type> iterator() {
        assert _indexedSequence.length() == _length;
        return _indexedSequence.iterator();
    }
}
