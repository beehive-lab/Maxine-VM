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
/*VCSID=0451036a-6f86-4ce4-a1a9-d80c900d3dc6*/
package com.sun.max.tele.debug;

import java.io.*;

import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
public final class PageDataStreamFactory implements DataStreamFactory {

    private final OldPageMemory _pageMemory;

    public PageDataStreamFactory(OldPageMemory pageMemory) {
        _pageMemory = pageMemory;
    }

    private final class PageInputStream extends InputStream {
        private Address _address;

        private PageInputStream(Address address) {
            _address = address;
        }

        @Override
        public int read() throws IOException {
            final Address a = _address;
            _address = _address.plus(1);
            return _pageMemory.readByte(a) & 0xff;
        }
    }

    public InputStream createInputStream(Address address, int size) {
        return new PageInputStream(address);
    }

    private final class PageOutputStream extends OutputStream {
        private Address _address;

        private PageOutputStream(Address address) {
            _address = address;
        }

        @Override
        public void write(int value) throws IOException {
            _pageMemory.writeByte(_address, (byte) value);
            _address = _address.plus(1);
        }
    }

    public OutputStream createOutputStream(Address address, int size) {
        return new PageOutputStream(address);
    }

}
