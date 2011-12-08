/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.tele.debug.no;

import java.io.*;

import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;

public final class MemoryDataStreamFactory implements DataStreamFactory {

    public MemoryDataStreamFactory() {
    }

    private final class MemoryInputStream extends InputStream {
        private Pointer pointer;

        private MemoryInputStream(Pointer pointer) {
            this.pointer = pointer;
        }

        @Override
        public int read() throws IOException {
            final Pointer p = pointer;
            this.pointer = pointer.plus(1);
            return p.readByte(0) & 0xff;
        }
    }

    public InputStream createInputStream(Address address, int size) {
        return new MemoryInputStream(address.asPointer());
    }

    private final class MemoryOutputStream extends OutputStream {
        private Pointer pointer;

        private MemoryOutputStream(Pointer pointer) {
            this.pointer = pointer;
        }

        @Override
        public void write(int value) throws IOException {
            pointer.writeByte(0, (byte) value);
            pointer = pointer.plus(1);
        }
    }

    public OutputStream createOutputStream(Address address, int size) {
        return new MemoryOutputStream(address.asPointer());
    }

}
