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
package com.sun.max.vm.cps.jit;

import static com.sun.max.platform.Platform.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.b.c.d.e.amd64.target.*;
import com.sun.max.vm.runtime.*;

/**
 * Template based JIT compiler.
 *
 * @author Laurent Daynes
 */
public abstract class JitCompiler implements RuntimeCompiler {

    @HOSTED_ONLY
    private boolean isInitialized;

    private boolean createdCPSCompiler;

    @HOSTED_ONLY
    protected JitCompiler() {
        CPSCompiler compiler = CPSCompiler.Static.compiler();
        if (compiler == null) {
            if (platform().isa == ISA.AMD64) {
                compiler = new AMD64CPSCompiler();
                createdCPSCompiler = true;
            } else {
                throw FatalError.unimplemented();
            }
            assert CPSCompiler.Static.compiler() == compiler;
        }
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (createdCPSCompiler) {
            CPSCompiler.Static.compiler().initialize(phase);
        }

        if (MaxineVM.isHosted() && phase == MaxineVM.Phase.COMPILING) {
            init();
        }
    }

    @HOSTED_ONLY
    private void init() {
        synchronized (this) {
            if (!isInitialized) {
                targetGenerator().initialize();
                isInitialized = true;
            }
        }
    }

    @Override
    public CallEntryPoint calleeEntryPoint() {
        return CallEntryPoint.JIT_ENTRY_POINT;
    }

    protected abstract TemplateBasedTargetGenerator targetGenerator();

    public JitTargetMethod compile(ClassMethodActor classMethodActor) {
        if (MaxineVM.isHosted()) {
            init();
        }
        return (JitTargetMethod) targetGenerator().makeIrMethod(classMethodActor);
    }
}
