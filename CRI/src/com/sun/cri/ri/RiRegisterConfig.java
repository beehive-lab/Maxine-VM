/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.cri.ri;

import com.sun.cri.ci.*;

/**
 * Represents the register configuration specified by the runtime system
 * to the compiler, including such information as the return value register, the set
 * of allocatable registers, the order of registers within a reference map, calling
 * convention, etc.
 *
 * @author Ben L. Titzer
 */
public interface RiRegisterConfig {

    CiRegister getReturnRegister(CiKind kind);

    CiRegister getStackPointerRegister();

    CiRegister getFramePointerRegister();

    CiRegister getScratchRegister();

    /**
     * Gets the calling convention describing a call to or from Java code.
     * 
     * @param parameters the types of the arguments of the call
     * @param outgoing if {@code true}, this is a call to Java code otherwise it's a call from Java code
     * @param target the target platform
     */
    CiCallingConvention getJavaCallingConvention(CiKind[] parameters, boolean outgoing, CiTarget target);
    
    /**
     * Gets the calling convention describing a call to the runtime.
     * 
     * @param parameters the types of the arguments of the call
     * @param target the target platform
     */
    CiCallingConvention getRuntimeCallingConvention(CiKind[] parameters, CiTarget target);

    /**
     * Gets the calling convention describing a call to or from native code.
     * 
     * @param parameters the types of the arguments of the call
     * @param outgoing if {@code true}, this is a call to native code otherwise it's a call from native code
     * @param target the target platform
     */
    CiCallingConvention getNativeCallingConvention(CiKind[] parameters, boolean outgoing, CiTarget target);

    CiRegister[] getAllocatableRegisters();

    CiRegister[] getCallerSaveRegisters();

    int getMinimumCalleeSaveFrameSize();

    int getCalleeSaveRegisterOffset(CiRegister register);

    CiRegister[] getRegisterReferenceMapOrder();
    
    /**
     * Gets the integer register corresponding to a runtime-defined role.
     * 
     * @param id the identifier of a runtime-defined register role
     * @return the integer register playing the role specified by {@code id}
     */
    CiRegister getIntegerRegister(int id);
}
