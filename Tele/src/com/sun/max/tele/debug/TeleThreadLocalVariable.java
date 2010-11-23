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

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.value.*;

/**
 * Access to an individual thread local variable in the VM.
 *
 * @author Michael Van De Vanter
 */
public class TeleThreadLocalVariable extends AbstractTeleVMHolder implements MaxThreadLocalVariable {

    private static final int TRACE_LEVEL = 2;

    /**
     * Description of the memory region occupied by a {@linkplain MaxThreadLocalVariable thread local variable} in the VM.
     * <br>
     * The parent of this region is the {@linkplain MaxThreadLocalsArea thread locals area} in which it is contained.
     * <br>
     * This region has no children.
     */
    private static final class ThreadLocalVariableMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxThreadLocalVariable> {

        private static final List<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY = Collections.emptyList();

        private final MaxThreadLocalVariable owner;

        protected ThreadLocalVariableMemoryRegion(TeleVM teleVM, MaxThreadLocalVariable owner, String regionName, Address start, Size size) {
            super(teleVM, regionName, start, size);
            this.owner = owner;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            final MaxThreadLocalVariable threadLocalVariable = owner();
            return threadLocalVariable.thread().localsBlock().tlaFor(threadLocalVariable.safepointState()).memoryRegion();
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            return EMPTY;
        }

        public MaxThreadLocalVariable owner() {
            return owner;
        }

        public boolean isBootRegion() {
            return false;
        }
    }

    private final VmThreadLocal vmThreadLocal;
    private final TeleNativeThread teleNativeThread;
    private final Safepoint.State safepointState;
    private final ThreadLocalVariableMemoryRegion threadLocalVariableMemoryRegion;
    private final String entityDescription;
    private volatile Value value;


    public TeleThreadLocalVariable(VmThreadLocal vmThreadLocal, TeleNativeThread teleNativeThread, Safepoint.State safepointState, Address start, Size size) {
        super(teleNativeThread.vm());
        this.vmThreadLocal = vmThreadLocal;
        this.teleNativeThread = teleNativeThread;
        this.safepointState = safepointState;
        final String entityName = teleNativeThread.entityName() + " local=" + vmThreadLocal.name + " (" + safepointState + ")";
        this.threadLocalVariableMemoryRegion = new ThreadLocalVariableMemoryRegion(teleNativeThread.vm(), this, entityName, start, size);
        this.value = VoidValue.VOID;
        this.entityDescription = "thread-local variable:  " + vmThreadLocal.description;
    }

    public String entityName() {
        return threadLocalVariableMemoryRegion.regionName();
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxThreadLocalVariable> memoryRegion() {
        return threadLocalVariableMemoryRegion;
    }

    public boolean contains(Address address) {
        return threadLocalVariableMemoryRegion.contains(address);
    }

    public TeleNativeThread thread() {
        return teleNativeThread;
    }

    public Safepoint.State safepointState() {
        return safepointState;
    }

    public String variableName() {
        return vmThreadLocal.name;
    }

    public boolean isReference() {
        return vmThreadLocal.isReference;
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

    /**
     * Sets the cache of the current value of this thread local in the VM.
     *
     * @param value the current value.
     */
    void setValue(Value value) {
        this.value = value;
    }

}
