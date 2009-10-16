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
package com.sun.max.vm.compiler.b;

import com.sun.max.collect.*;
import com.sun.max.profile.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.bir.*;

/**
 * Translates the bytecodes of a method into BIR.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class ActorToBirTranslator extends BirGenerator {

    public ActorToBirTranslator(BirGeneratorScheme birGeneratorScheme) {
        super(birGeneratorScheme);
    }

    private static final TimerMetric timer = GlobalMetrics.newTimer("Translate-ActorToBir", Clock.SYSTEM_MILLISECONDS);

    private void translate(BirMethod birMethod) {
        timer.start();
        final CodeAttribute codeAttribute = birMethod.classMethodActor().compilee().codeAttribute();
        final ControlFlowAnalyzer controlFlowAnalyzer = new ControlFlowAnalyzer(codeAttribute.code());
        final IndexedSequence<BirBlock> blocks = controlFlowAnalyzer.run();
        final BirBlock[] blockMap = controlFlowAnalyzer.blockMap();

        birMethod.setGenerated(
                        codeAttribute.code(),
                        codeAttribute.maxStack,
                        codeAttribute.maxLocals,
                        blocks,
                        blockMap,
                        codeAttribute.exceptionHandlerTable());
        timer.stop();
    }

    @Override
    protected void generateIrMethod(BirMethod birMethod) {
        translate(birMethod);
    }
}
