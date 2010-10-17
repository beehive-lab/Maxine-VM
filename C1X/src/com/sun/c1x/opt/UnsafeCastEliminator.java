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
package com.sun.c1x.opt;

import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;

/**
 * A mechanism to remove {@linkplain UnsafeCast#redundant redundant} unsafe casts.
 *
 * @author Doug Simon
 */
public class UnsafeCastEliminator implements ValueClosure, BlockClosure {

    final IR ir;

    /**
     * Eliminates redundant unsafe casts from a given IR.
     */
    public UnsafeCastEliminator(IR ir) {
        this.ir = ir;
        ir.startBlock.iterateAnyOrder(this, false);
    }

    public void apply(BlockBegin block) {
        if (block.isExceptionEntry()) {
            for (FrameState ehState : block.exceptionHandlerStates()) {
                ehState.valuesDo(this);
            }
        }

        Instruction i = block;
        while (i != null) {
            FrameState stateBefore = i.stateBefore();
            if (stateBefore != null) {
                stateBefore.valuesDo(this);
            }
            i.inputValuesDo(this);
            if (i instanceof BlockEnd) {
                // Remove redundant unsafe casts in the state at the end of a block
                i.stateAfter().valuesDo(this);
            }
            i = i.next();
        }
    }

    public Value apply(Value i) {
        if (i instanceof UnsafeCast) {
            Value y = ((UnsafeCast) i).nonRedundantReplacement();
            assert y != null;
            return y;
        }
        return i;
    }
}
