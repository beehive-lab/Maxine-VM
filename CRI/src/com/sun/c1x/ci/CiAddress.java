/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ci;


/**
 * Represents an address specified via some combination of a base register,
 * an index register, a displacement and a scale.
 * 
 * @author Doug Simon
 */
public final class CiAddress extends CiLocation {

    public static final CiAddress InternalRelocation = new CiAddress(CiKind.Illegal, CiRegister.None.asLocation());
    
    public final CiLocation base;
    public final CiLocation index;
    public final Scale scale;
    public final int displacement;
    
    public CiAddress(CiKind kind, CiLocation base) {
        super(kind);
        this.base = base;
        this.index = CiLocation.IllegalLocation;
        this.scale = Scale.Times1;
        this.displacement = 0;
        this.format = Format.BASE;
    }

    public CiAddress(CiKind kind, CiLocation base, int displacement) {
        super(kind);
        this.base = base;
        this.index = CiLocation.IllegalLocation;
        this.scale = Scale.Times1;
        this.displacement = displacement;
        this.format = Format.BASE_DISP;
    }

    public CiAddress(CiKind kind, CiLocation base, CiLocation index) {
        super(kind);
        this.base = base;
        this.index = index;
        this.scale = Scale.None;
        this.displacement = 0;
        this.format = Format.BASE_INDEX;
    }
    
    public CiAddress(CiKind kind, CiLocation base, CiLocation index, Scale scale, int displacement) {
        super(kind);
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.displacement = displacement;
        this.format = Format.BASE_SCALE_INDEX_DISP;
    }

    public enum Scale {
        None(-1, -1),
        Times1(1, 0),
        Times2(2, 1),
        Times4(4, 2),
        Times8(8, 3);

        Scale(int value, int valueLog2) {
            this.value = value;
            this.valueLog2 = valueLog2;
        }

        public final int value;
        public final int valueLog2;
        
        public static Scale fromInt(int scale) {
            switch (scale) {
                case 1: return Times1;
                case 2: return Times2;
                case 4: return Times4;
                case 8: return Times8;
                default: throw new IllegalArgumentException(String.valueOf(scale));
            }
        }

        public static Scale fromShift(int shift) {
            return fromInt(1 << shift);
        }
    }
    
    public CiRegister base() {
        return base.asRegister();
    }
    
    public CiRegister index() {
        return index.asRegister();
    }
    
    public enum Format {
        BASE,
        BASE_DISP,
        BASE_INDEX,
        BASE_SCALE_INDEX_DISP;
    }
    
    public final Format format;
    
    private static String s(CiLocation location) {
        if (location.isRegister()) {
            return location.asRegister().name;
        }
        assert location.isVariable();
        return "v" + ((CiLocation) location).variableNumber();
    }
    
    @Override
    public String toString() {
        switch (format) {
            case BASE                  : return "[" + s(base) + "]";
            case BASE_DISP             : return "[" + s(base) + " + " + displacement + "]";
            case BASE_INDEX            : return "[" + s(base) + " + " + s(index) + "]";
            case BASE_SCALE_INDEX_DISP : return "[" + s(base) + " + " + s(index) + "*" + scale.value + " + " + displacement + "]";
            default                    : throw new IllegalArgumentException("unknown format: " + format);
        }
    }

}
