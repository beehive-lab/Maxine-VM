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
import com.sun.cri.ci.CiCallingConvention.Type;
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
     * Gets the calling convention describing how arguments are passed.
     * 
     * @param type the type of calling convention being requested 
     * @param parameters the types of the arguments of the call
     * @param target the target platform
     */
    CiCallingConvention getCallingConvention(Type type, CiKind[] parameters, CiTarget target);
    
    /**
     * Gets the complete set of registers that are can be used to pass parameters according to a given calling
     * convention.
     * 
     * @param type the type of calling convention
     * @return the ordered set of registers that may be used to pass parameters in a call conforming to {@code type}.
     *         {@linkplain CiRegister#isCpu() Integral} registers appear in this array before
     *         {@linkplain CiRegister#isFpu() floating point} registers.
     */
    CiRegister[] getCallingConventionRegisters(Type type);
    
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
     * Gets the registers whose values must be preserved by a method across any call it makes. 
     */
    CiRegister[] getCallerSaveRegisters();
    
    /**
     * Gets the object describing the callee save area of this register configuration.
     * Note that this area may be {@linkplain CiCalleeSaveArea#EMPTY empty}. 
     */
    CiCalleeSaveArea getCalleeSaveArea();
    
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
