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
package test.com.sun.max.unsafe;

import com.sun.max.ide.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;

public abstract class WordTestCase extends MaxTestCase {

    protected WordTestCase(String name) {
        super(name);
    }

    private WordWidth wordWidth;

    public WordWidth wordWidth() {
        return wordWidth;
    }

    protected int tiny;
    protected int low;
    protected int medium;
    protected long high;

    protected Address address0;
    protected Address address1;
    protected Address addressTiny;
    protected Address addressLow;
    protected Address addressMedium;
    protected Address addressHigh;
    protected Address addressMax;
    protected Address addressMax32;

    protected Size size0;
    protected Size size1;
    protected Size sizeTiny;
    protected Size sizeLow;
    protected Size sizeMedium;
    protected Size sizeHigh;
    protected Size sizeMax;
    protected Size sizeMax32;

    protected Pointer pointer0;
    protected Pointer pointer1;
    protected Pointer pointerTiny;
    protected Pointer pointerLow;
    protected Pointer pointerMedium;
    protected Pointer pointerHigh;
    protected Pointer pointerMax;
    protected Pointer pointerMax32;


    protected Offset offsetMinus1;
    protected Offset offset0;
    protected Offset offset1;
    protected Offset offset2;
    protected Offset offset4;
    protected Offset offset8;
    protected Offset offset16;
    protected Offset offsetTiny;
    protected Offset offsetLow;
    protected Offset offsetMedium;
    protected Offset offsetHigh;
    protected Offset offsetMin;
    protected Offset offsetMax;

    protected static final long LOW_32_BITS_MASK = 0x00000000ffffffffL;

    @Override
    public void setUp() {
        wordWidth = Word.width();

        tiny = 1234; // small enough that _tiny^2 < 32 bits;

        low = 12345678; // always in int range, always positive, always less than _medium

        medium = Integer.MAX_VALUE >> 3; // always in int range, always positive, always less than _high

        high = ((long) Integer.MAX_VALUE << 16) & ~0x87770000L; // sometimes outside int range, always positive

        assert ((int) high + low) > 0;

        address0 = Address.zero();
        address1 = Address.fromInt(1);
        addressTiny = Address.fromInt(tiny);
        addressLow = Address.fromInt(low);
        addressMedium = Address.fromInt(medium);
        addressMax32 = Address.fromLong(LOW_32_BITS_MASK);
        switch (wordWidth()) {
            case BITS_64:
                addressHigh = Address.fromLong(high);
                addressMax = Offset.fromLong(-1L).asAddress();
                break;
            case BITS_32:
                addressHigh = Address.fromLong(((int) high) & LOW_32_BITS_MASK);
                addressMax = Address.fromLong(LOW_32_BITS_MASK);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }

        size0 = address0.asSize();
        size1 = address1.asSize();
        sizeTiny = addressTiny.asSize();
        sizeLow = addressLow.asSize();
        sizeMedium = addressMedium.asSize();
        sizeHigh = addressHigh.asSize();
        sizeMax = addressMax.asSize();
        sizeMax32 = addressMax32.asSize();

        pointer0 = address0.asPointer();
        pointer1 = address1.asPointer();
        pointerTiny = addressTiny.asPointer();
        pointerLow = addressLow.asPointer();
        pointerMedium = addressMedium.asPointer();
        pointerHigh = addressHigh.asPointer();
        pointerMax = addressMax.asPointer();
        pointerMax32 = addressMax32.asPointer();

        offsetMinus1 = Offset.fromLong(-1L);
        offset0 = Offset.zero();
        offset1 = Offset.fromInt(1);
        offset2 = Offset.fromInt(2);
        offset4 = Offset.fromInt(4);
        offset8 = Offset.fromInt(8);
        offset16 = Offset.fromInt(16);
        offsetTiny = Offset.fromInt(tiny);
        offsetLow = Offset.fromInt(low);
        offsetMedium = Offset.fromInt(medium);

        switch (wordWidth()) {
            case BITS_64:
                offsetMin = Offset.fromLong(Long.MIN_VALUE);
                offsetMax = Offset.fromLong(Long.MAX_VALUE);
                offsetHigh = Offset.fromLong(high);
                break;
            case BITS_32:
                offsetMin = Offset.fromInt(Integer.MIN_VALUE);
                offsetMax = Offset.fromInt(Integer.MAX_VALUE);
                offsetHigh = Offset.fromLong(((int) high) & LOW_32_BITS_MASK);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

}
