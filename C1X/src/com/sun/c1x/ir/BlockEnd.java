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
package com.sun.c1x.ir;

import java.util.*;

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.cri.ci.*;

/**
 * The {@code BlockEnd} instruction is a base class for all instructions that end a basic
 * block, including branches, switches, throws, and goto's.
 *
 * @author Ben L. Titzer
 */
public abstract class BlockEnd extends Instruction {

    BlockBegin begin;
    final List<BlockBegin> successors;
    FrameState stateAfter;

    /**
     * Constructs a new block end with the specified value type.
     * @param kind the type of the value produced by this instruction
     * @param stateAfter the value stack after the end of this block
     * @param isSafepoint {@code true} if this instruction is a safepoint instruction
     * @param successors the list of successor blocks. If {@code null}, a new one will be created.
     */
    public BlockEnd(CiKind kind, FrameState stateAfter, boolean isSafepoint, List<BlockBegin> successors) {
        super(kind);
        this.successors = successors == null ? new ArrayList<BlockBegin>(2) : successors;
        this.stateAfter = stateAfter;
        if (isSafepoint) {
            setFlag(Value.Flag.IsSafepoint);
        }
    }

    public BlockEnd(CiKind kind, FrameState stateAfter, boolean isSafepoint) {
        this(kind, stateAfter, isSafepoint, null);
    }

    /**
     * Get the state after the end of this block.
     * @return the value stack representing the state
     */
    @Override
    public FrameState stateAfter() {
        return stateAfter;
    }

    public void setStateAfter(FrameState state) {
        stateAfter = state;
    }

    /**
     * Checks whether this instruction is a safepoint.
     * @return {@code true} if this instruction is a safepoint
     */
    public boolean isSafepoint() {
        return checkFlag(Value.Flag.IsSafepoint);
    }

    /**
     * Gets the block begin associated with this block end.
     * @return the beginning of this basic block
     */
    public BlockBegin begin() {
        return begin;
    }

    /**
     * Sets the basic block beginning for this block end. This should only
     * be called from {@link BlockBegin}.
     *
     * @param block the beginning of this basic block
     */
    void setBegin(BlockBegin block) {
        begin = block;
    }

    /**
     * Substitutes a successor block in this block end's successor list. Note that
     * this method updates all occurrences in the list.
     * @param oldSucc the old successor to replace
     * @param newSucc the new successor
     */
    public void substituteSuccessor(BlockBegin oldSucc, BlockBegin newSucc) {
        Util.replaceAllInList(oldSucc, newSucc, successors);
    }

    /**
     * Gets the successor corresponding to the default (fall through) case.
     * @return the default successor
     */
    public BlockBegin defaultSuccessor() {
        return successors.get(successors.size() - 1);
    }

    /**
     * Searches for the specified successor and returns its index into the
     * successor list if found.
     * @param b the block to search for in the successor list
     * @return the index of the block in the list if found; </code>-1</code> otherwise
     */
    public int successorIndex(BlockBegin b) {
        final int max = successors.size();
        for (int i = 0; i < max; i++) {
            if (successors.get(i) == b) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets this block end's list of successors.
     * @return the successor list
     */
    public List<BlockBegin> successors() {
        return successors;
    }

    /**
     * Gets the successor at a specified index.
     * @param index the index of the successor
     * @return the successor
     */
    public BlockBegin suxAt(int index) {
        return successors.get(index);
    }
}
