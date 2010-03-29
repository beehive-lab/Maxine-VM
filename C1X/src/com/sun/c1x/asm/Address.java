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

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;

/**
 * The {@code Address} class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public class Address {

    public enum ScaleFactor {
        noScale(-1), times1(0), times2(1), times4(2), times8(3);

        public final int value;

        private ScaleFactor(int value) {
            this.value = value;
        }

        public static ScaleFactor fromInt(int v) {
            switch (v) {
                case 1: return times1;
                case 2: return times2;
                case 4: return times4;
                case 8: return times8;
            }
            throw Util.shouldNotReachHere();
        }

        public static ScaleFactor fromLog(int v) {
            switch (v) {
                case 0: return times1;
                case 1: return times2;
                case 2: return times4;
                case 3: return times8;
            }
            throw Util.shouldNotReachHere();
        }
    }

    public static final Address InternalRelocation = new Address(CiRegister.None, 0);

    public final CiRegister base;
    public final int disp;
    public final ScaleFactor scale;
    public final CiRegister index;

    public Address(CiRegister base, int displacement) {
        this(base, CiRegister.None, ScaleFactor.noScale, displacement);
    }

    public Address(CiRegister base, CiRegister index, ScaleFactor scale) {
        this(base, index, scale, 0);
    }

    public Address(CiRegister base, CiRegister index, ScaleFactor scale, int displacement) {
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.disp = displacement;
        assert base != null && index != null && scale != null;
        assert index.isValid() == (scale != ScaleFactor.noScale);
    }

    public Address(CiRegister reg) {
        this(reg, 0);
    }

    public boolean uses(CiRegister r) {
        return base == r || index == r;
    }

    @Override
    public String toString() {
        if (this == InternalRelocation) {
            return "[<internal reloc>]";
        }
        String base = this.base.isValid() ? this.base.toString() : "";
        if (index.isValid()) {
            if (disp == 0) {
                return String.format("[%s + %s*%d]", base, index, scale.value);
            }
            return String.format("[%s + %s*%d + %d]", base, index, scale.value, disp);
        } else {
            if (disp == 0) {
                return String.format("[%s]", base);
            }
            return String.format("[%s + %d]", base, disp);
        }
    }
}
