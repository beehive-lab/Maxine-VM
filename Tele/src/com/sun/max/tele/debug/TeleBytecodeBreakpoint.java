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
 * breakpoint in each compilation of the specified method.  When
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

    private final Factory factory;
    private final Key key;

    // Cached string representations of the three parts of a method key
    // for fast comparison when comparing with a method key in the VM.
    private final String holderTypeDescriptorString;
    private final String methodName;
    private final String signatureDescriptorString;

    // Breakpoints are enabled by default.
    private boolean enabled = true;

    // Breakpoints are unconditional by default.
    private BreakpointCondition condition = null;

    /**
     * All target code breakpoints created in compilations of the method in the VM; null iff this breakpoint not enabled.
     */
    private AppendableSequence<TeleTargetBreakpoint> teleTargetBreakpoints;

    /**
     * A new bytecode breakpoint, enabled by default, at a specified location.
     *
     * @param teleVM the VM
     * @param factory the associated bytecode breakpoint factory
     * @param key an abstract description of the location for this breakpoint, expressed in terms of the method and bytecode offset.
     */
    private TeleBytecodeBreakpoint(TeleVM teleVM, Factory factory, Key key) {
        super(teleVM, new TeleCodeLocation(teleVM, key), Kind.CLIENT);
        this.factory = factory;
        this.key = key;
        this.holderTypeDescriptorString = key.holder().string;
        this.methodName = key.name().string;
        this.signatureDescriptorString = key.signature().string;
        Trace.line(TRACE_VALUE, tracePrefix() + "new=" + this);
        createAllTargetBreakpoints();
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
        final TeleTargetBreakpoint teleTargetBreakpoint = factory.createTeleTargetBreakpoint(teleTargetMethod, key);
        if (teleTargetBreakpoint != null) {
            teleTargetBreakpoint.setTriggerEventHandler(condition);
            teleTargetBreakpoints.append(teleTargetBreakpoint);
            Trace.line(TRACE_VALUE, tracePrefix() + "created " + teleTargetBreakpoint + " for " + this);
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + "failed to create teleTargetBreakpoint for " + this);
        }
    }

    /**
     * Create a target code breakpoint in every existing compilation at the location
     * best corresponding to the bytecode location of this breakpoint.
     */
    private void createAllTargetBreakpoints() {
        assert enabled;
        assert teleTargetBreakpoints == null;
        teleTargetBreakpoints = new LinkSequence<TeleTargetBreakpoint>();
        for (TeleTargetMethod teleTargetMethod : TeleTargetMethod.get(teleVM, key)) {
            createTargetBreakpointForMethod(teleTargetMethod);
        }
    }

    /**
     * Remove all target code breakpoints created for this bytecode breakpoint.
     */
    private void clearAllTargetBreakpoints() {
        assert teleTargetBreakpoints != null;
        for (TeleTargetBreakpoint teleTargetBreakpoint : teleTargetBreakpoints) {
            teleTargetBreakpoint.remove();
        }
        teleTargetBreakpoints = null;
        Trace.line(TRACE_VALUE, tracePrefix() + "clearing all target breakpoints for " + this);
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
    public boolean setEnabled(boolean enabled) {
        if (enabled != this.enabled) {
            this.enabled = enabled;
            if (enabled) {
                createAllTargetBreakpoints();
            } else {
                clearAllTargetBreakpoints();
            }
            factory.announceStateChange();
            return true;
        }
        return false;
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
        clearAllTargetBreakpoints();
        factory.removeBreakpoint(this);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Bytecode breakpoint");
        sb.append("{").append(key().toString()).append(", ");
        sb.append(kind().toString()).append(", ");
        sb.append(isEnabled() ? "enabled " : "disabled ");
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
            return "{" + super.toString() + ", position=" + bytecodePosition + "}";
        }
    }

    /**
     * A factory that creates, tracks, and removes bytecode breakpoints from the VM.
     *
     * @author Michael Van De Vanter
     */
    public static class Factory extends Observable {

        private final TeleVM teleVM;
        private final TeleTargetBreakpoint.Factory teleTargetBreakpointFactory;
        private final String tracePrefix;

        // Platform-specific access to method invocation parameters in the VM.
        private final Symbol parameter0;
        private final Symbol parameter1;
        private final Symbol parameter2;
        private final Symbol parameter3;

        /**
         * A breakpoint that interrupts the compiler just as it finishes compiling a method.  Non-null and active
         * iff there are one or more bytecode breakpoints in existence.
         */
        private TeleTargetBreakpoint compilerTargetCodeBreakpoint = null;

        public Factory(TeleVM teleVM) {
            this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
            Trace.line(TRACE_VALUE, tracePrefix + "creating");
            this.teleVM = teleVM;
            this.teleTargetBreakpointFactory = teleVM.teleProcess().targetBreakpointFactory();
            parameter0 = (Symbol) VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI().integerIncomingParameterRegisters().get(0);
            parameter1 = (Symbol) VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI().integerIncomingParameterRegisters().get(1);
            parameter2 = (Symbol) VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI().integerIncomingParameterRegisters().get(2);
            parameter3 = (Symbol) VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI().integerIncomingParameterRegisters().get(3);
        }


        /**
         * Map:  method Key -> existing bytecode breakpoint (whether enabled or not).
         */
        private final VariableMapping<Key, TeleBytecodeBreakpoint> breakpoints = HashMapping.createVariableEqualityMapping();

        /**
         * Notify all observers that there has been a state change concerning these breakpoints.
         */
        private void announceStateChange() {
            setChanged();
            notifyObservers();
        }

        /**
         * @return all bytecode breakpoints that currently exist in the VM.
         * Modification safe against breakpoint removal.
         */
        public synchronized Iterable<MaxBreakpoint> breakpoints() {
            final AppendableSequence<MaxBreakpoint> breakpoints = new LinkSequence<MaxBreakpoint>();
            for (MaxBreakpoint bytecodeBreakpoint : this.breakpoints.values()) {
                breakpoints.append(bytecodeBreakpoint);
            }
            return breakpoints;
        }

        /**
         * @return the number of bytecode breakpoints that currently exist in the VM.
         */
        public synchronized int size() {
            return breakpoints.length();
        }

        /**
         * @param key description of a bytecode position in a method
         * @return a breakpoint set at the position, null if none.
         */
        public synchronized MaxBreakpoint getBreakpoint(Key key) {
            return breakpoints.get(key);
        }

        /**
         * @param key  description of a bytecode position in a method
         * @return a new, enabled bytecode breakpoint
         */
        private TeleBytecodeBreakpoint createBreakpoint(Key key) {
            if (breakpoints.length() == 0) {
                createCompilerBreakpoint();
            }
            final TeleBytecodeBreakpoint breakpoint = new TeleBytecodeBreakpoint(teleVM, this, key);
            breakpoints.put(key, breakpoint);
            Trace.line(TRACE_VALUE, tracePrefix + "new=" + breakpoint);
            announceStateChange();
            return breakpoint;
        }

        /**
         * @param key description of a bytecode position in a method
         * @return a possibly new, enabled bytecode breakpoint
         */
        public synchronized MaxBreakpoint makeBreakpoint(Key key) {
            TeleBytecodeBreakpoint breakpoint = breakpoints.get(key);
            if (breakpoint == null) {
                breakpoint = createBreakpoint(key);
            }
            return breakpoint;
        }

        /**
         * Removes a breakpoint at the described position, if one exists.
         * <br>
         * Assumes that all state related to the breakpoint has already
         * been disposed.
         *
         * @param teleBytecodeBreakpoint the breakpoint being removed.
         */
        private synchronized void removeBreakpoint(TeleBytecodeBreakpoint teleBytecodeBreakpoint) {
            breakpoints.remove(teleBytecodeBreakpoint.key());
            if (breakpoints.length() == 0) {
                removeCompilerBreakpoint();
            }
            Trace.line(TRACE_VALUE, tracePrefix + "removed " + teleBytecodeBreakpoint);
            announceStateChange();
        }

        /**
         * Sets a target code breakpoint on a method known to be called at completion of each method
         * compilation in the VM.  Arguments identify the method just compiled.
         * <br>
         * The arguments are read using low-level, type-unsafe techniques.  The order and types
         * of arguments processed here must match those of the compiler method where the
         * breakpoint is set.
         *
         * @see InspectableCodeInfo#compilationFinished(String, String, String, com.sun.max.vm.compiler.target.TargetMethod)
         */
        private void createCompilerBreakpoint() {
            assert compilerTargetCodeBreakpoint == null;
            final TeleClassMethodActor teleClassMethodActor = teleVM.teleMethods().InspectableCodeInfo_compilationFinished.teleClassMethodActor();
            // TODO (mlvdv) set the breakpoint on all present and future compilations of the compiler!  Not just the first, as is done here.
            final TeleTargetMethod javaTargetMethod = teleClassMethodActor.getJavaTargetMethod(0);
            final Address callEntryPoint = javaTargetMethod.callEntryPoint();
            ProgramError.check(!callEntryPoint.isZero());
            compilerTargetCodeBreakpoint = teleTargetBreakpointFactory.makeSystemBreakpoint(callEntryPoint);
            compilerTargetCodeBreakpoint.setTriggerEventHandler(new VMTriggerEventHandler() {

                @Override
                public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
                    final TeleVM teleVM = Factory.this.teleVM;

                    // The new compilation; don't bother to construct a representation of it unless there's a match and it's needed.
                    TeleTargetMethod teleTargetMethod = null;

                    final String holderTypeDescriptorString = teleVM.getString(teleVM.wordToTemporaryReference(teleNativeThread.integerRegisters().get(parameter0)));
                    final String methodName = teleVM.getString(teleVM.wordToTemporaryReference(teleNativeThread.integerRegisters().get(parameter1)));
                    final String signatureDescriptorString = teleVM.getString(teleVM.wordToTemporaryReference(teleNativeThread.integerRegisters().get(parameter2)));
                    Trace.line(TRACE_VALUE, "VM just compiled: " + holderTypeDescriptorString + " " + methodName + " " + signatureDescriptorString);
                    for (TeleBytecodeBreakpoint teleBytecodeBreakpoint : Factory.this.breakpoints.values()) {
                        // Streamlined comparison using as little Inspector machinery as possible, since we take this break at every VM compilation
                        if (holderTypeDescriptorString.equals(teleBytecodeBreakpoint.holderTypeDescriptorString) &&
                                        methodName.equals(teleBytecodeBreakpoint.methodName) &&
                                        signatureDescriptorString.equals(teleBytecodeBreakpoint.signatureDescriptorString)) {
                            // Match; must set a target breakpoint on the method just compiled; is is acceptable to incur some overhead now.
                            if (teleTargetMethod == null) {
                                final Reference targetMethodReference = teleVM.wordToReference(teleNativeThread.integerRegisters().get(parameter3));
                                teleTargetMethod = (TeleTargetMethod) teleVM.makeTeleObject(targetMethodReference);
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
         *
         * @param teleTargetMethod a compilation in the VM of the method specified in the key
         * @param key an abstract description of a method and bytecode offset
         * @return a target code breakpoint at a location in the compiled method corresponding
         * to the bytecode location specified in the key; null if unable to create.
         */
        private TeleTargetBreakpoint createTeleTargetBreakpoint(TeleTargetMethod teleTargetMethod, Key key) {
            Address address = Address.zero();
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
            return teleTargetBreakpointFactory.makeSystemBreakpoint(address, "Generated from bytecode breakpoint for key=" + key);
        }

        public void writeSummaryToStream(PrintStream printStream) {
            printStream.println("Bytecode breakpoints :");
            for (TeleBytecodeBreakpoint bytecodeBreakpoint : breakpoints.values()) {
                printStream.println("  " + bytecodeBreakpoint);
            }
        }

    }
}
