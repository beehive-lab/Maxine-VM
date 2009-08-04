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
package com.sun.max.vm.jdk;

import java.nio.*;

import sun.misc.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.runtime.*;

/**
 * Method sustitutions for the {@link sun.misc.Perf} class.
 *
 */
@METHOD_SUBSTITUTIONS(Perf.class)
final class JDK_sun_misc_Perf {

    private JDK_sun_misc_Perf() {
    }

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
        FatalError.unimplemented();
        return null;
    }

    /**
     * Returns the value of the internal high resolution counter.
     * @see sun.misc.Perf#highResCounter()
     * @return the high resolution counter's current value
     */
    @SUBSTITUTE
    public long highResCounter() {
        FatalError.unimplemented();
        return 0L;
    }

    /**
     * Returns the frequency of the high resolution counter.
     * @see sun.misc.Perf#highResFrequency()
     * @return the high resolution counter's frequency
     */
    @SUBSTITUTE
    public long highResFrequency() {
        FatalError.unimplemented();
        return 0L;
    }
}
