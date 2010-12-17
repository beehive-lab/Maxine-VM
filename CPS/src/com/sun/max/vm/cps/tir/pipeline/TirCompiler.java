/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.cps.tir.target.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.hotpath.compiler.*;

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
