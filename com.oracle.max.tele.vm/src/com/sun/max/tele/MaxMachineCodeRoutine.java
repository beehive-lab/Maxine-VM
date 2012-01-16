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
package com.sun.max.tele;

import java.io.*;

import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;

/**
 * Description of a single machine code routine in the VM, either compiled from a Java method or a block of external native code.
 * <br>
 * Note that machine code can get patched (changed) at runtime, so any caching of these results should be avoided.
 */
public interface MaxMachineCodeRoutine<MachineCode_Type extends MaxMachineCodeRoutine> extends MaxEntity<MachineCode_Type> {

    /**
     * @return VM address of the first instruction in the machine code represented by this routine. Note that this
     * may differ from the designated {@linkplain #getCallEntryLocation() entry point} of the code.
     */
    Address getCodeStart();

    /**
     * @return VM location of the first instruction in the machine code represented by this routine. Note that this
     *         may differ from the designated {@linkplain #getCallEntryLocation() call entry location} of the code.
     */
    CodeLocation getCodeStartLocation();

    /**
     * Gets the compiled entry point location for this code, which in the case of a compiled method is the
     * entry specified by the ABI in use when compiled.
     *
     * @return {@link Address#zero()} if not yet been compiled
     */
    CodeLocation getCallEntryLocation();

    /**
     * Gets a summary of various characteristics of the current machine code.
     *
     * @return meta-information about the current machine code instructions
     */
    MaxMachineCodeInfo getMachineCodeInfo();

    /**
     * Gets the count of the times the code has been observed to have changed in the VM,
     * starting with 0, the initial state of the code the first time it was observed.
     * <p>
     * This number is only related to observations, and may not correspond to actual changes
     * in the VM.
     * <p>
     * Any client of this interface should reloaded any cached information whenever the
     * version is observed to have changed.
     *
     * @return version of most recent observation of code in the VM.
     */
    int codeVersion();

    /**
     * Writes a textual disassembly of the machine code instructions.
     */
    void writeSummary(PrintStream printStream);

}
