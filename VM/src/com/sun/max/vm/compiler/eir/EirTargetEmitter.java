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

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.builtin.MakeStackVariable.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirTargetEmitter<Assembler_Type extends Assembler> {

    private final Assembler_Type assembler;

    public Assembler_Type assembler() {
        return assembler;
    }

    private final EirABI abi;

    public EirABI abi() {
        return abi;
    }

    private final int frameSize;

    public int frameSize() {
        return frameSize;
    }

    private final WordWidth stackSlotWidth;

    public WordWidth stackSlotWidth() {
        return stackSlotWidth;
    }

    /**
     * Code generator for adapter frame, if any is needed. The generator emits code needed to adapt call from code compiled by other compiler to code
     * produced by the EirTargetEmitter. The frame adapter is embedded in the target method's code. It's code is typically made of an prologue and epilogue.
     */
    private final AdapterFrameGenerator<Assembler_Type> adapterFrameGenerator;

    public void emitFrameAdapterPrologue() {
        if (adapterFrameGenerator != null) {
            adapterFrameGenerator.emitPrologue(assembler());
        }
    }

    public void emitFrameAdapterEpilogue() {
        if (adapterFrameGenerator != null) {
            adapterFrameGenerator.emitEpilogue(assembler());
        }
    }

    private final Safepoint safepoint;

    protected EirTargetEmitter(Assembler_Type assembler, EirABI abi, int frameSize, Safepoint safepoint, WordWidth stackSlotWidth, AdapterFrameGenerator<Assembler_Type> adapterFrameGenerator) {
        this.assembler = assembler;
        this.abi = abi;
        this.frameSize = frameSize;
        this.safepoint = safepoint;
        this.adapterFrameGenerator = adapterFrameGenerator;
        this.stackSlotWidth = stackSlotWidth;
    }

    private EirBlock currentEirBlock;

    public EirBlock currentEirBlock() {
        return currentEirBlock;
    }

    public void setCurrentEirBlock(EirBlock eirBlock) {
        currentEirBlock = eirBlock;
    }

    private final AppendableSequence<Label> catchRangeLabels = new LinkSequence<Label>();

    public Sequence<Label> catchRangeLabels() {
        return catchRangeLabels;
    }

    private final AppendableIndexedSequence<EirBlock> catchBlocks = new ArrayListSequence<EirBlock>();

    public IndexedSequence<EirBlock> catchBlocks() {
        return catchBlocks;
    }

    private EirBlock currentCatchBlock = null;

    public void addCatching(EirBlock catchBlock) {
        if (catchBlock == currentCatchBlock) {
            return;
        }
        currentCatchBlock = catchBlock;
        final Label label = new Label();
        assembler.bindLabel(label);
        catchRangeLabels.append(label);
        catchBlocks.append(catchBlock);
    }

    final AppendableSequence<Label> directCallLabels = new LinkSequence<Label>();

    public Sequence<Label> directCallLabels() {
        return directCallLabels;
    }

    private final AppendableSequence<EirCall> directCalls = new LinkSequence<EirCall>();

    public Sequence<EirCall> directCalls() {
        return directCalls;
    }

    private final BitSet directReferenceCalls = new BitSet();

    private TargetLocation[] eirToTargetLocations(EirOperand[] eirOperands) {
        final TargetLocation[] result = new TargetLocation[eirOperands.length];
        for (int i = 0; i < eirOperands.length; i++) {
            result[i] = eirOperands[i].location().toTargetLocation();
        }
        return result;
    }

    private final GrowableMapping<EirJavaFrameDescriptor, TargetJavaFrameDescriptor> eirToTargetJavaFrameDescriptor = HashMapping.createEqualityMapping();

    private TargetJavaFrameDescriptor eirToTargetJavaFrameDescriptor(EirJavaFrameDescriptor eirJavaFrameDescriptor) {
        if (eirJavaFrameDescriptor == null) {
            return null;
        }
        TargetJavaFrameDescriptor targetJavaFrameDescriptor = eirToTargetJavaFrameDescriptor.get(eirJavaFrameDescriptor);
        if (targetJavaFrameDescriptor == null) {
            targetJavaFrameDescriptor = new TargetJavaFrameDescriptor(eirToTargetJavaFrameDescriptor(eirJavaFrameDescriptor.parent()),
                                                                      eirJavaFrameDescriptor.classMethodActor(),
                                                                      eirJavaFrameDescriptor.bytecodePosition(),
                                                                      eirToTargetLocations(eirJavaFrameDescriptor.locals),
                                                                      eirToTargetLocations(eirJavaFrameDescriptor.stackSlots));
            eirToTargetJavaFrameDescriptor.put(eirJavaFrameDescriptor, targetJavaFrameDescriptor);
        }
        return targetJavaFrameDescriptor;
    }

    public void addDirectCall(EirCall call) {
        final Label label = new Label();
        assembler.bindLabel(label);
        if (call.result() != null && call.result().eirValue().kind() == Kind.REFERENCE) {
            directReferenceCalls.set(directCalls.length());
        }
        directCallLabels.append(label);
        directCalls.append(call);
    }

    final AppendableSequence<Label> indirectCallLabels = new LinkSequence<Label>();

    public Sequence<Label> indirectCallLabels() {
        return indirectCallLabels;
    }

    final AppendableSequence<EirCall> indirectCalls = new LinkSequence<EirCall>();

    public Sequence<EirCall> indirectCalls() {
        return indirectCalls;
    }

    private final BitSet indirectReferenceCalls = new BitSet();

    public void addIndirectCall(EirCall call) {
        final Label label = new Label();
        assembler.bindLabel(label);
        if (call.result() != null && call.result().eirValue().kind() == Kind.REFERENCE) {
            indirectReferenceCalls.set(indirectCalls.length());
        }
        indirectCallLabels.append(label);
        indirectCalls.append(call);
    }

    public final boolean isReferenceCall(int stopIndex) {
        if (stopIndex < directCalls.length()) {
            return directReferenceCalls.get(stopIndex);
        }
        return indirectReferenceCalls.get(stopIndex - directCalls.length());
    }

    private final AppendableSequence<Label> safepointLabels = new LinkSequence<Label>();

    public Sequence<Label> safepointLabels() {
        return safepointLabels;
    }

    private final AppendableSequence<EirSafepoint> safepoints = new LinkSequence<EirSafepoint>();

    Sequence<EirSafepoint> safepoints() {
        return safepoints;
    }

    public void addSafepoint(EirSafepoint eirSafepoint) {
        final Label label = new Label();
        assembler.bindLabel(label);
        safepointLabels.append(label);
        safepoints.append(eirSafepoint);
    }

    private final AppendableSequence<EirGuardpoint> guardpoints = new LinkSequence<EirGuardpoint>();

    Sequence<EirGuardpoint> guardpoints() {
        return guardpoints;
    }

    public void addGuardpoint(EirGuardpoint guardpoint) {
        guardpoints.append(guardpoint);
    }

    private void appendTargetJavaFrameDescriptors(Iterable<? extends EirStop> stops, AppendableSequence<TargetJavaFrameDescriptor> descriptors) {
        for (EirStop stop : stops) {
            final EirJavaFrameDescriptor javaFrameDescriptor = stop.javaFrameDescriptor();
            assert javaFrameDescriptor != null : " stop " + stop + " is missing a Java frame descriptor";
            descriptors.append(eirToTargetJavaFrameDescriptor(javaFrameDescriptor));
        }
    }

    private Sequence<TargetJavaFrameDescriptor> getTargetJavaFrameDescriptors() {
        final AppendableSequence<TargetJavaFrameDescriptor> descriptors = new ArrayListSequence<TargetJavaFrameDescriptor>(directCalls.length() + indirectCalls.length() + safepoints.length() + guardpoints.length());
        appendTargetJavaFrameDescriptors(directCalls, descriptors);
        appendTargetJavaFrameDescriptors(indirectCalls, descriptors);
        appendTargetJavaFrameDescriptors(safepoints, descriptors);
        appendTargetJavaFrameDescriptors(guardpoints, descriptors);
        return descriptors;
    }

    public final byte[] getCompressedJavaFrameDescriptors() {
        return TargetJavaFrameDescriptor.compress(getTargetJavaFrameDescriptors());
    }

    protected abstract boolean isCall(byte[] code, int offset);

    protected boolean isSafepoint(byte[] code, int offset) {
        return Bytes.equals(code, offset, safepoint.code);
    }

    /**
     * Tests whether all call labels are still pointing at CALL instructions.
     */
    private boolean areLabelsValid(byte[] code, Address startAddress) throws AssemblyException {
        for (Label label : directCallLabels) {
            if (!assembler.boundLabels().contains(label) || label.state() != Label.State.BOUND || !isCall(code, label.position())) {
                if (MaxineVM.isPrototyping()) {
                    Disassemble.disassemble(System.out, code, VMConfiguration.hostOrTarget().platform().processorKind, startAddress, InlineDataDecoder.createFrom(inlineDataRecorder), null);
                }
                return false;
            }
        }
        for (Label label : safepointLabels) {
            if (!assembler.boundLabels().contains(label) || label.state() != Label.State.BOUND || !isSafepoint(code, label.position())) {
                if (MaxineVM.isPrototyping()) {
                    Disassemble.disassemble(System.out, code, VMConfiguration.hostOrTarget().platform().processorKind, startAddress, InlineDataDecoder.createFrom(inlineDataRecorder), null);
                }
                return false;
            }
        }
        return true;
    }

    private final InlineDataRecorder inlineDataRecorder = new InlineDataRecorder();

    public InlineDataRecorder inlineDataRecorder() {
        return inlineDataRecorder;
    }

    public byte[] toByteArray() throws AssemblyException {
        final byte[] result = assembler.toByteArray(inlineDataRecorder);
        assert areLabelsValid(result, Address.fromLong(assembler.baseAddress()));
        return result;
    }

    protected abstract void setStartAddress(Address address);

    protected abstract void fixLabel(Label label, Address address);

    private Map<StackVariable, Integer> namedStackVariables;

    /**
     * See {@link StackVariable#create(String)} for an explanation as to why this can only be called while prototyping.
     */
    public void recordStackVariableOffset(StackVariable key, int offset) {
        ProgramError.check(MaxineVM.isPrototyping());
        if (namedStackVariables == null) {
            namedStackVariables = new HashMap<StackVariable, Integer>();
        }
        final Integer old = namedStackVariables.put(key, offset);
        assert old == null;
    }

    public Iterable<Map.Entry<StackVariable, Integer>> namedStackVariableOffsets() {
        if (namedStackVariables == null) {
            return Iterables.empty();
        }
        return namedStackVariables.entrySet();
    }


    private Label markerLabel = new Label();

    public Label markerLabel() {
        return markerLabel;
    }

    public void setMarker() {
        assembler.bindLabel(markerLabel);
    }
}
