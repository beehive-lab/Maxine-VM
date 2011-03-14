/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.t1x;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.bytecode.Bytecodes.MemoryBarriers.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveInstanceFieldForReading.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveInstanceFieldForWriting.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveInterfaceMethod.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveStaticFieldForReading.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveStaticFieldForWriting.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveVirtualMethod.*;
import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.io.*;

import com.sun.cri.bytecode.*;
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
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * Collection of methods called from T1X templates. These methods are typically non-inlined
 * to satisfy invariants enforced for the templates. They can also be used to keep the
 * template code small.
 *
 * @author Doug Simon
 */
@NEVER_INLINE
public class T1XRuntime {

    // ==========================================================================================================
    // == Resolution routines ===================================================================================
    // ==========================================================================================================

    public static Address resolveAndSelectVirtualMethod(Object receiver, ResolutionGuard.InPool guard, int receiverStackIndex) {
        final VirtualMethodActor virtualMethodActor = resolveVirtualMethod(guard);
        return MethodSelectionSnippet.SelectVirtualMethod.selectNonPrivateVirtualMethod(receiver, virtualMethodActor).asAddress();
    }

    public static Address resolveAndSelectInterfaceMethod(ResolutionGuard.InPool guard, final Object receiver) {
        final InterfaceMethodActor declaredInterfaceMethod = resolveInterfaceMethod(guard);
        final Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, declaredInterfaceMethod).asAddress();
        return entryPoint;
    }

    public static Address resolveSpecialMethod(ResolutionGuard.InPool guard) {
        final VirtualMethodActor virtualMethod = ResolveSpecialMethod.resolveSpecialMethod(guard);
        return MakeEntrypoint.makeEntrypoint(virtualMethod);
    }

    public static Address resolveStaticMethod(ResolutionGuard.InPool guard) {
        final StaticMethodActor staticMethod = ResolveStaticMethod.resolveStaticMethod(guard);
        MakeHolderInitialized.makeHolderInitialized(staticMethod);
        return MakeEntrypoint.makeEntrypoint(staticMethod);
    }

    public static Object resolveClassForNewAndCreate(ResolutionGuard guard) {
        final ClassActor classActor = ResolveClassForNew.resolveClassForNew(guard);
        MakeClassInitialized.makeClassInitialized(classActor);
        final Object tuple = CreateTupleOrHybrid.createTupleOrHybrid(classActor);
        return tuple;
    }

    public static Object resolveMirror(ResolutionGuard guard) {
        return ResolveClass.resolveClass(guard).javaClass();
    }

    // ==========================================================================================================
    // == Field access           ================================================================================
    // ==========================================================================================================

    /**
     * Inserts any necessary memory barriers before a volatile read as required by the JMM.
     */
    @INLINE
    private static void preVolatileRead() {
    }

    /**
     * Inserts any necessary memory barriers after a volatile read as required by the JMM.
     */
    @INTRINSIC(MEMBAR | ((LOAD_LOAD | LOAD_STORE) << 8))
    private static native void postVolatileRead();

    /**
     * Inserts any necessary memory barriers before a volatile read as required by the JMM.
     */
    @INTRINSIC(MEMBAR | ((LOAD_STORE | STORE_STORE) << 8))
    private static native void preVolatileWrite();

    /**
     * Inserts any necessary memory barriers after a volatile read as required by the JMM.
     */
    @INTRINSIC(MEMBAR | ((STORE_LOAD | STORE_STORE) << 8))
    private static native void postVolatileWrite();

    /**
     * Manual macros, Java style, for field access routines.
     */
    @HOSTED_ONLY
    public static void main(String[] args) {
        PrintStream o = System.out;
        String[] kinds = {"boolean", "byte", "char", "short", "int", "float", "long", "double", "Reference", "Word"};
        for (String k : kinds) {
            String uK = k.substring(0, 1).toUpperCase() + k.substring(1);
            String t = k.equals("Reference") ? "Object" : k;
            o.printf("    // GENERATED -- EDIT AND RUN main() TO MODIFY%n");
            o.printf("    public static %s resolveAndGetField%s(ResolutionGuard.InPool guard, Object object) {%n", t, uK);
            o.printf("        FieldActor f = resolveInstanceFieldForReading(guard);%n");
            o.printf("        if (f.isVolatile()) {%n");
            o.printf("            preVolatileRead();%n");
            o.printf("            %s value = FieldReadSnippet.Read%s.read%s(object, f);%n", t, uK, uK);
            o.printf("            postVolatileRead();%n");
            o.printf("            return value;%n");
            o.printf("        } else {%n");
            o.printf("            return FieldReadSnippet.Read%s.read%s(object, f);%n", uK, uK);
            o.printf("        }%n");
            o.printf("    }%n");
            o.printf("%n");
            o.printf("    // GENERATED -- EDIT AND RUN main() TO MODIFY%n");
            o.printf("    public static void resolveAndPutField%s(ResolutionGuard.InPool guard, Object object, %s value) {%n", uK, t);
            o.printf("        FieldActor f = resolveInstanceFieldForWriting(guard);%n");
            o.printf("        if (f.isVolatile()) {%n");
            o.printf("            preVolatileWrite();%n");
            o.printf("            FieldWriteSnippet.Write%s.write%s(object, f, value);%n", uK, uK);
            o.printf("            postVolatileWrite();%n");
            o.printf("        } else {%n");
            o.printf("            FieldWriteSnippet.Write%s.write%s(object, f, value);%n", uK, uK);
            o.printf("        }%n");
            o.printf("    }%n");
            o.printf("%n");
            o.printf("    // GENERATED -- EDIT AND RUN main() TO MODIFY%n");
            o.printf("    public static %s resolveAndGetStatic%s(ResolutionGuard.InPool guard) {%n", t, uK);
            o.printf("        FieldActor f = resolveStaticFieldForReading(guard);%n");
            o.printf("        MakeHolderInitialized.makeHolderInitialized(f);%n");
            o.printf("        if (f.isVolatile()) {%n");
            o.printf("            preVolatileRead();%n");
            o.printf("            %s value = FieldReadSnippet.Read%s.read%s(f.holder().staticTuple(), f);%n", t, uK, uK);
            o.printf("            postVolatileRead();%n");
            o.printf("            return value;%n");
            o.printf("        } else {%n");
            o.printf("            return FieldReadSnippet.Read%s.read%s(f.holder().staticTuple(), f);%n", uK, uK);
            o.printf("        }%n");
            o.printf("    }%n");
            o.printf("%n");
            o.printf("    // GENERATED -- EDIT AND RUN main() TO MODIFY%n");
            o.printf("    public static void resolveAndPutStatic%s(ResolutionGuard.InPool guard, %s value) {%n", uK, t);
            o.printf("        FieldActor f = resolveStaticFieldForWriting(guard);%n");
            o.printf("        MakeHolderInitialized.makeHolderInitialized(f);%n");
            o.printf("        if (f.isVolatile()) {%n");
            o.printf("            preVolatileWrite();%n");
            o.printf("            FieldWriteSnippet.Write%s.write%s(f.holder().staticTuple(), f, value);%n", uK, uK);
            o.printf("            postVolatileWrite();%n");
            o.printf("        } else {%n");
            o.printf("            FieldWriteSnippet.Write%s.write%s(f.holder().staticTuple(), f, value);%n", uK, uK);
            o.printf("        }%n");
            o.printf("    }%n");
            o.printf("%n");
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static boolean resolveAndGetFieldBoolean(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            boolean value = FieldReadSnippet.ReadBoolean.readBoolean(object, f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadBoolean.readBoolean(object, f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldBoolean(ResolutionGuard.InPool guard, Object object, boolean value) {
        FieldActor f = resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteBoolean.writeBoolean(object, f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteBoolean.writeBoolean(object, f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static boolean resolveAndGetStaticBoolean(ResolutionGuard.InPool guard) {
        FieldActor f = resolveStaticFieldForReading(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            boolean value = FieldReadSnippet.ReadBoolean.readBoolean(f.holder().staticTuple(), f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadBoolean.readBoolean(f.holder().staticTuple(), f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticBoolean(ResolutionGuard.InPool guard, boolean value) {
        FieldActor f = resolveStaticFieldForWriting(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteBoolean.writeBoolean(f.holder().staticTuple(), f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteBoolean.writeBoolean(f.holder().staticTuple(), f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static byte resolveAndGetFieldByte(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            byte value = FieldReadSnippet.ReadByte.readByte(object, f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadByte.readByte(object, f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldByte(ResolutionGuard.InPool guard, Object object, byte value) {
        FieldActor f = resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteByte.writeByte(object, f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteByte.writeByte(object, f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static byte resolveAndGetStaticByte(ResolutionGuard.InPool guard) {
        FieldActor f = resolveStaticFieldForReading(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            byte value = FieldReadSnippet.ReadByte.readByte(f.holder().staticTuple(), f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadByte.readByte(f.holder().staticTuple(), f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticByte(ResolutionGuard.InPool guard, byte value) {
        FieldActor f = resolveStaticFieldForWriting(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteByte.writeByte(f.holder().staticTuple(), f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteByte.writeByte(f.holder().staticTuple(), f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static char resolveAndGetFieldChar(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            char value = FieldReadSnippet.ReadChar.readChar(object, f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadChar.readChar(object, f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldChar(ResolutionGuard.InPool guard, Object object, char value) {
        FieldActor f = resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteChar.writeChar(object, f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteChar.writeChar(object, f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static char resolveAndGetStaticChar(ResolutionGuard.InPool guard) {
        FieldActor f = resolveStaticFieldForReading(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            char value = FieldReadSnippet.ReadChar.readChar(f.holder().staticTuple(), f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadChar.readChar(f.holder().staticTuple(), f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticChar(ResolutionGuard.InPool guard, char value) {
        FieldActor f = resolveStaticFieldForWriting(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteChar.writeChar(f.holder().staticTuple(), f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteChar.writeChar(f.holder().staticTuple(), f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static short resolveAndGetFieldShort(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            short value = FieldReadSnippet.ReadShort.readShort(object, f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadShort.readShort(object, f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldShort(ResolutionGuard.InPool guard, Object object, short value) {
        FieldActor f = resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteShort.writeShort(object, f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteShort.writeShort(object, f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static short resolveAndGetStaticShort(ResolutionGuard.InPool guard) {
        FieldActor f = resolveStaticFieldForReading(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            short value = FieldReadSnippet.ReadShort.readShort(f.holder().staticTuple(), f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadShort.readShort(f.holder().staticTuple(), f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticShort(ResolutionGuard.InPool guard, short value) {
        FieldActor f = resolveStaticFieldForWriting(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteShort.writeShort(f.holder().staticTuple(), f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteShort.writeShort(f.holder().staticTuple(), f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static int resolveAndGetFieldInt(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            int value = FieldReadSnippet.ReadInt.readInt(object, f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadInt.readInt(object, f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldInt(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteInt.writeInt(object, f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteInt.writeInt(object, f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static int resolveAndGetStaticInt(ResolutionGuard.InPool guard) {
        FieldActor f = resolveStaticFieldForReading(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            int value = FieldReadSnippet.ReadInt.readInt(f.holder().staticTuple(), f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadInt.readInt(f.holder().staticTuple(), f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticInt(ResolutionGuard.InPool guard, int value) {
        FieldActor f = resolveStaticFieldForWriting(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteInt.writeInt(f.holder().staticTuple(), f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteInt.writeInt(f.holder().staticTuple(), f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static float resolveAndGetFieldFloat(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            float value = FieldReadSnippet.ReadFloat.readFloat(object, f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadFloat.readFloat(object, f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldFloat(ResolutionGuard.InPool guard, Object object, float value) {
        FieldActor f = resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteFloat.writeFloat(object, f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteFloat.writeFloat(object, f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static float resolveAndGetStaticFloat(ResolutionGuard.InPool guard) {
        FieldActor f = resolveStaticFieldForReading(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            float value = FieldReadSnippet.ReadFloat.readFloat(f.holder().staticTuple(), f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadFloat.readFloat(f.holder().staticTuple(), f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticFloat(ResolutionGuard.InPool guard, float value) {
        FieldActor f = resolveStaticFieldForWriting(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteFloat.writeFloat(f.holder().staticTuple(), f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteFloat.writeFloat(f.holder().staticTuple(), f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static long resolveAndGetFieldLong(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            long value = FieldReadSnippet.ReadLong.readLong(object, f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadLong.readLong(object, f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldLong(ResolutionGuard.InPool guard, Object object, long value) {
        FieldActor f = resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteLong.writeLong(object, f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteLong.writeLong(object, f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static long resolveAndGetStaticLong(ResolutionGuard.InPool guard) {
        FieldActor f = resolveStaticFieldForReading(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            long value = FieldReadSnippet.ReadLong.readLong(f.holder().staticTuple(), f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadLong.readLong(f.holder().staticTuple(), f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticLong(ResolutionGuard.InPool guard, long value) {
        FieldActor f = resolveStaticFieldForWriting(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteLong.writeLong(f.holder().staticTuple(), f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteLong.writeLong(f.holder().staticTuple(), f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static double resolveAndGetFieldDouble(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            double value = FieldReadSnippet.ReadDouble.readDouble(object, f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadDouble.readDouble(object, f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldDouble(ResolutionGuard.InPool guard, Object object, double value) {
        FieldActor f = resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteDouble.writeDouble(object, f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteDouble.writeDouble(object, f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static double resolveAndGetStaticDouble(ResolutionGuard.InPool guard) {
        FieldActor f = resolveStaticFieldForReading(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            double value = FieldReadSnippet.ReadDouble.readDouble(f.holder().staticTuple(), f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadDouble.readDouble(f.holder().staticTuple(), f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticDouble(ResolutionGuard.InPool guard, double value) {
        FieldActor f = resolveStaticFieldForWriting(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteDouble.writeDouble(f.holder().staticTuple(), f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteDouble.writeDouble(f.holder().staticTuple(), f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static Object resolveAndGetFieldReference(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            Object value = FieldReadSnippet.ReadReference.readReference(object, f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadReference.readReference(object, f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldReference(ResolutionGuard.InPool guard, Object object, Object value) {
        FieldActor f = resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteReference.writeReference(object, f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteReference.writeReference(object, f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static Object resolveAndGetStaticReference(ResolutionGuard.InPool guard) {
        FieldActor f = resolveStaticFieldForReading(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            Object value = FieldReadSnippet.ReadReference.readReference(f.holder().staticTuple(), f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadReference.readReference(f.holder().staticTuple(), f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticReference(ResolutionGuard.InPool guard, Object value) {
        FieldActor f = resolveStaticFieldForWriting(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteReference.writeReference(f.holder().staticTuple(), f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteReference.writeReference(f.holder().staticTuple(), f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static Word resolveAndGetFieldWord(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            Word value = FieldReadSnippet.ReadWord.readWord(object, f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadWord.readWord(object, f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldWord(ResolutionGuard.InPool guard, Object object, Word value) {
        FieldActor f = resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteWord.writeWord(object, f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteWord.writeWord(object, f, value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static Word resolveAndGetStaticWord(ResolutionGuard.InPool guard) {
        FieldActor f = resolveStaticFieldForReading(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            Word value = FieldReadSnippet.ReadWord.readWord(f.holder().staticTuple(), f);
            postVolatileRead();
            return value;
        } else {
            return FieldReadSnippet.ReadWord.readWord(f.holder().staticTuple(), f);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticWord(ResolutionGuard.InPool guard, Word value) {
        FieldActor f = resolveStaticFieldForWriting(guard);
        MakeHolderInitialized.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            FieldWriteSnippet.WriteWord.writeWord(f.holder().staticTuple(), f, value);
            postVolatileWrite();
        } else {
            FieldWriteSnippet.WriteWord.writeWord(f.holder().staticTuple(), f, value);
        }
    }


    // ==========================================================================================================
    // == Misc routines =========================================================================================
    // ==========================================================================================================

    public static void resolveAndCheckcast(ResolutionGuard guard, final Object object) {
        Snippet.CheckCast.checkCast(ResolveClass.resolveClass(guard), object);
    }

    public static void arrayStore(final int index, final Object array, final Object value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.checkSetObject(array, value);
        ArrayAccess.setObject(array, index, value);
    }

    public static Object getClassMirror(ClassActor classActor) {
        return classActor.javaClass();
    }

    public static Object createTupleOrHybrid(ClassActor classActor) {
        return CreateTupleOrHybrid.createTupleOrHybrid(classActor);
    }

    public static Object cloneArray(int[] arr) {
        return arr.clone();
    }

    static void checkArrayDimension(int length) {
        Snippet.CheckArrayDimension.checkArrayDimension(length);
    }

    public static Throwable loadException() {
        Safepoint.safepoint();
        Throwable exception = UnsafeCast.asThrowable(EXCEPTION_OBJECT.loadRef(currentTLA()).toJava());
        EXCEPTION_OBJECT.store3(Reference.zero());
        FatalError.check(exception != null, "Exception object lost during unwinding");
        return exception;
    }

    public static void rethrowException() {
        Safepoint.safepoint();
        Throwable exception = UnsafeCast.asThrowable(EXCEPTION_OBJECT.loadRef(currentTLA()).toJava());
        EXCEPTION_OBJECT.store3(Reference.zero());
        Throw.raise(exception);
    }

    static void monitorenter(Object rcvr) {
        Monitor.enter(rcvr);
    }

    static void monitorexit(Object rcvr) {
        Monitor.exit(rcvr);
    }

    public static int f2i(float value) {
        return (int) value;
    }

    public static long f2l(float value) {
        return (long) value;
    }

    public static int d2i(double value) {
        return (int) value;
    }

    public static long d2l(double value) {
        return (long) value;
    }
}
