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
package com.sun.max.tele.page;

import java.io.*;

import com.sun.max.unsafe.*;


/**
 * @author Mick Jordan
 * @author Puneeet Lakhina
 *
 */
public abstract class AbstractX64PageTableAccess implements PageTableAccess {

    /* (non-Javadoc)
     * @see com.sun.max.tele.page.PageTableAccess#getMfnForAddress(com.sun.max.unsafe.Address)
     */
    @Override
    public long getMfnForAddress(Address address)throws IOException {
        return getPfnForMfn(address.toLong() >> X64VM.L1_SHIFT);
    }

    @Override
    public int getNumPTEntries(int level) {
        switch (level) {
            case 1:
                return X64VM.L1_ENTRIES;
            case 2:
                return X64VM.L2_ENTRIES;
            case 3:
                return X64VM.L3_ENTRIES;
            case 4:
                return X64VM.L4_ENTRIES;
            default:
                throw new IllegalArgumentException("illegal page table level: " + level);
        }
    }
    @Override
    public Address getAddressForPfn(long pfn) {
        return Address.fromLong(pfn << X64VM.L1_SHIFT);
    }
    /* (non-Javadoc)
     * @see com.sun.max.tele.page.PageTableAccess#getPTIndex(com.sun.max.unsafe.Address, int)
     */
    @Override
    public final int getPTIndex(Address address, int level) {
        final long a = address.toLong();
        long result;
        switch (level) {
            case 1:
                result =  (a >> X64VM.L1_SHIFT) & (X64VM.L1_ENTRIES - 1);
                break;
            case 2:
                result =  (a >> X64VM.L2_SHIFT) & (X64VM.L2_ENTRIES - 1);
                break;
            case 3:
                result =  (a >> X64VM.L3_SHIFT) & (X64VM.L3_ENTRIES - 1);
                break;
            case 4:
                result =  (a >> X64VM.L4_SHIFT) & (X64VM.L4_ENTRIES - 1);
                break;
            default:
                throw new IllegalArgumentException("illegal page table level: " + level);
        }
        return (int) result;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.page.PageTableAccess#getPteForAddress(com.sun.max.unsafe.Address)
     */
    @Override
    public final long getPteForAddress(Address address)throws IOException {
        Address table = getPageTableBase(); // level 4 table
        long pte = 0;
        int level = 4;
        while (level > 0) {
            final int index = getPTIndex(address, level);
            pte = getPTEntryAtIndex(table, index);
            if (!PageTableUtil.isPresent(pte)) {
                throw new PteNotPresentException("page table entry at index " + index + " in level " + level + " is not present");
            }
            table = getAddressForPte(pte);
            level--;
        }
        return pte;
    }

    @Override
    public Address getAddressForPte(long pte) throws IOException {
        return Address.fromLong(getPfnForPte(pte) << X64VM.PAGE_SHIFT);
    }

    @Override
    public long getPfnForPte(long pte)throws IOException {
        return getPfnForMfn((pte & X64VM.PADDR_MASK & X64VM.PAGE_MASK) >> X64VM.PAGE_SHIFT);
    }

    @Override
    public long getPfnForAddress(Address address) {
        return address.toLong() >> X64VM.L1_SHIFT;
    }

}
