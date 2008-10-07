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
/*VCSID=acb7db76-e55d-4010-8000-234ca639c7f4*/
package com.sun.max.vm.compiler.eir.amd64;

import com.sun.max.asm.amd64.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64EirSafepoint extends EirSafepoint<EirInstructionVisitor, AMD64EirTargetEmitter> {

    public AMD64EirSafepoint(EirBlock block) {
        super(block);
    }

    @Override
    public void emit(AMD64EirTargetEmitter emitter) {
        emitter.addSafepoint(this);
        final AMD64EirRegister.General r = (AMD64EirRegister.General) emitter.abi().safepointLatchRegister();
        final AMD64GeneralRegister64 register = r.as64();
        emitter.assembler().mov(register, register.indirect());
    }

}
