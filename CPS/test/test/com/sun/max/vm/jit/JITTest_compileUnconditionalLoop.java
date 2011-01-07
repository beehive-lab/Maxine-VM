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
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.cps.*;
import test.com.sun.max.vm.cps.bytecode.*;

import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * Testing the JIT-compiler with methods performing loop. This tests the JIT support for the goto bytecode.
 *
 * @author Laurent Daynes
 */
public class JITTest_compileUnconditionalLoop extends JitCompilerTestCase {

    void perform_loop() {
        int a = 0;
        while (true) {
            a = a << 2;
        }
    }

    public void test_perform_loop() {
        final TargetMethod targetMethod = compileMethod("perform_loop", SignatureDescriptor.VOID);
        new BytecodeConfirmation(targetMethod.classMethodActor()) {
            @Override
            public void goto_(int offset) {
                confirmPresence();
                // We're testing with backward branch only
                assert offset <= 0;
            }
        };
    }
}
