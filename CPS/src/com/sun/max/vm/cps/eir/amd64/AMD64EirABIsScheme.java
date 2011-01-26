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
package com.sun.max.vm.cps.eir.amd64;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.vm.cps.eir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AMD64EirABIsScheme extends EirABIsScheme<AMD64EirRegister> {

    @HOSTED_ONLY
    protected AMD64EirABIsScheme(EirABI<AMD64EirRegister> javaABI,
                    EirABI<AMD64EirRegister> nativeABI,
                    EirABI<AMD64EirRegister> j2cFunctionABI,
                    EirABI<AMD64EirRegister> c2jFunctionABI,
                    EirABI<AMD64EirRegister> templateABI,
                    EirABI<AMD64EirRegister> treeABI) {
        super(javaABI, nativeABI, j2cFunctionABI, c2jFunctionABI, templateABI, treeABI);
    }

    @Override
    public Pool<AMD64EirRegister> registerPool() {
        return AMD64EirRegister.pool();
    }

    @Override
    public AMD64EirRegister safepointLatchRegister() {
        return javaABI.safepointLatchRegister();
    }
}
