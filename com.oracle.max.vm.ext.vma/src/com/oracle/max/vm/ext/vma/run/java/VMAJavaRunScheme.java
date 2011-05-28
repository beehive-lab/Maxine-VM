/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.run.java;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.options.*;
import com.oracle.max.vm.ext.vma.runtime.*;
import com.sun.max.annotate.INLINE;
import com.sun.max.memory.VirtualMemory;
import com.sun.max.program.ProgramError;
import com.sun.max.unsafe.Address;
import com.sun.max.unsafe.Pointer;
import com.sun.max.unsafe.Size;
import com.sun.max.unsafe.Word;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.run.java.JavaRunScheme;
import com.sun.max.vm.thread.VmThread;
import com.sun.max.vm.thread.VmThreadLocal;

/**
 * Variant of {@link JavaRunScheme} that supports the VM advising framework.
 *
 */

public class VMAJavaRunScheme extends JavaRunScheme {
    /**
     * A thread local variable that is used to support VM advising, in
     * particular to indicate whether tracking is currently enabled.
     */
    public static final VmThreadLocal VM_ADVISING = new VmThreadLocal(
            "VM_ADVISING", false, "For use by VM advising framework");

    /**
     * A thread local variable that is used to support VM advising, in
     * particular the native buffer for per thread event storage.
     */
    public static final VmThreadLocal VM_ADVISING_BUFFER = new VmThreadLocal(
            "VM_ADVISING_BUFFER", false, "For use by VM advising framework");

    private static final ObjectStateHandler state = BitSetObjectStateHandler.create();

    private static final int BUFFER_SIZE = 64 * 1024;

    /**
     * Set to true when {@link VMAOptions.VMA} is set AND the VM is in a state to start advising.
     */
    private static boolean advising;

    /**
     * The runtime specified {@link VMAdviceHandler}.
     */
    private static VMAdviceHandler adviceHandler;

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.RUNNING) {
            if (VMAOptions.VMA) {
                try {
                    adviceHandler = VMAdviceHandlerFactory.create();
                    adviceHandler.initialise(state);
                } catch (Throwable ex) {
                    ProgramError.unexpected("VMA initialization failed", ex);
                }
                advising = true;
                threadStarting();
            }
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            if (advising) {
                disableAdvising();
                // N.B. daemon threads may still be running and invoking advice.
                // There is nothing we can do about that as they may be in the act
                // of logging so disabling advising for them would be meaningless.
                adviceHandler.finalise();
            }
        }
    }

    @INLINE
    public static VMAdviceHandler adviceHandler() {
        return adviceHandler;
    }

    /**
     * If the VM is being run with advising turned on, enables advising for the current thread.
     */
    public static void threadStarting() {
        if (advising) {
            Pointer buffer = VirtualMemory.allocate(Size.fromInt(BUFFER_SIZE), VirtualMemory.Type.DATA);
            assert !buffer.isZero();
            VM_ADVISING_BUFFER.store3(buffer);
            adviceHandler.adviseThreadStarting(AdviceMode.BEFORE, VmThread.current());
            enableAdvising();
        }
    }

    /**
     * If the VM is being run with advising turned on, notifies the advisee that this thread is terminating.
     */
    public static void threadTerminating() {
        if (advising) {
            adviceHandler.adviseThreadTerminating(AdviceMode.BEFORE, VmThread.current());
        }
    }

    /**
     * Unconditionally enables advising for the current thread.
     */
    @INLINE
    public static void enableAdvising() {
        VM_ADVISING.store3(Address.fromLong(1));
    }

    /**
     * Unconditionally disables advising for the current thread.
     */
    @INLINE
    public static void disableAdvising() {
        VM_ADVISING.store3(Word.zero());
    }

    /**
     * Is the VM being run with advising turned on?
     */
    @INLINE
    public static boolean isVMAdvising() {
        return advising;
    }

    /**
     * Is the VM being run with advising turned on and advising enabled for the
     * current thread?
     * @return
     */
    @INLINE
    public static boolean isEnabled() {
        return advising && VmThread.currentTLA().getWord(VM_ADVISING.index) != Word.zero();
    }

}
