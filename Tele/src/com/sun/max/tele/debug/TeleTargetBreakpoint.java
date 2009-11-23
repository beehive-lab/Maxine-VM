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

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.BreakpointCondition.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
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
    protected final byte[] originalCodeAtBreakpoint;
    private boolean isActivated;

    /**
     * Creates a target code breakpoint for a given address in the VM.
     *
     * @param teleProcess the tele process context of the breakpoint
     * @param factory the factory responsible for managing these breakpoints
     * @param address the address at which the breakpoint is to be created
     * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
     *            instruction. If this value is null, then the code will be read from {@code address}.
     * @param the kind of breakpoint
     */
    private TeleTargetBreakpoint(TeleVM teleVM, Factory factory, Address address, byte[] originalCode, Kind kind) {
        super(teleVM, new TeleCodeLocation(teleVM, address), kind);
        this.factory = factory;
        this.originalCodeAtBreakpoint = originalCode == null ? teleVM.dataAccess().readFully(address, factory.codeSize()) : originalCode;
    }

    /**
     * @return address of this breakpoint in the VM.
     */
    private Address address() {
        return teleCodeLocation().targetCodeInstructionAddress();
    }

    @Override
    public String toString() {
        return "Target breakpoint" + "{0x" + address().toHexString() + "} " + attributesToString();
    }

    @Override
    public void remove() {
        factory.removeNonTransientBreakpointAt(address());
    }

    /**
     * Determines if the target code is currently patched at this breakpoint's {@linkplain #address() address} with the
     * platform dependent instruction(s) implementing a breakpoint.
     */
    boolean isActivated() {
        return isActivated;
    }

    /**
     * Patches the target code at this breakpoint's address with platform dependent instruction(s) implementing a breakpoint.
     */
    void activate() {
        teleVM().dataAccess().writeBytes(address(), factory.code());
        isActivated = true;
    }

    /**
     * Patches the target code at this breakpoint's address with the original code that was compiled at that address.
     */
    private void deactivate() {
        teleVM().dataAccess().writeBytes(address(), originalCodeAtBreakpoint);
        isActivated = false;
    }

    /**
     * A target breakpoint set explicitly by a client.
     * It will be visible to clients and can be explicitly enabled/disabled/removed.
     */
    private static final class ClientTargetBreakpoint extends TeleTargetBreakpoint {

        private boolean enabled = true;
        private BreakpointCondition condition;

        /**
        * A client-created breakpoint for a given target code address, enabled by default.
        *
        * @param address the address at which the breakpoint is to be created
        * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
        *            instruction. If this value is null, then the code will be read from {@code address}.
        */
        ClientTargetBreakpoint(TeleVM teleVM, Factory factory, Address address, byte[] originalCode) {
            super(teleVM, factory, address, originalCode, Kind.CLIENT);
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public boolean setEnabled(boolean enabled) {
            if (enabled != this.enabled) {
                this.enabled = enabled;
                factory.announceStateChange();
                return true;
            }
            return false;
        }

        @Override
        public BreakpointCondition condition() {
            return condition;
        }

        @Override
        public void setCondition(String condition) throws ExpressionException {
            this.condition = new BreakpointCondition(teleVM(), condition);
        }

        @Override
        public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
            assert teleNativeThread.state() == TeleNativeThread.ThreadState.BREAKPOINT;
            if (condition == null) {
                // Unconditional break; don't continue;
                return true;
            }
            // If condition true then really break; otherwise continue silently.
            return condition.evaluate(teleNativeThread.teleProcess(), teleNativeThread);
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
         * @param address the address at which the breakpoint is to be created
         * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
         *            instruction. If this value is null, then the code will be read from {@code address}.
         */
        TransientTargetBreakpoint(TeleVM teleVM, Factory factory, Address address, byte[] originalCode) {
            super(teleVM, factory, address, originalCode, Kind.TRANSIENT);
        }

        @Override
        public boolean isEnabled() {
            // Transients are always enabled
            return true;
        }

        @Override
        public boolean setEnabled(boolean enabled) {
            ProgramError.unexpected("Can't enable/disable transient breakpoints");
            return false;
        }

        @Override
        public BreakpointCondition condition() {
            // Transients do not have conditions.
            return null;
        }

        @Override
        public void setCondition(String condition) throws ExpressionException {
            ProgramError.unexpected("Transient breakpoints do not have conditions");
        }
    }

    public static class Factory extends Observable {

        private final TeleVM teleVM;
        private final byte[] code;

        // The map implementations are not thread-safe; the factory must take care of that.
        private final Map<Long, TeleTargetBreakpoint> breakpoints = new HashMap<Long, TeleTargetBreakpoint>();
        private final Map<Long, TeleTargetBreakpoint> transientBreakpoints = new HashMap<Long, TeleTargetBreakpoint>();

        public Factory(TeleVM teleVM) {
            this.teleVM = teleVM;
            this.code = TargetBreakpoint.createBreakpointCode(teleVM.vmConfiguration().platform().processorKind.instructionSet);
        }

        /**
         * Notify all observers that there has been a state change concerning these breakpoints.
         */
        private void announceStateChange() {
            setChanged();
            notifyObservers();
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
        public synchronized Iterable<TeleTargetBreakpoint> clientBreakpoints() {
            return new ArrayListSequence<TeleTargetBreakpoint>(breakpoints.values());
        }

        /**
         * @return the number of client-created target breakpoints existing in the VM.
         */
        public synchronized int clientBreakpointCount() {
            int result = 0;
            for (TeleTargetBreakpoint teleTargetBreakpoint : breakpoints.values()) {
                if (teleTargetBreakpoint.kind() == Kind.CLIENT) {
                    result++;
                }
            }
            return result;
        }

        /**
         * Gets a target code breakpoint set at a specified address in the VM.
         * <br>
         * If both a non-transient and transient breakpoint are set at {@code address}, then the
         * non-transient breakpoint is returned.
         *
         * @return the target code breakpoint a the specified address, if it exists, null otherwise.
         */
        public synchronized TeleTargetBreakpoint getTargetBreakpointAt(Address address) {
            final TeleTargetBreakpoint breakpoint = breakpoints.get(address.toLong());
            if (breakpoint != null) {
                return breakpoint;
            }
            return transientBreakpoints.get(address.toLong());
        }

        /**
         * Gets the client-created target code breakpoint set at a specified
         * address in the VM, if it exists, null otherwise.
         */
        public synchronized TeleTargetBreakpoint getClientTargetBreakpointAt(Address address) {
            return breakpoints.get(address.toLong());
        }

        // TODO (mlvdv) Deprecate with eventual bytecode breakpoint redesign.
        /**
         * Creates a breakpoint for a given target code address.
         *
         * @param address the address at which the breakpoint is to be created
         * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
         *            instruction. If this value is null, then the code will be read from {@code address}.
         * @param isTransient specifies if the created breakpoint is to be deleted when a process execution stops or an
         *            inspection session finishes
         * @return the created breakpoint
         */
        private synchronized TeleTargetBreakpoint createBreakpoint(Address address, byte[] originalCode, boolean isTransient) {
            TeleTargetBreakpoint breakpoint;
            //breakpoint = new TeleTargetBreakpoint(teleVM, this, address, originalCode, isTransient);
            if (isTransient) {
                breakpoint = new TransientTargetBreakpoint(teleVM, this, address, originalCode);
                final TeleTargetBreakpoint oldBreakpoint = transientBreakpoints.put(address.toLong(), breakpoint);
                assert oldBreakpoint == null;
            } else {
                breakpoint = new ClientTargetBreakpoint(teleVM, this, address, originalCode);
                final TeleTargetBreakpoint oldBreakpoint = breakpoints.put(address.toLong(), breakpoint);
                assert oldBreakpoint == null;
                announceStateChange();
            }
            return breakpoint;
        }

        /**
         * Gets the breakpoint at a specified target code address in the tele VM, creating a new one first if needed.
         *
         * @param address the address at which the breakpoint is to be created
         */
        public synchronized TeleTargetBreakpoint makeClientBreakpoint(Address address) {
            TeleTargetBreakpoint breakpoint = getTargetBreakpointAt(address);
            if (breakpoint == null || breakpoint.isTransient()) {
                breakpoint = new ClientTargetBreakpoint(teleVM, this, address, null);
                final TeleTargetBreakpoint oldBreakpoint = breakpoints.put(address.toLong(), breakpoint);
                assert oldBreakpoint == null;
                announceStateChange();
            }
            return breakpoint;
        }

        /**
         * Gets the transient breakpoint at a specified target code address in the tele VM, creating a new one first if needed.
         *
         * @param address the address at which the breakpoint is to be created
         */
        public synchronized TeleTargetBreakpoint makeTransientBreakpoint(Address address) {
            TeleTargetBreakpoint breakpoint = getTargetBreakpointAt(address);
            if (breakpoint == null || !breakpoint.isTransient()) {
                breakpoint = new TransientTargetBreakpoint(teleVM, this, address, null);
                final TeleTargetBreakpoint oldBreakpoint = transientBreakpoints.put(address.toLong(), breakpoint);
                assert oldBreakpoint == null;
            }
            return breakpoint;
        }

        private byte[] recoverOriginalCodeForBreakpoint(Address instructionPointer) {
            try {
                final Value result = teleVM.teleMethods().TargetBreakpoint_findOriginalCode.interpret(LongValue.from(instructionPointer.toLong()));
                final Reference reference = result.asReference();
                if (reference.isZero()) {
                    return null;
                }
                return (byte[]) reference.toJava();
            } catch (TeleInterpreterException e) {
                throw ProgramError.unexpected(e);
            }
        }

        // TODO (mlvdv) Obsolete with eventual bytecode breakpoint redesign.
        /**
         * Registers a breakpoint that was set by the VM when it compiled a method for which a
         * {@linkplain TeleBytecodeBreakpoint bytecode breakpoint} was set. Such a breakpoint will not yet be registered
         * with this factory.
         *
         * @param thread a thread that may have just executed a breakpoint set by the VM
         */
        public void registerBreakpointSetByVM(TeleNativeThread thread) {
            final Address breakpointAddress = thread.breakpointAddressFromInstructionPointer();
            final byte[] codeBuffer = teleVM.dataAccess().readFully(breakpointAddress, codeSize());
            if (Bytes.equals(codeBuffer, code)) {
                TeleTargetBreakpoint breakpoint = getTargetBreakpointAt(breakpointAddress);
                if (breakpoint == null) {
                    final byte[] originalCode = recoverOriginalCodeForBreakpoint(breakpointAddress);
                    if (originalCode != null) {
                        breakpoint = createBreakpoint(breakpointAddress, originalCode, true);
                    } else {
                        // Breakpoint was set by VM in response to a bytecode breakpoint, which was cancelled before the VM
                        // finished compilation (race).  VM has a breakpoint, but the inspector has no record of it.
                        // Must look at the new history mechanisms, using the interpreter:
                        // - returns original code, then patch and forget
                        // - returns nothing - problem, should not happen
                    }
                } else if (!breakpoint.isEnabled()) {
                    FatalError.unexpected("found disabled tele breakpoint at same ip as VM breakpoint");
                }
            }
        }

        /**
         * Removes the breakpoint, if it exists, at specified target code address in the {@link TeleVM}.
         */
        private synchronized void removeNonTransientBreakpointAt(Address address) {
            breakpoints.remove(address.toLong());
            announceStateChange();
        }

        /**
         * {@linkplain TeleTargetBreakpoint#activate() Activates} all breakpoints (including transient breakpoints).
         */
        public synchronized void activateAll() {
            for (TeleTargetBreakpoint breakpoint : breakpoints.values()) {
                if (breakpoint.isEnabled()) {
                    breakpoint.activate();
                }
            }
            for (TeleTargetBreakpoint breakpoint : transientBreakpoints.values()) {
                breakpoint.activate();
            }
        }

        /**
         * Deactivates all {@linkplain #activateAll() activated} breakpoints.
         * @return all breakpoints that were disabled.
         */
        public synchronized Sequence<TeleTargetBreakpoint> deactivateAll() {
            final AppendableSequence<TeleTargetBreakpoint> deactivated = new ArrayListSequence<TeleTargetBreakpoint>();
            for (TeleTargetBreakpoint breakpoint : breakpoints.values()) {
                if (breakpoint.isEnabled()) {
                    breakpoint.deactivate();
                    deactivated.append(breakpoint);
                }
            }
            for (TeleTargetBreakpoint breakpoint : transientBreakpoints.values()) {
                breakpoint.deactivate();
                deactivated.append(breakpoint);
            }
            return deactivated;
        }

        /**
         * Removes and clears all state associated with transient breakpoints.
         */
        public synchronized void removeTransientBreakpoints() {
            transientBreakpoints.clear();
        }
    }
}
