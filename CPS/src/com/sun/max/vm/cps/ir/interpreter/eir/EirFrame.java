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
package com.sun.max.vm.cps.ir.interpreter.eir;

import com.sun.max.vm.cps.eir.*;

/**
 * A frame encapsulates the interpreter context of a single method.
 */
public class EirFrame {
    private final EirFrame caller;
    private final EirMethod method;

    public EirFrame(EirFrame caller, EirMethod method) {
        this.caller = caller;
        this.method = method;
    }

    public EirFrame caller() {
        return caller;
    }

    public EirMethod method() {
        return method;
    }

    public EirABI abi() {
        return method.abi;
    }

    private EirBlock catchBlock;

    public EirBlock catchBlock() {
        return catchBlock;
    }

    public void setCatchBlock(EirBlock catchBlock) {
        this.catchBlock = catchBlock;
    }

}
