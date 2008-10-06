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
/*VCSID=daba4efa-7f29-441c-8ee4-6c4b282c43cf*/
package com.sun.max.collect;

import com.sun.max.lang.*;


/**
 * @author Bernd Mathiske
 */
public class ByteVector {

    private int _length;
    private byte[] _array;

    public ByteVector() {
        _length = 0;
        _array = new byte[_length];
    }

    public ByteVector(byte[] bytes) {
        _length = bytes.length;
        _array = new byte[_length];
        Bytes.copyAll(bytes, _array);
    }

    public boolean isEmpty() {
        return _length == 0;
    }

    public int length() {
        return _length;
    }

    public void add(byte value) {
        if (_length >= _array.length) {
            int newLength = _length < Ints.K ? _length * 2 : (_length / 2) * 3;
            newLength += 4;
            final byte[] newArray = new byte[newLength];
            Bytes.copyAll(_array, newArray);
            _array = newArray;
        }
        _array[_length] = value;
        _length++;
    }

    public byte get(int index) {
        if (index >= _length) {
            throw new IndexOutOfBoundsException();
        }
        return _array[index];
    }

    public void set(int index, byte value) {
        if (index >= _length) {
            throw new IndexOutOfBoundsException();
        }
        _array[index] = value;
    }

    public void put(int index, byte value) {
        if (index >= _length) {
            if (index >= _array.length) {
                int newLength = _length < Ints.K ? _length * 2 : (_length / 2) * 3;
                newLength += 4;
                if (index >= newLength) {
                    newLength = _length + (((index - _length) / 2) * 3);
                }
                final byte[] newArray = new byte[newLength];
                Bytes.copyAll(_array, newArray);
                _array = newArray;
            }
            _length = index + 1;
        }
        _array[index] = value;
    }

    public byte[] toArray() {
        final byte[] result = new byte[_length];
        Bytes.copy(_array, result, _length);
        return result;
    }
}
