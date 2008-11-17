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
package com.sun.max.vm.compiler.tir;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.instrument.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.tir.pipeline.*;
import com.sun.max.vm.compiler.tir.target.*;

public class TirTree extends AbstractIrMethod {
    public static class Profile {
        public int _iterations;
        public int _executions;
    }

    private final Profile _profile = new Profile();

    public Profile profile() {
        return _profile;
    }

    private final TreeAnchor _anchor;
    private TargetTree _targetTree;

    public TargetTree targetTree() {
        return _targetTree;
    }

    public void setTarget(TargetTree targetTree) {
        _targetTree = targetTree;
    }

    private AppendableIndexedSequence<TirLocal> _locals = new ArrayListSequence<TirLocal>();
    private AppendableIndexedSequence<TirInstruction> _prologue = new ArrayListSequence<TirInstruction>();
    private AppendableIndexedSequence<TirTrace> _traces = new ArrayListSequence<TirTrace>();

    public Sequence<TirTrace> traces() {
        return _traces;
    }

    private final TirState _entryState;

    public TirState entryState() {
        return _entryState;
    }

    public TirTree(TreeAnchor anchor, TirState entryState) {
        super(anchor.method());
        _anchor = anchor;
        _entryState = entryState;
    }

    public void append(TirLocal local) {
        _locals.append(local);
    }

    public void append(TirInstruction instruction) {
        _prologue.append(instruction);
    }

    public void append(TirTrace trace) {
        _traces.append(trace);
    }

    public void send(TirMessageSink sink) {
        sink.receive(new TirMessage.TirTreeBegin(this, TirPipelineOrder.REVERSE));
        sendTraces(sink);
        sendPrologue(sink);
        sendLocals(sink);
        sink.receive(new TirMessage.TirTreeEnd());
    }

    private void sendTraces(TirMessageSink sink) {
        for (int i = _traces.length() - 1; i >= 0; i--) {
            _traces.get(i).send(sink);
        }
    }

    private void sendPrologue(TirMessageSink sink) {
        for (int i = _prologue.length() - 1; i >= 0; i--) {
            sink.receive(_prologue.get(i));
        }
    }

    private void sendLocals(TirMessageSink sink) {
        for (int i = _locals.length() - 1; i >= 0; i--) {
            sink.receive(_locals.get(i));
        }
    }

    @Override
    public String toString() {
        return _entryState.frames().first().toString();
    }

    @Override
    public boolean isGenerated() {
        return false;
    }

    @Override
    public String traceToString() {
        return null;
    }

    public TreeAnchor anchor() {
        return _anchor;
    }

    public Sequence<TirInstruction> prologue() {
        return _prologue;
    }

    public Sequence<TirLocal> locals() {
        return _locals;
    }

    public int getNumber(final TirInstruction instruction) {
        final MutableInnerClassGlobal<Integer> result = new MutableInnerClassGlobal<Integer>(-1);
        final Class<? extends TirInstruction> cls = instruction.getClass();
        send(new TirReverse(new TirMessageSink() {
            private int _number = 0;
            @Override
            public void receive(TirMessage message) {
                if (cls.isAssignableFrom(message.getClass())) {
                    if (message == instruction) {
                        result.setValue(_number);
                    } else {
                        _number++;
                    }
                }
            }
        }));
        return result.value();
    }

    public void createFlags() {
        for (TirLocal local : locals()) {
            local.createFlags();
        }
    }
    public void commitFlags() {
        for (TirLocal local : locals()) {
            local.commitFlags();
        }
    }

}
