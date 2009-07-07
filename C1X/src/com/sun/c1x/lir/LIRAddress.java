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

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;


/**
 * The <code>LIRAddress</code> class definition.
 *
 * @author Marcelo Cintra
 */
public class LIRAddress extends LIROperand {

    public enum Scale {
        Times1,
        Times2,
        Times4,
        Times8;
    }

    private LIROperand base;
    private LIROperand index;
    private Scale scale;
    private int  displacement;
    private BasicType type;

    public LIRAddress(LIROperand base, LIROperand index, BasicType type) {
        this.base = base;
        this.index = index;
        this.type = type;
    }

    public LIRAddress(LIROperand base, int displacement, BasicType type) {
        this.base = base;
        this.displacement = displacement;
        this.type = type;
    }

    public LIRAddress(LIROperand base, LIROperand index, Scale scale, int displacement, BasicType type) {
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.displacement = displacement;
        this.type = type;
        //verify();
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

    @Override
    public BasicType type() {
        return type;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LIRAddress) {
            LIRAddress otherAddress = (LIRAddress) other;
            return base == otherAddress.base && index == otherAddress.index && displacement == otherAddress.displacement && scale() == otherAddress.scale;
        }
        return false;
    }

   @Override
   public void printValueOn(LogStream out) {
       out.print("Base:" + base);
       if (!index.isIllegal()) {
           out.print(" Index:" + index);
           switch (scale()) {
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

   public void verify() {
   }

   public static Scale scale(BasicType type) {
       return null;
   }
}
