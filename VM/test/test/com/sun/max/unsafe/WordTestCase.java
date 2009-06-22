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

    private WordWidth _wordWidth;

    public WordWidth wordWidth() {
        return _wordWidth;
    }

    protected int _tiny;
    protected int _low;
    protected int _medium;
    protected long _high;

    protected Address _address0;
    protected Address _address1;
    protected Address _addressTiny;
    protected Address _addressLow;
    protected Address _addressMedium;
    protected Address addressHigh;
    protected Address _addressMax;
    protected Address _addressMax32;

    protected Size _size0;
    protected Size _size1;
    protected Size _sizeTiny;
    protected Size _sizeLow;
    protected Size _sizeMedium;
    protected Size _sizeHigh;
    protected Size _sizeMax;
    protected Size _sizeMax32;

    protected Pointer _pointer0;
    protected Pointer _pointer1;
    protected Pointer _pointerTiny;
    protected Pointer _pointerLow;
    protected Pointer _pointerMedium;
    protected Pointer _pointerHigh;
    protected Pointer _pointerMax;
    protected Pointer _pointerMax32;


    protected Offset _offsetMinus1;
    protected Offset _offset0;
    protected Offset _offset1;
    protected Offset _offset2;
    protected Offset _offset4;
    protected Offset _offset8;
    protected Offset _offset16;
    protected Offset _offsetTiny;
    protected Offset _offsetLow;
    protected Offset _offsetMedium;
    protected Offset _offsetHigh;
    protected Offset _offsetMin;
    protected Offset _offsetMax;

    protected static final long LOW_32_BITS_MASK = 0x00000000ffffffffL;

    @Override
    public void setUp() {
        _wordWidth = Word.width();

        _tiny = 1234; // small enough that _tiny^2 < 32 bits;

        _low = 12345678; // always in int range, always positive, always less than _medium

        _medium = Integer.MAX_VALUE >> 3; // always in int range, always positive, always less than _high

        _high = ((long) Integer.MAX_VALUE << 16) & ~0x87770000L; // sometimes outside int range, always positive

        assert ((int) _high + _low) > 0;

        _address0 = Address.zero();
        _address1 = Address.fromInt(1);
        _addressTiny = Address.fromInt(_tiny);
        _addressLow = Address.fromInt(_low);
        _addressMedium = Address.fromInt(_medium);
        _addressMax32 = Address.fromLong(LOW_32_BITS_MASK);
        switch (wordWidth()) {
            case BITS_64:
                addressHigh = Address.fromLong(_high);
                _addressMax = Offset.fromLong(-1L).asAddress();
                break;
            case BITS_32:
                addressHigh = Address.fromLong(((int) _high) & LOW_32_BITS_MASK);
                _addressMax = Address.fromLong(LOW_32_BITS_MASK);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }

        _size0 = _address0.asSize();
        _size1 = _address1.asSize();
        _sizeTiny = _addressTiny.asSize();
        _sizeLow = _addressLow.asSize();
        _sizeMedium = _addressMedium.asSize();
        _sizeHigh = addressHigh.asSize();
        _sizeMax = _addressMax.asSize();
        _sizeMax32 = _addressMax32.asSize();

        _pointer0 = _address0.asPointer();
        _pointer1 = _address1.asPointer();
        _pointerTiny = _addressTiny.asPointer();
        _pointerLow = _addressLow.asPointer();
        _pointerMedium = _addressMedium.asPointer();
        _pointerHigh = addressHigh.asPointer();
        _pointerMax = _addressMax.asPointer();
        _pointerMax32 = _addressMax32.asPointer();

        _offsetMinus1 = Offset.fromLong(-1L);
        _offset0 = Offset.zero();
        _offset1 = Offset.fromInt(1);
        _offset2 = Offset.fromInt(2);
        _offset4 = Offset.fromInt(4);
        _offset8 = Offset.fromInt(8);
        _offset16 = Offset.fromInt(16);
        _offsetTiny = Offset.fromInt(_tiny);
        _offsetLow = Offset.fromInt(_low);
        _offsetMedium = Offset.fromInt(_medium);

        switch (wordWidth()) {
            case BITS_64:
                _offsetMin = Offset.fromLong(Long.MIN_VALUE);
                _offsetMax = Offset.fromLong(Long.MAX_VALUE);
                _offsetHigh = Offset.fromLong(_high);
                break;
            case BITS_32:
                _offsetMin = Offset.fromInt(Integer.MIN_VALUE);
                _offsetMax = Offset.fromInt(Integer.MAX_VALUE);
                _offsetHigh = Offset.fromLong(((int) _high) & LOW_32_BITS_MASK);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

}
