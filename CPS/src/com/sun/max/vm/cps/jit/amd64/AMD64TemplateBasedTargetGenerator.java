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
package com.sun.max.vm.cps.jit.amd64;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.cps.template.*;

/**
 * Implementation of template-based target generator for AMD64.
 *
 * @author Laurent Daynes
 */
public class AMD64TemplateBasedTargetGenerator extends TemplateBasedTargetGenerator {

    public AMD64TemplateBasedTargetGenerator(JitCompiler jitCompiler) {
        super(jitCompiler, ISA.AMD64);
    }

    @Override
    public AMD64JitTargetMethod createIrMethod(ClassMethodActor classMethodActor) {
        final AMD64JitTargetMethod targetMethod = new AMD64JitTargetMethod(classMethodActor);
        notifyAllocation(targetMethod);
        return targetMethod;
    }

    private static final int NUMBER_OF_BYTES_PER_BYTECODE = 16;

    @Override
    protected BytecodeToTargetTranslator makeTargetTranslator(ClassMethodActor classMethodActor) {
        // allocate a buffer that is likely to be large enough, based on a linear expansion
        final int estimatedSize = classMethodActor.codeAttribute().code().length * NUMBER_OF_BYTES_PER_BYTECODE;
        final CodeBuffer codeBuffer = new CodeBuffer(estimatedSize);
        return new BytecodeToAMD64TargetTranslator(classMethodActor, codeBuffer, templateTable(), false);
    }
}
