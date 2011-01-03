/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.bytecode.graft;

import com.sun.max.vm.bytecode.graft.BytecodeAssembler.*;

/**
 *
 *
 * @author Doug Simon
 */
abstract class SynchronizedMethodTransformer extends BytecodeTransformer {

    /**
     * This field is only constructed if needed. That is, if the bytecode being transformed does not contain any return
     * instructions (e.g. method that sits in an infinite loop), it will never be constructed.
     */
    protected Label returnBlockLabel;

    SynchronizedMethodTransformer(BytecodeAssembler assembler) {
        super(assembler);
    }

    abstract void acquireMonitor();
    abstract void releaseMonitor();

    private void convertReturnToGoto() {
        if (returnBlockLabel == null) {
            returnBlockLabel = asm().newLabel();
        }
        asm().goto_(returnBlockLabel);
        ignoreCurrentInstruction();
    }

    @Override
    protected void areturn() {
        convertReturnToGoto();
    }

    @Override
    protected void dreturn() {
        convertReturnToGoto();
    }

    @Override
    protected void freturn() {
        convertReturnToGoto();
    }

    @Override
    protected void ireturn() {
        convertReturnToGoto();
    }

    @Override
    protected void lreturn() {
        convertReturnToGoto();
    }

    @Override
    protected void vreturn() {
        convertReturnToGoto();
    }
}
