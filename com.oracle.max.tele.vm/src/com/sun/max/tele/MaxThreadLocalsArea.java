/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Access to the {@linkplain VmThreadLocal thread local variables} related to a particular
 * {@linkplain SafepointPoll.State safepoint state} for {@linkplain MaxThread thread} in the VM.
 * The variables are stored in a region of VM memory local to the thread that does not move.
 * Variables are word sized, stored in index-order, and are accessible by either name or index.
 * If the region starts at {@link Address#zero()} then the {@linkplain VmThreadLocal thread local variables}
 * are assumed to be invalid.
 */
public interface MaxThreadLocalsArea extends MaxEntity<MaxThreadLocalsArea> {

    /**
     * Gets the thread containing this thread locals area in the VM.
     * <br>
     * Thread-safe
     *
     * @return the thread holding these locals
     */
    MaxThread thread();

    /**
     * Gets the {@linkplain SafepointPoll.State safepoint state} with which this thread locals area is associated in the VM.
     * <br>
     * Thread-safe
     *
     * @return the state
     */
    SafepointPoll.State safepointState();

    /**
     * Gets the list of {@linkplain VmThreadLocal thread local variables} from the target VM.
     * This should always be used in preference to {@link VmThreadLocal#values()}.
     */
    List<VmThreadLocal> values();

    /**
     * Gets the number of thread local variables in this thread locals area in the VM.
     * Equivalent to {@code values.size()}.
     * <br>
     * Thread-safe
     *
     * @return the number of {@linkplain MaxThreadLocalVariable thread local variables}
     */
    int variableCount();

    /**
     * Gets a thread local variable in this thread locals area,  by index.
     * <br>
     * Thread-safe
     *
     * @return the {@linkplain MaxThreadLocalVariable thread local variable} at a specified index.
     */
    MaxThreadLocalVariable getThreadLocalVariable(int index);

    /**
     * Finds by address the thread local in this block that starts at this address in the thread locals area in the VM, if any.
     * <br>
     * Thread-safe
     *
     * @return the {@linkplain MaxThreadLocalVariable thread local variable} in this thread locals area that is stored at a particular memory location in the VM, null if none.
     */
    MaxThreadLocalVariable findThreadLocalVariable(Address address);

}
