/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.value;

import com.sun.c1x.ir.Instruction;
import com.sun.c1x.ir.IRScope;
import com.sun.c1x.util.InstructionClosure;
import com.sun.c1x.util.Util;

/**
 * The <code>FrameState</code> class represents an immutable view of the state of a frame
 * (i.e. it does not have push, pop, or store operations). It is represented more compactly
 * than a <code>ValueStack</code>. Its immutability eliminates a large number of bugs in
 * caching of states within the IR.
 *
 * @author Ben L. Titzer
 */
public class FrameState {

    private final IRScope _scope;
    private final Instruction[] _state;
    private final char _localsSize;
    private final char _stackSize;

    public FrameState(ValueStack state) {
        _scope = state.scope();
        _localsSize = (char) state.localsSize();
        _stackSize = (char) state.stackSize();
        _state = new Instruction[_localsSize + _stackSize + state.locksSize()];

        int i = 0;
        for (int j = 0; j < _localsSize; j++) {
            _state[i++] = state.localAt(j);
        }
        for (int j = 0; j < _stackSize; j++) {
            _state[i++] = state.stackAt(j);
        }
        for (int j = 0; j < state.locksSize(); j++) {
            _state[i++] = state.lockAt(j);
        }
    }

    public IRScope scope() {
        return _scope;
    }

    public ValueStack asValueStack() {
        // TODO: efficiently copy this frame state into a value stack
        throw Util.unimplemented();
    }

    public Instruction localAt(int i) {
        return _state[i];
    }

    public Instruction stackAt(int i) {
        return _state[i + _localsSize];
    }

    public Instruction lockAt(int i) {
        return _state[i + _localsSize + _stackSize];
    }

    public int localsSize() {
        return _localsSize;
    }

    public int stackSize() {
        return _stackSize;
    }

    public int lockSize() {
        return _state.length - _stackSize - _localsSize;
    }

    public void valuesDo(InstructionClosure closure) {
        for (int i = 0; i < _state.length; i++) {
            Instruction x = _state[i];
            if (x != null) {
                _state[i] = closure.apply(x);
            }
        }
    }
}

