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
package com.sun.max.vm.cps.eir.amd64;

import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AMD64EirABI extends EirABI<AMD64EirRegister> {

    protected AMD64EirABI(VMConfiguration vmConfiguration, TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> targetABI) {
        super(vmConfiguration, AMD64EirRegister.class);
        this.targetABI = targetABI;
    }

    @Override
    public int stackSlotSize() {
        return Longs.SIZE;
    }

    @Override
    public Pool<AMD64EirRegister> registerPool() {
        return AMD64EirRegister.pool();
    }

    @Override
    public final AMD64EirRegister.General integerRegisterActingAs(VMRegister.Role role) {
        final AMD64GeneralRegister64 r = targetABI.registerRoleAssignment.integerRegisterActingAs(role);
        if (r == null) {
            return null;
        }
        return AMD64EirRegister.General.from(r);
    }

    @Override
    public final AMD64EirRegister.XMM floatingPointRegisterActingAs(VMRegister.Role role) {
        final AMD64XMMRegister r = targetABI.registerRoleAssignment.floatingPointRegisterActingAs(role);
        if (r == null) {
            return null;
        }
        return AMD64EirRegister.XMM.from(r);
    }

    private TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> targetABI;

    protected void initTargetABI(TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> abi) {
        this.targetABI = abi;
    }

    @Override
    public TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> targetABI() {
        return targetABI;
    }
}
