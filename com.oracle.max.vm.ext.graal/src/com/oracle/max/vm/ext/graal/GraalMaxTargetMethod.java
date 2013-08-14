/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.stack.*;


public class GraalMaxTargetMethod extends MaxTargetMethod {

    private GraalMaxTargetMethod(ClassMethodActor classMethodActor, CiTargetMethod ciTargetMethod, boolean install) {
        super(classMethodActor, ciTargetMethod, install);
    }

    @NEVER_INLINE
    public static MaxTargetMethod create(ClassMethodActor classMethodActor, CiTargetMethod ciTargetMethod, boolean install) {
        return new GraalMaxTargetMethod(classMethodActor, ciTargetMethod, install);
    }

    @Override
    protected boolean needsTrampolineRefMapOverflowArgHandling() {
        return true;
    }

    /**
     * Overflow reference parameters in the caller frame must be placed in the reference map, in case a GC happens
     * while resolving a trampoline. Such a GC can't happen in a normal Graal VM, as all calls are resolved,
     * so nothing needs to be done in the caller. Not so in Maxine.
     *
     */
    @Override
    @NEVER_INLINE
    protected void prepareTrampolineRefMapHandleOverflowParam(StackFrameCursor current, ClassMethodActor calledMethod, int offset, FrameReferenceMapVisitor preparer) {
        preparer.visitReferenceMapBits(current, current.sp().plus(offset), 1, 1);
    }

    @Override
    public boolean deoptOnImplicitException() {
        return true;
    }

}
