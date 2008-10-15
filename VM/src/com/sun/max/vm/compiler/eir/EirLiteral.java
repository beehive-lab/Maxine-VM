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
package com.sun.max.vm.compiler.eir;

import com.sun.max.asm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class EirLiteral extends EirLocation.Constant {

    private final int _index;
    private final Label _label = new Label();

    /**
     * Creates an object representing a scalar or reference constant used by a target instruction.
     * 
     * @param index
     *                if {@code value} is a scalar, then this is the index of the first byte of the scalar value in a
     *                byte array encoding all the scalar literals allocated from the same {@link EirLiteralPool}. If
     *                {@code value} is a reference, then this is the index of the reference in the object array holding
     *                all the reference literals allocated from the same {@link EirLiteralPool}.
     * @param value
     *                a scalar or reference value
     */
    EirLiteral(int index, Value value) {
        super(value);
        _index = index;
    }

    public int index() {
        return _index;
    }

    @Override
    public EirLocationCategory category() {
        return EirLocationCategory.LITERAL;
    }

    @Override
    public String toString() {
        return value().kind().character() + ":" + _index + "(" + value().toString() + ")";
    }

    public Label asLabel() {
        return _label;
    }

    @Override
    public TargetLocation toTargetLocation() {
        if (value().kind() == Kind.REFERENCE) {
            return new TargetLocation.ReferenceLiteral(_index);
        }
        return new TargetLocation.ScalarLiteral(_index);
    }

}
