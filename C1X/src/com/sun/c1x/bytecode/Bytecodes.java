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
package com.sun.c1x.bytecode;

import com.sun.c1x.util.Bytes;

/**
 * The <code>Bytecodes</code> class defines constants associated with bytecodes,
 * in particular the opcode numbers for each bytecode.
 *
 * @author Ben L. Titzer
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

    public static final int NUM_JAVA_CODES = 203;
    public static final int ILLEGAL = 255;
    public static final int END = 256;

    private static final int FLAG_TRAP = 1;
    private static final int FLAG_COMMUTATIVE = 2;
    private static final int FLAG_ASSOCIATIVE = 4;
    private static final int FLAG_LOAD = 8;
    private static final int FLAG_STORE = 16;

    private static final String[] _names = new String[NUM_JAVA_CODES];
    private static final byte[] _flags = new byte[NUM_JAVA_CODES];
    private static final byte[] _length = new byte[NUM_JAVA_CODES];

    // Checkstyle: stop
    static {
        def(NOP                 , "nop"                 , "b"    );
        def(ACONST_NULL         , "aconst_null"         , "b"    );
        def(ICONST_M1           , "iconst_m1"           , "b"    );
        def(ICONST_0            , "iconst_0"            , "b"    );
        def(ICONST_1            , "iconst_1"            , "b"    );
        def(ICONST_2            , "iconst_2"            , "b"    );
        def(ICONST_3            , "iconst_3"            , "b"    );
        def(ICONST_4            , "iconst_4"            , "b"    );
        def(ICONST_5            , "iconst_5"            , "b"    );
        def(LCONST_0            , "lconst_0"            , "b"    );
        def(LCONST_1            , "lconst_1"            , "b"    );
        def(FCONST_0            , "fconst_0"            , "b"    );
        def(FCONST_1            , "fconst_1"            , "b"    );
        def(FCONST_2            , "fconst_2"            , "b"    );
        def(DCONST_0            , "dconst_0"            , "b"    );
        def(DCONST_1            , "dconst_1"            , "b"    );
        def(BIPUSH              , "bipush"              , "bc"   );
        def(SIPUSH              , "sipush"              , "bcc"  );
        def(LDC                 , "ldc"                 , "bi"   , FLAG_TRAP);
        def(LDC_W               , "ldc_w"               , "bii"  , FLAG_TRAP);
        def(LDC2_W              , "ldc2_w"              , "bii"  , FLAG_TRAP);
        def(ILOAD               , "iload"               , "bi"   , FLAG_LOAD);
        def(LLOAD               , "lload"               , "bi"   , FLAG_LOAD);
        def(FLOAD               , "fload"               , "bi"   , FLAG_LOAD);
        def(DLOAD               , "dload"               , "bi"   , FLAG_LOAD);
        def(ALOAD               , "aload"               , "bi"   , FLAG_LOAD);
        def(ILOAD_0             , "iload_0"             , "b"    , FLAG_LOAD);
        def(ILOAD_1             , "iload_1"             , "b"    , FLAG_LOAD);
        def(ILOAD_2             , "iload_2"             , "b"    , FLAG_LOAD);
        def(ILOAD_3             , "iload_3"             , "b"    , FLAG_LOAD);
        def(LLOAD_0             , "lload_0"             , "b"    , FLAG_LOAD);
        def(LLOAD_1             , "lload_1"             , "b"    , FLAG_LOAD);
        def(LLOAD_2             , "lload_2"             , "b"    , FLAG_LOAD);
        def(LLOAD_3             , "lload_3"             , "b"    , FLAG_LOAD);
        def(FLOAD_0             , "fload_0"             , "b"    , FLAG_LOAD);
        def(FLOAD_1             , "fload_1"             , "b"    , FLAG_LOAD);
        def(FLOAD_2             , "fload_2"             , "b"    , FLAG_LOAD);
        def(FLOAD_3             , "fload_3"             , "b"    , FLAG_LOAD);
        def(DLOAD_0             , "dload_0"             , "b"    , FLAG_LOAD);
        def(DLOAD_1             , "dload_1"             , "b"    , FLAG_LOAD);
        def(DLOAD_2             , "dload_2"             , "b"    , FLAG_LOAD);
        def(DLOAD_3             , "dload_3"             , "b"    , FLAG_LOAD);
        def(ALOAD_0             , "aload_0"             , "b"    , FLAG_LOAD);
        def(ALOAD_1             , "aload_1"             , "b"    , FLAG_LOAD);
        def(ALOAD_2             , "aload_2"             , "b"    , FLAG_LOAD);
        def(ALOAD_3             , "aload_3"             , "b"    , FLAG_LOAD);
        def(IALOAD              , "iaload"              , "b"    , FLAG_TRAP);
        def(LALOAD              , "laload"              , "b"    , FLAG_TRAP);
        def(FALOAD              , "faload"              , "b"    , FLAG_TRAP);
        def(DALOAD              , "daload"              , "b"    , FLAG_TRAP);
        def(AALOAD              , "aaload"              , "b"    , FLAG_TRAP);
        def(BALOAD              , "baload"              , "b"    , FLAG_TRAP);
        def(CALOAD              , "caload"              , "b"    , FLAG_TRAP);
        def(SALOAD              , "saload"              , "b"    , FLAG_TRAP);
        def(ISTORE              , "istore"              , "bi"   , FLAG_STORE);
        def(LSTORE              , "lstore"              , "bi"   , FLAG_STORE);
        def(FSTORE              , "fstore"              , "bi"   , FLAG_STORE);
        def(DSTORE              , "dstore"              , "bi"   , FLAG_STORE);
        def(ASTORE              , "astore"              , "bi"   , FLAG_STORE);
        def(ISTORE_0            , "istore_0"            , "b"    , FLAG_STORE);
        def(ISTORE_1            , "istore_1"            , "b"    , FLAG_STORE);
        def(ISTORE_2            , "istore_2"            , "b"    , FLAG_STORE);
        def(ISTORE_3            , "istore_3"            , "b"    , FLAG_STORE);
        def(LSTORE_0            , "lstore_0"            , "b"    , FLAG_STORE);
        def(LSTORE_1            , "lstore_1"            , "b"    , FLAG_STORE);
        def(LSTORE_2            , "lstore_2"            , "b"    , FLAG_STORE);
        def(LSTORE_3            , "lstore_3"            , "b"    , FLAG_STORE);
        def(FSTORE_0            , "fstore_0"            , "b"    , FLAG_STORE);
        def(FSTORE_1            , "fstore_1"            , "b"    , FLAG_STORE);
        def(FSTORE_2            , "fstore_2"            , "b"    , FLAG_STORE);
        def(FSTORE_3            , "fstore_3"            , "b"    , FLAG_STORE);
        def(DSTORE_0            , "dstore_0"            , "b"    , FLAG_STORE);
        def(DSTORE_1            , "dstore_1"            , "b"    , FLAG_STORE);
        def(DSTORE_2            , "dstore_2"            , "b"    , FLAG_STORE);
        def(DSTORE_3            , "dstore_3"            , "b"    , FLAG_STORE);
        def(ASTORE_0            , "astore_0"            , "b"    , FLAG_STORE);
        def(ASTORE_1            , "astore_1"            , "b"    , FLAG_STORE);
        def(ASTORE_2            , "astore_2"            , "b"    , FLAG_STORE);
        def(ASTORE_3            , "astore_3"            , "b"    , FLAG_STORE);
        def(IASTORE             , "iastore"             , "b"    , FLAG_TRAP);
        def(LASTORE             , "lastore"             , "b"    , FLAG_TRAP);
        def(FASTORE             , "fastore"             , "b"    , FLAG_TRAP);
        def(DASTORE             , "dastore"             , "b"    , FLAG_TRAP);
        def(AASTORE             , "aastore"             , "b"    , FLAG_TRAP);
        def(BASTORE             , "bastore"             , "b"    , FLAG_TRAP);
        def(CASTORE             , "castore"             , "b"    , FLAG_TRAP);
        def(SASTORE             , "sastore"             , "b"    , FLAG_TRAP);
        def(POP                 , "pop"                 , "b"    );
        def(POP2                , "pop2"                , "b"    );
        def(DUP                 , "dup"                 , "b"    );
        def(DUP_X1              , "dup_x1"              , "b"    );
        def(DUP_X2              , "dup_x2"              , "b"    );
        def(DUP2                , "dup2"                , "b"    );
        def(DUP2_X1             , "dup2_x1"             , "b"    );
        def(DUP2_X2             , "dup2_x2"             , "b"    );
        def(SWAP                , "swap"                , "b"    );
        def(IADD                , "iadd"                , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(LADD                , "ladd"                , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(FADD                , "fadd"                , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(DADD                , "dadd"                , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(ISUB                , "isub"                , "b"    );
        def(LSUB                , "lsub"                , "b"    );
        def(FSUB                , "fsub"                , "b"    );
        def(DSUB                , "dsub"                , "b"    );
        def(IMUL                , "imul"                , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(LMUL                , "lmul"                , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(FMUL                , "fmul"                , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(DMUL                , "dmul"                , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(IDIV                , "idiv"                , "b"    , FLAG_TRAP);
        def(LDIV                , "ldiv"                , "b"    , FLAG_TRAP);
        def(FDIV                , "fdiv"                , "b"    );
        def(DDIV                , "ddiv"                , "b"    );
        def(IREM                , "irem"                , "b"    , FLAG_TRAP);
        def(LREM                , "lrem"                , "b"    , FLAG_TRAP);
        def(FREM                , "frem"                , "b"    );
        def(DREM                , "drem"                , "b"    );
        def(INEG                , "ineg"                , "b"    );
        def(LNEG                , "lneg"                , "b"    );
        def(FNEG                , "fneg"                , "b"    );
        def(DNEG                , "dneg"                , "b"    );
        def(ISHL                , "ishl"                , "b"    );
        def(LSHL                , "lshl"                , "b"    );
        def(ISHR                , "ishr"                , "b"    );
        def(LSHR                , "lshr"                , "b"    );
        def(IUSHR               , "iushr"               , "b"    );
        def(LUSHR               , "lushr"               , "b"    );
        def(IAND                , "iand"                , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(LAND                , "land"                , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(IOR                 , "ior"                 , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(LOR                 , "lor"                 , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(IXOR                , "ixor"                , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(LXOR                , "lxor"                , "b"    , FLAG_COMMUTATIVE | FLAG_ASSOCIATIVE);
        def(IINC                , "iinc"                , "bic"  , FLAG_LOAD | FLAG_STORE);
        def(I2L                 , "i2l"                 , "b"    );
        def(I2F                 , "i2f"                 , "b"    );
        def(I2D                 , "i2d"                 , "b"    );
        def(L2I                 , "l2i"                 , "b"    );
        def(L2F                 , "l2f"                 , "b"    );
        def(L2D                 , "l2d"                 , "b"    );
        def(F2I                 , "f2i"                 , "b"    );
        def(F2L                 , "f2l"                 , "b"    );
        def(F2D                 , "f2d"                 , "b"    );
        def(D2I                 , "d2i"                 , "b"    );
        def(D2L                 , "d2l"                 , "b"    );
        def(D2F                 , "d2f"                 , "b"    );
        def(I2B                 , "i2b"                 , "b"    );
        def(I2C                 , "i2c"                 , "b"    );
        def(I2S                 , "i2s"                 , "b"    );
        def(LCMP                , "lcmp"                , "b"    );
        def(FCMPL               , "fcmpl"               , "b"    );
        def(FCMPG               , "fcmpg"               , "b"    );
        def(DCMPL               , "dcmpl"               , "b"    );
        def(DCMPG               , "dcmpg"               , "b"    );
        def(IFEQ                , "ifeq"                , "boo"  );
        def(IFNE                , "ifne"                , "boo"  );
        def(IFLT                , "iflt"                , "boo"  );
        def(IFGE                , "ifge"                , "boo"  );
        def(IFGT                , "ifgt"                , "boo"  );
        def(IFLE                , "ifle"                , "boo"  );
        def(IF_ICMPEQ           , "if_icmpeq"           , "boo"  , FLAG_COMMUTATIVE);
        def(IF_ICMPNE           , "if_icmpne"           , "boo"  , FLAG_COMMUTATIVE);
        def(IF_ICMPLT           , "if_icmplt"           , "boo"  );
        def(IF_ICMPGE           , "if_icmpge"           , "boo"  );
        def(IF_ICMPGT           , "if_icmpgt"           , "boo"  );
        def(IF_ICMPLE           , "if_icmple"           , "boo"  );
        def(IF_ACMPEQ           , "if_acmpeq"           , "boo"  , FLAG_COMMUTATIVE);
        def(IF_ACMPNE           , "if_acmpne"           , "boo"  , FLAG_COMMUTATIVE);
        def(GOTO                , "goto"                , "boo"  );
        def(JSR                 , "jsr"                 , "boo"  );
        def(RET                 , "ret"                 , "bi"   );
        def(TABLESWITCH         , "tableswitch"         , ""     );
        def(LOOKUPSWITCH        , "lookupswitch"        , ""     );
        def(IRETURN             , "ireturn"             , "b"    , FLAG_TRAP);
        def(LRETURN             , "lreturn"             , "b"    , FLAG_TRAP);
        def(FRETURN             , "freturn"             , "b"    , FLAG_TRAP);
        def(DRETURN             , "dreturn"             , "b"    , FLAG_TRAP);
        def(ARETURN             , "areturn"             , "b"    , FLAG_TRAP);
        def(RETURN              , "return"              , "b"    , FLAG_TRAP);
        def(GETSTATIC           , "getstatic"           , "bjj"  , FLAG_TRAP);
        def(PUTSTATIC           , "putstatic"           , "bjj"  , FLAG_TRAP);
        def(GETFIELD            , "getfield"            , "bjj"  , FLAG_TRAP);
        def(PUTFIELD            , "putfield"            , "bjj"  , FLAG_TRAP);
        def(INVOKEVIRTUAL       , "invokevirtual"       , "bjj"  , FLAG_TRAP);
        def(INVOKESPECIAL       , "invokespecial"       , "bjj"  , FLAG_TRAP);
        def(INVOKESTATIC        , "invokestatic"        , "bjj"  , FLAG_TRAP);
        def(INVOKEINTERFACE     , "invokeinterface"     , "bjj"  , FLAG_TRAP);
        def(XXXUNUSEDXXX        , "xxxunusedxxx"        , ""     );
        def(NEW                 , "new"                 , "bii"  , FLAG_TRAP);
        def(NEWARRAY            , "newarray"            , "bc"   , FLAG_TRAP);
        def(ANEWARRAY           , "anewarray"           , "bii"  , FLAG_TRAP);
        def(ARRAYLENGTH         , "arraylength"         , "b"    , FLAG_TRAP);
        def(ATHROW              , "athrow"              , "b"    , FLAG_TRAP);
        def(CHECKCAST           , "checkcast"           , "bii"  , FLAG_TRAP);
        def(INSTANCEOF          , "instanceof"          , "bii"  , FLAG_TRAP);
        def(MONITORENTER        , "monitorenter"        , "b"    , FLAG_TRAP);
        def(MONITOREXIT         , "monitorexit"         , "b"    , FLAG_TRAP);
        def(WIDE                , "wide"                , ""     );
        def(MULTIANEWARRAY      , "multianewarray"      , "biic" , FLAG_TRAP);
        def(IFNULL              , "ifnull"              , "boo"  );
        def(IFNONNULL           , "ifnonnull"           , "boo"  );
        def(GOTO_W              , "goto_w"              , "boooo");
        def(JSR_W               , "jsr_w"               , "boooo");
        def(BREAKPOINT          , "breakpoint"          , "b"    , FLAG_TRAP);
    }
    // Checkstyle: resume

    public static boolean isCommutative(int op) {
        return (_flags[op] & FLAG_COMMUTATIVE) != 0;
    }

    public static int length(int opcode) {
        return _length[opcode];
    }

    public static int length(byte[] code, int bci) {
        int opcode = Bytes.beU1(code, bci);
        int length = _length[opcode];
        if (length == 0) {
            switch (opcode) {
                case Bytecodes.TABLESWITCH: {
                    return new BytecodeTableSwitch(code, bci).size();
                }
                case Bytecodes.LOOKUPSWITCH: {
                    return new BytecodeLookupSwitch(code, bci).size();
                }
                case Bytecodes.WIDE: {
                    int opc = Bytes.beU1(code, bci + 1);
                    if (opc == Bytecodes.RET) {
                        return 4;
                    } else if (opc == Bytecodes.IINC) {
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

    public static String name(int opcode) {
        return _names[opcode];
    }

    public static boolean canTrap(int opcode) {
        return (_flags[opcode] & FLAG_TRAP) != 0;
    }

    public static boolean isLoad(int opcode) {
        return (_flags[opcode] & FLAG_LOAD) != 0;
    }

    public static boolean isStore(int opcode) {
        return (_flags[opcode] & FLAG_STORE) != 0;
    }


    /**
     * This method attempts to fold a binary operation on two constant integer inputs.
     *
     * @param opcode the bytecode operation to perform
     * @param x the first input
     * @param y the second input
     * @return a <code>Integer</code> instance representing the result of folding the operation,
     * if it is foldable, <code>null</code> otherwise
     */
    public static Integer foldIntOp2(int opcode, int x, int y) {
        // attempt to fold a binary operation with constant inputs
        switch (opcode) {
            case Bytecodes.IADD: return (x + y);
            case Bytecodes.ISUB: return (x - y);
            case Bytecodes.IMUL: return (x * y);
            case Bytecodes.IDIV: {
                if (y == 0) {
                    return null;
                }
                return (x / y);
            }
            case Bytecodes.IREM: {
                if (y == 0) {
                    return null;
                }
                return (x % y);
            }
            case Bytecodes.IAND: return (x & y);
            case Bytecodes.IOR:  return (x | y);
            case Bytecodes.IXOR: return (x ^ y);
            case Bytecodes.ISHL: return (x << y);
            case Bytecodes.ISHR: return (x >> y);
            case Bytecodes.IUSHR: return (x >>> y);
        }
        return null;
    }

    /**
     * This method attempts to fold a binary operation on two constant long inputs.
     *
     * @param opcode the bytecode operation to perform
     * @param x the first input
     * @param y the second input
     * @return a <code>Long</code> instance representing the result of folding the operation,
     * if it is foldable, <code>null</code> otherwise
     */
    public static Long foldLongOp2(int opcode, long x, long y) {
        // attempt to fold a binary operation with constant inputs
        switch (opcode) {
            case Bytecodes.LADD: return (x + y);
            case Bytecodes.LSUB: return (x - y);
            case Bytecodes.LMUL: return (x * y);
            case Bytecodes.LDIV: {
                if (y == 0) {
                    return null;
                }
                return (x / y);
            }
            case Bytecodes.LREM: {
                if (y == 0) {
                    return null;
                }
                return (x % y);
            }
            case Bytecodes.LAND: return (x & y);
            case Bytecodes.LOR:  return (x | y);
            case Bytecodes.LXOR: return (x ^ y);
            case Bytecodes.LSHL: return (x << y);
            case Bytecodes.LSHR: return (x >> y);
            case Bytecodes.LUSHR: return (x >>> y);
        }
        return null;
    }

    public static strictfp Float foldFloatOp2(int opcode, float x, float y) {
        switch (opcode) {
            case Bytecodes.FADD: return (x + y);
            case Bytecodes.FSUB: return (x - y);
            case Bytecodes.FMUL: return (x * y);
            case Bytecodes.FDIV: return (x / y);
            case Bytecodes.FREM: return (x % y);
        }
        return null;
    }

    public static strictfp Double foldDoubleOp2(int opcode, double x, double y) {
        switch (opcode) {
            case Bytecodes.DADD: return (x + y);
            case Bytecodes.DSUB: return (x - y);
            case Bytecodes.DMUL: return (x * y);
            case Bytecodes.DDIV: return (x / y);
            case Bytecodes.DREM: return (x % y);
        }
        return null;
    }

    public static int foldLongCompare(long x, long y) {
        if (x < y) return -1;
        if (x == y) return 0;
        return 1;
    }

    public static Integer foldFloatCompare(int opcode, float x, float y) {
        // unfortunately we cannot write Java source to generate FCMPL or FCMPG
        int result = 0;
        if (x < y) result = -1;
        else if (x > y) result = 1;
        if (opcode == FCMPL) {
            if (Float.isNaN(x) || Float.isNaN(y)) return -1;
            return result;
        } else if (opcode == FCMPG) {
            if (Float.isNaN(x) || Float.isNaN(y)) return 1;
            return result;
        }
        return null; // unknown compare opcode
    }

    public static Integer foldDoubleCompare(int opcode, double x, double y) {
        // unfortunately we cannot write Java source to generate DCMPL or DCMPG
        int result = 0;
        if (x < y) result = -1;
        else if (x > y) result = 1;
        if (opcode == DCMPL) {
            if (Double.isNaN(x) || Double.isNaN(y)) return -1;
            return result;
        } else if (opcode == DCMPG) {
            if (Double.isNaN(x) || Double.isNaN(y)) return 1;
            return result;
        }
        return null; // unknown compare opcode
    }

    private static void def(int opcode, String name, String format) {
        def(opcode, name, format, 0);
    }

    private static void def(int opcode, String name, String format, int flags) {
        _names[opcode] = name;
        _length[opcode] = (byte) format.length();
        _flags[opcode] = (byte) flags;
    }
}
