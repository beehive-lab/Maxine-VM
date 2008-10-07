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
/*VCSID=3b99bff0-1589-4ded-b824-6f3055c441fb*/
package com.sun.max.vm.compiler.tir;

import com.sun.max.vm.compiler.tir.pipeline.*;
import com.sun.max.vm.type.*;

public class TirNestedLocal extends TirInstruction {
    private final TirTreeCall _call;
    private final int _slot;

    public TirNestedLocal(TirTreeCall call, int slot) {
        _call = call;
        _slot = slot;
    }

    @Override
    public void accept(TirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "NESTED LOCAL slot: " + _slot;
    }

    private Kind _kind = Kind.VOID;

    @Override
    public Kind kind() {
        return _kind;
    }

    @Override
    public void setKind(Kind kind) {
        _kind = kind;
    }

    public int slot() {
        return _slot;
    }

    public TirTreeCall call() {
        return _call;
    }
}
