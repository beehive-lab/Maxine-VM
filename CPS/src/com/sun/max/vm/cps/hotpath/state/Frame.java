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
package com.sun.max.vm.cps.hotpath.state;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;

public class Frame {
    ClassMethodActor method;
    int lp;
    int sp;
    int pc;

    public Frame() {

    }

    public Frame(Frame frame) {
        this(frame, 0);
    }

    public Frame(Frame frame, int offset) {
        this.method = frame.method;
        this.lp = frame.lp + offset;
        this.sp = frame.sp + offset;
        this.pc = frame.pc;
    }

    public Frame(ClassMethodActor method, int pc, int sp) {
        this.method = method;
        this.sp = sp;
        this.pc = pc;
    }

    public Frame(BytecodeLocation location, int sp) {
        this.method = location.classMethodActor;
        this.sp = sp;
        this.pc = location.bytecodePosition;
    }

    public ClassMethodActor method() {
        return method;
    }

    public int pc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int lp() {
        return lp;
    }

    public int sp() {
        return sp;
    }

    public int stackHeight() {
        return sp - lp;
    }

    @Override
    public String toString() {
        String name = "null";
        if (method != null) {
            name = method.name.toString();
        }
        return name + " lp: " + lp + ", sp: " + sp + ", pc: " + pc;
    }

    public Frame copy() {
        return new Frame(this, 0);
    }

    @Override
    public boolean equals(Object obj) {
        final Frame other = (Frame) obj;
        return method == other.method && lp == other.lp && sp == other.sp && pc == other.pc;
    }

    /**
     * Tests if two frames have the same method and stack height.
     */
    public boolean matches(Frame other) {
        return method == other.method && stackHeight() == other.stackHeight();
    }

    public void empty() {
        sp = lp + method.codeAttribute().maxLocals;
    }

    public boolean isEmpty() {
        return sp == lp + method.codeAttribute().maxLocals;
    }

    public BytecodeLocation location() {
        return new BytecodeLocation(method, pc);
    }
}
