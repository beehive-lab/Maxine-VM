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
/*VCSID=5b5d437d-4e48-40b5-8cb9-bf6f868a9f35*/
package com.sun.max.vm.interpreter;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hotpath.state.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class BirState extends State<Value> {
    /**
     * Creates an interpretation state. It is convenient to add one empty frame to
     * facilitate parameter passing.
     */
    public BirState() {
        _frames.append(new Frame());
    }

    /**
     * Copies an existing state.
     */
    public BirState(BirState other) {
        super(other);
    }

    protected BirState(BirState other, int frameIndex, int frameCount) {
        super(other, frameIndex, frameCount);
    }

    public BirState copy() {
        return new BirState(this);
    }

    /**
     * Slices a specified number of frames from this state.
     */
    public BirState slice(int frameCount) {
        return new BirState(this, this._frames.length() - frameCount, frameCount);
    }

    @Override
    protected void setKind(Value element, Kind kind) {
        // We don't care about kind changes.
    }

    boolean hasFrames() {
        return last().method() != null;
    }

    public int position() {
        return last().pc();
    }

    public void setPosition(int position) {
        last().setPc(position);
    }

    public byte[] code() {
        return last().method().compilee().codeAttribute().code();
    }

    public ClassMethodActor method() {
        return last().method();
    }

    @Override
    protected Value filler() {
        return BirInterpreter._filler;
    }

    @Override
    protected Value undefined() {
        return BirInterpreter._undefined;
    }

    @Override
    protected Value[] createArray(int length) {
        return new Value[length];
    }
}
