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
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.tele.*;

/**
 * A breakpoint located at the beginning of a bytecode instruction
 * in a method in the VM.
 * <br>
 * When enabled, a bytecode breakpoint creates a machine code
 * breakpoint in each compilation of the specified method.   This
 * is true for compilations that exist when the breakpoint is created,
 * as well as all subsequent compilations.  When
 * disabled, all related target code breakpoints are removed.
 * <br>
 * Conditions are supported; they are set in each target code
 * breakpoint created for this breakpoint.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class TeleBytecodeBreakpoint extends TeleBreakpoint {

    private static final int TRACE_VALUE = 1;

    // Traces each compilation completed in the VM
    private static final int COMPILATION_TRACE_VALUE = 2;

    private final BytecodeBreakpointManager bytecodeBreakpointManager;

    // Cached string representations of the three parts of a method key
    // for fast comparison when comparing with a method key in the VM.
    private final String holderTypeDescriptorString;
    private final String methodName;
    private final String signatureDescriptorString;

    private boolean enabled = true;

    // Breakpoints are unconditional by default.
    private BreakpointCondition condition = null;

    // Private key used by the manager.
    private MethodPositionKey methodPositionKey;

    /**
     * All target code breakpoints created in compilations of the method in the VM.
     * Non-null iff this breakpoint is enabled; null if disabled.
     */
    private AppendableSequence<TeleTargetBreakpoint> teleTargetBreakpoints = new LinkSequence<TeleTargetBreakpoint>();

    /**
     * A new bytecode breakpoint, enabled by default, at a specified location.
     *
     * @param teleVM the VM
     * @param bytecodeBreakpointManager the associated bytecode breakpoint manager
     * @param key an abstract description of the location for this breakpoint, expressed in terms of the method and bytecode offset.
     * @param kind the kind of breakpoint to create
     */
    private TeleBytecodeBreakpoint(TeleVM teleVM, BytecodeBreakpointManager bytecodeBreakpointManager, CodeLocation codeLocation, BreakpointKind kind, MethodPositionKey methodPositionKey) {
        super(teleVM, codeLocation, kind, null);
        this.bytecodeBreakpointManager = bytecodeBreakpointManager;
        this.methodPositionKey = methodPositionKey;
        final MethodKey methodKey = codeLocation.methodKey();
        this.holderTypeDescriptorString = methodKey.holder().string;
        this.methodName = methodKey.name().string;
        this.signatureDescriptorString = methodKey.signature().string;
        Trace.line(TRACE_VALUE, tracePrefix() + "new=" + this);
    }

    /**
     * Creates a target code breakpoint in a compilation of this method in the VM, at a location
     * corresponding to the bytecode location for which this breakpoint was created.
     *
     * @param teleTargetMethod a compilation in the VM of the method for which this breakpoint was created.
     * @throws MaxVMBusyException
     */
    private void createTargetBreakpointForMethod(TeleTargetMethod teleTargetMethod) throws MaxVMBusyException {
        assert enabled;
        // Delegate creation of the target breakpoints to the manager.
        final Sequence<TeleTargetBreakpoint> newBreakpoints = bytecodeBreakpointManager.createTeleTargetBreakpoints(this, teleTargetMethod);
        if (newBreakpoints.isEmpty()) {
            Trace.line(TRACE_VALUE, tracePrefix() + "failed to create teleTargetBreakpoint for " + this);
        } else {
            // TODO (mlvdv) If we support conditions, need to combine it with the trigger handler added by factory method.
            for (TeleTargetBreakpoint newBreakpoint : newBreakpoints) {
                teleTargetBreakpoints.append(newBreakpoint);
                Trace.line(TRACE_VALUE, tracePrefix() + "created " + newBreakpoint + " for " + this);
            }
        }
    }

    /**
     * Handle notification that the method for which this breakpoint was created has just been compiled, possibly
     * but not necessarily the first of several compilations.
     *
     * @param teleTargetMethod a just completed compilation in the VM of the method for which this breakpoint was created.
     * @throws MaxVMBusyException
     */
    private void handleNewCompilation(TeleTargetMethod teleTargetMethod) throws MaxVMBusyException {
        if (enabled) {
            createTargetBreakpointForMethod(teleTargetMethod);
        }
    }

    public boolean isBytecodeBreakpoint() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) throws MaxVMBusyException {
        assert this.enabled != enabled;
        if (!teleVM().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            this.enabled = enabled;
            if (enabled) {
                assert teleTargetBreakpoints == null;
                // Create a target code breakpoint in every existing compilation at the location
                // best corresponding to the bytecode location of this breakpoint.
                teleTargetBreakpoints = new LinkSequence<TeleTargetBreakpoint>();
                for (TeleTargetMethod teleTargetMethod : TeleTargetMethod.get(teleVM(), codeLocation().methodKey())) {
                    createTargetBreakpointForMethod(teleTargetMethod);
                }
            } else {
                assert teleTargetBreakpoints != null;
                // Remove all target code breakpoints that were created because of this breakpoint
                for (TeleTargetBreakpoint teleTargetBreakpoint : teleTargetBreakpoints) {
                    teleTargetBreakpoint.remove();
                }
                teleTargetBreakpoints = null;
                Trace.line(TRACE_VALUE, tracePrefix() + "clearing all target breakpoints for " + this);
            }
            if (kind() == BreakpointKind.CLIENT) {
                bytecodeBreakpointManager.fireBreakpointsChanged();
            }
        } finally {
            teleVM().unlock();
        }
    }

    @Override
    public BreakpointCondition getCondition() {
        return condition;
    }

    @Override
    public void setCondition(String conditionDescriptor) throws ExpressionException, MaxVMBusyException {
        if (!teleVM().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            this.condition = new BreakpointCondition(teleVM(), conditionDescriptor);
            for (TeleTargetBreakpoint teleTargetBreakpoint : teleTargetBreakpoints) {
                teleTargetBreakpoint.setTriggerEventHandler(condition);
            }
        } finally {
            teleVM().unlock();
        }
    }

    @Override
    public void remove() throws MaxVMBusyException {
        if (!teleVM().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            Trace.line(TRACE_VALUE, tracePrefix() + "removing breakpoint=" + this);
            if (enabled) {
                setEnabled(false);
            }
            bytecodeBreakpointManager.removeBreakpoint(this);
        } finally {
            teleVM().unlock();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Bytecodes breakpoint");
        sb.append("{");
        sb.append(kind().toString()).append(", ");
        sb.append(codeLocation().methodKey().toString()).append(", ");
        sb.append(isEnabled() ? "enabled " : "disabled ");
        if (getDescription() != null) {
            sb.append(", \"").append(getDescription()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }


    /**
     * A key for recording abstract bytecode instruction location in a method;
     * defines equality to be same method descriptor, same offset.
     *
     * @author Michael Van De Vanter
     */
    private static final class MethodPositionKey extends DefaultMethodKey {

        /**
         * Create a key that uniquely identifies a method and bytecode position.
         * Equality defined in terms of equivalence of the method key and position.
         *
         * @param codeLocation a code location that must have a method key defined.
         * @return a new key
         */
        public static MethodPositionKey make(CodeLocation codeLocation) {
            assert codeLocation.hasMethodKey();
            return new MethodPositionKey(codeLocation);
        }

        protected final int bytecodePosition;

        private MethodPositionKey(CodeLocation codeLocation) {
            super(codeLocation.methodKey().holder(), codeLocation.methodKey().name(), codeLocation.methodKey().signature());
            this.bytecodePosition = codeLocation.bytecodePosition();
        }

        @Override
        public boolean equals(Object obj) {
            if (super.equals(obj) && obj instanceof MethodPositionKey) {
                final MethodPositionKey otherKey = (MethodPositionKey) obj;
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
            final StringBuilder sb = new StringBuilder();
            sb.append("MethodPositionKey{");
            sb.append(name()).append(signature().toJavaString(false, false));
            sb.append(", pos=").append(bytecodePosition);
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * A manager that creates, tracks, and removes bytecode breakpoints from the VM.
     * <br>
     * Bytecodes breakpoints can be created before a specified method is compiled
     * or even loaded.
     * <br>
     * A bytecode breakpoint causes a target code breakpoint to be created for every
     * compilation of the specified method, current and future.
     *
     * @author Michael Van De Vanter
     */
    public static final class BytecodeBreakpointManager extends AbstractTeleVMHolder {

        private static final Sequence<TeleBytecodeBreakpoint> EMPTY_BREAKPOINT_SEQUENCE = Sequence.Static.empty(TeleBytecodeBreakpoint.class);

        private final TeleTargetBreakpoint.TargetBreakpointManager teleTargetBreakpointManager;
        private final String tracePrefix;

        // Platform-specific access to method invocation parameters in the VM.
        private final Symbol parameter0;
        private final Symbol parameter1;
        private final Symbol parameter2;
        private final Symbol parameter3;

        /**
         * Map:  method {@link MethodPositionKey} -> existing bytecode breakpoint (whether enabled or not).
         */
        private final VariableMapping<MethodPositionKey, TeleBytecodeBreakpoint> breakpoints = HashMapping.createVariableEqualityMapping();

        /**
         * A cache of the existing breakpoints for fast traversal without allocation.
         */
        private Sequence<TeleBytecodeBreakpoint> breakpointCache = EMPTY_BREAKPOINT_SEQUENCE;

        /**
         * A breakpoint that interrupts the compiler just as it finishes compiling a method.  Non-null and active
         * iff there are one or more bytecode breakpoints in existence.
         */
        private TeleTargetBreakpoint compilerTargetCodeBreakpoint = null;

        private List<MaxBreakpointListener> breakpointListeners = new CopyOnWriteArrayList<MaxBreakpointListener>();

        /**
         * The number of times that the list of classes in which breakpoints are set has
         * been written into the VM.
         *
         * @see InspectableCodeInfo
         */
        private int breakpointClassDescriptorsEpoch = 0;

        public BytecodeBreakpointManager(TeleVM teleVM) {
            super(teleVM);
            this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
            Trace.begin(TRACE_VALUE, tracePrefix + "initializing");
            final long startTimeMillis = System.currentTimeMillis();
            this.teleTargetBreakpointManager = teleVM.teleProcess().targetBreakpointManager();
            // Predefine parameter accessors for reading compilation details
            parameter0 = (Symbol) VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI.integerIncomingParameterRegisters.get(0);
            parameter1 = (Symbol) VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI.integerIncomingParameterRegisters.get(1);
            parameter2 = (Symbol) VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI.integerIncomingParameterRegisters.get(2);
            parameter3 = (Symbol) VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI.integerIncomingParameterRegisters.get(3);

            Trace.end(TRACE_VALUE, tracePrefix() + "initializing", startTimeMillis);
        }

        /**
         * Adds a listener for breakpoint changes.
         * <br>
         * Thread-safe
         *
         * @param listener a breakpoint listener
         */
        void addListener(MaxBreakpointListener listener) {
            assert listener != null;
            breakpointListeners.add(listener);
        }

        /**
         * Removes a listener for breakpoint changes.
         * <br>
         * Thread-safe
         *
         * @param listener a breakpoint listener
         */
        void removeListener(MaxBreakpointListener listener) {
            assert listener != null;
            breakpointListeners.remove(listener);
        }

        /**
         * @return all client bytecode breakpoints that currently exist in the VM.
         * Modification safe against breakpoint removal.
         */
        Sequence<TeleBytecodeBreakpoint> clientBreakpoints() {
            if (breakpointCache.isEmpty()) {
                return Sequence.Static.empty(TeleBytecodeBreakpoint.class);
            }
            final AppendableSequence<TeleBytecodeBreakpoint> clientBreakpoints = new ArrayListSequence<TeleBytecodeBreakpoint>();
            for (TeleBytecodeBreakpoint breakpoint : breakpointCache) {
                if (breakpoint.kind() == BreakpointKind.CLIENT) {
                    clientBreakpoints.append(breakpoint);
                }
            }
            return clientBreakpoints;
        }

        /**
         * @param methodCodeLocation description of a bytecode position in a method
         * @return a client breakpoint set at the position, null if no client breakpoint at the position
         */
        public TeleBytecodeBreakpoint findClientBreakpoint(BytecodeLocation methodCodeLocation) {
            final MethodPositionKey key = MethodPositionKey.make(methodCodeLocation);
            TeleBytecodeBreakpoint breakpoint = breakpoints.get(key);
            return (breakpoint.kind() == BreakpointKind.CLIENT) ? breakpoint : null;
        }

        /**
         * Returns a clientBreakpoint matching a method location described
         * abstractly, newly created if one does not already exist for the location.
         * Fails if there is a system breakpoint already at that location.
         * <br>
         * Thread-safe; synchronizes on VM lock
         *
         * @param codeLocation description of a bytecode position in a method
         * @return a possibly new, enabled bytecode breakpoint,
         * null if a system breakpoint is already at the location.
         * @throws MaxVMBusyException
         */
        public TeleBreakpoint makeClientBreakpoint(CodeLocation codeLocation) throws MaxVMBusyException {
            assert codeLocation.hasMethodKey();
            if (!teleVM().tryLock()) {
                throw new MaxVMBusyException();
            }
            TeleBytecodeBreakpoint breakpoint;
            try {
                final MethodPositionKey key = MethodPositionKey.make(codeLocation);
                breakpoint = breakpoints.get(key);
                if (breakpoint == null) {
                    breakpoint = createBreakpoint(codeLocation, key, BreakpointKind.CLIENT);
                    breakpoint.setDescription("Client-specified breakpoint");
                } else if (breakpoint.kind() != BreakpointKind.CLIENT) {
                    breakpoint = null;
                }
            } finally {
                teleVM().unlock();
            }
            return breakpoint;
        }

        /**
         * Returns a system breakpoint at the entry of a method location described
         * abstractly, newly created if one does not already exist for the location.
         * Fails if there is a client breakpoint already at that location.
         * <br>
         * Thread-safe; synchronizes on VM lock
         *
         * @param codeLocation description of a bytecode position in a method
         * @param handler handler to be invoked when breakpoint triggers
         * @return a possibly new, enabled bytecode breakpoint at method entry,
         * null if a client breakpoint is already at the location.
         * @throws MaxVMBusyException
         */
        public TeleBreakpoint makeSystemBreakpoint(CodeLocation codeLocation, VMTriggerEventHandler handler) throws MaxVMBusyException {
            assert codeLocation.hasMethodKey();
            if (!teleVM().tryLock()) {
                throw new MaxVMBusyException();
            }
            TeleBytecodeBreakpoint breakpoint;
            try {
                final MethodPositionKey key = MethodPositionKey.make(codeLocation);
                breakpoint = breakpoints.get(key);
                if (breakpoint == null) {
                    breakpoint = createBreakpoint(codeLocation, key, BreakpointKind.SYSTEM);
                    breakpoint.setTriggerEventHandler(handler);
                    breakpoint.setDescription(codeLocation.description());
                } else if (breakpoint.kind() != BreakpointKind.SYSTEM) {
                    ProgramWarning.message("Can't create system bytecode breakpoint - client breakpoint already exists: " + codeLocation);
                    breakpoint = null;
                }
            } finally {
                teleVM().unlock();
            }
            return breakpoint;
        }


        private void updateBreakpointCache() {
            if (breakpoints.length() == 0) {
                breakpointCache = EMPTY_BREAKPOINT_SEQUENCE;
            } else {
                breakpointCache = new ArrayListSequence<TeleBytecodeBreakpoint>(breakpoints.values());
            }
        }

        /**
         * @param codeLocation abstract description of a bytecode position in a method
         * @param kind he kind of breakpoint to be created
         * @return a new, enabled bytecode breakpoint
         * @throws MaxVMBusyException
         */
        private TeleBytecodeBreakpoint createBreakpoint(CodeLocation codeLocation, MethodPositionKey key, BreakpointKind kind) throws MaxVMBusyException {
            if (breakpoints.length() == 0) {
                createCompilerBreakpoint();
            }
            final TeleBytecodeBreakpoint breakpoint = new TeleBytecodeBreakpoint(teleVM(), this, codeLocation, kind, key);
            breakpoint.setDescription(codeLocation.description());
            breakpoints.put(key, breakpoint);
            updateBreakpointCache();
            Trace.line(TRACE_VALUE, tracePrefix + "new=" + breakpoint);
            if (kind == BreakpointKind.CLIENT) {
                fireBreakpointsChanged();
            }
            return breakpoint;
        }

        /**
         * Removes a breakpoint at the described position, if one exists.
         * <br>
         * Assumes that all state related to the breakpoint has already
         * been removed.
         *
         * @param teleBytecodeBreakpoint the breakpoint being removed.
         */
        private void removeBreakpoint(TeleBytecodeBreakpoint teleBytecodeBreakpoint) {
            final TeleBytecodeBreakpoint removedBreakpoint = breakpoints.remove(teleBytecodeBreakpoint.methodPositionKey);
            ProgramWarning.check(removedBreakpoint != null, "Failed to remove breakpoint" + teleBytecodeBreakpoint);
            if (breakpoints.length() == 0) {
                try {
                    removeCompilerBreakpoint();
                } catch (MaxVMBusyException maxVMBusyException) {
                    ProgramError.unexpected("Unable to remove compiler breakpont for " + teleBytecodeBreakpoint);
                }
            }
            updateBreakpointCache();
            Trace.line(TRACE_VALUE, tracePrefix + "removed " + teleBytecodeBreakpoint);
            if (teleBytecodeBreakpoint.kind() == BreakpointKind.CLIENT) {
                fireBreakpointsChanged();
            }
        }

        /**
         * Sets a target code breakpoint on a method known to be called at completion of each method
         * compilation in the VM.  Arguments identify the method just compiled.
         * <br>
         * The arguments are read using low-level, type-unsafe techniques.  The order and types
         * of arguments processed here must match those of the compiler method where the
         * breakpoint is set.
         * @throws MaxVMBusyException
         *
         * @see InspectableCodeInfo#compilationComplete(String, String, String, com.sun.max.vm.compiler.target.TargetMethod)
         */
        private void createCompilerBreakpoint() throws MaxVMBusyException {
            assert compilerTargetCodeBreakpoint == null;
            compilerTargetCodeBreakpoint = teleTargetBreakpointManager.makeSystemBreakpoint(teleVM().teleMethods().compilationComplete(), null);
            compilerTargetCodeBreakpoint.setDescription("System trap for VM compiler");
            compilerTargetCodeBreakpoint.setTriggerEventHandler(new VMTriggerEventHandler() {
                public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {

                    // The new compilation; don't bother to construct a representation of it unless there's a match and it's needed.
                    TeleTargetMethod teleTargetMethod = null;

                    final TeleIntegerRegisterSet integerRegisterSet = teleNativeThread.registers().integerRegisterSet();
                    final String holderTypeDescriptorString = teleVM().getStringUnsafe(teleVM().wordToTemporaryReference(integerRegisterSet.getValue(parameter0)));
                    final String methodName = teleVM().getStringUnsafe(teleVM().wordToTemporaryReference(integerRegisterSet.getValue(parameter1)));
                    final String signatureDescriptorString = teleVM().getStringUnsafe(teleVM().wordToTemporaryReference(integerRegisterSet.getValue(parameter2)));
                    Trace.line(COMPILATION_TRACE_VALUE, "VM just compiled: " + holderTypeDescriptorString + " " + methodName + " " + signatureDescriptorString);
                    for (TeleBytecodeBreakpoint teleBytecodeBreakpoint : breakpointCache) {
                        // Streamlined comparison using as little Inspector machinery as possible, since we take this break at every VM compilation
                        if (holderTypeDescriptorString.equals(teleBytecodeBreakpoint.holderTypeDescriptorString) &&
                                        methodName.equals(teleBytecodeBreakpoint.methodName) &&
                                        signatureDescriptorString.equals(teleBytecodeBreakpoint.signatureDescriptorString)) {
                            // Match; must set a target breakpoint on the method just compiled; is is acceptable to incur some overhead now.
                            if (teleTargetMethod == null) {
                                final Reference targetMethodReference = teleVM().wordToReference(integerRegisterSet.getValue(parameter3));
                                teleTargetMethod = (TeleTargetMethod) teleVM().makeTeleObject(targetMethodReference);
                            }
                            try {
                                teleBytecodeBreakpoint.handleNewCompilation(teleTargetMethod);
                            } catch (MaxVMBusyException maxVMBusyException) {
                                ProgramError.unexpected("Unable to create target breakpoint for new compilation of " + teleBytecodeBreakpoint);
                            }
                        }
                    }
                    // Handling done; now resume VM execution.
                    return false;
                }
            });
            Trace.line(TRACE_VALUE, tracePrefix + "creating compiler breakpoint=" + compilerTargetCodeBreakpoint);
        }

        /**
         * Removes the special target breakpoint set on a method called after each compilation in the VM.
         * Don't incur the overhead of a break if there are no bytecode breakpoints enabled.
         * @throws MaxVMBusyException
         */
        private void removeCompilerBreakpoint() throws MaxVMBusyException {
            assert compilerTargetCodeBreakpoint != null;
            Trace.line(TRACE_VALUE, tracePrefix + "removing compiler breakpoint=" + compilerTargetCodeBreakpoint);
            compilerTargetCodeBreakpoint.remove();
            compilerTargetCodeBreakpoint = null;
        }

        /**
         * Creates special system target code breakpoints in a compiled method in the VM
         * at location specified abstractly by a key.  Normally there is exactly one such location,
         * but in the special case where bytecodePosition is -1, which specifies the beginning
         * of the compiled method's prologue, there may be more than one for different kinds
         * of calls.
         * <br>
         * May fail when it is not possible to map the bytecode location into a target code location,
         * for example in optimized code where deoptimization is not supported.
         * <br>
         * Trigger events are delegated to the owning bytecode breakpoint.
         *
         * @param owner the breakpoint on whose behalf this breakpoint is being created.
         * @param teleTargetMethod a compilation in the VM of the method specified in the key
         * @return a target code breakpoint at a location in the compiled method corresponding
         * to the bytecode location specified in the key; null if unable to create.
         * @throws MaxVMBusyException
         */
        private Sequence<TeleTargetBreakpoint> createTeleTargetBreakpoints(final TeleBytecodeBreakpoint owner, TeleTargetMethod teleTargetMethod) throws MaxVMBusyException {
            assert owner != null;
            final AppendableSequence<TeleTargetBreakpoint> teleTargetBreakpoints = new LinkSequence<TeleTargetBreakpoint>();
            final int bytecodePosition = owner.methodPositionKey.bytecodePosition;
            Address address = Address.zero();
            if (teleTargetMethod instanceof TeleJitTargetMethod) {
                final TeleJitTargetMethod teleJitTargetMethod = (TeleJitTargetMethod) teleTargetMethod;
                final int targetCodePosition;
                if (bytecodePosition == -1) {
                    // Specifies the code start, at the beginning of the method prologue
                    TargetMethod targetMethod = teleTargetMethod.targetMethod();
                    targetCodePosition = AdapterGenerator.prologueSizeForCallee(targetMethod);
                } else {
                    final int[] bytecodeToTargetCodePositionMap = teleJitTargetMethod.bytecodeToTargetCodePositionMap();
                    targetCodePosition = bytecodeToTargetCodePositionMap[bytecodePosition];
                }
                address = teleTargetMethod.getCodeStart().plus(targetCodePosition);
                Trace.line(TRACE_VALUE, tracePrefix + "creating target breakpoint for offset " + targetCodePosition + " in " + teleTargetMethod);
            } else {
                if (bytecodePosition == 0) {
                    address = teleTargetMethod.callEntryPoint();
                    Trace.line(TRACE_VALUE, tracePrefix + "creating target breakpoint at method entry in " + teleTargetMethod);
                } else {
                    ProgramError.unexpected(tracePrefix + "Non-entry bytecode breakpoint unimplemented for target method=" + teleTargetMethod);
                }
            }
            if (teleTargetBreakpointManager.getTargetBreakpointAt(address) == null) {
                final CodeLocation location = CodeLocation.createMachineCodeLocation(teleVM(), address, "For bytecode breapoint=" + owner.codeLocation());
                final VMTriggerEventHandler vmTriggerEventHandler = new VMTriggerEventHandler() {

                    public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
                        return owner.handleTriggerEvent(teleNativeThread);
                    }
                };
                teleTargetBreakpoints.append(teleTargetBreakpointManager.makeSystemBreakpoint(location, vmTriggerEventHandler, owner));
            } else {
                Trace.line(TRACE_VALUE, tracePrefix + "Target breakpoint already exists at 0x" + address.toHexString() + " in " + teleTargetMethod);
            }
            return teleTargetBreakpoints;
        }

        private void fireBreakpointsChanged() {
            // Notify registered listeners
            for (final MaxBreakpointListener listener : breakpointListeners) {
                listener.breakpointsChanged();
            }
            // Notify the VM
            // Gather classes in which there is at least one bytecode breakpoint set.
            final Set<String> breakpointClassDescriptors = new HashSet<String>();
            for (TeleBytecodeBreakpoint breakpoint : breakpointCache) {
                breakpointClassDescriptors.add(breakpoint.holderTypeDescriptorString);
            }
            // Create string containing class descriptors for all classes in which breakpoints are set, each terminated by a space.
            final StringBuilder typeDescriptorsBuilder = new StringBuilder();
            for (String descriptor : breakpointClassDescriptors) {
                typeDescriptorsBuilder.append(descriptor).append(" ");
            }
            final String breakpointClassDescriptorsString = typeDescriptorsBuilder.toString();
            if (breakpointClassDescriptorsString.length() > InspectableCodeInfo.BREAKPOINT_DESCRIPTORS_ARRAY_LENGTH) {
                final StringBuilder errMsg = new StringBuilder();
                errMsg.append("Implementation Restriction exceeded: list of type descriptors for classes containing ");
                errMsg.append("bytecode breakpoints must not exceed ");
                errMsg.append(InspectableCodeInfo.BREAKPOINT_DESCRIPTORS_ARRAY_LENGTH).append(" characters.  ");
                errMsg.append("Current length=").append(breakpointClassDescriptorsString.length()).append(" characters.");
                ProgramError.unexpected(errMsg.toString());
            }
            Trace.line(TRACE_VALUE, tracePrefix + "Writing to VM type descriptors for breakpoint classes =\"" + breakpointClassDescriptorsString + "\"");
            // Write the string into the designated region in the VM, along with length and incremented epoch counter
            final int charsLength = breakpointClassDescriptorsString.length();
            final Reference charArrayReference = teleVM().teleFields().InspectableCodeInfo_breakpointClassDescriptorCharArray.readReference(teleVM());
            ProgramError.check(charArrayReference != null && !charArrayReference.isZero(), "Can't locate inspectable code array for breakpoint classes");
            final CharArrayLayout charArrayLayout = teleVM().layoutScheme().charArrayLayout;
            for (int index = 0; index < charsLength; index++) {
                charArrayLayout.setChar(charArrayReference, index, breakpointClassDescriptorsString.charAt(index));
            }
            teleVM().teleFields().InspectableCodeInfo_breakpointClassDescriptorsCharCount.writeInt(teleVM(), charsLength);
            teleVM().teleFields().InspectableCodeInfo_breakpointClassDescriptorsEpoch.writeInt(teleVM(), ++breakpointClassDescriptorsEpoch);
        }

        /**
         * Writes a description of every bytecode breakpoint to the stream, including those usually not shown to clients,
         * with more detail than typically displayed.
         * <br>
         * Thread-safe
         *
         * @param printStream
         */
        void writeSummaryToStream(PrintStream printStream) {
            printStream.println("Bytecodes breakpoints :");
            for (TeleBytecodeBreakpoint bytecodeBreakpoint : breakpointCache) {
                printStream.println("  " + bytecodeBreakpoint);
            }
        }

    }
}
