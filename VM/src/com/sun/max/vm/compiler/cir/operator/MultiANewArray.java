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

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.type.*;


public class MultiANewArray extends JavaOperator {
    private final int _index;
    private final ConstantPool _constantPool;
    private final int _ndimension;

    @CONSTANT_WHEN_NOT_ZERO
    private ArrayClassActor _arrayClassActor;

    public MultiANewArray(ConstantPool constantPool, int index, int dimensions) {
        _constantPool = constantPool;
        _index = index;
        _ndimension = dimensions;
        final ClassConstant classConstant = constantPool.classAt(index);
        if (classConstant.isResolvableWithoutClassLoading(_constantPool)) {
            _arrayClassActor = (ArrayClassActor) classConstant.resolve(constantPool, index);
        } else {
            _arrayClassActor = null;
        }
    }

    public boolean isResolved() {
        return _arrayClassActor != null;
    }

    public void resolve() {
        if (!isResolved()) {
            _arrayClassActor = (ArrayClassActor) _constantPool.classAt(_index).resolve(_constantPool, _index);
        }
    }

    @Override
    public Kind resultKind() {
        return Kind.REFERENCE;
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitHCirOperator(this);
    }

    @Override
    public void acceptVisitor(HCirOperatorVisitor visitor) {
        visitor.visit(this);
    }

    public int index() {
        return _index;
    }

    public ConstantPool constantPool() {
        return _constantPool;
    }


    public ArrayClassActor arrayClassActor() {
        return _arrayClassActor;
    }

    public int ndimension() {
        return _ndimension;
    }
    @Override
    public String toString() {
        return "Multianewarray";
    }
}
