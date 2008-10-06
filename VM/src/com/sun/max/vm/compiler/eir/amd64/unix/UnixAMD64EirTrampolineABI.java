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
/*VCSID=b9414e3a-327f-4a0a-85e2-1619b55363b3*/
package com.sun.max.vm.compiler.eir.amd64.unix;

import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.eir.amd64.*;

/**
 * Trampolines callee-save and restore all parameter registers.
 * 
 * @author Bernd Mathiske
 */
public class UnixAMD64EirTrampolineABI extends UnixAMD64EirJavaABI {
    private final PoolSet<AMD64EirRegister> _calleeSavedRegisters;
    public UnixAMD64EirTrampolineABI(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        _calleeSavedRegisters = PoolSet.noneOf(AMD64EirRegister.pool());
        _calleeSavedRegisters.or(allocatableRegisters());

        // Do not save the integer return register. Trampolines do use it.
        _calleeSavedRegisters.remove(AMD64EirRegister.General.RAX);
    }

    @Override
    public PoolSet<AMD64EirRegister> calleeSavedRegisters() {
        return _calleeSavedRegisters;
    }
}
