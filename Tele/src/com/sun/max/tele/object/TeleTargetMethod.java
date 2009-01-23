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

import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.type.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
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
            final Reference targetMethodReference = teleVM.methods().Code_codePointerToTargetMethod.interpret(new WordValue(address)).asReference();
            // Possible that the address points to an unallocated area of a code region.
            if (targetMethodReference != null && !targetMethodReference.isZero()) {
                teleTargetMethod = (TeleTargetMethod) teleVM.makeTeleObject(targetMethodReference);  // Constructor will add to register.
            }
        }
        return teleTargetMethod;
    }

    /**
     * @return  local surrogates for all {@link TargetMethod}s in the tele VM that match the specified key.
     */
    public static Sequence<TeleTargetMethod> get(TeleVM teleVM, MethodKey methodKey) {
        final AppendableSequence<TeleTargetMethod> result = new LinkSequence<TeleTargetMethod>();
        final Reference targetMethodArrayReference = teleVM.methods().Code_methodKeyToTargetMethods.interpret(TeleReferenceValue.from(methodKey)).asReference();
        final TeleArrayObject teleTargetMethodArrayObject = (TeleArrayObject) teleVM.makeTeleObject(targetMethodArrayReference);
        if (teleTargetMethodArrayObject != null) {
            for (Reference targetMethodReference : (Reference []) teleTargetMethodArrayObject.shallowCopy()) {
                final TeleTargetMethod teleTargetMethod = (TeleTargetMethod) teleVM.makeTeleObject(targetMethodReference);
                result.append(teleTargetMethod);
            }
        }
        return result;
    }

    private final TargetCodeRegion _targetCodeRegion;

    public TargetCodeRegion targetCodeRegion() {
        return _targetCodeRegion;
    }

    private Pointer _codeStart = Pointer.zero();

    public Pointer codeStart() {
        if (_codeStart.isZero()) {
            _codeStart = teleVM().fields().TargetMethod_codeStart.readWord(reference()).asPointer();
        }
        return _codeStart;
    }

    public String name() {
        return getClass().getSimpleName() + " for " + classMethodActor().simpleName();
    }

    public int numberOfDirectCalls() {
        final int[] stopPositions = getStopPositions();
        if (stopPositions == null) {
            return 0;
        }
        return stopPositions.length - (numberOfIndirectCalls() + numberOfSafepoints());
    }

    public int numberOfIndirectCalls() {
        return teleVM().fields().TargetMethod_numberOfIndirectCalls.readInt(reference());
    }

    public int numberOfSafepoints() {
        return teleVM().fields().TargetMethod_numberOfSafepoints.readInt(reference());
    }

    private TeleClassMethodActor _teleClassMethodActor;

    /**
     * Gets the tele class method actor for this target method.
     *
     * @return {@code null} if the class method actor is null the target method
     */
    public TeleClassMethodActor getTeleClassMethodActor() {
        if (_teleClassMethodActor == null) {
            final Reference classMethodActorReference = teleVM().fields().TargetMethod_classMethodActor.readReference(reference());
            _teleClassMethodActor = (TeleClassMethodActor) teleVM().makeTeleObject(classMethodActorReference);
        }
        return _teleClassMethodActor;
    }

    public TeleRoutine teleRoutine() {
        return getTeleClassMethodActor();
    }

    TeleTargetMethod(TeleVM teleVM, Reference targetMethodReference) {
        super(teleVM, targetMethodReference);
        // Exception to the general policy of not performing VM i/o during object
        // construction.  This is needed for the code registry.
        // A consequence is synchronized call to the registry from within a synchronized call to {@link TeleObject} construction.
        _targetCodeRegion = new TargetCodeRegion(this, start(), size());
        // Register every method compilation, so that they can be located by code address.
        teleVM.registerTeleTargetRoutine(this);
    }

    /**
     * Gets the entry point for this method as specified by the ABI in use when this target method was compiled.
     *
     * @return {@link Address#zero()} if this target method has not yet been compiled
     */
    public Address callEntryPoint() {
        final Pointer codeStart = codeStart();
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

    private IndexedSequence<TargetCodeInstruction> _instructions;

    public IndexedSequence<TargetCodeInstruction> getInstructions() {
        if (_instructions == null) {
            final Reference codeReference = teleVM().fields().TargetMethod_code.readReference(reference());
            if (!codeReference.isZero()) {
                final TeleArrayObject teleCode = (TeleArrayObject) teleVM().makeTeleObject(codeReference);
                final byte[] code = (byte[]) teleCode.shallowCopy();
                final Reference encodedInlineDataDescriptorsReference = teleVM().fields().TargetMethod_encodedInlineDataDescriptors.readReference(reference());
                final TeleArrayObject teleEncodedInlineDataDescriptors = (TeleArrayObject) teleVM().makeTeleObject(encodedInlineDataDescriptorsReference);
                final byte[] encodedInlineDataDescriptors = teleEncodedInlineDataDescriptors == null ? null : (byte[]) teleEncodedInlineDataDescriptors.shallowCopy();

                _instructions = TeleDisassembler.decode(teleVM(), codeStart(), code, encodedInlineDataDescriptors);
            }
        }
        return _instructions;
    }


    // [tw] Warning: duplicated code!
    @Override
    public MachineCodeInstructionArray getTargetCodeInstructions() {
        final IndexedSequence<TargetCodeInstruction> instructions = getInstructions();
        final MachineCodeInstruction[] result = new MachineCodeInstruction[instructions.length()];
        for (int i = 0; i < result.length; i++) {
            final TargetCodeInstruction ins = instructions.get(i);
            result[i] = new MachineCodeInstruction(ins.getMnemonic(), ins.getPosition(), ins.getAddress(), ins.getLabel(), ins.getBytes(), ins.getOperands(), ins.getTargetAddress());
        }
        return new MachineCodeInstructionArray(result);
    }

    @Override
    public MethodProvider getMethodProvider() {
        return this._teleClassMethodActor;
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
                if (targetCodeInstruction.label() != null) {
                    teleVM().makeTargetBreakpoint(targetCodeInstruction.address());
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
                if (targetCodeInstruction.label() != null) {
                    teleVM().removeTargetBreakpoint(targetCodeInstruction.address());
                }
            }
        }
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

    private int[] _stopPositions;

    public int[] getStopPositions() {
        if (_stopPositions == null) {
            final Reference intArrayReference = teleVM().fields().TargetMethod_stopPositions.readReference(reference());
            final TeleArrayObject teleIntArrayObject = (TeleArrayObject) teleVM().makeTeleObject(intArrayReference);
            if (teleIntArrayObject == null) {
                return null;
            }
            _stopPositions = (int[]) teleIntArrayObject.shallowCopy();

            // Since only the VM's deoptimization algorithm cares about these flags, we omit them here:
            for (int i = 0; i < _stopPositions.length; i++) {
                _stopPositions[i] &= ~TargetMethod.REFERENCE_RETURN_FLAG;
            }
        }
        return _stopPositions;
    }

    public int getJavaStopIndex(Address address) {
        final int[] stopPositions = getStopPositions();
        if (stopPositions != null) {
            final int targetCodePosition = address.minus(codeStart()).toInt();
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

    private IndexedSequence<TargetJavaFrameDescriptor> _javaFrameDescriptors;

    /**
     * Gets the Java frame descriptor corresponding to a given stop index.
     *
     * @param stopIndex a stop index
     * @return the Java frame descriptor corresponding to {@code stopIndex} or null if there is no Java frame descriptor
     *         for {@code stopIndex}
     */
    public TargetJavaFrameDescriptor getJavaFrameDescriptor(int stopIndex) {
        if (_javaFrameDescriptors == null) {
            final Reference byteArrayReference = teleVM().fields().TargetMethod_compressedJavaFrameDescriptors.readReference(reference());
            final TeleArrayObject teleByteArrayObject = (TeleArrayObject) teleVM().makeTeleObject(byteArrayReference);
            if (teleByteArrayObject == null) {
                return null;
            }
            final byte[] compressedDescriptors = (byte[]) teleByteArrayObject.shallowCopy();
            try {
                _javaFrameDescriptors = TeleClassRegistry.usingTeleClassIDs(new Function<IndexedSequence<TargetJavaFrameDescriptor>>() {
                    public IndexedSequence<TargetJavaFrameDescriptor> call() {
                        return TargetJavaFrameDescriptor.inflate(compressedDescriptors);
                    }
                });
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        if (_javaFrameDescriptors != null && stopIndex < _javaFrameDescriptors.length()) {
            return _javaFrameDescriptors.get(stopIndex);
        }
        return null;
    }

    private TargetABI _abi;

    public TargetABI getAbi() {
        if (_abi == null) {
            final Reference abiReference = teleVM().fields().TargetMethod_abi.readReference(reference());
            final TeleObject teleTargetABI = teleVM().makeTeleObject(abiReference);
            if (teleTargetABI != null) {
                _abi = (TargetABI) teleTargetABI.deepCopy();
            }
        }
        return _abi;
    }

    /**
     * Speeds up repeated copying. This is safe as long as TargetMethods are immutable and don't move.
     */
    private static final Map<TeleTargetMethod, TargetMethod> _teleTargetMethodToTargetMethod = new HashMap<TeleTargetMethod, TargetMethod>();

    /**
     * Gets a special, reduced shallow copy, (newly created if not in cache)  that excludes the
     * {@linkplain TargetMethod#scalarLiteralBytes()} or {@linkplain TargetMethod#referenceLiterals() reference}
     * literals of the method or its {@linkplain TargetMethod#code() compiled code}.
     */
    public synchronized TargetMethod reducedShallowCopy() {
        TargetMethod targetMethod = _teleTargetMethodToTargetMethod.get(this);
        if (targetMethod == null) {
            targetMethod = (TargetMethod) deepCopy(new OmittedTargetMethodFields());
            _teleTargetMethodToTargetMethod.put(this, targetMethod);
        }
        return targetMethod;
    }

    private static class OmittedTargetMethodFields implements FieldIncludeChecker {
        private static final FieldActor _scalarLiteralBytes = TeleFieldAccess.findFieldActor(TargetMethod.class, "_scalarLiteralBytes");
        private static final FieldActor _referenceLiterals = TeleFieldAccess.findFieldActor(TargetMethod.class, "_referenceLiterals");

        public boolean include(int level, FieldActor fieldActor) {
            if (fieldActor.equals(_referenceLiterals) || fieldActor.equals(_scalarLiteralBytes)) {
                return false;
            }
            return true;
        }
    }

    @Override
    public ReferenceTypeProvider getReferenceType() {
        return teleVM().vmAccess().getReferenceType(getClass());
    }
}
