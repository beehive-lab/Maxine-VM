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
package com.sun.max.asm.arm;

/**
* Handles validations for all immediate operands in ARM instructions.
*
* @author Sumeet Panchal
*/

public final class ARMImmediates {

    private ARMImmediates() {
    }

    /**
     * Finds out legitimate 8 bit immediate value and 4 bit rotate value for the given 32 bit value. Throws an
     * exception if there doesn't exist any such combination, as such a value is an invalid operand.
     * 
     * @param value
     *            32 bit immediate operand
     * @return 12 bit shifter operand
     */
    public static int calculateShifter(int value) {
        int immed;
        for (int i = 0; i < 32; i += 2) {
            immed = Integer.rotateLeft(value, i);
            if (immed >= 0 && immed <= 255) {
                return immed | i << 7;
            }
        }
        throw new IllegalArgumentException("Invalid immediate operand value");
    }

    /**
     * Tests if rotate amount is even, hence valid.
     * 
     * @param value
     *            rotate amount specified as operand
     * @return
     */
    public static boolean isValidRotate(int value) {
        return value % 2 == 0;
    }

    /**
     * Tests if 32 bit immediate value is valid for 4 bit rotate and 8 bit immediate value representation.
     * 
     * @param value
     *            32 bit immediate operand value
     * @return
     */
    public static boolean isValidImmediate(int value) {
        int a;
        for (int i = 0; i < 32; i += 2) {
            a = Integer.rotateLeft(value, i);
            if (a >= 0 && a <= 255) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests if immediate shift value is between 0 and 32.
     * 
     * @param value
     *            6 bit number with only 0 to 32 allowed
     * @return
     */
    public static boolean isValidShiftImm(int value) {
        return value >= 0 && value <= 32;
    }

}
