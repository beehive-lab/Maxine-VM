/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.cri.ci.CiCallingConvention.Type.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiRegister.RegisterFlag;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.BreakpointCondition.ExpressionException;
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.BytecodeLocation;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.DefaultMethodKey;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.tele.*;

/**
 * A breakpoint located at the beginning of a bytecode instruction
 * in a method in the VM.
 * <p>
 * When enabled, a bytecode breakpoint creates a machine code
 * breakpoint in each compilation of the specified method.   This
 * is true for compilations that exist when the breakpoint is created,
 * as well as all subsequent compilations.  When
 * disabled, all related target code breakpoints are removed.
 * <p>
 * Conditions are supported; they are set in each target code
 * breakpoint created for this breakpoint.
 */
public final class VmBytecodeBreakpoint extends VmBreakpoint {

    private static final int TRACE_VALUE = 1;

    // Traces each compilation completed in the VM
    private static final int COMPILATION_TRACE_VALUE = 1;


    private static BytecodeBreakpointManager bytecodeBreakpointManager;

    public static BytecodeBreakpointManager makeManager(TeleVM vm) {
        if (bytecodeBreakpointManager == null) {
            bytecodeBreakpointManager = new BytecodeBreakpointManager(vm);
        }
        return bytecodeBreakpointManager;
    }

    // Cached string representations of the three parts of a method key
    // for fast comparison when comparing with a method key in the VM.
    private final String holderTypeDescriptorString;
    private final String methodName;
    private final String signatureDescriptorString;

    private boolean enabled = false;

    // Breakpoint is unconditional by default.
    private BreakpointCondition condition = null;

    // Private key used by the manager.
    private MethodPositionKey methodPositionKey;

    /**
     * All machine code breakpoints created in compilations of the method in the VM.
     * Non-null if this breakpoint is enabled; null if disabled.
     */
    private List<VmTargetBreakpoint> targetBreakpoints = null;

    /**
     * A new bytecode breakpoint, enabled by default, at a specified location.
     *
     * @param vm the VM
     * @param kind the kind of breakpoint to create
     * @param key an abstract description of the location for this breakpoint, expressed in terms of the method and bytecode offset.
     */
    private VmBytecodeBreakpoint(TeleVM vm, CodeLocation codeLocation, BreakpointKind kind, MethodPositionKey methodPositionKey) {
        super(vm, codeLocation, kind);
        this.methodPositionKey = methodPositionKey;
        final MethodKey methodKey = codeLocation.methodKey();
        this.holderTypeDescriptorString = methodKey.holder().string;
        this.methodName = methodKey.name().string;
        this.signatureDescriptorString = methodKey.signature().string;
        Trace.line(TRACE_VALUE, tracePrefix() + "new=" + this);
    }

    /**
     * Creates a machine code breakpoint in a specific compilation of this method in the VM, at a location
     * corresponding to the bytecode location for which this breakpoint was created.  Note that in some
     * cases there may be more than one.
     *
     * @param teleTargetMethod a compilation in the VM of the method for which this breakpoint was created.
     * @throws MaxVMBusyException
     */
    private void createTargetBreakpointForMethod(TeleTargetMethod teleTargetMethod) throws MaxVMBusyException {
        assert enabled;
        // Delegate creation of the target breakpoints to the manager.
        final List<VmTargetBreakpoint> newTargetBreakpoints = bytecodeBreakpointManager.createTargetBreakpoints(this, teleTargetMethod);
        if (newTargetBreakpoints.isEmpty()) {
            // This will always return true in the current implementation of method entry breakpoints, because only
            // transient breakpoints are created.  They go away immediately after one execution cycle.
            //TeleWarning.message(tracePrefix() + "failed to create targetBreakpoint for " + this);
        } else {
            // TODO (mlvdv) If we support conditions, need to combine it with the trigger handler added by factory method.
            for (VmTargetBreakpoint newTargetBreakpoint : newTargetBreakpoints) {
                targetBreakpoints.add(newTargetBreakpoint);
                Trace.line(TRACE_VALUE, tracePrefix() + "created " + newTargetBreakpoint + " for " + this);
            }
        }
    }

    /**
     * Handle notification that the method for which this breakpoint was created has just been compiled, possibly
     * but not necessarily the first of more than one.
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
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            this.enabled = enabled;
            if (enabled) {
                assert targetBreakpoints == null;
                // Create a machine code breakpoint in every existing compilation at the location
                // best corresponding to the bytecode location of this breakpoint.
                targetBreakpoints = new ArrayList<VmTargetBreakpoint>();
                for (TeleTargetMethod teleTargetMethod : vm().machineCode().findCompilations(codeLocation().methodKey())) {
                    createTargetBreakpointForMethod(teleTargetMethod);
                }
            } else {
                assert targetBreakpoints != null;
                // Remove all target code breakpoints that were created because of this breakpoint
                for (VmTargetBreakpoint targetBreakpoint : targetBreakpoints) {
                    targetBreakpoint.remove();
                }
                targetBreakpoints = null;
                Trace.line(TRACE_VALUE, tracePrefix() + "clearing all target breakpoints for " + this);
            }
            if (kind() == BreakpointKind.CLIENT) {
                bytecodeBreakpointManager.fireBreakpointsChanged();
            }
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
            this.condition = new BreakpointCondition(vm(), conditionDescriptor);
            for (VmTargetBreakpoint targetBreakpoint : targetBreakpoints) {
                targetBreakpoint.setTriggerEventHandler(condition);
            }
        } finally {
            vm().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Bytecode breakpoints don't have an owner; they only own other (target) breakpoints.
     */
    public VmBreakpoint owner() {
        return null;
    }

    @Override
    public void remove() throws MaxVMBusyException {
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            Trace.line(TRACE_VALUE, tracePrefix() + "removing breakpoint=" + this);
            if (enabled) {
                // Be sure to clear any associated machine code breakpoints.
                setEnabled(false);
            }
            bytecodeBreakpointManager.removeBreakpoint(this);
        } finally {
            vm().unlock();
        }
    }

    /**
     * Receives notification that a machine code breakpoint, created in a compilation of the method
     * covered by this bytecode breakpoint, has been removed because the compilation was evicted
     * from the code cache.
     *
     * @param evictedSystemBreakpoint a target breakpoint that was created for the purpose of implementing this breakpoint in a particular compilation.
     */
    public void notifyCompilationEvicted(VmTargetBreakpoint evictedSystemBreakpoint) {
        Trace.line(TRACE_VALUE, tracePrefix() + " bytecode breakpoint removing target breakpoint due to code eviction;" + evictedSystemBreakpoint);
        if (!targetBreakpoints.remove(evictedSystemBreakpoint)) {
            // This will always return false under the current implementation of bytecode breakpoints at method entry, with bci=-1,
            // since the policy is to create only a transient target breakpoint, which will have disappeared by the time this
            // notification happens.
            TeleWarning.message(tracePrefix() + " failed to handle removal of target breakpoint because of code eviction, breakpoint=" + this);
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
     */
    private static final class MethodPositionKey extends DefaultMethodKey {

        /**
         * Creates a key that uniquely identifies a method and bytecode position.
         * Equality defined in terms of equivalence of the method key and position.
         *
         * @param codeLocation a code location that must have a method key defined.
         * @return a new key
         */
        public static MethodPositionKey make(CodeLocation codeLocation) {
            assert codeLocation.hasMethodKey();
            return new MethodPositionKey(codeLocation);
        }

        protected final int bci;

        private MethodPositionKey(CodeLocation codeLocation) {
            super(codeLocation.methodKey().holder(), codeLocation.methodKey().name(), codeLocation.methodKey().signature());
            this.bci = codeLocation.bci();
        }

        @Override
        public boolean equals(Object obj) {
            if (super.equals(obj) && obj instanceof MethodPositionKey) {
                final MethodPositionKey otherKey = (MethodPositionKey) obj;
                return bci == otherKey.bci;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return super.hashCode() ^ bci;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("MethodPositionKey{");
            sb.append(name()).append(signature().toJavaString(false, false));
            sb.append(", bci=").append(bci);
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * A singleton manager that creates, tracks, and removes bytecode breakpoints from the VM.
     * <p>
     * Bytecodes breakpoints can be created before a specified method is compiled
     * or even loaded, in which case they are described by an abstract key (descriptor).
     * <p>
     * A bytecode breakpoint causes a target code breakpoint to be created for every
     * compilation of the specified method, current and future.
     */
    public static final class BytecodeBreakpointManager extends AbstractVmHolder implements TeleVMCache {

        protected final class CompilationEventHandler implements VMTriggerEventHandler {
            final boolean preCompilationEvent;

            public CompilationEventHandler(boolean preCompilationEvent) {
                this.preCompilationEvent = preCompilationEvent;
            }

            public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {

                final TeleIntegerRegisters teleIntegerRegisters = teleNativeThread.registers().teleIntegerRegisters();
                final String holderTypeDescriptorString = vm().getStringUnsafe(teleIntegerRegisters.getValue(parameter0));
                final String methodName = vm().getStringUnsafe(teleIntegerRegisters.getValue(parameter1));
                final String signatureDescriptorString = vm().getStringUnsafe(teleIntegerRegisters.getValue(parameter2));
                if (Trace.hasLevel(COMPILATION_TRACE_VALUE)) {
                    String eventPrefix = preCompilationEvent ? "VM about to compile: " : "VM just compiled: ";
                    Trace.line(COMPILATION_TRACE_VALUE, eventPrefix + holderTypeDescriptorString + " " + methodName + " " + signatureDescriptorString);
                }

                for (VmBytecodeBreakpoint bytecodeBreakpoint : breakpointCache) {
                    // Streamlined comparison using as little Inspector machinery as possible, since we take this break at every VM compilation
                    if (holderTypeDescriptorString.equals(bytecodeBreakpoint.holderTypeDescriptorString) &&
                                    methodName.equals(bytecodeBreakpoint.methodName) &&
                                    signatureDescriptorString.equals(bytecodeBreakpoint.signatureDescriptorString)) {
                        if (preCompilationEvent) {
                            return true;
                        }
                        // Match; must set a target breakpoint on the method just compiled; is is acceptable to incur some overhead now.
                        final RemoteReference targetMethodReference = referenceManager().makeReference(teleIntegerRegisters.getValue(parameter3));
                        if (targetMethodReference.isZero()) {
                            TeleWarning.message("targetMethod parameter to post-compilation trigger method was null");
                            continue;
                        }
                        TeleTargetMethod teleTargetMethod = (TeleTargetMethod) objects().makeTeleObject(targetMethodReference);
                        try {
                            bytecodeBreakpoint.handleNewCompilation(teleTargetMethod);
                        } catch (MaxVMBusyException maxVMBusyException) {
                            TeleError.unexpected("Unable to create target breakpoint for new compilation of " + bytecodeBreakpoint);
                        }
                    }
                }
                // Handling done; now resume VM execution.
                return false;
            }
        }

        public static boolean usePrecompilationBreakpoints;

        private static final List<VmBytecodeBreakpoint> EMPTY_BREAKPOINT_SEQUENCE = Collections.emptyList();

        private final String tracePrefix;

        // Platform-specific access to method invocation parameters in the VM.
        private final CiRegister parameter0;
        private final CiRegister parameter1;
        private final CiRegister parameter2;
        private final CiRegister parameter3;

        /**
         * Map:  method {@link MethodPositionKey} -> existing bytecode breakpoint (whether enabled or not).
         */
        private final Map<MethodPositionKey, VmBytecodeBreakpoint> breakpoints = new HashMap<MethodPositionKey, VmBytecodeBreakpoint>();

        /**
         * A cache of the existing breakpoints for fast traversal without allocation.
         */
        private List<VmBytecodeBreakpoint> breakpointCache = EMPTY_BREAKPOINT_SEQUENCE;

        /**
         * A breakpoint that interrupts the compiler just as it starts compiling a method.  Non-null and active
         * iff there are one or more bytecode breakpoints in existence.
         */
        private VmTargetBreakpoint compilationStartedBreakpoint = null;

        /**
         * A breakpoint that interrupts the compiler just as it finishes compiling a method.  Non-null and active
         * iff there are one or more bytecode breakpoints in existence.
         */
        private VmTargetBreakpoint compilationCompletedBreakpoint = null;

        private List<MaxBreakpointListener> breakpointListeners = new CopyOnWriteArrayList<MaxBreakpointListener>();

        /**
         * The number of times that the list of classes in which breakpoints are set has
         * been written into the VM.
         *
         * @see InspectableCompilationInfo
         */
        private int breakpointClassDescriptorsEpoch = 0;

        private BytecodeBreakpointManager(TeleVM vm) {
            super(vm);
            this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
            Trace.begin(TRACE_VALUE, tracePrefix + "initializing");
            final long startTimeMillis = System.currentTimeMillis();
            // Predefine parameter accessors for reading compilation details
            CiRegister[] args = MaxineVM.vm().registerConfigs.standard.getCallingConventionRegisters(JavaCall, RegisterFlag.CPU);
            parameter0 = args[0];
            parameter1 = args[1];
            parameter2 = args[2];
            parameter3 = args[3];
            Trace.end(TRACE_VALUE, tracePrefix() + "initializing", startTimeMillis);
        }

        public void updateCache(long epoch) {
        }

        /**
         * Adds a listener for breakpoint changes.
         * <p>
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
         * <p>
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
        List<VmBytecodeBreakpoint> clientBreakpoints() {
            if (breakpointCache.isEmpty()) {
                return Collections.emptyList();
            }
            final List<VmBytecodeBreakpoint> clientBreakpoints = new ArrayList<VmBytecodeBreakpoint>();
            for (VmBytecodeBreakpoint breakpoint : breakpointCache) {
                if (breakpoint.kind() == BreakpointKind.CLIENT) {
                    clientBreakpoints.add(breakpoint);
                }
            }
            return clientBreakpoints;
        }

        /**
         * @param methodCodeLocation description of a bytecode position in a method
         * @return a client breakpoint set at the position, null if no client breakpoint at the position
         */
        public VmBytecodeBreakpoint findClientBreakpoint(BytecodeLocation methodCodeLocation) {
            final MethodPositionKey key = MethodPositionKey.make(methodCodeLocation);
            VmBytecodeBreakpoint breakpoint = breakpoints.get(key);
            return (breakpoint.kind() == BreakpointKind.CLIENT) ? breakpoint : null;
        }

        /**
         * Returns a clientBreakpoint matching a method location described
         * abstractly, newly created if one does not already exist for the location.
         * Fails if there is a system breakpoint already at that location.
         * <p>
         * Thread-safe; synchronizes on VM lock
         *
         * @param codeLocation description of a bytecode position in a method
         * @return a possibly new, enabled bytecode breakpoint,
         * null if a system breakpoint is already at the location.
         * @throws MaxVMBusyException
         */
        public VmBreakpoint makeClientBreakpoint(CodeLocation codeLocation) throws MaxVMBusyException {
            assert codeLocation.hasMethodKey();
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            VmBytecodeBreakpoint breakpoint;
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
                vm().unlock();
            }
            return breakpoint;
        }

        private void updateBreakpointCache() {
            if (breakpoints.size() == 0) {
                breakpointCache = EMPTY_BREAKPOINT_SEQUENCE;
            } else {
                breakpointCache = new ArrayList<VmBytecodeBreakpoint>(breakpoints.values());
            }
        }

        /**
         * @param codeLocation abstract description of a bytecode position in a method
         * @param kind he kind of breakpoint to be created
         * @return a new, enabled bytecode breakpoint
         * @throws MaxVMBusyException
         */
        private VmBytecodeBreakpoint createBreakpoint(CodeLocation codeLocation, MethodPositionKey key, BreakpointKind kind) throws MaxVMBusyException {
            if (breakpoints.size() == 0) {
                createCompilerBreakpoints();
            }
            final VmBytecodeBreakpoint breakpoint = new VmBytecodeBreakpoint(vm(), codeLocation, kind, key);
            breakpoint.setDescription(codeLocation.description());
            breakpoint.setEnabled(true);
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
         * <p>
         * Assumes that all state related to the breakpoint has already
         * been removed.
         *
         * @param bytecodeBreakpoint the breakpoint being removed.
         */
        private void removeBreakpoint(VmBytecodeBreakpoint bytecodeBreakpoint) {
            final VmBytecodeBreakpoint removedBreakpoint = breakpoints.remove(bytecodeBreakpoint.methodPositionKey);
            TeleWarning.check(removedBreakpoint != null, "Failed to remove breakpoint" + bytecodeBreakpoint);
            if (breakpoints.size() == 0) {
                try {
                    removeCompilerBreakpoints();
                } catch (MaxVMBusyException maxVMBusyException) {
                    TeleError.unexpected("Unable to remove compiler breakpont for " + bytecodeBreakpoint);
                }
            }
            updateBreakpointCache();
            Trace.line(TRACE_VALUE, tracePrefix + "removed " + bytecodeBreakpoint);
            if (bytecodeBreakpoint.kind() == BreakpointKind.CLIENT) {
                fireBreakpointsChanged();
            }
        }

        /**
         * Sets target code breakpoints on methods known to be called before and after of each method
         * compilation in the VM.  Arguments identify the method being compiled.
         * <p>
         * The arguments are read using low-level, type-unsafe techniques.  The order and types
         * of arguments processed here must match those of the compiler method where the
         * breakpoint is set.
         * @throws MaxVMBusyException
         *
         * @see InspectableCompilationInfo#notifyCompilationEvent(ClassMethodActor, TargetMethod)
         */
        private void createCompilerBreakpoints() throws MaxVMBusyException {
            assert compilationStartedBreakpoint == null;
            assert compilationCompletedBreakpoint == null;
            if (usePrecompilationBreakpoints) {
                compilationStartedBreakpoint = breakpointManager().targetBreakpoints().makeSystemBreakpoint(methods().compilationStartedMethodLocation(), null);
                compilationStartedBreakpoint.setDescription("System trap for compilation start");
                compilationStartedBreakpoint.setTriggerEventHandler(new CompilationEventHandler(true));
                Trace.line(TRACE_VALUE, tracePrefix + "creating compilation started breakpoint=" + compilationStartedBreakpoint);
            }
            compilationCompletedBreakpoint = breakpointManager().targetBreakpoints().makeSystemBreakpoint(methods().compilationCompletedMethodLocation(), null);
            compilationCompletedBreakpoint.setDescription("System trap for compilation end");
            compilationCompletedBreakpoint.setTriggerEventHandler(new CompilationEventHandler(false));
            Trace.line(TRACE_VALUE, tracePrefix + "creating compilation completed breakpoint=" + compilationCompletedBreakpoint);
        }

        /**
         * Removes the special target breakpoints set on a method called before and after each compilation in the VM.
         * Don't incur the overhead of a break if there are no bytecode breakpoints enabled.
         * @throws MaxVMBusyException
         */
        private void removeCompilerBreakpoints() throws MaxVMBusyException {
            assert compilationCompletedBreakpoint != null;
            Trace.line(TRACE_VALUE, tracePrefix + "removing compilation completed breakpoint=" + compilationCompletedBreakpoint);
            compilationCompletedBreakpoint.remove();
            compilationCompletedBreakpoint = null;
            if (compilationStartedBreakpoint != null) {
                Trace.line(TRACE_VALUE, tracePrefix + "removing compilation started breakpoint=" + compilationStartedBreakpoint);
                compilationStartedBreakpoint.remove();
                compilationStartedBreakpoint = null;
            }
        }

        /**
         * Creates special system machine code breakpoints in a compiled method in the VM
         * at location specified abstractly by a key.  Normally there is exactly one such location,
         * but in the special case where bytecode index is -1, which specifies the beginning
         * of the compiled method's prologue, there may be more than one for different kinds
         * of calls.
         * <p>
         * May fail when it is not possible to map the bytecode location into a target code location,
         * for example in optimized code where deoptimization is not supported.
         * <p>
         * Trigger events are delegated to the owning bytecode breakpoint.
         *
         * @param owner the breakpoint on whose behalf this breakpoint is being created.
         * @param teleTargetMethod a compilation in the VM of the method specified in the key
         * @return  machine code breakpoints at a location in the compiled method corresponding
         * to the bytecode location specified in the key; null if unable to create.
         * @throws MaxVMBusyException
         */
        private List<VmTargetBreakpoint> createTargetBreakpoints(final VmBytecodeBreakpoint owner, TeleTargetMethod teleTargetMethod) throws MaxVMBusyException {
            assert owner != null;
            final List<VmTargetBreakpoint> targetBreakpoints = new LinkedList<VmTargetBreakpoint>();
            final int bci = owner.methodPositionKey.bci;
            Address address = Address.zero();
            if (bci == -1) {
                int pos = AdapterGenerator.prologueSizeForCallee(teleTargetMethod.targetMethod());
                address = teleTargetMethod.getCodeStart().plus(pos);
                Trace.line(TRACE_VALUE, tracePrefix + "creating transient target breakpoint at method entry in " + teleTargetMethod);
            } else {
                int[] bciToPosMap = teleTargetMethod.bciToPosMap();
                if (bciToPosMap != null && bci < bciToPosMap.length) {
                    int pos = bciToPosMap[bci];
                    address = teleTargetMethod.getCodeStart().plus(pos);
                    Trace.line(TRACE_VALUE, tracePrefix + "creating target breakpoint for offset " + pos + " in " + teleTargetMethod);
                } else {
                    TeleError.unexpected(tracePrefix + "Non-entry bytecode breakpoint unimplemented for target method=" + teleTargetMethod);
                }
            }
            RemoteCodePointer codePointer = null;
            try {
                codePointer = vm().machineCode().makeCodePointer(address);
            } catch (InvalidCodeAddressException e) {
                TeleWarning.message(tracePrefix() + "Invalid breakpoint address " + e.getAddressString() + ":  " + e.getMessage());
            }
            if (codePointer != null && breakpointManager().targetBreakpoints().find(codePointer) == null) {
                final CodeLocation location = vm().codeLocations().createMachineCodeLocation(codePointer, "For bytecode breakpoint=" + owner.codeLocation());
                if (bci == -1) {
                    breakpointManager().targetBreakpoints().makeTransientBreakpoint(location);
                } else {
                    final VMTriggerEventHandler vmTriggerEventHandler;
                    vmTriggerEventHandler = new VMTriggerEventHandler() {
                        public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
                            return owner.handleTriggerEvent(teleNativeThread);
                        }
                    };
                    targetBreakpoints.add(breakpointManager().targetBreakpoints().makeSystemBreakpoint(location, vmTriggerEventHandler, owner));
                }
            } else {
                Trace.line(TRACE_VALUE, tracePrefix + "Target breakpoint already exists at 0x" + address.toHexString() + " in " + teleTargetMethod);
            }
            return targetBreakpoints;
        }

        private void fireBreakpointsChanged() {
            // Notify registered listeners
            for (final MaxBreakpointListener listener : breakpointListeners) {
                listener.breakpointsChanged();
            }
            // Notify the VM
            // Gather classes in which there is at least one bytecode breakpoint set.
            final Set<String> breakpointClassDescriptors = new HashSet<String>();
            for (VmBytecodeBreakpoint breakpoint : breakpointCache) {
                breakpointClassDescriptors.add(breakpoint.holderTypeDescriptorString);
            }
            // Create string containing class descriptors for all classes in which breakpoints are set, each terminated by a space.
            final StringBuilder typeDescriptorsBuilder = new StringBuilder();
            for (String descriptor : breakpointClassDescriptors) {
                typeDescriptorsBuilder.append(descriptor).append(" ");
            }
            final String breakpointClassDescriptorsString = typeDescriptorsBuilder.toString();
            if (breakpointClassDescriptorsString.length() > InspectableCompilationInfo.BREAKPOINT_DESCRIPTORS_ARRAY_LENGTH) {
                final StringBuilder errMsg = new StringBuilder();
                errMsg.append("Implementation Restriction exceeded: list of type descriptors for classes containing ");
                errMsg.append("bytecode breakpoints must not exceed ");
                errMsg.append(InspectableCompilationInfo.BREAKPOINT_DESCRIPTORS_ARRAY_LENGTH).append(" characters.  ");
                errMsg.append("Current length=").append(breakpointClassDescriptorsString.length()).append(" characters.");
                TeleError.unexpected(errMsg.toString());
            }
            Trace.line(TRACE_VALUE, tracePrefix + "Writing to VM type descriptors for breakpoint classes =\"" + breakpointClassDescriptorsString + "\"");
            // Write the string into the designated region in the VM, along with length and incremented epoch counter
            final int charsLength = breakpointClassDescriptorsString.length();
            final RemoteReference charArrayReference = fields().InspectableCompilationInfo_breakpointClassDescriptorCharArray.readReference(vm());
            TeleError.check(!charArrayReference.isZero(), "Can't locate inspectable code array for breakpoint classes");
            for (int index = 0; index < charsLength; index++) {
                Layout.setChar(charArrayReference, index, breakpointClassDescriptorsString.charAt(index));
            }
            fields().InspectableCompilationInfo_breakpointClassDescriptorsCharCount.writeInt(vm(), charsLength);
            fields().InspectableCompilationInfo_breakpointClassDescriptorsEpoch.writeInt(vm(), ++breakpointClassDescriptorsEpoch);
        }

        /**
         * Writes a description of every bytecode breakpoint to the stream, including those usually not shown to clients,
         * with more detail than typically displayed.
         * <p>
         * Thread-safe
         *
         * @param printStream
         */
        void writeSummaryToStream(PrintStream printStream) {
            printStream.println("Bytecodes breakpoints :");
            for (VmBytecodeBreakpoint bytecodeBreakpoint : breakpointCache) {
                printStream.println("  " + bytecodeBreakpoint);
            }
        }

    }
}
