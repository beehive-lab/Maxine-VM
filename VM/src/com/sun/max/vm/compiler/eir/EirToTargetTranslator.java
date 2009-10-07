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

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.profile.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.eir.EirTargetEmitter.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.tele.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class EirToTargetTranslator extends TargetGenerator {

    private final int registerReferenceMapSize;

    protected EirToTargetTranslator(TargetGeneratorScheme targetGeneratorScheme, InstructionSet instructionSet, int registerReferenceMapSize) {
        super(targetGeneratorScheme, instructionSet);
        this.registerReferenceMapSize = registerReferenceMapSize;
    }

    private Address fixLiteralLabels(EirTargetEmitter emitter, Sequence<EirLiteral> literals, Address address) {
        Address a = address;
        for (EirLiteral literal : literals) {
            emitter.fixLabel(literal.asLabel(), a);
            a = a.plus(literal.value().kind().width.numberOfBytes);
        }
        return a;
    }

    private byte[] packScalarLiteralBytes(Sequence<EirLiteral> scalarLiterals, DataModel dataModel) {
        if (scalarLiterals.isEmpty()) {
            return null;
        }
        final EirLiteral lastLiteral = scalarLiterals.last();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream(lastLiteral.index() + lastLiteral.value().kind().width.numberOfBytes);
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

    private void encodeExtraStopPositionInfo(int[] stopPositions, EirTargetEmitter<?> emitter) {
        if (stopPositions != null) {
            try {
                if (!emitter.extraCallInfos.isEmpty()) {
                    final IntHashMap<Integer> map = new IntHashMap<Integer>();
                    for (int stopIndex = 0; stopIndex < stopPositions.length; ++stopIndex) {
                        map.put(stopPositions[stopIndex], stopIndex);
                    }

                    for (ExtraCallInfo extraCallInfo : emitter.extraCallInfos) {
                        final int stopIndex = map.get(extraCallInfo.label.position());
                        if (extraCallInfo.isNativeFunctionCall) {
                            stopPositions[stopIndex] |= StopPositions.NATIVE_FUNCTION_CALL;
                        }
                    }
                }
            } catch (AssemblyException assemblyException) {
                ProgramError.unexpected();
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
            stop.addFrameReferenceMap(stackSlotWidth, bitMap);
            bitMap.next();
        }
    }

    private byte[] packReferenceMaps(int referenceMapsSize, EirTargetEmitter<?> emitter, int frameReferenceMapSize, int regReferenceMapSize) {
        if (referenceMapsSize == 0) {
            return null;
        }
        final byte[] referenceMaps = new byte[referenceMapsSize];
        final WordWidth stackSlotWidth = emitter.stackSlotWidth();
        final ByteArrayBitMap bitMap = new ByteArrayBitMap(referenceMaps, 0, frameReferenceMapSize);
        if (frameReferenceMapSize > 0) {
            addStackReferenceMaps(emitter.directCalls(), stackSlotWidth, bitMap);
            addStackReferenceMaps(emitter.indirectCalls(), stackSlotWidth, bitMap);
            addStackReferenceMaps(emitter.safepoints(), stackSlotWidth, bitMap);
        }
        bitMap.setSize(regReferenceMapSize);
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
        final CPSTargetMethod targetMethod = createIrMethod(eirMethod.classMethodActor());
        generateTarget(targetMethod, eirMethod);
        return targetMethod;
    }

    private static final TimerMetric timer = GlobalMetrics.newTimer("Translate-EirToTarget", Clock.SYSTEM_MILLISECONDS);

    @Override
    protected void generateIrMethod(CPSTargetMethod targetMethod) {
        final EirGeneratorScheme eirGeneratorScheme = (EirGeneratorScheme) compilerScheme();
        final EirGenerator<?> eirGenerator = eirGeneratorScheme.eirGenerator();
        final EirMethod eirMethod = eirGenerator.makeIrMethod(targetMethod.classMethodActor());

        timer.start();
        generateTarget(targetMethod, eirMethod);
        timer.stop();
    }

    private void generateTarget(CPSTargetMethod targetMethod, final EirMethod eirMethod) throws ProgramError {
        final EirTargetEmitter<?> emitter = createEirTargetEmitter(eirMethod);
        emitter.emitFrameAdapterPrologue();
        eirMethod.emit(emitter);
        emitter.emitFrameAdapterEpilogue();

        final DataModel dataModel = compilerScheme().vmConfiguration().platform().processorKind.dataModel;

        final Sequence<EirLiteral> scalarLiterals = eirMethod.literalPool().scalarLiterals();
        final Sequence<EirLiteral> referenceLiterals = eirMethod.literalPool().referenceLiterals();

        final byte[] scalarLiteralBytes = packScalarLiteralBytes(scalarLiterals, dataModel);
        final Object[] referenceLiteralObjects = packReferenceLiterals(referenceLiterals);

        final int numberOfFrameSlots = Unsigned.idiv(Ints.roundUnsignedUpByPowerOfTwo(eirMethod.frameSize(), STACK_SLOT_SIZE), STACK_SLOT_SIZE);
        final int frameReferenceMapSize = ByteArrayBitMap.computeBitMapSize(numberOfFrameSlots);

        final int placeholderCodeLength = 0;
        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(
                        (scalarLiteralBytes == null) ? 0 : scalarLiteralBytes.length,
                        (referenceLiteralObjects == null) ? 0 : referenceLiteralObjects.length,
                        placeholderCodeLength);

        if (!scalarLiterals.isEmpty()) {
            fixLiteralLabels(emitter, scalarLiterals, targetBundleLayout.firstElementPointer(Address.zero(), ArrayField.scalarLiterals));
        }
        if (!referenceLiterals.isEmpty()) {
            fixLiteralLabels(emitter, referenceLiterals, targetBundleLayout.firstElementPointer(Address.zero(), ArrayField.referenceLiterals));
        }

        final byte[] code;
        try {
            emitter.setStartAddress(targetBundleLayout.firstElementPointer(Address.zero(), ArrayField.code));
            code = emitter.toByteArray();
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected("assembling failed", assemblyException);
        }

        targetBundleLayout.update(ArrayField.code, code.length);
        Code.allocate(targetBundleLayout, targetMethod);

        final Sequence<EirCall> directCalls = emitter.directCalls();
        final int[] stopPositions = packLabelPositions(emitter.directCallLabels(), emitter.indirectCallLabels(), emitter.safepointLabels());
        encodeExtraStopPositionInfo(stopPositions, emitter);
        final byte[] compressedJavaFrameDescriptors = targetMethod.classMethodActor().isTemplate() ? null : emitter.getCompressedJavaFrameDescriptors();

        final int numberOfDirectCalls = emitter.directCallLabels().length();
        final int numberOfIndirectCalls = emitter.indirectCallLabels().length();
        final int numberOfSafepoints = emitter.safepointLabels().length();
        final int registerReferenceMapSize = targetMethod.registerReferenceMapSize();
        int referenceMapsSize = CPSTargetMethod.computeReferenceMapsSize(numberOfDirectCalls, numberOfIndirectCalls, numberOfSafepoints, frameReferenceMapSize, registerReferenceMapSize);

        targetMethod.setGenerated(
                        packLabelPositions(emitter.catchRangeLabels()),
                        packCatchBlockPositions(emitter.catchBlocks()),
                        stopPositions,
                        compressedJavaFrameDescriptors,
                        packDirectCallees(directCalls),
                        emitter.indirectCallLabels().length(),
                        emitter.safepointLabels().length(),
                        packReferenceMaps(referenceMapsSize, emitter, frameReferenceMapSize, registerReferenceMapSize),
                        scalarLiteralBytes,
                        referenceLiteralObjects,
                        code,
                        emitter.inlineDataRecorder().encodedDescriptors(),
                        eirMethod.frameSize(),
                        frameReferenceMapSize, eirMethod.abi.targetABI()
        );
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
