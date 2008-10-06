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
/*VCSID=05f341b0-44c6-48cc-bfb7-9d12da4b9c04*/
package com.sun.max.vm.compiler.eir.ia32.unix;

import static com.sun.max.vm.compiler.eir.ia32.IA32EirRegister.General.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.ia32.*;
import com.sun.max.vm.type.*;

/**
 * C functions that we implement behave almost like native functions.
 * They have the same parameters and callee save registers.
 * For simplicity, we do use the stack pointer as frame pointer, though,
 * just as we are used to in Java methods.
 *
 * @author Bernd Mathiske
 */
public class UnixIA32EirCFunctionABI extends UnixIA32EirJavaABI {

    @Override
    public EirLocation getResultLocation(Kind kind) {
        if (kind != null) {
            switch (kind.asEnum()) {
                case FLOAT:
                case DOUBLE:
                    Problem.unimplemented("ST0");
                    return null;
                default:
                    return super.getResultLocation(kind);
            }
        }
        return null;
    }

    private final PoolSet<IA32EirRegister> _callerSavedRegisters = PoolSet.noneOf(IA32EirRegister.General.pool());

    @Override
    public PoolSet<IA32EirRegister> callerSavedRegisters() {
        return _callerSavedRegisters;
    }

    private final PoolSet<IA32EirRegister> _calleeSavedRegisters = PoolSet.of(IA32EirRegister.General.pool(), EBX, ESP, EBP, ESI, EDI);

    @Override
    public PoolSet<IA32EirRegister> calleeSavedRegisters() {
        return _calleeSavedRegisters;
    }

    public UnixIA32EirCFunctionABI(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

}
