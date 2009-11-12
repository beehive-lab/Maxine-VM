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
 * Machine code breakpoints.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public final class TeleTargetBreakpoint extends TeleBreakpoint {

    private final Factory factory;

    private final TeleCodeLocation teleCodeLocation;

    @Override
    public TeleCodeLocation teleCodeLocation() {
        return teleCodeLocation;
    }

    public Address address() {
        return teleCodeLocation.targetCodeInstructionAddress();
    }

    private final byte[] originalCode;

    public byte[] originalCode() {
        return originalCode;
    }

    private boolean enabled = true;

    private boolean activated;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        assert !isTransient() : "cannot disable a transient breakpoint: " + this;
        if (enabled != this.enabled) {
            this.enabled = enabled;
            factory.announceStateChange();
            return true;
        }
        return false;
    }

    private BreakpointCondition condition;

    @Override
    public BreakpointCondition condition() {
        return condition;
    }

    @Override
    public void remove() {
        factory.removeBreakpointAt(address());
    }

    /**
     * Creates a breakpoint for a given target code address.
     *
     * @param teleProcess the tele process context of the breakpoint
     * @param factory the factory responsible for managing these breakpoints
     * @param address the address at which the breakpoint is to be created
     * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
     *            instruction. If this value is null, then the code will be read from {@code address}.
     * @param isTransient specifies if the created breakpoint is to be deleted when a process execution stops or an
     *            inspection session finishes
     */
    private TeleTargetBreakpoint(TeleVM teleVM, Factory factory, Address address, byte[] originalCode, boolean isTransient) {
        super(teleVM, isTransient);
        this.factory = factory;
        this.teleCodeLocation = new TeleCodeLocation(teleVM(), address);
        this.originalCode = originalCode == null ? teleVM.dataAccess().readFully(address, factory.codeSize()) : originalCode;
    }

    /**
     * Gets the code at this breakpoint's address.
     *
     * @return the current contents at this breakpoint's address of length {@link Factory#codeSize()}
     */
    public byte[] readCode() {
        return teleVM().dataAccess().readFully(address(), originalCode.length);
    }

    /**
     * Determines if the target code is currently patched at this breakpoint's {@linkplain #address() address} with the
     * platform dependent instruction(s) implementing a breakpoint.
     */
    public boolean isActivated() {
        return activated;
    }

    /**
     * Patches the target code at this breakpoint's address with platform dependent instruction(s) implementing a breakpoint.
     */
    public void activate() {
        teleVM().dataAccess().writeBytes(address(), factory.code());
        activated = true;
    }

    /**
     * Patches the target code at this breakpoint's address with the original code that was compiled at that address.
     */
    public void deactivate() {
        teleVM().dataAccess().writeBytes(address(), originalCode);
        activated = false;
    }

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

    @Override
    public String toString() {
        return "Target breakpoint" + "{0x" + address().toHexString() + "} " + attributesToString();
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
         * @return all the {@linkplain TeleBreakpoint#isTransient() persistent} target code breakpoints that currently exist
         * in the {@link TeleVM}.  Modification safe against breakpoint removal.
         *
         * @param omitTransientBreakpoints specifies whether or not {@linkplain TeleBreakpoint#isTransient()}
         */
        public synchronized Iterable<TeleTargetBreakpoint> breakpoints(boolean omitTransientBreakpoints) {
            final AppendableSequence<TeleTargetBreakpoint> targetBreakpoints = new ArrayListSequence<TeleTargetBreakpoint>(breakpoints.values());
            if (!omitTransientBreakpoints) {
                for (TeleTargetBreakpoint transientBreakpoint : transientBreakpoints.values()) {
                    targetBreakpoints.append(transientBreakpoint);
                }
            }
            return targetBreakpoints;
        }

        /**
         * @param omitTransientBreakpoints specifies whether or not {@linkplain TeleBreakpoint#isTransient()}
         * @return the number of existing target breakpoints in the {@link TeleVM}.
         */
        public synchronized int size(boolean omitTransientBreakpoints) {
            int result = breakpoints.size();
            if (!omitTransientBreakpoints) {
                result += transientBreakpoints.size();
            }
            return result;
        }

        /**
         * Gets a target code breakpoint set at a specified address in the {@link TeleVM}, if it exists, null otherwise. If
         * there is both a {@linkplain TeleBreakpoint#isTransient() non-transient} and
         * {@linkplain TeleBreakpoint#isTransient() transient} breakpoint set at {@code address}, then the
         * non-transient breakpoint is returned.
         */
        public synchronized TeleTargetBreakpoint getBreakpointAt(Address address) {
            final TeleTargetBreakpoint breakpoint = breakpoints.get(address.toLong());
            if (breakpoint != null) {
                return breakpoint;
            }
            return transientBreakpoints.get(address.toLong());
        }

        /**
         * Gets the {@linkplain TeleBreakpoint#isTransient() transient} target code breakpoint set at a specified
         * address in the {@link TeleVM}, if it exists, null otherwise.
         */
        public synchronized TeleTargetBreakpoint getTransientBreakpointAt(Address address) {
            return transientBreakpoints.get(address.toLong());
        }

        /**
         * Gets the {@linkplain TeleBreakpoint#isTransient() non-transient} target code breakpoint set at a specified
         * address in the {@link TeleVM}., if it exists, null otherwise.
         */
        public synchronized TeleTargetBreakpoint getNonTransientBreakpointAt(Address address) {
            return breakpoints.get(address.toLong());
        }

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
        public synchronized TeleTargetBreakpoint createBreakpoint(Address address, byte[] originalCode, boolean isTransient) {
            TeleTargetBreakpoint breakpoint;
            breakpoint = new TeleTargetBreakpoint(teleVM, this, address, originalCode, isTransient);
            if (!isTransient) {
                final TeleTargetBreakpoint oldBreakpoint = breakpoints.put(address.toLong(), breakpoint);
                assert oldBreakpoint == null;
                announceStateChange();
            } else {
                final TeleTargetBreakpoint oldBreakpoint = transientBreakpoints.put(address.toLong(), breakpoint);
                assert oldBreakpoint == null;
            }
            return breakpoint;
        }

        /**
         * Gets the breakpoint at a specified target code address in the tele VM, creating a new one first if needed.
         *
         * @param address the address at which the breakpoint is to be created
         * @param isTransient specifies if the created breakpoint is to be deleted when a process execution stops or an
         *            inspection session finishes
         */
        public synchronized TeleTargetBreakpoint makeBreakpoint(Address address, boolean isTransient) {
            final TeleTargetBreakpoint breakpoint = getBreakpointAt(address);
            if (breakpoint == null || breakpoint.isTransient() != isTransient) {
                return createBreakpoint(address, null, isTransient);
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
                TeleTargetBreakpoint breakpoint = getBreakpointAt(breakpointAddress);
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
        private synchronized void removeBreakpointAt(Address address) {
            breakpoints.remove(address.toLong());
            announceStateChange();
        }

        /**
         * {@linkplain TeleTargetBreakpoint#activate() Activates} all breakpoints (including transient breakpoints).
         */
        public synchronized void activateAll() {
            for (TeleTargetBreakpoint breakpoint : breakpoints.values()) {
                if (breakpoint.enabled) {
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
                if (breakpoint.enabled) {
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
