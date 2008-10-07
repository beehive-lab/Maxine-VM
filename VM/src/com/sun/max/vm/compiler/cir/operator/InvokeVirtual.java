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

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.type.*;


public class InvokeVirtual extends JavaOperator {
    private final ConstantPool _constantPool;
    private final int _index;
    private final Kind _returnKind;
    private VirtualMethodActor _virtualMethodActor;
    private final BirToCirMethodTranslation _translation;
    private final BlockState _blockState;
    public boolean isResolved() {
        return _virtualMethodActor != null;
    }
    public void resolve() {
        constantPool().methodAt(index()).resolve(constantPool(), index());
    }

    public VirtualMethodActor virtualMethodActor() {
        return _virtualMethodActor;
    }

    public InvokeVirtual(ConstantPool constantPool, int index, BirToCirMethodTranslation translation, BlockState blockState) {
        _constantPool = constantPool;
        _index = index;
        _returnKind = constantPool.methodAt(index).signature(constantPool).getResultKind();
        _translation = translation;
        _blockState = blockState;
        final MethodRefConstant ref = constantPool.methodAt(index);
        if (ref.isResolved()) {
            _virtualMethodActor = (VirtualMethodActor) ref.resolve(constantPool, index);
        } else {
            _virtualMethodActor = null;
        }
    }

    @Override
    public Kind resultKind() {
        return _returnKind;
    }

    @Override
    public boolean needsJavaFrameDescriptor() {
        return true;
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitHCirOperator(this);
    }

    @Override
    public void acceptVisitor(HCirOperatorVisitor visitor) {
        visitor.visit(this);
    }
    public ConstantPool constantPool() {
        return _constantPool;
    }
    public int index() {
        return _index;
    }
    public BirToCirMethodTranslation methodTranslation() {
        return _translation;
    }
    public BlockState blockState() {
        return _blockState;
    }

    @Override
    public String toString() {
        return "Invokevirtual <" + (isResolved() ? _virtualMethodActor : _index) + ">";
    }
}
