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

import static com.sun.max.asm.dis.Disassembler.*;

import java.io.*;
import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.runtime.*;

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

    private final AdapterGenerator adapterGenerator;

    public Adapter adapt(ClassMethodActor callee) {
        if (adapterGenerator != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(8);
            Adapter adapter = adapterGenerator.adapt(callee, baos);
            byte[] prologue = baos.toByteArray();
            assembler().emitByteArray(prologue, 0, prologue.length);
            return adapter;
        }
        return null;
    }

    private final Safepoint safepoint;

    protected EirTargetEmitter(Assembler_Type assembler, EirABI abi, int frameSize, Safepoint safepoint, WordWidth stackSlotWidth, AdapterGenerator adapterGenerator) {
        this.assembler = assembler;
        this.abi = abi;
        this.frameSize = frameSize;
        this.safepoint = safepoint;
        this.adapterGenerator = adapterGenerator;
        this.stackSlotWidth = stackSlotWidth;
    }

    private EirBlock currentEirBlock;

    public EirBlock currentEirBlock() {
        return currentEirBlock;
    }

    public void setCurrentEirBlock(EirBlock eirBlock) {
        currentEirBlock = eirBlock;
    }

    private final List<Label> catchRangeLabels = new LinkedList<Label>();

    public List<Label> catchRangeLabels() {
        return catchRangeLabels;
    }

    private final List<EirBlock> catchBlocks = new ArrayList<EirBlock>();

    public List<EirBlock> catchBlocks() {
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
        catchRangeLabels.add(label);
        catchBlocks.add(catchBlock);
    }

    public static class ExtraCallInfo {
        public final Label label;
        public final boolean isReferenceCall;
        public final boolean isNativeFunctionCall;

        public ExtraCallInfo(Label label, boolean isReferenceCall, boolean isNativeFunctionCall) {
            this.label = label;
            this.isReferenceCall = isReferenceCall;
            this.isNativeFunctionCall = isNativeFunctionCall;
        }
    }

    final List<ExtraCallInfo> extraCallInfos = new LinkedList<ExtraCallInfo>();

    public List<ExtraCallInfo> extraCallInfos() {
        return extraCallInfos;
    }

    private void recordExtraCallInfo(EirCall call, Label label) {
        // TODO: Re-enable recording of reference calls once they are needed for deoptimization
        final boolean isReferenceCall = false && call.result() != null && call.result().eirValue().kind().isReference;
        if (isReferenceCall || call.isNativeFunctionCall) {
            extraCallInfos.add(new ExtraCallInfo(label, isReferenceCall, call.isNativeFunctionCall));
        }
    }

    final List<Label> directCallLabels = new LinkedList<Label>();

    public List<Label> directCallLabels() {
        return directCallLabels;
    }

    private final List<EirCall> directCalls = new LinkedList<EirCall>();

    public List<EirCall> directCalls() {
        return directCalls;
    }

    private TargetLocation[] eirToTargetLocations(EirOperand[] eirOperands) {
        final TargetLocation[] result = new TargetLocation[eirOperands.length];
        for (int i = 0; i < eirOperands.length; i++) {
            result[i] = eirOperands[i].location().toTargetLocation();
        }
        return result;
    }

    private final Mapping<EirJavaFrameDescriptor, TargetJavaFrameDescriptor> eirToTargetJavaFrameDescriptor = HashMapping.createEqualityMapping();

    private TargetJavaFrameDescriptor eirToTargetJavaFrameDescriptor(EirJavaFrameDescriptor eirJavaFrameDescriptor) {
        if (eirJavaFrameDescriptor == null) {
            return null;
        }
        TargetJavaFrameDescriptor targetJavaFrameDescriptor = eirToTargetJavaFrameDescriptor.get(eirJavaFrameDescriptor);
        if (targetJavaFrameDescriptor == null) {
            targetJavaFrameDescriptor = new TargetJavaFrameDescriptor(eirToTargetJavaFrameDescriptor(eirJavaFrameDescriptor.parent()),
                                                                      eirJavaFrameDescriptor.classMethodActor,
                                                                      eirJavaFrameDescriptor.bci,
                                                                      eirToTargetLocations(eirJavaFrameDescriptor.locals),
                                                                      eirToTargetLocations(eirJavaFrameDescriptor.stackSlots));
            eirToTargetJavaFrameDescriptor.put(eirJavaFrameDescriptor, targetJavaFrameDescriptor);
        }
        return targetJavaFrameDescriptor;
    }

    public void addDirectCall(EirCall call) {
        final Label label = new Label();
        assembler.bindLabel(label);
        directCallLabels.add(label);
        directCalls.add(call);
        recordExtraCallInfo(call, label);
    }

    final List<Label> indirectCallLabels = new LinkedList<Label>();

    public List<Label> indirectCallLabels() {
        return indirectCallLabels;
    }

    final List<EirCall> indirectCalls = new LinkedList<EirCall>();

    public List<EirCall> indirectCalls() {
        return indirectCalls;
    }

    public void addIndirectCall(EirCall call) {
        final Label label = new Label();
        assembler.bindLabel(label);
        indirectCallLabels.add(label);
        indirectCalls.add(call);
        recordExtraCallInfo(call, label);
    }

    private final List<Label> safepointLabels = new LinkedList<Label>();

    public List<Label> safepointLabels() {
        return safepointLabels;
    }

    private final List<EirInfopoint> safepoints = new LinkedList<EirInfopoint>();

    List<EirInfopoint> safepoints() {
        return safepoints;
    }

    public void addSafepoint(EirInfopoint eirInfopoint) {
        final Label label = new Label();
        assembler.bindLabel(label);
        safepointLabels.add(label);
        safepoints.add(eirInfopoint);
    }

    private void appendTargetJavaFrameDescriptors(Iterable<? extends EirStop> stops, List<TargetJavaFrameDescriptor> descriptors) {
        for (EirStop stop : stops) {
            final EirJavaFrameDescriptor javaFrameDescriptor = stop.javaFrameDescriptor();
            assert javaFrameDescriptor != null : " stop " + stop + " is missing a Java frame descriptor";
            descriptors.add(eirToTargetJavaFrameDescriptor(javaFrameDescriptor));
        }
    }

    private List<TargetJavaFrameDescriptor> getTargetJavaFrameDescriptors() {
        final List<TargetJavaFrameDescriptor> descriptors = new ArrayList<TargetJavaFrameDescriptor>(directCalls.size() + indirectCalls.size() + safepoints.size());
        appendTargetJavaFrameDescriptors(directCalls, descriptors);
        appendTargetJavaFrameDescriptors(indirectCalls, descriptors);
        appendTargetJavaFrameDescriptors(safepoints, descriptors);
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
        final boolean isTemplate = MaxineVM.isHosted() ? abi().templatesOnly() : false;
        for (Label label : directCallLabels) {
            if (!assembler.boundLabels().contains(label) || label.state() != Label.State.BOUND ||
                            !isCall(code, label.position()) || !(isTemplate || AMD64TargetMethodUtil.isPatchableCallSite(Address.fromInt(label.position())))) {
                if (MaxineVM.isHosted()) {
                    Platform platform = Platform.platform();
                    disassemble(System.out, code, platform.isa, platform.wordWidth(), startAddress.toLong(), InlineDataDecoder.createFrom(inlineDataRecorder), null);
                }
                return false;
            }
        }
        for (Label label : safepointLabels) {
            if (!assembler.boundLabels().contains(label) || label.state() != Label.State.BOUND/* || !isSafepoint(code, label.position())*/) {
                if (MaxineVM.isHosted()) {
                    Platform platform = Platform.platform();
                    disassemble(System.out, code, platform.isa, platform.wordWidth(), startAddress.toLong(), InlineDataDecoder.createFrom(inlineDataRecorder), null);
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
}
