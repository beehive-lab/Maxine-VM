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
package com.sun.max.vm.cps.jit.amd64;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.template.*;

/**
 * Implementation of template-based target generator for AMD64.
 *
 * @author Laurent Daynes
 */
public class AMD64TemplateBasedTargetGenerator extends TemplateBasedTargetGenerator {

    public AMD64TemplateBasedTargetGenerator(JitCompiler jitCompiler) {
        super(jitCompiler, ISA.AMD64);
    }

    @Override
    public AMD64JitTargetMethod createIrMethod(ClassMethodActor classMethodActor) {
        final AMD64JitTargetMethod targetMethod = new AMD64JitTargetMethod(classMethodActor);
        notifyAllocation(targetMethod);
        return targetMethod;
    }

    private static final int NUMBER_OF_BYTES_PER_BYTECODE = 16;

    @Override
    protected BytecodeToTargetTranslator makeTargetTranslator(ClassMethodActor classMethodActor) {
        // allocate a buffer that is likely to be large enough, based on a linear expansion
        final int estimatedSize = classMethodActor.codeAttribute().code().length * NUMBER_OF_BYTES_PER_BYTECODE;
        final CodeBuffer codeBuffer = new CodeBuffer(estimatedSize);
        return new BytecodeToAMD64TargetTranslator(classMethodActor, codeBuffer, templateTable(), false);
    }
}
