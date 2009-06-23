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
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.jit.amd64.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Breakpoints at the beginning of bytecode instructions.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class TeleBytecodeBreakpoint extends TeleBreakpoint {

    private TeleCodeLocation teleCodeLocation;
    private final Factory factory;

    @Override
    public TeleCodeLocation teleCodeLocation() {
        return teleCodeLocation;
    }

    /**
     * target breakpoints that were created in compilations of the method in the {@link TeleVM}.
     */
    private AppendableSequence<TeleTargetBreakpoint> teleTargetBreakpoints;

    private TeleBytecodeBreakpoint(TeleVM teleVM, Factory factory, Key key, boolean isTransient) {
        super(teleVM, isTransient);
        this.teleCodeLocation = new TeleCodeLocation(teleVM, key);
        this.factory = factory;
    }

    /**
     * @return description of the bytecode location of this breakpoint.
     */
    public Key key() {
        return teleCodeLocation.key();
    }

    private void request() {
        teleVM().messenger().requestBytecodeBreakpoint(key(), key().bytecodePosition);
    }

    private Deoptimizer deoptimizer;

    private Deoptimizer makeDeoptimizer() {
        if (deoptimizer == null) {
            switch (teleVM().vmConfiguration().platform().processorKind().instructionSet()) {
                case AMD64:
                    deoptimizer = AMD64Deoptimizer.deoptimizer();
                    break;
                default:
                    FatalError.unimplemented();
            }
        }
        return deoptimizer;
    }

    private void triggerDeoptimization(TeleTargetMethod teleTargetMethod) {
        final int[] stopPositions = teleTargetMethod.getStopPositions();
        int i;
        for (i = 0; i < teleTargetMethod.getNumberOfDirectCalls(); i++) {
            final Pointer callSite = teleTargetMethod.getCodeStart().plus(stopPositions[i]);
            final Pointer patchAddress = callSite.plus(makeDeoptimizer().directCallSize());
            teleVM().dataAccess().writeBytes(patchAddress, makeDeoptimizer().illegalInstruction());
        }
        for (; i < teleTargetMethod.getNumberOfIndirectCalls(); i++) {
            final Pointer callSite = teleTargetMethod.getCodeStart().plus(stopPositions[i]);
            final byte firstInstructionByte = teleVM().dataAccess().readByte(callSite);
            final Pointer patchAddress = callSite.plus(makeDeoptimizer().indirectCallSize(firstInstructionByte));
            teleVM().dataAccess().writeBytes(patchAddress, makeDeoptimizer().illegalInstruction());
        }
        for (; i < teleTargetMethod.getNumberOfSafepoints(); i++) {
            final Pointer safepoint = teleTargetMethod.getCodeStart().plus(stopPositions[i]);
            teleVM().dataAccess().writeBytes(safepoint, makeDeoptimizer().illegalInstruction());
        }
    }

    /**
     * Makes this breakpoint active in the {@link TeleVM} by locating all compilations and setting
     * target code breakpoints at the corresponding location, if that can be determined.
     */
    public void activate() {
        final Sequence<TeleTargetMethod> teleTargetMethods = TeleTargetMethod.get(teleVM(), key());
        if (teleTargetMethods.length() > 0) {
            teleTargetBreakpoints = new LinkSequence<TeleTargetBreakpoint>();
            for (TeleTargetMethod teleTargetMethod : teleTargetMethods) {
                if (teleTargetMethod instanceof TeleJitTargetMethod) {
                    final TeleJitTargetMethod teleJitTargetMethod = (TeleJitTargetMethod) teleTargetMethod;
                    final int[] bytecodeToTargetCodePositionMap = teleJitTargetMethod.bytecodeToTargetCodePositionMap();
                    final int targetCodePosition = bytecodeToTargetCodePositionMap[key().bytecodePosition];
                    final Address targetAddress = teleTargetMethod.getCodeStart().plus(targetCodePosition);
                    final TeleTargetBreakpoint teleTargetBreakpoint = teleVM().makeTargetBreakpoint(targetAddress);
                    teleTargetBreakpoint.setEnabled(true);
                    teleTargetBreakpoints.append(teleTargetBreakpoint);
                } else {
                    triggerDeoptimization(teleTargetMethod);
                }
            }
        }
        request();
    }

    /**
     * Removes any state, including in the {@link TeleVM}, associated with this breakpoint.
     */
    private void dispose() {
        final Sequence<TeleTargetMethod> teleTargetMethods = TeleTargetMethod.get(teleVM(), key());
        if (teleTargetMethods.length() > 0) {
            for (TeleTargetMethod teleTargetMethod : teleTargetMethods) {
                if (teleTargetMethod instanceof TeleJitTargetMethod) {
                    final TeleJitTargetMethod teleJitTargetMethod = (TeleJitTargetMethod) teleTargetMethod;
                    final int[] bytecodeToTargetCodePositionMap = teleJitTargetMethod.bytecodeToTargetCodePositionMap();
                    final int targetCodePosition = bytecodeToTargetCodePositionMap[key().bytecodePosition];
                    final Address targetAddress = teleTargetMethod.getCodeStart().plus(targetCodePosition);
                    final TeleTargetBreakpoint targetBreakpoint = teleVM().getTargetBreakpoint(targetAddress);
                    if (targetBreakpoint != null) {
                        targetBreakpoint.remove();
                    }
                    // Assume for now the whole VM is stopped; there will be races to be fixed otherwise, likely with an agent thread in the {@link TeleVM}.
                }
            }
            teleTargetBreakpoints = null;
        }
        teleVM().messenger().cancelBytecodeBreakpoint(key(), key().bytecodePosition);
        // Note that just sending a message to cancel the breakpoint request in the VM doesn't actually remove any
        // breakpoints generated by the VM in response to the original request.  Those will eventually be discovered
        // and removed remotely.
    }

    @Override
    public void remove() {
        dispose();
        factory.removeBreakpoint(key());
    }

    private boolean enabled;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        assert !isTransient() : "cannot disable transient breakpoint: " + this;
        if (enabled != this.enabled) {
            this.enabled = enabled;
            if (enabled) {
                activate();
            }
            factory.announceStateChange();
            // TODO (mlvdv) disable bytecode breakpoint
            return true;
        }
        return false;
    }

    @Override
    public BreakpointCondition condition() {
        // Conditional bytecode breakpoints not supported yet
        return null;
    }

    @Override
    public String toString() {
        return "Bytecode breakpoint" + key() + " " + attributesToString();
    }

    /**
     * Describes a bytecode position in the {@link TeleVM},
     * i.e. indicates the exact method and byte code position.
     *
     * The method does not have to be compiled, nor even loaded yet.
     */
    public static class Key extends DefaultMethodKey {

        protected final int bytecodePosition;

        /**
         * @return bytecode position in the method.
         */
        public int position() {
            return bytecodePosition;
        }

        public Key(MethodKey methodKey) {
            this(methodKey, 0);
        }

        public Key(BytecodeLocation bytecodeLocation) {
            super(bytecodeLocation.classMethodActor());
            this.bytecodePosition = bytecodeLocation.bytecodePosition();
        }

        public Key(MethodKey methodKey, int bytecodePosition) {
            super(methodKey.holder(), methodKey.name(), methodKey.signature());
            this.bytecodePosition = bytecodePosition;
        }

        public Key(SignatureDescriptor signature, TypeDescriptor holder, Utf8Constant name, int bytecodePosition) {
            super(holder, name, signature);
            this.bytecodePosition = bytecodePosition;
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            if (obj instanceof Key) {
                final Key otherKey = (Key) obj;
                return bytecodePosition == otherKey.bytecodePosition;

            }
            return false;
        }

        @Override
        public int hashCode() {
            return super.hashCode() ^ bytecodePosition;
        }

        @Override
        public String toString() {
            return "{" + super.toString() + ", position=" + bytecodePosition + "}";
        }

    }

    /**
     * Creates, tracks, and removes bytecode breakpoints from the {@link TeleVM}.
     */
    public static class Factory extends Observable {

        private final TeleVM teleVM;

        public Factory(TeleVM teleVM) {
            this.teleVM = teleVM;
            teleVM.addVMStateObserver(new TeleVMStateObserver() {

                public void upate(MaxVMState maxVMState) {
                    if (maxVMState.processState() == ProcessState.TERMINATED) {
                        breakpoints.clear();
                        announceStateChange();
                    }
                }
            });
        }

        private final VariableMapping<Key, TeleBytecodeBreakpoint> breakpoints = HashMapping.createVariableEqualityMapping();

        /**
         * Notify all observers that there has been a state change concerning these breakpoints.
         */
        private void announceStateChange() {
            setChanged();
            notifyObservers();
        }

        /**
         * @return all bytecode breakpoints that currently exist in the {@link TeleVM}.
         * Modification safe against breakpoint removal.
         */
        public synchronized Iterable<TeleBytecodeBreakpoint> breakpoints() {
            final AppendableSequence<TeleBytecodeBreakpoint> breakpoints = new LinkSequence<TeleBytecodeBreakpoint>();
            for (TeleBytecodeBreakpoint teleBytecodeBreakpoint : this.breakpoints.values()) {
                breakpoints.append(teleBytecodeBreakpoint);
            }
            return breakpoints;
        }

        /**
         * @return the number of bytecode breakpoints that currently exist in the {@link TeleVM}.
         */
        public synchronized int size() {
            return breakpoints.length();
        }

        /**
         * @param key description of a bytecode position in a method
         * @return a breakpoint set at the position, null if none.
         */
        public synchronized TeleBytecodeBreakpoint getBreakpoint(Key key) {
            return breakpoints.get(key);
        }

        private TeleBytecodeBreakpoint createBreakpoint(Key key, boolean persistent) {
            final TeleBytecodeBreakpoint breakpoint = new TeleBytecodeBreakpoint(teleVM, this, key, false);
            breakpoints.put(key, breakpoint);
            announceStateChange();
            return breakpoint;
        }

        /**
         * @param key description of a bytecode position in a method
         * @param isTransient
         * @return a possibly new, enabled bytecode breakpoint
         */
        public synchronized TeleBytecodeBreakpoint makeBreakpoint(Key key, boolean isTransient) {
            TeleBytecodeBreakpoint breakpoint = getBreakpoint(key);
            if (breakpoint == null) {
                breakpoint = createBreakpoint(key, isTransient);
            }
            breakpoint.setEnabled(true);
            return breakpoint;
        }

        /**
         * Removes a breakpoint at the described position, if one exists.
         * @param key description of a bytecode position in a method
         */
        private synchronized void removeBreakpoint(Key key) {
            breakpoints.remove(key);
            announceStateChange();
        }

        /**
         * Removes all bytecode breakpoints.
         */
        public synchronized void removeAllBreakpoints() {
            for (TeleBytecodeBreakpoint teleBytecodeBreakpoint : breakpoints.values()) {
                teleBytecodeBreakpoint.dispose();
            }
            breakpoints.clear();
            announceStateChange();
        }
    }
}
