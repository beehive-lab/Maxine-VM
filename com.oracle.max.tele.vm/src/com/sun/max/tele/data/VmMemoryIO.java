/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.data;

import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Singleton accessor to low-level data reading from VM memory.  Some of the method here are convenience
 * methods wrapped around the low level data reading interface, but access to the low level
 * interface is possible for other cases.
 */
public final class VmMemoryIO extends AbstractVmHolder implements MaxMemoryIO {

    private static final int TRACE_VALUE = 1;

    private static VmMemoryIO vmMemoryIO;

    public static VmMemoryIO make(TeleVM vm, TeleProcess teleProcess) {
        if (vmMemoryIO == null) {
            vmMemoryIO = new VmMemoryIO(vm, teleProcess);
        }
        return vmMemoryIO;
    }

    private final DataAccess access;

    private VmMemoryIO(TeleVM vm, TeleProcess teleProcess) {
        super(vm);
        this.access = teleProcess.dataAccess();
    }

    public Value readWordValue(Address address) {
        try {
            return WordValue.from(access.readWord(address));
        } catch (DataIOError e) {
            return VoidValue.VOID;
        }
    }

    public Value readArrayElementValue(Kind kind, RemoteReference reference, int index) {
        return objects().unsafeReadArrayElementValue(kind, reference, index);
    }

    public void readBytes(Address address, byte[] bytes) {
        access.readFully(address, bytes);
    }

    public int readInt(Address address) {
        return access.readInt(address);
    }

    public int readInt(Address address, int offset) {
        return access.readInt(address, offset);
    }

    /**
     * @return extended access to low-level reading and writing of memory in the VM.
     */
    public DataAccess access() {
        return access;
    }

    /**
     * Reads a single word from VM memory.
     *
     * @param address a location in VM memory
     * @return the current contents of the word of VM memory at that location.
     */
    public Word readWord(Address address) {
        return access.readWord(address);
    }

    /**
     * Reads a single word from VM memory.
     *
     * @param address a location in VM memory
     * @param offset from the location at which to read
     * @return the current contents of the word of VM memory at that location.
     */
    public Word readWord(Address address, int offset) {
        return access.readWord(address, offset);
    }

    /**
     * Read an array indexed word from VM memory.
     * @param address a location in VM memory
     * @param displacement offset from the location of index 0
     * @param index word index into the array
     * @return the value stored at the indexed location in VM memory
     */
    public Word getWord(Address address, int displacement, int index) {
        return access.getWord(address, displacement, index);
    }

    /**
     * Reads a single byte from VM memory.
     *
     * @param address a location in VM memory
     * @return the current contents of the word of VM memory at that location.
     */
    public byte readByte(Address address) {
        return access.readByte(address);
    }

    /**
     * Reads a single byte from VM memory.
     *
     * @param address a location in VM memory
     * @param offset from the location at which to read
     * @return the current contents of the byte of VM memory at that location.
     */
    public byte readByte(Address address, int offset) {
        return access.readByte(address, offset);
    }

    /**
     * Reads a {@code long} from VM memory.
     *
     * @param address a location in VM memory
     * @return the current contents of VM memory at that location as a long
     */
    public long readLong(Address address) {
        return access.readLong(address);
    }

    /**
     * Read an array of bytes from VM memory.
     *
     * @param address a location in VM memory
     * @param length the number of bytes to be read
     */
    public byte[] readBytes(Address address, int length) {
        return access.readFully(address, length);
    }

    /**
     * Copies the contents of a byte array into VM memory.
     *
     * @param address a location in VM memory
     * @param bytes bytes to be written
     */
    public void writeBytes(Address address, byte[] bytes) {
        access.writeBytes(address, bytes);
    }

    /**
     * Write a word into VM memory.
     *
     * @param address a location in memory
     * @param offset offset from the location at which to write
     * @param value the value to be written.
     */
    public void writeWord(Address address, int offset, Word value) {
        access.writeWord(address, offset, value);
    }

    /**
     * Write an array indexed word into VM memory.
     * @param address a location in VM memory
     * @param displacement offset from the location of index 0
     * @param index word index into the array
     * @param value the word to be written into the indexed location
     */
    public void setWord(Address address, int displacement, int index, Word value) {
        access.setWord(address, displacement, index, value);
    }

}
