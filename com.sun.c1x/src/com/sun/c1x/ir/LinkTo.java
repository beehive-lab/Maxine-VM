/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.max.cri.intrinsics.IntrinsicIDs;
import com.oracle.max.criutils.LogStream;
import com.sun.c1x.value.FrameState;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ci.CiUtil;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiSignature;

/**
 * Instruction implementing the semantics of {@link IntrinsicIDs#LINKTOINTERFACE}, {@link IntrinsicIDs#LINKTOSPECIAL},
 * and {@link IntrinsicIDs#LINKTOVIRTUAL}.
 */
public final class LinkTo extends StateSplit {

    private final RiMethod target;
    private final Value[] arguments;
    private final String intrinsic;

    /**
     * Creates a {@link LinkTo} instance.
     * @param intrinsic
     * @param target
     * @param arguments
     * @param stateBefore
     */
    public LinkTo(String intrinsic, RiMethod target, Value[] arguments, FrameState stateBefore) {
        super(target.signature().returnKind(true), stateBefore);
        this.target = target;
        this.arguments = arguments;
        this.intrinsic = intrinsic;
        setFlag(Flag.LiveSideEffect);
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitLinkTo(this);
    }

    @Override
    public void print(LogStream out) {
        out.print(" LinkTo ");
    }

    public RiMethod target() {
        return target;
    }

    public Value[] arguments() {
        Value[] arguments = new Value[this.arguments.length - 1];
        System.arraycopy(this.arguments, 0, arguments, 0, arguments.length); // Pop of the membername
        return arguments;
    }

    public String intrinsic() {
        return intrinsic;
    }

    public CiKind[] signature() {
        RiSignature signature = target.signature();
        CiKind[] argumentKinds = CiUtil.signatureToKinds(signature, null);
        CiKind[] argumentKindsWithoutMembername = new CiKind[argumentKinds.length - 1];
        System.arraycopy(argumentKinds, 0, argumentKindsWithoutMembername, 0, argumentKindsWithoutMembername.length);
        return argumentKindsWithoutMembername;
    }

    public Value receiver() {
        return arguments[0];
    }

    public Value memberName() {
        return arguments[arguments.length - 1];
    }
}
