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
package test.com.sun.max.vm.jit.amd64;

import static com.sun.max.platform.Platform.*;
import junit.framework.*;
import test.com.sun.max.vm.cps.amd64.*;
import test.com.sun.max.vm.jit.*;

import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.cps.jit.amd64.*;
import com.sun.max.vm.cps.template.*;
import com.sun.max.vm.hosted.*;

/**
 * Test setup for JIT tests on AMD64.
 *
 * @author Laurent Daynes
 */
public class AMD64JITTestSetup extends AMD64TranslatorTestSetup implements JITTestSetup {
    public AMD64JITTestSetup(Test test) {
        super(test);
    }

    @Override
    protected void initializeVM() {
        Platform.set(platform().constrainedByInstructionSet(ISA.AMD64));
        CompilationScheme.optimizingCompilerOption.setValue(CPSCompiler.Static.defaultCPSCompilerClassName());
        VMConfigurator.installStandard(BuildLevel.DEBUG);
    }

    public JitCompiler newJitCompiler(TemplateTable templateTable) {
        return new AMD64JitCompiler(templateTable);
    }

    public boolean disassembleCompiledMethods() {
        return true;
    }
}
