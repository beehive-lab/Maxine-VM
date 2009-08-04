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

import com.sun.c1x.ci.CiLocation;
import com.sun.c1x.debug.LogStream;
import com.sun.c1x.debug.TTY;
import com.sun.c1x.util.Util;

/**
 * The <code>OopMapValue</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class OopMapValue {

    private short value;

    int value() {
        return value;
    }

    private void setValue(int value) {
        assert (value & 0xFFFF0000) == 0 : "Might be loosing information";
        this.value = (short) value;
    }

    private short contentReg;

    // Constants
    public enum OopMapValueBits {
        TypeBits(5),
        RegisterBits(Short.SIZE - OopMapValueBits.TypeBits.value);

        public final int value;

        OopMapValueBits(int value) {
            this.value = value;
        }
    }

    public enum OopMapValueShift {
        TypeShift(0),
        RegisterShift(OopMapValueBits.TypeBits.value);

        public final int value;

        OopMapValueShift(int value) {
            this.value = value;
        }
    }

    public enum OopMapValueMask {
        TypeMask(Util.rightNBits(OopMapValueBits.TypeBits.value)),
        TypeMaskInPlace(OopMapValueMask.TypeMask.value << OopMapValueShift.TypeShift.value),
        RegisterMask(Util.rightNBits(OopMapValueBits.RegisterBits.value)),
        RegisterMaskInPlace(OopMapValueMask.RegisterMask.value << OopMapValueShift.RegisterShift.value);

        public final int value;

        OopMapValueMask(int value) {
            this.value = value;
        }
    }

    public enum OopTypes { // must fit in typeBits
        UnusedValue, // powers of 2, for masking OopMapStream
        OopValue,
        ValueValue,
        NarrowOopValue,
        CalleeSavedValue,
        DerivedOopValue;

        public int mask() {
            if (ordinal() == 0) {
                return ordinal();
            } else {
                return 1 << ordinal();
            }
        }

        public static OopTypes fromInt(int value) {
            switch (value) {
                case 0:
                    return UnusedValue;
                case 1:
                    return OopValue;
                case 2:
                    return ValueValue;
                case 4:
                    return NarrowOopValue;
                case 8:
                    return CalleeSavedValue;
                case 16:
                    return DerivedOopValue;
                default:
                    Util.shouldNotReachHere();
                    return UnusedValue;
            }
        }
    }

    public static void printRegisterType(OopTypes x, CiLocation optional, LogStream st) {
        switch (x) {
            case OopValue:
                st.print("Oop");
                break;
            case ValueValue:
                st.print("Value");
                break;
            case NarrowOopValue:
                st.print("NarrowOop");
                break;
            case CalleeSavedValue:
                st.print("Callers_");
                st.print(optional.toString());
                break;
            case DerivedOopValue:
                st.print("DerivedOop_");
                st.print(optional.toString());
                break;
            default:
                Util.shouldNotReachHere();
        }
    }

    // Constructors
    public OopMapValue() {
        setValue(0);
        setContentReg(CiLocation.InvalidLocation);
    }

    public OopMapValue(CiLocation reg, OopTypes t) {
        setRegType(reg, t);
    }

    public OopMapValue(CiLocation reg, OopTypes t, CiLocation reg2) {
        setRegType(reg, t);
        setContentReg(reg2);
    }

    public OopMapValue(CompressedReadStream stream) {
        readFrom(stream);
    }

    // Archiving
    public void writeOn(CompressedWriteStream stream) {
        stream.writeInt(value());
        if (isCalleeSaved() || isDerivedOop()) {
            stream.writeInt(contentReg().first.number);
        }
    }

    public void readFrom(CompressedReadStream stream) {
        setValue(stream.readInt());
        if (isCalleeSaved() || isDerivedOop()) {
            setContentReg(OopMapValue.asVMReg((short) stream.readInt(), true));
        }
    }

    // Querying
    boolean isOop() {
        return (value & OopMapValueMask.TypeMaskInPlace.value) == OopTypes.OopValue.mask();
    }

    boolean isValue() {
        return (value() & OopMapValueMask.TypeMaskInPlace.value) == OopTypes.ValueValue.mask();
    }

    boolean isNarrowoop() {
        return (value() & OopMapValueMask.TypeMaskInPlace.value) == OopTypes.NarrowOopValue.mask();
    }

    boolean isCalleeSaved() {
        return (value() & OopMapValueMask.TypeMaskInPlace.value) == OopTypes.CalleeSavedValue.mask();
    }

    boolean isDerivedOop() {
        return (value() & OopMapValueMask.TypeMaskInPlace.value) == OopTypes.DerivedOopValue.mask();
    }

    void setOop() {
        setValue((value() & OopMapValueMask.RegisterMaskInPlace.value) | OopTypes.OopValue.mask());
    }

    void setValue() {
        setValue((value() & OopMapValueMask.RegisterMaskInPlace.value) | OopTypes.ValueValue.mask());
    }

    void setNarrowoop() {
        setValue((value() & OopMapValueMask.RegisterMaskInPlace.value) | OopTypes.NarrowOopValue.mask());
    }

    void setCalleeSaved() {
        setValue((value() & OopMapValueMask.RegisterMaskInPlace.value) | OopTypes.CalleeSavedValue.mask());
    }

    void setDerivedOop() {
        setValue((value() & OopMapValueMask.RegisterMaskInPlace.value) | OopTypes.DerivedOopValue.mask());
    }

    CiLocation reg() {
        return Location.asVMReg((value() & OopMapValueMask.RegisterMaskInPlace.value) >> OopMapValueShift.RegisterShift.value);
    }

    OopTypes type() {
        return OopTypes.fromInt(value() & OopMapValueMask.TypeMaskInPlace.value);
    }

    static boolean legalVmRegName(CiLocation p) {
        return (p.first.number == (p.first.number & OopMapValueMask.RegisterMask.value));
    }

    void setRegType(CiLocation p, OopTypes t) {
        setValue((p.first.number << OopMapValueShift.RegisterShift.value) | t.mask());
        assert reg() == p : "sanity check";
        assert type() == t : "sanity check";
    }

    CiLocation contentReg() {
        return OopMapValue.asVMReg(contentReg, true);
    }

    void setContentReg(CiLocation r) {
        contentReg = (short) r.first.number;
    }

    // Physical location queries
    boolean isRegisterLoc() {
        return reg().isRegister();
    }

    boolean isStackLoc() {
        return reg().isStackOffset();
    }

    // Returns offset from sp.
    int stackOffset() {
        assert isStackLoc() : "must be stack location";
        return reg().stackOffset;
    }

    void printOn(LogStream tty) {
        TTY.print(reg().toString());
        tty.print("=");
        printRegisterType(type(), contentReg(), tty);
        tty.print(" ");
    }

    void print() {
        printOn(TTY.out);
    }

    /**
     * @param contentReg
     * @param b
     * @return
     */
    public static CiLocation asVMReg(short contentReg, boolean b) {
        return Util.nonFatalUnimplemented(null);
    }
}
