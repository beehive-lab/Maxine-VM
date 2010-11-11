package com.sun.cri.ci;

import java.util.*;


/**
 * The register save area (RSA) is contiguous space in a stack frame
 * used to save (and restore) the values of the caller's registers.
 * This class describes the layout of a RSA in terms of its
 * {@linkplain #size size}, {@linkplain #slotSize slot size} and
 * the {@linkplain #indexOf(CiRegister) index} of each platform register
 * in the  RSA.
 * 
 * The RSA is only reserved in stack frames for code compiled under
 * callee-save semantics. In addition, the compiled code may not
 * save and restore all the registers that have a slot in the RSA.
 *  
 * @author Doug Simon
 */
public class CiRegisterSaveArea {
    /**
     * The size (in bytes) of the RSA.
     */
    public final int size;
    
    /**
     * The size (in bytes) of an {@linkplain #registerAtIndex(int) indexable} slot in the RSA.
     */
    public final int slotSize;
    
    /**
     * The index of the lowest slot in the RSA that maps to a register in which an object reference can be stored.  
     */
    public final int referenceSlotsStartIndex;

    /**
     * The number of slots in the RSA that map to registers in which an object reference can be stored.  
     */
    public final int referenceSlotsCount;

    /**
     * Map from {@linkplain CiRegister#number register numbers} to slot indexes in the RSA.
     */
    private final int[] regNumToIndex;
    
    /**
     * Map from {@linkplain CiRegister#number register numbers} to registers.
     */
    private final CiRegister[] regMap;

    private final CiRegister[] indexToReg;

    /**
     * The list of registers {@linkplain #contains(CiRegister) contained} by this RSA.
     * This array is sorted in ascending order of register {@linkplain CiRegister#number numbers}.
     */
    public final CiRegister[] registers;
                            
    /**
     * Creates an RSA descriptor.
     * 
     * @param size size (in bytes) of the RSA
     * @param registerOffsets map from registers to offsets in the RSA for the registers
     * @param slotSize the size (in bytes) of an {@linkplain #registerAtIndex(int) indexable} slot in the RSA
     */
    public CiRegisterSaveArea(int size, Map<CiRegister, Integer> registerOffsets, int slotSize, CiRegister firstReferenceReg, CiRegister lastReferenceReg) {
        assert CiUtil.isPowerOf2(slotSize);
        this.size = size;
        this.slotSize = slotSize;
        int maxRegNum = 0;
        int maxOffset = 0;
        registers = new CiRegister[registerOffsets.size()];
        int i = 0;
        for (Map.Entry<CiRegister, Integer> e : registerOffsets.entrySet()) {
            CiRegister reg = e.getKey();
            int offset = e.getValue();
            assert offset >= 0 && offset < size;
            assert offset % slotSize == 0;
            assert reg.number >= 0;
            if (reg.number > maxRegNum) {
                maxRegNum = reg.number;
            }
            if (offset > maxOffset) {
                maxOffset = offset;
            }
            registers[i++] = reg;
        }
        Arrays.sort(registers);
        this.regNumToIndex = new int[maxRegNum + 1]; 
        this.regMap = new CiRegister[maxRegNum + 1];
        this.indexToReg = new CiRegister[size / slotSize];
        Arrays.fill(regNumToIndex, -1);
        for (Map.Entry<CiRegister, Integer> e : registerOffsets.entrySet()) {
            CiRegister reg = e.getKey();
            Integer offset = e.getValue();
            int index = offset / slotSize;
            regNumToIndex[reg.number] = index;
            regMap[reg.number] = reg;
            indexToReg[index] = reg;
        }
        this.referenceSlotsStartIndex = indexOf(firstReferenceReg);
        this.referenceSlotsCount = indexOf(lastReferenceReg) - referenceSlotsStartIndex + 1;
    }
    
    /**
     * Gets the offset of a given register in the RSA.
     * 
     * @return the offset (in bytes) of {@code reg} in the RSA
     * @throws IllegalArgumentException if {@code reg} does not have a slot in the RSA 
     */
    public int offsetOf(CiRegister reg) {
        return indexOf(reg)  * slotSize;
    }

    /**
     * Gets the index of a given register in the RSA.
     * 
     * @return the index of {@code reg} in the RSA
     * @throws IllegalArgumentException if {@code reg} does not have a slot in the RSA 
     */
    public int indexOf(CiRegister reg) {
        if (!contains(reg)) {
            throw new IllegalArgumentException();
        }
        return regNumToIndex[reg.number];
    }
    
    /**
     * Determines if the RSA includes a slot for a given register.
     *  
     * @param reg the register to test
     * @return true if the RSA contains a slot for {@code reg}
     */
    public boolean contains(CiRegister reg) {
        return reg.number >= 0 && reg.number < regNumToIndex.length && regNumToIndex[reg.number] != -1;
    }

    /**
     * Gets the register whose slot in the RSA is at a given index.
     * 
     * @param index an index of a slot in the RSA
     * @return the register whose slot in the RSA is at  {@code index} or {@code null} if {@code index} does not denote a
     *         slot in the RSA aligned with a register
     */
    public CiRegister registerAt(int index) {
        if (index < 0 || index >= indexToReg.length) {
            return null;
        }
        return indexToReg[index];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int offset = 0; offset < size; ++offset) {
            CiRegister reg = registerAt(offset);
            if (reg != null) {
                if (sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append(offset).append(" -> ").append(reg);
            }
        }
        return sb.toString();
    }
}
