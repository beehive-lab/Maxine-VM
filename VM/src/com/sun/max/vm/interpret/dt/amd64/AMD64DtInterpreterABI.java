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
package com.sun.max.vm.interpret.dt.amd64;

import com.sun.max.asm.amd64.*;
import com.sun.max.vm.compiler.eir.amd64.*;

/**
 * This needs refactoring into the existing ABIScheme framework.
 *
 * @author Simon Wilkinson
 */
public final class AMD64DtInterpreterABI {

    /**
     * Registers used by the interpreter which must not be allocated when generating the templates.
     */
    public static AMD64EirRegister.General[] unallocatableToTemplateABI() {
        return new AMD64EirRegister.General[] {
                        eirBytecodeArrayPointer(),
                        eirBytecodeIndex(),
                        eirFirstTemplatePointer()};
    }

    static AMD64EirRegister.General eirBytecodeArrayPointer() {
        return AMD64EirRegister.General.R12;
    }

    static AMD64EirRegister.General eirBytecodeIndex() {
        return AMD64EirRegister.General.R13;
    }

    static AMD64EirRegister.General eirFirstTemplatePointer() {
        return AMD64EirRegister.General.R15;
    }

    static AMD64GeneralRegister64 bytecodeArrayPointer() {
        return AMD64GeneralRegister64.R12;
    }

    static AMD64GeneralRegister64 bytecodeIndex() {
        return AMD64GeneralRegister64.R13;
    }

    static AMD64GeneralRegister64 firstTemplatePointer() {
        return AMD64GeneralRegister64.R15;
    }

    static AMD64GeneralRegister64 nonParameterlocalsSize() {
        return AMD64GeneralRegister64.R8;
    }
}
