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

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiRegister.RegisterFlag;

/**
 * A register configuration binds roles and {@linkplain RiRegisterAttributes attributes}
 * to physical registers.
 * 
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public interface RiRegisterConfig {

    /**
     * Gets the register to be used for returning a value of a given kind.
     */
    CiRegister getReturnRegister(CiKind kind);

    /**
     * Gets the register to which {@link CiRegister#Frame} and {@link CiRegister#CallerFrame} are bound.
     */
    CiRegister getFrameRegister();

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

    /**
     * Gets the set of registers that can be used by the register allocator.
     */
    CiRegister[] getAllocatableRegisters();

    /**
     * Gets the set of registers that can be used by the register allocator,
     * {@linkplain CiRegister#categorize(CiRegister[]) categorized} by register {@linkplain RegisterFlag flags}.
     * 
     * @return a map from each {@link RegisterFlag} constant to the list of {@linkplain #getAllocatableRegisters()
     *         allocatable} registers for which the flag is {@linkplain #isSet(RegisterFlag) set}
     * 
     */
    EnumMap<RegisterFlag, CiRegister[]> getCategorizedAllocatableRegisters();

    /**
     * Denotes the registers whose values must be preserved by a method across any call it makes. 
     */
    CiRegister[] getCallerSaveRegisters();
    
    /**
     * Denotes the registers whose values must be preserved by a method for its caller. 
     */
    CiRegister[] getCalleeSaveRegisters();

    /**
     * Gets a map from register {@linkplain CiRegister#number numbers} to register
     * {@linkplain RiRegisterAttributes attributes} for this register configuration.
     * 
     * @return an array where an element at index i holds the attributes of the register whose number is i
     * @see CiRegister#categorize(CiRegister[])
     */
    RiRegisterAttributes[] getAttributesMap();
    
    /**
     * Gets the register corresponding to a runtime-defined role.
     * 
     * @param id the identifier of a runtime-defined register role
     * @return the register playing the role specified by {@code id}
     */
    CiRegister getRegister(int id);
}
