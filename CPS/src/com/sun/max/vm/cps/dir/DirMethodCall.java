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
package com.sun.max.vm.cps.dir;

import com.sun.cri.bytecode.*;
import com.sun.max.vm.cps.dir.transform.*;

/**
 * @author Bernd Mathiske
 */
public class DirMethodCall extends DirCall<DirValue> {

    private final DirValue method;

    public DirValue method() {
        return method;
    }

    private final boolean isNative;

    /**
     * Determines if this is a call to a native function. Note, this does not mean a call to a native method, but the
     * call inside a native method's stub to the actual native code. This will be the translation of the
     * {@link Bytecodes#JNICALL} instruction.
     */
    public boolean isNative() {
        return isNative;
    }

    public DirMethodCall(DirVariable result, DirValue method, DirValue[] arguments, DirCatchBlock catchBlock, boolean isNativeCall, DirJavaFrameDescriptor javaFrameDescriptor) {
        super(result, arguments, catchBlock, javaFrameDescriptor);
        this.method = method;
        this.isNative = isNativeCall;
    }

    @Override
    protected DirValue procedure() {
        return method;
    }

    @Override
    public int hashCodeForBlock() {
        return super.hashCodeForBlock() ^ method.hashCodeForBlock();
    }

    @Override
    public void acceptVisitor(DirVisitor visitor) {
        visitor.visitMethodCall(this);
    }

    @Override
    public String toString() {
        if (isNative) {
            return super.toString() + " <native function call>";
        }
        return super.toString();
    }
}
