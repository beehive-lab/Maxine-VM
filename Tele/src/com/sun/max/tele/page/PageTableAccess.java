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
 * @author Puneeet Lakhina
 *
 */
public interface PageTableAccess {

    /**
     * Get Total No of current pages.
     *
     * @return
     */
    long getNoOfPages()throws IOException;

    /**
     * Get machine frame number for pseudo physical frame number.
     *
     * @param pfn
     * @return
     */
    long getMfnForPfn(long pfn)throws IOException;

    /**
     * Return the machine frame corresponding to the given virtual address.
     *
     * @param address
     * @return machine frame
     */
    long getMfnForAddress(Address address)throws IOException;

    /**
     * Get Pseudo Physical Frame Number for this address.
     *
     * @param address
     * @return
     */
    long getPfnForAddress(Address address)throws IOException;

    Address getAddressForPfn(long pfn)throws IOException;

    /**
     * Get the entry at a given index in a given page table.
     *
     * @param table virtual address of table base
     * @param index index into table
     * @return
     */
    long getPTEntryAtIndex(Address table, int index)throws IOException;

    /**
     * Get number of page tables entries in a page frame at a given level.
     *
     * @param level
     * @return
     */
    int getNumPTEntries(int level)throws IOException;

    /**
     * Return the page table entry for a given address. Requires walking the page table structure.
     *
     * @param address
     * @return
     */
    long getPteForAddress(Address address)throws IOException;

    /**
     * Return the index into the given page table for given address.
     *
     * @param address virtual address
     * @param level the page table level
     */
    int getPTIndex(Address address, int level)throws IOException;

    /**
     * Return the physical frame that is mapped to the given machine frame.
     *
     * @param mfn machine frame number
     * @return physical frame number
     */
    long getPfnForMfn(long mfn)throws IOException;

    /**
     * Get base address of Level 1 page table.
     *
     * @return
     */
    Address getPageTableBase()throws IOException;

    Address getAddressForPte(long pte)throws IOException;

    long getPfnForPte(long pte)throws IOException;


}
