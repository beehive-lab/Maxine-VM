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

/**
 * {@link HCirOperatorVisitor} defines general purpose visitor for visiting
 * {@link JavaOperator}s.  See {@link HCirOperatorLowering} for a
 * usage example.
 *
 * @author Yi Guo
 * @author Aziz Ghuloum
 */
public abstract class HCirOperatorVisitor {
    public abstract void visit(JavaOperator op);
    public abstract void visit(GetField op);
    public abstract void visit(PutField op);
    public abstract void visit(GetStatic op);
    public abstract void visit(PutStatic op);
    public abstract void visit(InvokeVirtual op);
    public abstract void visit(InvokeInterface op);
    public abstract void visit(InvokeSpecial op);
    public abstract void visit(InvokeStatic op);
    public abstract void visit(CheckCast op);
    public abstract void visit(New op);
    public abstract void visit(ArrayStore op);
    public abstract void visit(ArrayLoad op);
    public abstract void visit(InstanceOf op);
    public abstract void visit(NewArray op);
    public abstract void visit(MultiANewArray op);
    public abstract void visit(ArrayLength op);
    public abstract void visit(MonitorEnter op);
    public abstract void visit(MonitorExit op);
    public abstract void visit(Mirror op);
    public abstract void visit(CallNative op);
    public abstract void visit(Call op);
}
