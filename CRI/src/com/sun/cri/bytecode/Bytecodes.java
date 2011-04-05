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
package com.sun.cri.bytecode;

import static com.sun.cri.bytecode.Bytecodes.Flags.*;
import static com.sun.cri.bytecode.Bytecodes.JniOp.*;
import static com.sun.cri.bytecode.Bytecodes.MemoryBarriers.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

/**
 * The definitions of the bytecodes that are valid input to the compiler and
 * related utility methods. This comprises two groups: the standard Java
 * bytecodes defined by <a href=
 * "http://java.sun.com/docs/books/jvms/second_edition/html/VMSpecTOC.doc.html">
 * Java Virtual Machine Specification</a>, and a set of <i>extended</i>
 * bytecodes that support low-level programming, for example, memory barriers.
 *
 * The extended bytecodes are one or three bytes in size. The one-byte bytecodes
 * follow the values in the standard set, with no gap. The three-byte extended
 * bytecodes share a common first byte and carry additional instruction-specific
 * information in the second and third bytes.
 *
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class Bytecodes {
    public static final int NOP                  =   0; // 0x00
    public static final int ACONST_NULL          =   1; // 0x01
    public static final int ICONST_M1            =   2; // 0x02
    public static final int ICONST_0             =   3; // 0x03
    public static final int ICONST_1             =   4; // 0x04
    public static final int ICONST_2             =   5; // 0x05
    public static final int ICONST_3             =   6; // 0x06
    public static final int ICONST_4             =   7; // 0x07
    public static final int ICONST_5             =   8; // 0x08
    public static final int LCONST_0             =   9; // 0x09
    public static final int LCONST_1             =  10; // 0x0A
    public static final int FCONST_0             =  11; // 0x0B
    public static final int FCONST_1             =  12; // 0x0C
    public static final int FCONST_2             =  13; // 0x0D
    public static final int DCONST_0             =  14; // 0x0E
    public static final int DCONST_1             =  15; // 0x0F
    public static final int BIPUSH               =  16; // 0x10
    public static final int SIPUSH               =  17; // 0x11
    public static final int LDC                  =  18; // 0x12
    public static final int LDC_W                =  19; // 0x13
    public static final int LDC2_W               =  20; // 0x14
    public static final int ILOAD                =  21; // 0x15
    public static final int LLOAD                =  22; // 0x16
    public static final int FLOAD                =  23; // 0x17
    public static final int DLOAD                =  24; // 0x18
    public static final int ALOAD                =  25; // 0x19
    public static final int ILOAD_0              =  26; // 0x1A
    public static final int ILOAD_1              =  27; // 0x1B
    public static final int ILOAD_2              =  28; // 0x1C
    public static final int ILOAD_3              =  29; // 0x1D
    public static final int LLOAD_0              =  30; // 0x1E
    public static final int LLOAD_1              =  31; // 0x1F
    public static final int LLOAD_2              =  32; // 0x20
    public static final int LLOAD_3              =  33; // 0x21
    public static final int FLOAD_0              =  34; // 0x22
    public static final int FLOAD_1              =  35; // 0x23
    public static final int FLOAD_2              =  36; // 0x24
    public static final int FLOAD_3              =  37; // 0x25
    public static final int DLOAD_0              =  38; // 0x26
    public static final int DLOAD_1              =  39; // 0x27
    public static final int DLOAD_2              =  40; // 0x28
    public static final int DLOAD_3              =  41; // 0x29
    public static final int ALOAD_0              =  42; // 0x2A
    public static final int ALOAD_1              =  43; // 0x2B
    public static final int ALOAD_2              =  44; // 0x2C
    public static final int ALOAD_3              =  45; // 0x2D
    public static final int IALOAD               =  46; // 0x2E
    public static final int LALOAD               =  47; // 0x2F
    public static final int FALOAD               =  48; // 0x30
    public static final int DALOAD               =  49; // 0x31
    public static final int AALOAD               =  50; // 0x32
    public static final int BALOAD               =  51; // 0x33
    public static final int CALOAD               =  52; // 0x34
    public static final int SALOAD               =  53; // 0x35
    public static final int ISTORE               =  54; // 0x36
    public static final int LSTORE               =  55; // 0x37
    public static final int FSTORE               =  56; // 0x38
    public static final int DSTORE               =  57; // 0x39
    public static final int ASTORE               =  58; // 0x3A
    public static final int ISTORE_0             =  59; // 0x3B
    public static final int ISTORE_1             =  60; // 0x3C
    public static final int ISTORE_2             =  61; // 0x3D
    public static final int ISTORE_3             =  62; // 0x3E
    public static final int LSTORE_0             =  63; // 0x3F
    public static final int LSTORE_1             =  64; // 0x40
    public static final int LSTORE_2             =  65; // 0x41
    public static final int LSTORE_3             =  66; // 0x42
    public static final int FSTORE_0             =  67; // 0x43
    public static final int FSTORE_1             =  68; // 0x44
    public static final int FSTORE_2             =  69; // 0x45
    public static final int FSTORE_3             =  70; // 0x46
    public static final int DSTORE_0             =  71; // 0x47
    public static final int DSTORE_1             =  72; // 0x48
    public static final int DSTORE_2             =  73; // 0x49
    public static final int DSTORE_3             =  74; // 0x4A
    public static final int ASTORE_0             =  75; // 0x4B
    public static final int ASTORE_1             =  76; // 0x4C
    public static final int ASTORE_2             =  77; // 0x4D
    public static final int ASTORE_3             =  78; // 0x4E
    public static final int IASTORE              =  79; // 0x4F
    public static final int LASTORE              =  80; // 0x50
    public static final int FASTORE              =  81; // 0x51
    public static final int DASTORE              =  82; // 0x52
    public static final int AASTORE              =  83; // 0x53
    public static final int BASTORE              =  84; // 0x54
    public static final int CASTORE              =  85; // 0x55
    public static final int SASTORE              =  86; // 0x56
    public static final int POP                  =  87; // 0x57
    public static final int POP2                 =  88; // 0x58
    public static final int DUP                  =  89; // 0x59
    public static final int DUP_X1               =  90; // 0x5A
    public static final int DUP_X2               =  91; // 0x5B
    public static final int DUP2                 =  92; // 0x5C
    public static final int DUP2_X1              =  93; // 0x5D
    public static final int DUP2_X2              =  94; // 0x5E
    public static final int SWAP                 =  95; // 0x5F
    public static final int IADD                 =  96; // 0x60
    public static final int LADD                 =  97; // 0x61
    public static final int FADD                 =  98; // 0x62
    public static final int DADD                 =  99; // 0x63
    public static final int ISUB                 = 100; // 0x64
    public static final int LSUB                 = 101; // 0x65
    public static final int FSUB                 = 102; // 0x66
    public static final int DSUB                 = 103; // 0x67
    public static final int IMUL                 = 104; // 0x68
    public static final int LMUL                 = 105; // 0x69
    public static final int FMUL                 = 106; // 0x6A
    public static final int DMUL                 = 107; // 0x6B
    public static final int IDIV                 = 108; // 0x6C
    public static final int LDIV                 = 109; // 0x6D
    public static final int FDIV                 = 110; // 0x6E
    public static final int DDIV                 = 111; // 0x6F
    public static final int IREM                 = 112; // 0x70
    public static final int LREM                 = 113; // 0x71
    public static final int FREM                 = 114; // 0x72
    public static final int DREM                 = 115; // 0x73
    public static final int INEG                 = 116; // 0x74
    public static final int LNEG                 = 117; // 0x75
    public static final int FNEG                 = 118; // 0x76
    public static final int DNEG                 = 119; // 0x77
    public static final int ISHL                 = 120; // 0x78
    public static final int LSHL                 = 121; // 0x79
    public static final int ISHR                 = 122; // 0x7A
    public static final int LSHR                 = 123; // 0x7B
    public static final int IUSHR                = 124; // 0x7C
    public static final int LUSHR                = 125; // 0x7D
    public static final int IAND                 = 126; // 0x7E
    public static final int LAND                 = 127; // 0x7F
    public static final int IOR                  = 128; // 0x80
    public static final int LOR                  = 129; // 0x81
    public static final int IXOR                 = 130; // 0x82
    public static final int LXOR                 = 131; // 0x83
    public static final int IINC                 = 132; // 0x84
    public static final int I2L                  = 133; // 0x85
    public static final int I2F                  = 134; // 0x86
    public static final int I2D                  = 135; // 0x87
    public static final int L2I                  = 136; // 0x88
    public static final int L2F                  = 137; // 0x89
    public static final int L2D                  = 138; // 0x8A
    public static final int F2I                  = 139; // 0x8B
    public static final int F2L                  = 140; // 0x8C
    public static final int F2D                  = 141; // 0x8D
    public static final int D2I                  = 142; // 0x8E
    public static final int D2L                  = 143; // 0x8F
    public static final int D2F                  = 144; // 0x90
    public static final int I2B                  = 145; // 0x91
    public static final int I2C                  = 146; // 0x92
    public static final int I2S                  = 147; // 0x93
    public static final int LCMP                 = 148; // 0x94
    public static final int FCMPL                = 149; // 0x95
    public static final int FCMPG                = 150; // 0x96
    public static final int DCMPL                = 151; // 0x97
    public static final int DCMPG                = 152; // 0x98
    public static final int IFEQ                 = 153; // 0x99
    public static final int IFNE                 = 154; // 0x9A
    public static final int IFLT                 = 155; // 0x9B
    public static final int IFGE                 = 156; // 0x9C
    public static final int IFGT                 = 157; // 0x9D
    public static final int IFLE                 = 158; // 0x9E
    public static final int IF_ICMPEQ            = 159; // 0x9F
    public static final int IF_ICMPNE            = 160; // 0xA0
    public static final int IF_ICMPLT            = 161; // 0xA1
    public static final int IF_ICMPGE            = 162; // 0xA2
    public static final int IF_ICMPGT            = 163; // 0xA3
    public static final int IF_ICMPLE            = 164; // 0xA4
    public static final int IF_ACMPEQ            = 165; // 0xA5
    public static final int IF_ACMPNE            = 166; // 0xA6
    public static final int GOTO                 = 167; // 0xA7
    public static final int JSR                  = 168; // 0xA8
    public static final int RET                  = 169; // 0xA9
    public static final int TABLESWITCH          = 170; // 0xAA
    public static final int LOOKUPSWITCH         = 171; // 0xAB
    public static final int IRETURN              = 172; // 0xAC
    public static final int LRETURN              = 173; // 0xAD
    public static final int FRETURN              = 174; // 0xAE
    public static final int DRETURN              = 175; // 0xAF
    public static final int ARETURN              = 176; // 0xB0
    public static final int RETURN               = 177; // 0xB1
    public static final int GETSTATIC            = 178; // 0xB2
    public static final int PUTSTATIC            = 179; // 0xB3
    public static final int GETFIELD             = 180; // 0xB4
    public static final int PUTFIELD             = 181; // 0xB5
    public static final int INVOKEVIRTUAL        = 182; // 0xB6
    public static final int INVOKESPECIAL        = 183; // 0xB7
    public static final int INVOKESTATIC         = 184; // 0xB8
    public static final int INVOKEINTERFACE      = 185; // 0xB9
    public static final int XXXUNUSEDXXX         = 186; // 0xBA
    public static final int NEW                  = 187; // 0xBB
    public static final int NEWARRAY             = 188; // 0xBC
    public static final int ANEWARRAY            = 189; // 0xBD
    public static final int ARRAYLENGTH          = 190; // 0xBE
    public static final int ATHROW               = 191; // 0xBF
    public static final int CHECKCAST            = 192; // 0xC0
    public static final int INSTANCEOF           = 193; // 0xC1
    public static final int MONITORENTER         = 194; // 0xC2
    public static final int MONITOREXIT          = 195; // 0xC3
    public static final int WIDE                 = 196; // 0xC4
    public static final int MULTIANEWARRAY       = 197; // 0xC5
    public static final int IFNULL               = 198; // 0xC6
    public static final int IFNONNULL            = 199; // 0xC7
    public static final int GOTO_W               = 200; // 0xC8
    public static final int JSR_W                = 201; // 0xC9
    public static final int BREAKPOINT           = 202; // 0xCA

    // Start extended bytecodes

    /**
     * Native function call.
     *
     * The 'function_address' value on the top of the stack is the result of
     * linking a native function. Typically, a {@link #JNIOP_LINK} operation
     * is used to obtain this address.
     * 
     * <pre>
     * Format: { u1 opcode;  // JNICALL
     *           u2 sig;     // Constant pool index of a CONSTANT_Utf8_info representing the signature of the call
     *         }
     *
     * Operand Stack:
     *     ..., [arg1, [arg2 ... ]] function_address => [return value, ]...,
     * </pre>
     *
     * @see #JNIOP_LINK
     * @see #JNIOP_J2N
     * @see #JNIOP_N2J
     */
    public static final int JNICALL              = 203;
    
    /**
     * @see #JNIOP_LINK
     * @see #JNIOP_J2N
     * @see #JNIOP_N2J
     */
    public static final int JNIOP                = 204;
    
    /**
     * Template call.
     * 
     * <pre>
     * Format: { u1 opcode;  // CALL
     *           u2 sig;     // Constant pool index of a CONSTANT_Utf8_info representing the signature of the call.
     *                          The parameters part of the signature must be "()", "(word)" or "(word, object)".
     *         }
     *
     * Operand Stack:
     *     ..., [arg1, [arg2 ... ]] => [return value, ]...,
     * </pre>
     */
    public static final int TEMPLATE_CALL        = 205;

    public static final int WLOAD                = 206;
    public static final int WLOAD_0              = 207;
    public static final int WLOAD_1              = 208;
    public static final int WLOAD_2              = 209;
    public static final int WLOAD_3              = 210;

    public static final int WSTORE               = 211;
    public static final int WSTORE_0             = 212;
    public static final int WSTORE_1             = 213;
    public static final int WSTORE_2             = 214;
    public static final int WSTORE_3             = 215;

    public static final int WCONST_0             = 216;
    public static final int WDIV                 = 217;
    public static final int WDIVI                = 218; // Divisor is an int
    public static final int WREM                 = 219;
    public static final int WREMI                = 220; // Divisor is an int

    public static final int ICMP                 = 221; // Signed int compare, sets condition flags (for template JIT)
    public static final int WCMP                 = 222; // Word compare, sets condition flags (for template JIT)

    public static final int PREAD                = 223;
    public static final int PWRITE               = 224;

    public static final int PGET                 = 225;
    public static final int PSET                 = 226;

    /**
     * Atomic update of a value in memory.
     * 
     * Compares {@code expectedValue} value with the actual value in a memory location (given by {@code pointer + offset}).
     * Iff they are same, {@code newValue} is stored into the memory location and the {@code expectedValue} is returned.
     * Otherwise, the actual value is returned.
     * All of the above is performed in one atomic hardware transaction.
     * <pre>
     * Format: { u3 opcode;   // PCMPSWP_INT, PCMPSWP_WORD, PCMPSWP_REFERENCE,
     *                        // PCMPSWP_INT_I, PCMPSWP_WORD_I or PCMPSWP_REFERENCE_I
     *         }
     *         
     * Operand Stack:
     *     ... pointer, offset, expectedValue, newValue => ..., result
     *
     * param pointer base of denoted memory location
     * param offset offset from pointer to the memory location
     * param expectedValue if this value is currently in the memory location, perform the swap
     * param newValue the new value to store into the memory location
     * return either expectedValue or the actual value
     * </pre>
     */
    public static final int PCMPSWP               = 227;

    public static final int MOV_I2F              = 228;
    public static final int MOV_F2I              = 229;
    public static final int MOV_L2D              = 230;
    public static final int MOV_D2L              = 231;

    /**
     * Unsigned integer comparison.
     *
     * <pre>
     * Format: { u1 opcode;   // UCMP
     *           u2 op;       // ABOVE_EQUAL, ABOVE_THAN, BELOW_EQUAL or BELOW_THAN
     *         }
     *
     * Operand Stack:
     *     ..., left, right => ..., result
     * </pre>
     *
     * @see UnsignedComparisons
     */
    public static final int UCMP                 = 232;

    /**
     * Unsigned word comparison.
     *
     * <pre>
     * Format: { u1 opcode;   // UWCMP
     *           u2 op;       // ABOVE_EQUAL, ABOVE_THAN, BELOW_EQUAL or BELOW_THAN
     *         }
     *
     * Operand Stack:
     *     ..., left, right => ..., result
     * </pre>
     *
     * @see UnsignedComparisons
     */
    public static final int UWCMP                = 233;

    /**
     * Reads the value of a register playing a runtime-defined role.
     *
     * <pre>
     * Format: { u1 opcode;   // READREG
     *           u2 role;     // runtime-defined register role id
     *         }
     *
     * Operand Stack:
     *     ... => ..., value
     * </pre>
     */
    public static final int READREG              = 234;

    /**
     * Writes the value of a register playing a runtime-defined role.
     *
     * <pre>
     * Format: { u1 opcode;   // WRITEREG
     *           u2 role;     // runtime-defined register role id
     *         }
     *
     * Operand Stack:
     *     ..., value => ...
     * </pre>
     */
    public static final int WRITEREG             = 235;

    public static final int INCREG               = 236;
    
    /**
     * Unsafe cast of top value on stack.
     *
     * <pre>
     * Format: { u1 opcode;   // UNSAFE_CAST
     *           u2 method;   // Constant pool index to method (CONSTANT_Methodref_info) whose signature
     *                        // describes the source and target types of the cast
     *         }
     *
     * Operand Stack:
     *     ..., value => ..., value
     * </pre>
     */
    public static final int UNSAFE_CAST          = 237;

    public static final int WRETURN              = 238;
    
    /**
     * Record debug info at the current code location.
     * 
     * <pre>
     * Format: { u1 opcode;    // INFOPOINT
     *           u1 opcode2;   // SAFEPOINT, HERE or HERE_NOP
     *           u1 inclFrame; // non-zero if the full frame is to be saved 
     *         }
     * </pre>
     * 
     * @see #SAFEPOINT
     * @see #HERE
     * @see #INFO
     */
    public static final int INFOPOINT        = 239;

    /**
     * Record debug info at the current code location
     * and emit the instruction(s) that enable a thread
     * to be safely stopped for a VM operation (e.g. a GC).
     * 
     * <pre>
     * Format: { u1 opcode;    // INFOPOINT
     *           u1 opcode2;   // SAFEPOINT
     *           u1 inclFrame; // non-zero if the full frame is to be saved
     *         }
     *
     * Operand Stack:
     *     ... => ...
     * </pre>
     */
    public static final int SAFEPOINT         = INFOPOINT  | 1 << 16;

    /**
     * Record debug info at the current code location
     * and push its address to the stack. This is useful (for example)
     * when initiating a stack walk from the current execution position.
     * 
     * <pre>
     * Format: { u1 opcode;    // INFOPOINT
     *           u1 opcode2;   // HERE
     *           u1 inclFrame; // non-zero if the full frame is to be saved 
     *         }
     *
     * Operand Stack:
     *     ... => ..., value
     * </pre>
     */
    public static final int HERE              = INFOPOINT  | 2 << 16;

    /**
     * Record debug info at the current code location.
     * No instructions are emitted.
     * 
     * <pre>
     * Format: { u1 opcode;    // INFOPOINT
     *           u1 opcode2;   // HERE_NOP
     *           u1 inclFrame; // non-zero if the full frame is to be saved 
     *         }
     *
     * Operand Stack:
     *     ... => ...
     * </pre>
     */
    public static final int INFO          = INFOPOINT  | 3 << 16;
    
    /**
     * Allocates a requested block of memory within the current activation frame.
     * The allocated memory is reclaimed when the method returns.
     *
     * The allocation is for the lifetime of the method execution. That is, the compiler
     * reserves the space in the compiled size of the frame. As such, a failure
     * to allocate the requested space will result in a {@link StackOverflowError}
     * when the method's prologue is executed.
     *
     * <pre>
     * Format: { u1 opcode;   // ALLOCA
     *           u2 unused;
     *         }
     *
     * Operand Stack:
     *     ..., size => ..., address
     * </pre>
     *
     * The value on the top of the stack is the size in bytes to allocate.
     * The result is the address of the allocated block. <b>N.B.</b> The contents of the block are uninitialized.
     */
    public static final int ALLOCA               = 240;

    /**
     * Inserts a memory barrier.
     *
     * <pre>
     * Format: { u1 opcode;   // MEMBAR
     *           u2 barriers;  // mask of constants defined in {@link MemoryBarriers}
     *         }
     *
     * Operand Stack:
     *     ... => ...
     * </pre>
     * 
     * @see MemoryBarriers
     */
    public static final int MEMBAR               = 241;
    
    /**
     * Create a handle for a given value on the native stack frame.
     * <p>
     * Forces the compiler to allocate a native frame slot and initializes it with {@code value}
     * If the {@code value} is 0 or {@code null}, then the result is 0 otherwise the result is the
     * address of the native frame slot.
     * <p>
     * The native frame slot will be live for the rest of the method. The type of {@code value} determines
     * if it is a GC root. If {@code value} is an object, any subsequent value written to the slot
     * (via {@code address}) must be an object. If {@code value} is not an object, any subsequent value
     * written to the slot must not be an object.
     * 
     * <b>The compiler is not required to enforce this type safety.</b>
     * 
     * <pre>
     * Format: { u1 opcode;   // STACKHANDLE
     *           u2 method;   // Constant pool index to method (CONSTANT_Methodref_info) whose signature
     *                        // describes the type of the input value
     *         }
     *
     * Operand Stack:
     *     ..., value => ..., address
     * </pre>
     */
    public static final int STACKHANDLE          = 242;
    
    public static final int PAUSE                = 243;
    public static final int BREAKPOINT_TRAP      = 244;
    public static final int ADD_SP               = 245;
    public static final int FLUSHW               = 246;
    
    /**
     * Produces the index of the least significant bit within {@code value} or {@code -1} if {@code value == 0}.
     * 
     * <pre>
     * Format: { u1 opcode;  // LSB
     *           u2 ignore;
     *         }
     *
     * Operand Stack:
     *     ..., value => ..., index
     * </pre>
     */
    public static final int LSB                  = 247;
    
    /**
     * Produces the index of the most significant bit within {@code value} or {@code -1} if {@code value == 0}.
     * 
     * <pre>
     * Format: { u1 opcode;  // MSB
     *           u2 ignore;
     *         }
     *
     * Operand Stack:
     *     ..., value => ..., index
     * </pre>
     */
    public static final int MSB                  = 248;

    // End extended bytecodes

    // Extended bytecodes with operand:

    // Pointer compare-and-swap with word-sized offset
    public static final int PCMPSWP_INT         = PCMPSWP  | 1 << 8;
    public static final int PCMPSWP_LONG        = PCMPSWP  | 2 << 8;
    public static final int PCMPSWP_WORD        = PCMPSWP  | 3 << 8;
    public static final int PCMPSWP_REFERENCE   = PCMPSWP  | 4 << 8;

    // Pointer compare-and-swap with int-sized offset
    public static final int PCMPSWP_INT_I       = PCMPSWP  | 5 << 8;
    public static final int PCMPSWP_LONG_I      = PCMPSWP  | 6 << 8;
    public static final int PCMPSWP_WORD_I      = PCMPSWP  | 7 << 8;
    public static final int PCMPSWP_REFERENCE_I = PCMPSWP  | 8 << 8;

    // Pointer read with word-sized offset
    public static final int PREAD_BYTE         = PREAD  | 1 << 8;
    public static final int PREAD_CHAR         = PREAD  | 2 << 8;
    public static final int PREAD_SHORT        = PREAD  | 3 << 8;
    public static final int PREAD_INT          = PREAD  | 4 << 8;
    public static final int PREAD_FLOAT        = PREAD  | 5 << 8;
    public static final int PREAD_LONG         = PREAD  | 6 << 8;
    public static final int PREAD_DOUBLE       = PREAD  | 7 << 8;
    public static final int PREAD_WORD         = PREAD  | 8 << 8;
    public static final int PREAD_REFERENCE    = PREAD  | 9 << 8;

    // Pointer read with int-sized offset
    public static final int PREAD_BYTE_I       = PREAD  | 10  << 8;
    public static final int PREAD_CHAR_I       = PREAD  | 11 << 8;
    public static final int PREAD_SHORT_I      = PREAD  | 12 << 8;
    public static final int PREAD_INT_I        = PREAD  | 13 << 8;
    public static final int PREAD_FLOAT_I      = PREAD  | 14 << 8;
    public static final int PREAD_LONG_I       = PREAD  | 15 << 8;
    public static final int PREAD_DOUBLE_I     = PREAD  | 16 << 8;
    public static final int PREAD_WORD_I       = PREAD  | 17 << 8;
    public static final int PREAD_REFERENCE_I  = PREAD  | 18 << 8;

    // Pointer write with word-sized offset
    public static final int PWRITE_BYTE        = PWRITE | 1 << 8;
    public static final int PWRITE_SHORT       = PWRITE | 2 << 8;
    public static final int PWRITE_INT         = PWRITE | 3 << 8;
    public static final int PWRITE_FLOAT       = PWRITE | 4 << 8;
    public static final int PWRITE_LONG        = PWRITE | 5 << 8;
    public static final int PWRITE_DOUBLE      = PWRITE | 6 << 8;
    public static final int PWRITE_WORD        = PWRITE | 7 << 8;
    public static final int PWRITE_REFERENCE   = PWRITE | 8 << 8;
    // Pointer write with int-sized offset
    public static final int PWRITE_BYTE_I      = PWRITE | 9  << 8;
    public static final int PWRITE_SHORT_I     = PWRITE | 10  << 8;
    public static final int PWRITE_INT_I       = PWRITE | 11 << 8;
    public static final int PWRITE_FLOAT_I     = PWRITE | 12 << 8;
    public static final int PWRITE_LONG_I      = PWRITE | 13 << 8;
    public static final int PWRITE_DOUBLE_I    = PWRITE | 14 << 8;
    public static final int PWRITE_WORD_I      = PWRITE | 15 << 8;
    public static final int PWRITE_REFERENCE_I = PWRITE | 16 << 8;

    public static final int PGET_BYTE          = PGET   | 1 << 8;
    public static final int PGET_CHAR          = PGET   | 2 << 8;
    public static final int PGET_SHORT         = PGET   | 3 << 8;
    public static final int PGET_INT           = PGET   | 4 << 8;
    public static final int PGET_FLOAT         = PGET   | 5 << 8;
    public static final int PGET_LONG          = PGET   | 6 << 8;
    public static final int PGET_DOUBLE        = PGET   | 7 << 8;
    public static final int PGET_WORD          = PGET   | 8 << 8;
    public static final int PGET_REFERENCE     = PGET   | 9 << 8;

    public static final int PSET_BYTE          = PSET   | 1 << 8;
    public static final int PSET_SHORT         = PSET   | 2 << 8;
    public static final int PSET_INT           = PSET   | 3 << 8;
    public static final int PSET_FLOAT         = PSET   | 4 << 8;
    public static final int PSET_LONG          = PSET   | 5 << 8;
    public static final int PSET_DOUBLE        = PSET   | 6 << 8;
    public static final int PSET_WORD          = PSET   | 7 << 8;
    public static final int PSET_REFERENCE     = PSET   | 8 << 8;

    public static final int MEMBAR_LOAD_LOAD   = MEMBAR   | LOAD_LOAD << 8;
    public static final int MEMBAR_LOAD_STORE  = MEMBAR   | LOAD_STORE << 8;
    public static final int MEMBAR_STORE_LOAD  = MEMBAR   | STORE_LOAD << 8;
    public static final int MEMBAR_STORE_STORE = MEMBAR   | STORE_STORE << 8;

    /**
     * Links a native function.
     * 
     * This instruction can only be used in a stub generated for a {@code native} method.
     * It causes the VM linker to find the address for the native function
     * corresponding to the native method.
     *
     * <pre>
     * Format: { u1 opcode;  // JNIOP
     *           u2 op;      // JniOp.LINK
     *         }
     *
     * Operand Stack:
     *     ... => address
     * </pre>
     */
    public static final int JNIOP_LINK         = JNIOP | LINK << 8;
    
    /**
     * Effects a transition from compiled Java code to native code.
     * 
     * This instruction must appear immediately before {@link #JNICALL} in the instruction stream.
     *
     * <pre>
     * Format: { u1 opcode;  // JNIOP
     *           u2 op;      // JniOp.J2N
     *         }
     *
     * Operand Stack:
     *     ... => ...
     * </pre>
     */
    public static final int JNIOP_J2N          = JNIOP | J2N << 8;

    /**
     * Effects a transition from native code to compiled Java code.
     * 
     * This instruction must appear immediately after {@link #JNICALL} in the instruction stream.
     *
     * <pre>
     * Format: { u1 opcode;  // JNIOP
     *           u2 op;      // JniOp.N2J
     *         }
     *
     * Operand Stack:
     *     ... => ...
     * </pre>
     */
    public static final int JNIOP_N2J          = JNIOP | N2J << 8;
    
    /**
     * Constants for the operations performed as part of JNI call from compiled Java code
     * to native code or vice versa.
     */
    public static class JniOp {
        /**
         * @see Bytecodes#JNIOP_LINK
         */
        public static final int LINK = 1;
        
        /**
         * @see Bytecodes#JNIOP_N2J
         */
        public static final int N2J = 2;

        /**
         * @see Bytecodes#JNIOP_J2N
         */
        public static final int J2N = 3;
    }
    
    /**
     * Constants and {@link INTRINSIC} definitions for unsigned comparisons.
     */
    public static class UnsignedComparisons {
        public static final int ABOVE_THAN    = 1;
        public static final int ABOVE_EQUAL   = 2;
        public static final int BELOW_THAN    = 3;
        public static final int BELOW_EQUAL   = 4;

        @INTRINSIC(UCMP | (ABOVE_EQUAL << 8))
        public static native boolean aboveOrEqual(int x, int y);

        @INTRINSIC(UCMP | (BELOW_EQUAL << 8))
        public static native boolean belowOrEqual(int x, int y);

        @INTRINSIC(UCMP | (ABOVE_THAN << 8))
        public static native boolean aboveThan(int x, int y);

        @INTRINSIC(UCMP | (BELOW_THAN << 8))
        public static native boolean belowThan(int x, int y);
    }
    
    /**
     * Intrinsic functions for using {@link Bytecodes#INFOPOINT}s.
     */
    public static class Infopoints {
        public static final int WITH_FRAME = 1 << 8;

        @INTRINSIC(SAFEPOINT | WITH_FRAME)
        public static native void safepoint();
        
        @INTRINSIC(SAFEPOINT)
        public static native void safepointWithoutFrame();
        
        @INTRINSIC(HERE)
        public static native long here();

        @INTRINSIC(HERE | WITH_FRAME)
        public static native long hereWithFrame();

        @INTRINSIC(INFO | WITH_FRAME)
        public static native void info();
        
        @INTRINSIC(INFO)
        public static native void infoWithoutFrame();
    }

    /**
     * Constants for memory barriers.
     *
     * The documentation for each constant is taken from Doug Lea's
     * <a href="http://gee.cs.oswego.edu/dl/jmm/cookbook.html">The JSR-133 Cookbook for Compiler Writers</a>.
     * <p>
     * The {@code JMM_*} constants capture the memory barriers necessary to implement the Java Memory Model
     * with respect to volatile field accesses. Their values are explained by this
     * comment from templateTable_i486.cpp in the HotSpot source code:
     * <pre>
     * Volatile variables demand their effects be made known to all CPU's in
     * order.  Store buffers on most chips allow reads & writes to reorder; the
     * JMM's ReadAfterWrite.java test fails in -Xint mode without some kind of
     * memory barrier (i.e., it's not sufficient that the interpreter does not
     * reorder volatile references, the hardware also must not reorder them).
     *
     * According to the new Java Memory Model (JMM):
     * (1) All volatiles are serialized wrt to each other.
     * ALSO reads & writes act as acquire & release, so:
     * (2) A read cannot let unrelated NON-volatile memory refs that happen after
     * the read float up to before the read.  It's OK for non-volatile memory refs
     * that happen before the volatile read to float down below it.
     * (3) Similarly, a volatile write cannot let unrelated NON-volatile memory refs
     * that happen BEFORE the write float down to after the write.  It's OK for
     * non-volatile memory refs that happen after the volatile write to float up
     * before it.
     *
     * We only put in barriers around volatile refs (they are expensive), not
     * _between_ memory refs (which would require us to track the flavor of the
     * previous memory refs).  Requirements (2) and (3) require some barriers
     * before volatile stores and after volatile loads.  These nearly cover
     * requirement (1) but miss the volatile-store-volatile-load case.  This final
     * case is placed after volatile-stores although it could just as well go
     * before volatile-loads.
     * </pre>
     */
    public static class MemoryBarriers {

        /**
         * The sequence {@code Load1; LoadLoad; Load2} ensures that {@code Load1}'s data are loaded before data accessed
         * by {@code Load2} and all subsequent load instructions are loaded. In general, explicit {@code LoadLoad}
         * barriers are needed on processors that perform speculative loads and/or out-of-order processing in which
         * waiting load instructions can bypass waiting stores. On processors that guarantee to always preserve load
         * ordering, these barriers amount to no-ops.
         */
        public static final int LOAD_LOAD   = 0x0001;

        /**
         * The sequence {@code Load1; LoadStore; Store2} ensures that {@code Load1}'s data are loaded before all data
         * associated with {@code Store2} and subsequent store instructions are flushed. {@code LoadStore} barriers are
         * needed only on those out-of-order processors in which waiting store instructions can bypass loads.
         */
        public static final int LOAD_STORE  = 0x0002;

        /**
         * The sequence {@code Store1; StoreLoad; Load2} ensures that {@code Store1}'s data are made visible to other
         * processors (i.e., flushed to main memory) before data accessed by {@code Load2} and all subsequent load
         * instructions are loaded. {@code StoreLoad} barriers protect against a subsequent load incorrectly using
         * {@code Store1}'s data value rather than that from a more recent store to the same location performed by a
         * different processor. Because of this, on the processors discussed below, a {@code StoreLoad} is strictly
         * necessary only for separating stores from subsequent loads of the same location(s) as were stored before the
         * barrier. {@code StoreLoad} barriers are needed on nearly all recent multiprocessors, and are usually the most
         * expensive kind. Part of the reason they are expensive is that they must disable mechanisms that ordinarily
         * bypass cache to satisfy loads from write-buffers. This might be implemented by letting the buffer fully
         * flush, among other possible stalls.
         */
        public static final int STORE_LOAD  = 0x0004;

        /**
         * The sequence {@code Store1; StoreStore; Store2} ensures that {@code Store1}'s data are visible to other
         * processors (i.e., flushed to memory) before the data associated with {@code Store2} and all subsequent store
         * instructions. In general, {@code StoreStore} barriers are needed on processors that do not otherwise
         * guarantee strict ordering of flushes from write buffers and/or caches to other processors or main memory.
         */
        public static final int STORE_STORE = 0x0008;

        private static void appendFlag(StringBuilder sb, boolean flag, String string) {
            if (flag) {
                sb.append(string);
            }
        }

        public static String barriersString(int barriers) {
            StringBuilder sb = new StringBuilder();
            appendFlag(sb, (barriers & LOAD_LOAD) != 0, "LOAD_LOAD ");
            appendFlag(sb, (barriers & LOAD_STORE) != 0, "LOAD_STORE ");
            appendFlag(sb, (barriers & STORE_LOAD) != 0, "STORE_LOAD ");
            appendFlag(sb, (barriers & STORE_STORE) != 0, "STORE_STORE ");
            return sb.toString().trim();
        }
        
        /**
         * Ensures all preceding loads complete before any subsequent loads.
         */
        @INTRINSIC(MEMBAR_LOAD_LOAD)
        public static native void loadLoad();

        /**
         * Ensures all preceding loads complete before any subsequent stores.
         */
        @INTRINSIC(MEMBAR_LOAD_STORE)
        public static native void loadStore();

        /**
         * Ensures all preceding stores complete before any subsequent loads.
         */
        @INTRINSIC(MEMBAR_STORE_LOAD)
        public static native void storeLoad();
        
        /**
         * Ensures all preceding stores complete before any subsequent stores.
         */
        @INTRINSIC(MEMBAR_STORE_STORE)
        public static native void storeStore();

        /**
         * Ensures all preceding stores and loads complete before any subsequent stores.
         */
        @INTRINSIC(MEMBAR | ((LOAD_STORE | STORE_STORE) << 8))
        public static native void memopStore();
        
        
        public static final int JMM_PRE_VOLATILE_WRITE = LOAD_STORE | STORE_STORE;
        public static final int JMM_POST_VOLATILE_WRITE = STORE_LOAD | STORE_STORE;
        public static final int JMM_PRE_VOLATILE_READ = 0;
        public static final int JMM_POST_VOLATILE_READ = LOAD_LOAD | LOAD_STORE;
    }

    public static final int ILLEGAL = 255;
    public static final int END = 256;

    /**
     * The last opcode defined by the JVM specification. To iterate over all JVM bytecodes:
     * <pre>
     *     for (int opcode = 0; opcode <= Bytecodes.LAST_JVM_OPCODE; ++opcode) {
     *         //
     *     }
     * </pre>
     */
    public static final int LAST_JVM_OPCODE = JSR_W;

    /**
     * A collection of flags describing various bytecode attributes.
     */
    static class Flags {

        /**
         * Denotes an instruction that ends a basic block and does not let control flow fall through to its lexical successor.
         */
        static final int STOP = 0x00000001;

        /**
         * Denotes an instruction that ends a basic block and may let control flow fall through to its lexical successor.
         * In practice this means it is a conditional branch.
         */
        static final int FALL_THROUGH = 0x00000002;

        /**
         * Denotes an instruction that has a 2 or 4 byte operand that is an offset to another instruction in the same method.
         * This does not include the {@link Bytecodes#TABLESWITCH} or {@link Bytecodes#LOOKUPSWITCH} instructions.
         */
        static final int BRANCH = 0x00000004;

        /**
         * Denotes an instruction that reads the value of a static or instance field.
         */
        static final int FIELD_READ = 0x00000008;

        /**
         * Denotes an instruction that writes the value of a static or instance field.
         */
        static final int FIELD_WRITE = 0x00000010;

        /**
         * Denotes an instruction that is not defined in the JVM specification.
         */
        static final int EXTENSION = 0x00000020;

        /**
         * Denotes an instruction that can cause a trap.
         */
        static final int TRAP        = 0x00000080;
        /**
         * Denotes an instruction that is commutative.
         */
        static final int COMMUTATIVE = 0x00000100;
        /**
         * Denotes an instruction that is associative.
         */
        static final int ASSOCIATIVE = 0x00000200;
        /**
         * Denotes an instruction that loads an operand.
         */
        static final int LOAD        = 0x00000400;
        /**
         * Denotes an instruction that stores an operand.
         */
        static final int STORE       = 0x00000800;

    }

    // Performs a sanity check that none of the flags overlap.
    static {
        int allFlags = 0;
        try {
            for (Field field : Flags.class.getDeclaredFields()) {
                int flagsFilter = Modifier.FINAL | Modifier.STATIC;
                if ((field.getModifiers() & flagsFilter) == flagsFilter) {
                    assert field.getType() == int.class : "Only " + field;
                    final int flag = field.getInt(null);
                    assert flag != 0;
                    assert (flag & allFlags) == 0 : field.getName() + " has a value conflicting with another flag";
                    allFlags |= flag;
                }
            }
        } catch (Exception e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     * A array that maps from a bytecode value to a {@link String} for the corresponding instruction mnemonic.
     * This will include the root instruction for the three-byte extended instructions.
     */
    private static final String[] names = new String[256];
    
    /**
     * Maps from a three-byte extended bytecode value to a {@link String} for the corresponding instruction mnemonic.
     */
    private static final HashMap<Integer, String> threeByteExtNames = new HashMap<Integer, String>();
    
    /**
     * A array that maps from a bytecode value to the set of {@link Flags} for the corresponding instruction.
     */
    private static final int[] flags = new int[256];
    
    /**
     * A array that maps from a bytecode value to the length in bytes for the corresponding instruction.
     */
    private static final int[] length = new int[256];

    // Checkstyle: stop
    static {
        def(NOP                 , "nop"             , "b"    );
        def(ACONST_NULL         , "aconst_null"     , "b"    );
        def(ICONST_M1           , "iconst_m1"       , "b"    );
        def(ICONST_0            , "iconst_0"        , "b"    );
        def(ICONST_1            , "iconst_1"        , "b"    );
        def(ICONST_2            , "iconst_2"        , "b"    );
        def(ICONST_3            , "iconst_3"        , "b"    );
        def(ICONST_4            , "iconst_4"        , "b"    );
        def(ICONST_5            , "iconst_5"        , "b"    );
        def(LCONST_0            , "lconst_0"        , "b"    );
        def(LCONST_1            , "lconst_1"        , "b"    );
        def(FCONST_0            , "fconst_0"        , "b"    );
        def(FCONST_1            , "fconst_1"        , "b"    );
        def(FCONST_2            , "fconst_2"        , "b"    );
        def(DCONST_0            , "dconst_0"        , "b"    );
        def(DCONST_1            , "dconst_1"        , "b"    );
        def(BIPUSH              , "bipush"          , "bc"   );
        def(SIPUSH              , "sipush"          , "bcc"  );
        def(LDC                 , "ldc"             , "bi"   , TRAP);
        def(LDC_W               , "ldc_w"           , "bii"  , TRAP);
        def(LDC2_W              , "ldc2_w"          , "bii"  , TRAP);
        def(ILOAD               , "iload"           , "bi"   , LOAD);
        def(LLOAD               , "lload"           , "bi"   , LOAD);
        def(FLOAD               , "fload"           , "bi"   , LOAD);
        def(DLOAD               , "dload"           , "bi"   , LOAD);
        def(ALOAD               , "aload"           , "bi"   , LOAD);
        def(ILOAD_0             , "iload_0"         , "b"    , LOAD);
        def(ILOAD_1             , "iload_1"         , "b"    , LOAD);
        def(ILOAD_2             , "iload_2"         , "b"    , LOAD);
        def(ILOAD_3             , "iload_3"         , "b"    , LOAD);
        def(LLOAD_0             , "lload_0"         , "b"    , LOAD);
        def(LLOAD_1             , "lload_1"         , "b"    , LOAD);
        def(LLOAD_2             , "lload_2"         , "b"    , LOAD);
        def(LLOAD_3             , "lload_3"         , "b"    , LOAD);
        def(FLOAD_0             , "fload_0"         , "b"    , LOAD);
        def(FLOAD_1             , "fload_1"         , "b"    , LOAD);
        def(FLOAD_2             , "fload_2"         , "b"    , LOAD);
        def(FLOAD_3             , "fload_3"         , "b"    , LOAD);
        def(DLOAD_0             , "dload_0"         , "b"    , LOAD);
        def(DLOAD_1             , "dload_1"         , "b"    , LOAD);
        def(DLOAD_2             , "dload_2"         , "b"    , LOAD);
        def(DLOAD_3             , "dload_3"         , "b"    , LOAD);
        def(ALOAD_0             , "aload_0"         , "b"    , LOAD);
        def(ALOAD_1             , "aload_1"         , "b"    , LOAD);
        def(ALOAD_2             , "aload_2"         , "b"    , LOAD);
        def(ALOAD_3             , "aload_3"         , "b"    , LOAD);
        def(IALOAD              , "iaload"          , "b"    , TRAP);
        def(LALOAD              , "laload"          , "b"    , TRAP);
        def(FALOAD              , "faload"          , "b"    , TRAP);
        def(DALOAD              , "daload"          , "b"    , TRAP);
        def(AALOAD              , "aaload"          , "b"    , TRAP);
        def(BALOAD              , "baload"          , "b"    , TRAP);
        def(CALOAD              , "caload"          , "b"    , TRAP);
        def(SALOAD              , "saload"          , "b"    , TRAP);
        def(ISTORE              , "istore"          , "bi"   , STORE);
        def(LSTORE              , "lstore"          , "bi"   , STORE);
        def(FSTORE              , "fstore"          , "bi"   , STORE);
        def(DSTORE              , "dstore"          , "bi"   , STORE);
        def(ASTORE              , "astore"          , "bi"   , STORE);
        def(ISTORE_0            , "istore_0"        , "b"    , STORE);
        def(ISTORE_1            , "istore_1"        , "b"    , STORE);
        def(ISTORE_2            , "istore_2"        , "b"    , STORE);
        def(ISTORE_3            , "istore_3"        , "b"    , STORE);
        def(LSTORE_0            , "lstore_0"        , "b"    , STORE);
        def(LSTORE_1            , "lstore_1"        , "b"    , STORE);
        def(LSTORE_2            , "lstore_2"        , "b"    , STORE);
        def(LSTORE_3            , "lstore_3"        , "b"    , STORE);
        def(FSTORE_0            , "fstore_0"        , "b"    , STORE);
        def(FSTORE_1            , "fstore_1"        , "b"    , STORE);
        def(FSTORE_2            , "fstore_2"        , "b"    , STORE);
        def(FSTORE_3            , "fstore_3"        , "b"    , STORE);
        def(DSTORE_0            , "dstore_0"        , "b"    , STORE);
        def(DSTORE_1            , "dstore_1"        , "b"    , STORE);
        def(DSTORE_2            , "dstore_2"        , "b"    , STORE);
        def(DSTORE_3            , "dstore_3"        , "b"    , STORE);
        def(ASTORE_0            , "astore_0"        , "b"    , STORE);
        def(ASTORE_1            , "astore_1"        , "b"    , STORE);
        def(ASTORE_2            , "astore_2"        , "b"    , STORE);
        def(ASTORE_3            , "astore_3"        , "b"    , STORE);
        def(IASTORE             , "iastore"         , "b"    , TRAP);
        def(LASTORE             , "lastore"         , "b"    , TRAP);
        def(FASTORE             , "fastore"         , "b"    , TRAP);
        def(DASTORE             , "dastore"         , "b"    , TRAP);
        def(AASTORE             , "aastore"         , "b"    , TRAP);
        def(BASTORE             , "bastore"         , "b"    , TRAP);
        def(CASTORE             , "castore"         , "b"    , TRAP);
        def(SASTORE             , "sastore"         , "b"    , TRAP);
        def(POP                 , "pop"             , "b"    );
        def(POP2                , "pop2"            , "b"    );
        def(DUP                 , "dup"             , "b"    );
        def(DUP_X1              , "dup_x1"          , "b"    );
        def(DUP_X2              , "dup_x2"          , "b"    );
        def(DUP2                , "dup2"            , "b"    );
        def(DUP2_X1             , "dup2_x1"         , "b"    );
        def(DUP2_X2             , "dup2_x2"         , "b"    );
        def(SWAP                , "swap"            , "b"    );
        def(IADD                , "iadd"            , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(LADD                , "ladd"            , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(FADD                , "fadd"            , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(DADD                , "dadd"            , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(ISUB                , "isub"            , "b"    );
        def(LSUB                , "lsub"            , "b"    );
        def(FSUB                , "fsub"            , "b"    );
        def(DSUB                , "dsub"            , "b"    );
        def(IMUL                , "imul"            , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(LMUL                , "lmul"            , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(FMUL                , "fmul"            , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(DMUL                , "dmul"            , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(IDIV                , "idiv"            , "b"    , TRAP);
        def(LDIV                , "ldiv"            , "b"    , TRAP);
        def(FDIV                , "fdiv"            , "b"    );
        def(DDIV                , "ddiv"            , "b"    );
        def(IREM                , "irem"            , "b"    , TRAP);
        def(LREM                , "lrem"            , "b"    , TRAP);
        def(FREM                , "frem"            , "b"    );
        def(DREM                , "drem"            , "b"    );
        def(INEG                , "ineg"            , "b"    );
        def(LNEG                , "lneg"            , "b"    );
        def(FNEG                , "fneg"            , "b"    );
        def(DNEG                , "dneg"            , "b"    );
        def(ISHL                , "ishl"            , "b"    );
        def(LSHL                , "lshl"            , "b"    );
        def(ISHR                , "ishr"            , "b"    );
        def(LSHR                , "lshr"            , "b"    );
        def(IUSHR               , "iushr"           , "b"    );
        def(LUSHR               , "lushr"           , "b"    );
        def(IAND                , "iand"            , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(LAND                , "land"            , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(IOR                 , "ior"             , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(LOR                 , "lor"             , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(IXOR                , "ixor"            , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(LXOR                , "lxor"            , "b"    , COMMUTATIVE | ASSOCIATIVE);
        def(IINC                , "iinc"            , "bic"  , LOAD | STORE);
        def(I2L                 , "i2l"             , "b"    );
        def(I2F                 , "i2f"             , "b"    );
        def(I2D                 , "i2d"             , "b"    );
        def(L2I                 , "l2i"             , "b"    );
        def(L2F                 , "l2f"             , "b"    );
        def(L2D                 , "l2d"             , "b"    );
        def(F2I                 , "f2i"             , "b"    );
        def(F2L                 , "f2l"             , "b"    );
        def(F2D                 , "f2d"             , "b"    );
        def(D2I                 , "d2i"             , "b"    );
        def(D2L                 , "d2l"             , "b"    );
        def(D2F                 , "d2f"             , "b"    );
        def(I2B                 , "i2b"             , "b"    );
        def(I2C                 , "i2c"             , "b"    );
        def(I2S                 , "i2s"             , "b"    );
        def(LCMP                , "lcmp"            , "b"    );
        def(FCMPL               , "fcmpl"           , "b"    );
        def(FCMPG               , "fcmpg"           , "b"    );
        def(DCMPL               , "dcmpl"           , "b"    );
        def(DCMPG               , "dcmpg"           , "b"    );
        def(IFEQ                , "ifeq"            , "boo"  , FALL_THROUGH | BRANCH);
        def(IFNE                , "ifne"            , "boo"  , FALL_THROUGH | BRANCH);
        def(IFLT                , "iflt"            , "boo"  , FALL_THROUGH | BRANCH);
        def(IFGE                , "ifge"            , "boo"  , FALL_THROUGH | BRANCH);
        def(IFGT                , "ifgt"            , "boo"  , FALL_THROUGH | BRANCH);
        def(IFLE                , "ifle"            , "boo"  , FALL_THROUGH | BRANCH);
        def(IF_ICMPEQ           , "if_icmpeq"       , "boo"  , COMMUTATIVE | FALL_THROUGH | BRANCH);
        def(IF_ICMPNE           , "if_icmpne"       , "boo"  , COMMUTATIVE | FALL_THROUGH | BRANCH);
        def(IF_ICMPLT           , "if_icmplt"       , "boo"  , FALL_THROUGH | BRANCH);
        def(IF_ICMPGE           , "if_icmpge"       , "boo"  , FALL_THROUGH | BRANCH);
        def(IF_ICMPGT           , "if_icmpgt"       , "boo"  , FALL_THROUGH | BRANCH);
        def(IF_ICMPLE           , "if_icmple"       , "boo"  , FALL_THROUGH | BRANCH);
        def(IF_ACMPEQ           , "if_acmpeq"       , "boo"  , COMMUTATIVE | FALL_THROUGH | BRANCH);
        def(IF_ACMPNE           , "if_acmpne"       , "boo"  , COMMUTATIVE | FALL_THROUGH | BRANCH);
        def(GOTO                , "goto"            , "boo"  , STOP | BRANCH);
        def(JSR                 , "jsr"             , "boo"  , STOP | BRANCH);
        def(RET                 , "ret"             , "bi"   , STOP);
        def(TABLESWITCH         , "tableswitch"     , ""     , STOP);
        def(LOOKUPSWITCH        , "lookupswitch"    , ""     , STOP);
        def(IRETURN             , "ireturn"         , "b"    , TRAP | STOP);
        def(LRETURN             , "lreturn"         , "b"    , TRAP | STOP);
        def(FRETURN             , "freturn"         , "b"    , TRAP | STOP);
        def(DRETURN             , "dreturn"         , "b"    , TRAP | STOP);
        def(ARETURN             , "areturn"         , "b"    , TRAP | STOP);
        def(RETURN              , "return"          , "b"    , TRAP | STOP);
        def(GETSTATIC           , "getstatic"       , "bjj"  , TRAP | FIELD_READ);
        def(PUTSTATIC           , "putstatic"       , "bjj"  , TRAP | FIELD_WRITE);
        def(GETFIELD            , "getfield"        , "bjj"  , TRAP | FIELD_READ);
        def(PUTFIELD            , "putfield"        , "bjj"  , TRAP | FIELD_WRITE);
        def(INVOKEVIRTUAL       , "invokevirtual"   , "bjj"  , TRAP);
        def(INVOKESPECIAL       , "invokespecial"   , "bjj"  , TRAP);
        def(INVOKESTATIC        , "invokestatic"    , "bjj"  , TRAP);
        def(INVOKEINTERFACE     , "invokeinterface" , "bjja_", TRAP);
        def(XXXUNUSEDXXX        , "xxxunusedxxx"    , ""     );
        def(NEW                 , "new"             , "bii"  , TRAP);
        def(NEWARRAY            , "newarray"        , "bc"   , TRAP);
        def(ANEWARRAY           , "anewarray"       , "bii"  , TRAP);
        def(ARRAYLENGTH         , "arraylength"     , "b"    , TRAP);
        def(ATHROW              , "athrow"          , "b"    , TRAP | STOP);
        def(CHECKCAST           , "checkcast"       , "bii"  , TRAP);
        def(INSTANCEOF          , "instanceof"      , "bii"  , TRAP);
        def(MONITORENTER        , "monitorenter"    , "b"    , TRAP);
        def(MONITOREXIT         , "monitorexit"     , "b"    , TRAP);
        def(WIDE                , "wide"            , ""     );
        def(MULTIANEWARRAY      , "multianewarray"  , "biic" , TRAP);
        def(IFNULL              , "ifnull"          , "boo"  , FALL_THROUGH | BRANCH);
        def(IFNONNULL           , "ifnonnull"       , "boo"  , FALL_THROUGH | BRANCH);
        def(GOTO_W              , "goto_w"          , "boooo", STOP | BRANCH);
        def(JSR_W               , "jsr_w"           , "boooo", STOP | BRANCH);
        def(BREAKPOINT          , "breakpoint"      , "b"    , TRAP);
        
        
        def(WLOAD               , "wload"           , "bi"   , EXTENSION | LOAD);
        def(WLOAD_0             , "wload_0"         , "b"    , EXTENSION | LOAD);
        def(WLOAD_1             , "wload_1"         , "b"    , EXTENSION | LOAD);
        def(WLOAD_2             , "wload_2"         , "b"    , EXTENSION | LOAD);
        def(WLOAD_3             , "wload_3"         , "b"    , EXTENSION | LOAD);
        def(WSTORE              , "wstore"          , "bi"   , EXTENSION | STORE);
        def(WSTORE_0            , "wstore_0"        , "b"    , EXTENSION | STORE);
        def(WSTORE_1            , "wstore_1"        , "b"    , EXTENSION | STORE);
        def(WSTORE_2            , "wstore_2"        , "b"    , EXTENSION | STORE);
        def(WSTORE_3            , "wstore_3"        , "b"    , EXTENSION | STORE);
        def(WCONST_0            , "wconst_0"        , "bii"  , EXTENSION);
        def(WDIV                , "wdiv"            , "bii"  , EXTENSION | TRAP);
        def(WDIVI               , "wdivi"           , "bii"  , EXTENSION | TRAP);
        def(WREM                , "wrem"            , "bii"  , EXTENSION | TRAP);
        def(WREMI               , "wremi"           , "bii"  , EXTENSION | TRAP);
        def(ICMP                , "icmp"            , "bii"  , EXTENSION);
        def(WCMP                , "wcmp"            , "bii"  , EXTENSION);
        def(PREAD               , "pread"           , "bii"  , EXTENSION | TRAP);
        def(PWRITE              , "pwrite"          , "bii"  , EXTENSION | TRAP);
        def(PGET                , "pget"            , "bii"  , EXTENSION | TRAP);
        def(PSET                , "pset"            , "bii"  , EXTENSION | TRAP);
        def(PCMPSWP             , "pcmpswp"         , "bii"  , EXTENSION | TRAP);
        def(MOV_I2F             , "mov_i2f"         , "bii"  , EXTENSION | TRAP);
        def(MOV_F2I             , "mov_f2i"         , "bii"  , EXTENSION | TRAP);
        def(MOV_L2D             , "mov_l2d"         , "bii"  , EXTENSION | TRAP);
        def(MOV_D2L             , "mov_d2l"         , "bii"  , EXTENSION | TRAP);
        def(UCMP                , "ucmp"            , "bii"  , EXTENSION);
        def(UWCMP               , "uwcmp"           , "bii"  , EXTENSION);
        def(JNICALL             , "jnicall"         , "bii"  , EXTENSION | TRAP);
        def(JNIOP               , "jniop"           , "bii"  , EXTENSION);
        def(TEMPLATE_CALL       , "template_call"   , "bii"  , EXTENSION | TRAP);
        def(READREG             , "readreg"         , "bii"  , EXTENSION);
        def(WRITEREG            , "writereg"        , "bii"  , EXTENSION);
        def(INCREG              , "increg"          , "bii"  , EXTENSION);
        def(UNSAFE_CAST         , "unsafe_cast"     , "bii"  , EXTENSION);
        def(WRETURN             , "wreturn"         , "b"    , EXTENSION | TRAP | STOP);
        def(ALLOCA              , "alloca"          , "bii"  , EXTENSION);
        def(MEMBAR              , "membar"          , "bii"  , EXTENSION);
        def(STACKHANDLE         , "stackhandle"     , "bii"  , EXTENSION);
        def(PAUSE               , "pause"           , "bii"  , EXTENSION);
        def(BREAKPOINT_TRAP     , "breakpoint_trap" , "bii"  , EXTENSION);
        def(ADD_SP              , "add_sp"          , "bii"  , EXTENSION);
        def(INFOPOINT           , "infopoint"       , "bii"  , EXTENSION | TRAP);
        def(FLUSHW              , "flushw"          , "bii"  , EXTENSION);
        def(LSB                 , "lsb"             , "bii"  , EXTENSION);
        def(MSB                 , "msb"             , "bii"  , EXTENSION);
        
        def(JNIOP_J2N           , "jniop_j2n"         );
        def(JNIOP_LINK          , "jniop_link"        );
        def(JNIOP_N2J           , "jniop_n2j"         );
        
        def(SAFEPOINT           , "safepoint"         );
        def(HERE                , "here"              );
        def(INFO                , "info"              );
        
        def(MEMBAR_LOAD_LOAD    , "membar_load_load"  );
        def(MEMBAR_LOAD_STORE   , "membar_load_store" );
        def(MEMBAR_STORE_LOAD   , "membar_store_load" );
        def(MEMBAR_STORE_STORE  , "membar_store_store");
        
        def(PCMPSWP_INT         , "pcmpswp_int"       );
        def(PCMPSWP_INT_I       , "pcmpswp_int_i"     );
        def(PCMPSWP_LONG        , "pcmpswp_long"      );
        def(PCMPSWP_LONG_I      , "pcmpswp_long_i"    );
        def(PCMPSWP_REFERENCE   , "pcmpswp_reference" );
        def(PCMPSWP_REFERENCE_I , "pcmpswp_reference_i");
        def(PCMPSWP_WORD        , "pcmpswp_word"      );
        def(PCMPSWP_WORD_I      , "pcmpswp_word_i"    );
        
        def(PGET_BYTE           , "pget_byte"         );
        def(PGET_CHAR           , "pget_char"         );
        def(PGET_SHORT          , "pget_short"        );
        def(PGET_INT            , "pget_int"          );
        def(PGET_FLOAT          , "pget_float"        );
        def(PGET_LONG           , "pget_long"         );
        def(PGET_DOUBLE         , "pget_double"       );
        def(PGET_REFERENCE      , "pget_reference"    );
        def(PGET_WORD           , "pget_word"         );
        
        def(PREAD_BYTE          , "pread_byte"        );
        def(PREAD_BYTE_I        , "pread_byte_i"      );
        def(PREAD_CHAR          , "pread_char"        );
        def(PREAD_CHAR_I        , "pread_char_i"      );
        def(PREAD_SHORT         , "pread_short"       );
        def(PREAD_SHORT_I       , "pread_short_i"     );
        def(PREAD_INT           , "pread_int"         );
        def(PREAD_INT_I         , "pread_int_i"       );
        def(PREAD_FLOAT         , "pread_float"       );
        def(PREAD_FLOAT_I       , "pread_float_i"     );
        def(PREAD_LONG          , "pread_long"        );
        def(PREAD_LONG_I        , "pread_long_i"      );
        def(PREAD_DOUBLE        , "pread_double"      );
        def(PREAD_DOUBLE_I      , "pread_double_i"    );
        def(PREAD_REFERENCE     , "pread_reference"   );
        def(PREAD_REFERENCE_I   , "pread_reference_i" );
        def(PREAD_WORD          , "pread_word"        );
        def(PREAD_WORD_I        , "pread_word_i"      );
        
        def(PSET_BYTE           , "pset_byte"         );
        def(PSET_SHORT          , "pset_short"        );
        def(PSET_INT            , "pset_int"          );
        def(PSET_FLOAT          , "pset_float"        );
        def(PSET_LONG           , "pset_long"         );
        def(PSET_DOUBLE         , "pset_double"       );
        def(PSET_REFERENCE      , "pset_reference"    );
        def(PSET_WORD           , "pset_word"         );
        
        def(PWRITE_BYTE         , "pwrite_byte"       );
        def(PWRITE_BYTE_I       , "pwrite_byte_i"     );
        def(PWRITE_SHORT        , "pwrite_short"      );
        def(PWRITE_SHORT_I      , "pwrite_short_i"    );
        def(PWRITE_INT          , "pwrite_int"        );
        def(PWRITE_INT_I        , "pwrite_int_i"      );
        def(PWRITE_FLOAT        , "pwrite_float"      );
        def(PWRITE_FLOAT_I      , "pwrite_float_i"    );
        def(PWRITE_LONG         , "pwrite_long"       );
        def(PWRITE_LONG_I       , "pwrite_long_i"     );
        def(PWRITE_DOUBLE       , "pwrite_double"     );
        def(PWRITE_DOUBLE_I     , "pwrite_double_i"   );
        def(PWRITE_REFERENCE    , "pwrite_reference"  );
        def(PWRITE_REFERENCE_I  , "pwrite_reference_i");
        def(PWRITE_WORD         , "pwrite_word"       );
        def(PWRITE_WORD_I       , "pwrite_word_i"     );        
    }
    // Checkstyle: resume

    /**
     * Determines if an opcode is commutative.
     * @param opcode the opcode to check
     * @return {@code true} iff commutative
     */
    public static boolean isCommutative(int opcode) {
        return (flags[opcode & 0xff] & COMMUTATIVE) != 0;
    }

    /**
     * Gets the length of an instruction denoted by a given opcode.
     *
     * @param opcode an instruction opcode
     * @return the length of the instruction denoted by {@code opcode}. If {@code opcode} is an illegal instruction or denotes a
     *         variable length instruction (e.g. {@link #TABLESWITCH}), then 0 is returned.
     */
    public static int lengthOf(int opcode) {
        return length[opcode & 0xff];
    }

    /**
     * Gets the length of an instruction at a given position in a given bytecode array.
     * This methods handles variable length and {@linkplain #WIDE widened} instructions.
     *
     * @param code an array of bytecode
     * @param bci the position in {@code code} of an instruction's opcode
     * @return the length of the instruction at position {@code bci} in {@code code}
     */
    public static int lengthOf(byte[] code, int bci) {
        int opcode = Bytes.beU1(code, bci);
        int length = Bytecodes.length[opcode & 0xff];
        if (length == 0) {
            switch (opcode) {
                case TABLESWITCH: {
                    return new BytecodeTableSwitch(code, bci).size();
                }
                case LOOKUPSWITCH: {
                    return new BytecodeLookupSwitch(code, bci).size();
                }
                case WIDE: {
                    int opc = Bytes.beU1(code, bci + 1);
                    if (opc == RET) {
                        return 4;
                    } else if (opc == IINC) {
                        return 6;
                    } else {
                        return 4; // a load or store bytecode
                    }
                }
                default:
                    throw new Error("unknown variable-length bytecode: " + opcode);
            }
        }
        return length;
    }

    /**
     * Gets the lower-case mnemonic for a given opcode.
     *
     * @param opcode an opcode
     * @return the mnemonic for {@code opcode} or {@code "<illegal opcode: " + opcode + ">"} if {@code opcode} is not a legal opcode
     */
    public static String nameOf(int opcode) throws IllegalArgumentException {
        String extName = threeByteExtNames.get(Integer.valueOf(opcode));
        if (extName != null) {
            return extName;
        }
        String name = names[opcode & 0xff];
        if (name == null) {
            return "<illegal opcode: " + opcode + ">";
        }
        return name;
    }
    
    /**
     * Allocation-free version of {@linkplain #nameOf(int)}.
     * @param opcode an opcode.
     * @return the mnemonic for {@code opcode} or {@code "<illegal opcode>"} if {@code opcode} is not a legal opcode.
     */
    public static String baseNameOf(int opcode) {
    	String name = names[opcode & 0xff];
    	if (name == null) {
    		return "<illegal opcode>"; 
    	}
    	return name;
    }

    /**
     * Gets the opcode corresponding to a given mnemonic.
     *
     * @param name an opcode mnemonic
     * @return the opcode corresponding to {@code mnemonic}
     * @throws IllegalArgumentException if {@code name} does not denote a valid opcode
     */
    public static int valueOf(String name) {
        for (int opcode = 0; opcode < names.length; ++opcode) {
            if (name.equalsIgnoreCase(names[opcode])) {
                return opcode;
            }
        }
        for (Map.Entry<Integer, String> entry : threeByteExtNames.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("No opcode for " + name);
    }

    /**
     * Determines if a given opcode denotes an instruction that can cause an implicit exception.
     *
     * @param opcode an opcode to test
     * @return {@code true} iff {@code opcode} can cause an implicit exception, {@code false} otherwise
     */
    public static boolean canTrap(int opcode) {
        return (flags[opcode & 0xff] & TRAP) != 0;
    }

    /**
     * Determines if a given opcode denotes an instruction that loads a local variable to the operand stack.
     *
     * @param opcode an opcode to test
     * @return {@code true} iff {@code opcode} loads a local variable to the operand stack, {@code false} otherwise
     */
    public static boolean isLoad(int opcode) {
        return (flags[opcode & 0xff] & LOAD) != 0;
    }

    /**
     * Determines if a given opcode denotes an instruction that ends a basic block and does not let control flow fall
     * through to its lexical successor.
     *
     * @param opcode an opcode to test
     * @return {@code true} iff {@code opcode} properly ends a basic block
     */
    public static boolean isStop(int opcode) {
        return (flags[opcode & 0xff] & STOP) != 0;
    }

    /**
     * Determines if a given opcode denotes an instruction that stores a value to a local variable
     * after popping it from the operand stack.
     *
     * @param opcode an opcode to test
     * @return {@code true} iff {@code opcode} stores a value to a local variable, {@code false} otherwise
     */
    public static boolean isStore(int opcode) {
        return (flags[opcode & 0xff] & STORE) != 0;
    }

    /**
     * Determines if a given opcode is an instruction that delimits a basic block.
     *
     * @param opcode an opcode to test
     * @return {@code true} iff {@code opcode} delimits a basic block
     */
    public static boolean isBlockEnd(int opcode) {
        return (flags[opcode & 0xff] & (STOP | FALL_THROUGH)) != 0;
    }

    /**
     * Determines if a given opcode is an instruction that has a 2 or 4 byte operand that is an offset to another
     * instruction in the same method. This does not include the {@linkplain #TABLESWITCH switch} instructions.
     *
     * @param opcode an opcode to test
     * @return {@code true} iff {@code opcode} is a branch instruction with a single operand
     */
    public static boolean isBranch(int opcode) {
        return (flags[opcode & 0xff] & BRANCH) != 0;
    }

    /**
     * Determines if a given opcode denotes a conditional branch.
     * @param opcode
     * @return {@code true} iff {@code opcode} is a conditional branch
     */
    public static boolean isConditionalBranch(int opcode) {
        return (flags[opcode & 0xff] & FALL_THROUGH) != 0;
    }

    /**
     * Determines if a given opcode denotes a standard bytecode. A standard bytecode is
     * defined in the JVM specification.
     *
     * @param opcode an opcode to test
     * @return {@code true} iff {@code opcode} is a standard bytecode
     */
    public static boolean isStandard(int opcode) {
        return (flags[opcode & 0xff] & EXTENSION) == 0;
    }

    /**
     * Determines if a given opcode denotes an extended bytecode.
     *
     * @param opcode an opcode to test
     * @return {@code true} if {@code opcode} is an extended bytecode
     */
    public static boolean isExtended(int opcode) {
    	return (flags[opcode & 0xff] & EXTENSION) != 0;
    }

    /**
     * Determines if a given opcode is a three-byte extended bytecode.
     *
     * @param opcode an opcode to test
     * @return {@code true} if {@code (opcode & ~0xff) != 0}
     */
    public static boolean isThreeByteExtended(int opcode) {
        return (opcode & ~0xff) != 0;
    }

    /**
     * Gets the arithmetic operator name for a given opcode. If {@code opcode} does not denote an
     * arithmetic instruction, then the {@linkplain #nameOf(int) name} of the opcode is returned
     * instead.
     *
     * @param op an opcode
     * @return the arithmetic operator name
     */
    public static String operator(int op) {
        switch (op) {
            // arithmetic ops
            case IADD : // fall through
            case LADD : // fall through
            case FADD : // fall through
            case DADD : return "+";
            case ISUB : // fall through
            case LSUB : // fall through
            case FSUB : // fall through
            case DSUB : return "-";
            case IMUL : // fall through
            case LMUL : // fall through
            case FMUL : // fall through
            case DMUL : return "*";
            case IDIV : // fall through
            case LDIV : // fall through
            case FDIV : // fall through
            case DDIV : return "/";
            case IREM : // fall through
            case LREM : // fall through
            case FREM : // fall through
            case DREM : return "%";
            // shift ops
            case ISHL : // fall through
            case LSHL : return "<<";
            case ISHR : // fall through
            case LSHR : return ">>";
            case IUSHR: // fall through
            case LUSHR: return ">>>";
            // logic ops
            case IAND : // fall through
            case LAND : return "&";
            case IOR  : // fall through
            case LOR  : return "|";
            case IXOR : // fall through
            case LXOR : return "^";
        }
        return nameOf(op);
    }

    /**
     * Inserts machine code to generate a breakpoint trap.
     */
    @INTRINSIC(BREAKPOINT_TRAP)
    public static native void breakpointTrap();
    
    /**
     * Attempts to fold a binary operation on two constant integer inputs.
     *
     * @param opcode the bytecode operation to perform
     * @param x the first input
     * @param y the second input
     * @return a {@code Integer} instance representing the result of folding the operation,
     * if it is foldable, {@code null} otherwise
     */
    public static Integer foldIntOp2(int opcode, int x, int y) {
        // attempt to fold a binary operation with constant inputs
        switch (opcode) {
            case IADD: return x + y;
            case ISUB: return x - y;
            case IMUL: return x * y;
            case IDIV: {
                if (y == 0) {
                    return null;
                }
                return x / y;
            }
            case IREM: {
                if (y == 0) {
                    return null;
                }
                return x % y;
            }
            case IAND: return x & y;
            case IOR:  return x | y;
            case IXOR: return x ^ y;
            case ISHL: return x << y;
            case ISHR: return x >> y;
            case IUSHR: return x >>> y;
        }
        return null;
    }

    /**
     * Attempts to fold a binary operation on two constant long inputs.
     *
     * @param opcode the bytecode operation to perform
     * @param x the first input
     * @param y the second input
     * @return a {@code Long} instance representing the result of folding the operation,
     * if it is foldable, {@code null} otherwise
     */
    public static Long foldLongOp2(int opcode, long x, long y) {
        // attempt to fold a binary operation with constant inputs
        switch (opcode) {
            case LADD: return x + y;
            case LSUB: return x - y;
            case LMUL: return x * y;
            case LDIV: {
                if (y == 0) {
                    return null;
                }
                return x / y;
            }
            case LREM: {
                if (y == 0) {
                    return null;
                }
                return x % y;
            }
            case LAND: return x & y;
            case LOR:  return x | y;
            case LXOR: return x ^ y;
            case LSHL: return x << y;
            case LSHR: return x >> y;
            case LUSHR: return x >>> y;
        }
        return null;
    }

    /**
     * Attempts to fold a binary operation on two constant {@code float} inputs.
     *
     * @param opcode the bytecode operation to perform
     * @param x the first input
     * @param y the second input
     * @return a {@code Float} instance representing the result of folding the operation,
     * if it is foldable, {@code null} otherwise
     */
    public static strictfp Float foldFloatOp2(int opcode, float x, float y) {
        switch (opcode) {
            case FADD: return x + y;
            case FSUB: return x - y;
            case FMUL: return x * y;
            case FDIV: return x / y;
            case FREM: return x % y;
        }
        return null;
    }

    /**
     * Attempts to fold a binary operation on two constant {@code double} inputs.
     *
     * @param opcode the bytecode operation to perform
     * @param x the first input
     * @param y the second input
     * @return a {@code Double} instance representing the result of folding the operation,
     * if it is foldable, {@code null} otherwise
     */
    public static strictfp Double foldDoubleOp2(int opcode, double x, double y) {
        switch (opcode) {
            case DADD: return x + y;
            case DSUB: return x - y;
            case DMUL: return x * y;
            case DDIV: return x / y;
            case DREM: return x % y;
        }
        return null;
    }

    /**
     * Attempts to fold a comparison operation on two constant {@code long} inputs.
     *
     * @param x the first input
     * @param y the second input
     * @return an {@code int}  representing the result of the compare
     */
    public static int foldLongCompare(long x, long y) {
        if (x < y) {
            return -1;
        }
        if (x == y) {
            return 0;
        }
        return 1;
    }

    /**
     * Attempts to fold a comparison operation on two constant {@code float} inputs.
     *
     * @param opcode the bytecode operation to perform
     * @param x the first input
     * @param y the second input
     * @return an {@code Integer}  instance representing the result of the compare,
     * if it is foldable, {@code null} otherwise
     */
    public static Integer foldFloatCompare(int opcode, float x, float y) {
        // unfortunately we cannot write Java source to generate FCMPL or FCMPG
        int result = 0;
        if (x < y) {
            result = -1;
        } else if (x > y) {
            result = 1;
        }
        if (opcode == FCMPL) {
            if (Float.isNaN(x) || Float.isNaN(y)) {
                return -1;
            }
            return result;
        } else if (opcode == FCMPG) {
            if (Float.isNaN(x) || Float.isNaN(y)) {
                return 1;
            }
            return result;
        }
        return null; // unknown compare opcode
    }

    /**
     * Attempts to fold a comparison operation on two constant {@code double} inputs.
     *
     * @param opcode the bytecode operation to perform
     * @param x the first input
     * @param y the second input
     * @return an {@code Integer}  instance representing the result of the compare,
     * if it is foldable, {@code null} otherwise
     */
    public static Integer foldDoubleCompare(int opcode, double x, double y) {
        // unfortunately we cannot write Java source to generate DCMPL or DCMPG
        int result = 0;
        if (x < y) {
            result = -1;
        } else if (x > y) {
            result = 1;
        }
        if (opcode == DCMPL) {
            if (Double.isNaN(x) || Double.isNaN(y)) {
                return -1;
            }
            return result;
        } else if (opcode == DCMPG) {
            if (Double.isNaN(x) || Double.isNaN(y)) {
                return 1;
            }
            return result;
        }
        return null; // unknown compare opcode
    }

    /**
     * Defines a bytecode by entering it into the arrays that record its name, length and flags.
     * 
     * @param name instruction name (should be lower case)
     * @param format encodes the length of the instruction
     * @param flags the set of {@link Flags} associated with the instruction
     */
    private static void def(int opcode, String name, String format) {
        def(opcode, name, format, 0);
    }

    /**
     * Defines a bytecode by entering it into the arrays that record its name, length and flags.
     * 
     * @param name instruction name (lower case)
     * @param format encodes the length of the instruction
     * @param flags the set of {@link Flags} associated with the instruction
     */
    private static void def(int opcode, String name, String format, int flags) {
        assert names[opcode] == null : "opcode " + opcode + " is already bound to name " + names[opcode];
        names[opcode] = name;
        int instructionLength = format.length();
        length[opcode] = instructionLength;
        Bytecodes.flags[opcode] = flags;

        assert !isConditionalBranch(opcode) || isBranch(opcode) : "a conditional branch must also be a branch";
    }

    /**
     * Defines a three-byte extended bytecode by entering it into the maps that records its name.
     */
    private static void def(int opcode, String name) {
         assert isExtended(opcode);
         String oldValue = threeByteExtNames.put(opcode, name);
         assert oldValue == null;
    }

    /**
     * Utility for ensuring that the extended opcodes are contiguous and follow on directly
     * from the standard JVM opcodes. If these conditions do not hold for the input source
     * file, then it is modified 'in situ' to fix the problem.
     *
     * @param args {@code args[0]} is the path to this source file
     */
    public static void main(String[] args) throws Exception {
        Pattern opcodeDecl = Pattern.compile("(\\s*public static final int )(\\w+)(\\s*=\\s*)(\\d+)(;.*)");

        File file = new File(args[0]);
        BufferedReader br = new BufferedReader(new FileReader(file));
        CharArrayWriter buffer = new CharArrayWriter((int) file.length());
        PrintWriter out = new PrintWriter(buffer);
        String line;
        int lastExtendedOpcode = BREAKPOINT;
        boolean modified = false;
        int section = 0;
        while ((line = br.readLine()) != null) {
            if (section == 0) {
                if (line.equals("    // Start extended bytecodes")) {
                    section = 1;
                }
            } else if (section == 1) {
                if (line.equals("    // End extended bytecodes")) {
                    section = 2;
                } else {
                    Matcher matcher = opcodeDecl.matcher(line);
                    if (matcher.matches()) {
                        String name = matcher.group(2);
                        String value = matcher.group(4);
                        int opcode = Integer.parseInt(value);
                        if (names[opcode] == null || !names[opcode].equalsIgnoreCase(name)) {
                            throw new RuntimeException("Missing definition of name and flags for " + opcode + ":" + name + " -- " + names[opcode]);
                        }
                        if (opcode != lastExtendedOpcode + 1) {
                            System.err.println("Fixed declaration of opcode " + name + " to be " + (lastExtendedOpcode + 1) + " (was " + value + ")");
                            opcode = lastExtendedOpcode + 1;
                            line = line.substring(0, matcher.start(4)) + opcode + line.substring(matcher.end(4));
                            modified = true;
                        }

                        if (opcode >= 256) {
                            throw new RuntimeException("Exceeded maximum opcode value with " + name);
                        }

                        lastExtendedOpcode = opcode;
                    }
                }
            }

            out.println(line);
        }
        if (section == 0) {
            throw new RuntimeException("Did not find line starting extended bytecode declarations:\n\n    // Start extended bytecodes");
        } else if (section == 1) {
            throw new RuntimeException("Did not find line ending extended bytecode declarations:\n\n    // End extended bytecodes");
        }

        if (modified) {
            out.flush();
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(buffer.toCharArray());
            fileWriter.close();

            System.out.println("Modified: " + file);
        }


        // Uncomment to print out visitor method declarations:
//        for (int opcode = 0; opcode < flags.length; ++opcode) {
//            if (isExtension(opcode)) {
//                String visitorParams = length(opcode) == 1 ? "" : "int index";
//                System.out.println("@Override");
//                System.out.println("protected void " + name(opcode) + "(" + visitorParams + ") {");
//                System.out.println("}");
//                System.out.println();
//            }
//        }

        // Uncomment to print out visitor method declarations:
//        for (int opcode = 0; opcode < flags.length; ++opcode) {
//            if (isExtension(opcode)) {
//                System.out.println("case " + name(opcode).toUpperCase() + ": {");
//                String arg = "";
//                int length = length(opcode);
//                if (length == 2) {
//                    arg = "readUnsigned1()";
//                } else if (length == 3) {
//                    arg = "readUnsigned2()";
//                }
//                System.out.println("    bytecodeVisitor." + name(opcode) + "(" + arg + ");");
//                System.out.println("    break;");
//                System.out.println("}");
//            }
//        }

    }
}
