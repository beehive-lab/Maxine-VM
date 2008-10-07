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
/*VCSID=40a0bf49-bf00-4d6f-a739-44cc9534a940*/
package com.sun.max.tele.page;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class Page {

    private TeleIO _teleIO;

    private final long _index;

    private final byte[] _bytes;

    public Page(TeleIO teleIO, long index) {
        _teleIO = teleIO;
        _index = index;
        _bytes = new byte[teleIO.pageSize()];
    }

    public int size() {
        return _teleIO.pageSize();
    }

    public Address address() {
        return Address.fromLong(_index * size());
    }

    boolean _isReadDirty = true;

    private void refreshRead() throws DataIOError {
        if (_isReadDirty) {
            DataIO.Static.readFully(_teleIO, address(), _bytes);
            _isReadDirty = false;
        }
    }

    public byte readByte(int offset) throws DataIOError {
        refreshRead();
        return _bytes[offset];
    }

    public int readBytes(int fromOffset, byte[] buffer, int toStart) throws DataIOError {
        refreshRead();
        final int n = Math.min(buffer.length - toStart, size() - fromOffset);
        Bytes.copy(_bytes, fromOffset, buffer, toStart, n);
        return n;
    }
}
