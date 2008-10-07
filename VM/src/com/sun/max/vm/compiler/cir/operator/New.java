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

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.type.*;


public class New extends JavaOperator {
    private final int _index;
    private final ConstantPool _constantPool;
    private ClassActor _classActor;

    public New(ConstantPool constantPool, int index) {
        _constantPool = constantPool;
        _index = index;
        final ClassConstant classConstant = constantPool.classAt(index);
        if (classConstant.isResolved() || classConstant.isResolvableWithoutClassLoading(constantPool)) {
            _classActor = classConstant.resolve(constantPool, index);
        } else {
            _classActor = null;
        }
    }

    public boolean isResolved() {
        return _classActor != null;
    }

    public void resolve() {
        _classActor = _constantPool.classAt(_index).resolve(_constantPool, _index);
    }

    public boolean isClassInitialized() {
        if (!isResolved()) {
            return false;
        }
        return _classActor.isInitialized();
    }

    public void initializeClass() {
        if (!isClassInitialized()) {
            if (!isResolved()) {
                resolve();
            }
            _classActor.makeInitialized();
        }
    }

    @Override
    public Kind resultKind() {
        return Kind.REFERENCE;
    }

    public int index() {
        return _index;
    }

    public ConstantPool constantPool() {
        return _constantPool;
    }

    public ClassActor classActor() {
        return _classActor;
    }

    @Override
    public void acceptVisitor(HCirOperatorVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitHCirOperator(this);
    }
    @Override
    public String toString() {
        return "<New_" + (isResolved() ? _classActor.toString() : _index + "") + ">";
    }
}
