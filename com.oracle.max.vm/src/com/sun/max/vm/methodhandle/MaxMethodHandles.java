/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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

package com.sun.max.vm.methodhandle;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.UNSAFE_CAST;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

public final class MaxMethodHandles {

    @INTRINSIC(UNSAFE_CAST)
    private static native MaxMethodHandles asThis(Object o);

    @ALIAS(declaringClass=java.lang.invoke.MethodHandle.class, descriptor="Ljava/lang/invoke/LambdaForm;")
    private Object form;

    @ALIAS(declaringClassName="java.lang.invoke.LambdaForm", descriptor="Ljava/lang/invoke/MemberName;")
    private Object vmentry;

    /**
     * Extract the MethodActor from a method handle to handle an invokeBasic
     * call, i.e. navigate
     * MH->form->vmentry->MethodActor (injected)
     * @param mh
     * @return The MethodActor
     */
    public static ClassMethodActor getInvokerForInvokeBasic(Object mh) {
        Trace.begin(1, "MaxMethodHandles.getInvokerForInvokeBasic:");
        Object lambdaForm = asThis(mh).form;
        Object memberName = asThis(lambdaForm).vmentry;
        VMTarget target = VMTarget.fromMemberName(memberName);
        assert(target != null);
        Trace.end(1, "MaxMethodHandles.getInvokerForInvokeBasic");
        return UnsafeCast.asClassMethodActor(target.getVmTarget());
    }

}
