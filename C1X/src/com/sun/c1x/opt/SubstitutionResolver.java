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

import com.sun.c1x.ir.*;

/**
 * The <code>SubstitutionResolver</code> iterates over the instructions of a program and replaces
 * the occurrence of each instruction with its substitution, if it has one.
 *
 * @author Ben L. Titzer
 */
public class SubstitutionResolver implements BlockClosure, InstructionClosure {

    /**
     * Creates a new SubstitutionResolver and applies it to each instruction
     * in the IR graph, starting from the specified block.
     * @param block the block from which to start substitution
     */
    public SubstitutionResolver(BlockBegin block) {
        block.iteratePreOrder(this);
    }

    public void apply(BlockBegin block) {
        Instruction last = null;
        for (Instruction n = block; n != null; n = last.next()) {
            n.allValuesDo(this);
            if (n.subst() != n && last != null) {
                last.setNext(n.next(), n.next().bci());
            } else {
                last = n;
            }
        }
    }

    public Instruction apply(Instruction i) {
        if (i != null) {
            return i.subst();
        }
        return i;
    }
}
