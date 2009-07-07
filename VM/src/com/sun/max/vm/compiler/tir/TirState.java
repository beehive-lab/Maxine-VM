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

import com.sun.max.vm.compiler.instrument.*;
import com.sun.max.vm.compiler.tir.TirInstruction.*;
import com.sun.max.vm.hotpath.state.*;
import com.sun.max.vm.type.*;

public class TirState extends State<TirInstruction> {

    public static TirTree fromAnchor(TreeAnchor anchor) {
        final TirState state = new TirState();
        final TirTree tree = new TirTree(anchor, state);
        final Frame frame = new Frame(anchor.location(), anchor.stackHeight());
        state.frames.append(frame);
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
        return frames().length() > 0;
    }
}
