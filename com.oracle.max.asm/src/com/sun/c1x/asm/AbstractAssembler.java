/*
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.c1x.asm;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.*;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public abstract class AbstractAssembler {
    public final Buffer codeBuffer;
    public final CiTarget target;
    public final CiTargetMethod targetMethod;

    public AbstractAssembler(CiTarget target) {
        this.target = target;
        this.targetMethod = new CiTargetMethod();
        this.codeBuffer = new Buffer(target.arch.byteOrder);
    }

    public final void bind(Label l) {
        assert !l.isBound() : "can bind label only once";
        l.bind(codeBuffer.position());
        l.patchInstructions(this);
    }

    public void setFrameSize(int frameSize) {
        targetMethod.setFrameSize(frameSize);
    }

    public Mark recordMark(Object id, Mark[] references) {
        return targetMethod.recordMark(codeBuffer.position(), id, references);
    }

    public abstract void nop();

    public abstract void nullCheck(CiRegister r);

    public abstract void align(int codeEntryAlignment);

    public abstract void patchJumpTarget(int branch, int target);

    public final void emitByte(int x) {
        codeBuffer.emitByte(x);
    }

    public final void emitShort(int x) {
        codeBuffer.emitShort(x);
    }

    public final void emitInt(int x) {
        codeBuffer.emitInt(x);
    }

    public final void emitLong(long x) {
        codeBuffer.emitLong(x);
    }

    public void blockComment(String s) {
        targetMethod.addAnnotation(new CodeComment(codeBuffer.position(), s));
    }
}
