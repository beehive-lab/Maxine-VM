/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir.interpreter.eir;

import java.util.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.value.*;

/**
 * Emulation of a call stack that grows downwards.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class EirStack {

    public static class StackAddressOutOfBoundsException extends IndexOutOfBoundsException {

        public StackAddressOutOfBoundsException(String message) {
            super(message);
        }
    }

    /**
     * The address at which the stack starts.  Any attempt to read from or write
     * to the stack at an address equal to or higher that this value results in
     * a {@link StackAddressOutOfBoundsException}.
     * Sufficiently high, so the stack can grow downwards.  This must also be
     * a word aligned value as the real stack will also be word aligned.
     */
    public final Address ceiling;

    private Address sp;

    public EirStack() {
        ceiling = VmThread.stackSize();
        assert !ceiling.isZero();
        sp = ceiling;
    }

    protected EirStack(EirStack copy) {
        ceiling = copy.ceiling;
        sp = copy.sp;
        slots = new ArrayList<Value>(copy.slots);
    }

    /**
     * Makes a copy of this stack state for the purpose of saving a CPU context that can be restored.
     */
    public EirStack save() {
        return new EirStack(this);
    }

    /**
     * Gets the address of the "top of stack". This is
     * the lowest address at which values can be read from or written to the stack
     * without throwing a {@link StackAddressOutOfBoundsException}.
     */
    public Address sp() {
        return sp;
    }

    public void setSP(Address address) {
        sp = address;
    }

    private List<Value> slots = new ArrayList<Value>();

    private int addressToSlotIndex(Address address) {
        if (address.lessThan(sp) || address.greaterEqual(ceiling)) {
            final String message = address.toLong() + " < " + sp.toLong() + " || " + address.toLong() + " >= " + ceiling.toLong();
            throw new StackAddressOutOfBoundsException(message);
        }
        final int index = ceiling.minus(address).dividedBy(8).toInt();
        if (index >= slots.size()) {
            for (int i = slots.size(); i <= index; i++) {
                slots.add(null);
            }
        }
        return index;
    }

    public Value read(Address address) {
        return slots.get(addressToSlotIndex(address));
    }

    public byte readByte(Address address) {
        assert read(address).kind().isWord;
        return read(address).unsignedToByte();
    }

    public short readShort(Address address) {
        assert read(address).kind().isWord;
        return read(address).unsignedToShort();
    }

    public int readInt(Address address) {
        assert read(address).kind().isWord;
        return read(address).unsignedToInt();
    }

    public long readLong(Address address) {
        assert read(address).kind().isWord;
        return read(address).toLong();
    }

    public float readFloat(Address address) {
        return read(address).asFloat();
    }

    public double readDouble(Address address) {
        return read(address).asDouble();
    }

    public void write(Address address, Value value) {
        final int index = addressToSlotIndex(address);
        slots.set(index, value);
    }

    private WordValue partiallyOverwrite(Address address, int n, int mask) {
        Address word = Address.zero();
        final Value value = read(address);
        if (value != null && value.kind().isWord) {
            word = value.asWord().asAddress().and(Address.fromInt(mask).not());
        }
        return new WordValue(word.or(n & mask));
    }

    public void writeByte(Address address, byte value) {
        write(address, partiallyOverwrite(address, value, 0xff));
    }

    public void writeShort(Address address, short value) {
        write(address, partiallyOverwrite(address, value, 0xffff));
    }

    public void writeInt(Address address, int value) {
        write(address, partiallyOverwrite(address, value, 0xffffffff));
    }

    public void writeLong(Address address, long value) {
        write(address, new WordValue(Address.fromLong(value)));
    }

    public void writeFloat(Address address, float value) {
        write(address, FloatValue.from(value));
    }

    public void writeDouble(Address address, double value) {
        write(address, DoubleValue.from(value));
    }

    public void writeWord(Address address, Word value) {
        write(address, new WordValue(value));
    }
}
