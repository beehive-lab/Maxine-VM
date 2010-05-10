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

import com.sun.c1x.graph.IR;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;

/**
 * The {@code PhiSimplifier} class is a helper class that can reduce phi instructions.
 *
 * @author Ben L. Titzer
 */
public class PhiSimplifier implements BlockClosure {

    final IR ir;
    final InstructionSubstituter subst;

    public PhiSimplifier(IR ir) {
        this.ir = ir;
        this.subst = new InstructionSubstituter(ir);
        ir.startBlock.iterateAnyOrder(this, false);
        subst.finish();
    }

    /**
     * This method is called for each block and processes any phi statements in the block.
     * @param block the block to apply the simplification to
     */
    public void apply(BlockBegin block) {
        FrameState state = block.stateBefore();
        for (int i = 0; i < state.stackSize(); i++) {
            simplify(state.stackAt(i));
        }
        for (int i = 0; i < state.localsSize(); i++) {
            simplify(state.localAt(i));
        }
    }

    Value simplify(Value x) {
        if (x == null || !(x instanceof Phi)) {
            return x;
        }
        Phi phi = (Phi) x;
        if (phi.hasSubst()) {
            // already substituted, but the subst could be a phi itself, so simplify
            return simplify(subst.getSubst(phi));
        } else if (phi.checkFlag(Value.Flag.PhiCannotSimplify)) {
            // already tried, cannot simplify this phi
            return phi;
        } else if (phi.checkFlag(Value.Flag.PhiVisited)) {
            // break cycles in phis
            return phi;
        } else if (phi.isIllegal()) {
            // don't bother with illegals
            return phi;
        } else {
            // attempt to simplify the phi by recursively simplifying its operands
            phi.setFlag(Value.Flag.PhiVisited);
            Value phiSubst = null;
            int max = phi.operandCount();
            for (int i = 0; i < max; i++) {
                Value oldInstr = phi.operandAt(i);

                if (oldInstr == null || oldInstr.isIllegal() || oldInstr.isDeadPhi()) {
                    // if one operand is illegal, make the entire phi illegal
                    phi.makeDead();
                    phi.clearFlag(Value.Flag.PhiVisited);
                    return phi;
                }

                // attempt to simplify this operand
                Value newInstr = simplify(oldInstr);

                if (newInstr != phi && newInstr != phiSubst) {
                    if (phiSubst == null) {
                        phiSubst = newInstr;
                        continue;
                    }
                    // this phi cannot be simplified
                    phi.setFlag(Value.Flag.PhiCannotSimplify);
                    phi.clearFlag(Value.Flag.PhiVisited);
                    return phi;
                }
            }
            // successfully simplified the phi
            assert phiSubst != null : "illegal phi function";
            phi.clearFlag(Value.Flag.PhiVisited);
            subst.setSubst(phi, phiSubst);
            return phiSubst;
        }
    }
}
