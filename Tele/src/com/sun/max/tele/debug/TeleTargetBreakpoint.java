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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.BreakpointCondition.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.value.*;

/**
 * Target code breakpoints.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public abstract class TeleTargetBreakpoint extends TeleBreakpoint {

    protected final Factory factory;

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
     * A bytecode breakpoint for which this target breakpoint was created, null if none.
     */
    private final TeleBytecodeBreakpoint owner;


    /**
     * Creates a target code breakpoint for a given address in the VM.
     *
     * @param teleVM the VM
     * @param factory the factory responsible for managing these breakpoints
     * @param address the address at which the breakpoint is to be created
     * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
     *            instruction. If this value is null, then the code will be read from {@code address}.
     * @param owner the bytecode breakpoint for which this is being created, null if none.
     * @param the kind of breakpoint
     */
    private TeleTargetBreakpoint(TeleVM teleVM, Factory factory, Address address, byte[] originalCode, BreakpointKind kind, TeleBytecodeBreakpoint owner) {
        super(teleVM, new TeleCodeLocation(teleVM, address), kind);
        this.factory = factory;
        this.owner = owner;
        this.originalCodeAtBreakpoint = originalCode == null ? teleVM.dataAccess().readFully(address, factory.codeSize()) : originalCode;
    }

    /**
     * @return address of this breakpoint in the VM.
     */
    private Address address() {
        return getCodeLocation().targetCodeInstructionAddress();
    }

    /**
     * Returns the bytecode breakpoint set by the client on whose behalf this breakpoint was created, if there is one.
     *
     * @return
     */
    public TeleBytecodeBreakpoint owner() {
        return owner;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Target breakpoint");
        sb.append("{0x").append(address().toHexString()).append(", ");
        sb.append(kind().toString()).append(", ");
        sb.append(isEnabled() ? "enabled" : "disabled");
        if (getDescription() != null) {
            sb.append(", \"").append(getDescription()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void remove() {
        factory.removeNonTransientBreakpointAt(address());
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
                teleVM().dataAccess().writeBytes(address(), factory.code());
            } else {
                // Patches the target code in the VM at this breakpoint's address with the original code that was compiled at that address.
                teleVM().dataAccess().writeBytes(address(), originalCodeAtBreakpoint);
            }
            isActive = active;
        }
    }

    /**
     * A target breakpoint set explicitly by a client.
     * <br>
     * It will be visible to clients and can be explicitly enabled/disabled/removed by the client.
     */
    private static final class ClientTargetBreakpoint extends TeleTargetBreakpoint {

        private boolean enabled = true;
        private BreakpointCondition condition;

        /**
        * A client-created breakpoint for a given target code address, enabled by default.
        *
        * @param teleVM the VM
        * @param factory the factory that manages these breakpoints.
        * @param address the address at which the breakpoint is to be created
        * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
        *            instruction. If this value is null, then the code will be read from {@code address}.
         */
        ClientTargetBreakpoint(TeleVM teleVM, Factory factory, Address address, byte[] originalCode) {
            super(teleVM, factory, address, originalCode, BreakpointKind.CLIENT, null);
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            factory.fireBreakpointsChanged();
        }

        @Override
        public BreakpointCondition getCondition() {
            return condition;
        }

        @Override
        public void setCondition(final String conditionDescriptor) throws ExpressionException {
            this.condition = new BreakpointCondition(teleVM(), conditionDescriptor);
            setTriggerEventHandler(this.condition);
        }

    }

    /**
     * A target breakpoint set for internal use by the inspection's implementation.
     * <br>
     * It may or may not be visible to clients, but can be explicitly enabled/disabled/removed by the internal
     * service for which it was created.
     */
    private static final class SystemTargetBreakpoint extends TeleTargetBreakpoint {

        private boolean enabled = true;
        private BreakpointCondition condition;

        /**
        * A system-created breakpoint for a given target code address, enabled by default.
        * There is by default no special handling, but this can be changed by overriding
        * {@link #handleTriggerEvent(TeleNativeThread)}.
        *
        * @param address the address at which the breakpoint is to be created
        * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
        *            instruction. If this value is null, then the code will be read from {@code address}.
        */
        SystemTargetBreakpoint(TeleVM teleVM, Factory factory, Address address, byte[] originalCode, TeleBytecodeBreakpoint owner) {
            super(teleVM, factory, address, originalCode, BreakpointKind.SYSTEM, owner);
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public BreakpointCondition getCondition() {
            return condition;
        }

        @Override
        public void setCondition(String conditionDescriptor) throws ExpressionException {
            condition = new BreakpointCondition(teleVM(), conditionDescriptor);
            setTriggerEventHandler(condition);
        }

    }

    /**
     * A target breakpoint set by the process controller, intended to persist only for the
     * duration of a single execution cycle.  It is always enabled for the duration of its lifetime.
     * <br>
     * It should not be visible to clients, has no condition, and cannot be explicitly enabled/disabled.
     */
    private static final class TransientTargetBreakpoint extends TeleTargetBreakpoint {

        /**
         * A transient breakpoint for a given target code address.
         *
         * @param teleVM the VM
         * @param factory the factory that manages these breakpoints
         * @param address the address at which the breakpoint is to be created
         * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
         *            instruction. If this value is null, then the code will be read from {@code address}.
         */
        TransientTargetBreakpoint(TeleVM teleVM, Factory factory, Address address, byte[] originalCode) {
            super(teleVM, factory, address, originalCode, BreakpointKind.TRANSIENT, null);
        }

        @Override
        public boolean isEnabled() {
            // Transients are always enabled
            return true;
        }

        @Override
        public void setEnabled(boolean enabled) {
            ProgramError.unexpected("Can't enable/disable transient breakpoints");
        }

        @Override
        public BreakpointCondition getCondition() {
            // Transients do not have conditions.
            return null;
        }

        @Override
        public void setCondition(String conditionDescriptor) throws ExpressionException {
            ProgramError.unexpected("Transient breakpoints do not have conditions");
        }

    }

    public static class Factory extends AbstractTeleVMHolder {

        private final byte[] code;

        // The map implementations are not thread-safe; the factory must take care of that.
        private final Map<Long, ClientTargetBreakpoint> clientBreakpoints = new HashMap<Long, ClientTargetBreakpoint>();
        private final Map<Long, SystemTargetBreakpoint> systemBreakpoints = new HashMap<Long, SystemTargetBreakpoint>();
        private final Map<Long, TransientTargetBreakpoint> transientBreakpoints = new HashMap<Long, TransientTargetBreakpoint>();

        private List<MaxBreakpointListener> breakpointListeners = new CopyOnWriteArrayList<MaxBreakpointListener>();

        public Factory(TeleVM teleVM) {
            super(teleVM);
            this.code = TargetBreakpoint.createBreakpointCode(teleVM.vmConfiguration().platform().processorKind.instructionSet);
        }

        /**
         * Adds a listener for breakpoint changes.
         *
         * @param listener a breakpoint listener
         */
        public final void addBreakpointListener(MaxBreakpointListener listener) {
            assert listener != null;
            breakpointListeners.add(listener);
        }

        /**
         * Removes a listener for breakpoint changes.
         *
         * @param listener a breakpoint listener
         */
        public final void removeBreakpointListener(MaxBreakpointListener listener) {
            assert listener != null;
            breakpointListeners.remove(listener);
        }

        /**
         * Gets the bytes encoding the platform dependent instruction(s) representing a breakpoint.
         */
        public byte[] code() {
            return code.clone();
        }

        /**
         * Gets number of bytes that encode the platform dependent instruction(s) representing a breakpoint.
         */
        public int codeSize() {
            return code.length;
        }

        /**
         * @return all the client-visible persistent target code breakpoints that currently exist
         * in the VM.  Modification safe against breakpoint removal.
         */
        public synchronized Iterable<MaxBreakpoint> clientBreakpoints() {
            return new ArrayListSequence<MaxBreakpoint>(clientBreakpoints.values());
        }

        /**
         * @return the number of client-created target breakpoints existing in the VM.
         */
        public synchronized int clientBreakpointCount() {
            return clientBreakpoints.size();
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
        public synchronized TeleTargetBreakpoint getTargetBreakpointAt(Address address) {
            final ClientTargetBreakpoint clientBreakpoint = clientBreakpoints.get(address.toLong());
            if (clientBreakpoint != null) {
                return clientBreakpoint;
            }
            final SystemTargetBreakpoint systemBreakpoint = systemBreakpoints.get(address.toLong());
            if (systemBreakpoint != null) {
                return systemBreakpoint;
            }
            return transientBreakpoints.get(address.toLong());
        }

        /**
         * Gets the client-created target code breakpoint set at a specified
         * address in the VM, if it exists, null otherwise.
         */
        public synchronized TeleTargetBreakpoint getClientTargetBreakpointAt(Address address) {
            return clientBreakpoints.get(address.toLong());
        }

        /**
         * Gets the client breakpoint at a specified target code address in the VM, creating a new one first if needed.
         *
         * @param address the address at which the breakpoint is to be created
         */
        public synchronized TeleTargetBreakpoint makeClientBreakpointAt(Address address) {
            TeleTargetBreakpoint breakpoint = getTargetBreakpointAt(address);
            if (breakpoint == null || breakpoint.isTransient()) {
                final ClientTargetBreakpoint clientBreakpoint = new ClientTargetBreakpoint(teleVM(), this, address, null);
                final TeleTargetBreakpoint oldBreakpoint = clientBreakpoints.put(address.toLong(), clientBreakpoint);
                assert oldBreakpoint == null;
                breakpoint = clientBreakpoint;
            }
            fireBreakpointsChanged();
            return breakpoint;
        }

        /**
         * Gets the system breakpoint at a specified target code address in the VM, creating a new one first if needed.
         *
         * @param address the address at which the breakpoint is to be created
         * @param owner a client-visible breakpoint for which this system breakpoint was created, null if none.
         * @return a system breakpoint at the specified target code address in the VM
         */
        public synchronized TeleTargetBreakpoint makeSystemBreakpoint(Address address, TeleBytecodeBreakpoint owner) {
            SystemTargetBreakpoint systemBreakpoint = systemBreakpoints.get(address.toLong());
            // TODO (mlvdv) handle case where there is already a client breakpoint at this address.
            if (systemBreakpoint == null) {
                systemBreakpoint = new SystemTargetBreakpoint(teleVM(), this, address, null, owner);
                final SystemTargetBreakpoint oldBreakpoint = systemBreakpoints.put(address.toLong(), systemBreakpoint);
                assert oldBreakpoint == null;
            }
            return systemBreakpoint;
        }

        /**
         * Gets the transient breakpoint at a specified target code address in the VM, creating a new one first if needed.
         *
         * @param address the address at which the breakpoint is to be created
         */
        public synchronized TeleTargetBreakpoint makeTransientBreakpoint(Address address) {
            TeleTargetBreakpoint breakpoint = getTargetBreakpointAt(address);
            if (breakpoint == null || !breakpoint.isTransient()) {
                final TransientTargetBreakpoint transientBreakpoint = new TransientTargetBreakpoint(teleVM(), this, address, null);
                final TeleTargetBreakpoint oldBreakpoint = transientBreakpoints.put(address.toLong(), transientBreakpoint);
                assert oldBreakpoint == null;
                breakpoint = transientBreakpoint;
            }
            return breakpoint;
        }

        private byte[] recoverOriginalCodeForBreakpoint(Address instructionPointer) {
            try {
                final Value result = teleVM().teleMethods().TargetBreakpoint_findOriginalCode.interpret(LongValue.from(instructionPointer.toLong()));
                final Reference reference = result.asReference();
                if (reference.isZero()) {
                    return null;
                }
                return (byte[]) reference.toJava();
            } catch (TeleInterpreterException e) {
                throw ProgramError.unexpected(e);
            }
        }

        /**
         * Removes the client or system breakpoint, if it exists, at specified target code address in the VM.
         */
        private synchronized void removeNonTransientBreakpointAt(Address address) {
            final long addressLong = address.toLong();
            if (clientBreakpoints.remove(addressLong) != null) {
                fireBreakpointsChanged();
            } else {
                systemBreakpoints.remove(addressLong);
            }
        }

        /**
         * Sets the activation state of all target breakpoints in the VM.
         *
         * @param active new activation state for all breakpoints
         * @see TeleTargetBreakpoint#setActive(boolean)
         */
        public synchronized void setActiveAll(boolean active) {
            for (TeleTargetBreakpoint breakpoint : clientBreakpoints.values()) {
                if (breakpoint.isEnabled()) {
                    breakpoint.setActive(active);
                }
            }
            for (TeleTargetBreakpoint breakpoint : systemBreakpoints.values()) {
                if (breakpoint.isEnabled()) {
                    breakpoint.setActive(active);
                }
            }
            for (TeleTargetBreakpoint breakpoint : transientBreakpoints.values()) {
                breakpoint.setActive(active);
            }
        }

        /**
         * Sets the activation state of all non-client target breakpoints in the VM.
         *
         * @param active new activation state for all breakpoints
         * @see TeleTargetBreakpoint#setActive(boolean)
         */
        public synchronized void setActiveNonClient(boolean active) {
            for (TeleTargetBreakpoint breakpoint : systemBreakpoints.values()) {
                if (breakpoint.isEnabled()) {
                    breakpoint.setActive(active);
                }
            }
            for (TeleTargetBreakpoint breakpoint : transientBreakpoints.values()) {
                breakpoint.setActive(active);
            }
        }

        /**
         * Removes and clears all state associated with transient breakpoints.
         */
        public synchronized void removeTransientBreakpoints() {
            transientBreakpoints.clear();
        }

        private void fireBreakpointsChanged() {
            for (final MaxBreakpointListener listener : breakpointListeners) {
                listener.breakpointsChanged();
            }
        }

        public void writeSummaryToStream(PrintStream printStream) {
            printStream.println("Target breakpoints :");
            for (ClientTargetBreakpoint targetBreakpoint : clientBreakpoints.values()) {
                printStream.println("  " + targetBreakpoint + describeLocation(targetBreakpoint));
            }
            for (SystemTargetBreakpoint targetBreakpoint : systemBreakpoints.values()) {
                printStream.println("  " + targetBreakpoint + describeLocation(targetBreakpoint));
            }
            for (TransientTargetBreakpoint targetBreakpoint : transientBreakpoints.values()) {
                printStream.println("  " + targetBreakpoint + describeLocation(targetBreakpoint));
            }
        }

        private String describeLocation(TeleTargetBreakpoint teleTargetBreakpoint) {
            final TeleTargetRoutine teleTargetRoutine = teleVM().findTeleTargetRoutine(TeleTargetRoutine.class, teleTargetBreakpoint.address());
            if (teleTargetRoutine != null) {
                return " in " + teleTargetRoutine.getName();
            }
            return "";
        }
    }
}
