/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.vm.runtime.*;
import com.sun.max.vm.value.*;

/**
 * Descriptive information about a thread local variable defined for this platform.
 */
public interface MaxThreadLocalVariable extends MaxEntity<MaxThreadLocalVariable> {

    /**
     * Gets the thread with which this thread local variable is associated in the VM.
     * <br>
     * Thread-safe
     *
     * @return the thread that contains this thread local variable in the VM
     */
    MaxThread thread();

    /**
     * Gets the {@linkplain SafePoint.State safepoint state} with which this thread local variable is associated in the VM.
     * <br>
     * Thread-safe
     *
     * @return the state
     */
    SafepointPoll.State safepointState();

    /**
     * Gets the name of this thread local variable.
     * <br>
     * Thread-safe
     *
     * @return the name
     */
    String variableName();

    /**
     * @return the description of the thread local variable.
     */
    String variableDocumentation();

    /**
     * Specifies if this thread local variable is a reference type.
     * <br>
     * Thread-safe
     *
     * @return whether this thread local variable contains a reference
     */
    boolean isReference();

    /**
     * Gets the index of this thread local variable in its thread locals area in the VM.
     * <br>
     * Thread-safe
     *
     * @return the index of this variable in the thread locals area.
     */
    int index();

    /**
     * Gets the offset in bytes of this thread local variable from the base..
     * <br>
     * Thread-safe
     *
     * @return the offset of this variable from the base of thread local variables.
     */
    int offset();

    /**
     * Gets the most recently cached value of the thread local variable in the VM.
     *
     * @return the cached value, {@link VoidValue.VOID}  if not valid.
     */
    Value value();

    /**
     * Gets the stack trace element that describes where this thread local variable is declared.
     * <br>
     * Thread-safe
     *
     * @return the stack trace element where this thread local variable is declared.
     */
    StackTraceElement declaration();

}
