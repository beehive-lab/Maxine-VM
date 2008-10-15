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
    ClassMethodActor _method;
    int _lp;
    int _sp;
    int _pc;

    public Frame() {

    }

    public Frame(Frame frame) {
        this(frame, 0);
    }

    public Frame(Frame frame, int offset) {
        _method = frame._method;
        _lp = frame._lp + offset;
        _sp = frame._sp + offset;
        _pc = frame._pc;
    }

    public Frame(ClassMethodActor method, int pc, int sp) {
        _method = method;
        _sp = sp;
        _pc = pc;
    }

    public Frame(BytecodeLocation location, int sp) {
        _method = location.classMethodActor();
        _sp = sp;
        _pc = location.position();
    }

    public ClassMethodActor method() {
        return _method;
    }

    public int pc() {
        return _pc;
    }

    public void setPc(int pc) {
        _pc = pc;
    }

    public int lp() {
        return _lp;
    }

    public int sp() {
        return _sp;
    }

    public int stackHeight() {
        return _sp - _lp;
    }

    @Override
    public String toString() {
        String name = "null";
        if (_method != null) {
            name = _method.name().toString();
        }
        return name + " lp: " + _lp + ", sp: " + _sp + ", pc: " + _pc;
    }

    public Frame copy() {
        return new Frame(this, 0);
    }

    @Override
    public boolean equals(Object obj) {
        final Frame other = (Frame) obj;
        return _method == other._method && _lp == other._lp && _sp == other._sp && _pc == other._pc;
    }

    /**
     * Tests if two frames have the same method and stack height.
     */
    public boolean matches(Frame other) {
        return _method == other._method && stackHeight() == other.stackHeight();
    }

    public void empty() {
        _sp = _lp + _method.codeAttribute().maxLocals();
    }

    public boolean isEmpty() {
        return _sp == _lp + _method.codeAttribute().maxLocals();
    }

    public BytecodeLocation location() {
        return new BytecodeLocation(_method, _pc);
    }
}
