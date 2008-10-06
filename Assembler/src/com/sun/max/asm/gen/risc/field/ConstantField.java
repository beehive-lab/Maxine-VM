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
/*VCSID=d144abef-5476-4086-a86b-f38f9c2f9303*/
package com.sun.max.asm.gen.risc.field;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.asm.gen.risc.bitRange.*;

/**
 * @author Dave Ungar
 * @author Bernd Mathiske
 * @author Adam Spitz
 */
public class ConstantField extends RiscField {

    public ConstantField(BitRange bitRange) {
        super(bitRange);
    }

    public ConstantField(String name, BitRange bitRange) {
        super(bitRange);
        setName(name);
    }

    public RiscConstant constant(Argument argument) {
        return new RiscConstant(this, argument);
    }

    public RiscConstant constant(int value) {
        return new RiscConstant(this, value);
    }

    public static ConstantField createAscending(int... bits) {
        final BitRange bitRange = BitRange.create(bits, BitRangeOrder.ASCENDING);
        return new ConstantField(bitRange);
    }

    public static ConstantField createDescending(int... bits) {
        final BitRange bitRange = BitRange.create(bits, BitRangeOrder.DESCENDING);
        return new ConstantField(bitRange);
    }

}
