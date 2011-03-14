/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.template.source;

import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveInstanceFieldForReading.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveInstanceFieldForWriting.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveInterfaceMethod.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveStaticFieldForReading.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveStaticFieldForWriting.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveVirtualMethod.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.MethodSelectionSnippet.SelectInterfaceMethod;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveClass;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveClassForNew;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveSpecialMethod;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveStaticMethod;
import com.sun.max.vm.compiler.snippet.Snippet.MakeClassInitialized;
import com.sun.max.vm.compiler.snippet.Snippet.MakeEntrypoint;
import com.sun.max.vm.compiler.snippet.Snippet.MakeHolderInitialized;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;

public class NoninlineTemplateRuntime {
    //--------------------------------------------------------------------
    // Out of line code:
    // Unresolved cases for complex bytecodes are kept out of line.
    //--------------------------------------------------------------------

    @NEVER_INLINE
    public static Address resolveAndSelectVirtualMethod(Object receiver, ResolutionGuard.InPool guard, int receiverStackIndex) {
        final VirtualMethodActor virtualMethodActor = resolveVirtualMethod(guard);
        return MethodSelectionSnippet.SelectVirtualMethod.selectNonPrivateVirtualMethod(receiver, virtualMethodActor).asAddress();
    }

    @NEVER_INLINE
    public static Address resolveAndSelectInterfaceMethod(ResolutionGuard.InPool guard, final Object receiver) {
        final InterfaceMethodActor declaredInterfaceMethod = resolveInterfaceMethod(guard);
        final Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, declaredInterfaceMethod).asAddress();
        return entryPoint;
    }

    @NEVER_INLINE
    public static Address resolveSpecialMethod(ResolutionGuard.InPool guard) {
        final VirtualMethodActor virtualMethod = ResolveSpecialMethod.resolveSpecialMethod(guard);
        return MakeEntrypoint.makeEntrypoint(virtualMethod);
    }

    @NEVER_INLINE
    public static Address resolveStaticMethod(ResolutionGuard.InPool guard) {
        final StaticMethodActor staticMethod = ResolveStaticMethod.resolveStaticMethod(guard);
        MakeHolderInitialized.makeHolderInitialized(staticMethod);
        return MakeEntrypoint.makeEntrypoint(staticMethod);
    }

    @NEVER_INLINE
    public static Object resolveClassForNewAndCreate(ResolutionGuard guard) {
        final ClassActor classActor = ResolveClassForNew.resolveClassForNew(guard);
        MakeClassInitialized.makeClassInitialized(classActor);
        final Object tuple = CreateTupleOrHybrid.createTupleOrHybrid(classActor);
        return tuple;
    }

    @NEVER_INLINE
    public static void noninlineArrayStore(final int index, final Object array, final Object value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.checkSetObject(array, value);
        ArrayAccess.setObject(array, index, value);
    }

    //== Putfield routines ====================================================================================

    @NEVER_INLINE
    public static void resolveAndPutFieldReference(ResolutionGuard.InPool guard, final Object object, final Object value) {
        final FieldActor fieldActor = resolveInstanceFieldForWriting(guard);
        FieldWriteSnippet.WriteReference.writeReference(object, fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldWord(ResolutionGuard.InPool guard, final Object object, final Word value) {
        final FieldActor fieldActor = resolveInstanceFieldForWriting(guard);
        FieldWriteSnippet.WriteWord.writeWord(object, fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldBoolean(ResolutionGuard.InPool guard, final Object object, final boolean value) {
        final FieldActor fieldActor = resolveInstanceFieldForWriting(guard);
        FieldWriteSnippet.WriteBoolean.writeBoolean(object, fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldByte(ResolutionGuard.InPool guard, final Object object, final byte value) {
        final FieldActor fieldActor = resolveInstanceFieldForWriting(guard);
        FieldWriteSnippet.WriteByte.writeByte(object, fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldShort(ResolutionGuard.InPool guard, final Object object, final short value) {
        final FieldActor fieldActor = resolveInstanceFieldForWriting(guard);
        FieldWriteSnippet.WriteShort.writeShort(object, fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldChar(ResolutionGuard.InPool guard, final Object object, final char value) {
        final FieldActor fieldActor = resolveInstanceFieldForWriting(guard);
        FieldWriteSnippet.WriteChar.writeChar(object, fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldInt(ResolutionGuard.InPool guard, final Object object, final int value) {
        final FieldActor fieldActor = resolveInstanceFieldForWriting(guard);
        FieldWriteSnippet.WriteInt.writeInt(object, fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldLong(ResolutionGuard.InPool guard, final Object object, final long value) {
        final FieldActor fieldActor = resolveInstanceFieldForWriting(guard);
        FieldWriteSnippet.WriteLong.writeLong(object, fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldFloat(ResolutionGuard.InPool guard, final Object object, final float value) {
        final FieldActor fieldActor = resolveInstanceFieldForWriting(guard);
        FieldWriteSnippet.WriteFloat.writeFloat(object, fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldDouble(ResolutionGuard.InPool guard, final Object object, final double value) {
        final FieldActor fieldActor = resolveInstanceFieldForWriting(guard);
        FieldWriteSnippet.WriteDouble.writeDouble(object, fieldActor, value);
    }

    // ==========================================================================================================
    // == Putstatic routines ====================================================================================
    // ==========================================================================================================

    @NEVER_INLINE
    public static void resolveAndPutStaticReference(ResolutionGuard.InPool guard, final Object value) {
        final FieldActor fieldActor = resolvePutstaticFieldActor(guard);
        FieldWriteSnippet.WriteReference.writeReference(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticWord(ResolutionGuard.InPool guard, final Word value) {
        final FieldActor fieldActor = resolvePutstaticFieldActor(guard);
        FieldWriteSnippet.WriteWord.writeWord(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticBoolean(ResolutionGuard.InPool guard, final boolean value) {
        final FieldActor fieldActor = resolvePutstaticFieldActor(guard);
        FieldWriteSnippet.WriteBoolean.writeBoolean(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticByte(ResolutionGuard.InPool guard, final byte value) {
        final FieldActor fieldActor = resolvePutstaticFieldActor(guard);
        FieldWriteSnippet.WriteByte.writeByte(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticShort(ResolutionGuard.InPool guard, final short value) {
        final FieldActor fieldActor = resolvePutstaticFieldActor(guard);
        FieldWriteSnippet.WriteShort.writeShort(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticChar(ResolutionGuard.InPool guard, final char value) {
        final FieldActor fieldActor = resolvePutstaticFieldActor(guard);
        FieldWriteSnippet.WriteChar.writeChar(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticInt(ResolutionGuard.InPool guard, final int value) {
        final FieldActor fieldActor = resolvePutstaticFieldActor(guard);
        FieldWriteSnippet.WriteInt.writeInt(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticLong(ResolutionGuard.InPool guard, final long value) {
        final FieldActor fieldActor = resolvePutstaticFieldActor(guard);
        FieldWriteSnippet.WriteLong.writeLong(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticFloat(ResolutionGuard.InPool guard, final float value) {
        final FieldActor fieldActor = resolvePutstaticFieldActor(guard);
        FieldWriteSnippet.WriteFloat.writeFloat(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticDouble(ResolutionGuard.InPool guard, final double value) {
        final FieldActor fieldActor = resolvePutstaticFieldActor(guard);
        FieldWriteSnippet.WriteDouble.writeDouble(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @INLINE
    private static FieldActor resolvePutstaticFieldActor(ResolutionGuard.InPool guard) {
        final FieldActor fieldActor = resolveStaticFieldForWriting(guard);
        MakeHolderInitialized.makeHolderInitialized(fieldActor);
        return fieldActor;
    }

    // ==========================================================================================================
    // == Getfield routines =====================================================================================
    // ==========================================================================================================

    @NEVER_INLINE
    public static Object resolveAndGetFieldReference(ResolutionGuard.InPool guard, final Object object) {
        final FieldActor fieldActor = resolveInstanceFieldForReading(guard);
        return FieldReadSnippet.ReadReference.readReference(object, fieldActor);
    }

    @NEVER_INLINE
    public static Word resolveAndGetFieldWord(ResolutionGuard.InPool guard, final Object object) {
        final FieldActor fieldActor = resolveInstanceFieldForReading(guard);
        return FieldReadSnippet.ReadWord.readWord(object, fieldActor);
    }

    @NEVER_INLINE
    public static boolean resolveAndGetFieldBoolean(ResolutionGuard.InPool guard, final Object object) {
        final FieldActor fieldActor = resolveInstanceFieldForReading(guard);
        return FieldReadSnippet.ReadBoolean.readBoolean(object, fieldActor);
    }

    @NEVER_INLINE
    public static byte resolveAndGetFieldByte(ResolutionGuard.InPool guard, final Object object) {
        final FieldActor fieldActor = resolveInstanceFieldForReading(guard);
        return FieldReadSnippet.ReadByte.readByte(object, fieldActor);
    }

    @NEVER_INLINE
    public static short resolveAndGetFieldShort(ResolutionGuard.InPool guard, final Object object) {
        final FieldActor fieldActor = resolveInstanceFieldForReading(guard);
        return FieldReadSnippet.ReadShort.readShort(object, fieldActor);
    }

    @NEVER_INLINE
    public static char resolveAndGetFieldChar(ResolutionGuard.InPool guard, final Object object) {
        final FieldActor fieldActor = resolveInstanceFieldForReading(guard);
        return FieldReadSnippet.ReadChar.readChar(object, fieldActor);
    }

    @NEVER_INLINE
    public static int resolveAndGetFieldInt(ResolutionGuard.InPool guard, final Object object) {
        final FieldActor fieldActor = resolveInstanceFieldForReading(guard);
        return FieldReadSnippet.ReadInt.readInt(object, fieldActor);
    }

    @NEVER_INLINE
    public static long resolveAndGetFieldLong(ResolutionGuard.InPool guard, final Object object) {
        final FieldActor fieldActor = resolveInstanceFieldForReading(guard);
        return FieldReadSnippet.ReadLong.readLong(object, fieldActor);
    }

    @NEVER_INLINE
    public static float resolveAndGetFieldFloat(ResolutionGuard.InPool guard, final Object object) {
        final FieldActor fieldActor = resolveInstanceFieldForReading(guard);
        return FieldReadSnippet.ReadFloat.readFloat(object, fieldActor);
    }

    @NEVER_INLINE
    public static double resolveAndGetFieldDouble(ResolutionGuard.InPool guard, final Object object) {
        final FieldActor fieldActor = resolveInstanceFieldForReading(guard);
        return FieldReadSnippet.ReadDouble.readDouble(object, fieldActor);
    }

    // ==========================================================================================================
    // == Getstatic routines =====================================================================================
    // ==========================================================================================================

    @INLINE
    private static FieldActor getstaticFieldActor(ResolutionGuard.InPool guard) {
        final FieldActor fieldActor = resolveStaticFieldForReading(guard);
        MakeHolderInitialized.makeHolderInitialized(fieldActor);
        return fieldActor;
    }

    @NEVER_INLINE
    public static Object resolveAndGetStaticReference(ResolutionGuard.InPool guard) {
        final FieldActor fieldActor = getstaticFieldActor(guard);
        return FieldReadSnippet.ReadReference.readReference(fieldActor.holder().staticTuple(), fieldActor);
    }

    @NEVER_INLINE
    public static Word resolveAndGetStaticWord(ResolutionGuard.InPool guard) {
        final FieldActor fieldActor = getstaticFieldActor(guard);
        return FieldReadSnippet.ReadWord.readWord(fieldActor.holder().staticTuple(), fieldActor);
    }

    @NEVER_INLINE
    public static boolean resolveAndGetStaticBoolean(ResolutionGuard.InPool guard) {
        final FieldActor fieldActor = getstaticFieldActor(guard);
        return FieldReadSnippet.ReadBoolean.readBoolean(fieldActor.holder().staticTuple(), fieldActor);
    }

    @NEVER_INLINE
    public static byte resolveAndGetStaticByte(ResolutionGuard.InPool guard) {
        final FieldActor fieldActor = getstaticFieldActor(guard);
        return FieldReadSnippet.ReadByte.readByte(fieldActor.holder().staticTuple(), fieldActor);
    }

    @NEVER_INLINE
    public static short resolveAndGetStaticShort(ResolutionGuard.InPool guard) {
        final FieldActor fieldActor = getstaticFieldActor(guard);
        return FieldReadSnippet.ReadShort.readShort(fieldActor.holder().staticTuple(), fieldActor);
    }

    @NEVER_INLINE
    public static char resolveAndGetStaticChar(ResolutionGuard.InPool guard) {
        final FieldActor fieldActor = getstaticFieldActor(guard);
        return FieldReadSnippet.ReadChar.readChar(fieldActor.holder().staticTuple(), fieldActor);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticInt(ResolutionGuard.InPool guard) {
        final FieldActor fieldActor = getstaticFieldActor(guard);
        return FieldReadSnippet.ReadInt.readInt(fieldActor.holder().staticTuple(), fieldActor);
    }

    @NEVER_INLINE
    public static long resolveAndGetStaticLong(ResolutionGuard.InPool guard) {
        final FieldActor fieldActor = getstaticFieldActor(guard);
        return FieldReadSnippet.ReadLong.readLong(fieldActor.holder().staticTuple(), fieldActor);
    }

    @NEVER_INLINE
    public static float resolveAndGetStaticFloat(ResolutionGuard.InPool guard) {
        final FieldActor fieldActor = getstaticFieldActor(guard);
        return FieldReadSnippet.ReadFloat.readFloat(fieldActor.holder().staticTuple(), fieldActor);
    }

    @NEVER_INLINE
    public static double resolveAndGetStaticDouble(ResolutionGuard.InPool guard) {
        final FieldActor fieldActor = getstaticFieldActor(guard);
        return FieldReadSnippet.ReadDouble.readDouble(fieldActor.holder().staticTuple(), fieldActor);
    }

    @NEVER_INLINE
    public static void resolveAndCheckcast(ResolutionGuard guard, final Object object) {
        Snippet.CheckCast.checkCast(ResolveClass.resolveClass(guard), object);
    }

    @NEVER_INLINE
    public static Object resolveMirror(ResolutionGuard guard) {
        return ResolveClass.resolveClass(guard).javaClass();
    }

    @NEVER_INLINE
    public static Object getClassMirror(ClassActor classActor) {
        return classActor.javaClass();
    }

    @NEVER_INLINE
    public static Object noninlineNew(ClassActor classActor) {
        return CreateTupleOrHybrid.createTupleOrHybrid(classActor);
    }

    @NEVER_INLINE
    public static Throwable loadException() {
        return CPSAbstractCompiler.safepointAndLoadExceptionObject();
    }

    @NEVER_INLINE
    public static int f2i(float value) {
        return (int) value;
    }

    @NEVER_INLINE
    public static long f2l(float value) {
        return (long) value;
    }

    @NEVER_INLINE
    public static int d2i(double value) {
        return (int) value;
    }

    @NEVER_INLINE
    public static long d2l(double value) {
        return (long) value;
    }
}
