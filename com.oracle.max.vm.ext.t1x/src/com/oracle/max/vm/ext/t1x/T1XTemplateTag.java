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
package com.oracle.max.vm.ext.t1x;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.type.*;

/**
 * The set of templates (potentially) used by T1X.
 *
 * The prefix of each enum constant name up to the first '$'
 * character (if any) corresponds to the name of a {@link Bytecodes} enum constant.
 * This convention is used to determine the {@linkplain #opcode} implemented by a given template.
 *
 * The remainder of an {@code T1XTemplateTag} enum constant's name specifies various
 * properties of the template such whether it includes a class {@linkplain #resolved resolution}
 * or {@linkplain #initialized initialization} check.
 *
 * There is an almost 1-1 correspondence of the prefix with the bytecode definitions in {@link Bytecodes},
 * except for the bytecodes, such as {@code ICONST_N}, that have multiple variants for differing
 * values of {@code N}, for which there is just one template.
 *
 * Note that a given implementation of T1X may not use (and therefore implement) a template for
 * every tag defined here.
 *

 */
public enum T1XTemplateTag {
    NOP,
    ACONST_NULL,
    ICONST(Bytecodes.ICONST_0),
    LCONST(Bytecodes.LCONST_0),
    FCONST(Bytecodes.FCONST_0),
    DCONST(Bytecodes.DCONST_0),
    BIPUSH,
    SIPUSH,
    LDC$int,
    LDC$long,
    LDC$float,
    LDC$double,
    LDC$reference,
    LDC$reference$resolved,
    ILOAD,
    LLOAD,
    FLOAD,
    DLOAD,
    ALOAD,
    IALOAD,
    LALOAD,
    FALOAD,
    DALOAD,
    AALOAD,
    BALOAD,
    CALOAD,
    SALOAD,
    ISTORE,
    LSTORE,
    FSTORE,
    DSTORE,
    ASTORE,
    IASTORE,
    LASTORE,
    FASTORE,
    DASTORE,
    AASTORE,
    BASTORE,
    CASTORE,
    SASTORE,
    POP,
    POP2,
    DUP,
    DUP_X1,
    DUP_X2,
    DUP2,
    DUP2_X1,
    DUP2_X2,
    SWAP,
    IADD,
    LADD,
    FADD,
    DADD,
    ISUB,
    LSUB,
    FSUB,
    DSUB,
    IMUL,
    LMUL,
    FMUL,
    DMUL,
    IDIV,
    LDIV,
    FDIV,
    DDIV,
    IREM,
    LREM,
    FREM,
    DREM,
    INEG,
    LNEG,
    FNEG,
    DNEG,
    ISHL,
    LSHL,
    ISHR,
    LSHR,
    IUSHR,
    LUSHR,
    IAND,
    LAND,
    IOR,
    LOR,
    IXOR,
    LXOR,
    IINC,
    I2L,
    I2F,
    I2D,
    L2I,
    L2F,
    L2D,
    F2I,
    F2L,
    F2D,
    D2I,
    D2L,
    D2F,
    I2B,
    I2C,
    I2S,
    LCMP,
    FCMPL,
    FCMPG,
    DCMPL,
    DCMPG,
    IFEQ,
    IFNE,
    IFLT,
    IFGE,
    IFGT,
    IFLE,
    IF_ICMPEQ,
    IF_ICMPNE,
    IF_ICMPLT,
    IF_ICMPGE,
    IF_ICMPGT,
    IF_ICMPLE,
    IF_ACMPEQ,
    IF_ACMPNE,
    GOTO,
    GOTO_W,
    IRETURN,
    LRETURN,
    FRETURN,
    DRETURN,
    ARETURN,
    RETURN,

    IRETURN$unlock,
    LRETURN$unlock,
    FRETURN$unlock,
    DRETURN$unlock,
    ARETURN$unlock,
    RETURN$unlock,

    RETURN$registerFinalizer,

    GETSTATIC$byte,
    GETSTATIC$boolean,
    GETSTATIC$char,
    GETSTATIC$short,
    GETSTATIC$int,
    GETSTATIC$float,
    GETSTATIC$long,
    GETSTATIC$double,
    GETSTATIC$reference,
    GETSTATIC$word,
    GETSTATIC$byte$init,
    GETSTATIC$boolean$init,
    GETSTATIC$char$init,
    GETSTATIC$short$init,
    GETSTATIC$int$init,
    GETSTATIC$float$init,
    GETSTATIC$long$init,
    GETSTATIC$double$init,
    GETSTATIC$reference$init,
    GETSTATIC$word$init,

    PUTSTATIC$byte,
    PUTSTATIC$boolean,
    PUTSTATIC$char,
    PUTSTATIC$short,
    PUTSTATIC$int,
    PUTSTATIC$float,
    PUTSTATIC$long,
    PUTSTATIC$double,
    PUTSTATIC$reference,
    PUTSTATIC$word,
    PUTSTATIC$byte$init,
    PUTSTATIC$boolean$init,
    PUTSTATIC$char$init,
    PUTSTATIC$short$init,
    PUTSTATIC$int$init,
    PUTSTATIC$float$init,
    PUTSTATIC$long$init,
    PUTSTATIC$double$init,
    PUTSTATIC$reference$init,
    PUTSTATIC$word$init,

    GETFIELD$byte,
    GETFIELD$boolean,
    GETFIELD$char,
    GETFIELD$short,
    GETFIELD$int,
    GETFIELD$float,
    GETFIELD$long,
    GETFIELD$double,
    GETFIELD$reference,
    GETFIELD$word,
    GETFIELD$byte$resolved,
    GETFIELD$boolean$resolved,
    GETFIELD$char$resolved,
    GETFIELD$short$resolved,
    GETFIELD$int$resolved,
    GETFIELD$float$resolved,
    GETFIELD$long$resolved,
    GETFIELD$double$resolved,
    GETFIELD$reference$resolved,
    GETFIELD$word$resolved,

    PUTFIELD$byte,
    PUTFIELD$boolean,
    PUTFIELD$char,
    PUTFIELD$short,
    PUTFIELD$int,
    PUTFIELD$float,
    PUTFIELD$long,
    PUTFIELD$double,
    PUTFIELD$reference,
    PUTFIELD$word,

    PUTFIELD$byte$resolved,
    PUTFIELD$boolean$resolved,
    PUTFIELD$char$resolved,
    PUTFIELD$short$resolved,
    PUTFIELD$int$resolved,
    PUTFIELD$float$resolved,
    PUTFIELD$long$resolved,
    PUTFIELD$double$resolved,
    PUTFIELD$reference$resolved,
    PUTFIELD$word$resolved,

    INVOKEVIRTUAL$void,
    INVOKEVIRTUAL$float,
    INVOKEVIRTUAL$long,
    INVOKEVIRTUAL$double,
    INVOKEVIRTUAL$word,
    INVOKEVIRTUAL$reference,
    INVOKEVIRTUAL$void$resolved,
    INVOKEVIRTUAL$float$resolved,
    INVOKEVIRTUAL$long$resolved,
    INVOKEVIRTUAL$double$resolved,
    INVOKEVIRTUAL$reference$resolved,
    INVOKEVIRTUAL$word$resolved,
    INVOKEVIRTUAL$void$instrumented,
    INVOKEVIRTUAL$float$instrumented,
    INVOKEVIRTUAL$long$instrumented,
    INVOKEVIRTUAL$double$instrumented,
    INVOKEVIRTUAL$reference$instrumented,
    INVOKEVIRTUAL$word$instrumented,

    INVOKESPECIAL$void,
    INVOKESPECIAL$float,
    INVOKESPECIAL$long,
    INVOKESPECIAL$double,
    INVOKESPECIAL$reference,
    INVOKESPECIAL$word,
    INVOKESPECIAL$void$resolved,
    INVOKESPECIAL$float$resolved,
    INVOKESPECIAL$long$resolved,
    INVOKESPECIAL$double$resolved,
    INVOKESPECIAL$reference$resolved,
    INVOKESPECIAL$word$resolved,

    INVOKESTATIC$void,
    INVOKESTATIC$float,
    INVOKESTATIC$long,
    INVOKESTATIC$double,
    INVOKESTATIC$reference,
    INVOKESTATIC$word,
    INVOKESTATIC$void$init,
    INVOKESTATIC$float$init,
    INVOKESTATIC$long$init,
    INVOKESTATIC$double$init,
    INVOKESTATIC$reference$init,
    INVOKESTATIC$word$init,

    INVOKEINTERFACE$void,
    INVOKEINTERFACE$float,
    INVOKEINTERFACE$long,
    INVOKEINTERFACE$double,
    INVOKEINTERFACE$reference,
    INVOKEINTERFACE$word,
    INVOKEINTERFACE$void$resolved,
    INVOKEINTERFACE$float$resolved,
    INVOKEINTERFACE$long$resolved,
    INVOKEINTERFACE$double$resolved,
    INVOKEINTERFACE$reference$resolved,
    INVOKEINTERFACE$word$resolved,
    INVOKEINTERFACE$void$instrumented,
    INVOKEINTERFACE$float$instrumented,
    INVOKEINTERFACE$long$instrumented,
    INVOKEINTERFACE$double$instrumented,
    INVOKEINTERFACE$reference$instrumented,
    INVOKEINTERFACE$word$instrumented,

    NEW,
    NEW$init,
    NEW_HYBRID(-1),
    NEWARRAY,
    ANEWARRAY,
    ANEWARRAY$resolved,
    ARRAYLENGTH,
    ATHROW,
    CHECKCAST,
    CHECKCAST$resolved,
    INSTANCEOF,
    INSTANCEOF$resolved,
    MONITORENTER,
    MONITOREXIT,
    MULTIANEWARRAY,
    MULTIANEWARRAY$resolved,
    IFNULL,
    IFNONNULL,

    LOAD_EXCEPTION(-1),
    RETHROW_EXCEPTION(-1),
    PROFILE_STATIC_METHOD_ENTRY(-1),
    PROFILE_NONSTATIC_METHOD_ENTRY(-1),
    PROFILE_BACKWARD_BRANCH(-1),
    TRACE_METHOD_ENTRY(-1),
    CREATE_MULTIANEWARRAY_DIMENSIONS(-1),
    LOCK(-1),
    UNLOCK(-1),

    // START VMA additions
    INVOKEVIRTUAL$adviseafter(-1),
    INVOKEINTERFACE$adviseafter(-1),
    INVOKESPECIAL$adviseafter(-1),
    INVOKESTATIC$adviseafter(-1);
    // END VMA additions

    public static final EnumMap<KindEnum, T1XTemplateTag> PUTSTATICS = makeKindMap(Bytecodes.PUTSTATIC);
    public static final EnumMap<KindEnum, T1XTemplateTag> GETSTATICS = makeKindMap(Bytecodes.GETSTATIC);
    public static final EnumMap<KindEnum, T1XTemplateTag> PUTFIELDS = makeKindMap(Bytecodes.PUTFIELD);
    public static final EnumMap<KindEnum, T1XTemplateTag> GETFIELDS = makeKindMap(Bytecodes.GETFIELD);

    public static final EnumMap<KindEnum, T1XTemplateTag> INVOKEVIRTUALS = makeKindMap(Bytecodes.INVOKEVIRTUAL);
    public static final EnumMap<KindEnum, T1XTemplateTag> INVOKEINTERFACES = makeKindMap(Bytecodes.INVOKEINTERFACE);
    public static final EnumMap<KindEnum, T1XTemplateTag> INVOKESPECIALS = makeKindMap(Bytecodes.INVOKESPECIAL);
    public static final EnumMap<KindEnum, T1XTemplateTag> INVOKESTATICS = makeKindMap(Bytecodes.INVOKESTATIC);

    /**
     * Creates a map from kinds to the template specialized for each kind a given bytecode is parameterized by.
     *
     * @param bytecode a bytecode instruction that is specialized for a number of kinds
     */
    @HOSTED_ONLY
    private static EnumMap<KindEnum, T1XTemplateTag> makeKindMap(int bytecode) {
        EnumMap<KindEnum, T1XTemplateTag> map = new EnumMap<KindEnum, T1XTemplateTag>(KindEnum.class);
        for (T1XTemplateTag bt : values()) {
            String name = Bytecodes.nameOf(bytecode).toUpperCase();
            if (bt.name().startsWith(name)) {
                for (KindEnum kind : KindEnum.VALUES) {
                    String k = kind.name().toLowerCase();
                    if (bt.name().equals(name + "$" + k)) {
                        map.put(kind, bt);
                    }
                }
            }
        }
        return map;
    }

    /**
     * The opcode of the bytecode instruction implemented by this template. This is a representative opcode
     * if a template is used to implement more than one bytecode instruction.
     */
    public final int opcode;

    /**
     * Denotes the template that omits the class initialization check required by the {@linkplain #opcode} instruction.
     * This field is only non-null for the template that includes the initialization check (i.e. the uninitialized case).
     */
    public T1XTemplateTag initialized;

    /**
     * Denotes the template that omits the class resolution check required by the {@linkplain #opcode} instruction.
     * This field is only non-null for the template that includes the resolution check (i.e. the unresolved case).
     */
    public T1XTemplateTag resolved;

    /**
     * Denotes the instrumented version of this template.
     */
    public T1XTemplateTag instrumented;

    @HOSTED_ONLY
    private T1XTemplateTag(int representativeOpcode) {
        this.opcode = representativeOpcode;
    }

    @HOSTED_ONLY
    private T1XTemplateTag() {
        String name = name();
        int dollar = name.indexOf('$');
        String opcodeName;
        if (dollar == -1) {
            opcodeName = name;
        } else {
            opcodeName = name.substring(0, dollar);
        }
        opcode = Bytecodes.valueOf(opcodeName);
    }

    static {
        initProps();
    }

    /**
     * Initializes the properties of the {@code BytecodeTemplate} enum constants that cannot
     * be initialized in the constructor.
     */
    @HOSTED_ONLY
    private static void initProps() {
        for (T1XTemplateTag bt : values()) {
            String name = bt.name();
            if (name.contains("$init")) {
                String uninitName = name.replace("$init", "");
                T1XTemplateTag uninit = valueOf(uninitName);
                assert uninit != null;
                uninit.initialized = bt;
            } else if (name.contains("$resolved")) {
                String unresolvedName = name.replace("$resolved", "");
                T1XTemplateTag unresolved = valueOf(unresolvedName);
                assert unresolved != null;
                unresolved.resolved = bt;
            } else if (name.contains("$instrumented")) {
                String uninstrumentedName = name.substring(0, name.indexOf("$instrumented"));
                T1XTemplateTag uninstrumented = valueOf(uninstrumentedName);
                assert uninstrumented != null;
                uninstrumented.instrumented = bt;
            }
        }
    }

    @HOSTED_ONLY
    public static void main(String[] args) {
        for (T1XTemplateTag bt : values()) {
            System.out.print(bt.ordinal() + ": " + bt);
            if (bt.initialized != null) {
                System.out.print(" -- uninitialized");
            } else if (bt.resolved != null) {
                System.out.print(" -- unresolved");
            }
            System.out.println();
        }
    }
}
