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

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.type.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;

/**
 * Canonical surrogate for several possible kinds of compilation of a Java {@link ClassMethod} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleTargetMethod extends TeleRuntimeMemoryRegion implements TeleTargetRoutine {

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
                final Reference targetMethodReference = teleVM.methods().Code_codePointerToTargetMethod.interpret(new WordValue(address)).asReference();
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
     * @return  local surrogates for all {@link TargetMethod}s in the {@link TeleVM} that match the specified key.
     */
    public static Sequence<TeleTargetMethod> get(TeleVM teleVM, MethodKey methodKey) {
        final AppendableSequence<TeleTargetMethod> result = new LinkSequence<TeleTargetMethod>();
        final DefaultMethodKey defaultMethodKey = new DefaultMethodKey(methodKey.holder(), methodKey.name(), methodKey.signature());
        // The interpreter will be unhappy passing in arguments whose dynamic types are alien (non-VM) classes, so create one for the key.
        try {
            final Reference targetMethodArrayReference = teleVM.methods().Code_methodKeyToTargetMethods.interpret(TeleReferenceValue.from(defaultMethodKey)).asReference();
            final TeleArrayObject teleTargetMethodArrayObject = (TeleArrayObject) teleVM.makeTeleObject(targetMethodArrayReference);
            if (teleTargetMethodArrayObject != null) {
                for (Reference targetMethodReference : (Reference []) teleTargetMethodArrayObject.shallowCopy()) {
                    final TeleTargetMethod teleTargetMethod = (TeleTargetMethod) teleVM.makeTeleObject(targetMethodReference);
                    result.append(teleTargetMethod);
                }
            }
            return result;
        } catch (TeleInterpreterException e) {
            throw ProgramError.unexpected(e);
        }
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
            codeStart = teleVM().fields().TargetMethod_codeStart.readWord(reference()).asPointer();
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
            final Reference classMethodActorReference = teleVM().fields().TargetMethod_classMethodActor.readReference(reference());
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
        final Reference callEntryPointReference = TeleInstanceReferenceFieldAccess.readPath(reference(), teleVM().fields().TargetMethod_abi, teleVM().fields().TargetABI_callEntryPoint);
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
        final Reference codeReference = teleVM().fields().TargetMethod_code.readReference(reference());
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
        final Reference codeReference = teleVM().fields().TargetMethod_code.readReference(reference());
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
     * @see  TargetMethod#stopPositions()
     */
    private int[] stopPositions;

    /**
     * @see TargetMethod#stopPositions()
     */
    public int[] getStopPositions() {
        if (stopPositions == null) {
            final Reference intArrayReference = teleVM().fields().TargetMethod_stopPositions.readReference(reference());
            final TeleArrayObject teleIntArrayObject = (TeleArrayObject) teleVM().makeTeleObject(intArrayReference);
            if (teleIntArrayObject == null) {
                return null;
            }
            stopPositions = (int[]) teleIntArrayObject.shallowCopy();

            // Since only the VM's deoptimization algorithm cares about these flags, we omit them here:
            for (int i = 0; i < stopPositions.length; i++) {
                stopPositions[i] &= ~TargetMethod.REFERENCE_RETURN_FLAG;
            }
        }
        return stopPositions;
    }

    /**
     * @see TargetMethod#numberOfStopPositions()
     */
    public int getNumberOfStopPositions() {
        final int[] stopPositions = getStopPositions();
        return stopPositions == null ? 0 : stopPositions.length;
    }

    public int getJavaStopIndex(Address address) {
        final int[] stopPositions = getStopPositions();
        if (stopPositions != null) {
            final int targetCodePosition = address.minus(getCodeStart()).toInt();
            for (int i = 0; i < stopPositions.length; i++) {
                if (stopPositions[i] == targetCodePosition) {
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
        final int[] stopPositions = getStopPositions();
        if (stopPositions != null) {
            final int numberOfCalls = getNumberOfDirectCalls() + getNumberOfIndirectCalls();
            for (int i = 0; i < numberOfCalls; i++) {
                final int stopPosition = stopPositions[i];
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
     * @see TargetMethod#catchRangePositions()
     */
    public int[] getCatchRangePositions() {
        final Reference intArrayReference = teleVM().fields().TargetMethod_catchRangePositions.readReference(reference());
        final TeleArrayObject teleIntArrayObject = (TeleArrayObject) teleVM().makeTeleObject(intArrayReference);
        if (teleIntArrayObject == null) {
            return null;
        }
        return (int[]) teleIntArrayObject.shallowCopy();
    }

    /**
     * @see TargetMethod#catchBlockPositions()
     */
    public int[] getCatchBlockPositions() {
        final Reference intArrayReference = teleVM().fields().TargetMethod_catchBlockPositions.readReference(reference());
        final TeleArrayObject teleIntArrayObject = (TeleArrayObject) teleVM().makeTeleObject(intArrayReference);
        if (teleIntArrayObject == null) {
            return null;
        }
        return (int[]) teleIntArrayObject.shallowCopy();
    }

    /**
     * @see TargetMethod#directCallees()
     */
    public Reference[] getDirectCallees() {
        final Reference refArrayReference = teleVM().fields().TargetMethod_directCallees.readReference(reference());
        final TeleArrayObject teleRefArrayObject = (TeleArrayObject) teleVM().makeTeleObject(refArrayReference);
        if (teleRefArrayObject == null) {
            return null;
        }
        final Reference[] classMethodActorReferenceArray = (Reference[]) teleRefArrayObject.shallowCopy();
        assert classMethodActorReferenceArray.length == getNumberOfDirectCalls();
        return classMethodActorReferenceArray;
    }

    public int getNumberOfDirectCalls() {
        final int[] stopPositions = getStopPositions();
        if (stopPositions == null) {
            return 0;
        }
        return stopPositions.length - (getNumberOfIndirectCalls() + getNumberOfSafepoints());
    }

    /**
     * @see TargetMethod#numberOfIndirectCalls()
     */
    public int getNumberOfIndirectCalls() {
        return teleVM().fields().TargetMethod_numberOfIndirectCalls.readInt(reference());
    }

    /**
     * @see TargetMethod#numberOfSafepoints()
     */
    public int getNumberOfSafepoints() {
        return teleVM().fields().TargetMethod_numberOfSafepoints.readInt(reference());
    }

     /**
     * Gets the {@linkplain InlineDataDescriptor inline data descriptors} associated with this target method's code in the {@link TeleVM}
     * encoded as a byte array in the format described {@linkplain InlineDataDescriptor here}.
     *
     * @return null if there are no inline data descriptors associated with this target method's code
     * @see TargetMethod#encodedInlineDataDescriptors()
     */
    public final byte[] getEncodedInlineDataDescriptors() {
        final Reference encodedInlineDataDescriptorsReference = teleVM().fields().TargetMethod_encodedInlineDataDescriptors.readReference(reference());
        final TeleArrayObject teleEncodedInlineDataDescriptors = (TeleArrayObject) teleVM().makeTeleObject(encodedInlineDataDescriptorsReference);
        return teleEncodedInlineDataDescriptors == null ? null : (byte[]) teleEncodedInlineDataDescriptors.shallowCopy();
    }

    private IndexedSequence<TargetJavaFrameDescriptor> javaFrameDescriptors = null;

    public IndexedSequence<TargetJavaFrameDescriptor> getJavaFrameDescriptors() {
        if (javaFrameDescriptors == null) {
            final Reference byteArrayReference = teleVM().fields().TargetMethod_compressedJavaFrameDescriptors.readReference(reference());
            final TeleArrayObject teleByteArrayObject = (TeleArrayObject) teleVM().makeTeleObject(byteArrayReference);
            if (teleByteArrayObject == null) {
                return null;
            }
            final byte[] compressedDescriptors = (byte[]) teleByteArrayObject.shallowCopy();
            try {
                javaFrameDescriptors = TeleClassRegistry.usingTeleClassIDs(new Function<IndexedSequence<TargetJavaFrameDescriptor>>() {
                    public IndexedSequence<TargetJavaFrameDescriptor> call() {
                        return TargetJavaFrameDescriptor.inflate(compressedDescriptors);
                    }
                });
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return javaFrameDescriptors;
    }

    /**
     * Gets the Java frame descriptor corresponding to a given stop index.
     *
     * @param stopIndex a stop index
     * @return the Java frame descriptor corresponding to {@code stopIndex} or null if there is no Java frame descriptor
     *         for {@code stopIndex}
     */
    public TargetJavaFrameDescriptor getJavaFrameDescriptor(int stopIndex) {
        final IndexedSequence<TargetJavaFrameDescriptor> javaFrameDescriptors = getJavaFrameDescriptors();
        if (javaFrameDescriptors != null && stopIndex < javaFrameDescriptors.length()) {
            return javaFrameDescriptors.get(stopIndex);
        }
        return null;
    }

    private TargetABI abi;

    public TargetABI getAbi() {
        if (abi == null) {
            final Reference abiReference = teleVM().fields().TargetMethod_abi.readReference(reference());
            final TeleObject teleTargetABI = teleVM().makeTeleObject(abiReference);
            if (teleTargetABI != null) {
                abi = (TargetABI) teleTargetABI.deepCopy();
            }
        }
        return abi;
    }

    /**
     * @see TargetMethod#traceBundle(IndentWriter)
     */
    public final void traceBundle(IndentWriter writer) {
        traceExceptionHandlers(writer);
        traceDirectCallees(writer);
        traceFrameDescriptors(writer);
    }

    /**
     * Traces the exception handlers of the compiled code represented by this object.
     *
     * @see TargetMethod#traceExceptionHandlers(IndentWriter)
     */
    public final void traceExceptionHandlers(IndentWriter writer) {
        final int[] catchRangePositions = getCatchRangePositions();
        if (catchRangePositions != null) {
            final int[] catchBlockPositions = getCatchBlockPositions();
            assert catchBlockPositions != null;
            writer.println("Catches: ");
            writer.indent();
            for (int i = 0; i < catchRangePositions.length; i++) {
                if (catchBlockPositions[i] != 0) {
                    final int catchRangeEnd = (i == catchRangePositions.length - 1) ? getCodeLength() : catchRangePositions[i + 1];
                    final int catchRangeStart = catchRangePositions[i];
                    writer.println("[" + catchRangeStart + " .. " + catchRangeEnd + ") -> " + catchBlockPositions[i]);
                }
            }
            writer.outdent();
        }
    }

    /**
     * Traces the {@linkplain #directCallees() direct callees} of the compiled code represented by this object.
     *
     * @see TargetMethod#traceDirectCallees(IndentWriter)
     */
    public final void traceDirectCallees(IndentWriter writer) {
        final Reference[] directCallees = getDirectCallees();
        if (directCallees != null) {
            assert stopPositions != null && directCallees.length <= getNumberOfStopPositions();
            writer.println("Direct Calls: ");
            writer.indent();
            for (int i = 0; i < directCallees.length; i++) {
                final Reference classMethodActorReference = directCallees[i];
                final TeleObject teleObject = teleVM().makeTeleObject(classMethodActorReference);
                if (teleObject instanceof TeleTargetMethod) {
                    writer.println("TeleTargetMethod");
                } else {
                    assert teleObject instanceof TeleClassMethodActor;
                    final TeleClassMethodActor teleClassMethodActor = (TeleClassMethodActor) teleObject;
                    final String calleeName = teleClassMethodActor == null ? "<unknown>" :  teleClassMethodActor.classMethodActor().format("%r %n(%p)" + " in %H");
                    writer.println(getStopPositions()[i] + " -> " + calleeName);
                }
            }
            writer.outdent();
        }
    }

    /**
     * Traces the {@linkplain #compressedJavaFrameDescriptors() frame descriptors} for the compiled code represented by this object in the {@link TeleVM}.
     *
     * @see TargetMethod#traceFrameDescriptors(IndentWriter)
     */
    public final void traceFrameDescriptors(IndentWriter writer) {
        final IndexedSequence<TargetJavaFrameDescriptor> javaFrameDescriptors = getJavaFrameDescriptors();
        if (javaFrameDescriptors != null) {
            writer.println("Frame Descriptors: ");
            writer.indent();
            for (int stopIndex = 0; stopIndex < javaFrameDescriptors.length(); ++stopIndex) {
                final TargetJavaFrameDescriptor frameDescriptor = javaFrameDescriptors.get(stopIndex);
                final int stopPosition = getStopPositions()[stopIndex];
                writer.println(stopPosition + ": " + frameDescriptor);
            }
            writer.outdent();
        }
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
        return teleVM().makeTargetBreakpoint(callEntryPoint);
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
                    teleVM().makeTargetBreakpoint(targetCodeInstruction.address);
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
                    final TeleTargetBreakpoint targetBreakpoint = teleVM().getTargetBreakpoint(targetCodeInstruction.address);
                    if (targetBreakpoint != null) {
                        targetBreakpoint.remove();
                    }
                }
            }
        }
    }


    /**
     * Speeds up repeated copying. This is safe as long as TargetMethods are immutable and don't move.
     */
    private static final Map<TeleTargetMethod, TargetMethod> teleTargetMethodToTargetMethod = new HashMap<TeleTargetMethod, TargetMethod>();

    /**
     * Gets a special, reduced shallow copy, (newly created if not in cache)  that excludes the
     * {@linkplain TargetMethod#scalarLiterals()} or {@linkplain TargetMethod#referenceLiterals() reference}
     * literals of the method or its {@linkplain TargetMethod#code() compiled code}.
     */
    public synchronized TargetMethod reducedShallowCopy() {
        TargetMethod targetMethod = teleTargetMethodToTargetMethod.get(this);
        if (targetMethod == null) {
            targetMethod = (TargetMethod) deepCopy(new OmittedTargetMethodFields(teleVM()));
            teleTargetMethodToTargetMethod.put(this, targetMethod);
        }
        return targetMethod;
    }

    private static class OmittedTargetMethodFields implements FieldIncludeChecker {
        public OmittedTargetMethodFields(TeleVM teleVM) {
            scalarLiterals = teleVM.fields().TargetMethod_scalarLiterals.fieldActor();
            referenceLiterals = teleVM.fields().TargetMethod_referenceLiterals.fieldActor();
        }
        private final FieldActor scalarLiterals;
        private final FieldActor referenceLiterals;

        public boolean include(int level, FieldActor fieldActor) {
            return !(fieldActor.equals(referenceLiterals) || fieldActor.equals(scalarLiterals));
        }
    }

    @Override
    public ReferenceTypeProvider getReferenceType() {
        return teleVM().vmAccess().getReferenceType(getClass());
    }
}
