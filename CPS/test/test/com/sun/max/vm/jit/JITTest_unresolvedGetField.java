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
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.cps.*;

import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.type.*;

/**
 * A simple test to print the output produced by the compiler for the
 * sequence aload_0, getfield, ireturn, so as to compare with what
 * a template-based JIT could do.
 *
 * @author Laurent Daynes
 */
public class JITTest_unresolvedGetField extends CompilerTestCase<CPSTargetMethod> {

    private int intField;

    static class UnresolvedAtCompileTime {
        int intUnresolvedField;
    }

    public int perform_visitor(UnresolvedAtCompileTime b) {
        final int i = b.intUnresolvedField;
        return i;
    }

    public void perform_visitor2(UnresolvedAtCompileTime b) {
        @SuppressWarnings("unused")
        final int i = b.intUnresolvedField;
    }

    public void test_visitor() {
        final TargetMethod method = compileMethod("perform_visitor", SignatureDescriptor.create(int.class, UnresolvedAtCompileTime.class));
        disassemble(method);
    }
}

