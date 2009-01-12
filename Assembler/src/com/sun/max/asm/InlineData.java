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
package com.sun.max.asm;

import com.sun.max.asm.InlineDataDescriptor.*;

/**
 * A binding of an {@linkplain InlineDataDescriptor inline data descriptor} to some bytes decoded
 * from the instruction stream with which the descriptor is associated.
 *
 * @author Doug Simon
 */
public class InlineData {

    private final InlineDataDescriptor _descriptor;
    private final byte[] _data;

    /**
     * Creates an object to represent some otherwise unstructured.
     * @param position
     * @param data
     */
    public InlineData(int position, byte[] data) {
        this(new ByteData(position, data.length), data);
    }

    public InlineData(InlineDataDescriptor descriptor, byte[] data) {
        assert descriptor.size() == data.length;
        _descriptor = descriptor;
        _data = data;
    }

    public InlineDataDescriptor descriptor() {
        return _descriptor;
    }

    public byte[] data() {
        return _data;
    }

    public int size() {
        return _data.length;
    }
}
