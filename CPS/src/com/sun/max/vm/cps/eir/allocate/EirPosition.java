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
package com.sun.max.vm.cps.eir.allocate;

import com.sun.max.vm.cps.eir.*;

/**
 * @author Bernd Mathiske
 */
public class EirPosition {

    private final EirBlock block;

    public EirBlock block() {
        return block;
    }

    private int index;
    private int number = -1;

    public int index() {
        return index;
    }

    public int number() {
        return number;
    }

    /**
     * Unique number within whole method.
     * @param instructionNumber the new unique number assigned to this instruction
     */
    public void setNumber(int instructionNumber) {
        number = instructionNumber;
    }

    public void setIndex(int instructionIndex) {
        index = instructionIndex;
    }

    public EirPosition(EirBlock block) {
        this.block = block;
        this.index = -1;
    }

    public EirPosition(EirBlock block, int index) {
        this.block = block;
        this.index = index;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof EirPosition) {
            final EirPosition position = (EirPosition) other;
            return index == position.index && block == position.block;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return index ^ block.hashCode();
    }
}
