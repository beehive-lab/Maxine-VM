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
/*VCSID=5f37273c-e90f-48a2-9514-b39ab9b096a7*/
package com.sun.max.unsafe.box;

import com.sun.max.unsafe.*;

/**
 * Boxed version of Size.
 * 
 * @see Size
 *
 * @author Bernd Mathiske
 */
public final class BoxedSize extends Size implements UnsafeBox {

    private long _nativeWord;

    public BoxedSize(UnsafeBox unsafeBox) {
        _nativeWord = unsafeBox.nativeWord();
    }

    public BoxedSize(long value) {
        _nativeWord = value;
    }

    public BoxedSize(int value) {
        _nativeWord = value & BoxedWord.INT_MASK;
    }

    public long nativeWord() {
        return _nativeWord;
    }

    @Override
    protected Size dividedByAddress(Address divisor) {
        return new BoxedAddress(_nativeWord).dividedByAddress(divisor).asSize();
    }

    @Override
    protected Size dividedByInt(int divisor) {
        return new BoxedAddress(_nativeWord).dividedByInt(divisor).asSize();
    }

    @Override
    protected Size remainderByAddress(Address divisor) {
        return new BoxedAddress(_nativeWord).remainderByAddress(divisor).asSize();
    }

    @Override
    protected int remainderByInt(int divisor) {
        return new BoxedAddress(_nativeWord).remainderByInt(divisor);
    }

}
