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
package com.sun.max.tele.object;

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.compiler.cps.target.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.value.*;

/**
 * Canonical surrogate for several possible kinds of compilation of a Java {@link ClassMethod} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public class TeleTargetMethod extends TeleRuntimeMemoryRegion implements TeleTargetRoutine {

    // TODO (mlvdv) implement a sensible and consistent caching strategy

    /**
     * Gets a {@code TeleTargetMethod} instance representing the {@link TargetMethod} in the tele VM that contains a
     * given instruction pointer. If the instruction pointer's address does not lie within a target method, then null is returned.
     * If the instruction pointer is within a target method but there is no {@code TeleTargetMethod} instance existing
     * for it in the {@linkplain TeleCodeRegistry tele code registry}, then a new instance is created and returned.
     *
     * @param address an instruction pointer in the tele VM's address space
     * @return {@code TeleTargetMethod} instance representing the {@code TargetMethod} containing {@code
     *         instructionPointer} or null if there is no {@code TargetMethod} containing {@code instructionPointer}
     */
    public static TeleTargetMethod make(TeleVM teleVM, Address address) {
        assert address != Address.zero();
        if (!teleVM.isBootImageRelocated()) {
            return null;
        }
        TeleTargetMethod teleTargetMethod = teleVM.findTeleTargetRoutine(TeleTargetMethod.class, address);
        if (teleTargetMethod == null
                        && teleVM.findTeleTargetRoutine(TeleTargetRoutine.class, address) == null
                        && teleVM.containsInCode(address)) {
            // Not a known java target method, and not some other kind of known target code, but in a code region
            // See if the code manager in the VM knows about it.
            try {
                final Reference targetMethodReference = teleVM.teleMethods().Code_codePointerToTargetMethod.interpret(new WordValue(address)).asReference();
                // Possible that the address points to an unallocated area of a code region.
                if (targetMethodReference != null && !targetMethodReference.isZero()) {
                    teleTargetMethod = (TeleTargetMethod) teleVM.makeTeleObject(targetMethodReference);  // Constructor will add to register.
                }
            } catch (TeleInterpreterException e) {
                throw ProgramError.unexpected(e);
            }
        }
        return teleTargetMethod;
    }

    /**
     * Gets all target methods that encapsulate code compiled for a given method, either as a top level compilation or
     * as a result of inlining.
     *
     * TODO: Once inlining dependencies are tracked, this method needs to use them.
     *
     * @param teleVM the VM to search
     * @param methodKey the key denoting a method for which the target methods are being requested
     * @return local surrogates for all {@link TargetMethod}s in the {@link TeleVM} that include code compiled for the
     *         method matching {@code methodKey}
     */
    public static Sequence<TeleTargetMethod> get(TeleVM teleVM, MethodKey methodKey) {
        TeleClassActor teleClassActor = teleVM.findTeleClassActor(methodKey.holder());
        if (teleClassActor != null) {
            final AppendableSequence<TeleTargetMethod> result = new LinkSequence<TeleTargetMethod>();
            for (TeleClassMethodActor teleClassMethodActor : teleClassActor.getTeleClassMethodActors()) {
                if (teleClassMethodActor.hasTargetMethod()) {
                    ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
                    if (classMethodActor.name.equals(methodKey.name()) && classMethodActor.descriptor.equals(methodKey.signature())) {
                        for (int i = 0; i < teleClassMethodActor.numberOfCompilations(); ++i) {
                            TeleTargetMethod teleTargetMethod = teleClassMethodActor.getJavaTargetMethod(i);
                            if (teleTargetMethod != null) {
                                result.append(teleTargetMethod);
                            }
                        }
                    }
                }
            }
            return result;
        }
        return Sequence.Static.empty(TeleTargetMethod.class);
    }

    private final TargetCodeRegion targetCodeRegion;

    public TargetCodeRegion targetCodeRegion() {
        return targetCodeRegion;
    }

    TeleTargetMethod(TeleVM teleVM, Reference targetMethodReference) {
        super(teleVM, targetMethodReference);
        // Exception to the general policy of not performing VM i/o during object
        // construction.  This is needed for the code registry.
        // A consequence is synchronized call to the registry from within a synchronized call to {@link TeleObject} construction.
        targetCodeRegion = new TargetCodeRegion(this, start(), size());
        // Register every method compilation, so that they can be located by code address.
        teleVM.registerTeleTargetRoutine(this);
    }

    private Pointer codeStart = Pointer.zero();

    /**
     * @see TargetMethod#codeStart()
     */
    public Pointer getCodeStart() {
        if (codeStart.isZero()) {
            codeStart = teleVM().teleFields().TargetMethod_codeStart.readWord(reference()).asPointer();
        }
        return codeStart;
    }

    public String getName() {
        return getClass().getSimpleName() + " for " + classMethodActor().simpleName();
    }

    private TeleClassMethodActor teleClassMethodActor;

    /**
     * Gets the tele class method actor for this target method.
     *
     * @return {@code null} if the class method actor is null the target method
     */
    public TeleClassMethodActor getTeleClassMethodActor() {
        if (teleClassMethodActor == null) {
            final Reference classMethodActorReference = teleVM().teleFields().TargetMethod_classMethodActor.readReference(reference());
            teleClassMethodActor = (TeleClassMethodActor) teleVM().makeTeleObject(classMethodActorReference);
        }
        return teleClassMethodActor;
    }

    public TeleRoutine teleRoutine() {
        return getTeleClassMethodActor();
    }

    /**
     * Gets the local mirror of the class method actor associated with this target method. This may be null.
     */
    public ClassMethodActor classMethodActor() {
        final TeleClassMethodActor teleClassMethodActor = getTeleClassMethodActor();
        if (teleClassMethodActor == null) {
            return null;
        }
        return teleClassMethodActor.classMethodActor();
    }

    /**
     * Gets the entry point for this method as specified by the ABI in use when this target method was compiled.
     *
     * @return {@link Address#zero()} if this target method has not yet been compiled
     */
    public final Address callEntryPoint() {
        final Pointer codeStart = getCodeStart();
        if (codeStart.isZero()) {
            return Address.zero();
        }
        final Reference callEntryPointReference = TeleInstanceReferenceFieldAccess.readPath(reference(), teleVM().teleFields().TargetMethod_abi, teleVM().teleFields().TargetABI_callEntryPoint);
        final TeleObject teleCallEntryPoint = teleVM().makeTeleObject(callEntryPointReference);
        if (teleCallEntryPoint == null) {
            return codeStart;
        }
        final CallEntryPoint callEntryPoint = (CallEntryPoint) teleCallEntryPoint.deepCopy();
        return codeStart.plus(callEntryPoint.offsetFromCodeStart());
    }

    /**
     * Gets the byte array containing the target-specific machine code of this target method in the {@link TeleVM}.
     * @see TargetMethod#code()
     */
    public final byte[] getCode() {
        final Reference codeReference = teleVM().teleFields().TargetMethod_code.readReference(reference());
        if (codeReference.isZero()) {
            return null;
        }
        final TeleArrayObject teleCode = (TeleArrayObject) teleVM().makeTeleObject(codeReference);
        return (byte[]) teleCode.shallowCopy();
    }

    /**
     * Gets the length of the byte array containing the target-specific machine code of this target method in the {@link TeleVM}.
     * @see TargetMethod#codeLength()
     */
    public final int getCodeLength() {
        final Reference codeReference = teleVM().teleFields().TargetMethod_code.readReference(reference());
        if (codeReference.isZero()) {
            return 0;
        }
        final TeleArrayObject teleCodeArrayObject = (TeleArrayObject) teleVM().makeTeleObject(codeReference);
        return teleCodeArrayObject.getLength();
    }

    private IndexedSequence<TargetCodeInstruction> instructions;

    public final  IndexedSequence<TargetCodeInstruction> getInstructions() {
        if (instructions == null) {
            final byte[] code = getCode();
            if (code != null) {
                instructions = TeleDisassembler.decode(teleVM().vmConfiguration().platform().processorKind, getCodeStart(), code, getEncodedInlineDataDescriptors());
            }
        }
        return instructions;
    }

    /**
     * @see  StopPositions
     * @see  TargetMethod#stopPositions()
     */
    protected StopPositions stopPositions;

    /**
     * @see TargetMethod#stopPositions()
     */
    public StopPositions getStopPositions() {
        if (stopPositions == null) {
            final Reference intArrayReference = teleVM().teleFields().TargetMethod_stopPositions.readReference(reference());
            final TeleArrayObject teleIntArrayObject = (TeleArrayObject) teleVM().makeTeleObject(intArrayReference);
            if (teleIntArrayObject == null) {
                return null;
            }
            stopPositions = new StopPositions((int[]) teleIntArrayObject.shallowCopy());
        }
        return stopPositions;
    }

    /**
     * @see TargetMethod#numberOfStopPositions()
     */
    public int getNumberOfStopPositions() {
        final StopPositions stopPositions = getStopPositions();
        return stopPositions == null ? 0 : stopPositions.length();
    }

    public int getJavaStopIndex(Address address) {
        final StopPositions stopPositions = getStopPositions();
        if (stopPositions != null) {
            final int targetCodePosition = address.minus(getCodeStart()).toInt();
            for (int i = 0; i < stopPositions.length(); i++) {
                if (stopPositions.get(i) == targetCodePosition) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean isAtJavaStop(Address address) {
        return getJavaStopIndex(address) >= 0;
    }

    /**
     * @return position of the closest following call instruction, direct or indirect; -1 if none.
     */
    private int getNextCallPosition(int position) {
        int nextCallPosition = -1;
        final StopPositions stopPositions = getStopPositions();
        if (stopPositions != null) {
            final int numberOfCalls = getNumberOfDirectCalls() + getNumberOfIndirectCalls();
            for (int i = 0; i < numberOfCalls; i++) {
                final int stopPosition = stopPositions.get(i);
                if (stopPosition > position && (nextCallPosition == -1 || stopPosition < nextCallPosition)) {
                    nextCallPosition = stopPosition;
                }
            }
        }
        return nextCallPosition;
    }

    public Address getNextCallAddress(Address address) {
        final int targetCodePosition = address.minus(getCodeStart()).toInt();
        final int nextCallPosition = getNextCallPosition(targetCodePosition);
        if (nextCallPosition > 0) {
            return getCodeStart().plus(nextCallPosition);
        }
        return Address.zero();
    }

    /**
     * @see TargetMethod#directCallees()
     */
    public Reference[] getDirectCallees() {
        final Reference refArrayReference = teleVM().teleFields().TargetMethod_directCallees.readReference(reference());
        final TeleArrayObject teleRefArrayObject = (TeleArrayObject) teleVM().makeTeleObject(refArrayReference);
        if (teleRefArrayObject == null) {
            return null;
        }
        final Reference[] classMethodActorReferenceArray = (Reference[]) teleRefArrayObject.shallowCopy();
        assert classMethodActorReferenceArray.length == getNumberOfDirectCalls();
        return classMethodActorReferenceArray;
    }

    public int getNumberOfDirectCalls() {
        final StopPositions stopPositions = getStopPositions();
        if (stopPositions == null) {
            return 0;
        }
        return stopPositions.length() - (getNumberOfIndirectCalls() + getNumberOfSafepoints());
    }

    /**
     * @see TargetMethod#numberOfIndirectCalls()
     */
    public int getNumberOfIndirectCalls() {
        return teleVM().teleFields().TargetMethod_numberOfIndirectCalls.readInt(reference());
    }

    /**
     * @see TargetMethod#numberOfSafepoints()
     */
    public int getNumberOfSafepoints() {
        return teleVM().teleFields().TargetMethod_numberOfSafepoints.readInt(reference());
    }

     /**
     * Gets the {@linkplain InlineDataDescriptor inline data descriptors} associated with this target method's code in the {@link TeleVM}
     * encoded as a byte array in the format described {@linkplain InlineDataDescriptor here}.
     *
     * @return null if there are no inline data descriptors associated with this target method's code
     * @see TargetMethod#encodedInlineDataDescriptors()
     */
    public byte[] getEncodedInlineDataDescriptors() {
        return null;
    }

    /**
     * Gets the Java frame descriptor corresponding to a given stop index.
     *
     * @param stopIndex a stop index
     * @return the Java frame descriptor corresponding to {@code stopIndex} or null if there is no Java frame descriptor
     *         for {@code stopIndex}
     */
    public TargetJavaFrameDescriptor getJavaFrameDescriptor(int stopIndex) {
        return null;
    }

    private TargetABI abi;

    public TargetABI getAbi() {
        if (abi == null) {
            final Reference abiReference = teleVM().teleFields().TargetMethod_abi.readReference(reference());
            final TeleObject teleTargetABI = teleVM().makeTeleObject(abiReference);
            if (teleTargetABI != null) {
                abi = (TargetABI) teleTargetABI.deepCopy();
            }
        }
        return abi;
    }

    // [tw] Warning: duplicated code!
    public MachineCodeInstructionArray getTargetCodeInstructions() {
        final IndexedSequence<TargetCodeInstruction> instructions = getInstructions();
        final MachineCodeInstruction[] result = new MachineCodeInstruction[instructions.length()];
        for (int i = 0; i < result.length; i++) {
            final TargetCodeInstruction ins = instructions.get(i);
            result[i] = new MachineCodeInstruction(ins.mnemonic, ins.position, ins.address.toLong(), ins.label, ins.bytes, ins.operands, ins.getTargetAddressAsLong());
        }
        return new MachineCodeInstructionArray(result);
    }

    public MethodProvider getMethodProvider() {
        return this.teleClassMethodActor;
    }

    /**
     * Sets a target breakpoint at the {@linkplain #callEntryPoint() entry point} of this target method.
     *
     * @return the breakpoint that was set or null if this target method has not yet been compiled
     */
    public TeleTargetBreakpoint setTargetBreakpointAtEntry() {
        final Address callEntryPoint = callEntryPoint();
        if (callEntryPoint.isZero()) {
            return null;
        }
        final TeleTargetBreakpoint teleTargetBreakpoint = teleVM().makeTargetBreakpointAt(callEntryPoint);
        teleTargetBreakpoint.setDescription("Method entry");
        return teleTargetBreakpoint;
    }

    /**
     * Sets a target breakpoint at each labeled instruction in this target method.
     * No breakpoints will be set if this target method has not yet been compiled
     */
    public void setTargetCodeLabelBreakpoints() {
        final IndexedSequence<TargetCodeInstruction> instructions = getInstructions();
        if (instructions != null) {
            for (TargetCodeInstruction targetCodeInstruction : instructions) {
                if (targetCodeInstruction.label != null) {
                    final TeleTargetBreakpoint teleTargetBreakpoint = teleVM().makeTargetBreakpointAt(targetCodeInstruction.address);
                    teleTargetBreakpoint.setDescription("Label " + targetCodeInstruction.label.toString());
                }
            }
        }
    }

    /**
     * Removed the target breakpoint (if any) at each labeled instruction in this target method.
     * No action is taken if this target method has not yet been compiled
     */
    public void removeTargetCodeLabelBreakpoints() {
        final IndexedSequence<TargetCodeInstruction> instructions = getInstructions();
        if (instructions != null) {
            for (TargetCodeInstruction targetCodeInstruction : instructions) {
                if (targetCodeInstruction.label != null) {
                    final MaxBreakpoint breakpoint = teleVM().getBreakpointAt(targetCodeInstruction.address);
                    if (breakpoint != null) {
                        breakpoint.remove();
                    }
                }
            }
        }
    }

    /**
     * Cache for {@link #reducedDeepCopy()}.
     */
    private TargetMethod targetMethod;

    /**
     * Gets a special, reduced deep copy, (newly created if not in cache) that is suitable for use by
     * {@link StackFrameWalker#targetMethodFor(Pointer)}.
     */
    public TargetMethod reducedDeepCopy() {
        if (targetMethod == null) {
            targetMethod = (TargetMethod) deepCopy(reducedDeepCopier());
        }
        return targetMethod;
    }

    protected DeepCopier reducedDeepCopier() {
        return new ReducedDeepCopier();
    }

    /**
     * A copier to be used for implementing {@link TeleTargetMethod#reducedDeepCopy()}.
     */
    class ReducedDeepCopier extends DeepCopier {
        public ReducedDeepCopier() {
            TeleFields teleFields = teleVM().teleFields();
            omit(teleFields.TargetMethod_scalarLiterals.fieldActor());
            omit(teleFields.TargetMethod_referenceLiterals.fieldActor());
            abi = teleFields.TargetMethod_abi.fieldActor();
            compilerScheme = teleFields.TargetMethod_compilerScheme.fieldActor();
        }
        private final FieldActor abi;
        private final FieldActor compilerScheme;

        private C1XCompilerScheme c1xCompilerScheme;

        @Override
        protected Object makeDeepCopy(FieldActor fieldActor, TeleObject teleObject) {
            if (fieldActor.equals(compilerScheme)) {
                Class<?> type = teleObject.classActorForType().toJava();
                if (VMConfiguration.hostOrTarget().bootCompilerScheme().getClass() == type) {
                    return VMConfiguration.hostOrTarget().bootCompilerScheme();
                } else if (VMConfiguration.hostOrTarget().jitCompilerScheme().getClass() == type) {
                    return VMConfiguration.hostOrTarget().jitCompilerScheme();
                } else if (C1XCompilerScheme.class == type) {
                    if (c1xCompilerScheme == null) {
                        c1xCompilerScheme = new C1XCompilerScheme(VMConfiguration.hostOrTarget());
                    }
                    return c1xCompilerScheme;
                }
                throw ProgramError.unexpected();
            } else if (fieldActor.equals(abi)) {
                return getAbi();
            } else {
                return super.makeDeepCopy(fieldActor, teleObject);
            }
        }
    }

    @Override
    public ReferenceTypeProvider getReferenceType() {
        return teleVM().vmAccess().getReferenceType(getClass());
    }

    /**
     * Disassembles this target method's code to a given writer.
     */
    public void disassemble(IndentWriter writer) {
    }
}
