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
 * Target code breakpoints.
 */
public abstract class VmTargetBreakpoint extends VmBreakpoint {

    protected final TargetBreakpointManager manager;

    /**
     * The original code from the target code in the VM that was present before
     * the breakpoint code was patched in.
     */
    protected final byte[] originalCodeAtBreakpoint;

    /**
     * Whether the breakpoint is actually active in the VM at the moment.
     */
    private boolean isActive;

    /**
     * Creates a target code breakpoint for a given address in the VM.
     *
     * @param vm the VM
     * @param manager the manager responsible for managing these breakpoints
     * @param codeLocation  the location at which the breakpoint is to be created, by address
     * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
     *            instruction. If this value is null, then the code will be read from {@code address}.
     * @param owner the bytecode breakpoint for which this is being created, null if none.
     * @param the kind of breakpoint
     */
    private VmTargetBreakpoint(TeleVM vm, TargetBreakpointManager manager, CodeLocation codeLocation, byte[] originalCode, BreakpointKind kind, VmBytecodeBreakpoint owner) {
        super(vm, codeLocation, kind, owner);
        this.manager = manager;
        this.originalCodeAtBreakpoint = originalCode == null ? vm.memory().readBytes(codeLocation.address(), manager.codeSize()) : originalCode;
    }

    public boolean isBytecodeBreakpoint() {
        return false;
    }

    /**
     * @return address of this breakpoint in the VM.
     */
    private Address address() {
        return codeLocation().address();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Target breakpoint");
        sb.append("{0x").append(address().toHexString()).append(", ");
        sb.append(kind().toString()).append(", ");
        sb.append(isEnabled() ? "enabled" : "disabled").append(", ");
        sb.append(isActive() ? "active" : "inactive");
        if (getDescription() != null) {
            sb.append(", \"").append(getDescription()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void remove() throws MaxVMBusyException {
        manager.removeNonTransientBreakpointAt(address());
    }

    /**
     * Determines if the target code in the VM is currently patched at this breakpoint's {@linkplain #address() address} with the
     * platform-dependent target instructions implementing a breakpoint.
     */
    boolean isActive() {
        return isActive;
    }

    /**
     * Sets the activation state of the breakpoint in the VM.
     *
     * @param active new activation state for the breakpoint
     */
    void setActive(boolean active) {
        if (active != isActive) {
            if (active) {
                // Patches the target code in the VM at this breakpoint's address with platform-dependent target instructions implementing a breakpoint.
                memory().writeBytes(address(), manager.code());
            } else {
                // Patches the target code in the VM at this breakpoint's address with the original code that was compiled at that address.
                memory().writeBytes(address(), originalCodeAtBreakpoint);
            }
            isActive = active;
        }
    }

    /**
     * A target breakpoint set explicitly by a client.
     * <br>
     * It will be visible to clients and can be explicitly enabled/disabled/removed by the client.
     */
    private static final class ClientTargetBreakpoint extends VmTargetBreakpoint {

        private boolean enabled = true;
        private BreakpointCondition condition;

        /**
         * A client-created breakpoint for a given target code address, enabled by default.
         *
         * @param vm the VM
         * @param manager the manager that manages these breakpoints.
         * @param codeLocation the location at which the breakpoint is to be created, by address
         * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
         *            instruction. If this value is null, then the code will be read from {@code address}.
         */
        ClientTargetBreakpoint(TeleVM vm, TargetBreakpointManager manager, CodeLocation codeLocation, byte[] originalCode) {
            super(vm, manager, codeLocation, originalCode, BreakpointKind.CLIENT, null);
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
     * A target breakpoint set for internal use by the inspection's implementation.
     * <br>
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
        * @param codeLocation the location at which the breakpoint will be created, by address
        * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
        *            instruction. If this value is null, then the code will be read from {@code address}.
        */
        SystemTargetBreakpoint(TeleVM vm, TargetBreakpointManager manager, CodeLocation codeLocation, byte[] originalCode, VmBytecodeBreakpoint owner) {
            super(vm, manager, codeLocation, originalCode, BreakpointKind.SYSTEM, owner);
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
            super(vm, manager, codeLocation, originalCode, BreakpointKind.TRANSIENT, null);
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

    public static final class TargetBreakpointManager extends AbstractVmHolder {

        private final byte[] code;

        // The map implementations are not thread-safe; the manager must take care of that.
        private final Map<Long, ClientTargetBreakpoint> clientBreakpoints = new HashMap<Long, ClientTargetBreakpoint>();
        private final Map<Long, SystemTargetBreakpoint> systemBreakpoints = new HashMap<Long, SystemTargetBreakpoint>();
        private final Map<Long, TransientTargetBreakpoint> transientBreakpoints = new HashMap<Long, TransientTargetBreakpoint>();


        // Thread-safe, immutable versions of the client map. Will be read many, many more times than will change.
        private volatile List<ClientTargetBreakpoint> clientBreakpointsCache = Collections.emptyList();

        private List<MaxBreakpointListener> breakpointListeners = new CopyOnWriteArrayList<MaxBreakpointListener>();

        TargetBreakpointManager(TeleVM vm) {
            super(vm);
            this.code = TargetBreakpoint.createBreakpointCode(platform().isa);
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
        synchronized VmTargetBreakpoint getTargetBreakpointAt(Address address) {
            final ClientTargetBreakpoint clientBreakpoint = clientBreakpoints.get(address.toLong());
            if (clientBreakpoint != null) {
                return clientBreakpoint;
            }
            final SystemTargetBreakpoint systemBreakpoint = systemBreakpoints.get(address.toLong());
            if (systemBreakpoint != null) {
                return systemBreakpoint;
            }
            TransientTargetBreakpoint transientTargetBreakpoint = transientBreakpoints.get(address.toLong());
            if (transientTargetBreakpoint != null) {
                return transientTargetBreakpoint;
            }
            try {
                byte[] c = new byte[code.length];
                memory().readBytes(address, c);
                if (Arrays.equals(c, code)) {
                    CodeLocation codeLocation = vm().codeLocationFactory().createMachineCodeLocation(address, "discovered bkpt");
                    return new TransientTargetBreakpoint(vm(), this, codeLocation, null);
                }
            } catch (DataIOError e) {
                e.printStackTrace();
            }
            return null;
        }

        public synchronized VmTargetBreakpoint findClientBreakpoint(MachineCodeLocation compiledCodeLocation) {
            assert compiledCodeLocation.hasAddress();
            return clientBreakpoints.get(compiledCodeLocation.address().toLong());
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
                breakpoint = getTargetBreakpointAt(codeLocation.address());
                if (breakpoint == null || breakpoint.isTransient()) {
                    final ClientTargetBreakpoint clientBreakpoint = new ClientTargetBreakpoint(vm(), this, codeLocation, null);
                    final VmTargetBreakpoint oldBreakpoint = clientBreakpoints.put(codeLocation.address().toLong(), clientBreakpoint);
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
         * @return a possibly new target code breakpoint
         * @throws MaxVMBusyException
         */
        VmTargetBreakpoint makeSystemBreakpoint(CodeLocation codeLocation, VMTriggerEventHandler handler, VmBytecodeBreakpoint owner) throws MaxVMBusyException {
            vm().lock();
            assert codeLocation.hasAddress();
            final Address address = codeLocation.address();
            TeleError.check(!address.isZero());

            SystemTargetBreakpoint systemBreakpoint;
            try {
                systemBreakpoint = systemBreakpoints.get(address.toLong());
                // TODO (mlvdv) handle case where there is already a client breakpoint at this address.
                if (systemBreakpoint == null) {
                    systemBreakpoint = new SystemTargetBreakpoint(vm(), this, codeLocation, null, owner);
                    systemBreakpoint.setTriggerEventHandler(handler);
                    final SystemTargetBreakpoint oldBreakpoint = systemBreakpoints.put(address.toLong(), systemBreakpoint);
                    assert oldBreakpoint == null;
                    updateAfterBreakpointChanges(false);
                }
            } finally {
                vm().unlock();
            }
            return systemBreakpoint;
        }

        public VmTargetBreakpoint makeSystemBreakpoint(CodeLocation codeLocation, VMTriggerEventHandler handler) throws MaxVMBusyException {
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
            final Address address = codeLocation.address();
            TeleError.check(!address.isZero());
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            try {
                VmTargetBreakpoint breakpoint = getTargetBreakpointAt(address);
                if (breakpoint == null || !breakpoint.isTransient()) {
                    final TransientTargetBreakpoint transientBreakpoint = new TransientTargetBreakpoint(vm(), this, codeLocation, null);
                    final VmTargetBreakpoint oldBreakpoint = transientBreakpoints.put(address.toLong(), transientBreakpoint);
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
         * Removes the client or system breakpoint, if it exists, at specified target code address in the VM.
         * <br>
         * Thread-safe; synchronizes on the VM lock
         *
         * @param address
         * @throws MaxVMBusyException
         */
        private void removeNonTransientBreakpointAt(Address address) throws MaxVMBusyException {
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            try {
                final long addressLong = address.toLong();
                if (clientBreakpoints.remove(addressLong) != null) {
                    updateAfterBreakpointChanges(true);
                } else {
                    if (systemBreakpoints.remove(addressLong) != null) {
                        updateAfterBreakpointChanges(false);
                    }
                }
            } finally {
                vm().unlock();
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
            final MaxMachineCode maxMachineCode = vm().codeCache().findMachineCode(targetBreakpoint.address());
            if (maxMachineCode != null) {
                return " in " + maxMachineCode.entityName();
            }
            return "";
        }
    }
}
