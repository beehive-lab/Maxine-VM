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
package com.sun.max.tele.debug;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;

/**
 * Abstract base class for caching the values of a set of ISA defined registers for a given thread.
 *
 * @author Bernd Mathiske
 * @author Mick Jordan
 * @author Doug Simon
 */
public abstract class TeleRegisters {

    protected final VMConfiguration _vmConfiguration;
    private final Symbolizer<? extends Symbol> _symbolizer;

    public final Symbolizer<? extends Symbol> symbolizer() {
        return _symbolizer;
    }

    private final Address[] _registerValues;
    private final byte[] _registerData;
    private final ByteArrayInputStream _registerDataInputStream;

    protected TeleRegisters(Symbolizer<? extends Symbol> symbolizer, VMConfiguration vmConfiguration) {
        _symbolizer = symbolizer;
        _vmConfiguration = vmConfiguration;
        _registerValues = new Address[symbolizer.numberOfValues()];
        _registerData = new byte[symbolizer.numberOfValues() * Address.size()];
        _registerDataInputStream = new ByteArrayInputStream(_registerData);
    }

    /**
     * Gets the raw buffer into which the registers' values are read from the remote process.
     */
    final byte[] registerData() {
        return _registerData;
    }

    /**
     * Refreshes the register values from the {@linkplain #registerData() raw buffer} holding the registers' values.
     * This method should be called whenever the raw buffer is updated.
     */
    public final void refresh() {
        _registerDataInputStream.reset();
        final Endianness endianness = _vmConfiguration.platform().processorKind().dataModel().endianness();
        for (int i = 0; i != _registerValues.length; i++) {
            try {
                _registerValues[i] = Address.fromLong(endianness.readLong(_registerDataInputStream));
            } catch (IOException ioException) {
                ProgramError.unexpected(ioException);
            }
        }
    }

    public Sequence<Symbol> find(Address startAddress, Address endAddress) {
        final AppendableSequence<Symbol> symbols = new ArrayListSequence<Symbol>(4);
        for (int index = 0; index < _registerValues.length; index++) {
            final Address value = _registerValues[index];
            if (startAddress.lessEqual(value)  && value.lessThan(endAddress)) {
                symbols.append(_symbolizer.fromValue(index));
            }
        }
        return symbols;
    }

    public Address get(int index) {
        return _registerValues[index];
    }

    /**
     * Gets the value of a given register.
     *
     * @param register the register whose value is to be returned
     * @return the value of {@code register}
     */
    public final Address get(Symbol register) {
        return _registerValues[register.value()];
    }

    /**
     * Sets the value of a given register.
     *
     * Note: This call only updates the value of the register in this cache. The update to the actual register in the
     * remote process must be done by the caller of this method.
     *
     * @param register the register whose value is to be updated
     * @param value the new value of {@code register}
     */
    public final void set(Symbol register, Address value) {
        _registerValues[register.value()] = value;
    }

    public Registers getRegisters(String name) {
        final String[] registerNames = new String[symbolizer().numberOfValues()];
        final long[] values = new long[registerNames.length];
        int z = 0;
        for (Symbol s : symbolizer()) {
            registerNames[z] = s.name();
            values[z] = get(s).toLong();
            z++;
        }

        final Registers result = new Registers(name, registerNames, values);


        return result;
    }
}
