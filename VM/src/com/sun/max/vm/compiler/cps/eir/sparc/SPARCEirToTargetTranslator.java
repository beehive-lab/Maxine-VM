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
package com.sun.max.vm.compiler.cps.eir.sparc;

import static com.sun.max.vm.compiler.CallEntryPoint.*;

import com.sun.max.asm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cps.b.c.d.e.sparc.target.*;
import com.sun.max.vm.compiler.cps.eir.*;
import com.sun.max.vm.compiler.cps.target.*;
import com.sun.max.vm.compiler.cps.target.sparc.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.sparc.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Laurent Daynes
 */
public final class SPARCEirToTargetTranslator extends EirToTargetTranslator {

    public SPARCEirToTargetTranslator(TargetGeneratorScheme targetGeneratorScheme) {
        super(targetGeneratorScheme, InstructionSet.SPARC, SPARCTrapStateAccess.registerReferenceMapSize());
    }

    @Override
    public CPSTargetMethod createIrMethod(ClassMethodActor classMethodActor) {
        final SPARCOptimizedTargetMethod targetMethod = new SPARCOptimizedTargetMethod(classMethodActor, compilerScheme());
        notifyAllocation(targetMethod);
        return targetMethod;
    }

    @Override
    protected EirTargetEmitter createEirTargetEmitter(EirMethod eirMethod) {
        final boolean requiresAdapter = (!(eirMethod.isTemplate() || eirMethod.abi.targetABI().callEntryPoint().equals(C_ENTRY_POINT))) && compilerScheme().vmConfiguration().jitCompilerScheme() != compilerScheme();
        SPARCAdapterFrameGenerator adapterFrameGenerator = null;
        if (requiresAdapter) {
            adapterFrameGenerator = SPARCAdapterFrameGenerator.jitToOptimizedCompilerAdapterFrameGenerator(eirMethod.classMethodActor(), eirMethod.abi);
        }
        return new SPARCEirTargetEmitter((SPARCEirABI) eirMethod.abi, eirMethod.frameSize(), compilerScheme().vmConfiguration().safepoint, adapterFrameGenerator);
    }

}
