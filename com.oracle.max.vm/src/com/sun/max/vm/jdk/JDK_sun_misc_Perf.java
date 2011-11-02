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
package com.sun.max.vm.jdk;

import java.nio.*;
import java.util.*;

import sun.misc.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;

/**
 * Method sustitutions for the {@link sun.misc.Perf} class.
 *
 */
@METHOD_SUBSTITUTIONS(Perf.class)
final class JDK_sun_misc_Perf {

    private JDK_sun_misc_Perf() {
    }

    private static final int VARIABILITY_CONSTANT = 1;
    private static final int VARIABILITY_MONOTONIC = 2;
    private static final int VARIABILITY_VARIABLE = 3;

    private static final int UNITS_NONE = 1;
    private static final int UNITS_BYTES = 2;
    private static final int UNITS_TICKS = 3;
    private static final int UNITS_EVENTS = 4;
    private static final int UNITS_STRING = 5;
    private static final int UNITS_HERTZ = 6;

    static class PerfData {
        String name;
        int units;
        int variability;
        PerfData(String name, int units, int variability) {
            this.name = name;
            this.units = units;
            this.variability = variability;
        }
    }

    static class PerfString extends PerfData {
        Pointer address;
        PerfString(String name, int units, int variability, Pointer address) {
            super(name, units, variability);
            this.address = address;
        }
    }

    private static final Map<String, PerfData> perfDataMap = new HashMap<String, PerfData>();
    /**
     * Register any native methods.
     */
    @SUBSTITUTE
    private static void registerNatives() {
    }

    /**
     * Attach to the instrumentation buffer for the specified Java virtual machine.
     * @see sun.misc.Perf#attach(String, int, int)
     * @param user the name of the owner of the VM
     * @param lvmid the id of the VM
     * @param mode the attach mode
     * @return a direct allocated byte buffer
     * @throws IllegalArgumentException if the lvmid or mode was invalid
     */
    @SUBSTITUTE
    private ByteBuffer attach(String user, int lvmid, int mode) throws IllegalArgumentException {
        FatalError.unimplemented();
        return null;
    }

    /**
     * Detach from an instrumentation buffer.
     * @see sun.misc.Perf#detach(ByteBuffer)
     * @param byteBuffer the byte buffer from which to detach
     */
    @SUBSTITUTE
    private void detach(ByteBuffer byteBuffer) {
        FatalError.unimplemented();
    }

    /**
     * Create a scalar entry of type long in the instrumentation buffer.
     * @see sun.misc.Perf#createLong(String, int, int, long)
     * @param name the name of this entry
     * @param variability the variability characteristics for this entry
     * @param units the units for the entry
     * @param value the initial value for this entry
     * @return a byte buffer that allows write access to a native memory location
     */
    @SUBSTITUTE
    public ByteBuffer createLong(String name, int variability, int units, long value) {
        FatalError.unimplemented();
        return null;
    }

    /**
     * Create a byte array entry in the instrumentation buffer.
     * @param name the name of the entry
     * @param variability the variability characteristics of the entry
     * @param units the units
     * @param value the initial value of the entry
     * @param maxLength the maximum length of the entry
     * @return a byte buffer that allows write access to a native memory location
     */
    @SUBSTITUTE
    public ByteBuffer createByteArray(String name, int variability, int units, byte[] value, int maxLength) {
        if (name == null || value == null) {
            throw new NullPointerException();
        }
        if (!(variability == VARIABILITY_CONSTANT || variability == VARIABILITY_VARIABLE)) {
            throw new IllegalArgumentException("invalid variability: " + variability);
        }
        if (units != UNITS_STRING) {
            throw new IllegalArgumentException("invalid units: " + units);
        }
        PerfData perfData = perfDataMap.get(name);
        if (perfData != null) {
            throw new IllegalArgumentException("name: " + name + " already exists");
        }

        final Pointer address = Memory.mustAllocate(maxLength);
        Memory.writeBytes(value, address);
        final PerfString perfString = new PerfString(name, variability, units, address);
        perfDataMap.put(name, perfString);
        return ObjectAccess.createDirectByteBuffer(address.toLong(), maxLength);
    }

    /**
     * Returns the value of the internal high resolution counter.
     * @see sun.misc.Perf#highResCounter()
     * @return the high resolution counter's current value
     */
    @SUBSTITUTE
    public long highResCounter() {
        return System.nanoTime() - MaxineVM.getStartupTimeNano();
    }

    /**
     * Returns the frequency of the high resolution counter.
     * @see sun.misc.Perf#highResFrequency()
     * @return the high resolution counter's frequency
     */
    @SUBSTITUTE
    public long highResFrequency() {
        return 1000000000L;
    }
}
