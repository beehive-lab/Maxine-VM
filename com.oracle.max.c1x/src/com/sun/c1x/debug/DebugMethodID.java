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
package com.sun.c1x.debug;

import com.oracle.max.criutils.*;
import com.sun.c1x.ir.*;
import com.sun.cri.ci.*;

public final class DebugMethodID extends Instruction {

    private final int bci;
    private final String parentMethod;
    private final String inlinedMethod;

    public DebugMethodID(int bci, String parentMethod, String inlinedMethod) {
        super(CiKind.Illegal);
        this.bci = bci;
        this.parentMethod = parentMethod;
        this.inlinedMethod = inlinedMethod;
        setFlag(Flag.LiveSideEffect);
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitDebugMethodID(this);
    }

    @Override
    public void print(LogStream out) {
        // TODO Auto-generated method stub
    }

    public int getBci() {
        return bci;
    }

    public String getParentMethod() {
        return parentMethod;
    }

    public String getInlinedMethod() {
        return inlinedMethod;
    }
}
