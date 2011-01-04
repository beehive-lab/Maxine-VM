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
package com.sun.max.vm.cps.b.c;

import com.sun.max.vm.cps.cir.operator.*;

public class HCirOperatorDefaultVisitor extends HCirOperatorVisitor {
    protected void visitDefault(JavaOperator op) {
    }

    @Override
    public void visit(JavaOperator op) {
        visitDefault(op);
    }

    @Override
    public void visit(GetField op) {
        visitDefault(op);
    }

    @Override
    public void visit(PutField op) {
        visitDefault(op);
    }

    @Override
    public void visit(GetStatic op) {
        visitDefault(op);
    }

    @Override
    public void visit(PutStatic op) {
        visitDefault(op);
    }

    @Override
    public void visit(InvokeVirtual op) {
        visitDefault(op);
    }

    @Override
    public void visit(InvokeInterface op) {
        visitDefault(op);
    }

    @Override
    public void visit(InvokeSpecial op) {
        visitDefault(op);
    }

    @Override
    public void visit(InvokeStatic op) {
        visitDefault(op);
    }

    @Override
    public void visit(CheckCast op) {
        visitDefault(op);
    }

    @Override
    public void visit(New op) {
        visitDefault(op);
    }

    @Override
    public void visit(ArrayStore op) {
        visitDefault(op);
    }

    @Override
    public void visit(ArrayLoad op) {
        visitDefault(op);
    }

    @Override
    public void visit(InstanceOf op) {
        visitDefault(op);
    }

    @Override
    public void visit(NewArray op) {
        visitDefault(op);
    }

    @Override
    public void visit(MultiANewArray op) {
        visitDefault(op);
    }
    @Override
    public void visit(ArrayLength op) {
        visitDefault(op);
    }

    @Override
    public void visit(MonitorEnter op) {
        visitDefault(op);
    }

    @Override
    public void visit(MonitorExit op) {
        visitDefault(op);
    }

    @Override
    public void visit(Mirror op) {
        visitDefault(op);
    }

    @Override
    public void visit(CallNative op) {
        visitDefault(op);
    }

    @Override
    public void visit(Call op) {
        visitDefault(op);
    }
}
