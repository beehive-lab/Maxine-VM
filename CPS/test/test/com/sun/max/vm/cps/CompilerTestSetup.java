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
package test.com.sun.max.vm.cps;

import static com.sun.max.asm.dis.Disassembler.*;
import static com.sun.max.vm.VMConfiguration.*;
import junit.framework.*;
import test.com.sun.max.vm.*;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.platform.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.interpreter.*;

public abstract class CompilerTestSetup<Method_Type> extends VmTestSetup {

    private static CompilerTestSetup compilerTestSetup = null;

    public static CompilerTestSetup compilerTestSetup() {
        return compilerTestSetup;
    }

    protected CompilerTestSetup(Test test) {
        super(test);
        compilerTestSetup = this;
    }

    @Override
    protected void chainedSetUp() {
        super.chainedSetUp();
        vmConfig().initializeSchemes(Phase.RUNNING);
        compilerScheme().compileSnippets();
    }

    /**
     * Gets a disassembler for a given target method.
     *
     * @param targetMethod a compiled method whose {@linkplain TargetMethod#code() code} is to be disassembled
     * @return a disassembler for the ISA specific code in {@code targetMethod} or null if no such disassembler is available
     */
    public final Disassembler disassemblerFor(TargetMethod targetMethod) {
        Platform platform = Platform.platform();
        InlineDataDecoder inlineDataDecoder = InlineDataDecoder.createFrom(targetMethod.encodedInlineDataDescriptors());
        return createDisassembler(platform.isa, platform.wordWidth(), targetMethod.codeStart().toLong(), inlineDataDecoder);
    }

    public static CPSCompiler compilerScheme() {
        return CPSCompiler.Static.compiler();
    }

    public abstract Method_Type translate(ClassMethodActor classMethodActor);

    protected IrInterpreter<? extends IrMethod> createInterpreter() {
        return null;
    }

}
