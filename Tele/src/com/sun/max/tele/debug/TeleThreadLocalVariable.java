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

import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.value.*;

/**
 * Wrapper for an individual thread local variable in the VM.
 *
 * @author Michael Van De Vanter
 */
public class TeleThreadLocalVariable implements MaxThreadLocalVariable {

    private final VmThreadLocal vmThreadLocal;
    private final TeleNativeThread teleNativeThread;
    private final Safepoint.State safepointState;
    private final MemoryRegion memoryRegion;
    private volatile Value value;

    public TeleThreadLocalVariable(VmThreadLocal vmThreadLocal, TeleNativeThread teleNativeThread, Safepoint.State safepointState, MemoryRegion threadLocalMemoryRegion) {
        this.vmThreadLocal = vmThreadLocal;
        this.teleNativeThread = teleNativeThread;
        this.safepointState = safepointState;
        this.memoryRegion = threadLocalMemoryRegion;
        this.value = VoidValue.VOID;
    }

    public TeleNativeThread thread() {
        return teleNativeThread;
    }

    public Safepoint.State safepointState() {
        return safepointState;
    }

    public String name() {
        return vmThreadLocal.name;
    }

    public boolean isReference() {
        return vmThreadLocal.isReference;
    }

    public MemoryRegion memoryRegion() {
        return memoryRegion;
    }

    public int index() {
        return vmThreadLocal.index;
    }

    public int offset() {
        return vmThreadLocal.offset;
    }

    public Value value() {
        return value;
    }

    public StackTraceElement declaration() {
        return vmThreadLocal.declaration;
    }

    public String description() {
        return vmThreadLocal.description;
    }

    /**
     * Sets the cache of the current value of this thread local in the VM.
     *
     * @param value the current value.
     */
    public void setValue(Value value) {
        this.value = value;
    }

}
