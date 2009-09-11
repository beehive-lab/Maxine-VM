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
package com.sun.max.vm.jit.sparc;

import com.sun.max.asm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.b.c.d.e.sparc.target.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.sparc.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.template.*;

/**
 *
 * @author Laurent Daynes
 */
public class SPARCTemplateBasedTargetGenerator extends TemplateBasedTargetGenerator {

    private static final int NUMBER_OF_BYTES_PER_BYTECODE = 16;

    private final boolean needsAdapterFrame;

    public SPARCTemplateBasedTargetGenerator(JitCompiler jitCompiler) {
        super(jitCompiler, InstructionSet.SPARC);
        needsAdapterFrame = compilerScheme().vmConfiguration().compilerScheme() instanceof BcdeTargetSPARCCompiler;
    }

    @Override
    public CPSTargetMethod createIrMethod(ClassMethodActor classMethodActor) {
        final SPARCJitTargetMethod targetMethod = new SPARCJitTargetMethod(classMethodActor, compilerScheme());
        notifyAllocation(targetMethod);
        return targetMethod;
    }

    @Override
    protected BytecodeToTargetTranslator makeTargetTranslator(ClassMethodActor classMethodActor) {
        // allocate a buffer that is likely to be large enough, based on a linear expansion
        final  int estimatedSize = classMethodActor.codeAttribute().code().length * NUMBER_OF_BYTES_PER_BYTECODE;
        final CodeBuffer codeBuffer = new ByteArrayCodeBuffer(estimatedSize);
        SPARCEirABI optimizingCompilerAbi = null;
        if (needsAdapterFrame) {
            final EirGenerator eirGenerator = ((BcdeTargetSPARCCompiler) compilerScheme().vmConfiguration().compilerScheme()).eirGenerator();
            optimizingCompilerAbi = (SPARCEirABI) eirGenerator.eirABIsScheme().getABIFor(classMethodActor);
        }
        return new BytecodeToSPARCTargetTranslator(classMethodActor, codeBuffer, templateTable(), optimizingCompilerAbi, false);
    }

}
