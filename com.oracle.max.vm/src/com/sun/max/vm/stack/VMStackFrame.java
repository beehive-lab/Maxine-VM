/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.stack;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

/**
 * A {@code CompiledStackFrame} object abstracts an activation frame on a call stack for a method compiled by the VM.
 */
public abstract class VMStackFrame extends StackFrame {

    public final VMFrameLayout layout;

    private final TargetMethod targetMethod;

    public VMStackFrame(StackFrame callee, TargetMethod targetMethod, Pointer ip, Pointer fp, Pointer sp) {
        super(callee, ip, sp, fp);
        this.layout = targetMethod.frameLayout();
        this.targetMethod = targetMethod;
    }

    @Override
    public TargetMethod targetMethod() {
        return targetMethod;
    }

    /**
     * {@inheritDoc}
     *
     * The given stack frame is the same as the one represented by this object if all the following conditions hold:
     * <ul>
     * <li>Both frames have a known canonical frame pointer and its value is the same for both frames.</li>
     * <li>Both frames denote the same target method.</li>
     * </ul>
     * Other frame attributes such as the {@linkplain #ip} and the value in each frame slot may differ
     * for the two frames.
     */
    @Override
    public abstract boolean isSameFrame(StackFrame stackFrame);

    @Override
    public String toString() {
        ClassMethodActor classMethodActor = targetMethod.classMethodActor();
        String offset = "[+" + targetMethod.posFor(ip) + "]";
        if (classMethodActor != null) {
            return classMethodActor.format("%H.%n(%p)@") + ip.toHexString() + offset;
        }
        return targetMethod.regionName() + "@" + ip.toHexString() + offset;
    }

}
