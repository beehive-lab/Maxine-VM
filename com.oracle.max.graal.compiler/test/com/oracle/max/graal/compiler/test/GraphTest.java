/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.test;

import java.lang.reflect.*;

import junit.framework.Assert;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.debug.*;
import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.nodes.*;
import com.sun.cri.ri.*;

public abstract class GraphTest {

    private static String[] graalCompilerFactoryClasses = {
        "com.oracle.max.graal.hotspot.CompilerImpl",
        "com.oracle.max.vm.ext.c1xgraal.C1XGraal"
    };

    private static GraalCompiler getGraalCompiler() {
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

    private GraalCompiler graalCompiler;

    public GraphTest() {
        this.graalCompiler = getGraalCompiler();
        System.out.println("initialized");
    }

    protected RiRuntime runtime() {
        return graalCompiler.runtime;
    }

    protected StructuredGraph parse(String methodName) {
        Method found = null;
        for (Method m : this.getClass().getMethods()) {
            if (m.getName().equals(methodName)) {
                Assert.assertNull(found);
                found = m;
            }
        }
        return parse(found);
    }

    protected StructuredGraph parse(Method m) {
        RiRuntime runtime = graalCompiler.runtime;
        RiResolvedMethod riMethod = runtime.getRiMethod(m);
        StructuredGraph graph = new StructuredGraph();
        new GraphBuilderPhase(runtime, riMethod).apply(graph);
        return graph;
    }

    protected void print(StructuredGraph graph) {
        IdealGraphPrinterObserver observer = new IdealGraphPrinterObserver(GraalOptions.PrintIdealGraphAddress, GraalOptions.PrintIdealGraphPort);
        observer.printSingleGraph(getClass().getSimpleName(), graph);
    }
}
