/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.debug;

import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;


/**
 * A variant code pointer that knows how to locate instances of
 * {@link TargetMethod}s when running in the Inspector.
 */
public final class TeleNativeOrVmIP extends NativeOrVmIP {

    private final TeleVM vm;

    /**
     * Creates a variant code pointer implementation that can locate
     * compiled code associated with an address when running in the
     * context of the inspector.
     */
    public TeleNativeOrVmIP(TeleVM vm) {
        super();
        this.vm = vm;
    }

    /**
     * {@inheritDoc}
     * <p>
     * When running the Inspector, the code pointer resolves addresses
     * to compilations using the Inspector's model of what's in the
     * VM's code cache.
     */
    @Override
    protected TargetMethod targetMethodFor(Pointer pointer) {
        final TeleCompilation compilation = vm.machineCode().findCompilation(pointer);
        if (compilation != null) {
            return compilation.teleTargetMethod().targetMethod();
        }
        return null;
    }

}
