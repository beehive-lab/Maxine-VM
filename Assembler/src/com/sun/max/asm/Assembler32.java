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
/*VCSID=fa7ecc8c-7bba-4636-a7c2-68329b93f0f5*/
package com.sun.max.asm;

/**
 * Assemblers for 32-bit address spaces.
 *
 * @author Bernd Mathiske
 */
public interface Assembler32 {

    /**
     * Gets the base address for relative labels.
     * 
     * @return the address at which the assembled object code will reside
     */
    int startAddress();

    void setStartAddress(int addresss);

    /**
     * Assigns a fixed, absolute 32-bit address to a given label.
     * 
     * @param label    the label to update
     * @param address  an absolute 32-bit address
     */
    void fixLabel(Label label, int address);

    /**
     * Gets the address to which a label has been bound.
     * 
     * @param label  the label to decode
     * @return the address to which {@code label} has been bound
     * @throws AssemblyException if {@code label} has not been bound to an address
     */
    int address(Label label) throws AssemblyException;
}
