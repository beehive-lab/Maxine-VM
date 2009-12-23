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
package com.sun.max.vm.compiler.cps.cir.transform;

import com.sun.max.vm.compiler.cps.cir.*;

/**
 * Lifts the continuations of the first CirSwitch nodes
 * that are reachable from the given node to blocks
 * to facilitate their being shared,
 * not replicated throughout further transformations.
 *
 * There may be several "first" CirSwitch nodes
 * encountered along different continuations
 * (taking exceptions into account).
 * The algorithm stops at each CirSwitch, though,
 * not searching any further throughout the CirSwitch arguments.
 *
 * @author Bernd Mathiske
 */
public final class CirSwitchEncapsulation extends CirTraversal {

    public CirSwitchEncapsulation(CirNode node) {
        super(node);
    }

    private void encapsulateSwitchArguments(CirValue[] arguments) {
        for (int i = arguments.length >> 1; i < arguments.length; i++) { // i.e., for all continuation arguments
            if (arguments[i] instanceof CirClosure) {
                final CirClosure continuation = (CirClosure) arguments[i];
                continuation.makeBlockCall();
            }
        }
    }

    @Override
    public void visitCall(CirCall call) {
        if (call.procedure() instanceof CirSwitch) {
            encapsulateSwitchArguments(call.arguments());
        } else {
            super.visitCall(call);
        }
    }

    public static void apply(CirNode node) {
        final CirSwitchEncapsulation firstSwitchBlockLifting = new CirSwitchEncapsulation(node);
        firstSwitchBlockLifting.run();
    }

}
