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
package com.sun.max.tele.method;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

/**
 * Factory and manager for descriptions of locations in code in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class CodeManager extends AbstractTeleVMHolder implements MaxCodeManager {

    public CodeManager(TeleVM teleVM) {
        super(teleVM);
    }

    public BytecodeLocation createBytecodeLocation(MethodKey methodKey, String description) throws ProgramError {
        return CodeLocation.createBytecodeLocation(vm(), methodKey, description);
    }

    public  BytecodeLocation createBytecodeLocation(TeleClassMethodActor teleClassMethodActor, int bytecodePosition, String description) {
        return CodeLocation.createBytecodeLocation(vm(), teleClassMethodActor, bytecodePosition, description);
    }

    public MachineCodeLocation createMachineCodeLocation(Address address, String description) throws ProgramError {
        return CodeLocation.createMachineCodeLocation(vm(), address, description);
    }

    public MachineCodeLocation createMachineCodeLocation(Address address, TeleClassMethodActor teleClassMethodActor, int position, String description) throws ProgramError {
        return CodeLocation.createMachineCodeLocation(vm(), address, teleClassMethodActor, position, description);
    }

}

