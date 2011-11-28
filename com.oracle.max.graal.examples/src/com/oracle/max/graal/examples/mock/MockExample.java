/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.examples.mock;

import java.lang.reflect.*;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.nodes.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class MockExample {

    private static String[] graalCompilerFactoryClasses = {
        "com.oracle.max.graal.hotspot.CompilerImpl",
        "com.oracle.max.vm.ext.c1xgraal.C1XGraal"
    };

    protected static GraalCompiler getGraalCompiler() {
        for (String className : graalCompilerFactoryClasses) {
            Class<?> c = null;
            try {
                c = Class.forName(className);
                Method m = c.getDeclaredMethod("getGraalCompiler");
                return (GraalCompiler) m.invoke(null);
            } catch (Exception e) {
                System.err.println(e);
            }
        }
        throw new InternalError("Could not create a GraalCompiler instance");
    }

    public static void run() {
        System.out.println();
        System.out.println();
        System.out.println("Running Mock Example");
        long start = System.currentTimeMillis();
        try {
            Method m = MockExample.class.getMethod("mockMethod");
            System.out.println("result=" + MockExample.createAndCallMethod(m));
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }
        System.out.println("time=" + (System.currentTimeMillis() - start) + "ms");
    }

    private static int createAndCallMethod(Method m) {
        // Obtain RiMethod and RiRuntime instances.
        GraalCompiler graalCompiler = getGraalCompiler();
        RiRuntime runtime = graalCompiler.runtime;
        RiResolvedMethod riMethod = runtime.getRiMethod(m);

        // Create the compiler graph for the method.
        StructuredGraph graph = new StructuredGraph();
        ReturnNode returnNode = graph.add(new ReturnNode(ConstantNode.forInt(42, graph)));
        graph.start().setNext(returnNode);

        // Compile and print disassembly.
        CiResult result = graalCompiler.compileMethod(riMethod, graph, PhasePlan.DEFAULT);
        System.out.println(runtime.disassemble(result.targetMethod()));

        // Install method!
        runtime.installMethod(riMethod, result.targetMethod());

        // And call it!!
        return mockMethod();
    }

    public static int mockMethod() {
        return 41;
    }
}
