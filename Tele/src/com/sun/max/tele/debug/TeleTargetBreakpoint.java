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
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
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

    private final TeleProcess _teleProcess;

    private final TeleCodeLocation _teleCodeLocation;

    @Override
    public TeleCodeLocation teleCodeLocation() {
        return _teleCodeLocation;
    }

    public Address address() {
        return _teleCodeLocation.targetCodeInstructionAddresss();
    }

    private final byte[] _originalCode;

    public byte[] originalCode() {
        return _originalCode;
    }

    private boolean _enabled = true;

    private boolean _activated;

    @Override
    public boolean enabled() {
        return _enabled;
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        assert !isTransient() : "cannot disable a transient breakpoint: " + this;
        if (enabled != _enabled) {
            _enabled = enabled;
            return true;
        }
        return false;
    }

    private BreakpointCondition _condition;

    @Override
    public BreakpointCondition condition() {
        return _condition;
    }

    @Override
    public void remove() {
        teleProcess().targetBreakpointFactory().removeBreakpointAt(address());
    }

    /**
     * Creates a breakpoint for a given target code address.
     *
     * @param teleProcess the tele process context of the breakpoint
     * @param address the address at which the breakpoint is to be created
     * @param originalCode the target code at {@code address} that will be overwritten by the breakpoint
     *            instruction. If this value is null, then the code will be read from {@code address}.
     * @param isTransient specifies if the created breakpoint is to be deleted when a process execution stops or an
     *            inspection session finishes
     */
    private TeleTargetBreakpoint(TeleProcess teleProcess, Address address, byte[] originalCode, boolean isTransient) {
        super(teleProcess.teleVM(), isTransient);
        _teleProcess = teleProcess;
        _teleCodeLocation = new TeleCodeLocation(teleVM(), address);
        _originalCode = originalCode == null ? teleProcess.dataAccess().readFully(address, teleProcess.targetBreakpointFactory().codeSize()) : originalCode;
    }

    /**
     * Gets the code at this breakpoint's address.
     *
     * @return the current contents at this breakpoint's address of length {@link Factory#codeSize()}
     */
    public byte[] readCode() {
        return _teleProcess.dataAccess().readFully(address(), _originalCode.length);
    }

    /**
     * Determines if the target code is currently patched at this breakpoint's {@linkplain #address() address} with the
     * platform dependent instruction(s) implementing a breakpoint.
     */
    public boolean isActivated() {
        return _activated;
    }

    /**
     * Patches the target code at this breakpoint's address with platform dependent instruction(s) implementing a breakpoint.
     */
    public void activate() {
        _teleProcess.dataAccess().writeBytes(address(), _teleProcess.targetBreakpointFactory().code());
        _activated = true;
    }

    /**
     * Patches the target code at this breakpoint's address with the original code that was compiled at that address.
     */
    public void deactivate() {
        _teleProcess.dataAccess().writeBytes(address(), _originalCode);
        _activated = false;
    }

    public void setCondition(BreakpointCondition condition) {
        _condition = condition;
    }

    @Override
    public String toString() {
        return "Target breakpoint" + "{0x" + address().toHexString() + "} " + attributesToString();
    }

    public static class Factory extends TeleViewModel {

        private final TeleProcess _teleProcess;
        private final byte[] _code;

        /**
         * Gets the bytes encoding the platform dependent instruction(s) representing a breakpoint.
         */
        public byte[] code() {
            return _code.clone();
        }

        /**
         * Gets number of bytes that encode the platform dependent instruction(s) representing a breakpoint.
         */
        public int codeSize() {
            return _code.length;
        }

        public Factory(TeleProcess teleProcess) {
            _teleProcess = teleProcess;
            _code = TargetBreakpoint.createBreakpointCode(_teleProcess.platform().processorKind().instructionSet());
        }

        private final Map<Long, TeleTargetBreakpoint> _breakpoints = new HashMap<Long, TeleTargetBreakpoint>();

        private final Map<Long, TeleTargetBreakpoint> _transientBreakpoints = new HashMap<Long, TeleTargetBreakpoint>();

        /**
         * Gets all the {@linkplain TeleBreakpoint#isTransient() persistent} target code breakpoints that currently exist
         * in the tele VM.
         *
         * @param omitTransientBreakpoints specifies whether or not {@linkplain TeleBreakpoint#isTransient()}
         */
        public synchronized IterableWithLength<TeleTargetBreakpoint> breakpoints(boolean omitTransientBreakpoints) {
            if (omitTransientBreakpoints) {
                return Iterables.toIterableWithLength(_breakpoints.values());
            }
            return Iterables.join(Iterables.toIterableWithLength(_breakpoints.values()), Iterables.toIterableWithLength(_transientBreakpoints.values()));
        }
        
        /**
         * @param omitTransientBreakpoints specifies whether or not {@linkplain TeleBreakpoint#isTransient()}
         * @return the number of existing target breakpoints in he {@link TeleVM}.
         */
        public synchronized int size(boolean omitTransientBreakpoints) {
            int result = _breakpoints.size();
            if (!omitTransientBreakpoints) {
                result += _transientBreakpoints.size();       
            }
            return result;
        }

        /**
         * Gets a target code breakpoint set at a specified address in the tele VM, if it exists, null otherwise. If
         * there is both a {@linkplain TeleBreakpoint#isTransient() non-transient} and
         * {@linkplain TeleBreakpoint#isTransient() transient} breakpoint set at {@code address}, then the
         * non-transient breakpoint is returned.
         */
        public synchronized TeleTargetBreakpoint getBreakpointAt(Address address) {
            final TeleTargetBreakpoint breakpoint = _breakpoints.get(address.toLong());
            if (breakpoint != null) {
                return breakpoint;
            }
            return _transientBreakpoints.get(address.toLong());
        }

        /**
         * Gets the {@linkplain TeleBreakpoint#isTransient() transient} target code breakpoint set at a specified
         * address in the tele VM, if it exists, null otherwise.
         */
        public synchronized TeleTargetBreakpoint getTransientBreakpointAt(Address address) {
            return _transientBreakpoints.get(address.toLong());
        }

        /**
         * Gets the {@linkplain TeleBreakpoint#isTransient() non-transient} target code breakpoint set at a specified
         * address in the tele VM, if it exists, null otherwise.
         */
        public synchronized TeleTargetBreakpoint getNonTransientBreakpointAt(Address address) {
            return _breakpoints.get(address.toLong());
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
            final TeleTargetBreakpoint breakpoint = new TeleTargetBreakpoint(_teleProcess, address, originalCode, isTransient);
            if (!isTransient) {
                final TeleTargetBreakpoint oldBreakpoint = _breakpoints.put(address.toLong(), breakpoint);
                assert oldBreakpoint == null;
                refreshView();
            } else {
                final TeleTargetBreakpoint oldBreakpoint = _transientBreakpoints.put(address.toLong(), breakpoint);
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
            final Value result = _teleProcess.teleVM().methods().TargetBreakpoint_findOriginalCode.interpret(LongValue.from(instructionPointer.toLong()));
            final Reference reference = result.asReference();
            if (reference.isZero()) {
                return null;
            }
            return (byte[]) reference.toJava();
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
            final byte[] codeBuffer = _teleProcess.dataAccess().readFully(breakpointAddress, codeSize());
            if (Bytes.equals(codeBuffer, _code)) {
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
                } else if (!breakpoint.enabled()) {
                    Problem.unimplemented("found disabled tele breakpoint at same ip as VM breakpoint");
                }
            }
        }

        /**
         * Removes the breakpoint, if it exists, at specified target code address in the teleVM.
         */
        public synchronized void removeBreakpointAt(Address address) {
            _breakpoints.remove(address.toLong());
            refreshView();
        }

        /**
         * Removes all target code breakpoints in the teleVM.
         */
        public synchronized void removeAllBreakpoints() {
            _breakpoints.clear();
            refreshView();
        }

        /**
         * {@linkplain TeleTargetBreakpoint#activate() Activates} all breakpoints (including transient breakpoints).
         */
        public synchronized void activateAll() {
            for (TeleTargetBreakpoint breakpoint : _breakpoints.values()) {
                if (breakpoint._enabled) {
                    breakpoint.activate();
                }
            }
            for (TeleTargetBreakpoint breakpoint : _transientBreakpoints.values()) {
                breakpoint.activate();
            }
        }

        /**
         * Deactivates all {@linkplain #activateAll() activated} breakpoints and removes all transient breakpoints.
         */
        public synchronized Sequence<TeleTargetBreakpoint> deactivateAll() {
            final AppendableSequence<TeleTargetBreakpoint> deactivated = new ArrayListSequence<TeleTargetBreakpoint>();
            for (TeleTargetBreakpoint breakpoint : _breakpoints.values()) {
                if (breakpoint._enabled) {
                    breakpoint.deactivate();
                    deactivated.append(breakpoint);
                }
            }
            for (TeleTargetBreakpoint breakpoint : _transientBreakpoints.values()) {
                breakpoint.deactivate();
                deactivated.append(breakpoint);
            }
            _transientBreakpoints.clear();
            return deactivated;
        }
    }
}
