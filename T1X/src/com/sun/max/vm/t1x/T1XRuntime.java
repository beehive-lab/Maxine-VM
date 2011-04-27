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

import java.io.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

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
        final VirtualMethodActor virtualMethodActor = Snippets.resolveVirtualMethod(guard);
        return Snippets.selectNonPrivateVirtualMethod(receiver, virtualMethodActor).asAddress();
    }

    public static Address resolveAndSelectInterfaceMethod(ResolutionGuard.InPool guard, final Object receiver) {
        final InterfaceMethodActor declaredInterfaceMethod = Snippets.resolveInterfaceMethod(guard);
        final Address entryPoint = Snippets.selectInterfaceMethod(receiver, declaredInterfaceMethod).asAddress();
        return entryPoint;
    }

    public static Address resolveSpecialMethod(ResolutionGuard.InPool guard) {
        final VirtualMethodActor virtualMethod = Snippets.resolveSpecialMethod(guard);
        return Snippets.makeEntrypoint(virtualMethod);
    }

    public static Address resolveStaticMethod(ResolutionGuard.InPool guard) {
        final StaticMethodActor staticMethod = Snippets.resolveStaticMethod(guard);
        Snippets.makeHolderInitialized(staticMethod);
        return Snippets.makeEntrypoint(staticMethod);
    }

    public static Object resolveClassForNewAndCreate(ResolutionGuard guard) {
        final ClassActor classActor = Snippets.resolveClassForNew(guard);
        Snippets.makeClassInitialized(classActor);
        final Object tuple = Snippets.createTupleOrHybrid(classActor);
        return tuple;
    }

    public static Object resolveMirror(ResolutionGuard guard) {
        return Snippets.resolveClass(guard).javaClass();
    }

    // ==========================================================================================================
    // == Field access           ================================================================================
    // ==========================================================================================================

    /**
     * Inserts any necessary memory barriers before a volatile read as required by the JMM.
     */
    @INLINE
    public static void preVolatileRead() {
    }

    /**
     * Inserts any necessary memory barriers after a volatile read as required by the JMM.
     */
    @INTRINSIC(MEMBAR | ((LOAD_LOAD | LOAD_STORE) << 8))
    public static native void postVolatileRead();

    /**
     * Inserts any necessary memory barriers before a volatile read as required by the JMM.
     */
    @INTRINSIC(MEMBAR | ((LOAD_STORE | STORE_STORE) << 8))
    public static native void preVolatileWrite();

    /**
     * Inserts any necessary memory barriers after a volatile read as required by the JMM.
     */
    @INTRINSIC(MEMBAR | ((STORE_LOAD | STORE_STORE) << 8))
    public static native void postVolatileWrite();

    /**
     * Manual macros, Java style, for field access routines.
     *             Object value = TupleAccess.readObject(object, f.offset());

     */

    /**
     * Allows customization of the generated methods to support bytecode advising.
     */
    @HOSTED_ONLY
    public interface Hook {
        /**
         * Should return a string that will be inserted after resolving the field.
         * @param indent current indentation
         * @param method GetField/PutField/GetStatic/PutStatic
         * @param t kind string with Reference replaced by Object
         * @param uK kind string with first char uppercase
         * @param uT {@code t} with first char uppercase
         * @return string to include
         */
        String getHook(String indent, String method, String t, String uK, String uT);
        /**
         * Customizes the methodname to resolveXXXname, where this method returns the XXX.
         * @return
         */
        String getMethodNameModifier();
    }

    @HOSTED_ONLY
    public static void generate(Hook hook) {
        PrintStream o = System.out;
        String[] kinds = {"boolean", "byte", "char", "short", "int", "float", "long", "double", "Reference", "Word"};
        final String mm = hook == null ? "" : hook.getMethodNameModifier();
        for (String k : kinds) {
            String t = k.equals("Reference") ? "Object" : k;
            String uK = k.substring(0, 1).toUpperCase() + k.substring(1);
            String uT = t.substring(0, 1).toUpperCase() + t.substring(1);
            o.printf("    // GENERATED -- EDIT AND RUN main() TO MODIFY%n");
            o.printf("    public static %s resolve%sAndGetField%s(ResolutionGuard.InPool guard, Object object) {%n", t, mm, uK);
            o.printf("        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);%n");
            if (hook != null) {
                o.printf(hook.getHook("        ", "GetField", t, uK, uT));
            }
            o.printf("        if (f.isVolatile()) {%n");
            o.printf("            preVolatileRead();%n");
            o.printf("            %s value = TupleAccess.read%s(object, f.offset());%n", t, uT);
            o.printf("            postVolatileRead();%n");
            o.printf("            return value;%n");
            o.printf("        } else {%n");
            o.printf("            return TupleAccess.read%s(object, f.offset());%n", uT);
            o.printf("        }%n");
            o.printf("    }%n");
            o.printf("%n");
            o.printf("    // GENERATED -- EDIT AND RUN main() TO MODIFY%n");
            o.printf("    public static void resolve%sAndPutField%s(ResolutionGuard.InPool guard, Object object, %s value) {%n", mm, uK, t);
            o.printf("        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);%n");
            if (hook != null) {
                o.printf(hook.getHook("        ", "PutField", t, uK, uT));
            }
            o.printf("        if (f.isVolatile()) {%n");
            o.printf("            preVolatileWrite();%n");
            o.printf("            TupleAccess.write%s(object, f.offset(), value);%n", uT);
            o.printf("            postVolatileWrite();%n");
            o.printf("        } else {%n");
            o.printf("            TupleAccess.write%s(object, f.offset(), value);%n", uT);
            o.printf("        }%n");
            o.printf("    }%n");
            o.printf("%n");
            o.printf("    // GENERATED -- EDIT AND RUN main() TO MODIFY%n");
            o.printf("    public static %s resolve%sAndGetStatic%s(ResolutionGuard.InPool guard) {%n", t, mm, uK);
            o.printf("        FieldActor f = Snippets.resolveStaticFieldForReading(guard);%n");
            o.printf("        Snippets.makeHolderInitialized(f);%n");
            if (hook != null) {
                o.printf(hook.getHook("        ", "GetStatic", t, uK, uT));
            }
            o.printf("        if (f.isVolatile()) {%n");
            o.printf("            preVolatileRead();%n");
            o.printf("            %s value = TupleAccess.read%s(f.holder().staticTuple(), f.offset());%n", t, uT);
            o.printf("            postVolatileRead();%n");
            o.printf("            return value;%n");
            o.printf("        } else {%n");
            o.printf("            return TupleAccess.read%s(f.holder().staticTuple(), f.offset());%n", uT);
            o.printf("        }%n");
            o.printf("    }%n");
            o.printf("%n");
            o.printf("    // GENERATED -- EDIT AND RUN main() TO MODIFY%n");
            o.printf("    public static void resolve%sAndPutStatic%s(ResolutionGuard.InPool guard, %s value) {%n", mm, uK, t);
            o.printf("        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);%n");
            if (hook != null) {
                o.printf(hook.getHook("        ", "PutStatic", t, uK, uT));
            }
            o.printf("        Snippets.makeHolderInitialized(f);%n");
            o.printf("        if (f.isVolatile()) {%n");
            o.printf("            preVolatileWrite();%n");
            o.printf("            TupleAccess.write%s(f.holder().staticTuple(), f.offset(), value);%n", uT);
            o.printf("            postVolatileWrite();%n");
            o.printf("        } else {%n");
            o.printf("            TupleAccess.write%s(f.holder().staticTuple(), f.offset(), value);%n", uT);
            o.printf("        }%n");
            o.printf("    }%n");
            o.printf("%n");
        }
    }

    @HOSTED_ONLY
    public static void main(String[] args) {
        generate(null);
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static boolean resolveAndGetFieldBoolean(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            boolean value = TupleAccess.readBoolean(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readBoolean(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldBoolean(ResolutionGuard.InPool guard, Object object, boolean value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeBoolean(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeBoolean(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static boolean resolveAndGetStaticBoolean(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            boolean value = TupleAccess.readBoolean(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readBoolean(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticBoolean(ResolutionGuard.InPool guard, boolean value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeBoolean(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeBoolean(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static byte resolveAndGetFieldByte(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            byte value = TupleAccess.readByte(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readByte(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldByte(ResolutionGuard.InPool guard, Object object, byte value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeByte(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeByte(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static byte resolveAndGetStaticByte(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            byte value = TupleAccess.readByte(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readByte(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticByte(ResolutionGuard.InPool guard, byte value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeByte(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeByte(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static char resolveAndGetFieldChar(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            char value = TupleAccess.readChar(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readChar(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldChar(ResolutionGuard.InPool guard, Object object, char value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeChar(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeChar(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static char resolveAndGetStaticChar(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            char value = TupleAccess.readChar(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readChar(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticChar(ResolutionGuard.InPool guard, char value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeChar(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeChar(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static short resolveAndGetFieldShort(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            short value = TupleAccess.readShort(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readShort(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldShort(ResolutionGuard.InPool guard, Object object, short value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeShort(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeShort(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static short resolveAndGetStaticShort(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            short value = TupleAccess.readShort(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readShort(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticShort(ResolutionGuard.InPool guard, short value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeShort(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeShort(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static int resolveAndGetFieldInt(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            int value = TupleAccess.readInt(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readInt(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldInt(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeInt(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeInt(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static int resolveAndGetStaticInt(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            int value = TupleAccess.readInt(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readInt(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticInt(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeInt(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeInt(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static float resolveAndGetFieldFloat(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            float value = TupleAccess.readFloat(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readFloat(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldFloat(ResolutionGuard.InPool guard, Object object, float value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeFloat(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeFloat(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static float resolveAndGetStaticFloat(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            float value = TupleAccess.readFloat(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readFloat(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticFloat(ResolutionGuard.InPool guard, float value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeFloat(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeFloat(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static long resolveAndGetFieldLong(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            long value = TupleAccess.readLong(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readLong(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldLong(ResolutionGuard.InPool guard, Object object, long value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeLong(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeLong(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static long resolveAndGetStaticLong(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            long value = TupleAccess.readLong(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readLong(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticLong(ResolutionGuard.InPool guard, long value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeLong(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeLong(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static double resolveAndGetFieldDouble(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            double value = TupleAccess.readDouble(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readDouble(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldDouble(ResolutionGuard.InPool guard, Object object, double value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeDouble(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeDouble(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static double resolveAndGetStaticDouble(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            double value = TupleAccess.readDouble(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readDouble(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticDouble(ResolutionGuard.InPool guard, double value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeDouble(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeDouble(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static Object resolveAndGetFieldReference(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            Object value = TupleAccess.readObject(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readObject(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldReference(ResolutionGuard.InPool guard, Object object, Object value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeObject(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeObject(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static Object resolveAndGetStaticReference(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            Object value = TupleAccess.readObject(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readObject(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticReference(ResolutionGuard.InPool guard, Object value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeObject(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeObject(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static Word resolveAndGetFieldWord(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (f.isVolatile()) {
            preVolatileRead();
            Word value = TupleAccess.readWord(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readWord(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutFieldWord(ResolutionGuard.InPool guard, Object object, Word value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeWord(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeWord(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static Word resolveAndGetStaticWord(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileRead();
            Word value = TupleAccess.readWord(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readWord(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN main() TO MODIFY
    public static void resolveAndPutStaticWord(ResolutionGuard.InPool guard, Word value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeWord(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeWord(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // ==========================================================================================================
    // == Misc routines =========================================================================================
    // ==========================================================================================================

    public static void resolveAndCheckcast(ResolutionGuard guard, final Object object) {
        Snippets.checkCast(Snippets.resolveClass(guard), object);
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
        return Snippets.createTupleOrHybrid(classActor);
    }

    public static Object createPrimitiveArray(Kind kind, int length) {
        return Snippets.createArray(kind.arrayClassActor(), length);
    }

    public static Object createReferenceArray(ArrayClassActor arrayClassActor, int length) {
        return Snippets.createArray(arrayClassActor, length);
    }

    public static Object cloneArray(int[] arr) {
        return arr.clone();
    }

    static void checkArrayDimension(int length) {
        Snippets.checkArrayDimension(length);
    }

    public static Throwable loadException() {
        return VmThread.current().loadExceptionForHandler();
    }

    public static void rethrowException() {
        Throw.raise(VmThread.current().loadExceptionForHandler());
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
