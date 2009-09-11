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

import com.sun.c1x.ir.*;
import com.sun.c1x.util.*;

/**
 * The <code>FrameState</code> class represents an immutable view of the state of a frame
 * (i.e. it does not have push, pop, or store operations). It is represented more compactly
 * than a <code>ValueStack</code>. Its immutability eliminates a large number of bugs in
 * caching of states within the IR.
 *
 * @author Ben L. Titzer
 */
public class FrameState {

    private final IRScope scope;
    private final Value[] state;
    private final char localsSize;
    private final char stackSize;

    public FrameState(ValueStack state) {
        scope = state.scope();
        localsSize = (char) state.localsSize();
        stackSize = (char) state.stackSize();
        this.state = new Value[localsSize + stackSize + state.locksSize()];

        int i = 0;
        for (int j = 0; j < localsSize; j++) {
            this.state[i++] = state.localAt(j);
        }
        for (int j = 0; j < stackSize; j++) {
            this.state[i++] = state.stackAt(j);
        }
        for (int j = 0; j < state.locksSize(); j++) {
            this.state[i++] = state.lockAt(j);
        }
    }

    public IRScope scope() {
        return scope;
    }

    public ValueStack asValueStack() {
        // TODO: efficiently copy this frame state into a value stack
        throw Util.unimplemented();
    }

    public Value localAt(int i) {
        return state[i];
    }

    public Value stackAt(int i) {
        return state[i + localsSize];
    }

    public Value lockAt(int i) {
        return state[i + localsSize + stackSize];
    }

    public int localsSize() {
        return localsSize;
    }

    public int stackSize() {
        return stackSize;
    }

    public int lockSize() {
        return state.length - stackSize - localsSize;
    }

    public void valuesDo(ValueClosure closure) {
        for (int i = 0; i < state.length; i++) {
            Value x = state[i];
            if (x != null) {
                state[i] = closure.apply(x);
            }
        }
    }
}

