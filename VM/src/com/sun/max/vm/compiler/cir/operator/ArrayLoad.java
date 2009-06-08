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
package com.sun.max.vm.compiler.cir.operator;

import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.type.*;


public class ArrayLoad extends JavaOperator {

    private final Kind _kind;

    public ArrayLoad(Kind kind) {
        super(CALL | NULL_POINTER_CHECK | ARRAY_BOUNDS_CHECK);
        _kind = kind;
    }

    public Kind resultKind() {
        return _kind;
    }

    @Override
    public void acceptVisitor(HCirOperatorVisitor visitor) {
        visitor.visit(this);
    }

    private static final Kind[] _parameterKinds = {Kind.REFERENCE, Kind.INT};

    public Kind[] parameterKinds() {
        return _parameterKinds;
    }

    public String toString() {
        return "ArrayLoad";
    }
}
