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
package com.sun.max.vm.compiler.cps.tir;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.cps.ir.*;
import com.sun.max.vm.compiler.cps.tir.pipeline.*;
import com.sun.max.vm.compiler.cps.tir.target.*;
import com.sun.max.vm.profile.TreeAnchor;

public class TirTree extends AbstractIrMethod {
    public static class Profile {
        public int iterations;
        public int executions;
    }

    private final Profile profile = new Profile();

    public Profile profile() {
        return profile;
    }

    private final TreeAnchor anchor;
    private TargetTree targetTree;

    public TargetTree targetTree() {
        return targetTree;
    }

    public void setTarget(TargetTree targetTree) {
        this.targetTree = targetTree;
    }

    private AppendableIndexedSequence<TirLocal> locals = new ArrayListSequence<TirLocal>();
    private AppendableIndexedSequence<TirInstruction> prologue = new ArrayListSequence<TirInstruction>();
    private AppendableIndexedSequence<TirTrace> traces = new ArrayListSequence<TirTrace>();

    public Sequence<TirTrace> traces() {
        return traces;
    }

    private final TirState entryState;

    public TirState entryState() {
        return entryState;
    }

    public TirTree(TreeAnchor anchor, TirState entryState) {
        super(anchor.method());
        this.anchor = anchor;
        this.entryState = entryState;
    }

    public void append(TirLocal local) {
        locals.append(local);
    }

    public void append(TirInstruction instruction) {
        prologue.append(instruction);
    }

    public void append(TirTrace trace) {
        traces.append(trace);
    }

    public void send(TirMessageSink sink) {
        sink.receive(new TirMessage.TirTreeBegin(this, TirPipelineOrder.REVERSE));
        sendTraces(sink);
        sendPrologue(sink);
        sendLocals(sink);
        sink.receive(new TirMessage.TirTreeEnd());
    }

    private void sendTraces(TirMessageSink sink) {
        for (int i = traces.length() - 1; i >= 0; i--) {
            traces.get(i).send(sink);
        }
    }

    private void sendPrologue(TirMessageSink sink) {
        for (int i = prologue.length() - 1; i >= 0; i--) {
            sink.receive(prologue.get(i));
        }
    }

    private void sendLocals(TirMessageSink sink) {
        for (int i = locals.length() - 1; i >= 0; i--) {
            sink.receive(locals.get(i));
        }
    }

    @Override
    public String toString() {
        return entryState.frames().first().toString();
    }

    public boolean isGenerated() {
        return false;
    }

    public String traceToString() {
        return null;
    }

    public TreeAnchor anchor() {
        return anchor;
    }

    public Sequence<TirInstruction> prologue() {
        return prologue;
    }

    public Sequence<TirLocal> locals() {
        return locals;
    }

    public int getNumber(final TirInstruction instruction) {
        final MutableInnerClassGlobal<Integer> result = new MutableInnerClassGlobal<Integer>(-1);
        final Class<? extends TirInstruction> cls = instruction.getClass();
        send(new TirReverse(new TirMessageSink() {
            private int number = 0;
            public void receive(TirMessage message) {
                if (cls.isAssignableFrom(message.getClass())) {
                    if (message == instruction) {
                        result.setValue(number);
                    } else {
                        number++;
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
