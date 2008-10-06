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
/*VCSID=fc1885ea-5600-45b7-aee2-4b559ea8d816*/
package com.sun.max.vm.compiler.eir.ia32;

import com.sun.max.asm.ia32.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * @author Bernd Mathiske
 */
public final class IA32EirSafepoint extends EirSafepoint<EirInstructionVisitor, IA32EirTargetEmitter> {

    public IA32EirSafepoint(EirBlock block) {
        super(block);
    }

    @Override
    public void emit(IA32EirTargetEmitter emitter) {
        emitter.addSafepoint(this);
        final IA32GeneralRegister32 register = (IA32GeneralRegister32) VMConfiguration.hostOrTarget().safepoint().latchRegister();
        emitter.assembler().mov(register, register.indirect());
    }

}
