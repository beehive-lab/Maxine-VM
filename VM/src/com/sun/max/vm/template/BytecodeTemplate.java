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
package com.sun.max.vm.template;

import java.util.*;

import com.sun.c1x.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.type.*;

/**
 * The set of bytecode templates available to a template-based JIT compiler.
 *
 * The prefix of each {@code BytecodeTemplate} enum constant name up to the first '$'
 * character (if any) corresponds to the name of a {@link Bytecodes} enum constant.
 * This convention is used to determine the {@linkplain #bytecode} implemented by a given
 * bytecode template.
 *
 * The remainder of an {@code BytecodeTemplate} enum constant's name specifies various
 * properties of the bytecode template such whether it includes a class {@linkplain #resolved resolution}
 * or {@linkplain #initialized initialization} check.
 *
 * @author Doug Simon
 */
public enum BytecodeTemplate {
    NOP,
    NOP$instrumented$MethodEntry,
    ACONST_NULL,
    ICONST_M1,
    ICONST_0,
    ICONST_1,
    ICONST_2,
    ICONST_3,
    ICONST_4,
    ICONST_5,
    LCONST_0,
    LCONST_1,
    FCONST_0,
    FCONST_1,
    FCONST_2,
    DCONST_0,
    DCONST_1,
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
    ILOAD_0,
    ILOAD_1,
    ILOAD_2,
    ILOAD_3,
    LLOAD_0,
    LLOAD_1,
    LLOAD_2,
    LLOAD_3,
    FLOAD_0,
    FLOAD_1,
    FLOAD_2,
    FLOAD_3,
    DLOAD_0,
    DLOAD_1,
    DLOAD_2,
    DLOAD_3,
    ALOAD_0,
    ALOAD_1,
    ALOAD_2,
    ALOAD_3,
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
    ISTORE_0,
    ISTORE_1,
    ISTORE_2,
    ISTORE_3,
    LSTORE_0,
    LSTORE_1,
    LSTORE_2,
    LSTORE_3,
    FSTORE_0,
    FSTORE_1,
    FSTORE_2,
    FSTORE_3,
    DSTORE_0,
    DSTORE_1,
    DSTORE_2,
    DSTORE_3,
    ASTORE_0,
    ASTORE_1,
    ASTORE_2,
    ASTORE_3,
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
    IRETURN,
    LRETURN,
    FRETURN,
    DRETURN,
    ARETURN,
    RETURN,

    GETSTATIC$byte,
    GETSTATIC$boolean,
    GETSTATIC$char,
    GETSTATIC$short,
    GETSTATIC$int,
    GETSTATIC$float,
    GETSTATIC$long,
    GETSTATIC$double,
    GETSTATIC$reference,
    GETSTATIC$byte$init,
    GETSTATIC$boolean$init,
    GETSTATIC$char$init,
    GETSTATIC$short$init,
    GETSTATIC$int$init,
    GETSTATIC$float$init,
    GETSTATIC$long$init,
    GETSTATIC$double$init,
    GETSTATIC$reference$init,

    PUTSTATIC$byte,
    PUTSTATIC$boolean,
    PUTSTATIC$char,
    PUTSTATIC$short,
    PUTSTATIC$int,
    PUTSTATIC$float,
    PUTSTATIC$long,
    PUTSTATIC$double,
    PUTSTATIC$reference,
    PUTSTATIC$byte$init,
    PUTSTATIC$boolean$init,
    PUTSTATIC$char$init,
    PUTSTATIC$short$init,
    PUTSTATIC$int$init,
    PUTSTATIC$float$init,
    PUTSTATIC$long$init,
    PUTSTATIC$double$init,
    PUTSTATIC$reference$init,

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
    INVOKEVIRTUAL$void$resolved,
    INVOKEVIRTUAL$float$resolved,
    INVOKEVIRTUAL$long$resolved,
    INVOKEVIRTUAL$double$resolved,
    INVOKEVIRTUAL$word$resolved,
    INVOKEVIRTUAL$void$instrumented,
    INVOKEVIRTUAL$float$instrumented,
    INVOKEVIRTUAL$long$instrumented,
    INVOKEVIRTUAL$double$instrumented,
    INVOKEVIRTUAL$word$instrumented,

    INVOKESPECIAL$void,
    INVOKESPECIAL$float,
    INVOKESPECIAL$long,
    INVOKESPECIAL$double,
    INVOKESPECIAL$word,
    INVOKESPECIAL$void$resolved,
    INVOKESPECIAL$float$resolved,
    INVOKESPECIAL$long$resolved,
    INVOKESPECIAL$double$resolved,
    INVOKESPECIAL$word$resolved,

    INVOKESTATIC$void,
    INVOKESTATIC$float,
    INVOKESTATIC$long,
    INVOKESTATIC$double,
    INVOKESTATIC$word,
    INVOKESTATIC$void$init,
    INVOKESTATIC$float$init,
    INVOKESTATIC$long$init,
    INVOKESTATIC$double$init,
    INVOKESTATIC$word$init,

    INVOKEINTERFACE$void,
    INVOKEINTERFACE$float,
    INVOKEINTERFACE$long,
    INVOKEINTERFACE$double,
    INVOKEINTERFACE$word,
    INVOKEINTERFACE$void$resolved,
    INVOKEINTERFACE$float$resolved,
    INVOKEINTERFACE$long$resolved,
    INVOKEINTERFACE$double$resolved,
    INVOKEINTERFACE$word$resolved,
    INVOKEINTERFACE$void$instrumented,
    INVOKEINTERFACE$float$instrumented,
    INVOKEINTERFACE$long$instrumented,
    INVOKEINTERFACE$double$instrumented,
    INVOKEINTERFACE$word$instrumented,

    NEW,
    NEW$init,
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
    IFNONNULL;

    public static final EnumMap<KindEnum, BytecodeTemplate> PUTSTATICS = makeKindMap(Bytecodes.PUTSTATIC);
    public static final EnumMap<KindEnum, BytecodeTemplate> GETSTATICS = makeKindMap(Bytecodes.GETSTATIC);
    public static final EnumMap<KindEnum, BytecodeTemplate> PUTFIELDS = makeKindMap(Bytecodes.PUTFIELD);
    public static final EnumMap<KindEnum, BytecodeTemplate> GETFIELDS = makeKindMap(Bytecodes.GETFIELD);

    public static final EnumMap<KindEnum, BytecodeTemplate> INVOKEVIRTUALS = makeKindMap(Bytecodes.INVOKEVIRTUAL);
    public static final EnumMap<KindEnum, BytecodeTemplate> INVOKEINTERFACES = makeKindMap(Bytecodes.INVOKEINTERFACE);
    public static final EnumMap<KindEnum, BytecodeTemplate> INVOKESPECIALS = makeKindMap(Bytecodes.INVOKESPECIAL);
    public static final EnumMap<KindEnum, BytecodeTemplate> INVOKESTATICS = makeKindMap(Bytecodes.INVOKESTATIC);

    /**
     * Creates a map from kinds to the template specialized for each kind a given bytecode is parameterized by.
     *
     * @param bytecode a bytecode instruction that is specialized for a number of kinds
     */
    @HOSTED_ONLY
    private static EnumMap<KindEnum, BytecodeTemplate> makeKindMap(int bytecode) {
        EnumMap<KindEnum, BytecodeTemplate> map = new EnumMap<KindEnum, BytecodeTemplate>(KindEnum.class);
        for (BytecodeTemplate bt : values()) {
            String name = Bytecodes.nameOf(bytecode).toUpperCase();
            if (bt.name().startsWith(name)) {
                for (KindEnum kind : KindEnum.VALUES) {
                    if (bt.name().equals(name + "$" + kind.name().toLowerCase())) {
                        map.put(kind, bt);
                    }
                }
            }
        }
        return map;
    }

    /**
     * The bytecode implemented by this template.
     */
    public final int bytecode;

    /**
     * Denotes the template that omits the class initialization check required by the {@linkplain #bytecode} instruction.
     * This field is only non-null for the template that includes the initialization check (i.e. the uninitialized case).
     */
    public BytecodeTemplate initialized;

    /**
     * Denotes the template that omits the class resolution check required by the {@linkplain #bytecode} instruction.
     * This field is only non-null for the template that includes the resolution check (i.e. the unresolved case).
     */
    public BytecodeTemplate resolved;

    /**
     * Denotes the instrumented version of this template.
     */
    public BytecodeTemplate instrumented;

    @HOSTED_ONLY
    private BytecodeTemplate() {
        String name = name();
        int dollar = name.indexOf('$');
        if (dollar == -1) {
            bytecode = Bytecodes.valueOf(name);
        } else {
            bytecode = Bytecodes.valueOf(name.substring(0, dollar));
        }
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
        for (BytecodeTemplate bt : values()) {
            String name = bt.name();
            if (name.contains("$init")) {
                String uninitName = name.replace("$init", "");
                BytecodeTemplate uninit = valueOf(uninitName);
                assert uninit != null;
                uninit.initialized = bt;
            } else if (name.contains("$resolved")) {
                String unresolvedName = name.replace("$resolved", "");
                BytecodeTemplate unresolved = valueOf(unresolvedName);
                assert unresolved != null;
                unresolved.resolved = bt;
            } else if (name.contains("$instrumented")) {
                String uninstrumentedName = name.substring(0, name.indexOf("$instrumented"));
                BytecodeTemplate uninstrumented = valueOf(uninstrumentedName);
                assert uninstrumented != null;
                uninstrumented.instrumented = bt;
            }
        }
    }

    @HOSTED_ONLY
    public static void main(String[] args) {
        for (BytecodeTemplate bt : values()) {
            System.out.print(bt);
            if (bt.initialized != null) {
                System.out.print(" -- uninitialized");
            } else if (bt.resolved != null) {
                System.out.print(" -- unresolved");
            }
            System.out.println();
        }
    }
}
