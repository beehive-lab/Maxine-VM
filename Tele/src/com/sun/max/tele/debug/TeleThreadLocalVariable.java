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
package com.sun.max.tele.debug;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
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

        protected ThreadLocalVariableMemoryRegion(TeleVM teleVM, MaxThreadLocalVariable owner, String regionName, Address start, int nBytes) {
            super(teleVM, regionName, start, nBytes);
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


    public TeleThreadLocalVariable(VmThreadLocal vmThreadLocal, TeleNativeThread teleNativeThread, Safepoint.State safepointState, Address start, int nBytes) {
        super(teleNativeThread.vm());
        this.vmThreadLocal = vmThreadLocal;
        this.teleNativeThread = teleNativeThread;
        this.safepointState = safepointState;
        final String entityName = teleNativeThread.entityName() + " local=" + vmThreadLocal.name + " (" + safepointState + ")";
        this.threadLocalVariableMemoryRegion = new ThreadLocalVariableMemoryRegion(teleNativeThread.vm(), this, entityName, start, nBytes);
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

    public final TeleObject representation() {
        // No distinguished object in VM runtime represents this.
        return null;
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

    public String variableDocumentation() {        //
        return vmThreadLocal.description;
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
