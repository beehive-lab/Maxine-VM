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
package com.sun.cri.ci;



/**
 * Represents an address specified via some combination of a base register,
 * an index register, a displacement and a scale.
 * 
 * @author Doug Simon
 */
public final class CiAddress extends CiLocation {

    /**
     * A sentinel value used as a place holder in an instruction stream for an address that will be patched.
     */
    public static final CiAddress Placeholder = new CiAddress(CiKind.Illegal, CiRegister.None.asValue());
    
    public final CiLocation base;
    public final CiLocation index;
    public final Scale scale;
    public final int displacement;
    
    public CiAddress(CiKind kind, CiLocation base) {
        this(kind, base, IllegalLocation, Scale.Times1, 0);
    }

    public CiAddress(CiKind kind, CiLocation base, int displacement) {
        this(kind, base, IllegalLocation, Scale.Times1, displacement);
    }

    public CiAddress(CiKind kind, CiLocation base, CiLocation index) {
        this(kind, base, index, Scale.Times1, 0);
    }
    
    public CiAddress(CiKind kind, CiLocation base, CiLocation index, Scale scale, int displacement) {
        super(kind);
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.displacement = displacement;
    }
    
    /**
     * A scaling factor used in complex addressing modes such as those supported by x86 platforms.
     */
    public enum Scale {
        Times1(1, 0),
        Times2(2, 1),
        Times4(4, 2),
        Times8(8, 3);

        Scale(int value, int log2) {
            this.value = value;
            this.log2 = log2;
        }

        /**
         * The value (or multiplier) of this scale.
         */
        public final int value;
        
        /**
         * The {@linkplain #value value} of this scale log 2.
         */
        public final int log2;
        
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
        BASE_INDEX_DISP,
        PLACEHOLDER;
    }
    
    public Format format() {
        if (this == Placeholder) {
            return Format.PLACEHOLDER;
        }
        assert base.isLegal();
        if (index.isLegal()) {
            if (displacement != 0) {
                return Format.BASE_INDEX_DISP;
            } else {
                return Format.BASE_INDEX;
            }
        } else {
            if (displacement != 0) {
                return Format.BASE_DISP;
            } else {
                return Format.BASE;
            }
        }
    }
    
    private static String s(CiLocation location) {
        if (location.isRegister()) {
            return location.asRegister().name;
        }
        assert location.isVariable();
        return "v" + ((CiVariable) location).index;
    }
    
    @Override
    public String name() {
        switch (format()) {
            case BASE            : return "[" + s(base) + "]";
            case BASE_DISP       : return "[" + s(base) + " + " + displacement + "]";
            case BASE_INDEX      : return "[" + s(base) + " + " + s(index) + "]";
            case BASE_INDEX_DISP : return "[" + s(base) + " + " + s(index) + "*" + scale.value + " + " + displacement + "]";
            case PLACEHOLDER     : return "[<placeholder>]";
            default              : throw new IllegalArgumentException("unknown format: " + format());
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CiAddress) {
            CiAddress addr = (CiAddress) obj;
            return kind == addr.kind && displacement == addr.displacement && base.equals(addr.base) && scale == addr.scale && index.equals(addr.index);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return (base.hashCode() << 4) | kind.ordinal();
    }
}
