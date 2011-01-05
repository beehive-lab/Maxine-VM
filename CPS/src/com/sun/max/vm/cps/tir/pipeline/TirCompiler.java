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
package com.sun.max.vm.cps.tir.pipeline;

import static com.sun.max.vm.MaxineVM.*;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.b.c.d.e.amd64.target.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.cps.tir.target.*;

public class TirCompiler {
    static final boolean BACKEND = false;
    static final boolean ASSEMBLE = true;
    static final boolean DISSASSEMBLE = true;
    static final boolean PRINT = true;
    static final boolean PERFORM_DEAD_CODE_ELIMINATION = true;

    public static void compile(TirTree tree) {
        TirMessageSink pipeline = TirVoidSink.SINK;
        final TirToDirTranslator dirTranslator = new TirToDirTranslator();

        if (ASSEMBLE && BACKEND) {
            pipeline = dirTranslator;
        }

        if (PRINT) {
            pipeline = new TirFork(pipeline, new TirPrintSink());
        }

        if (PERFORM_DEAD_CODE_ELIMINATION) {
            pipeline = new TirDeadCodeElimination(pipeline);
        }

        tree.send(pipeline);

        if (BACKEND == false) {
            return;
        }

        final TargetTree targetTree = new TargetTree();

        final AMD64EirGeneratorScheme generator = (AMD64EirGeneratorScheme) CPSCompiler.Static.compiler();
        final TreeEirMethod eirMethod = (TreeEirMethod) generator.eirGenerator().makeIrMethod(dirTranslator.method());
        final EirToTargetTranslator targetGenerator = (EirToTargetTranslator) ((AMD64CPSCompiler) CPSCompiler.Static.compiler()).targetGenerator();
        final TargetMethod targetMethod = targetGenerator.makeIrMethod(eirMethod);

        targetTree.setGenerated(eirMethod, targetMethod);

        if (isHosted() && DISSASSEMBLE) {
            Visualizer.print(dirTranslator.method());
            Trace.stream().println(eirMethod.traceToString());
            targetMethod.disassemble(System.out);
            final List<TargetJavaFrameDescriptor> descriptors = TargetJavaFrameDescriptor.inflate(((CPSTargetMethod) targetMethod).compressedJavaFrameDescriptors());
            for (TargetJavaFrameDescriptor descriptor : descriptors) {
                if (descriptor != null) {
                    Console.println(descriptor.toMultiLineString());
                }
            }
        }

        tree.setTarget(targetTree);
    }
}
