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
package com.sun.c1x.asm;

import com.sun.c1x.target.Architecture;
import com.sun.c1x.target.Register;
import com.sun.c1x.util.Util;

/**
 * The <code>Address</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class Address {

    public enum ScaleFactor {
        noScale(-1), times1(0), times2(1), times4(2), times8(3);

        public final int value;

        private ScaleFactor(int value) {
            this.value = value;
        }

        public static ScaleFactor timesPtr(Architecture arch) {
            return (arch.is32bit()) ? times8 : times4;
        }

        public static ScaleFactor fromInt(int v) {
            for (ScaleFactor f : values()) {
                if (f.value == v) {
                    return f;
                }
            }

            return null;
        }
    }

    public static final Address InternalRelocation = new Address(Register.noreg, 0);

    public final Register base;
    public final int disp;
    public final ScaleFactor scale;
    public final Register index;

    public Address(Register base, int displacement) {
        this(base, Register.noreg, ScaleFactor.noScale, displacement);
    }

    public Address(Register base, Register index, ScaleFactor scale) {
        this(base, index, scale, 0);
    }

    public Address(Register base, Register index, ScaleFactor scale, int displacement) {
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.disp = displacement;
        assert base != null && index != null && scale != null;
    }

    public Address(Register base, RegisterOrConstant index, ScaleFactor scale, int displacement) {
        this(base, index.registerOrNoReg(), scale, displacement);
        assert !this.index.isValid() == (scale == ScaleFactor.noScale) : "inconsistent Pointer";
    }

    public Address(Register base, long displacement) {
        this(base, Util.safeToInt(displacement));
    }

    public Address(Register reg) {
        this(reg, 0);
    }

    public boolean uses(Register r) {
        return base == r || index == r;
    }
}
