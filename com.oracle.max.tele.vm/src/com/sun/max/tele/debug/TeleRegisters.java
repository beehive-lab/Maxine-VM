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
package com.sun.max.tele.debug;

import static com.sun.max.platform.Platform.*;

import java.io.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * Abstract base class for caching the values of a set of ISA defined registers for a given thread.
 */
abstract class TeleRegisters extends AbstractTeleVMHolder implements TeleVMCache {

    private static final int TRACE_VALUE = 2;

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    public final CiRegister[] registers;

    private final TeleRegisterSet teleRegisterSet;
    final Endianness endianness;

    private final Address[] registerValues;
    private final byte[] registerData;
    private final ByteArrayInputStream registerDataInputStream;

    protected TeleRegisters(TeleVM teleVM, TeleRegisterSet teleRegisterSet, CiRegister[] registers) {
        super(teleVM);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + teleRegisterSet.thread().entityName() + " creating");
        tracer.begin();
        this.registers = registers;
        this.teleRegisterSet = teleRegisterSet;
        this.endianness = platform().endianness();
        this.registerValues = new Address[registers.length];
        this.registerData = new byte[registers.length * Address.size()];
        this.registerDataInputStream = new ByteArrayInputStream(registerData);
        Arrays.fill(this.registerValues, Address.zero());

        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + teleRegisterSet.thread().entityName() + " updating");

        tracer.end(null);
    }

    public final void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
            updateTracer.begin();
            // Refreshes the register values from the {@linkplain #registerData() raw buffer} holding the registers' values.
            // This method should be called whenever the raw buffer is updated.
            registerDataInputStream.reset();
            for (int i = 0; i != registerValues.length; i++) {
                try {
                    registerValues[i] = Word.read(registerDataInputStream, endianness).asAddress();
                } catch (IOException ioException) {
                    TeleError.unexpected(ioException);
                }
            }
            updateTracer.end(null);
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + "redundant update epoch=" + epoch + ": " + this);
        }
    }

    /**
     * Gets the value of a given register.
     *
     * @param register the register whose value is to be returned
     * @return the value of {@code register}
     */
    final Address getValue(CiRegister register) {
        return registerValues[register.encoding];
    }

    /**
     * Determines whether a particular register is the instruction pointer.
     *
     * @param register
     * @return whether the register is the instruction pointer
     */
    boolean isInstructionPointerRegister(CiRegister register) {
        return false;
    }

    /**
     * Determines whether a particular register is a flags register.
     *
     * @param register
     * @return whether the register is a flags register
     */
    boolean isFlagsRegister(CiRegister register) {
        return false;
    }

    /**
     * Gets the raw buffer into which the registers' values are read from the remote process.
     */
    final byte[] registerData() {
        return registerData;
    }

    Address getValueAt(int index) {
        return registerValues[index];
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
    protected final void setValue(CiRegister register, Address value) {
        registerValues[register.encoding] = value;
    }

    Registers getRegisters(String name) {
        final String[] registerNames = new String[registers.length];
        final long[] values = new long[registerNames.length];
        int z = 0;
        for (CiRegister reg : registers) {
            registerNames[z] = reg.name;
            values[z] = getValue(reg).toLong();
            z++;
        }

        final Registers result = new Registers(name, registerNames, values);
        return result;
    }
}
