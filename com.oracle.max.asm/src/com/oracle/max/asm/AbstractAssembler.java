/*
 * Copyright (c) 2017, 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.oracle.max.asm;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiArchitecture.*;

/**
 * The platform-independent base class for the assembler.
 */
public abstract class AbstractAssembler {
    public final CiTarget target;
    public final Buffer codeBuffer;
    public static boolean ASM_DEBUG_MARKERS = false;

    public AbstractAssembler(CiTarget target) {
        this.target = target;
        if (target.arch.byteOrder == ByteOrder.BigEndian) {
            this.codeBuffer = new Buffer.BigEndian();
        } else {
            this.codeBuffer = new Buffer.LittleEndian();
        }
    }

    public final void bind(Label l) {
        assert !l.isBound() : "can bind label only once";
        l.bind(codeBuffer.position());
        l.patchInstructions(this);
    }

    protected abstract void patchJumpTarget(int branch, int target);

    protected final void emitByte(int x) {
        codeBuffer.emitByte(x);
    }

    protected final void emitShort(int x) {
        codeBuffer.emitShort(x);
    }

    public final void emitInt(int x) {
        codeBuffer.emitInt(x);
    }

    protected final void emitInt(int x, int pos) {
        codeBuffer.emitInt(x, pos);
    }

    protected final void emitLong(long x) {
        codeBuffer.emitLong(x);
    }

    public final void offlineAddToBuffer(byte[] b) {
        codeBuffer.offlineCopyBuffer(b);
    }

    /**
     * Emit an array of call trampoline stubs.
     * @param i
     * @return
     */
    public byte[] trampolines(int i) {
        throw new UnsupportedOperationException("Trampolines are not implemented for the current architecture.");
    }
}
