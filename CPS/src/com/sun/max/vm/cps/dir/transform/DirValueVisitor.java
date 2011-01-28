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
package com.sun.max.vm.cps.dir.transform;

import com.sun.max.vm.cps.dir.*;

/**
 * Abstract helper for visiting the values defined and used by a DIR instruction.
 *
 * @author Doug Simon
 */
public abstract class DirValueVisitor implements DirVisitor {

    public abstract void visitDef(DirVariable variable);
    public abstract void visitUse(DirValue value);

    public void visitAssign(DirAssign dirAssign) {
        visitDef(dirAssign.destination());
        visitUse(dirAssign.source());
    }

    public void visitCall(DirCall dirCall) {
        if (dirCall.result() != null) {
            visitDef(dirCall.result());
        }
        for (DirValue argument : dirCall.arguments()) {
            visitUse(argument);
        }
    }

    public void visitBuiltinCall(DirBuiltinCall dirBuiltinCall) {
        visitCall(dirBuiltinCall);
    }

    public void visitGoto(DirGoto dirGoto) {
    }

    public void visitMethodCall(DirMethodCall dirMethodCall) {
        visitUse(dirMethodCall.method());
        visitCall(dirMethodCall);
    }

    public void visitReturn(DirReturn dirReturn) {
        if (dirReturn.returnValue() != null) {
            visitUse(dirReturn.returnValue());
        }
    }

    public void visitSwitch(DirSwitch dirSwitch) {
        visitUse(dirSwitch.tag());
        for (DirValue match : dirSwitch.matches()) {
            visitUse(match);
        }
    }

    public void visitThrow(DirThrow dirThrow) {
        visitUse(dirThrow.throwable());
    }

    public void visitInfopoint(DirInfopoint dirSafepoint) {
        for (DirValue value : dirSafepoint.javaFrameDescriptor().locals) {
            visitUse(value);
        }
        for (DirValue value : dirSafepoint.javaFrameDescriptor().stackSlots) {
            visitUse(value);
        }
    }
}
