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
package com.sun.max.vm.compiler.cps.cir.dir;

import com.sun.max.collect.*;
import com.sun.max.profile.*;
import com.sun.max.program.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.cps.cir.*;
import com.sun.max.vm.compiler.cps.cir.transform.*;
import com.sun.max.vm.compiler.cps.dir.*;

/**
 * A DIR generator that translates from CIR.
 *
 * @author Bernd Mathiske
 */
public class CirToDirTranslator extends DirGenerator {

    public CirToDirTranslator(DirGeneratorScheme dirGeneratorScheme) {
        super(dirGeneratorScheme);
    }

    private static final TimerMetric timer = GlobalMetrics.newTimer("Translate-CirToDir", Clock.SYSTEM_MILLISECONDS);

    @Override
    protected void generateIrMethod(DirMethod dirMethod) {
        final CirGeneratorScheme cirGeneratorScheme = (CirGeneratorScheme) compilerScheme();
        final CirGenerator cirGenerator = cirGeneratorScheme.cirGenerator();
        final CirMethod cirMethod = cirGenerator.makeIrMethod(dirMethod.classMethodActor());

        Trace.begin(3, "CIR->DIR " + cirMethod.getQualifiedName());

        timer.start();

        final CirClosure closure = cirMethod.copyClosure();
        CirBlockUpdating.apply(closure);

        if (MaxineVM.isHosted()) {
            if (cirMethod.classMethodActor().isTemplate()) {
                CirTemplateChecker.apply(cirGenerator, cirMethod, closure);
            }
        }

        final CirToDirMethodTranslation translation = new CirToDirMethodTranslation(cirMethod.resultKind(), closure, dirMethod, this);
        final IndexedSequence<DirBlock> dirBlocks = translation.translateMethod();

        final DirVariable[] dirParameters = new DirVariable[closure.parameters().length - 2]; // exclude continuations
        for (int i = 0; i < dirParameters.length; i++) {
            dirParameters[i] = translation.cirToDirVariable(closure.parameters()[i]);
        }

        dirMethod.setGenerated(dirParameters, dirBlocks, translation.usesMakeStackVariable);

        timer.stop();

        Trace.end(3, "CIR->DIR " + cirMethod.getQualifiedName());
    }
}
