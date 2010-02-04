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
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.type.*;

/**
 * A breakpoint located at the beginning of a bytecode instruction
 * in a method in the VM.
 * <br>
 * When enabled, a bytecode breakpoint creates a target code
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

    private final Factory factory;
    private final Key key;

    // Cached string representations of the three parts of a method key
    // for fast comparison when comparing with a method key in the VM.
    private final String holderTypeDescriptorString;
    private final String methodName;
    private final String signatureDescriptorString;

    private boolean enabled = false;

    // Breakpoints are unconditional by default.
    private BreakpointCondition condition = null;

    /**
     * All target code breakpoints created in compilations of the method in the VM.
     * Non-null iff this breakpoint is enabled; null if disabled.
     */
    private AppendableSequence<TeleTargetBreakpoint> teleTargetBreakpoints;

    /**
     * A new bytecode breakpoint, enabled by default, at a specified location.
     *
     * @param teleVM the VM
     * @param factory the associated bytecode breakpoint factory
     * @param key an abstract description of the location for this breakpoint, expressed in terms of the method and bytecode offset.
     * @param kind the kind of breakpoint to create
     */
    private TeleBytecodeBreakpoint(TeleVM teleVM, Factory factory, Key key, BreakpointKind kind) {
        super(teleVM, new TeleCodeLocation(teleVM, key), kind);
        this.factory = factory;
        this.key = key;
        this.holderTypeDescriptorString = key.holder().string;
        this.methodName = key.name().string;
        this.signatureDescriptorString = key.signature().string;
        Trace.line(TRACE_VALUE, tracePrefix() + "new=" + this);
        setEnabled(true);
    }

    /**
     * Creates a target code breakpoint in a compilation of this method in the VM, at a location
     * corresponding to the bytecode location for which this breakpoint was created.
     *
     * @param teleTargetMethod a compilation in the VM of the method for which this breakpoint was created.
     */
    private void createTargetBreakpointForMethod(TeleTargetMethod teleTargetMethod) {
        assert enabled;
        // Delegate creation of the target breakpoint to the factory.
        final TeleTargetBreakpoint teleTargetBreakpoint = factory.createTeleTargetBreakpoint(this, teleTargetMethod);
        if (teleTargetBreakpoint != null) {
            // TODO (mlvdv) If we support conditions, need to combine it with the trigger handler added by factory method.
            teleTargetBreakpoints.append(teleTargetBreakpoint);
            Trace.line(TRACE_VALUE, tracePrefix() + "created " + teleTargetBreakpoint + " for " + this);
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + "failed to create teleTargetBreakpoint for " + this);
        }
    }

    /**
     * Handle notification that the method for which this breakpoint was created has just been compiled, possibly
     * but not necessarily the first of several compilations.
     *
     * @param teleTargetMethod a just completed compilation in the VM of the method for which this breakpoint was created.
     */
    private void handleNewCompilation(TeleTargetMethod teleTargetMethod) {
        if (enabled) {
            createTargetBreakpointForMethod(teleTargetMethod);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        assert this.enabled != enabled;
        this.enabled = enabled;
        if (enabled) {
            assert teleTargetBreakpoints == null;
            // Create a target code breakpoint in every existing compilation at the location
            // best corresponding to the bytecode location of this breakpoint.
            teleTargetBreakpoints = new LinkSequence<TeleTargetBreakpoint>();
            for (TeleTargetMethod teleTargetMethod : TeleTargetMethod.get(teleVM, key)) {
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
        factory.fireBreakpointsChanged();
    }

    @Override
    public BreakpointCondition getCondition() {
        return condition;
    }

    @Override
    public void setCondition(String conditionDescriptor) throws ExpressionException {
        this.condition = new BreakpointCondition(teleVM(), conditionDescriptor);
        for (TeleTargetBreakpoint teleTargetBreakpoint : teleTargetBreakpoints) {
            teleTargetBreakpoint.setTriggerEventHandler(condition);
        }
    }

    @Override
    public void remove() {
        Trace.line(TRACE_VALUE, tracePrefix() + "removing breakpoint=" + this);
        if (enabled) {
            setEnabled(false);
        }
        factory.removeBreakpoint(this);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Bytecode breakpoint");
        sb.append("{");
        sb.append(kind().toString()).append(", ");
        sb.append(key().toString()).append(", ");
        sb.append(isEnabled() ? "enabled " : "disabled ");
        if (getDescription() != null) {
            sb.append(", \"").append(getDescription()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * @return description of the bytecode location of this breakpoint.
     */
    public Key key() {
        return getCodeLocation().key();
    }

    /**
     * Describes a bytecode position in the VM,
     * i.e. indicates the exact method and byte code position.
     * <br>
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
            super(bytecodeLocation.classMethodActor);
            this.bytecodePosition = bytecodeLocation.bytecodePosition;
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
            final StringBuilder sb = new StringBuilder();
            sb.append("Key{");
            sb.append(name()).append(signature().toJavaString(false, false));
            sb.append(", pos=").append(bytecodePosition);
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * A factory that creates, tracks, and removes bytecode breakpoints from the VM.
     * <br>
     * Bytecode breakpoints can be created before a specified method is compiled
     * or even loaded.
     * <br>
     * A bytecode breakpoint causes a target code breakpoint to be created for every
     * compilation of the specified method, current and future.
     *
     * @author Michael Van De Vanter
     */
    public static class Factory extends AbstractTeleVMHolder {

        private static final Sequence<TeleBytecodeBreakpoint> EMPTY_BREAKPOINT_SEQUENCE = Sequence.Static.empty(TeleBytecodeBreakpoint.class);

        private final TeleTargetBreakpoint.Factory teleTargetBreakpointFactory;
        private final String tracePrefix;

        // Platform-specific access to method invocation parameters in the VM.
        private final Symbol parameter0;
        private final Symbol parameter1;
        private final Symbol parameter2;
        private final Symbol parameter3;

        /**
         * Map:  method Key -> existing bytecode breakpoint (whether enabled or not).
         */
        private final VariableMapping<Key, TeleBytecodeBreakpoint> breakpoints = HashMapping.createVariableEqualityMapping();

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

        public Factory(TeleVM teleVM) {
            super(teleVM);
            this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
            Trace.line(TRACE_VALUE, tracePrefix + "creating");
            this.teleTargetBreakpointFactory = teleVM.teleProcess().targetBreakpointFactory();
            parameter0 = (Symbol) VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI().integerIncomingParameterRegisters().get(0);
            parameter1 = (Symbol) VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI().integerIncomingParameterRegisters().get(1);
            parameter2 = (Symbol) VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI().integerIncomingParameterRegisters().get(2);
            parameter3 = (Symbol) VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI().integerIncomingParameterRegisters().get(3);
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
         * @return all client bytecode breakpoints that currently exist in the VM.
         * Modification safe against breakpoint removal.
         */
        public Sequence<MaxBreakpoint> clientBreakpoints() {
            if (breakpointCache.isEmpty()) {
                return Sequence.Static.empty(MaxBreakpoint.class);
            }
            final AppendableSequence<MaxBreakpoint> clientBreakpoints = new ArrayListSequence<MaxBreakpoint>();
            for (TeleBytecodeBreakpoint breakpoint : breakpointCache) {
                if (breakpoint.kind() == BreakpointKind.CLIENT) {
                    clientBreakpoints.append(breakpoint);
                }
            }
            return clientBreakpoints;
        }

        public int clientBreakpointCount() {
            int count = 0;
            for (TeleBytecodeBreakpoint breakpoint : breakpointCache) {
                if (breakpoint.kind() == BreakpointKind.CLIENT) {
                    count++;
                }
            }
            return count;
        }

        /**
         * @param key description of a bytecode position in a method
         * @return a client breakpoint set at the position, null if no client breakpoint at the position
         */
        public synchronized MaxBreakpoint getClientBreakpoint(Key key) {
            TeleBytecodeBreakpoint breakpoint = breakpoints.get(key);
            return (breakpoint.kind() == BreakpointKind.CLIENT) ? breakpoint : null;
        }

        /**
         * Returns a clientBreakpoint matching a method location described
         * abstractly, newly created if one does not already exist for the location.
         * Fails if there is a system breakpoint already at that location.
         *
         * @param key description of a bytecode position in a method
         * @return a possibly new, enabled bytecode breakpoint,
         * null if a system breakpoint is already at the location.
         */
        public synchronized TeleBreakpoint makeClientBreakpoint(Key key) {
            TeleBytecodeBreakpoint breakpoint = breakpoints.get(key);
            if (breakpoint == null) {
                breakpoint = createBreakpoint(key, BreakpointKind.CLIENT);
                breakpoint.setDescription("Client-specified breakpoint");
            } else if (breakpoint.kind() != BreakpointKind.CLIENT) {
                return null;
            }
            return breakpoint;
        }

        /**
         * Returns a client breakpoint at the entry of a method location described
         * abstractly, newly created if one does not already exist for the location.
         * Fails if there is a system breakpoint already at that location.
         *
         * @param maxInspectableMethod description of a method
         * @return a possibly new, enabled bytecode breakpoint at method entry,
         * null if a system breakpoint is already at the location.
         */
        public synchronized TeleBreakpoint makeClientBreakpoint(MaxInspectableMethod maxInspectableMethod) {
            final MethodActor methodActor = maxInspectableMethod.teleClassMethodActor().methodActor();
            final TeleBytecodeBreakpoint.Key key = new TeleBytecodeBreakpoint.Key(new MethodActorKey(methodActor), 0);
            TeleBytecodeBreakpoint breakpoint = breakpoints.get(key);
            if (breakpoint == null) {
                breakpoint = createBreakpoint(key, BreakpointKind.CLIENT);
                breakpoint.setDescription(maxInspectableMethod.description());
            } else if (breakpoint.kind() != BreakpointKind.CLIENT) {
                return null;
            }
            return breakpoint;
        }

        /**
         * Returns a system breakpoint at the entry of a method location described
         * abstractly, newly created if one does not already exist for the location.
         * Fails if there is a client breakpoint already at that location.
         *
         * @param maxInspectableMethod description of a method
         * @param handler handler to be invoked when breakpoint triggers
         * @return a possibly new, enabled bytecode breakpoint at method entry,
         * null if a client breakpoint is already at the location.
         */
        public synchronized TeleBreakpoint makeSystemBreakpoint(MaxInspectableMethod maxInspectableMethod, VMTriggerEventHandler handler) {
            final MethodActor methodActor = maxInspectableMethod.teleClassMethodActor().methodActor();
            final TeleBytecodeBreakpoint.Key key = new TeleBytecodeBreakpoint.Key(new MethodActorKey(methodActor), 0);
            TeleBytecodeBreakpoint breakpoint = breakpoints.get(key);
            if (breakpoint == null) {
                breakpoint = createBreakpoint(key, BreakpointKind.SYSTEM);
                breakpoint.setTriggerEventHandler(handler);
                breakpoint.setDescription(maxInspectableMethod.description());
            } else if (breakpoint.kind() != BreakpointKind.SYSTEM) {
                ProgramWarning.message("Can't create system bytecode breakpoint - client breakpoint already exists: " + maxInspectableMethod);
                return null;
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
         * @param key  description of a bytecode position in a method
         * @param kind he kind of breakpoint to be created
         * @return a new, enabled bytecode breakpoint
         */
        private TeleBytecodeBreakpoint createBreakpoint(Key key, BreakpointKind kind) {
            if (breakpoints.length() == 0) {
                createCompilerBreakpoint();
            }
            final TeleBytecodeBreakpoint breakpoint = new TeleBytecodeBreakpoint(teleVM(), this, key, kind);
            breakpoints.put(key, breakpoint);
            updateBreakpointCache();
            Trace.line(TRACE_VALUE, tracePrefix + "new=" + breakpoint);
            fireBreakpointsChanged();
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
        private synchronized void removeBreakpoint(TeleBytecodeBreakpoint teleBytecodeBreakpoint) {
            final TeleBytecodeBreakpoint removedBreakpoint = breakpoints.remove(teleBytecodeBreakpoint.key());
            ProgramWarning.check(removedBreakpoint != null, "Failed to remove breakpoint" + teleBytecodeBreakpoint);
            if (breakpoints.length() == 0) {
                removeCompilerBreakpoint();
            }
            updateBreakpointCache();
            Trace.line(TRACE_VALUE, tracePrefix + "removed " + teleBytecodeBreakpoint);
            fireBreakpointsChanged();
        }

        /**
         * Sets a target code breakpoint on a method known to be called at completion of each method
         * compilation in the VM.  Arguments identify the method just compiled.
         * <br>
         * The arguments are read using low-level, type-unsafe techniques.  The order and types
         * of arguments processed here must match those of the compiler method where the
         * breakpoint is set.
         *
         * @see InspectableCodeInfo#compilationComplete(String, String, String, com.sun.max.vm.compiler.target.TargetMethod)
         */
        private void createCompilerBreakpoint() {
            assert compilerTargetCodeBreakpoint == null;
            final TeleClassMethodActor teleClassMethodActor = teleVM().teleMethods().InspectableCodeInfo_inspectableCompilationComplete.teleClassMethodActor();
            // TODO (mlvdv) set the breakpoint on all present and future compilations of the compiler!  Not just the first, as is done here.
            final TeleTargetMethod javaTargetMethod = teleClassMethodActor.getJavaTargetMethod(0);
            final Address callEntryPoint = javaTargetMethod.callEntryPoint();
            ProgramError.check(!callEntryPoint.isZero());
            compilerTargetCodeBreakpoint = teleTargetBreakpointFactory.makeSystemBreakpoint(callEntryPoint, null);
            compilerTargetCodeBreakpoint.setDescription("System trap for VM compiler");
            compilerTargetCodeBreakpoint.setTriggerEventHandler(new VMTriggerEventHandler() {
                public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {

                    // The new compilation; don't bother to construct a representation of it unless there's a match and it's needed.
                    TeleTargetMethod teleTargetMethod = null;

                    final String holderTypeDescriptorString = teleVM().getStringUnsafe(teleVM().wordToTemporaryReference(teleNativeThread.integerRegisters().get(parameter0)));
                    final String methodName = teleVM().getStringUnsafe(teleVM().wordToTemporaryReference(teleNativeThread.integerRegisters().get(parameter1)));
                    final String signatureDescriptorString = teleVM().getStringUnsafe(teleVM().wordToTemporaryReference(teleNativeThread.integerRegisters().get(parameter2)));
                    Trace.line(COMPILATION_TRACE_VALUE, "VM just compiled: " + holderTypeDescriptorString + " " + methodName + " " + signatureDescriptorString);
                    for (TeleBytecodeBreakpoint teleBytecodeBreakpoint : breakpointCache) {
                        // Streamlined comparison using as little Inspector machinery as possible, since we take this break at every VM compilation
                        if (holderTypeDescriptorString.equals(teleBytecodeBreakpoint.holderTypeDescriptorString) &&
                                        methodName.equals(teleBytecodeBreakpoint.methodName) &&
                                        signatureDescriptorString.equals(teleBytecodeBreakpoint.signatureDescriptorString)) {
                            // Match; must set a target breakpoint on the method just compiled; is is acceptable to incur some overhead now.
                            if (teleTargetMethod == null) {
                                final Reference targetMethodReference = teleVM().wordToReference(teleNativeThread.integerRegisters().get(parameter3));
                                teleTargetMethod = (TeleTargetMethod) teleVM().makeTeleObject(targetMethodReference);
                            }
                            teleBytecodeBreakpoint.handleNewCompilation(teleTargetMethod);
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
         */
        private void removeCompilerBreakpoint() {
            assert compilerTargetCodeBreakpoint != null;
            Trace.line(TRACE_VALUE, tracePrefix + "removing compiler breakpoint=" + compilerTargetCodeBreakpoint);
            compilerTargetCodeBreakpoint.remove();
            compilerTargetCodeBreakpoint = null;
        }

        /**
         * Creates a special system target code breakpoint in a compiled method in the VM
         * at a location specified abstractly by a key.
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
         */
        private TeleTargetBreakpoint createTeleTargetBreakpoint(final TeleBytecodeBreakpoint owner, TeleTargetMethod teleTargetMethod) {
            assert owner != null;
            Address address = Address.zero();
            final Key key = owner.key();
            if (teleTargetMethod instanceof TeleJitTargetMethod) {
                final TeleJitTargetMethod teleJitTargetMethod = (TeleJitTargetMethod) teleTargetMethod;
                final int[] bytecodeToTargetCodePositionMap = teleJitTargetMethod.bytecodeToTargetCodePositionMap();
                final int targetCodePosition = bytecodeToTargetCodePositionMap[key.bytecodePosition];
                address = teleTargetMethod.getCodeStart().plus(targetCodePosition);
                Trace.line(TRACE_VALUE, tracePrefix + "creating target breakpoint for offset " + targetCodePosition + " in " + teleTargetMethod);
            } else {
                if (key.bytecodePosition == 0) {
                    address = teleTargetMethod.callEntryPoint();
                    Trace.line(TRACE_VALUE, tracePrefix + "creating target breakpoint at method entry in " + teleTargetMethod);
                } else {
                    ProgramWarning.message(tracePrefix + "Non-entry bytecode breakpoint unimplemented for target method=" + teleTargetMethod);
                    return null;
                }
            }
            if (teleTargetBreakpointFactory.getTargetBreakpointAt(address) != null) {
                Trace.line(TRACE_VALUE, tracePrefix + "Target breakpoint already exists at 0x" + address.toHexString() + " in " + teleTargetMethod);
                return null;
            }
            final TeleTargetBreakpoint teleTargetBreakpoint = teleTargetBreakpointFactory.makeSystemBreakpoint(address, owner);
            teleTargetBreakpoint.setDescription("For bytecode " + key);
            teleTargetBreakpoint.setTriggerEventHandler(new VMTriggerEventHandler() {

                public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
                    return owner.handleTriggerEvent(teleNativeThread);
                }
            });
            return teleTargetBreakpoint;
        }


        private void fireBreakpointsChanged() {
            for (final MaxBreakpointListener listener : breakpointListeners) {
                listener.breakpointsChanged();
            }
        }

        public void writeSummaryToStream(PrintStream printStream) {
            printStream.println("Bytecode breakpoints :");
            for (TeleBytecodeBreakpoint bytecodeBreakpoint : breakpointCache) {
                printStream.println("  " + bytecodeBreakpoint);
            }
        }

    }
}
