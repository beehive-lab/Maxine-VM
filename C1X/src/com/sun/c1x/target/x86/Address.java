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
package com.sun.c1x.target.x86;

import com.sun.c1x.asm.*;
import com.sun.c1x.asm.RelocInfo.*;
import com.sun.c1x.lir.LIRAddress.*;
import com.sun.c1x.target.*;

/**
 * The <code>Address</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class Address {

    public enum ScaleFactor {
        noScale(-1), times1(0), times2(1), times4(2), times8(3), timesPtr64(3), timesPtr32(2);

        public final int value;

        private ScaleFactor(int value) {
            this.value = value;
        }

        public static ScaleFactor timesPtr(Architecture arch) {
            return (arch.is32bit()) ? timesPtr32 : timesPtr64;
        }
    }

    public X86Register base;
    public int disp;
    public ScaleFactor scale;
    public X86Register index;
    public RelocationHolder rspec;;

    public Address() {
    }

    public Address(X86Register base, X86Register index, Scale scale, int displacement) {
        // TODO Auto-generated constructor stub
    }

    public Address(X86Register rsp, int i) {
        // TODO Auto-generated constructor stub
    }

    public Address(int displacement) {
        // TODO Auto-generated constructor stub
    }

    public Address(X86Register base2, long addrOffset) {
        // TODO Auto-generated constructor stub
    }

    public Address(Register base2, int displacement) {
        // TODO Auto-generated constructor stub
    }

    public Address(Register base2, Register index2, Scale scale2, int displacement) {
        // TODO Auto-generated constructor stub
    }

    public Address(Register base2, long addrOffset) {
        // TODO Auto-generated constructor stub
    }

    public Address(X86Register noreg, X86Register tmp, ScaleFactor times1) {
        // TODO Auto-generated constructor stub
    }

    public Address(long minLong) {
        // TODO Auto-generated constructor stub
    }

    public Address(X86Register base2, X86Register index2, ScaleFactor scale2, int displacement) {
        // TODO Auto-generated constructor stub
    }

    public Address(int i, long target, Type reloc) {
        // TODO Auto-generated constructor stub
    }

    public Address(X86Register subKlass, RegisterOrConstant superCheckOffset, ScaleFactor times1, int displacement) {
        // TODO Auto-generated constructor stub
    }

    public Address(X86Register arrSize, X86Register len, int scaleFactor) {
        // TODO Auto-generated constructor stub
    }

    public Address(long l, Pointer target, Type reloc) {
        // TODO Auto-generated constructor stub
    }

    public int asInt() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int disp() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean baseNeedsRex() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean indexNeedsRex() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean uses(Register asRegister) {
        // TODO Auto-generated method stub
        return false;
    }
}
