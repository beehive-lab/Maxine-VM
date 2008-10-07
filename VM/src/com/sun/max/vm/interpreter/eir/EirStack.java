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
/*VCSID=64b70697-fbcf-4b10-baa3-257d72f3492f*/
package com.sun.max.vm.interpreter.eir;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
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
     * Sufficiently high, so the stack can grow downwards. This must also be
     * a word aligned value as the real stack will also be word aligned.
     */
    private final Address _ceiling;

    private Address _sp;

    public EirStack() {
        _ceiling = VmThread.stackSize();
        assert !_ceiling.isZero();
        _sp = _ceiling;
    }

    protected EirStack(EirStack copy) {
        _ceiling = copy._ceiling;
        _sp = copy._sp;
        _slots = new ArrayListSequence<Value>(copy._slots);
    }

    /**
     * Makes a copy of this stack state for the purpose of saving a CPU context that can be restored.
     */
    public EirStack save() {
        return new EirStack(this);
    }

    /**
     * Gets the address at which the stack starts. Any attempt to read from or write
     * to the stack at an address equal to or higher that this value results in
     * a {@link StackAddressOutOfBoundsException}.
     */
    public Address ceiling() {
        return _ceiling;
    }

    /**
     * Gets the address of the "top of stack". This is
     * the lowest address at which values can be read from or written to the stack
     * without throwing a {@link StackAddressOutOfBoundsException}.
     */
    public Address sp() {
        return _sp;
    }

    public void setSP(Address address) {
        _sp = address;
    }

    private List<Value> _slots = new ArrayListSequence<Value>();

    private int addressToSlotIndex(Address address) {
        if (address.lessThan(_sp) || address.greaterEqual(_ceiling)) {
            final String message = address.toLong() + " < " + _sp.toLong() + " || " + address.toLong() + " >= " + _ceiling.toLong();
            throw new StackAddressOutOfBoundsException(message);
        }
        final int index = _ceiling.minus(address).dividedBy(8).toInt();
        if (index >= _slots.size()) {
            for (int i = _slots.size(); i <= index; i++) {
                _slots.add(null);
            }
        }
        return index;
    }

    public Value read(Address address) {
        return _slots.get(addressToSlotIndex(address));
    }

    public byte readByte(Address address) {
        assert read(address).kind() == Kind.WORD;
        return read(address).unsignedToByte();
    }

    public short readShort(Address address) {
        assert read(address).kind() == Kind.WORD;
        return read(address).unsignedToShort();
    }

    public int readInt(Address address) {
        assert read(address).kind() == Kind.WORD;
        return read(address).unsignedToInt();
    }

    public long readLong(Address address) {
        assert read(address).kind() == Kind.WORD;
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
        _slots.set(index, value);
    }

    private WordValue partiallyOverwrite(Address address, int n, int mask) {
        Address word = Address.zero();
        final Value value = read(address);
        if (value != null && value.kind() == Kind.WORD) {
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

    /**
     * @see EirCPU#replaceUninitializedValue(Value, Value)
     */
    public void replaceUninitializedValue(Value uninitializedValue, Value initializedValue) {
        for (int i = 0; i != _slots.size(); ++i) {
            if (uninitializedValue.equals(_slots.get(i))) {
                _slots.set(i, initializedValue);
            }
        }
    }

}
