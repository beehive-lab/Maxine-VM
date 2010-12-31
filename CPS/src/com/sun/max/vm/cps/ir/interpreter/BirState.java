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
package com.sun.max.vm.cps.ir.interpreter;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.hotpath.state.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class BirState extends State<Value> {
    /**
     * Creates an interpretation state. It is convenient to add one empty frame to
     * facilitate parameter passing.
     */
    public BirState() {
        frames.add(new Frame());
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
        return new BirState(this, this.frames.size() - frameCount, frameCount);
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
        return BirInterpreter.filler;
    }

    @Override
    protected Value undefined() {
        return BirInterpreter.undefined;
    }

    @Override
    protected Value[] createArray(int length) {
        return new Value[length];
    }
}
