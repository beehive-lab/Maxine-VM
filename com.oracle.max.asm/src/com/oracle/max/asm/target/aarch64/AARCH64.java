package com.oracle.max.asm.target.aarch64;

import static com.oracle.max.cri.intrinsics.MemoryBarriers.*;
import static com.sun.cri.ci.CiKind.*;
import static com.sun.cri.ci.CiRegister.RegisterFlag.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiRegister.RegisterFlag;

/**
 * jiaqi.liu
 * Represents the AArch64 architecture.
 * Registers in AArch64 state
 */
public class AARCH64 extends CiArchitecture {

    // General purpose CPU registers
    // AARCH64 provides 31 general purpose registers. x0 - x30
    // CiRegister(int number, int encoding, int spillSlotSize, String name, RegisterFlag... flags)
    public static final CiRegister x0 = new CiRegister(0, 0, 8, "x0", CPU);
    public static final CiRegister x1 = new CiRegister(1, 1, 8, "x1", CPU);
    public static final CiRegister x2 = new CiRegister(2, 2, 8, "x2", CPU);
    public static final CiRegister x3 = new CiRegister(3, 3, 8, "x3", CPU);
    public static final CiRegister x4 = new CiRegister(4, 4, 8, "x4", CPU);
    public static final CiRegister x5 = new CiRegister(5, 5, 8, "x5", CPU);
    public static final CiRegister x6 = new CiRegister(6, 6, 8, "x6", CPU);
    public static final CiRegister x7 = new CiRegister(7, 7, 8, "x7", CPU);
    public static final CiRegister x8 = new CiRegister(8, 8, 8, "x8", CPU);
    public static final CiRegister x9 = new CiRegister(9, 9, 8, "x9", CPU);
    public static final CiRegister x10 = new CiRegister(10, 10, 8, "x10", CPU);
    public static final CiRegister x11 = new CiRegister(11, 11, 8, "x11", CPU);
    public static final CiRegister x12 = new CiRegister(12, 12, 8, "x12", CPU);
    public static final CiRegister x13 = new CiRegister(13, 13, 8, "x13", CPU);
    public static final CiRegister x14 = new CiRegister(14, 14, 8, "x14", CPU);
    public static final CiRegister x15 = new CiRegister(15, 15, 8, "x15", CPU);
    public static final CiRegister x16 = new CiRegister(16, 16, 8, "x16", CPU);
    public static final CiRegister x17 = new CiRegister(17, 17, 8, "x17", CPU);
    public static final CiRegister x18 = new CiRegister(18, 18, 8, "x18", CPU);
    public static final CiRegister x19 = new CiRegister(19, 19, 8, "x19", CPU);
    public static final CiRegister x20 = new CiRegister(20, 20, 8, "x20", CPU);
    public static final CiRegister x21 = new CiRegister(21, 21, 8, "x21", CPU);
    public static final CiRegister x22 = new CiRegister(22, 22, 8, "x22", CPU);
    public static final CiRegister x23 = new CiRegister(23, 23, 8, "x23", CPU);
    public static final CiRegister x24 = new CiRegister(24, 24, 8, "x24", CPU);
    public static final CiRegister x25 = new CiRegister(25, 25, 8, "x25", CPU);
    public static final CiRegister x26 = new CiRegister(26, 26, 8, "x26", CPU);
    public static final CiRegister x27 = new CiRegister(27, 27, 8, "x27", CPU);
    public static final CiRegister x28 = new CiRegister(28, 28, 8, "x28", CPU);
    public static final CiRegister x29 = new CiRegister(29, 29, 8, "x29", CPU);
    public static final CiRegister x30 = new CiRegister(30, 30, 8, "x30", CPU);

    public static final CiRegister[] cpuRegisters = {
        x0, x1, x2, x3, x4, x5, x6, x7, x8, x9,
        x10, x11, x12, x13, x14, x15, x16, x17, x18, x19,
        x20, x21, x22, x23, x24, x25, x26, x27, x28, x29,
        x30
    };


    // TODO AARCH dose not have registers like XMM?
    public static final CiRegister[] xmmRegisters = {

    };

    // TODO AARCH dose not have registers like XMM?
    public static final CiRegister[] cpuxmmRegisters = {
        x0, x1, x2, x3, x4, x5, x6, x7, x8, x9,
        x10, x11, x12, x13, x14, x15, x16, x17, x18, x19,
        x20, x21, x22, x23, x24, x25, x26, x27, x28, x29,
        x30
    };

    /**
     * Arch64 stack pointer register
     */
    public static final CiRegister sp = new CiRegister(31, 31, 8, "sp", CPU, RegisterFlag.Byte);

    /**
     * Register used to construct an instruction-relative address.
     */
    public static final CiRegister pc = new CiRegister(32, -1, 0, "pc");

    public static final CiRegister[] allRegisters = {
        x0, x1, x2, x3, x4, x5, x6, x7, x8, x9,
        x10, x11, x12, x13, x14, x15, x16, x17, x18, x19,
        x20, x21, x22, x23, x24, x25, x26, x27, x28, x29, x30,
        sp, pc
    };

    public static final CiRegisterValue SP = sp.asValue(Long);

    public AARCH64() {
        super(
            "AARCH64", 8,
            ByteOrder.LittleEndian,
            allRegisters,
            LOAD_LOAD | LOAD_STORE | STORE_LOAD | STORE_STORE,
            1,
            x30.encoding + 1,
            8);
    }

    @Override
    public boolean isX86() {
        return false;
    }

    @Override
    public boolean twoOperandMode() {
        return false;
    }

}
