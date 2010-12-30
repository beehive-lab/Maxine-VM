/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.tir;

import java.util.*;

import com.sun.max.*;
import com.sun.max.lang.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.tir.pipeline.*;
import com.sun.max.vm.cps.tir.target.*;

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

    private List<TirLocal> locals = new ArrayList<TirLocal>();
    private List<TirInstruction> prologue = new ArrayList<TirInstruction>();
    private List<TirTrace> traces = new ArrayList<TirTrace>();

    public List<TirTrace> traces() {
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
        locals.add(local);
    }

    public void append(TirInstruction instruction) {
        prologue.add(instruction);
    }

    public void append(TirTrace trace) {
        traces.add(trace);
    }

    public void send(TirMessageSink sink) {
        sink.receive(new TirMessage.TirTreeBegin(this, TirPipelineOrder.REVERSE));
        sendTraces(sink);
        sendPrologue(sink);
        sendLocals(sink);
        sink.receive(new TirMessage.TirTreeEnd());
    }

    private void sendTraces(TirMessageSink sink) {
        for (int i = traces.size() - 1; i >= 0; i--) {
            traces.get(i).send(sink);
        }
    }

    private void sendPrologue(TirMessageSink sink) {
        for (int i = prologue.size() - 1; i >= 0; i--) {
            sink.receive(prologue.get(i));
        }
    }

    private void sendLocals(TirMessageSink sink) {
        for (int i = locals.size() - 1; i >= 0; i--) {
            sink.receive(locals.get(i));
        }
    }

    @Override
    public String toString() {
        return Utils.first(entryState.frames()).toString();
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

    public List<TirInstruction> prologue() {
        return prologue;
    }

    public List<TirLocal> locals() {
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
