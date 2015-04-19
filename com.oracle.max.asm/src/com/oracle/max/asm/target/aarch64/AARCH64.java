package com.oracle.max.asm.target.aarch64;

import static com.oracle.max.cri.intrinsics.MemoryBarriers.*;
import static com.sun.cri.ci.CiRegister.RegisterFlag.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiArchitecture.*;
import com.sun.cri.ci.CiRegister.*;


public class AARCH64 extends CiArchitecture {

    // General purpose CPU registers
    // r0 - r7 -> arg0 - arg7
    public static final CiRegister r0 = gpCiRegister(0);
    public static final CiRegister r1 = gpCiRegister(1);
    public static final CiRegister r2 = gpCiRegister(2);
    public static final CiRegister r3 = gpCiRegister(3);
    public static final CiRegister r4 = gpCiRegister(4);
    public static final CiRegister r5 = gpCiRegister(5);
    public static final CiRegister r6 = gpCiRegister(6);
    public static final CiRegister r7 = gpCiRegister(7);
    public static final CiRegister r8 = gpCiRegister(8);
    public static final CiRegister r9 = gpCiRegister(9);
    public static final CiRegister r10 = gpCiRegister(10);
    public static final CiRegister r11 = gpCiRegister(11);
    public static final CiRegister r12 = gpCiRegister(12);
    public static final CiRegister r13 = gpCiRegister(13);
    public static final CiRegister r14 = gpCiRegister(14);
    public static final CiRegister r15 = gpCiRegister(15);
    public static final CiRegister r16 = gpCiRegister(16);
    public static final CiRegister r17 = gpCiRegister(17);
    public static final CiRegister r18 = gpCiRegister(18);
    public static final CiRegister r19 = gpCiRegister(19);
    public static final CiRegister r20 = gpCiRegister(20);
    public static final CiRegister r21 = gpCiRegister(21);
    public static final CiRegister r22 = gpCiRegister(22);
    public static final CiRegister r23 = gpCiRegister(23);
    public static final CiRegister r24 = gpCiRegister(24);
    public static final CiRegister r25 = gpCiRegister(25);
    public static final CiRegister r26 = gpCiRegister(26);
    public static final CiRegister r27 = gpCiRegister(27);
    public static final CiRegister r28 = gpCiRegister(28);
    public static final CiRegister r29 = gpCiRegister(29);
    public static final CiRegister r30 = gpCiRegister(30);

/********************************************************************************************************/
    // r31 is not a general purpose register, but represents either the stackpointer
    // or the zero/discard register depending on the instruction. So we represent
    // those two uses as two different registers.
    // The register numbers are kept in sync with register_aarch64.hpp and have to
    // be sequential, hence we also need a general r31 register here, which is never used.
    public static final CiRegister r31 = gpCiRegister(31);
    public static final CiRegister sp = new CiRegister(32, 31, 8, "SP", CPU);
    public static final CiRegister zr = new CiRegister(33, 31, 8, "ZR", CPU);

    // Names for special registers.
    public static final CiRegister linkRegister = r30;
    public static final CiRegister fp = r29;
    public static final CiRegister threadRegister = r28;
    public static final CiRegister heapBaseRegister = r27;
    // Register used for inline cache class.
    // see definition of IC_Klass in c1_LIRAssembler_aarch64.cpp
    public static final CiRegister inlineCacheRegister = r9;
    // Register used to store metaspace method.
    // see definition in sharedRuntime_aarch64.cpp:gen_c2i_adapter
    public static final CiRegister metaspaceMethodRegister = r12;
/********************************************************************************************************/

    // Floating point and SIMD registers
    public static final CiRegister v0 = fpCiRegister(0);
    public static final CiRegister v1 = fpCiRegister(1);
    public static final CiRegister v2 = fpCiRegister(2);
    public static final CiRegister v3 = fpCiRegister(3);
    public static final CiRegister v4 = fpCiRegister(4);
    public static final CiRegister v5 = fpCiRegister(5);
    public static final CiRegister v6 = fpCiRegister(6);
    public static final CiRegister v7 = fpCiRegister(7);
    public static final CiRegister v8 = fpCiRegister(8);
    public static final CiRegister v9 = fpCiRegister(9);
    public static final CiRegister v10 = fpCiRegister(10);
    public static final CiRegister v11 = fpCiRegister(11);
    public static final CiRegister v12 = fpCiRegister(12);
    public static final CiRegister v13 = fpCiRegister(13);
    public static final CiRegister v14 = fpCiRegister(14);
    public static final CiRegister v15 = fpCiRegister(15);
    public static final CiRegister v16 = fpCiRegister(16);
    public static final CiRegister v17 = fpCiRegister(17);
    public static final CiRegister v18 = fpCiRegister(18);
    public static final CiRegister v19 = fpCiRegister(19);
    public static final CiRegister v20 = fpCiRegister(20);
    public static final CiRegister v21 = fpCiRegister(21);
    public static final CiRegister v22 = fpCiRegister(22);
    public static final CiRegister v23 = fpCiRegister(23);
    public static final CiRegister v24 = fpCiRegister(24);
    public static final CiRegister v25 = fpCiRegister(25);
    public static final CiRegister v26 = fpCiRegister(26);
    public static final CiRegister v27 = fpCiRegister(27);
    public static final CiRegister v28 = fpCiRegister(28);
    public static final CiRegister v29 = fpCiRegister(29);
    public static final CiRegister v30 = fpCiRegister(30);
    public static final CiRegister v31 = fpCiRegister(31);

    public static final CiRegister[] CPU_REGISTERS = {
        r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13,
        r14, r15, r16, r17, r18, r19, r20, r21, r22, r23, r24,
        r25, r26, r27, r28, r29, r30, r31, sp, zr
    };

    public static final CiRegister[] FPU_REGISTERS = {
        v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13,
        v14, v15, v16, v17, v18, v19, v20, v21, v22, v23, v24,
        v25, v26, v27, v28, v29, v30, v31
    };

    public static final CiRegister[] allRegisters = {
        r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13,
        r14, r15, r16, r17, r18, r19, r20, r21, r22, r23, r24,
        r25, r26, r27, r28, r29, r30, r31, sp, zr,
        v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13,
        v14, v15, v16, v17, v18, v19, v20, v21, v22, v23, v24,
        v25, v26, v27, v28, v29, v30, v31
    };

    private static CiRegister gpCiRegister(int nr) {
        return new CiRegister(nr, nr, 8, "r"+nr, CPU, RegisterFlag.Byte);
    }

    private static CiRegister fpCiRegister(int nr) {
        // zr is last integer register.
        int firstFpRegNumber = zr.number + 1;
        return new CiRegister(firstFpRegNumber + nr, nr, 16, "v" + nr, FPU);
    }

    public AARCH64() {
        super("AARCH64",                        //architecture name
              8,                                //word size (8 bytes)
              ByteOrder.LittleEndian,           //endianness
              allRegisters,                     //available registers
              0, /*LOAD_STORE | STORE_STORE*/   //implicitMemoryBarriers (no implicit barriers)
              -1,                               //nativeCallDisplacementOffset (ingore)
              32,                               //registerReferenceMapBitCount
              8);                               //returnAddressSize (8 bytes)
    }
}
