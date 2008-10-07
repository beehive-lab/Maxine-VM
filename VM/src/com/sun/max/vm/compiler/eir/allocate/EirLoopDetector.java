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
/*VCSID=c3c40a0d-8e5d-4fd2-b84e-24d84764bb61*/
package com.sun.max.vm.compiler.eir.allocate;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * Assign loop nesting depth values to EIR blocks
 * to inform the register allocator's spilling decisions.
 * Can be a bit imprecise, even wrong,
 * because it does not affect correctness of the register allocator.
 *
 * @author Bernd Mathiske
 */
public final class EirLoopDetector {

    private EirLoopDetector() {
    }

    public static String toString(BitSet bs) {
        String s = "{";
        String separator = "";
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            if (bs.get(i)) {
                s += separator + i;
                separator = " ";
            }
        }
        return s + "}";
    }

    /**
     * Very simple.
     * We figure that usually blocks have been generated with serial numbers that are in succession order.
     * If a block's successor has a lower serial number than the block
     * then we assume that they form a loop and that all blocks in between are part of the loop as well.
     * This is a gross simplification, which will do until we have a more precise algorithm.
     */
    public static void determineNestingDepths(Pool<EirBlock> blocks) {
        final int[] nestingDepths = new int[blocks.length()];
        for (EirBlock block : blocks) {
            for (EirBlock predecessor : block.predecessors()) {
                if (predecessor.serial() > block.serial()) {
                    for (int i = block.serial(); i <= predecessor.serial(); i++) {
                        nestingDepths[i]++;
                    }
                }
            }
        }
        for (EirBlock block : blocks) {
            block.setLoopNestingDepth(nestingDepths[block.serial()]);
        }
    }

}
