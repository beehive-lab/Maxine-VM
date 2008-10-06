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
/*VCSID=eff7e87e-1e06-4478-b967-24e9275ac890*/
package com.sun.max.tele.debug.no;

import java.io.*;

import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;

public final class MemoryDataStreamFactory implements DataStreamFactory {

    public MemoryDataStreamFactory() {
    }

    private final class MemoryInputStream extends InputStream {
        private Pointer _pointer;

        private MemoryInputStream(Pointer pointer) {
            _pointer = pointer;
        }

        @Override
        public int read() throws IOException {
            final Pointer p = _pointer;
            _pointer = _pointer.plus(1);
            return p.readByte(0) & 0xff;
        }
    }

    public InputStream createInputStream(Address address, int size) {
        return new MemoryInputStream(address.asPointer());
    }

    private final class MemoryOutputStream extends OutputStream {
        private Pointer _pointer;

        private MemoryOutputStream(Pointer pointer) {
            _pointer = pointer;
        }

        @Override
        public void write(int value) throws IOException {
            _pointer.writeByte(0, (byte) value);
            _pointer = _pointer.plus(1);
        }
    }

    public OutputStream createOutputStream(Address address, int size) {
        return new MemoryOutputStream(address.asPointer());
    }

}
