/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.dir;

import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.dir.transform.*;

/**
 * A call to a compiler builtin.
 *
 * @author Bernd Mathiske
 */
public class DirBuiltinCall extends DirCall<Builtin> {

    private final Builtin builtin;

    /**
     * @param result the variable which will hold the result of the builtin execution
     * @param builtin the compiler builtin
     * @param arguments arguments for the builtin
     * @param catchBlock the block where execution continues in case of an (implicit) exception thrown by the builtin
     */
    public DirBuiltinCall(DirVariable result, Builtin builtin, DirValue[] arguments, DirCatchBlock catchBlock, DirJavaFrameDescriptor javaFrameDescriptor) {
        super(result, arguments, catchBlock, javaFrameDescriptor);
        this.builtin = builtin;
    }

    public Builtin builtin() {
        return builtin;
    }

    @Override
    protected Builtin procedure() {
        return builtin;
    }

    @Override
    public int hashCodeForBlock() {
        return super.hashCodeForBlock() ^ builtin.serial();
    }

    @Override
    public void acceptVisitor(DirVisitor visitor) {
        visitor.visitBuiltinCall(this);
    }
}
