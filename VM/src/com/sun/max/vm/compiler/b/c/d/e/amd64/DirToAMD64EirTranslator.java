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
package com.sun.max.vm.compiler.b.c.d.e.amd64;

import com.sun.max.profile.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.dir.eir.amd64.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.compiler.tir.*;

/**
 * @author Bernd Mathiske
 */
public class DirToAMD64EirTranslator extends AMD64EirGenerator {

    public DirToAMD64EirTranslator(AMD64EirGeneratorScheme eirGeneratorScheme) {
        super(eirGeneratorScheme);
    }

    private static final TimerMetric timer = GlobalMetrics.newTimer("Translate-DirToEir", Clock.SYSTEM_MILLISECONDS);

    @Override
    protected void generateIrMethod(EirMethod eirMethod, CompilationDirective compilationDirective) {
        final DirGeneratorScheme dirGeneratorScheme = (DirGeneratorScheme) compilerScheme();
        final DirMethod dirMethod = dirGeneratorScheme.dirGenerator().makeIrMethod(eirMethod.classMethodActor());

        timer.start();

        final DirToAMD64EirMethodTranslation translation = new DirToAMD64EirMethodTranslation(this, eirMethod, dirMethod);
        translation.translateMethod();

        eirMethod.setGenerated(translation.eirBlocks(), translation.literalPool, translation.parameterEirLocations, translation.resultEirLocation(), translation.frameSize());

        timer.stop();
    }

    private TreeEirMethod createTreeEirMethod(ClassMethodActor classMethodActor) {
        final TreeEirMethod eirMethod = new TreeEirMethod(classMethodActor, eirABIsScheme().treeABI());
        notifyAllocation(eirMethod);
        return eirMethod;
    }

    @Override
    public EirMethod makeIrMethod(DirMethod dirMethod) {
        final TreeEirMethod tirMethod = createTreeEirMethod(dirMethod.classMethodActor());
        final DirToAMD64EirMethodTranslation translation = new DirToAMD64EirMethodTranslation(this, tirMethod, dirMethod);
        translation.translateMethod();
        tirMethod.setGenerated(translation.eirBlocks(), translation.literalPool, translation.parameterEirLocations, translation.resultEirLocation(), translation.frameSize());
        return tirMethod;
    }

}
