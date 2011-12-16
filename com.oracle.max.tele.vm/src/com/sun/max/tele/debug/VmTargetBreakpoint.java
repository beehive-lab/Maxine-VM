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
package com.sun.max.tele.debug;

import static com.sun.max.platform.Platform.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.debug.BreakpointCondition.ExpressionException;
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.tele.*;

/**
 * A breakpoint set at a machine code location in VM memory.
 * <p>
 * If the breakpoint location is known to point, expressed as an instance of {@link RemoteCodePointer}, into compiled
 * code in a managed region of the VM's code cache, then the breakpoint will be relocated automatically should the method
 * compilation be relocated.
 *
 * @see RemoteCodePointer
 *
 */
public abstract class VmTargetBreakpoint extends VmBreakpoint {

    private static TargetBreakpointManager manager;

    public static TargetBreakpointManager makeManager(TeleVM vm) {
        if (manager == null) {
            manager = new TargetBreakpointManager(vm);
        }
        return manager;
    }

    /**
     * A copy of the code in the VM that was replaced when the breakpoint code was patched in, saved so that it can be
     * restored when the breakpoint becomes inactive.
     * <p>
     * Assumes that the code does not change, although its original location in memory might if a compilation is relocated.
     */
    protected final byte[] originalCodeAtBreakpoint;

    /**
     * Records whether the breakpoint is active in the VM, and if so where.
     * <ul>
     * <li>{@code null} if the breakpoint is <em>inactive</em>;</li>
     * <li>the absolute location of the breakpoint in VM memory if <em>active</em>.</li>
     * </ul>
     * Note that this is the <em>only state</em> concerning code location that is held as a concrete {@link Address},
     * since it corresponds directly to an operation (writing and restoring breakpoint code in VM memory) at a specific
     * memory location. All other references to code are expressed in terms of {@link RemoteCodePointer}s, which track
     * relocated code and are canonical.
     */
    private Address activeAddress;

    /**
     * Is the location of the breakpoint in a managed code cache region.
     */
    private final boolean codeLocationIsManaged;

    private final VmBytecodeBreakpoint owner;

    /**
     * Creates a target code breakpoint for a given code location in the VM.
     *
     * @param vm the VM
     * @param codeLocation  the location at which the breakpoint is to be created
     * @param originalCode the machine code at the breakpoint location that will be overwritten by the breakpoint
     *            instruction. If this value is null, then the code will be read from the current code location.
     * @param owner the bytecode breakpoint for which this is being created, null if none.
     * @param the kind of breakpoint
     */
    private VmTargetBreakpoint(TeleVM vm, CodeLocation codeLocation, byte[] originalCode, BreakpointKind kind, VmBytecodeBreakpoint owner) {
        super(vm, codeLocation, kind);
        final VmCodeCacheRegion codeCacheRegion = vm.codeCache().findCodeCacheRegion(codeLocation.address());
        this.owner = owner;
        this.codeLocationIsManaged = codeCacheRegion != null && codeCacheRegion.isManaged();
        this.originalCodeAtBreakpoint = originalCode == null ? vm.memory().readBytes(codeLocation.address(), manager.codeSize()) : originalCode;
    }

    public boolean isBytecodeBreakpoint() {
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Target breakpoint");
        sb.append("{0x").append(codeLocation().address().toHexString()).append(", ");
        sb.append(kind().toString()).append(", ");
        sb.append(isEnabled() ? "enabled" : "disabled").append(", ");
        sb.append(isActive() ? "active" : "inactive");
        if (getDescription() != null) {
            sb.append(", \"").append(getDescription()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }


    public VmBytecodeBreakpoint owner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     * <br>
     * Thread-safe; synchronizes on the VM lock.
     */
    @Override
    public void remove() throws MaxVMBusyException {
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            manager.removeNonTransientBreakpointAt(codeLocation().codePointer());
        } finally {
            vm().unlock();
        }
    }

    /**
     * Determines if the target code in the VM is currently patched at this breakpoint's {@linkplain #address() address} with the
     * platform-dependent target instructions implementing a breakpoint.
     */
    boolean isActive() {
        return activeAddress != null;
    }

    /**
     * Sets the activation state of the breakpoint in the VM; no-op if breakpoint already in that state.
     *
     * @param active new activation state for the breakpoint
     */
    void setActive(boolean active) {
        if (active != isActive()) {
            if (active) {
                // Make the breakpoint active, using the current absolute memory address of the code location.
                // Patches the target code in the VM at this breakpoint's address with platform-dependent target instructions implementing a breakpoint.
                final Address newActiveAddress = codeLocation().address();
                memory().writeBytes(newActiveAddress, manager.code());
                activeAddress = newActiveAddress;
            } else {
                // Make the breakpoint inactive:  patch the memory at the original breakpoint location with code originally there.
                memory().writeBytes(activeAddress, originalCodeAtBreakpoint);
                activeAddress = null;
            }
        }
    }

    /**
     * A target breakpoint set explicitly by a client, with an optional <em>condition</em>.
     * <p>
     * This kind of breakpoint is visible to clients and can be explicitly enabled/disabled/removed by the client.
     */
    private static final class ClientTargetBreakpoint extends VmTargetBreakpoint {

        private boolean enabled = true;
        private BreakpointCondition condition;

        /**
         * A client-created breakpoint for a given machine code location in VM memory, enabled by default.
         *
         * @param vm the VM
         * @param manager the manager that manages these breakpoints.
         * @param codeLocation the location at which the breakpoint is to be created
         * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
         *            instruction. If this value is null, then the code will be read from {@code address}.
         */
        ClientTargetBreakpoint(TeleVM vm, TargetBreakpointManager manager, CodeLocation codeLocation, byte[] originalCode) {
            super(vm, codeLocation, originalCode, BreakpointKind.CLIENT, null);
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) throws MaxVMBusyException {
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            try {
                this.enabled = enabled;
                manager.updateAfterBreakpointChanges(true);
            } finally {
                vm().unlock();
            }
        }

        @Override
        public BreakpointCondition getCondition() {
            return condition;
        }

        @Override
        public void setCondition(final String conditionDescriptor) throws ExpressionException, MaxVMBusyException {
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            try {
                this.condition = new BreakpointCondition(vm(), conditionDescriptor);
                setTriggerEventHandler(this.condition);
            } finally {
                vm().unlock();
            }
        }

    }

    /**
     * A target breakpoint set for internal use by other inspection services, with optional condition.
     * <p>
     * It may or may not be visible to clients, but can be explicitly enabled/disabled/removed by the internal
     * service for which it was created.
     */
    private static final class SystemTargetBreakpoint extends VmTargetBreakpoint {

        private boolean enabled = true;
        private BreakpointCondition condition;

        /**
        * A system-created breakpoint for a given target code address, enabled by default.
        * There is by default no special handling, but this can be changed by overriding
        * {@link #handleTriggerEvent(TeleNativeThread)}.
        *
        * @param codeLocation the location at which the breakpoint will be created
        * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
        *            instruction. If this value is null, then the code will be read from {@code address}.
        */
        SystemTargetBreakpoint(TeleVM vm, TargetBreakpointManager manager, CodeLocation codeLocation, byte[] originalCode, VmBytecodeBreakpoint owner) {
            super(vm, codeLocation, originalCode, BreakpointKind.SYSTEM, owner);
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) throws MaxVMBusyException {
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            try {
                this.enabled = enabled;
            } finally {
                vm().unlock();
            }
        }

        @Override
        public BreakpointCondition getCondition() {
            return condition;
        }

        @Override
        public void setCondition(String conditionDescriptor) throws ExpressionException, MaxVMBusyException {
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            try {
                condition = new BreakpointCondition(vm(), conditionDescriptor);
                setTriggerEventHandler(condition);
            } finally {
                vm().unlock();
            }
        }

    }

    /**
     * A target breakpoint set by the process controller, intended to persist only for the
     * duration of a single execution cycle.  It is always enabled for the duration of its lifetime.
     * <br>
     * It should not be visible to clients, has no condition, and cannot be explicitly enabled/disabled.
     */
    private static final class TransientTargetBreakpoint extends VmTargetBreakpoint {

        /**
         * A transient breakpoint for a given target code address.
         *
         * @param vm the VM
         * @param manager the manager for these breakpoints
         * @param codeLocation location containing the target code address at which the breakpoint is to be created
         * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
         *            instruction. If this value is null, then the code will be read from {@code address}.
         */
        TransientTargetBreakpoint(TeleVM vm, TargetBreakpointManager manager, CodeLocation codeLocation, byte[] originalCode) {
            super(vm, codeLocation, originalCode, BreakpointKind.TRANSIENT, null);
        }

        @Override
        public boolean isEnabled() {
            // Transients are always enabled
            return true;
        }

        @Override
        public void setEnabled(boolean enabled) {
            TeleError.unexpected("Can't enable/disable transient breakpoints");
        }

        @Override
        public BreakpointCondition getCondition() {
            // Transients do not have conditions.
            return null;
        }

        @Override
        public void setCondition(String conditionDescriptor) throws ExpressionException {
            TeleError.unexpected("Transient breakpoints do not have conditions");
        }

    }

    public static final class TargetBreakpointManager extends AbstractVmHolder implements TeleVMCache {

        private final byte[] code;

        // Maps:  RemoteCodePointer --> TargetBreakpoint
        // This relies on RemoteCodePointers being canonical
        // The map implementations are not thread-safe; the manager must take care of that.
        private final Map<RemoteCodePointer, ClientTargetBreakpoint> clientBreakpoints = new HashMap<RemoteCodePointer, ClientTargetBreakpoint>();
        private final Map<RemoteCodePointer, SystemTargetBreakpoint> systemBreakpoints = new HashMap<RemoteCodePointer, SystemTargetBreakpoint>();
        private final Map<RemoteCodePointer, TransientTargetBreakpoint> transientBreakpoints = new HashMap<RemoteCodePointer, TransientTargetBreakpoint>();

        // Thread-safe, immutable versions of the client map. Will be read many, many more times than will change.
        private volatile List<ClientTargetBreakpoint> clientBreakpointsCache = Collections.emptyList();

        private List<MaxBreakpointListener> breakpointListeners = new CopyOnWriteArrayList<MaxBreakpointListener>();

        private TargetBreakpointManager(TeleVM vm) {
            super(vm);
            this.code = TargetBreakpoint.createBreakpointCode(platform().isa);
        }

        public void updateCache(long epoch) {
            // Review client breakpoints, those set explicitly in machine code only, and handle evictions.
            final List<VmTargetBreakpoint> evictedClientBreakpoints = new ArrayList<VmTargetBreakpoint>();
            for (VmTargetBreakpoint clientBreakpoint : clientBreakpoints.values()) {
                if (!clientBreakpoint.codeLocation().codePointer().isCodeLive()) {
                    evictedClientBreakpoints.add(clientBreakpoint);
                }
            }
            for (VmTargetBreakpoint evictedClientBreakpoint : evictedClientBreakpoints) {
                final String reason = "The compilation has been evicted from the code cache";
                TeleWarning.message("Breakpoint removed: " + evictedClientBreakpoint + "(" + reason + ")");
                for (final MaxBreakpointListener listener : breakpointListeners) {
                    listener.breakpointToBeDeleted(evictedClientBreakpoint, reason);
                }
                removeNonTransientBreakpointAt(evictedClientBreakpoint.codeLocation().codePointer());
            }
            // Check for system breakpoint set on behalf of a bytecode breakpoint
            final List<VmTargetBreakpoint> evictedSystemBreakpoints = new ArrayList<VmTargetBreakpoint>();
            for (VmTargetBreakpoint systemBreakpoint : systemBreakpoints.values()) {
                if (systemBreakpoint.owner != null && !systemBreakpoint.codeLocation().codePointer().isCodeLive()) {
                    // The compilation in which this breakpoint is set has been evicted and is no longer live.
                    evictedSystemBreakpoints.add(systemBreakpoint);
                }
            }
            for (VmTargetBreakpoint evictedSystemBreakpoint : evictedSystemBreakpoints) {
                removeNonTransientBreakpointAt(evictedSystemBreakpoint.codeLocation().codePointer());
                // Notify the bytecode breakpoint for which this breakpoint was originally created.
                evictedSystemBreakpoint.owner().notifyCompilationEvicted(evictedSystemBreakpoint);
            }
        }

        /**
         * Adds a listener for breakpoint changes.
         *
         * @param listener a breakpoint listener
         */
        void addListener(MaxBreakpointListener listener) {
            assert listener != null;
            breakpointListeners.add(listener);
        }

        /**
         * Removes a listener for breakpoint changes.
         *
         * @param listener a breakpoint listener
         */
        void removeListener(MaxBreakpointListener listener) {
            assert listener != null;
            breakpointListeners.remove(listener);
        }

        /**
         * Gets the bytes encoding the platform dependent instruction(s) representing a breakpoint.
         */
        private byte[] code() {
            return code.clone();
        }

        // TODO (mlvdv) inline
        /**
         * Gets number of bytes that encode the platform dependent instruction(s) representing a breakpoint.
         */
        int codeSize() {
            return code.length;
        }

        /**
         * @return all the client-visible persistent target code breakpoints that currently exist
         * in the VM.  Modification safe against breakpoint removal.
         */
        synchronized List<ClientTargetBreakpoint> clientBreakpoints() {
            return clientBreakpointsCache;
        }

        /**
         * Gets a target code breakpoint set at a specified address in the VM.
         * <br>
         * If multiple breakpoints are set at {@code address}, then one is selected
         * according to the following preference:  a client breakpoint, if one exists,
         * otherwise a system breakpoint, if one exists, otherwise a transient breakpoint,
         * if one exists.
         *
         * @return the target code breakpoint a the specified address, if it exists, null otherwise.
         */
        synchronized VmTargetBreakpoint find(RemoteCodePointer codePointer) {
            final ClientTargetBreakpoint clientBreakpoint = clientBreakpoints.get(codePointer);
            if (clientBreakpoint != null) {
                return clientBreakpoint;
            }
            final SystemTargetBreakpoint systemBreakpoint = systemBreakpoints.get(codePointer);
            if (systemBreakpoint != null) {
                return systemBreakpoint;
            }
            TransientTargetBreakpoint transientTargetBreakpoint = transientBreakpoints.get(codePointer);
            if (transientTargetBreakpoint != null) {
                return transientTargetBreakpoint;
            }
            try {
                byte[] c = new byte[code.length];
                memory().readBytes(codePointer.getAddress(), c);
                if (Arrays.equals(c, code)) {
                    final MachineCodeLocation codeLocation = vm().codeLocationFactory().createMachineCodeLocation(codePointer.getAddress(), "discovered breakpoint");
                    return new TransientTargetBreakpoint(vm(), this, codeLocation, null);
                }
            } catch (DataIOError e) {
                e.printStackTrace();
            }
            return null;
        }

        synchronized VmTargetBreakpoint findClientBreakpoint(RemoteCodePointer codePointer) {
            return clientBreakpoints.get(codePointer);
        }

        /**
         * Return a client-visible target code breakpoint, creating a new one if none exists at that location.
         * <br>
         * Thread-safe (synchronizes on the VM lock)
         *
         * @param codeLocation location (with address) for the breakpoint
         * @return a possibly new target code breakpoint
         * @throws MaxVMBusyException
         */
        VmTargetBreakpoint makeClientBreakpoint(CodeLocation codeLocation) throws MaxVMBusyException {
            assert codeLocation.hasAddress();
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            VmTargetBreakpoint breakpoint;
            try {
                breakpoint = find(codeLocation.codePointer());
                if (breakpoint == null || breakpoint.isTransient()) {
                    final ClientTargetBreakpoint clientBreakpoint = new ClientTargetBreakpoint(vm(), this, codeLocation, null);
                    final VmTargetBreakpoint oldBreakpoint = clientBreakpoints.put(codeLocation.codePointer(), clientBreakpoint);
                    assert oldBreakpoint == null;
                    breakpoint = clientBreakpoint;
                    updateAfterBreakpointChanges(true);
                }
            } finally {
                vm().unlock();
            }
            return breakpoint;
        }

        /**
         * Return a client-invisible target code breakpoint, creating a new one if none exists at that location.
         * <br>
         * Thread-safe (synchronizes on the VM lock)
         *
         * @param codeLocation location (with address) for the breakpoint
         * @param handler an optional handler to be invoked when the breakpoint triggers
         * @param owner a bytecode breakpoint for which this breakpoint is being created.
         * @return a possibly new target code breakpoint
         * @throws MaxVMBusyException
         */
        VmTargetBreakpoint makeSystemBreakpoint(CodeLocation codeLocation, VMTriggerEventHandler handler, VmBytecodeBreakpoint owner) throws MaxVMBusyException {
            vm().lock();
            assert codeLocation.hasAddress();
            TeleError.check(codeLocation.codePointer().isCodeLive());

            SystemTargetBreakpoint systemBreakpoint;
            try {
                systemBreakpoint = systemBreakpoints.get(codeLocation.codePointer());
                // TODO (mlvdv) handle case where there is already a client breakpoint at this address.
                if (systemBreakpoint == null) {
                    systemBreakpoint = new SystemTargetBreakpoint(vm(), this, codeLocation, null, owner);
                    systemBreakpoint.setTriggerEventHandler(handler);
                    systemBreakpoint.setDescription(codeLocation.description());
                    final SystemTargetBreakpoint oldBreakpoint = systemBreakpoints.put(codeLocation.codePointer(), systemBreakpoint);
                    assert oldBreakpoint == null;
                    updateAfterBreakpointChanges(false);
                }
            } finally {
                vm().unlock();
            }
            return systemBreakpoint;
        }

        /**
         * Return a client-invisible target code breakpoint, creating a new one if none exists at that location.
         * <br>
         * Thread-safe (synchronizes on the VM lock)
         *
         * @param codeLocation location (with address) for the breakpoint
         * @param handler an optional handler to be invoked when the breakpoint triggers
         * @return a possibly new target code breakpoint
         * @throws MaxVMBusyException
         */
        VmTargetBreakpoint makeSystemBreakpoint(CodeLocation codeLocation, VMTriggerEventHandler handler) throws MaxVMBusyException {
            return makeSystemBreakpoint(codeLocation, handler, null);
        }


        /**
         * Return a client-invisible transient breakpoint at a specified target code address in the VM, creating a new one first if needed.
         * <br>
         * Thread-safe (synchronized on the VM lock)
         *
         * @param codeLocation location (with address) for the breakpoint
         * @return a possibly new target code breakpoint
         * @throws MaxVMBusyException
         */
        VmTargetBreakpoint makeTransientBreakpoint(CodeLocation codeLocation) throws MaxVMBusyException {
            assert codeLocation.hasAddress();
            TeleError.check(codeLocation.address().isNotZero());
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            try {
                VmTargetBreakpoint breakpoint = find(codeLocation.codePointer());
                if (breakpoint == null || !breakpoint.isTransient()) {
                    final TransientTargetBreakpoint transientBreakpoint = new TransientTargetBreakpoint(vm(), this, codeLocation, null);
                    final VmTargetBreakpoint oldBreakpoint = transientBreakpoints.put(codeLocation.codePointer(), transientBreakpoint);
                    assert oldBreakpoint == null;
                    breakpoint = transientBreakpoint;
                    updateAfterBreakpointChanges(false);
                }
                return breakpoint;
            } finally {
                vm().unlock();
            }
        }

        /**
         * Removes the client or system breakpoint, if it exists, at specified target code location in the VM.
         */
        private void removeNonTransientBreakpointAt(RemoteCodePointer codePointer) {
            if (clientBreakpoints.remove(codePointer) != null) {
                updateAfterBreakpointChanges(true);
            } else {
                if (systemBreakpoints.remove(codePointer) != null) {
                    updateAfterBreakpointChanges(false);
                }
            }
        }

        /**
         * Sets the activation state of all target breakpoints in the VM.
         * <br>
         * Assumes VM lock held
         *
         * @param active new activation state for all breakpoints
         * @see VmTargetBreakpoint#setActive(boolean)
         */
        void setActiveAll(boolean active) {
            assert vm().lockHeldByCurrentThread();
            for (VmTargetBreakpoint breakpoint : clientBreakpoints.values()) {
                if (breakpoint.isEnabled()) {
                    breakpoint.setActive(active);
                }
            }
            for (VmTargetBreakpoint breakpoint : systemBreakpoints.values()) {
                if (breakpoint.isEnabled()) {
                    breakpoint.setActive(active);
                }
            }
            for (VmTargetBreakpoint breakpoint : transientBreakpoints.values()) {
                breakpoint.setActive(active);
            }
        }

        /**
         * Sets the activation state of all non-client target breakpoints in the VM.
        * <br>
         * Assumes VM lock held
         *
         * @param active new activation state for all breakpoints
         * @see VmTargetBreakpoint#setActive(boolean)
         */
        void setActiveNonClient(boolean active) {
            assert vm().lockHeldByCurrentThread();
            for (VmTargetBreakpoint breakpoint : systemBreakpoints.values()) {
                if (breakpoint.isEnabled()) {
                    breakpoint.setActive(active);
                }
            }
            for (VmTargetBreakpoint breakpoint : transientBreakpoints.values()) {
                breakpoint.setActive(active);
            }
        }

        /**
         * Removes and clears all state associated with transient breakpoints.
         */
        void removeTransientBreakpoints() {
            assert vm().lockHeldByCurrentThread();
            if (transientBreakpoints.size() > 0) {
                transientBreakpoints.clear();
            }
            updateAfterBreakpointChanges(false);
        }

        /**
         * Update immutable cache of breakpoint list and possibly notify listeners.
         *
         * @param announce whether to notify listeners
         */
        private void updateAfterBreakpointChanges(boolean announce) {
            clientBreakpointsCache = Collections.unmodifiableList(new ArrayList<ClientTargetBreakpoint>(clientBreakpoints.values()));
            if (announce) {
                for (final MaxBreakpointListener listener : breakpointListeners) {
                    listener.breakpointsChanged();
                }
            }
        }

        /**
         * Writes a description of every target breakpoint to the stream, including those usually not shown to clients,
         * with more detail than typically displayed.
         * <br>
         * Thread-safe
         *
         * @param printStream
         */
        void writeSummaryToStream(PrintStream printStream) {
            printStream.println("Target breakpoints :");
            for (ClientTargetBreakpoint targetBreakpoint : clientBreakpointsCache) {
                printStream.println("  " + targetBreakpoint + describeLocation(targetBreakpoint));
            }
            for (SystemTargetBreakpoint targetBreakpoint : systemBreakpoints.values()) {
                printStream.println("  " + targetBreakpoint + describeLocation(targetBreakpoint));
            }
            for (TransientTargetBreakpoint targetBreakpoint : transientBreakpoints.values()) {
                printStream.println("  " + targetBreakpoint + describeLocation(targetBreakpoint));
            }
        }

        private String describeLocation(VmTargetBreakpoint targetBreakpoint) {
            final MaxMachineCodeRoutine maxMachineCode = vm().machineCode().findMachineCode(targetBreakpoint.codeLocation().codePointer().getAddress());
            if (maxMachineCode != null) {
                return " in " + maxMachineCode.entityName();
            }
            return "";
        }
    }
}
