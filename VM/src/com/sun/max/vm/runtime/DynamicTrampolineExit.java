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
package com.sun.max.vm.runtime;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.trampoline.*;

/**
 * Dynamic Trampoline Exit. Takes care of manipulation that may be required for the correct routing of execution to the
 * method invoked via the trampoline when the trampoline returns.
 *
 * @author Laurent Daynes
 */
public abstract class DynamicTrampolineExit {

    /**
     * Gets the return address to which the trampoline will return. This address is derived from
     * {@code vtableEntryPoint} by adjusting it to account for the call entry point associated
     * with the caller of the trampoline.
     *
     * The means by which the address is used to alter the return path of the trampoline is platform dependent.
     * For example, on AMD64 the {@code stackPointer} value can be used to patch return addresses on the stack.
     * On SPARC, the compiler used to compile the trampoline to use the return register (i.e. %o0) as the link
     * register instead of the standard link register (i.e. %i7).
     *
     * @param dynamicTrampoline the trampoline being called
     * @param vtableEntryPoint the {@link CallEntryPoint#VTABLE_ENTRY_POINT} for the method being trampolined to
     * @param stackPointer the CPU stack pointer in the frame of the trampoline
     */
    public abstract Address trampolineReturnAddress(DynamicTrampoline dynamicTrampoline, Address vtableEntryPoint, Pointer stackPointer);

    @HOSTED_ONLY
    public static DynamicTrampolineExit create(VMConfiguration vmConfiguration) {
        try {
            final String isa = vmConfiguration.platform.instructionSet().name();
            final Class<?> dynamicTrampolineExitClass = Class.forName(MaxPackage.fromClass(DynamicTrampolineExit.class).subPackage(isa.toLowerCase()).name() + "." + isa + DynamicTrampolineExit.class.getSimpleName());
            return (DynamicTrampolineExit) dynamicTrampolineExitClass.newInstance();
        } catch (Exception exception) {
            throw ProgramError.unexpected("could not create dynamic trampoline exit: " + exception);
        }
    }

}
