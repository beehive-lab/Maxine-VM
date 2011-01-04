/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.cir.transform;

import com.sun.max.vm.cps.cir.*;

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
