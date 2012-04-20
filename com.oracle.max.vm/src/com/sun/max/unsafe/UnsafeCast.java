/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.unsafe;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import java.security.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reflection.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Any method annotated with {@link INTRINSIC}(UNSAFE_CAST) exists solely to provide an escape hatch from Java's type checking. All
 * such methods are recognized by the compiler to simply be an unsafe coercion from one type to another.
 *
 * Any method annotated with this annotation must take exactly one parameter (which will be the receiver if the method
 * is non-static ), have a non-void, non-generic return type. The type of the parameter is the type being
 * converted from and the return type is the type being converted to.
 *
 * The compiler must translate calls to these methods to simply replace the use of the result with the single parameter.
 *
 * A method annotated with the {@code INTRINSIC(UNSAFE_CAST)} may have an implementation (i.e. it is not {@code native} and not
 * {@code abstract}). This implementation is used to fold (i.e. compile-time evaluate) the method. The implementation
 * will simply be an explicit cast statement that results in a runtime type check when the method is
 * evaluated.
 */
public final class UnsafeCast {
    @HOSTED_ONLY
    private UnsafeCast() {
    }

    @INTRINSIC(UNSAFE_CAST) public static VmThread                 asVmThread(Object object) { return (VmThread) object; }
    @INTRINSIC(UNSAFE_CAST) public static Object[]                 asObjectArray(Object object) { return (Object[]) object; }
    @INTRINSIC(UNSAFE_CAST) public static Hybrid                   asHybrid(Object object) { return (Hybrid) object; }
    @INTRINSIC(UNSAFE_CAST) public static StackUnwindingContext    asStackUnwindingContext(Object object) { return (StackUnwindingContext) object; }
    @INTRINSIC(UNSAFE_CAST) public static Class                    asClass(Object object) { return (Class) object; }
    @INTRINSIC(UNSAFE_CAST) public static ClassRegistry            asClassRegistry(Object object) { return (ClassRegistry) object; }
    @INTRINSIC(UNSAFE_CAST) public static MethodInvocationStub     asMethodInvocationStub(Object object) { return (MethodInvocationStub) object; }
    @INTRINSIC(UNSAFE_CAST) public static ConstructorInvocationStub asConstructorInvocationStub(Object object) { return (ConstructorInvocationStub) object; }
    @INTRINSIC(UNSAFE_CAST) public static Throwable                asThrowable(Object object) { return (Throwable) object; }
    @INTRINSIC(UNSAFE_CAST) public static int[]                    asIntArray(Object object) { return (int[]) object; }
    @INTRINSIC(UNSAFE_CAST) public static DynamicHub               asDynamicHub(Object object) { return (DynamicHub) object; }
    @INTRINSIC(UNSAFE_CAST) public static Hub                      asHub(Object object) { return (Hub) object; }
    @INTRINSIC(UNSAFE_CAST) public static ArrayClassActor          asArrayClassActor(Object object) { return (ArrayClassActor) object; }
    @INTRINSIC(UNSAFE_CAST) public static ClassActor               asClassActor(Object object) { return (ClassActor) object; }
    @INTRINSIC(UNSAFE_CAST) public static FieldActor               asFieldActor(Object object) { return (FieldActor) object; }
    @INTRINSIC(UNSAFE_CAST) public static MethodActor              asClassMethodActor(Object object) { return (ClassMethodActor) object; }
    @INTRINSIC(UNSAFE_CAST) public static StaticMethodActor        asStaticMethodActor(Object object) { return (StaticMethodActor) object; }
    @INTRINSIC(UNSAFE_CAST) public static VirtualMethodActor       asVirtualMethodActor(Object object) { return (VirtualMethodActor) object; }
    @INTRINSIC(UNSAFE_CAST) public static AccessControlContext     asAccessControlContext(Object object) { return (AccessControlContext) object; }
    @INTRINSIC(UNSAFE_CAST) public static InterfaceActor           asInterfaceActor(Object object) { return (InterfaceActor) object; }
    @INTRINSIC(UNSAFE_CAST) public static InterfaceMethodActor     asInterfaceMethodActor(Object object) { return (InterfaceMethodActor) object; }

    @INTRINSIC(UNSAFE_CAST) public static Address                  asAddress(int value) { return Address.fromUnsignedInt(value); }
    @INTRINSIC(UNSAFE_CAST) public static Address                  asAddress(long value) { return Address.fromLong(value); }
    @INTRINSIC(UNSAFE_CAST) public static Offset                   asOffset(int value) { return Offset.fromUnsignedInt(value); }
    @INTRINSIC(UNSAFE_CAST) public static Offset                   asOffset(long value) { return Offset.fromLong(value); }

    @INTRINSIC(UNSAFE_CAST) public static int                      asInt(Word word) { return word.asAddress().toInt(); }
    @INTRINSIC(UNSAFE_CAST) public static long                     asLong(Word word) { return word.asAddress().toLong(); }
    @INTRINSIC(UNSAFE_CAST) public static boolean                  asBoolean(byte value) { return value != 0; }
    @INTRINSIC(UNSAFE_CAST) public static byte                     asByte(boolean value) { return value ? 1 : (byte) 0; }
    @INTRINSIC(UNSAFE_CAST) public static int                      asInt(boolean value) { return value ? 1 : 0; }
    @INTRINSIC(UNSAFE_CAST) public static char                     asChar(short value) { return (char) value; }
    @INTRINSIC(UNSAFE_CAST) public static short                    asShort(char value) { return (short) value; }

    @INTRINSIC(UNSAFE_CAST) public static CodePointer              asCodePointer(long value) { return CodePointer.from(value); }
    @INTRINSIC(UNSAFE_CAST) public static CodePointer              asCodePointerTagged(long value) { return CodePointer.fromTaggedLong(value); }
    @INTRINSIC(UNSAFE_CAST) public static long                     asLong(CodePointer cp) { return cp.toLong(); }
    @INTRINSIC(UNSAFE_CAST) public static long                     asTaggedLong(CodePointer cp) { return cp.toTaggedLong(); }
}
