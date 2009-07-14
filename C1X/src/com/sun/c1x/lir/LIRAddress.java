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
package com.sun.c1x.lir;

import com.sun.c1x.target.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;


/**
 * The <code>LIRAddress</code> class represents an operand that is in
 * memory. It includes a base address, and index, a scale and a displacement.
 *
 * @author Marcelo Cintra
 * @author Ben L. Titzer
 */
public class LIRAddress extends LIROperand {

    public enum Scale {
        Times1,
        Times2,
        Times4,
        Times8;

        public static Scale fromInt(int shift) {
            assert shift < Scale.values().length;
            return Scale.values()[shift];
        }
    }

    public final LIROperand base;
    public final LIROperand index;
    public final Scale scale;
    public final int displacement;

    /**
     * Creates a new LIRAddress with the specified base address, index, and basic type.
     *
     * @param base the LIROperand representing the base address
     * @param index the LIROperand representing the index
     * @param basicType the basic type of the resulting operand
     */
    public LIRAddress(LIROperand base, LIROperand index, BasicType basicType) {
        super(basicType);
        this.base = base;
        this.index = index;
        this.scale = Scale.Times1;
        this.displacement = 0;
    }

    /**
     * Creates a new LIRAddress with the specified base address, displacement, and basic type.
     *
     * @param base the LIROperand representing the base address
     * @param displacement the constant displacement from the base address
     * @param basicType the basic type of the resulting operand
     */
    public LIRAddress(LIROperand base, int displacement, BasicType basicType) {
        super(basicType);
        this.base = base;
        this.index = LIROperand.ILLEGAL;
        this.displacement = displacement;
        this.scale = Scale.Times1;
    }

    /**
     * Creates a new LIRAddress with the specified base address, index, and basic type.
     *
     * @param base the LIROperand representing the base address
     * @param index the LIROperand representing the index
     * @param scale the scaling factor for the index
     * @param displacement the constant displacement from the base address
     * @param basicType the basic type of the resulting operand
     */
    public LIRAddress(LIROperand base, LIROperand index, Scale scale, int displacement, BasicType basicType) {
        super(basicType);
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.displacement = displacement;
    }

    public LIROperand base() {
        return base;
    }

    public LIROperand index() {
        return index;
    }

    public Scale scale() {
        return scale;
    }

    public int displacement() {
        return displacement;
    }

    /**
     * The equals() for object comparisons.
     *
     * @return <code>true</code> if this address is equal to the other address
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof LIRAddress) {
            LIRAddress otherAddress = (LIRAddress) other;
            return base == otherAddress.base && index == otherAddress.index && displacement == otherAddress.displacement && scale == otherAddress.scale;
        }
        return false;
    }

    /**
     * Prints this operand on the specified output stream.
     *
     * @param out the output stream to print to
     */
    @Override
    public void printValueOn(LogStream out) {
        out.print("Base:" + base);
        if (!index.isIllegal()) {
            out.print(" Index:" + index);
            switch (scale) {
                case Times1:
                    break;
                case Times2:
                    out.print(" * 2");
                    break;
                case Times4:
                    out.print(" * 4");
                    break;
                case Times8:
                    out.print(" * 8");
                    break;
            }
        }
        out.print(" Disp: %d" + displacement);
    }

    /**
     * Verifies the address is valid on the specified architecture.
     * @param architecture the architecture to validate on
     */
    public void verify(Architecture architecture) {
        if (architecture.isSPARC()) {
            assert scale == Scale.Times1 : "Scaled addressing mode not available on SPARC and should not be used";
            assert displacement == 0 || index.isIllegal() : "can't have both";
        } else if (architecture.is64bit()) {
            assert base.isCpuRegister() : "wrong base operand";
            assert index.isIllegal() || index.isDoubleCpu() : "wrong index operand";
            assert base.type() == BasicType.Object || base.type() == BasicType.Long : "wrong type for addresses";
        } else {
            assert base.isSingleCpu() : "wrong base operand";
            assert index.isIllegal() || index.isSingleCpu() : "wrong index operand";
            assert base.type() == BasicType.Object || base.type() == BasicType.Int : "wrong type for addresses";
        }
    }

    /**
     * Computes the scaling factor for the specified basic type.
     * @param type the basic type
     * @param oopSize the size of an oop on this architecture
     * @return the scaling factor
     */
    public static Scale scale(BasicType type, int oopSize) {
        switch (type.sizeInBytes(oopSize)) {
            case 1: return Scale.Times1;
            case 2: return Scale.Times2;
            case 4: return Scale.Times4;
            case 8: return Scale.Times8;
            default:
                throw Util.shouldNotReachHere();
        }
    }
}
