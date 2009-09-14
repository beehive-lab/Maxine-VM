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
package com.sun.max.unsafe;

import java.lang.ref.*;
import java.security.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reflection.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * A collection of methods used to perform {@link UNSAFE_CAST unchecked type casts}.
 *
 * The bodies of these methods exist solely so that the methods can be {@linkplain FOLD folded}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class UnsafeLoophole {
    @PROTOTYPE_ONLY
    private UnsafeLoophole() {
    }

    @UNSAFE_CAST public static VmThread                 asVmThread(Object object) { return (VmThread) object; }
    @UNSAFE_CAST public static Object[]                 asObjectArray(Object object) { return (Object[]) object; }
    @UNSAFE_CAST public static Hybrid                   asHybrid(Object object) { return (Hybrid) object; }
    @UNSAFE_CAST public static StackUnwindingContext    asStackUnwindingContext(Object object) { return (StackUnwindingContext) object; }
    @UNSAFE_CAST public static Class                    asClass(Object object) { return (Class) object; }
    @UNSAFE_CAST public static GeneratedMethodStub      asGeneratedMethodStub(Object object) { return (GeneratedMethodStub) object; }
    @UNSAFE_CAST public static GeneratedConstructorStub asGeneratedConstructorStub(Object object) { return (GeneratedConstructorStub) object; }
    @UNSAFE_CAST public static Throwable                asThrowable(Object object) { return (Throwable) object; }
    @UNSAFE_CAST public static int[]                    asIntArray(Object object) { return (int[]) object; }
    @UNSAFE_CAST public static DynamicHub               asDynamicHub(Object object) { return (DynamicHub) object; }
    @UNSAFE_CAST public static Grip                     asGrip(Object object) { return (Grip) object; }
    @UNSAFE_CAST public static Hub                      asHub(Object object) { return (Hub) object; }
    @UNSAFE_CAST public static ArrayClassActor          asArrayClassActor(Object object) { return (ArrayClassActor) object; }
    @UNSAFE_CAST public static ClassActor               asClassActor(Object object) { return (ClassActor) object; }
    @UNSAFE_CAST public static FieldActor               asFieldActor(Object object) { return (FieldActor) object; }
    @UNSAFE_CAST public static StaticMethodActor        asStaticMethodActor(Object object) { return (StaticMethodActor) object; }
    @UNSAFE_CAST public static VirtualMethodActor       asVirtualMethodActor(Object object) { return (VirtualMethodActor) object; }
    @UNSAFE_CAST public static AccessControlContext     asAccessControlContext(Object object) { return (AccessControlContext) object; }
    @UNSAFE_CAST public static Reference                asJDKReference(Object object) { return (Reference) object; }
    @UNSAFE_CAST public static InterfaceActor           asInterfaceActor(Object object) { return (InterfaceActor) object; }
    @UNSAFE_CAST public static InterfaceMethodActor     asInterfaceMethodActor(Object object) { return (InterfaceMethodActor) object; }

    @UNSAFE_CAST public static Address                  asAddress(int value) { return Address.fromUnsignedInt(value); }
    @UNSAFE_CAST public static Address                  asAddress(long value) { return Address.fromLong(value); }
    @UNSAFE_CAST public static Offset                   asOffset(int value) { return Offset.fromUnsignedInt(value); }
    @UNSAFE_CAST public static Offset                   asOffset(long value) { return Offset.fromLong(value); }

    @UNSAFE_CAST public static int                      asInt(Word word) { return word.asAddress().toInt(); }
    @UNSAFE_CAST public static long                     asLong(Word word) { return word.asAddress().toLong(); }
    @UNSAFE_CAST public static boolean                  asBoolean(byte value) { return value != 0; }
    @UNSAFE_CAST public static byte                     asByte(boolean value) { return value ? 1 : (byte) 0; }
    @UNSAFE_CAST public static char                     asChar(short value) { return (char) value; }
    @UNSAFE_CAST public static short                    asShort(char value) { return (short) value; }

    @BUILTIN(builtinClass = SpecialBuiltin.IntToFloat.class)
    public static float asFloat(int value) {
        return Float.intBitsToFloat(value);
    }

    @BUILTIN(builtinClass = SpecialBuiltin.FloatToInt.class)
    public static int asInt(float value) {
        return Float.floatToRawIntBits(value);
    }

    @BUILTIN(builtinClass = SpecialBuiltin.LongToDouble.class)
    public static double asDouble(long value) {
        return Double.longBitsToDouble(value);
    }

    @BUILTIN(builtinClass = SpecialBuiltin.DoubleToLong.class)
    public static long asLong(double value) {
        return Double.doubleToRawLongBits(value);
    }
}
