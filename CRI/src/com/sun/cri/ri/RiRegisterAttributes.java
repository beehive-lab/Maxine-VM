package com.sun.cri.ri;

import java.util.*;

import com.sun.cri.ci.*;

/**
 * A collection of register attributes. The specific attribute values for a register may be
 * local to a compilation context. For example, a {@link RiRegisterConfig} in use during
 * a compilation will determine which registers are callee saved.
 * 
 * @author Doug Simon
 */
public class RiRegisterAttributes {
    
    /**
     * Denotes a register whose value preservation (if required) across a call is the responsibility of the caller. 
     */
    public final boolean isCallerSave;
    
    /**
     * Denotes a register whose value preservation (if required) across a call is the responsibility of the callee. 
     */
    public final boolean isCalleeSave;
    
    /**
     * Denotes a register that is available for use by a register allocator. 
     */
    public final boolean isAllocatable;
    
    /**
     * Denotes a register guaranteed to be non-zero if read in compiled Java code.
     * For example, a register dedicated to holding the current thread.
     */
    public final boolean isNonZero;
    
    public RiRegisterAttributes(boolean isCallerSave, boolean isCalleeSave, boolean isAllocatable, boolean isNonZero) {
        this.isCallerSave = isCallerSave;
        this.isCalleeSave = isCalleeSave;
        this.isAllocatable = isAllocatable;
        this.isNonZero = isNonZero;
    }
    
    public static final RiRegisterAttributes NONE = new RiRegisterAttributes(false, false, false, false);
    
    /**
     * Creates a map from register {@linkplain CiRegister#number numbers} to register
     * {@linkplain RiRegisterAttributes attributes} for a given register configuration and set of
     * registers.
     * 
     * @param registerConfig a register configuration
     * @param registers a set of registers
     * @return an array whose length is the max register number in {@code registers} plus 1. An element at index i holds
     *         the attributes of the register whose number is i.
     */
    public static RiRegisterAttributes[] createMap(RiRegisterConfig registerConfig, CiRegister[] registers) {
        RiRegisterAttributes[] map = new RiRegisterAttributes[registers.length];
        for (CiRegister reg : registers) {
            if (reg != null) {
                RiRegisterAttributes attr = new RiRegisterAttributes(
                                Arrays.asList(registerConfig.getCallerSaveRegisters()).contains(reg),
                                Arrays.asList(registerConfig.getCalleeSaveRegisters()).contains(reg),
                                Arrays.asList(registerConfig.getAllocatableRegisters()).contains(reg),
                                false);
                if (map.length <= reg.number) {
                    map = Arrays.copyOf(map, reg.number + 1);
                }
                map[reg.number] = attr;
            }
        }
        for (int i = 0; i < map.length; i++) {
            if (map[i] == null) {
                map[i] = NONE;
            }
        }
        return map;
    }
}