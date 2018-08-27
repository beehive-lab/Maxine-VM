/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.c1x.ir;

import com.oracle.max.criutils.LogStream;
import com.sun.c1x.value.MutableFrameState;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ci.CiUtil;
import com.sun.cri.ri.*;

/**
 * The {@code InvokeHandle} instruction is used for methodHandle invocations.
 */
public final class InvokeHandle extends StateSplit {

    private final RiResolvedMethod target;
    private final int cpi;
    private final RiConstantPool constantPool;
    private final Value[] arguments;

    public InvokeHandle(RiResolvedMethod target, Value[] arguments, int cpi, RiConstantPool constantPool, MutableFrameState stateBefore, CiKind returnKind) {
        super(returnKind, stateBefore);
        this.target = target;
        this.arguments = arguments;
        this.cpi = cpi;
        this.constantPool = constantPool;
    }

    @Override
    public void inputValuesDo(ValueClosure closure) {
        for (int i = 0; i < arguments.length; i++) {
            Value arg = arguments[i];
            if (arg != null) {
                arguments[i] = closure.apply(arg);
                assert arguments[i] != null;
            }
        }
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitInvokeHandle(this);
    }

    @Override
    public void print(LogStream out) {
        out.print("invokeHandle");
    }

    public RiResolvedMethod target() {
        return target;
    }

    public int cpi() {
        return cpi;
    }

    public RiConstantPool constantPool() {
        return constantPool;
    }

    public Value[] arguments() {
        return arguments;
    }

    public CiKind[] signature() {
        return CiUtil.signatureToKinds(target.signature(), null);
    }
}
