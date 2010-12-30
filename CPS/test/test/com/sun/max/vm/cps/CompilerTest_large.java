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

import com.sun.cri.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;

/**
 * Tests for individual methods that exposed limits in the compiler's use of resources, mostly causing OutOfMemoryErrors.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class CompilerTest_large<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    public CompilerTest_large(String name) {
        super(name);
        // Set the system property that overrides the default behaviour of ClassfileReader when it encounters
        // a <clinit> while MaxineVM.isPrototying() returns true. The default behaviour is to discard such methods.
        System.setProperty("max.loader.preserveClinitMethods", "");
    }

    public void test_Bytecode_clinit() {
        compileMethod(Bytecodes.class, SymbolTable.CLINIT.toString(), SignatureDescriptor.create(void.class));
    }

}
