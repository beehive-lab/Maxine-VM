/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.eir;

import static com.sun.max.vm.stack.VMFrameLayout.*;

import java.io.*;
import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.ArrayField;
import com.sun.max.vm.cps.eir.EirTargetEmitter.ExtraCallInfo;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.object.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class EirToTargetTranslator extends TargetGenerator {

    protected EirToTargetTranslator(TargetGeneratorScheme targetGeneratorScheme, ISA isa, int registerReferenceMapSize) {
        super(targetGeneratorScheme, isa);
    }

    private Address fixLiteralLabels(EirTargetEmitter emitter, Collection<EirLiteral> literals, Address address) {
        Address a = address;
        for (EirLiteral literal : literals) {
            emitter.fixLabel(literal.asLabel(), a);
            a = a.plus(literal.value().kind().width.numberOfBytes);
        }
        return a;
    }

    private byte[] packScalarLiteralBytes(Collection<EirLiteral> scalarLiterals, DataModel dataModel) {
        if (scalarLiterals.isEmpty()) {
            return null;
        }
        final ByteArrayOutputStream stream = new ByteArrayOutputStream(64);
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

    private Object[] packReferenceLiterals(Collection<EirLiteral> referenceLiterals) {
        if (referenceLiterals.isEmpty()) {
            return null;
        }
        final Object[] result = new Object[referenceLiterals.size()];
        int i = 0;
        for (EirLiteral referenceLiteral : referenceLiterals) {
            if (MaxineVM.isHosted()) {
                result[i] = referenceLiteral.value().asObject();
            } else {
                // Must not cause checkcast here, since some reference literals may be static tuples.
                ArrayAccess.setObject(result, i, referenceLiteral.value().asObject());
            }
            i++;
        }
        return result;
    }

    private int[] packLabelPositions(List... labelSequences) {
        int n = 0;
        for (List labels : labelSequences) {
            n += labels.size();
        }
        if (n == 0) {
            return null;
        }
        final int[] positions = new int[n];
        int i = 0;
        for (List<Label> labels : labelSequences) {
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

    private int[] packCatchBlockPositions(List<EirBlock> eirBlocks) {
        if (eirBlocks.isEmpty()) {
            return null;
        }
        final int[] positions = new int[eirBlocks.size()];
        try {
            for (int i = 0; i < eirBlocks.size(); i++) {
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

    private ClassMethodActor[] packDirectCallees(List<EirCall> directCalls) {
        if (directCalls.isEmpty()) {
            return null;
        }
        final ClassMethodActor[] callees = new ClassMethodActor[directCalls.size()];
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
        for (EirInfopoint safepoint : emitter.safepoints()) {
            safepoint.addRegisterReferenceMap(bitMap);
            bitMap.next();
        }
        assert bitMap.offset() == referenceMaps.length;
        return referenceMaps;
    }

    protected abstract EirTargetEmitter createEirTargetEmitter(EirMethod eirMethod);

    public TargetMethod makeIrMethod(EirMethod eirMethod) {
        final CPSTargetMethod targetMethod = createIrMethod(eirMethod.classMethodActor());
        generateTarget(targetMethod, eirMethod, true);
        return targetMethod;
    }

    @Override
    protected void generateIrMethod(CPSTargetMethod targetMethod, boolean install) {
        final EirGeneratorScheme eirGeneratorScheme = (EirGeneratorScheme) compilerScheme();
        final EirGenerator<?> eirGenerator = eirGeneratorScheme.eirGenerator();
        final EirMethod eirMethod = eirGenerator.makeIrMethod(targetMethod.classMethodActor(), install);

        generateTarget(targetMethod, eirMethod, install);
    }

    private void generateTarget(CPSTargetMethod targetMethod, final EirMethod eirMethod, boolean install) throws ProgramError {
        final EirTargetEmitter<?> emitter = createEirTargetEmitter(eirMethod);

        Adapter adapter = emitter.adapt(targetMethod.classMethodActor);

        eirMethod.emit(emitter);

        final DataModel dataModel = Platform.platform().dataModel;

        final Collection<EirLiteral> scalarLiterals = eirMethod.literalPool().scalarLiterals();
        final Collection<EirLiteral> referenceLiterals = eirMethod.literalPool().referenceLiterals();

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
        if (install) {
            Code.allocate(targetBundleLayout, targetMethod);
        } else {
            Code.allocateInHeap(targetBundleLayout, targetMethod);
        }

        final List<EirCall> directCalls = emitter.directCalls();
        final int[] stopPositions = packLabelPositions(emitter.directCallLabels(), emitter.indirectCallLabels(), emitter.safepointLabels());
        encodeExtraStopPositionInfo(stopPositions, emitter);
        final byte[] compressedJavaFrameDescriptors = targetMethod.classMethodActor().isTemplate() ? null : emitter.getCompressedJavaFrameDescriptors();

        final int numberOfDirectCalls = emitter.directCallLabels().size();
        final int numberOfIndirectCalls = emitter.indirectCallLabels().size();
        final int numberOfSafepoints = emitter.safepointLabels().size();
        final int registerReferenceMapSize = targetMethod.registerReferenceMapSize();
        int referenceMapsSize = CPSTargetMethod.computeReferenceMapsSize(numberOfDirectCalls, numberOfIndirectCalls, numberOfSafepoints, frameReferenceMapSize, registerReferenceMapSize);

        targetMethod.setGenerated(
                        packLabelPositions(emitter.catchRangeLabels()),
                        packCatchBlockPositions(emitter.catchBlocks()),
                        stopPositions,
                        compressedJavaFrameDescriptors,
                        packDirectCallees(directCalls),
                        emitter.indirectCallLabels().size(),
                        emitter.safepointLabels().size(),
                        packReferenceMaps(referenceMapsSize, emitter, frameReferenceMapSize, registerReferenceMapSize),
                        scalarLiteralBytes,
                        referenceLiteralObjects,
                        code,
                        emitter.inlineDataRecorder().encodedDescriptors(),
                        eirMethod.frameSize(),
                        frameReferenceMapSize
        );
        assert TargetBundleLayout.from(targetMethod).bundleSize().equals(targetBundleLayout.bundleSize()) :
            "computed target bundle size differs from derived target bundle size for " + targetMethod.classMethodActor() +
            "\n    computed:\n" + targetBundleLayout +
            "\n     derived:\n" + TargetBundleLayout.from(targetMethod);
        if (MaxineVM.isHosted()) {
            // the compiled prototype links all methods in a separate phase
        } else {
            if (install) {
                // at target runtime, each method gets linked individually right after generating it:
                targetMethod.linkDirectCalls(adapter);
            } else {
                // the displacement between a call site in the heap and a code cache location may not fit in the offset operand of a call
            }
        }
        eirMethod.cleanupAfterEmitting();
    }
}
