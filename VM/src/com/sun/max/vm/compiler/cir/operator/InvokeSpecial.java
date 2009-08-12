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
import com.sun.max.vm.compiler.cir.operator.JavaOperator.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.type.*;


public class InvokeSpecial extends JavaResolvableOperator<MethodActor> {

    public InvokeSpecial(ConstantPool constantPool, int index) {
        super(CALL | NULL_POINTER_CHECK, constantPool, index, constantPool.classMethodAt(index).signature(constantPool).resultKind());
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitHCirOperator(this);
    }

    @Override
    public void acceptVisitor(HCirOperatorVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Kind[] parameterKinds() {
        final MethodRefConstant method = constantPool.methodAt(index);
        final Kind[] kinds = method.signature(constantPool).copyParameterKinds(null, 1);
        kinds[0] = method.holder(constantPool).toKind();
        return kinds;
    }
}
