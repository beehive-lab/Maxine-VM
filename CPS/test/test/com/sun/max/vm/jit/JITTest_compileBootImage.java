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

import static com.sun.max.vm.MaxineVM.*;

import java.util.*;

import test.com.sun.max.vm.cps.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.target.*;

/**
 * Compiles all methods that makes the alternate jit-ed VM start.
 *
 * @author Laurent Daynes
 */
public class JITTest_compileBootImage extends JitCompilerTestCase {

    public void test_compileAll() {
        final LinkedList<TargetMethod> toDo = new LinkedList<TargetMethod>();
        final RuntimeCompilerScheme jit = vm().config.jitCompilerScheme();

        final ClassActor classActor = ClassActor.fromJava(com.sun.max.vm.cps.run.jitTest.JitTest.class);

        for (VirtualMethodActor virtualMethodActor : classActor.localVirtualMethodActors()) {
            toDo.add(jit.compile(virtualMethodActor));
        }
        for (StaticMethodActor staticMethodActor : classActor.localStaticMethodActors()) {
            toDo.add(jit.compile(staticMethodActor));
        }
        for (TargetMethod targetMethod : toDo) {
            Trace.line(1,  targetMethod.regionName());
            traceBundleAndDisassemble((CPSTargetMethod) targetMethod);
        }
    }

}
