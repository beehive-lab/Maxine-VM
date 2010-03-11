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
package com.sun.max.tele;

import com.sun.max.memory.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.value.*;

/**
 * Descriptive information about a thread local variable defined for this platform.
 *
 * @author Michael Van De Vanter
 */
public interface MaxThreadLocalVariable {

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
    Safepoint.State safepointState();

    /**
     * Gets the name of this thread local variable.
     * <br>
     * Thread-safe
     *
     * @return the name
     *
     */
    String name();

    /**
     * Specifies if this thread local variable is a {@link Reference}.
     * <br>
     * Thread-safe
     *
     * @return whether this thread local variable contains a reference
     */
    boolean isReference();

    /**
     * Gets a description of the VM memory occupied by this thread local variable.
     * <br>
     * Thread-safe
     *
     * @return the memory in the VM occupied by this thread local variable.
     */
    MemoryRegion memoryRegion();

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

    /**
     * Gets a string documenting the purpose of the thread local variable.
     * <br>
     * Thread-safe
     *
     * @return a very short string describing the thread local variable, useful for debugging.
     */
    String description();

}
