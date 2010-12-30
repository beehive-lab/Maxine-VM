/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.vm.cps.cir.operator;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.cps.b.c.*;
import com.sun.max.vm.cps.cir.operator.JavaOperator.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.type.*;

public class InvokeVirtual extends JavaResolvableOperator<VirtualMethodActor> {

    public InvokeVirtual(ConstantPool constantPool, int index) {
        super(CALL_STOP | NULL_POINTER_CHECK, constantPool, index, constantPool.methodAt(index).signature(constantPool).resultKind());
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
