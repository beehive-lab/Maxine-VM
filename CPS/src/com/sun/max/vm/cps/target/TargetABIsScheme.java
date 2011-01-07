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
package com.sun.max.vm.cps.target;

import static com.sun.max.platform.Platform.*;

import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 */
public abstract class TargetABIsScheme<IntegerRegister_Type extends Symbol, FloatingPointRegister_Type extends Symbol> extends AbstractVMScheme implements VMScheme {

    public static final TargetABIsScheme INSTANCE;
    static {
        TargetABIsScheme scheme = null;
        try {
            final String isa = platform().isa.name();
            final Class<?> c = Class.forName("com.sun.max.vm.cps.target." + isa.toLowerCase() + "." + isa + TargetABIsScheme.class.getSimpleName());
            scheme = (TargetABIsScheme) c.newInstance();
        } catch (Exception exception) {
            throw FatalError.unexpected("could not create TrapStateAccess", exception);
        }
        INSTANCE = scheme;
    }

    public boolean usingRegisterWindows() {
        return false;
    }

    public final TargetABI<IntegerRegister_Type, FloatingPointRegister_Type> nativeABI;

    public final TargetABI<IntegerRegister_Type, FloatingPointRegister_Type> jitABI;

    public final TargetABI<IntegerRegister_Type, FloatingPointRegister_Type> optimizedJavaABI;

    protected TargetABIsScheme(TargetABI<IntegerRegister_Type, FloatingPointRegister_Type> nativeABI,
                               TargetABI<IntegerRegister_Type, FloatingPointRegister_Type> jitABI,
                               TargetABI<IntegerRegister_Type, FloatingPointRegister_Type> optimizedJavaABI) {
        this.nativeABI = nativeABI;
        this.jitABI = jitABI;
        this.optimizedJavaABI = optimizedJavaABI;
    }
}
