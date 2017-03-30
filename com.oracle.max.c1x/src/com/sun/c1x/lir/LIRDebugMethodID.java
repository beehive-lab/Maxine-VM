/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
package com.sun.c1x.lir;

import com.sun.cri.ci.*;

public class LIRDebugMethodID extends LIRInstruction {

    final int bci;
    final String parentMethod;
    final String inlinedMethod;

    public LIRDebugMethodID(int bci, String parentMethod, String inlinedMethod) {
        super(LIROpcode.DebugMethodID, CiValue.IllegalValue, null, false);
        this.bci = bci;
        this.parentMethod = parentMethod;
        this.inlinedMethod = inlinedMethod;
    }

    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitDebugID(parentMethod, inlinedMethod);
    }
}
