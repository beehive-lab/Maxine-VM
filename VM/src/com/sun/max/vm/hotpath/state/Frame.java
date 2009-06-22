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
package com.sun.max.vm.hotpath.state;

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
        this.method = location.classMethodActor();
        this.sp = sp;
        this.pc = location.bytecodePosition();
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
        sp = lp + method.codeAttribute().maxLocals();
    }

    public boolean isEmpty() {
        return sp == lp + method.codeAttribute().maxLocals();
    }

    public BytecodeLocation location() {
        return new BytecodeLocation(method, pc);
    }
}
