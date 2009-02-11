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
package com.sun.max.vm.compiler.eir;

import static com.sun.max.vm.stack.JavaStackFrameLayout.*;

import java.io.*;
import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.MakeStackVariable.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.tele.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class EirToTargetTranslator extends TargetGenerator {

    private final int _registerReferenceMapSize;

    protected EirToTargetTranslator(TargetGeneratorScheme targetGeneratorScheme, InstructionSet instructionSet, int registerReferenceMapSize) {
        super(targetGeneratorScheme, instructionSet);
        _registerReferenceMapSize = registerReferenceMapSize;
    }

    private Address fixLiteralLabels(EirTargetEmitter emitter, Sequence<EirLiteral> literals, Address address) {
        Address a = address;
        for (EirLiteral literal : literals) {
            emitter.fixLabel(literal.asLabel(), a);
            a = a.plus(literal.value().kind().size());
        }
        return a;
    }

    private byte[] packScalarLiteralBytes(Sequence<EirLiteral> scalarLiterals, DataModel dataModel) {
        if (scalarLiterals.isEmpty()) {
            return null;
        }
        final EirLiteral lastLiteral = scalarLiterals.last();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream(lastLiteral.index() + lastLiteral.value().kind().size());
        for (EirLiteral literal : scalarLiterals) {
            try {
                assert literal.index() == stream.size();
                dataModel.write(stream, literal.value());
            } catch (IOException ioException) {
                ProgramError.unexpected();
            }
        }
        return stream.toByteArray();
    }

    private Object[] packReferenceLiterals(Sequence<EirLiteral> referenceLiterals) {
        if (referenceLiterals.isEmpty()) {
            return null;
        }
        final Object[] result = new Object[referenceLiterals.length()];
        int i = 0;
        for (EirLiteral referenceLiteral : referenceLiterals) {
            if (MaxineVM.isPrototyping()) {
                result[i] = referenceLiteral.value().asObject();
            } else {
                // Must not cause checkcast here, since some reference literals may be static tuples.
                ArrayAccess.setObject(result, i, referenceLiteral.value().asObject());
            }
            i++;
        }
        return result;
    }

    private int[] packLabelPositions(Sequence... labelSequences) {
        int n = 0;
        for (Sequence labels : labelSequences) {
            n += labels.length();
        }
        if (n == 0) {
            return null;
        }
        final int[] positions = new int[n];
        int i = 0;
        for (Sequence<Label> labels : labelSequences) {
            try {
                for (Label label : labels) {
                    positions[i] = label.position();
                    i++;
                }
            } catch (AssemblyException assemblyException) {
                ProgramError.unexpected();
            }
        }
        return positions;
    }

    private void markReferenceCalls(int[] stopPositions, EirTargetEmitter<?> emitter) {
        if (stopPositions != null) {
            for (int i = 0; i < stopPositions.length; i++) {
                if (emitter.isReferenceCall(i)) {
                    stopPositions[i] |= TargetMethod.REFERENCE_RETURN_FLAG;
                }
            }
        }
    }

    private int[] packCatchBlockPositions(IndexedSequence<EirBlock> eirBlocks) {
        if (eirBlocks.isEmpty()) {
            return null;
        }
        final int[] positions = new int[eirBlocks.length()];
        try {
            for (int i = 0; i < eirBlocks.length(); i++) {
                final EirBlock eirBlock = eirBlocks.get(i);
                if (eirBlock == null) {
                    positions[i] = 0; // indicate non-catching by an impossible value for a catch block position
                } else {
                    positions[i] = eirBlock.asLabel().position();
                }
            }
        } catch (AssemblyException assemblyException) {
            ProgramError.unexpected();
        }
        return positions;
    }

    private ClassMethodActor[] packDirectCallees(Sequence<EirCall> directCalls) {
        if (directCalls.isEmpty()) {
            return null;
        }
        final ClassMethodActor[] callees = new ClassMethodActor[directCalls.length()];
        int i = 0;
        for (EirCall call : directCalls) {
            final EirMethodValue methodValue =  (EirMethodValue) call.function().eirValue();
            callees[i++] = methodValue.classMethodActor();
        }
        return callees;
    }

    private void addStackReferenceMaps(Iterable<? extends EirStop> stops, WordWidth stackSlotWidth, ByteArrayBitMap bitMap) {
        for (EirStop stop : stops) {
            stop.addStackReferenceMap(stackSlotWidth, bitMap);
            bitMap.next();
        }
    }

    private byte[] packReferenceMaps(TargetBundleLayout targetBundleLayout, EirTargetEmitter<?> emitter, int frameReferenceMapSize, int registerReferenceMapSize) {
        if (targetBundleLayout.cellSize(ArrayField.referenceMaps).isZero()) {
            return null;
        }
        final byte[] referenceMaps = new byte[targetBundleLayout.length(ArrayField.referenceMaps)];
        final WordWidth stackSlotWidth = emitter.stackSlotWidth();
        final ByteArrayBitMap bitMap = new ByteArrayBitMap(referenceMaps, 0, frameReferenceMapSize);
        if (frameReferenceMapSize > 0) {
            addStackReferenceMaps(emitter.directCalls(), stackSlotWidth, bitMap);
            addStackReferenceMaps(emitter.indirectCalls(), stackSlotWidth, bitMap);
            addStackReferenceMaps(emitter.safepoints(), stackSlotWidth, bitMap);
        }
        bitMap.setSize(_registerReferenceMapSize);
        for (EirSafepoint safepoint : emitter.safepoints()) {
            safepoint.addRegisterReferenceMap(bitMap);
            bitMap.next();
        }
        assert bitMap.offset() == referenceMaps.length;
        return referenceMaps;
    }

    private int getMarkerPosition(EirTargetEmitter<?> emitter) {
        if (emitter.markerLabel().state() == Label.State.BOUND) {
            try {
                return emitter.markerLabel().position();
            } catch (AssemblyException e) {
                ProgramError.unexpected();
            }
        }
        return -1;
    }

    protected abstract EirTargetEmitter createEirTargetEmitter(EirMethod eirMethod);

    @Override
    public TargetMethod makeIrMethod(EirMethod eirMethod) {
        final TargetMethod targetMethod = createIrMethod(eirMethod.classMethodActor());
        generateTarget(targetMethod, eirMethod);
        return targetMethod;
    }

    @Override
    protected void generateIrMethod(TargetMethod targetMethod, CompilationDirective compilationDirective) {
        final EirGeneratorScheme eirGeneratorScheme = (EirGeneratorScheme) compilerScheme();
        final EirGenerator<?> eirGenerator = eirGeneratorScheme.eirGenerator();
        final EirMethod eirMethod = eirGenerator.makeIrMethod(targetMethod.classMethodActor());

        generateTarget(targetMethod, eirMethod);
    }

    private void generateTarget(TargetMethod targetMethod, final EirMethod eirMethod) throws ProgramError {
        final EirTargetEmitter<?> emitter = createEirTargetEmitter(eirMethod);
        emitter.emitFrameAdapterPrologue();
        eirMethod.emit(emitter);
        emitter.emitFrameAdapterEpilogue();

        final DataModel dataModel = compilerScheme().vmConfiguration().platform().processorKind().dataModel();

        final Sequence<EirLiteral> scalarLiterals = eirMethod.literalPool().scalarLiterals();
        final Sequence<EirLiteral> referenceLiterals = eirMethod.literalPool().referenceLiterals();

        final byte[] scalarLiteralBytes = packScalarLiteralBytes(scalarLiterals, dataModel);
        final Object[] referenceLiteralObjects = packReferenceLiterals(referenceLiterals);

        final int numberOfFrameSlots = Ints.roundUp(eirMethod.frameSize(), STACK_SLOT_SIZE) / STACK_SLOT_SIZE;
        final int frameReferenceMapSize = ByteArrayBitMap.computeBitMapSize(numberOfFrameSlots);

        final int placeholderCodeLength = 0;
        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(
                        emitter.catchRangeLabels().length(),
                        emitter.directCallLabels().length(),
                        emitter.indirectCallLabels().length(),
                        emitter.safepointLabels().length(),
                        (scalarLiteralBytes == null) ? 0 : scalarLiteralBytes.length,
                        (referenceLiteralObjects == null) ? 0 : referenceLiteralObjects.length,
                        placeholderCodeLength,
                        frameReferenceMapSize,
                        targetMethod.registerReferenceMapSize());

        final TargetBundle canonicalTargetBundle = new TargetBundle(targetBundleLayout, Address.zero());


        if (!scalarLiterals.isEmpty()) {
            fixLiteralLabels(emitter, scalarLiterals, canonicalTargetBundle.firstElementPointer(ArrayField.scalarLiteralBytes));
        }
        if (!referenceLiterals.isEmpty()) {
            fixLiteralLabels(emitter, referenceLiterals, canonicalTargetBundle.firstElementPointer(ArrayField.referenceLiterals));
        }

        final byte[] code;
        try {
            emitter.setStartAddress(canonicalTargetBundle.firstElementPointer(ArrayField.code));
            code = emitter.toByteArray();
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected("assembling failed", assemblyException);
        }

        targetBundleLayout.update(ArrayField.code, code.length);

        targetMethod.setSize(targetBundleLayout.bundleSize());
        Code.allocate(targetMethod);

        final TargetBundle targetBundle = new TargetBundle(targetBundleLayout, targetMethod.start());
        final Sequence<EirCall> directCalls = emitter.directCalls();
        final int[] stopPositions = packLabelPositions(emitter.directCallLabels(), emitter.indirectCallLabels(), emitter.safepointLabels());
        markReferenceCalls(stopPositions, emitter);
        final byte[] compressedJavaFrameDescriptors = targetMethod.classMethodActor().isTemplate() ? null : emitter.getCompressedJavaFrameDescriptors();

        targetMethod.setGenerated(targetBundle,
                        packLabelPositions(emitter.catchRangeLabels()),
                        packCatchBlockPositions(emitter.catchBlocks()),
                        stopPositions,
                        compressedJavaFrameDescriptors,
                        packDirectCallees(directCalls),
                        emitter.indirectCallLabels().length(),
                        emitter.safepointLabels().length(),
                        emitter.guardpoints().length(),
                        packReferenceMaps(targetBundleLayout, emitter, frameReferenceMapSize, targetMethod.registerReferenceMapSize()),
                        scalarLiteralBytes,
                        referenceLiteralObjects,
                        code,
                        emitter.inlineDataRecorder().encodedDescriptors(),
                        eirMethod.frameSize(),
                        frameReferenceMapSize,
                        eirMethod.abi().targetABI(),
                        getMarkerPosition(emitter));
        assert TargetBundleLayout.from(targetMethod).bundleSize().equals(targetBundleLayout.bundleSize()) :
            "computed target bundle size differs from derived target bundle size for " + targetMethod.classMethodActor() +
            "\n    computed:\n" + targetBundleLayout +
            "\n     derived:\n" + TargetBundleLayout.from(targetMethod);
        if (MaxineVM.isPrototyping()) {
            // the compiled prototype links all methods in a separate phase
        } else {
            // at target runtime, each method gets linked individually right after generating it:
            targetMethod.linkDirectCalls();
        }
        BytecodeBreakpointMessage.makeTargetBreakpoints(targetMethod);

        if (MaxineVM.isPrototyping()) {
            for (Map.Entry<StackVariable, Integer> entry : emitter.namedStackVariableOffsets()) {
                entry.getKey().record(targetMethod, entry.getValue());
            }
        }

        eirMethod.cleanupAfterEmitting();
    }

    @Override
    public boolean hasStackParameters(ClassMethodActor classMethodActor) {
        final EirGeneratorScheme eirGeneratorScheme = (EirGeneratorScheme) compilerScheme();
        final EirGenerator<?> eirGenerator = eirGeneratorScheme.eirGenerator();
        final EirABI abi = eirGenerator.eirABIsScheme().getABIFor(classMethodActor);
        for (EirLocation location : abi.getParameterLocations(EirStackSlot.Purpose.PARAMETER, classMethodActor.getParameterKinds())) {
            if (location instanceof EirStackSlot) {
                return true;
            }
        }
        return false;
    }
}
