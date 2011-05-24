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
package com.sun.max.vm.runtime;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.lang.Classes.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.thread.*;

/**
 * The platform specific details of the mechanism by which a thread can be
 * frozen via polling at prudently chosen execution points.
 */
public abstract class Safepoint {

    /**
     * The three states a thread can be in with respect to safepoints.
     * Note that the order of these enum matches the layout of the three
     * {@linkplain VmThreadLocal thread local areas}.
     */
    public enum State implements PoolObject {
        TRIGGERED(TTLA),
        ENABLED(ETLA),
        DISABLED(DTLA);

        public static final List<State> CONSTANTS = Arrays.asList(values());

        private final VmThreadLocal key;

        State(VmThreadLocal key) {
            this.key = key;
        }

        public int serial() {
            return ordinal();
        }

        public int offset() {
            return key.offset;
        }
    }

    @HOSTED_ONLY
    public static Safepoint create() {
        try {
            final String isa = platform().isa.name();
            final Class<?> safepointClass = Class.forName(getPackageName(Safepoint.class) + "." + isa.toLowerCase() + "." + isa + Safepoint.class.getSimpleName());
            return (Safepoint) safepointClass.newInstance();
        } catch (Exception exception) {
            exception.printStackTrace();
            throw ProgramError.unexpected("could not create safepoint: " + exception);
        }
    }

    public final byte[] code = createCode();

    protected Safepoint() {
    }

    /**
     * Gets the current value of the safepoint latch register.
     */
    @INLINE
    public static Pointer getLatchRegister() {
        return VMRegister.getSafepointLatchRegister();
    }

    /**
     * Updates the current value of the safepoint latch register.
     *
     * @param tla a pointer to a copy of the thread locals from which the base of the safepoints-enabled
     *            thread locals can be obtained
     */
    @INLINE
    public static void setLatchRegister(Pointer tla) {
        VMRegister.setSafepointLatchRegister(tla);
    }

    /**
     * Determines if safepoints are disabled for the current thread.
     * @return {@code true} if safepoints are disabled
     */
    public static boolean isDisabled() {
        return getLatchRegister().equals(DTLA.load(currentTLA()));
    }

    /**
     * Determines if safepoints are triggered for the current thread.
     * @return {@code true} if safepoints are triggered
     */
    @INLINE
    public static boolean isTriggered() {
        if (isHosted()) {
            return false;
        }
        Pointer etla = ETLA.load(currentTLA());
        return SAFEPOINT_LATCH.load(etla).equals(TTLA.load(currentTLA()));
    }

    /**
     * Disables safepoints for the current thread. To temporarily disable safepoints on a thread, a call to this method
     * should paired with a call to {@link #enable()}, passing the value returned by the former as the single
     * parameter to the later. That is:
     * <pre>
     *     boolean wasDisabled = Safepoint.disable();
     *     // perform action with safepoints disabled
     *     if (!wasDisabled) {
     *         Safepoints.enable();
     *     }
     * </pre>
     *
     * @return true if safepoints were disabled upon entry to this method
     */
    @INLINE
    public static boolean disable() {
        final boolean wasDisabled = getLatchRegister().equals(DTLA.load(currentTLA()));
        setLatchRegister(DTLA.load(currentTLA()));
        return wasDisabled;
    }

    /**
     * Enables safepoints for the current thread, irrespective of whether or not they are currently enabled.
     *
     * @see #disable()
     */
    @INLINE
    public static void enable() {
        setLatchRegister(ETLA.load(currentTLA()));
    }

    /**
     * Emits a safepoint at the call site.
     */
    @INTRINSIC(SAFEPOINT)
    public static native void safepoint();

    @HOSTED_ONLY
    protected abstract byte[] createCode();

    public boolean isAt(Pointer instructionPointer) {
        return Memory.equals(instructionPointer, code);
    }
}
