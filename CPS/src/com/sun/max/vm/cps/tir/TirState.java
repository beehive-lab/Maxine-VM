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

import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.state.*;
import com.sun.max.vm.cps.tir.TirInstruction.*;
import com.sun.max.vm.type.*;

public class TirState extends State<TirInstruction> {

    public static TirTree fromAnchor(TreeAnchor anchor) {
        final TirState state = new TirState();
        final TirTree tree = new TirTree(anchor, state);
        final Frame frame = new Frame(anchor.location(), anchor.stackHeight());
        state.frames.add(frame);
        for (int i = 0; i < anchor.stackHeight(); i++) {
            final TirLocal context = new TirLocal(i);
            state.setOne(i, context);
            tree.append(context);
        }
        return tree;
    }

    private TirState() {
        super();
    }

    protected TirState(TirState other) {
        super(other);
    }

    public TirState copy() {
        return new TirState(this);
    }

    @Override
    protected void setKind(TirInstruction element, Kind kind) {
        element.setKind(kind);
    }

    @Override
    protected TirInstruction[] createArray(int length) {
        return new TirInstruction[length];
    }

    @Override
    protected TirInstruction filler() {
        return Placeholder.FILLER;
    }

    @Override
    protected TirInstruction undefined() {
        return Placeholder.UNDEFINED;
    }

    public boolean hasFrames() {
        return frames().size() > 0;
    }
}
