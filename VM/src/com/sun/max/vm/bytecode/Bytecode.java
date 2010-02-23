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
package com.sun.max.vm.bytecode;

import static com.sun.max.vm.bytecode.Bytecode.Flags.*;
import static com.sun.max.vm.classfile.ErrorContext.*;

import java.io.*;
import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.type.*;

/**
 * The opcodes defined in the JVM specification plus some extra MaxineVM specific opcodes.
 *
 * @author Doug Simon
 */
public enum Bytecode {

    // Standard opcodes

    NOP,
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
    LDC(LDC_),
    LDC_W(LDC_),
    LDC2_W(LDC_),
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
    IFEQ(CONDITIONAL_BRANCH),
    IFNE(CONDITIONAL_BRANCH),
    IFLT(CONDITIONAL_BRANCH),
    IFGE(CONDITIONAL_BRANCH),
    IFGT(CONDITIONAL_BRANCH),
    IFLE(CONDITIONAL_BRANCH),
    IF_ICMPEQ(CONDITIONAL_BRANCH),
    IF_ICMPNE(CONDITIONAL_BRANCH),
    IF_ICMPLT(CONDITIONAL_BRANCH),
    IF_ICMPGE(CONDITIONAL_BRANCH),
    IF_ICMPGT(CONDITIONAL_BRANCH),
    IF_ICMPLE(CONDITIONAL_BRANCH),
    IF_ACMPEQ(CONDITIONAL_BRANCH),
    IF_ACMPNE(CONDITIONAL_BRANCH),
    GOTO(FALL_THROUGH_DELIMITER | UNCONDITIONAL_BRANCH),
    JSR(FALL_THROUGH_DELIMITER | UNCONDITIONAL_BRANCH | JSR_OR_RET),
    RET(FALL_THROUGH_DELIMITER | UNCONDITIONAL_BRANCH | JSR_OR_RET),
    TABLESWITCH(FALL_THROUGH_DELIMITER | SWITCH),
    LOOKUPSWITCH(FALL_THROUGH_DELIMITER | SWITCH),
    IRETURN(FALL_THROUGH_DELIMITER | RETURN_),
    LRETURN(FALL_THROUGH_DELIMITER | RETURN_),
    FRETURN(FALL_THROUGH_DELIMITER | RETURN_),
    DRETURN(FALL_THROUGH_DELIMITER | RETURN_),
    ARETURN(FALL_THROUGH_DELIMITER | RETURN_),
    RETURN(FALL_THROUGH_DELIMITER | RETURN_),
    GETSTATIC(FIELD_READ),
    PUTSTATIC(FIELD_WRITE),
    GETFIELD(FIELD_READ),
    PUTFIELD(FIELD_WRITE),
    INVOKEVIRTUAL(INVOKE_),
    INVOKESPECIAL(INVOKE_),
    INVOKESTATIC(INVOKE_),
    INVOKEINTERFACE(INVOKE_),
    XXXUNUSEDXXX,
    NEW,
    NEWARRAY,
    ANEWARRAY,
    ARRAYLENGTH,
    ATHROW(FALL_THROUGH_DELIMITER),
    CHECKCAST,
    INSTANCEOF,
    MONITORENTER,
    MONITOREXIT,
    WIDE,
    MULTIANEWARRAY,
    IFNULL(CONDITIONAL_BRANCH),
    IFNONNULL(CONDITIONAL_BRANCH),
    GOTO_W(FALL_THROUGH_DELIMITER | UNCONDITIONAL_BRANCH),
    JSR_W(FALL_THROUGH_DELIMITER | UNCONDITIONAL_BRANCH | JSR_OR_RET),
    BREAKPOINT,

    // MaxineVM extensions

    /**
     * Calls a native function, linking it first if necessary. This instruction can only be used in a
     * {@linkplain NativeStubGenerator generated native method stub}. The native function to be called is the one
     * implementing the {@code native} method for which the stub was generated.
     *
     * <pre>
     * Format: CALLNATIVE
     *         indexbyte1
     *         indexbyte2
     * </pre>
     *
     * Stack: ..., [arg1, [arg2 ...]] => ...
     * <p>
     * The unsigned indexbyte1 and indexbyte2 are used to construct an index into the constant pool of the current
     * class, where the value of the index is {@code (indexbyte1 << 8) | indexbyte2}. The constant pool item at that
     * index must be a CONSTANT_Utf8_info structure, which gives the {@linkplain SignatureDescriptor descriptor} of the
     * native function.
     * <p>
     * If the native method is not annotated with {@link C_FUNCTION}, then the stub is for a native call adhering to
     * the JNI specification. In this case, the descriptor will match the C function prototype generated by <a
     * href="http://java.sun.com/javase/6/docs/technotes/tools/windows/javah.html">javah</a>.
     * <p>
     * The operand stack must contain nargs argument values, where the number, type, and order of the values must be
     * consistent with the descriptor.
     * <p>
     * If platform-dependent code that implements it has not yet been bound into the Java virtual machine, that is done.
     * <p>
     * If the native method is not annotated with {@link C_FUNCTION} then the VM makes no assumption about the execution
     * of the native code. In particular, it does not require the execution to re-enter Java code (either by returning
     * or by calling a {@linkplain JniFunctions JNI function}) in a timely manner. Nor does the VM assume that it can
     * interpret the subsequent stack frame(s) entered by the native code. For these reasons, the following prologue
     * must be performed immediately before entering native code:
     * <ol>
     * <li>The current Java frame info (stack, frame and instruction pointers) is recorded to
     * {@linkplain VmThreadLocals thread locals}.
     * <li>The thread must be put in a state that denotes it is now executing in native code. While in this state, the
     * VM must guarantee that the thread does not mutate any object references.
     * </ol>
     * In addition, the {@link NativeCallEpilogue} must be performed immediately after returning from the call.>
     */
    CALLNATIVE(EXTENSION),

    // Reserved

    RESERVED_204,
    RESERVED_205,
    RESERVED_206,
    RESERVED_207,
    RESERVED_208,
    RESERVED_209,
    RESERVED_210,
    RESERVED_211,
    RESERVED_212,
    RESERVED_213,
    RESERVED_214,
    RESERVED_215,
    RESERVED_216,
    RESERVED_217,
    RESERVED_218,
    RESERVED_219,
    RESERVED_220,
    RESERVED_221,
    RESERVED_222,
    RESERVED_223,
    RESERVED_224,
    RESERVED_225,
    RESERVED_226,
    RESERVED_227,
    RESERVED_228,
    RESERVED_229,
    RESERVED_230,
    RESERVED_231,
    RESERVED_232,
    RESERVED_233,
    RESERVED_234,
    RESERVED_235,
    RESERVED_236,
    RESERVED_237,
    RESERVED_238,
    RESERVED_239,
    RESERVED_240,
    RESERVED_241,
    RESERVED_242,
    RESERVED_243,
    RESERVED_244,
    RESERVED_245,
    RESERVED_246,
    RESERVED_247,
    RESERVED_248,
    RESERVED_249,
    RESERVED_250,
    RESERVED_251,
    RESERVED_252,
    RESERVED_253,

    // Standard implementation dependent opcodes

    IMPDEP1,
    IMPDEP2;

    private final int flags;

    private final int implicitNumericOperand;

    public static final int INVALID_IMPLICIT_NUMERIC_OPERAND = 0x10101010;

    private Bytecode() {
        this(0);
    }

    private Bytecode(int flags) {
        this.flags = flags;
        final String name = name();
        final int lastUnderscoreIndex = name.lastIndexOf('_');
        if (lastUnderscoreIndex != -1 && Character.isDigit(name.charAt(lastUnderscoreIndex + 1))) {
            implicitNumericOperand = Integer.parseInt(name.substring(lastUnderscoreIndex + 1));
        } else {
            if (ordinal() == 2) {
                // ICONST_M1
                implicitNumericOperand = -1;
            } else {
                implicitNumericOperand = INVALID_IMPLICIT_NUMERIC_OPERAND;
            }
        }
    }

    public Bytecode bytecode() {
        return this;
    }

    /**
     * Returns the bytecode constant for a given opcode value.
     *
     * @param value  the ordinal value of an opcode (only the low byte of which will be used here)
     * @throws ClassFormatError if {@code value} does not denote a bytecode constant
     */
    public static final Bytecode from(int value) {
        try {
            return VALUES.get(value & 0xff);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw classFormatError("Invalid opcode: " + value);
        }
    }

    /**
     * Gets the numeric operand implied by this opcode's mnemonic.
     *
     * @return {@value #INVALID_IMPLICIT_NUMERIC_OPERAND} if this opcode's mnemonic does not imply a numeric operand
     */
    public int implicitNumericOperand() {
        return implicitNumericOperand;
    }

    /**
     * Writes this bytecode's value to a given stream.
     */
    public void writeTo(OutputStream stream) throws IOException {
        stream.write(ordinal());
    }

    /**
     * Determines if this bytecode can be present in a well formed class file.
     */
    public final boolean isLegalInClassfile() {
        return this != XXXUNUSEDXXX && ordinal() < BREAKPOINT.ordinal();
    }

    /**
     * Determines if any of the given attributes applies to this bytecode. In combination with the <a
     * href="http://java.sun.com/j2se/1.5.0/docs/guide/language/static-import.html">static import facility</a>, use of
     * this method can make for quite readable bytecode attribute testing. For example:
     *
     * <pre>
     * import static com.sun.max.vm.bytecode.Bytecode.Flags.*;
     * ...
     *     return bytecode.is(FIELD_READ | FIELD_WRITE);
     * </pre>
     *
     * @param maskOfFlags
     *                a mask of {@linkplain Flags attribute constants}
     */
    public final boolean is(int maskOfFlags) {
        return (flags & maskOfFlags) != 0;
    }

    /**
     * Determines if none of the given attributes applies to this bytecode.
     *
     * @param maskOfFlags a mask of {@linkplain Flags attribute constants}
     */
    public final boolean isNot(int maskOfFlags) {
        return (flags & maskOfFlags) == 0;
    }

    /**
     * Immutable (and thus sharable) view of the enum constants defined by this class.
     */
    public static final IndexedSequence<Bytecode> VALUES = new ArraySequence<Bytecode>(values());

    static {
        if (MaxineVM.isHosted()) {
            verifyFlags();
        }
    }

    @HOSTED_ONLY
    private static void verifyFlags() {
        try {
            int allFlags = 0;
            for (Field field : Flags.class.getDeclaredFields()) {
                ProgramError.check(field.getType() == int.class);
                final int flag = field.getInt(null);
                ProgramError.check(flag != 0);
                ProgramError.check((flag & allFlags) == 0, field.getName() + " has a value conflicting with another flag");
                allFlags |= flag;
            }
        } catch (Exception e) {
            ProgramError.unexpected(e);
        }
    }

    /**
     * A collection of flags describing various bytecode attributes.
     */
    public static class Flags {

        /**
         * Denotes a {@code INVOKEVIRTUAL}, {@code INVOKESPECIAL}, {@code INVOKEINTERFACE} or {@code INVOKESTATIC} instruction.
         */
        public static final int INVOKE_ = 0x00000001;

        /**
         * Denotes a {@code LDC} or {@code LDC_W} instruction.
         */
        public static final int LDC_ = 0x00000002;

        /**
         * Denotes an instruction that never falls through to its lexical successor.
         */
        public static final int FALL_THROUGH_DELIMITER = 0x00000004;

        /**
         * Denotes an instruction that may branch. This does not include the {@linkplain #SWITCH switch} instructions.
         */
        public static final int CONDITIONAL_BRANCH = 0x00000008;

        /**
         * Denotes an instruction that always branches. This does not include the {@linkplain #SWITCH switch} instructions.
         */
        public static final int UNCONDITIONAL_BRANCH = 0x00000010;

        /**
         * Denotes an instruction that reads the value of a static or instance field.
         */
        public static final int FIELD_READ = 0x00000020;

        /**
         * Denotes an instruction that writes the value of a static or instance field.
         */
        public static final int FIELD_WRITE = 0x00000040;

        /**
         * Denotes an instruction that is not defined in the JVM specification.
         */
        public static final int EXTENSION = 0x00000080;

        /**
         * Denotes a {@code TABLESWITCH} or {@code LOOKUPSWITCH} instruction.
         */
        public static final int SWITCH = 0x00000100;

        /**
         * Denotes a {@code RETURN}, {@code IRETURN}, {@code LRETURN}, {@code DRETURN}, {@code ARETURN} or {@code FRETURN} instruction.
         */
        public static final int RETURN_ = 0x00000200;

        /**
         * Denotes a {@code RET}, {@code JSR}, {@code JSR_W} instruction.
         */
        public static final int JSR_OR_RET = 0x00000400;
    }
}

