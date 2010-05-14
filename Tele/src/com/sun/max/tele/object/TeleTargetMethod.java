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
import com.sun.max.io.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.BytecodeLocation;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;

/**
 * Canonical surrogate for several possible kinds of compilation of a Java {@link ClassMethod} in the VM.
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
        ProgramError.check(!address.isZero());
        if (!teleVM.isBootImageRelocated()) {
            return null;
        }
        TeleTargetMethod teleTargetMethod = teleVM.findTeleTargetRoutine(TeleTargetMethod.class, address);
        if (teleTargetMethod == null
                        && teleVM.findTeleTargetRoutine(TeleTargetRoutine.class, address) == null
                        && teleVM.codeCache().contains(address)) {
            // Not a known Java target method, and not some other kind of known target code, but in a code region
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
     * @return local surrogates for all {@link TargetMethod}s in the VM that include code compiled for the
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
        targetCodeRegion = new MethodTargetCodeRegion(teleVM, this);
        // Initializes the start and size fields via a refresh so that the following
        // call to register the target method gets the correct values for placing
        // the target method in a sorted list
        refresh();
        // Register every method compilation, so that they can be located by code address.
        teleVM.registerTeleTargetRoutine(this);
    }

    private CodeLocation codeStartLocation = null;
    /**
     * @see TargetMethod#codeStart()
     */
    public Pointer getCodeStart() {
        return targetMethod().codeStart();
    }

    /**
     * @see TargetMethod#codeStart()
     */
    public CodeLocation getCodeStartLocation() {
        if (codeStartLocation == null && getCodeStart() != null) {
            codeStartLocation = codeManager().createMachineCodeLocation(getCodeStart(), "code start location in method");
        }
        return codeStartLocation;
    }

    public String getName() {
        String name;
        ClassMethodActor classMethodActor = classMethodActor();
        if (classMethodActor == null) {
            Reference nameReference = vm().teleFields().RuntimeMemoryRegion_regionName.readReference(reference());
            if (!nameReference.isZero()) {
                name = vm().getString(nameReference);
            } else {
                name = "???";
            }
        } else {
            name = classMethodActor.simpleName();
        }
        return getClass().getSimpleName() + " for " + name;
    }

    private TeleClassMethodActor teleClassMethodActor;

    /**
     * Gets the tele class method actor for this target method.
     *
     * @return {@code null} if the class method actor is null the target method
     */
    public TeleClassMethodActor getTeleClassMethodActor() {
        if (teleClassMethodActor == null) {
            final Reference classMethodActorReference = vm().teleFields().TargetMethod_classMethodActor.readReference(reference());
            if (!classMethodActorReference.isZero()) {
                teleClassMethodActor = (TeleClassMethodActor) vm().makeTeleObject(classMethodActorReference);
            }
        }
        return teleClassMethodActor;
    }

    public TeleRoutine teleRoutine() {
        TeleClassMethodActor teleClassMethodActor = getTeleClassMethodActor();
        if (teleClassMethodActor == null) {
            return new TeleRoutine() {
                public String getUniqueName() {
                    return getName();
                }
            };
        }

        return teleClassMethodActor;
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
        final Reference callEntryPointReference = TeleInstanceReferenceFieldAccess.readPath(reference(), vm().teleFields().TargetMethod_abi, vm().teleFields().TargetABI_callEntryPoint);
        final TeleObject teleCallEntryPoint = vm().makeTeleObject(callEntryPointReference);
        if (teleCallEntryPoint == null) {
            return codeStart;
        }
        final CallEntryPoint callEntryPoint = (CallEntryPoint) teleCallEntryPoint.deepCopy();
        return codeStart.plus(callEntryPoint.offset());
    }

    /**
     * Gets the compiled entry point location for this method as specified by the ABI in use when this target method was compiled.
     *
     * @return {@link Address#zero()} if this target method has not yet been compiled
     */
    public final CodeLocation callEntryLocation() {
        final Pointer codeStart = getCodeStart();
        if (codeStart.isZero()) {
            return null;
        }
        final Reference callEntryPointReference = TeleInstanceReferenceFieldAccess.readPath(reference(), vm().teleFields().TargetMethod_abi, vm().teleFields().TargetABI_callEntryPoint);
        final TeleObject teleCallEntryPoint = vm().makeTeleObject(callEntryPointReference);
        if (teleCallEntryPoint == null) {
            return getCodeStartLocation();
        }
        final CallEntryPoint callEntryPoint = (CallEntryPoint) teleCallEntryPoint.deepCopy();
        final Pointer callEntryPointer = codeStart.plus(callEntryPoint.offset());
        return codeManager().createMachineCodeLocation(callEntryPointer, "call entry");
    }

    /**
     * Gets the byte array containing the target-specific machine code of this target method in the VM.
     * @see TargetMethod#code()
     */
    public final byte[] getCode() {
        return targetMethod().code();
    }

    /**
     * Gets the length of the byte array containing the target-specific machine code of this target method in the VM.
     * @see TargetMethod#codeLength()
     */
    public final int getCodeLength() {
        final Reference codeReference = vm().teleFields().TargetMethod_code.readReference(reference());
        if (codeReference.isZero()) {
            return 0;
        }
        final TeleArrayObject teleCodeArrayObject = (TeleArrayObject) vm().makeTeleObject(codeReference);
        return teleCodeArrayObject.getLength();
    }

    private IndexedSequence<TargetCodeInstruction> instructions;
    private IndexedSequence<MachineCodeLocation> instructionLocations;

    public final  IndexedSequence<TargetCodeInstruction> getInstructions() {
        if (instructions == null) {
            final byte[] code = getCode();
            if (code != null) {
                instructions = TeleDisassembler.decode(vm().vmConfiguration().platform().processorKind, getCodeStart(), code, targetMethod().encodedInlineDataDescriptors());
            }
        }
        return instructions;
    }

    public IndexedSequence<MachineCodeLocation> getInstructionLocations() {
        if (instructionLocations == null) {
            getInstructions();
            final int length = instructions.length();
            final CodeManager codeManager = codeManager();
            final VariableSequence<MachineCodeLocation> locations = new VectorSequence<MachineCodeLocation>(length);
            for (int i = 0; i < length; i++) {
                locations.append(codeManager.createMachineCodeLocation(instructions.get(i).address, "native target code instruction"));
            }
            instructionLocations = locations;
        }
        return instructionLocations;
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
            if (targetMethod().stopPositions() != null) {
                stopPositions = new StopPositions(targetMethod().stopPositions());
            }
        }
        return stopPositions;
    }

    /**
     * @see TargetMethod#numberOfStopPositions()
     */
    public int getNumberOfStopPositions() {
        return targetMethod().numberOfStopPositions();
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
            final int numberOfCalls = targetMethod().numberOfDirectCalls() + targetMethod().numberOfIndirectCalls();
            for (int i = 0; i < numberOfCalls; i++) {
                final int stopPosition = stopPositions.get(i);
                if (stopPosition > position && (nextCallPosition == -1 || stopPosition < nextCallPosition)) {
                    nextCallPosition = stopPosition;
                }
            }
        }
        return nextCallPosition;
    }

    public MaxCodeLocation getNextCallLocation(MaxCodeLocation maxCodeLocation) {
        assert maxCodeLocation.hasAddress();
        final int targetCodePosition = maxCodeLocation.address().minus(getCodeStart()).toInt();
        final int nextCallPosition = getNextCallPosition(targetCodePosition);
        if (nextCallPosition > 0) {
            return codeManager().createMachineCodeLocation(getCodeStart().plus(nextCallPosition), "next call location");
        }
        return null;
    }

    /**
     * Gets the Java frame descriptor corresponding to a given stop index.
     *
     * @param stopIndex a stop index
     * @return the Java frame descriptor corresponding to {@code stopIndex} or null if there is no Java frame descriptor
     *         for {@code stopIndex}
     */
    public BytecodeLocation getBytecodeLocation(int stopIndex) {
        TargetMethod targetMethod = targetMethod();
        return targetMethod.getBytecodeLocationFor(stopIndex);
    }

    private TargetABI abi;

    private static final Map<TeleObject, TargetABI> abiCache = new HashMap<TeleObject, TargetABI>();

    public TargetABI getAbi() {
        if (abi == null) {
            final Reference abiReference = vm().teleFields().TargetMethod_abi.readReference(reference());
            final TeleObject teleTargetABI = vm().makeTeleObject(abiReference);
            if (teleTargetABI != null) {
                synchronized (abiCache) {
                    abi = abiCache.get(teleTargetABI);
                    if (abi == null) {
                        abi = (TargetABI) teleTargetABI.deepCopy();
                        abiCache.put(teleTargetABI, abi);
                    }
                }
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

    public CodeLocation entryLocation() {
        final Address callEntryPoint = callEntryPoint();
        if (callEntryPoint.isZero()) {
            return null;
        }
        return codeManager().createMachineCodeLocation(callEntryPoint, "Method entry");
    }

    public Sequence<MaxCodeLocation> labelLocations() {
        final AppendableSequence<MaxCodeLocation> locations = new ArrayListSequence<MaxCodeLocation>();
        for (TargetCodeInstruction targetCodeInstruction : getInstructions()) {
            if (targetCodeInstruction.label != null) {
                final String description = "Label " + targetCodeInstruction.label.toString() + " in " + getName();
                locations.append(codeManager().createMachineCodeLocation(targetCodeInstruction.address, description));
            }
        }
        return locations;
    }

    /**
     * Cache for {@link #targetMethod()}.
     */
    private TargetMethod targetMethod;

    public TargetMethod targetMethod() {
        if (targetMethod == null) {
            targetMethod = (TargetMethod) deepCopy();
        }
        return targetMethod;
    }

    @Override
    protected DeepCopier newDeepCopier() {
        return new ReducedDeepCopier();
    }

    /**
     * A copier to be used for implementing {@link TeleTargetMethod#targetMethod()}.
     */
    class ReducedDeepCopier extends DeepCopier {
        public ReducedDeepCopier() {
            TeleFields teleFields = vm().teleFields();
            omit(teleFields.TargetMethod_scalarLiterals.fieldActor());
            omit(teleFields.TargetMethod_referenceLiterals.fieldActor());
            abi = teleFields.TargetMethod_abi.fieldActor();
            generator = teleFields.Adapter_generator.fieldActor();
        }
        private final FieldActor abi;
        private final FieldActor generator;

        @Override
        protected Object makeDeepCopy(FieldActor fieldActor, TeleObject teleObject) {
            if (fieldActor.equals(generator)) {
                return null;
            } else if (fieldActor.equals(abi)) {
                return getAbi();
            } else {
                return super.makeDeepCopy(fieldActor, teleObject);
            }
        }
    }

    @Override
    public ReferenceTypeProvider getReferenceType() {
        return vm().vmAccess().getReferenceType(getClass());
    }

    /**
     * Disassembles this target method's code to a given writer.
     */
    public void disassemble(IndentWriter writer) {
    }
}
