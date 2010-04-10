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

import com.sun.cri.ci.CiKind;
import com.sun.cri.ci.CiLocation;
import com.sun.cri.ci.CiRegister;
import com.sun.cri.ci.CiTarget;

/**
 * This interface represents the register configuration specified by the runtime system
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

    CiRegister getSafepointRegister(); // will be deprecated with XIR

    CiRegister getThreadRegister(); // will be deprecated with XIR

    CiLocation[] getJavaParameterLocations(CiKind[] types, boolean outgoing, CiTarget target);

    CiLocation[] getRuntimeParameterLocations(CiKind[] types, CiTarget target);

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
