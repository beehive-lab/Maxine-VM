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
import java.util.Arrays;

import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
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
 * @author Michael Van De Vanter
 */
public abstract class TeleRegisters {

    protected final VMConfiguration vmConfiguration;
    private final Symbolizer<? extends Symbol> symbolizer;

    public final Symbolizer<? extends Symbol> symbolizer() {
        return symbolizer;
    }

    private final Address[] registerValues;
    private final byte[] registerData;
    private final ByteArrayInputStream registerDataInputStream;

    protected TeleRegisters(Symbolizer<? extends Symbol> symbolizer, VMConfiguration vmConfiguration) {
        this.symbolizer = symbolizer;
        this.vmConfiguration = vmConfiguration;
        this.registerValues = new Address[symbolizer.numberOfValues()];
        this.registerData = new byte[symbolizer.numberOfValues() * Address.size()];
        this.registerDataInputStream = new ByteArrayInputStream(registerData);
        Arrays.fill(this.registerValues, Address.zero());
    }

    /**
     * Gets the raw buffer into which the registers' values are read from the remote process.
     */
    final byte[] registerData() {
        return registerData;
    }

    /**
     * Refreshes the register values from the {@linkplain #registerData() raw buffer} holding the registers' values.
     * This method should be called whenever the raw buffer is updated.
     */
    public final void refresh() {
        registerDataInputStream.reset();
        final Endianness endianness = vmConfiguration.platform().processorKind.dataModel.endianness;
        for (int i = 0; i != registerValues.length; i++) {
            try {
                registerValues[i] = Word.read(registerDataInputStream, endianness).asAddress();
            } catch (IOException ioException) {
                ProgramError.unexpected(ioException);
            }
        }
    }

    /**
     * @return a list of the registers in this set that point into the described area of memory in the VM..
     */
    public Sequence<Symbol> find(MemoryRegion memoryRegion) {
        final AppendableSequence<Symbol> symbols = new ArrayListSequence<Symbol>(4);
        if (memoryRegion != null) {
            for (int index = 0; index < registerValues.length; index++) {
                final Address address = registerValues[index];
                if (memoryRegion.contains(address)) {
                    symbols.append(symbolizer.fromValue(index));
                }
            }
        }
        return symbols;
    }

    /**
     * @return a comma-separated list of the register names in this set that
     * point into the described area of memory in the VM.
     */
    public String findAsNameList(MemoryRegion memoryRegion) {
        String nameList = "";
        for (Symbol registerSymbol : find(memoryRegion)) {
            if (nameList.length() > 0) {
                nameList += ",";
            }
            nameList += registerSymbol.name();
        }
        return nameList;
    }

    public Address get(int index) {
        return registerValues[index];
    }

    /**
     * Gets the value of a given register.
     *
     * @param register the register whose value is to be returned
     * @return the value of {@code register}
     */
    public final Address get(Symbol register) {
        return registerValues[register.value()];
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
        registerValues[register.value()] = value;
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
