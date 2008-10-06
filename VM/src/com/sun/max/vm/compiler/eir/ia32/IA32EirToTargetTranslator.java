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
/*VCSID=aa70ff6d-7e9a-4e27-9684-be13decb3733*/
package com.sun.max.vm.compiler.eir.ia32;

import com.sun.max.asm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.ia32.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Laurent Daynes
 */
public final class IA32EirToTargetTranslator extends EirToTargetTranslator {

    public IA32EirToTargetTranslator(TargetGeneratorScheme targetGeneratorScheme) {
        super(targetGeneratorScheme, InstructionSet.IA32, IA32TargetMethod.Static.registerReferenceMapSize());
    }

    @Override
    public TargetMethod createIrMethod(ClassMethodActor classMethodActor) {
        final IA32OptimizedTargetMethod targetMethod = new IA32OptimizedTargetMethod(classMethodActor);
        notifyAllocation(targetMethod);
        return targetMethod;
    }

    @Override
    protected EirTargetEmitter createEirTargetEmitter(EirMethod eirMethod) {
        return new IA32EirTargetEmitter((IA32EirABI) eirMethod.abi(), eirMethod.frameSize(), compilerScheme().vmConfiguration().safepoint());
    }
}
