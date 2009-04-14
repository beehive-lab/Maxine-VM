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

import com.sun.max.collect.*;
import com.sun.max.program.*;
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
import com.sun.max.vm.type.*;

/**
 * Breakpoints at the beginning of bytecode instructions.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class TeleBytecodeBreakpoint extends TeleBreakpoint {

    private TeleCodeLocation _teleCodeLocation;
    private final Factory _factory;

    @Override
    public TeleCodeLocation teleCodeLocation() {
        return _teleCodeLocation;
    }

    /**
     * target breakpoints that were created in compilations of the method in the {@link TeleVM}.
     */
    private AppendableSequence<TeleTargetBreakpoint> _teleTargetBreakpoints;

    private TeleBytecodeBreakpoint(TeleVM teleVM, Factory factory, Key key, boolean isTransient) {
        super(teleVM, isTransient);
        _teleCodeLocation = new TeleCodeLocation(teleVM, key);
        _factory = factory;
    }

    /**
     * @return description of the bytecode location of this breakpoint.
     */
    public Key key() {
        return _teleCodeLocation.key();
    }

    private void request() {
        teleVM().messenger().requestBytecodeBreakpoint(key(), key()._bytecodePosition);
    }

    private Deoptimizer _deoptimizer;

    private Deoptimizer makeDeoptimizer() {
        if (_deoptimizer == null) {
            switch (teleVM().vmConfiguration().platform().processorKind().instructionSet()) {
                case AMD64:
                    _deoptimizer = AMD64Deoptimizer.deoptimizer();
                    break;
                default:
                    Problem.unimplemented();
            }
        }
        return _deoptimizer;
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
            _teleTargetBreakpoints = new LinkSequence<TeleTargetBreakpoint>();
            for (TeleTargetMethod teleTargetMethod : teleTargetMethods) {
                if (teleTargetMethod instanceof TeleJitTargetMethod) {
                    final TeleJitTargetMethod teleJitTargetMethod = (TeleJitTargetMethod) teleTargetMethod;
                    final int[] bytecodeToTargetCodePositionMap = teleJitTargetMethod.bytecodeToTargetCodePositionMap();
                    final int targetCodePosition = bytecodeToTargetCodePositionMap[key()._bytecodePosition];
                    final Address targetAddress = teleTargetMethod.getCodeStart().plus(targetCodePosition);
                    final TeleTargetBreakpoint teleTargetBreakpoint = teleVM().makeTargetBreakpoint(targetAddress);
                    teleTargetBreakpoint.setEnabled(true);
                    _teleTargetBreakpoints.append(teleTargetBreakpoint);
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
                    final int targetCodePosition = bytecodeToTargetCodePositionMap[key()._bytecodePosition];
                    final Address targetAddress = teleTargetMethod.getCodeStart().plus(targetCodePosition);
                    teleVM().removeTargetBreakpoint(targetAddress);
                    // Assume for now the whole VM is stopped; there will be races to be fixed otherwise, likely with an agent thread in the {@link TeleVM}.
                }
            }
            _teleTargetBreakpoints = null;
        }
        teleVM().messenger().cancelBytecodeBreakpoint(key(), key()._bytecodePosition);
        // Note that just sending a message to cancel the breakpoint request in the VM doesn't actually remove any
        // breakpoints generated by the VM in response to the original request.  Those will eventually be discovered
        // and removed remotely.
    }

    @Override
    public void remove() {
        dispose();
        _factory.removeBreakpoint(key());
    }

    private boolean _enabled;

    @Override
    public boolean isEnabled() {
        return _enabled;
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        assert !isTransient() : "cannot disable transient breakpoint: " + this;
        if (enabled != _enabled) {
            _enabled = enabled;
            if (enabled) {
                activate();
            }
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

        protected final int _bytecodePosition;

        /**
         * @return bytecode position in the method.
         */
        public int position() {
            return _bytecodePosition;
        }

        public Key(MethodKey methodKey) {
            this(methodKey, 0);
        }

        public Key(BytecodeLocation bytecodeLocation) {
            super(bytecodeLocation.classMethodActor());
            _bytecodePosition = bytecodeLocation.bytecodePosition();
        }

        public Key(MethodKey methodKey, int bytecodePosition) {
            super(methodKey.holder(), methodKey.name(), methodKey.signature());
            _bytecodePosition = bytecodePosition;
        }

        public Key(SignatureDescriptor signature, TypeDescriptor holder, Utf8Constant name, int bytecodePosition) {
            super(holder, name, signature);
            _bytecodePosition = bytecodePosition;
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            if (obj instanceof Key) {
                final Key otherKey = (Key) obj;
                return _bytecodePosition == otherKey._bytecodePosition;

            }
            return false;
        }

        @Override
        public int hashCode() {
            return super.hashCode() ^ _bytecodePosition;
        }

        @Override
        public String toString() {
            return "{" + super.toString() + ", position=" + _bytecodePosition + "}";
        }

    }

    /**
     * Creates, tracks, and removes bytecode breakpoints from the {@link TeleVM}.
     */
    public static class Factory extends TeleViewModel {

        private final TeleVM _teleVM;

        public Factory(TeleVM teleVM) {
            _teleVM = teleVM;
        }

        private final VariableMapping<Key, TeleBytecodeBreakpoint> _breakpoints = HashMapping.createVariableEqualityMapping();

        /**
         * @return all bytecode breakpoints that currently exist in the {@link TeleVM}.
         */
        public synchronized Sequence<TeleBytecodeBreakpoint> breakpoints() {
            final AppendableSequence<TeleBytecodeBreakpoint> breakpoints = new LinkSequence<TeleBytecodeBreakpoint>();
            for (TeleBytecodeBreakpoint teleBytecodeBreakpoint : _breakpoints.values()) {
                breakpoints.append(teleBytecodeBreakpoint);
            }
            return breakpoints;
        }

        /**
         * @return the number of bytecode breakpoints that currently exist in the {@link TeleVM}.
         */
        public synchronized int size() {
            return _breakpoints.length();
        }

        /**
         * @param key description of a bytecode position in a method
         * @return a breakpoint set at the position, null if none.
         */
        public synchronized TeleBytecodeBreakpoint getBreakpoint(Key key) {
            return _breakpoints.get(key);
        }

        private TeleBytecodeBreakpoint createBreakpoint(Key key, boolean persistent) {
            final TeleBytecodeBreakpoint breakpoint = new TeleBytecodeBreakpoint(_teleVM, this, key, false);
            _breakpoints.put(key, breakpoint);
            refreshView(_teleVM.epoch());
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
            _breakpoints.remove(key);
            refreshView(_teleVM.epoch());
        }

        /**
         * Removes all bytecode breakpoints.
         */
        public synchronized void removeAllBreakpoints() {
            for (TeleBytecodeBreakpoint teleBytecodeBreakpoint : _breakpoints.values()) {
                teleBytecodeBreakpoint.dispose();
            }
            _breakpoints.clear();
            refreshView(_teleVM.epoch());
        }
    }
}
