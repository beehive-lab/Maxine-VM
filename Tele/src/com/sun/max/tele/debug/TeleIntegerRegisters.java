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
package com.sun.max.tele.debug;

import static com.sun.max.platform.Platform.*;

import com.sun.c1x.target.amd64.*;
import com.sun.cri.ci.*;
import com.sun.max.asm.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * Encapsulates the values of the integer (or general purpose) registers for a tele native thread.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class TeleIntegerRegisters extends TeleRegisters {

    private final CiRegister indirectCallRegister;
    private final CiRegister fp;
    private final CiRegister sp;

    public static CiRegister[] getIntegerRegisters() {
        if (platform().isa == ISA.AMD64) {
            return AMD64.cpuRegisters;
        }
        throw FatalError.unimplemented();
    }

    public TeleIntegerRegisters(TeleVM teleVM, TeleRegisterSet teleRegisterSet) {
        super(teleVM, teleRegisterSet, getIntegerRegisters());
        if (platform().isa == ISA.AMD64) {
            indirectCallRegister = AMD64.rax;
            sp = AMD64.rsp;
            fp = AMD64.rbp;
        } else {
            throw FatalError.unimplemented();
        }
    }

    /**
     * Returns the value of the register that is used to make indirect calls.
     *
     * @return null if there is no fixed register used to for indirect calls on the target platform
     */
    Address getCallRegisterValue() {
        if (indirectCallRegister == null) {
            return null;
        }
        return getValue(indirectCallRegister);
    }

    /**
     * Returns the value of the register that is used as the stack pointer.
     *
     * @return the current stack pointer
     */
    Pointer stackPointer() {
        return getValue(sp).asPointer();
    }

    /**
     * Returns the value of the register that is used as the frame pointer.
     *
     * @return the current frame pointer
     */
    Pointer framePointer() {
        return getValue(fp).asPointer();
    }
}
