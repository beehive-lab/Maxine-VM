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

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;
import com.sun.c1x.debug.LogStream;


/**
 * The <code>Location</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class Location {

    public enum Where {
        OnStack,
        InRegister;

        public static Where fromInt(int value) {
            switch (value) {
                case 0:
                    return OnStack;
                case 1:
                    return InRegister;
                default:
                    Util.shouldNotReachHere();
            }
            return null;
        }
    };

    public enum LocationType {
        Invalid,        // Invalid location
        Normal,         // Ints, floats, double halves
        Oop,            // Oop (please GC me!)
        IntInLong,      // Integer held in long register
        Long,           // Long held in one register
        FloatInDbl,     // Float held in double register
        Double,         // Double held in one register
        Addr,           // JSR return address
        NarrowOop;       // Narrow Oop (please GC me!)

        public static LocationType fromInt(int value) {
            switch (value) {
                case 0:
                    return Invalid;
                case 1:
                    return Normal;
                case 2:
                    return Oop;
                case 3:
                    return IntInLong;
                case 4:
                    return Long;
                case 5:
                    return FloatInDbl;
                case 6:
                    return Double;
                case 7:
                    return Addr;
                case 8:
                    return NarrowOop;
                default:
                    Util.shouldNotReachHere();
            }
            return null;
        }
    };

    private enum LocationMask{
        TypeMask(0x0F),
        TypeShift(0),
        WhereMask(0x10),
        WhereShift(4),
        OffsetMask(0xFFFFFFE0),
        OffsetShift(5);

        public final int value;

        LocationMask(final int value) {
            this.value = value;
        }
      };

    // Stack location Factory. Offset is 4-byte aligned; remove low bits
    static Location newStkLoc(LocationType t, int offset, int logBytesPerInt) {
        return new Location(Where.OnStack, t, offset >> logBytesPerInt); // TODO : Need to get the logBytesPerInt from the target statically
    }

    // Register location Factory
    public static Location newRegLoc(LocationType t, CiLocation reg) {
        return new Location(Where.InRegister, t, reg.first.number);
    }

    // Default constructor

    private Where where;
    private LocationType type;
    private int offset;

    /**
     * @param where
     * @param type
     * @param offset
     */
    public Location(Where where, LocationType type, int offset) {
        super();
        this.where = where;
        this.type = type;
        this.offset = offset;
    }


    /**
     * @param stream
     */
    public Location(DebugInfoReadStream stream) {
        int value = stream.readInt();
        where = Where.fromInt((value & LocationMask.WhereMask.value) >> LocationMask.WhereShift.value);
        type = LocationType.fromInt((value & LocationMask.TypeMask.value) >> LocationMask.TypeShift.value);
        offset = (value & LocationMask.OffsetMask.value) >> LocationMask.OffsetShift.value;
    }

    public Location() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Gets the where of this class.
     *
     * @return the where
     */
    public Where where() {
        return where;
    }


    /**
     * Gets the type of this class.
     *
     * @return the type
     */
    public LocationType type() {
        return type;
    }


    /**
     * Gets the offset of this class.
     *
     * @return the offset
     */
    public int offset() {
        return offset;
    }

    /**
     * Determines if the variable location is in a register.
     *
     * @return true if the variable is in a register
     */
    boolean isRegister() {
        return where == Where.InRegister;
    }

    /**
     * Determines if the variable location is on stack.
     *
     * @return true if the variable is on stack
     */
    boolean isStack() {
        return where == Where.OnStack;
    }

    public int stackOffset(int logBytesPerInt) {
        assert where == Where.OnStack : "wrong Where";
        return offset << logBytesPerInt;
    }

    public int registerNumber() {
        assert where() == Where.InRegister : "wrong Where";
        return offset;
    }

    public CiLocation reg() {
        assert where() == Where.InRegister : "wrong Where";
        return Location.asVMReg(offset);
    }

    public void printOn(LogStream out, int logBytesPerInt) {
        if (type == LocationType.Invalid) {
            // product of Location.invalidLoc() or Location.Location().
            switch (where) {
                case OnStack:
                    out.print("empty");
                    break;
                case InRegister:
                    out.print("invalid");
                    break;
            }
            return;
        }
        switch (where()) {
            case OnStack:
                out.printf("stack[%d]", stackOffset(logBytesPerInt));
                break;
            case InRegister:
                out.printf("reg %s [%d]", reg().toString(), registerNumber());
                break;
            default:
                out.printf("Wrong location where %d", where);
        }
        switch (type()) {
            case Normal:
                break;
            case Oop:
                out.print(",oop");
                break;
            case NarrowOop:
                out.print(",narrowoop");
                break;
            case IntInLong:
                out.print(",int");
                break;
            case Long:
                out.print(",long");
                break;
            case FloatInDbl:
                out.print(",float");
                break;
            case Double:
                out.print(",double");
                break;
            case Addr:
                out.print(",address");
                break;
            default:
                out.printf("Wrong location type %d", type);
        }
    }

    private int packLocationInfo() {
        return ((where.ordinal()  << LocationMask.WhereShift.value)   |
               (type.ordinal()   << LocationMask.TypeShift.value)     |
               ((offset << LocationMask.OffsetShift.value) & LocationMask.OffsetMask.value));

    }
    public void writeOn(DebugInfoWriteStream stream) {
        stream.writeInt(packLocationInfo());
    }

    public boolean legalOffsetInBytes(int offsetInBytes, int wordSize) {
        if ((offsetInBytes % wordSize) != 0) {
            return false;
        }
        return true;
    }

    /**
     * @param i
     * @return
     */
        // TODO Auto-generated method stub
    public static CiLocation asVMReg(int i) {
        return null;
    }
}
