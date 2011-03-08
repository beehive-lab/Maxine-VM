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
package test.com.sun.max.vm.cps;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.type.*;

/**
 * Tests whether we can get hold of the VM entry points needed for producing a boot image.
 *
 * @author Bernd Mathiske
 */
public abstract class CompilerTest_entryPoints extends CompilerTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CompilerTest_entryPoints.class);
    }

    public CompilerTest_entryPoints(String name) {
        super(name);
    }

    public void test_startupMethod() {
        final ClassMethodActor methodActor = (ClassMethodActor) ClassRegistry.MaxineVM_run;
        assertNotNull(methodActor);
        final int entryPointOffset = CompilerTestSetup.compilerScheme().compile(methodActor, true, null).getEntryPoint(CallEntryPoint.OPTIMIZED_ENTRY_POINT).asOffset().toInt();
        assertTrue(entryPointOffset > 0);
    }

    public void test_runMethod() {
        final ClassMethodActor methodActor = (ClassMethodActor) ClassRegistry.VmThread_run;
        assertNotNull(methodActor);
        final int entryPointOffset = CompilerTestSetup.compilerScheme().compile(methodActor, true, null).getEntryPoint(CallEntryPoint.OPTIMIZED_ENTRY_POINT).asOffset().toInt();
        assertTrue(entryPointOffset > 0);
    }
}
