package com.sun.bce;

public class BytecodeExtender {


    public static class Frame {
        private final Type[] locals;
        private final Type[] stack;

        Frame(int maxStack, int maxLocals) {
            locals = new Type[maxLocals];
            stack = new Type[maxStack];
        }
    }

    public int[] extra;

    public static class StackMap {

    }

    public BytecodeExtender(byte[] code, StackMap stackMap) {

    }

    public void run(byte[] code) {


        byte[] out = code.clone();

        int bci = 0;
        while (bci != code.length) {
            int opcode = code[bci++];
            if (opcode == WIDE) {
                opcode = code[bci++];
            }

            switch (opcode) {
                case NOP            : /* nothing to do */ break;
                case ACONST_NULL    : /*apush(appendConstant(CiConstant.NULL_OBJECT))*/; break;
                case ICONST_M1      : /*/*ipush(appendConstant(CiConstant.INT_MINUS_1))*/; break;
                case ICONST_0       : /*ipush(appendConstant(CiConstant.INT_0))*/; break;
                case ICONST_1       : /*ipush(appendConstant(CiConstant.INT_1))*/; break;
                case ICONST_2       : /*ipush(appendConstant(CiConstant.INT_2))*/; break;
                case ICONST_3       : /*ipush(appendConstant(CiConstant.INT_3))*/; break;
                case ICONST_4       : /*ipush(appendConstant(CiConstant.INT_4))*/; break;
                case ICONST_5       : /*ipush(appendConstant(CiConstant.INT_5))*/; break;
                case LCONST_0       : /*lpush(appendConstant(CiConstant.LONG_0))*/; break;
                case LCONST_1       : /*lpush(appendConstant(CiConstant.LONG_1))*/; break;
                case FCONST_0       : /*fpush(appendConstant(CiConstant.FLOAT_0))*/; break;
                case FCONST_1       : /*fpush(appendConstant(CiConstant.FLOAT_1))*/; break;
                case FCONST_2       : /*fpush(appendConstant(CiConstant.FLOAT_2))*/; break;
                case DCONST_0       : /*dpush(appendConstant(CiConstant.DOUBLE_0))*/; break;
                case DCONST_1       : /*dpush(appendConstant(CiConstant.DOUBLE_1))*/; break;
                case BIPUSH         : /*ipush(appendConstant(CiConstant.forInt(s.readByte())))*/; break;
                case SIPUSH         : /*ipush(appendConstant(CiConstant.forInt(s.readShort())))*/; break;
                case LDC            : // fall through
                case LDC_W          : // fall through
                case LDC2_W         : /*genLoadConstant(stream().readCPI())*/; break;
                case ILOAD          : /*loadLocal(s.readLocalIndex(), CiKind.Int)*/; break;
                case LLOAD          : /*loadLocal(s.readLocalIndex(), CiKind.Long)*/; break;
                case FLOAD          : /*loadLocal(s.readLocalIndex(), CiKind.Float)*/; break;
                case DLOAD          : /*loadLocal(s.readLocalIndex(), CiKind.Double)*/; break;
                case ALOAD          : /*loadLocal(s.readLocalIndex(), CiKind.Object)*/; break;
                case ILOAD_0        : /*loadLocal(0, CiKind.Int)*/; break;
                case ILOAD_1        : /*loadLocal(1, CiKind.Int)*/; break;
                case ILOAD_2        : /*loadLocal(2, CiKind.Int)*/; break;
                case ILOAD_3        : /*loadLocal(3, CiKind.Int)*/; break;
                case LLOAD_0        : /*loadLocal(0, CiKind.Long)*/; break;
                case LLOAD_1        : /*loadLocal(1, CiKind.Long)*/; break;
                case LLOAD_2        : /*loadLocal(2, CiKind.Long)*/; break;
                case LLOAD_3        : /*loadLocal(3, CiKind.Long)*/; break;
                case FLOAD_0        : /*loadLocal(0, CiKind.Float)*/; break;
                case FLOAD_1        : /*loadLocal(1, CiKind.Float)*/; break;
                case FLOAD_2        : /*loadLocal(2, CiKind.Float)*/; break;
                case FLOAD_3        : /*loadLocal(3, CiKind.Float)*/; break;
                case DLOAD_0        : /*loadLocal(0, CiKind.Double)*/; break;
                case DLOAD_1        : /*loadLocal(1, CiKind.Double)*/; break;
                case DLOAD_2        : /*loadLocal(2, CiKind.Double)*/; break;
                case DLOAD_3        : /*loadLocal(3, CiKind.Double)*/; break;
                case ALOAD_0        : /*loadLocal(0, CiKind.Object)*/; break;
                case ALOAD_1        : /*loadLocal(1, CiKind.Object)*/; break;
                case ALOAD_2        : /*loadLocal(2, CiKind.Object)*/; break;
                case ALOAD_3        : /*loadLocal(3, CiKind.Object)*/; break;
                case IALOAD         : /*genLoadIndexed(CiKind.Int   )*/; break;
                case LALOAD         : /*genLoadIndexed(CiKind.Long  )*/; break;
                case FALOAD         : /*genLoadIndexed(CiKind.Float )*/; break;
                case DALOAD         : /*genLoadIndexed(CiKind.Double)*/; break;
                case AALOAD         : /*genLoadIndexed(CiKind.Object)*/; break;
                case BALOAD         : /*genLoadIndexed(CiKind.Byte  )*/; break;
                case CALOAD         : /*genLoadIndexed(CiKind.Char  )*/; break;
                case SALOAD         : /*genLoadIndexed(CiKind.Short )*/; break;
                case ISTORE         : /*storeLocal(CiKind.Int, s.readLocalIndex())*/; break;
                case LSTORE         : /*storeLocal(CiKind.Long, s.readLocalIndex())*/; break;
                case FSTORE         : /*storeLocal(CiKind.Float, s.readLocalIndex())*/; break;
                case DSTORE         : /*storeLocal(CiKind.Double, s.readLocalIndex())*/; break;
                case ASTORE         : /*storeLocal(CiKind.Object, s.readLocalIndex())*/; break;
                case ISTORE_0       : /*storeLocal(CiKind.Int, 0)*/; break;
                case ISTORE_1       : /*storeLocal(CiKind.Int, 1)*/; break;
                case ISTORE_2       : /*storeLocal(CiKind.Int, 2)*/; break;
                case ISTORE_3       : /*storeLocal(CiKind.Int, 3)*/; break;
                case LSTORE_0       : /*storeLocal(CiKind.Long, 0)*/; break;
                case LSTORE_1       : /*storeLocal(CiKind.Long, 1)*/; break;
                case LSTORE_2       : /*storeLocal(CiKind.Long, 2)*/; break;
                case LSTORE_3       : /*storeLocal(CiKind.Long, 3)*/; break;
                case FSTORE_0       : /*storeLocal(CiKind.Float, 0)*/; break;
                case FSTORE_1       : /*storeLocal(CiKind.Float, 1)*/; break;
                case FSTORE_2       : /*storeLocal(CiKind.Float, 2)*/; break;
                case FSTORE_3       : /*storeLocal(CiKind.Float, 3)*/; break;
                case DSTORE_0       : /*storeLocal(CiKind.Double, 0)*/; break;
                case DSTORE_1       : /*storeLocal(CiKind.Double, 1)*/; break;
                case DSTORE_2       : /*storeLocal(CiKind.Double, 2)*/; break;
                case DSTORE_3       : /*storeLocal(CiKind.Double, 3)*/; break;
                case ASTORE_0       : /*storeLocal(CiKind.Object, 0)*/; break;
                case ASTORE_1       : /*storeLocal(CiKind.Object, 1)*/; break;
                case ASTORE_2       : /*storeLocal(CiKind.Object, 2)*/; break;
                case ASTORE_3       : /*storeLocal(CiKind.Object, 3)*/; break;
                case IASTORE        : /*genStoreIndexed(CiKind.Int   )*/; break;
                case LASTORE        : /*genStoreIndexed(CiKind.Long  )*/; break;
                case FASTORE        : /*genStoreIndexed(CiKind.Float )*/; break;
                case DASTORE        : /*genStoreIndexed(CiKind.Double)*/; break;
                case AASTORE        : /*genStoreIndexed(CiKind.Object)*/; break;
                case BASTORE        : /*genStoreIndexed(CiKind.Byte  )*/; break;
                case CASTORE        : /*genStoreIndexed(CiKind.Char  )*/; break;
                case SASTORE        : /*genStoreIndexed(CiKind.Short )*/; break;
                case POP            : // fall through
                case POP2           : // fall through
                case DUP            : // fall through
                case DUP_X1         : // fall through
                case DUP_X2         : // fall through
                case DUP2           : // fall through
                case DUP2_X1        : // fall through
                case DUP2_X2        : // fall through
                case SWAP           : /*stackOp(opcode)*/; break;
                case IADD           : // fall through
                case ISUB           : // fall through
                case IMUL           : /*genArithmeticOp(CiKind.Int, opcode)*/; break;
                case IDIV           : // fall through
                case IREM           : /*genArithmeticOp(CiKind.Int, opcode, curState.copy())*/; break;
                case LADD           : // fall through
                case LSUB           : // fall through
                case LMUL           : /*genArithmeticOp(CiKind.Long, opcode)*/; break;
                case LDIV           : // fall through
                case LREM           : /*genArithmeticOp(CiKind.Long, opcode, curState.copy())*/; break;
                case FADD           : // fall through
                case FSUB           : // fall through
                case FMUL           : // fall through
                case FDIV           : // fall through
                case FREM           : /*genArithmeticOp(CiKind.Float, opcode)*/; break;
                case DADD           : // fall through
                case DSUB           : // fall through
                case DMUL           : // fall through
                case DDIV           : // fall through
                case DREM           : /*genArithmeticOp(CiKind.Double, opcode)*/; break;
                case INEG           : /*genNegateOp(CiKind.Int)*/; break;
                case LNEG           : /*genNegateOp(CiKind.Long)*/; break;
                case FNEG           : /*genNegateOp(CiKind.Float)*/; break;
                case DNEG           : /*genNegateOp(CiKind.Double)*/; break;
                case ISHL           : // fall through
                case ISHR           : // fall through
                case IUSHR          : /*genShiftOp(CiKind.Int, opcode)*/; break;
                case IAND           : // fall through
                case IOR            : // fall through
                case IXOR           : /*genLogicOp(CiKind.Int, opcode)*/; break;
                case LSHL           : // fall through
                case LSHR           : // fall through
                case LUSHR          : /*genShiftOp(CiKind.Long, opcode)*/; break;
                case LAND           : // fall through
                case LOR            : // fall through
                case LXOR           : /*genLogicOp(CiKind.Long, opcode)*/; break;
                case IINC           : /*genIncrement()*/; break;
                case I2L            : /*genConvert(opcode, CiKind.Int   , CiKind.Long  )*/; break;
                case I2F            : /*genConvert(opcode, CiKind.Int   , CiKind.Float )*/; break;
                case I2D            : /*genConvert(opcode, CiKind.Int   , CiKind.Double)*/; break;
                case L2I            : /*genConvert(opcode, CiKind.Long  , CiKind.Int   )*/; break;
                case L2F            : /*genConvert(opcode, CiKind.Long  , CiKind.Float )*/; break;
                case L2D            : /*genConvert(opcode, CiKind.Long  , CiKind.Double)*/; break;
                case F2I            : /*genConvert(opcode, CiKind.Float , CiKind.Int   )*/; break;
                case F2L            : /*genConvert(opcode, CiKind.Float , CiKind.Long  )*/; break;
                case F2D            : /*genConvert(opcode, CiKind.Float , CiKind.Double)*/; break;
                case D2I            : /*genConvert(opcode, CiKind.Double, CiKind.Int   )*/; break;
                case D2L            : /*genConvert(opcode, CiKind.Double, CiKind.Long  )*/; break;
                case D2F            : /*genConvert(opcode, CiKind.Double, CiKind.Float )*/; break;
                case I2B            : /*genConvert(opcode, CiKind.Int   , CiKind.Byte  )*/; break;
                case I2C            : /*genConvert(opcode, CiKind.Int   , CiKind.Char  )*/; break;
                case I2S            : /*genConvert(opcode, CiKind.Int   , CiKind.Short )*/; break;
                case LCMP           : /*genCompareOp(CiKind.Long, opcode)*/; break;
                case FCMPL          : /*genCompareOp(CiKind.Float, opcode)*/; break;
                case FCMPG          : /*genCompareOp(CiKind.Float, opcode)*/; break;
                case DCMPL          : /*genCompareOp(CiKind.Double, opcode)*/; break;
                case DCMPG          : /*genCompareOp(CiKind.Double, opcode)*/; break;
                case IFEQ           : /*genIfZero(Condition.eql)*/; break;
                case IFNE           : /*genIfZero(Condition.neq)*/; break;
                case IFLT           : /*genIfZero(Condition.lss)*/; break;
                case IFGE           : /*genIfZero(Condition.geq)*/; break;
                case IFGT           : /*genIfZero(Condition.gtr)*/; break;
                case IFLE           : /*genIfZero(Condition.leq)*/; break;
                case IF_ICMPEQ      : /*genIfSame(CiKind.Int, Condition.eql)*/; break;
                case IF_ICMPNE      : /*genIfSame(CiKind.Int, Condition.neq)*/; break;
                case IF_ICMPLT      : /*genIfSame(CiKind.Int, Condition.lss)*/; break;
                case IF_ICMPGE      : /*genIfSame(CiKind.Int, Condition.geq)*/; break;
                case IF_ICMPGT      : /*genIfSame(CiKind.Int, Condition.gtr)*/; break;
                case IF_ICMPLE      : /*genIfSame(CiKind.Int, Condition.leq)*/; break;
                case IF_ACMPEQ      : /*genIfSame(CiKind.Object, Condition.eql)*/; break;
                case IF_ACMPNE      : /*genIfSame(CiKind.Object, Condition.neq)*/; break;
                case GOTO           : /*genGoto(s.currentBCI(), s.readBranchDest())*/; break;
                case JSR            : /*genJsr(s.readBranchDest())*/; break;
                case RET            : /*genRet(s.readLocalIndex())*/; break;
                case TABLESWITCH    : /*genTableswitch()*/; break;
                case LOOKUPSWITCH   : /*genLookupswitch()*/; break;
                case IRETURN        : /*genMethodReturn(ipop())*/; break;
                case LRETURN        : /*genMethodReturn(lpop())*/; break;
                case FRETURN        : /*genMethodReturn(fpop())*/; break;
                case DRETURN        : /*genMethodReturn(dpop())*/; break;
                case ARETURN        : /*genMethodReturn(apop())*/; break;
                case RETURN         : /*genMethodReturn(null  )*/; break;
                case GETSTATIC      : /*genGetStatic(stream().readCPI())*/; break;
                case PUTSTATIC      : /*genPutStatic(stream().readCPI())*/; break;
                case GETFIELD       : /*genGetField(stream().readCPI())*/; break;
                case PUTFIELD       : /*genPutField(stream().readCPI())*/; break;
                case INVOKEVIRTUAL  : /*cpi = s.readCPI(); genInvokeVirtual(constantPool().lookupInvokeVirtual(cpi), cpi, constantPool())*/; break;
                case INVOKESPECIAL  : /*cpi = s.readCPI(); genInvokeSpecial(constantPool().lookupInvokeSpecial(cpi), null, cpi, constantPool())*/; break;
                case INVOKESTATIC   : /*cpi = s.readCPI(); genInvokeStatic(constantPool().lookupInvokeStatic(cpi), cpi, constantPool())*/; break;
                case INVOKEINTERFACE: /*cpi = s.readCPI(); genInvokeInterface(constantPool().lookupInvokeInterface(cpi), cpi, constantPool())*/; break;
                case NEW            : /*genNewInstance(stream().readCPI())*/; break;
                case NEWARRAY       : /*genNewTypeArray(stream().readLocalIndex())*/; break;
                case ANEWARRAY      : /*genNewObjectArray(stream().readCPI())*/; break;
                case ARRAYLENGTH    : /*genArrayLength()*/; break;
                case ATHROW         : /*genThrow(s.currentBCI())*/; break;
                case CHECKCAST      : /*genCheckCast()*/; break;
                case INSTANCEOF     : /*genInstanceOf()*/; break;
                case MONITORENTER   : /*genMonitorEnter(s.currentBCI(), curState.copy())*/; break;
                case MONITOREXIT    : /*genMonitorExit(s.currentBCI(), curState.copy())*/; break;
                case MULTIANEWARRAY : /*genNewMultiArray(stream().readCPI())*/; break;
                case IFNULL         : /*genIfNull(Condition.eql)*/; break;
                case IFNONNULL      : /*genIfNull(Condition.neq)*/; break;
                case GOTO_W         : /*genGoto(s.currentBCI(), s.readFarBranchDest())*/; break;
                case JSR_W          : /*genJsr(s.readFarBranchDest())*/; break;
                case BREAKPOINT:
            }
        }
    }



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


    public static final int WLOAD                = 203;
    public static final int WLOAD_0              = 204;
    public static final int WLOAD_1              = 205;
    public static final int WLOAD_2              = 206;
    public static final int WLOAD_3              = 207;

    public static final int WSTORE               = 208;
    public static final int WSTORE_0             = 209;
    public static final int WSTORE_1             = 210;
    public static final int WSTORE_2             = 211;
    public static final int WSTORE_3             = 212;

    enum Type {
        Boolean('z', "boolean", "jboolean", 1),
        Byte('b', "byte", "jbyte", 1),
        Short('s', "short", "jshort", 1),
        Char('c', "char", "jchar", 1),
        Int('i', "int", "jint", 1),
        Float('f', "float", "jfloat", 1),
        Double('d', "double", "jdouble", 2),
        Long('l', "long", "jlong", 2),
        Object('a', "object", "jobject", 1),
        Word('w', "word", null, 1),
        Void('v', "void", "void", 0);

        Type(char ch, String javaName, String jniName, int size) {

        }
    }

/*
    wadd
    wsub
    wdiv (unsigned)
    wdivi (divisor is an int)
    wmul
    wand
    wor
    wxor
    wshl
    wshr

    4. A set of (3) pointer operations:

    pread     (read from pointer, type of operand is in constant pool) // offset, scaled-index, displacement?
    pwrite    (write to pointer, type of operand is in constant pool) // offset, scaled-index, displacement?
    pcmpswp   (compare and swap, type of operand is in constant pool) // offset, scaled-index, displacement?

    5. A set of (10) comparison operators (to set condition variable):

    swcmp     (Signed word compare)
    swlt
    swgt
    swlteq
    swgteq
    uwcmp     (Unsigned word compare)
    uwlt
    uwgt
    uwlteq
    uwgteq

    6. A set of (6) conversion operators:

    w2i   (word to integer)
    w2l   (word to long)
    i2w   (integer to word)
    l2w   (long to word)
    w2a   (word to object)
    a2w   (object to word)

    7. An optional set of operators for performance

    wroundup4    (align word on next 4 byte boundary)
    wroundup8
    wroundup16
    wrounddn4    (align word on previous 4 byte boundary)
    wrounddn8
    wrounddn16
    wudiv        (unsigned divide)
    pfread       (prefetch for read)
    pfwrite      (prefetch for write)

    8. An invokenative instruction (?)

    invokenative  (invoke a native method using the native abi - signature in constant pool)

    9. Register access (based on abstract register roles - see below)

    getreg (operand denotes role)
    setreg (operand denotes role)
*/

}
