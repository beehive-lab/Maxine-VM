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
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The <code>PhiSimplifier</code> class is a helper class that can reduce phi instructions.
 *
 * @author Ben L. Titzer
 */
public class PhiSimplifier implements BlockClosure {

    boolean hasSubstitutions;

    /**
     * Creates a new PhiSimplifier for the specified start block and performs phi simplification.
     * @param start the start block from which to start performing phi simplification
     */
    public PhiSimplifier(BlockBegin start) {
        start.iteratePreOrder(this);
        if (hasSubstitutions) {
            // perform substitutions
            new SubstitutionResolver(start);
        }
    }

    /**
     * This method is called for each block and processes any phi statements in the block.
     * @param block the block to apply the simplification to
     */
    public void apply(BlockBegin block) {
        ValueStack state = block.state();
        for (int i = 0; i < state.stackSize(); i++) {
            simplify(state.stackAt(i));
        }
        for (int i = 0; i < state.localsSize(); i++) {
            simplify(state.localAt(i));
        }
    }

    Instruction simplify(Instruction x) {
        if (x == null || !(x instanceof Phi)) {
            return x;
        }
        Phi phi = (Phi) x;
        if (phi.hasSubst()) {
            // already substituted, but the subst could be a phi itself, so simplify
            return simplify(phi.subst());
        } else if (phi.checkPhiFlag(Phi.PhiFlag.CannotSimplify)) {
            // already tried, cannot simplify this phi
            return phi;
        } else if (phi.checkPhiFlag(Phi.PhiFlag.Visited)) {
            // break cycles in phis
            return phi;
        } else if (phi.type().isIllegal()) {
            // don't bother with illegals
            return phi;
        } else {
            // attempt to simplify the phi by recursively simplifying its operands
            phi.setPhiFlag(Phi.PhiFlag.Visited);
            Instruction phiSubst = null;
            int max = phi.operandCount();
            for (int i = 0; i < max; i++) {
                Instruction oldInstr = phi.operandAt(i);

                if (oldInstr == null || oldInstr.type().isIllegal()) {
                    // if one operand is illegal, make the entire phi illegal
                    phi.makeIllegal();
                    phi.clearPhiFlag(Phi.PhiFlag.Visited);
                    return phi;
                }

                // attempt to simplify this operand
                Instruction newInstr = simplify(oldInstr);

                if (newInstr != phi && newInstr != phiSubst) {
                    if (phiSubst == null) {
                        phiSubst = newInstr;
                    } else {
                        // this phi cannot be simplified
                        phi.setPhiFlag(Phi.PhiFlag.CannotSimplify);
                        phi.clearPhiFlag(Phi.PhiFlag.Visited);
                        return phi;
                    }
                }
            }
            // successfully simplified the phi
            assert phiSubst != null : "illegal phi function";
            phi.clearPhiFlag(Phi.PhiFlag.Visited);
            phi.setSubst(phiSubst);
            hasSubstitutions = true;
            return phiSubst;
        }
    }
}
