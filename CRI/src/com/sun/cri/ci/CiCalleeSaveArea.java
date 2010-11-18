package com.sun.cri.ci;

import java.util.*;


/**
 * The register save area (CSA) is contiguous space in a stack frame
 * used to save (and restore) the values of the caller's registers.
 * This class describes the layout of a CSA in terms of its
 * {@linkplain #size size}, {@linkplain #slotSize slot size} and
 * the {@linkplain #registers callee save registers} covered by the CSA.
 *  
 * @author Doug Simon
 */
public class CiCalleeSaveArea {
    
    /**
     * An empty callee-save area.
     */
    public static final CiCalleeSaveArea EMPTY = new CiCalleeSaveArea(0, new CiRegister[0], 0);
    
    /**
     * The size (in bytes) of the CSA.
     */
    public final int size;
    
    /**
     * The size (in bytes) of an {@linkplain #registerAtIndex(int) indexable} slot in the CSA.
     */
    public final int slotSize;
    
    /**
     * Map from {@linkplain CiRegister#number register numbers} to slot indexes in the CSA.
     */
    private final int[] regNumToIndex;
    
    private final CiRegister[] indexToReg;

    /**
     * The list of registers {@linkplain #contains(CiRegister) contained} by this CSA.
     */
    public final CiRegister[] registers;
                            
    /**
     * Creates an CSA descriptor.
     * 
     * @param size size (in bytes) of the CSA. If this is {@code -1}, then the CSA size will be computed from {@code registers}.
     * @param registers the registers that can be saved in the CSA
     * @param slotSize the size (in bytes) of an {@linkplain #registerAtIndex(int) indexable} slot in the CSA
     */
    public CiCalleeSaveArea(int size, CiRegister[] registers, int slotSize) {
        assert slotSize == 0 || CiUtil.isPowerOf2(slotSize);
        this.slotSize = slotSize;
        int maxRegNum = -1;
        int maxOffset = 0;
        this.registers = registers;
        int offset = 0;
        for (CiRegister reg : registers) {
            assert offset % slotSize == 0;
            assert reg.number >= 0;
            if (reg.number > maxRegNum) {
                maxRegNum = reg.number;
            }
            if (offset > maxOffset) {
                maxOffset = offset;
            }
            offset += reg.spillSlotSize;
        }
        if (size == -1) {
            this.size = offset;
        } else {
            assert offset <= size;
            this.size = size;
        }
        size = this.size; 

        this.regNumToIndex = new int[maxRegNum + 1]; 
        this.indexToReg = offset == 0 ? new CiRegister[0] : new CiRegister[offset / slotSize];
        Arrays.fill(regNumToIndex, -1);
        offset = 0;
        for (CiRegister reg : registers) {
            int index = offset / slotSize;
            regNumToIndex[reg.number] = index;
            indexToReg[index] = reg;
            offset += reg.spillSlotSize;
        }
    }
    
    /**
     * Gets the offset of a given register in the CSA.
     * 
     * @return the offset (in bytes) of {@code reg} in the CSA
     * @throws IllegalArgumentException if {@code reg} does not have a slot in the CSA 
     */
    public int offsetOf(CiRegister reg) {
        return indexOf(reg)  * slotSize;
    }

    /**
     * Gets the index of a given register in the CSA.
     * 
     * @return the index of {@code reg} in the CSA
     * @throws IllegalArgumentException if {@code reg} does not have a slot in the CSA 
     */
    public int indexOf(CiRegister reg) {
        if (!contains(reg)) {
            throw new IllegalArgumentException();
        }
        return regNumToIndex[reg.number];
    }
    
    /**
     * Determines if the CSA includes a slot for a given register.
     *  
     * @param reg the register to test
     * @return true if the CSA contains a slot for {@code reg}
     */
    public boolean contains(CiRegister reg) {
        return reg.number >= 0 && reg.number < regNumToIndex.length && regNumToIndex[reg.number] != -1;
    }

    /**
     * Gets the register whose slot in the CSA is at a given index.
     * 
     * @param index an index of a slot in the CSA
     * @return the register whose slot in the CSA is at  {@code index} or {@code null} if {@code index} does not denote a
     *         slot in the CSA aligned with a register
     */
    public CiRegister registerAt(int index) {
        if (index < 0 || index >= indexToReg.length) {
            return null;
        }
        return indexToReg[index];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (CiRegister reg : registers) {
            if (sb.length() != 1) {
                sb.append(", ");
            }
            sb.append(reg).append("{+").append(offsetOf(reg)).append('}');
        }
        return sb.append("] size=").append(size).toString();
    }
}
