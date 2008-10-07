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
/*VCSID=89da06f8-f73f-43fd-9849-05ea80e32108*/

package com.sun.max.vm.compiler.tir;

import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.tir.pipeline.*;


public class TirBuiltinCall extends TirCall {
    private final Builtin _builtin;

    public TirBuiltinCall(Builtin builtin, TirInstruction... arguments) {
        super(builtin.classMethodActor(), arguments);
        _builtin = builtin;
    }

    @Override
    public void accept(TirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "BUILTIN " + method().name().toString();
    }

    public Builtin builtin() {
        return _builtin;
    }

    @Override
    public boolean isLiveIfUnused() {
        return _builtin.hasSideEffects();
    }
}
