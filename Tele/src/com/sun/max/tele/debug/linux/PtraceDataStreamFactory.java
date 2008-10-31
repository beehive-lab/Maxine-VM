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
package com.sun.max.tele.debug.linux;

import java.io.*;

import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
public final class PtraceDataStreamFactory implements DataStreamFactory {

    private final Ptrace _ptrace;

    public PtraceDataStreamFactory(Ptrace ptrace) {
        _ptrace = ptrace;
    }

    private final class PtraceInputStream extends InputStream {
        private Address _address;

        private PtraceInputStream(Address address) {
            _address = address;
        }

        @Override
        public int read() throws IOException {
            final Address a = _address;
            _address = _address.plus(1);
            return _ptrace.readDataByte(a) & 0xff;
        }
    }

    public InputStream createInputStream(Address address, int size) {
        return new PtraceInputStream(address);
    }

    private final class PtraceOutputStream extends OutputStream {
        private Address _address;

        private PtraceOutputStream(Address address) {
            _address = address;
        }

        @Override
        public void write(int value) throws IOException {
            _ptrace.writeDataByte(_address, (byte) value);
            _address = _address.plus(1);
        }
    }

    public OutputStream createOutputStream(Address address, int size) {
        return new PtraceOutputStream(address);
    }

}
