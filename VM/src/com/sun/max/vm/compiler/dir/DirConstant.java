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
/*VCSID=ddc92322-4bc2-41fe-a8f1-4be1925e3583*/
package com.sun.max.vm.compiler.dir;

import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A value that is constant at compile time.
 * 
 * @author Bernd Mathiske
 */
public class DirConstant extends DirValue {

    private final Value _value;

    public DirConstant(Value value) {
        _value = value;
    }

    public Kind kind() {
        return _value.kind();
    }

    @Override
    public Value value() {
        return _value;
    }

    public boolean isConstant() {
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DirConstant) {
            final DirConstant dirConstant = (DirConstant) other;
            return _value.equals(dirConstant._value);
        }
        return false;
    }

    @Override
    public int hashCodeForBlock() {
        if (_value.kind() == Kind.REFERENCE) {
            return super.hashCodeForBlock();
        }
        return super.hashCodeForBlock() ^ _value.hashCode();
    }

    @Override
    public String toString() {
        return _value.toString();
    }

    public static final DirConstant VOID = new DirConstant(VoidValue.VOID);
}
