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
import com.sun.c1x.ci.CiCodePos;

/**
 * The <code>FrameState</code> class represents an immutable view of the state of a frame
 * (i.e. it does not have push, pop, or store operations). It is represented more compactly
 * than a <code>ValueStack</code>. Its immutability eliminates a large number of bugs in
 * caching of states within the IR.
 *
 * @author Ben L. Titzer
 */
public class FrameState {

    /**
     * The frame state of the caller method, if any.
     */
    public final FrameState caller;

    /**
     * The code position of this frame state.
     */
    public final CiCodePos pos;

    private final Value[] state;
    private final char localsSize;
    private final char stackSize;

    /**
     * Creates a new frame state from the specified caller, at the specified position, by copying
     * the values from the supplied value stack.
     * @param caller the caller frame state
     * @param pos the code position for this frame state
     * @param state the value stack containing the values
     */
    public FrameState(FrameState caller, CiCodePos pos, ValueStack state) {
        this.pos = pos;
        this.caller = caller;
        this.localsSize = (char) state.localsSize();
        this.stackSize = (char) state.stackSize();
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
        assert verify(state);
    }

    private boolean verify(ValueStack state) {
        if (caller == null) {
            assert pos.caller == null : "should not have caller";
            assert state.scope().isTopScope() : "should be top scope";
        } else {
            assert pos.caller.matches(caller.pos) : "caller mismatch";
            assert state.scope().callerCodeSite().matches(caller.pos) : "caller mismatch";
        }
        return true;
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

