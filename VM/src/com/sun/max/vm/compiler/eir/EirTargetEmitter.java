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

    private final Assembler_Type _assembler;

    public Assembler_Type assembler() {
        return _assembler;
    }

    private final EirABI _abi;

    public EirABI abi() {
        return _abi;
    }

    private final int _frameSize;

    public int frameSize() {
        return _frameSize;
    }

    private final WordWidth _stackSlotWidth;

    public WordWidth stackSlotWidth() {
        return _stackSlotWidth;
    }

    /**
     * Code generator for adapter frame, if any is needed. The generator emits code needed to adapt call from code compiled by other compiler to code
     * produced by the EirTargetEmitter. The frame adapter is embedded in the target method's code. It's code is typically made of an prologue and epilogue.
     */
    private final AdapterFrameGenerator<Assembler_Type> _adapterFrameGenerator;

    public void emitFrameAdapterPrologue() {
        if (_adapterFrameGenerator != null) {
            _adapterFrameGenerator.emitPrologue(assembler());
        }
    }

    public void emitFrameAdapterEpilogue() {
        if (_adapterFrameGenerator != null) {
            _adapterFrameGenerator.emitEpilogue(assembler());
        }
    }

    private final Safepoint _safepoint;

    protected EirTargetEmitter(Assembler_Type assembler, EirABI abi, int frameSize, Safepoint safepoint, WordWidth stackSlotWidth, AdapterFrameGenerator<Assembler_Type> adapterFrameGenerator) {
        _assembler = assembler;
        _abi = abi;
        _frameSize = frameSize;
        _safepoint = safepoint;
        _adapterFrameGenerator = adapterFrameGenerator;
        _stackSlotWidth = stackSlotWidth;
    }

    private EirBlock _currentEirBlock;

    public EirBlock currentEirBlock() {
        return _currentEirBlock;
    }

    public void setCurrentEirBlock(EirBlock eirBlock) {
        _currentEirBlock = eirBlock;
    }

    private final AppendableSequence<Label> _catchRangeLabels = new LinkSequence<Label>();

    public Sequence<Label> catchRangeLabels() {
        return _catchRangeLabels;
    }

    private final AppendableIndexedSequence<EirBlock> _catchBlocks = new ArrayListSequence<EirBlock>();

    public IndexedSequence<EirBlock> catchBlocks() {
        return _catchBlocks;
    }

    private EirBlock _currentCatchBlock = null;

    public void addCatching(EirBlock catchBlock) {
        if (catchBlock == _currentCatchBlock) {
            return;
        }
        _currentCatchBlock = catchBlock;
        final Label label = new Label();
        _assembler.bindLabel(label);
        _catchRangeLabels.append(label);
        _catchBlocks.append(catchBlock);
    }

    final AppendableSequence<Label> _directCallLabels = new LinkSequence<Label>();

    public Sequence<Label> directCallLabels() {
        return _directCallLabels;
    }

    private final AppendableSequence<EirCall> _directCalls = new LinkSequence<EirCall>();

    public Sequence<EirCall> directCalls() {
        return _directCalls;
    }

    private final BitSet _directReferenceCalls = new BitSet();

    private TargetLocation[] eirToTargetLocations(EirOperand[] eirOperands) {
        final TargetLocation[] result = new TargetLocation[eirOperands.length];
        for (int i = 0; i < eirOperands.length; i++) {
            result[i] = eirOperands[i].location().toTargetLocation();
        }
        return result;
    }

    private final GrowableMapping<EirJavaFrameDescriptor, TargetJavaFrameDescriptor> _eirToTargetJavaFrameDescriptor = HashMapping.createEqualityMapping();

    private TargetJavaFrameDescriptor eirToTargetJavaFrameDescriptor(EirJavaFrameDescriptor eirJavaFrameDescriptor) {
        if (eirJavaFrameDescriptor == null) {
            return null;
        }
        TargetJavaFrameDescriptor targetJavaFrameDescriptor = _eirToTargetJavaFrameDescriptor.get(eirJavaFrameDescriptor);
        if (targetJavaFrameDescriptor == null) {
            targetJavaFrameDescriptor = new TargetJavaFrameDescriptor(eirToTargetJavaFrameDescriptor(eirJavaFrameDescriptor.parent()),
                                                                      eirJavaFrameDescriptor.bytecodeLocation(),
                                                                      eirToTargetLocations(eirJavaFrameDescriptor.locals()),
                                                                      eirToTargetLocations(eirJavaFrameDescriptor.stackSlots()));
            _eirToTargetJavaFrameDescriptor.put(eirJavaFrameDescriptor, targetJavaFrameDescriptor);
        }
        return targetJavaFrameDescriptor;
    }

    public void addDirectCall(EirCall call) {
        final Label label = new Label();
        _assembler.bindLabel(label);
        if (call.result() != null && call.result().eirValue().kind() == Kind.REFERENCE) {
            _directReferenceCalls.set(_directCalls.length());
        }
        _directCallLabels.append(label);
        _directCalls.append(call);
    }

    final AppendableSequence<Label> _indirectCallLabels = new LinkSequence<Label>();

    public Sequence<Label> indirectCallLabels() {
        return _indirectCallLabels;
    }

    final AppendableSequence<EirCall> _indirectCalls = new LinkSequence<EirCall>();

    public Sequence<EirCall> indirectCalls() {
        return _indirectCalls;
    }

    private final BitSet _indirectReferenceCalls = new BitSet();

    public void addIndirectCall(EirCall call) {
        final Label label = new Label();
        _assembler.bindLabel(label);
        if (call.result() != null && call.result().eirValue().kind() == Kind.REFERENCE) {
            _indirectReferenceCalls.set(_indirectCalls.length());
        }
        _indirectCallLabels.append(label);
        _indirectCalls.append(call);
    }

    public final boolean isReferenceCall(int stopIndex) {
        if (stopIndex < _directCalls.length()) {
            return _directReferenceCalls.get(stopIndex);
        }
        return _indirectReferenceCalls.get(stopIndex - _directCalls.length());
    }

    private final AppendableSequence<Label> _safepointLabels = new LinkSequence<Label>();

    public Sequence<Label> safepointLabels() {
        return _safepointLabels;
    }

    private final AppendableSequence<EirSafepoint> _safepoints = new LinkSequence<EirSafepoint>();

    Sequence<EirSafepoint> safepoints() {
        return _safepoints;
    }

    public void addSafepoint(EirSafepoint safepoint) {
        final Label label = new Label();
        _assembler.bindLabel(label);
        _safepointLabels.append(label);
        _safepoints.append(safepoint);
    }

    private final AppendableSequence<EirGuardpoint> _guardpoints = new LinkSequence<EirGuardpoint>();

    Sequence<EirGuardpoint> guardpoints() {
        return _guardpoints;
    }

    public void addGuardpoint(EirGuardpoint guardpoint) {
        _guardpoints.append(guardpoint);
    }

    private void appendTargetJavaFrameDescriptors(Iterable<? extends EirStop> stops, AppendableSequence<TargetJavaFrameDescriptor> descriptors) {
        for (EirStop stop : stops) {
            descriptors.append(eirToTargetJavaFrameDescriptor(stop.javaFrameDescriptor()));
        }
    }

    private Sequence<TargetJavaFrameDescriptor> getTargetJavaFrameDescriptors() {
        final AppendableSequence<TargetJavaFrameDescriptor> descriptors = new ArrayListSequence<TargetJavaFrameDescriptor>(_directCalls.length() + _indirectCalls.length() + _safepoints.length() + _guardpoints.length());
        appendTargetJavaFrameDescriptors(_directCalls, descriptors);
        appendTargetJavaFrameDescriptors(_indirectCalls, descriptors);
        appendTargetJavaFrameDescriptors(_safepoints, descriptors);
        appendTargetJavaFrameDescriptors(_guardpoints, descriptors);
        return descriptors;
    }

    public final byte[] getCompressedJavaFrameDescriptors() {
        return TargetJavaFrameDescriptor.compress(getTargetJavaFrameDescriptors());
    }

    protected abstract boolean isCall(byte[] code, int offset);

    protected boolean isSafepoint(byte[] code, int offset) {
        return Bytes.equals(code, offset, _safepoint.code());
    }

    /**
     * Tests whether all call labels are still pointing at CALL instructions.
     */
    private boolean areLabelsValid(byte[] code, Address startAddress) throws AssemblyException {
        for (Label label : _directCallLabels) {
            if (!_assembler.boundLabels().contains(label) || label.state() != Label.State.BOUND || !isCall(code, label.position())) {
                if (MaxineVM.isPrototyping()) {
                    Disassemble.disassemble(System.out, code, VMConfiguration.hostOrTarget().platform().processorKind(), startAddress, InlineDataDecoder.createFrom(_inlineDataRecorder));
                }
                return false;
            }
        }
        for (Label label : _safepointLabels) {
            if (!_assembler.boundLabels().contains(label) || label.state() != Label.State.BOUND || !isSafepoint(code, label.position())) {
                if (MaxineVM.isPrototyping()) {
                    Disassemble.disassemble(System.out, code, VMConfiguration.hostOrTarget().platform().processorKind(), startAddress, InlineDataDecoder.createFrom(_inlineDataRecorder));
                }
                return false;
            }
        }
        return true;
    }

    private final InlineDataRecorder _inlineDataRecorder = new InlineDataRecorder();

    public InlineDataRecorder inlineDataRecorder() {
        return _inlineDataRecorder;
    }

    public byte[] toByteArray() throws AssemblyException {
        final byte[] result = _assembler.toByteArray(_inlineDataRecorder);
        assert areLabelsValid(result, Address.fromLong(_assembler.baseAddress()));
        return result;
    }

    protected abstract void setStartAddress(Address address);

    protected abstract void fixLabel(Label label, Address address);

    private Map<StackVariable, Integer> _namedStackVariables;

    public void recordStackVariableOffset(StackVariable key, int offset) {
        if (_namedStackVariables == null) {
            _namedStackVariables = new HashMap<StackVariable, Integer>();
        }
        final Integer old = _namedStackVariables.put(key, offset);
        assert old == null;
    }

    public Iterable<Map.Entry<StackVariable, Integer>> namedStackVariableOffsets() {
        if (_namedStackVariables == null) {
            return Iterables.empty();
        }
        return _namedStackVariables.entrySet();
    }


    private Label _markerLabel = new Label();

    public Label markerLabel() {
        return _markerLabel;
    }

    public void setMarker() {
        _assembler.bindLabel(_markerLabel);
    }
}
