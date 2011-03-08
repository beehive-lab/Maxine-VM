/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.c0x;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;

/**
 * The {@code C0XCompiler} class definition.
 *
 * @author Ben L. Titzer
 */
public class C0XCompiler {

    /**
     * The target that this compiler has been configured for.
     */
    public final CiTarget target;
    /**
     * The runtime that this compiler has been configured for.
     */
    public final RiRuntime runtime;

    public C0XCompiler(RiRuntime runtime, CiTarget target) {
        this.runtime = runtime;
        this.target = target;
    }

    public CiResult compileMethod(RiMethod method, int osrBCI, RiXirGenerator xirGenerator) {
        C0XCompilation comp = new C0XCompilation(runtime, method, target);
        comp.compile();
        return null;
    }
}
