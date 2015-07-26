package test.arm.asm;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.asm.target.aarch64.Aarch64Assembler.ExtendType;
import com.oracle.max.asm.target.aarch64.Aarch64Assembler.ShiftType;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;

public class Aarch64AssemblerTest extends MaxTestCase {

    private static final int VARIANT_32 = 32;
    private static final int VARIANT_64 = 64;

    private Aarch64Assembler asm;
    private Aarch64MacroAssembler masm;

    private CiTarget aarch64;
    private ARMCodeWriter code;

    static final class Pair {

        public final int first;
        public final int second;

        public Pair(int first, int second) {
            this.first = first;
            this.second = second;
        }
    }

    public Aarch64AssemblerTest() {
        aarch64 = new CiTarget(new Aarch64(), true, 8, 0, 4096, 0, false, false, false);
        asm = new Aarch64Assembler(aarch64, null);
        masm = new Aarch64MacroAssembler(aarch64, null);
        code = null;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Aarch64AssemblerTest.class);
    }

    private static int[] valueTestSet = { 0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65535};
    private static int[] scratchTestSet = { 0, 1, 0xff, 0xffff, 0xffffff, 0xfffffff, 0x7fffffff};
    private static boolean[] testValues = new boolean[MaxineARMTester.NUM_REGS];

    // Each test should set the contents of this array appropriately,
    // it enables the instruction under test to select the specific bit values for
    // comparison i.e. for example ignoring upper or lower 16bits for movt, movw
    // and for ignoring specific bits in the status register etc
    // concerning whether a carry has been set
    private static MaxineARMTester.BitsFlag[] bitmasks = new MaxineARMTester.BitsFlag[MaxineARMTester.NUM_REGS];
    static {
        for (int i = 0; i < MaxineARMTester.NUM_REGS; i++) {
            bitmasks[i] = MaxineARMTester.BitsFlag.All32Bits;
        }
    }

    private static void setBitMask(int i, MaxineARMTester.BitsFlag mask) {
        bitmasks[i] = mask;
    }

    private static void setAllBitMasks(MaxineARMTester.BitsFlag mask) {
        for (int i = 0; i < bitmasks.length; i++) {
            bitmasks[i] = mask;
        }
    }

    private static void setIgnoreValue(int i, boolean value, boolean all) {
        testValues[i] = value;
    }

    private static void resetIgnoreValues() {
        for (int i = 0; i < testValues.length; i++) {
            testValues[i] = false;
        }
    }

    // The following values will be updated
    // to those expected to be found in a register after simulated execution of code.
    private static long[] expectedValues = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21 , 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    private static long[] expectedLongValues = { Long.MAX_VALUE - 100, Long.MAX_VALUE};

    private static void initialiseExpectedValues() {
        for (int i = 0; i < MaxineARMTester.NUM_REGS; i++) {
            expectedValues[i] = i;
        }
    }

    private static void initialiseTestValues() {
        for (int i = 0; i < MaxineARMTester.NUM_REGS; i++) {
            testValues[i] = false;
        }
    }

    private void generateAndTest(long[] expected, boolean[] tests, MaxineARMTester.BitsFlag[] masks, Buffer codeBuffer) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(codeBuffer);
        code.createCodeFile();
        MaxineARMTester r = new MaxineARMTester(expected, tests, masks);
        if (!MaxineARMTester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            return;
        }
        r.assembleStartup();
        r.compile();
        r.link();
        r.objcopy();
        r.runSimulation();
        r.reset();
    }
    private long [] generate() throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(asm.codeBuffer);
        code.createCodeFile();
        long [] retArr;
        MaxineARMTester r = new MaxineARMTester();
        if (!MaxineARMTester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            return null;
        }
        r.assembleStartup();
        r.assembleEntry();
        r.compile();
        r.link();
        r.objcopy();
        retArr = r.runSimulationRegisters();
        r.reset();
        return retArr;
    }

    private void generateAndTest(long[] expected, boolean[] tests, MaxineARMTester.BitsFlag[] masks) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(asm.codeBuffer);
        code.createCodeFile();
        MaxineARMTester r = new MaxineARMTester(expected, tests, masks);
        if (!MaxineARMTester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            return;
        }
        r.assembleStartup();
        r.assembleEntry();
        r.compile();
        r.link();
        r.objcopy();
        r.runSimulation();
        r.reset();
    }

  /**
  * branch
  */
    public void test_b() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.b(8);
        asm.add(VARIANT_64, Aarch64.cpuRegisters[10], Aarch64.cpuRegisters[10], 0);     // dummy instruction to take 4 bytes memory space
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x123, 0);        // b(8) should jump here
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 0x123, 0);        // and then reach here

        expectedValues[0] = 0x123;
        testValues[0] = true;
        expectedValues[1] = 0x123;
        testValues[1] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }


    /**
     * load and store instructions
     */
    public void test_ldr_str() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x123, 0);        // value to be stored to stack
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 8, 0);           // offset
        Aarch64Address address = Aarch64Address.createRegisterOffsetAddress(Aarch64.sp,  Aarch64.cpuRegisters[10], false); // stack address
        asm.str(VARIANT_64,  Aarch64.cpuRegisters[0], address);         // store value to stack
        asm.ldr(VARIANT_64, Aarch64.cpuRegisters[1], address);          // load value from stack

        expectedValues[0] = 0x123;
        testValues[0] = true;
        expectedValues[1] = 0x123;
        testValues[1] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_add_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // ADD Xi, Xi, valueof(Xi) ; Xi's value should double
        for (int i = 0; i < 10; i++) {
            // expected values must not be larger than an integer in order to be converted to an int correctly
            assert (expectedValues[i] < Integer.MAX_VALUE);
            asm.movImmediate(Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            asm.add(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            expectedValues[i] += expectedValues[i];
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_sub_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // SUB Xi, Xi, valueof(Xi) ; Xi should then be 0
        for (int i = 0; i < 10; i++) {
            // expected values must not be larger than an integer in order to be converted to an int correctly
            assert (expectedValues[i] < Integer.MAX_VALUE);
            asm.movImmediate(Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            asm.sub(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            expectedValues[i] = 0; //expectedValues[i] -= expectedValues[i];
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_and_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // AND Xi, Xi, 0x1
        for (int i = 0; i < 10; i++) {
            asm.movImmediate(Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            asm.and(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 0x1);
            expectedValues[i] &= 0x1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_eor_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // EOR Xi, Xi, 0x1
        for (int i = 0; i < 10; i++) {
            asm.movImmediate(Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            asm.eor(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 0x1);
            expectedValues[i] ^= 0x1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_orr_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // ORR Xi, Xi, 0x1
        for (int i = 0; i < 10; i++) {
            asm.movImmediate(Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            asm.orr(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 0x1);
            expectedValues[i] |= 0x1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_movz_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // MOVZ Xi, 0x1, LSL #0x10
        for (int i = 0; i < 1; i++) {
            asm.movz(VARIANT_64, Aarch64.cpuRegisters[i], 0x1, 32);
            expectedValues[i] = (long) 1 << 32;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_movn_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // MOVZ Xi, 0x1, LSL #0x10
        for (int i = 0; i < 1; i++) {
            asm.movn(VARIANT_64, Aarch64.cpuRegisters[i], 0x1, 32);
            expectedValues[i] = ~((long) 1 << 32);
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_bfm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);

        asm.bfm(VARIANT_64, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[10], 3, 5);
        expectedValues[0] = 0b111111l >>> 3;
        testValues[0] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_ubfm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);

        asm.ubfm(VARIANT_64, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[10], 3, 5);
        expectedValues[0] = 0b111111l >>> 3;
        testValues[0] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_sbfm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);

        asm.ubfm(VARIANT_64, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[10], 3, 5);
        expectedValues[0] = 0b111111l >>> 3;
        testValues[0] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /******* Arithmetic (shifted register) (5.5.1) *******/

    public void test_add_shift_reg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImmediate(Aarch64.cpuRegisters[10], 1);
        for (int i = 0; i < 1; i++) {
            asm.add(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64.cpuRegisters[10], ShiftType.LSL, 2);
            expectedValues[i] = 5;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_sub_shift_reg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImmediate(Aarch64.cpuRegisters[10], 0b1000);
        for (int i = 0; i < 1; i++) {
            asm.sub(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64.cpuRegisters[10], ShiftType.LSR, 3);
            expectedValues[i] = 0b1000 - (0b1000 >>> 3);
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /******* Arithmetic (extended register) (5.5.2) *******/

    public void test_add_ext_reg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImmediate(Aarch64.cpuRegisters[0], 1);
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);

        for (int i = 0; i < 1; i++) {
            asm.add(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], ExtendType.UXTB, 3);
            expectedValues[i] = 1 + 0b11111111000l;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_sub_ext_reg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImmediate(Aarch64.cpuRegisters[0], 0b11111111000);
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);

        for (int i = 0; i < 1; i++) {
            asm.sub(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], ExtendType.UXTB, 3);
            expectedValues[i] = 0;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /******* Logical (shifted register) (5.5.3) *******/

    public void test_and_shift_reg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 0xf, 0);

        for (int i = 0; i < 1; i++) {
            asm.and(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], ShiftType.LSL, 4);
            expectedValues[i] = 0xffffffffffffffffl & (0b1111l << 4);
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /* Variable Shift (5.5.4) */

    public void test_asr() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 0xf, 0);

        for (int i = 0; i < 1; i++) {
            asm.and(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], ShiftType.LSL, 4);
            expectedValues[i] = 0xffffffffffffffffl & (0b1111l << 4);
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /* Bit Operations (5.5.5) */

    public void test_cls() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[30], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);

            asm.cls(VARIANT_64, Aarch64.cpuRegisters[30], Aarch64.cpuRegisters[10]);
            expectedValues[30] = 63;
            testValues[30] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /* Integer Multiply/Divide (5.6) */


    /* Floating-point Move (register) (5.7.2) */

    public void test_float0() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 10, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 11, 0);

        asm.fmovCpu2Fpu(VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);
        asm.fmovCpu2Fpu(VARIANT_64, Aarch64.fpuRegisters[1], Aarch64.cpuRegisters[1]);
        asm.fmovFpu2Cpu(VARIANT_64, Aarch64.cpuRegisters[2], Aarch64.fpuRegisters[0]);
        asm.fmovFpu2Cpu(VARIANT_64, Aarch64.cpuRegisters[3], Aarch64.fpuRegisters[1]);

        expectedValues[2] = 10;
        testValues[2] = true;
        expectedValues[3] = 11;
        testValues[3] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);

    }

    /**
     *  test: fadd, fsub, fmul, fdiv, scvtf, fcvtzs
     */
    public void test_float1() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 10, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 110, 0);

        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);
        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[1], Aarch64.cpuRegisters[1]);

        asm.fadd(VARIANT_64, Aarch64.fpuRegisters[2], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1]);
        asm.fsub(VARIANT_64, Aarch64.fpuRegisters[3], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[0]);
        asm.fmul(VARIANT_64, Aarch64.fpuRegisters[4], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[0]);
        asm.fdiv(VARIANT_64, Aarch64.fpuRegisters[5], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[0]);

        asm.fcvtzs(VARIANT_64, VARIANT_64,  Aarch64.cpuRegisters[2],  Aarch64.fpuRegisters[2]);
        asm.fcvtzs(VARIANT_64, VARIANT_64,  Aarch64.cpuRegisters[3],  Aarch64.fpuRegisters[3]);
        asm.fcvtzs(VARIANT_64, VARIANT_64,  Aarch64.cpuRegisters[4],  Aarch64.fpuRegisters[4]);
        asm.fcvtzs(VARIANT_64, VARIANT_64,  Aarch64.cpuRegisters[5],  Aarch64.fpuRegisters[5]);

        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 110;
        testValues[1] = true;
        expectedValues[2] = 120;         // fadd
        testValues[2] = true;
        expectedValues[3] = 100;         // fsub
        testValues[3] = true;
        expectedValues[4] = 10 * 110;    // fmul
        testValues[4] = true;
        expectedValues[5] = 110 / 10;    // fdiv
        testValues[5] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * frintz, fabs, fneg, fsqrt
     */
    public void test_float2() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 100, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 110, 0);

        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);
        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[1], Aarch64.cpuRegisters[1]);

        asm.fdiv(VARIANT_64, Aarch64.fpuRegisters[2], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[0]);
        asm.frintz(VARIANT_64, Aarch64.fpuRegisters[2], Aarch64.fpuRegisters[2]);
        asm.fcvtzs(VARIANT_64, VARIANT_64,  Aarch64.cpuRegisters[2],  Aarch64.fpuRegisters[2]);

        asm.fsub(VARIANT_64, Aarch64.fpuRegisters[3], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1]);
        asm.fabs(VARIANT_64, Aarch64.fpuRegisters[3], Aarch64.fpuRegisters[3]);
        asm.fcvtzs(VARIANT_64, VARIANT_64,  Aarch64.cpuRegisters[3],  Aarch64.fpuRegisters[3]);

        asm.fsub(VARIANT_64, Aarch64.fpuRegisters[4], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1]);
        asm.fneg(VARIANT_64, Aarch64.fpuRegisters[4], Aarch64.fpuRegisters[4]);
        asm.fcvtzs(VARIANT_64, VARIANT_64,  Aarch64.cpuRegisters[4],  Aarch64.fpuRegisters[4]);

        asm.fsqrt(VARIANT_64, Aarch64.fpuRegisters[5], Aarch64.fpuRegisters[0]);
        asm.fcvtzs(VARIANT_64, VARIANT_64,  Aarch64.cpuRegisters[5],  Aarch64.fpuRegisters[5]);

        expectedValues[0] = 100;
        testValues[0] = true;
        expectedValues[1] = 110;
        testValues[1] = true;
        expectedValues[2] = 1;         // frintz
        testValues[2] = true;
        expectedValues[3] = 10;        // fabs
        testValues[3] = true;
        expectedValues[4] = 10;        // fneg
        testValues[4] = true;
        expectedValues[5] = 10;        // fsqrt
        testValues[5] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * fmadd, fmsub
     */
    public void test_float3() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 100, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 110, 0);

        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);
        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[1], Aarch64.cpuRegisters[1]);

        asm.fmadd(VARIANT_64, Aarch64.fpuRegisters[3], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[1]);
        asm.fmsub(VARIANT_64, Aarch64.fpuRegisters[4], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[1]);

        asm.fcvtzs(VARIANT_64, VARIANT_64,  Aarch64.cpuRegisters[3],  Aarch64.fpuRegisters[3]);
        asm.fcvtzs(VARIANT_64, VARIANT_64,  Aarch64.cpuRegisters[4],  Aarch64.fpuRegisters[4]);

        expectedValues[0] = 100;
        testValues[0] = true;
        expectedValues[1] = 110;
        testValues[1] = true;

        expectedValues[3] = 110 + 100 * 110;        // fmadd
        testValues[3] = true;
        expectedValues[4] = 110 - 100 * 110;        // fmsub
        testValues[4] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     *  fstr, fldr
     */
    public void test_float4() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x123, 0);        // value to be stored to stack
        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 8, 0);           // offset
        Aarch64Address address = Aarch64Address.createRegisterOffsetAddress(Aarch64.sp,  Aarch64.cpuRegisters[10], false); // stack address

        asm.fstr(VARIANT_64, Aarch64.fpuRegisters[0], address);         // store value to stack
        asm.fldr(VARIANT_64, Aarch64.fpuRegisters[1], address);          // load value from stack

        asm.fcvtzs(VARIANT_64, VARIANT_64,  Aarch64.cpuRegisters[1],  Aarch64.fpuRegisters[1]);

        expectedValues[0] = 0x123;
        testValues[0] = true;
        expectedValues[1] = 0x123;
        testValues[1] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }






    /*
    public void ignore_Ldrb() throws Exception {
        int[] testval = { 0x03020100, 0xffedcba9};
        int mask = 0xff;
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // load r0 and r1 with sensible values for ignoring the loading of bytes.
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], testval[0]);
        asm.mov32BitConstant(ARMv8.cpuRegisters[1], testval[1]);
        asm.push(ARMv8Assembler.ConditionFlag.Always, 1 | 2); // values now lie on the stack
        for (int i = 0; i < 8; i++) {
            // stackpointer advanced by 8
            asm.ldrb(ARMv8Assembler.ConditionFlag.Always, 1, 1, 0, ARMv8.cpuRegisters[i], ARMv8.cpuRegisters[13], i);
            testValues[i] = true;
            if (i < 4) {
                expectedValues[i] = testval[0] & (mask << (8 * (i % 4)));
            } else {
                expectedValues[i] = testval[1] & (mask << (8 * (i % 4)));
            }

            expectedValues[i] = expectedValues[i] >> 8 * (i % 4);
            if (expectedValues[i] < 0) {
                expectedValues[i] = 0x100 + expectedValues[i];
            }
            // Bytes do not have a sign! So we need to make sure the expectedValues are
            // not affected by sign extension side effects when we take the MSByte of
            // an integer.
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);

    }
    public long connectRegs(int reg0, int reg1) {
        long returnVal = 0;
        long tmp = 0;
        //  r1 is MSW
        // r2 is LSW
        System.out.println(" REG0 " + reg0 + " REG1 " + reg1);
        if(reg1 < 0) {
            // -ve long number

            returnVal = ((long) reg1) << 32;


            if(reg0 < 0) {
                returnVal += Long.parseLong(String.format("%32s", Integer.toBinaryString(reg0)).replace(' ', '0'),2);
            }else {
                returnVal += reg0;
            }
        } else {
            // +ve long number
            returnVal = ((long)reg1) << 32;
            if(reg1 == 0) {
                returnVal += reg0;
            } else
            if(reg0 < 0) {
                //returnVal += 1L << 31;
                returnVal += Long.parseLong(String.format("%32s", Integer.toBinaryString(reg0)).replace(' ', '0'),2);
            } else {
                returnVal += reg0;
            }
        }

        return returnVal;
    }

    public void ignore_mov64BitConstant() throws Exception {
        int[] instructions = new int[6];
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        long[] values = new long[10];
        values[0] = 0L;
        values[1] = -1L;
        values[2] = (long) Integer.MIN_VALUE;
        values[3] = (long) Integer.MAX_VALUE;
        values[4] = Long.MAX_VALUE;
        values[5] = Long.MIN_VALUE;
        values[6] = Long.MIN_VALUE + 5;
        values[7] = Long.MAX_VALUE - 5;
        values[8] = ((long) Integer.MIN_VALUE) + 5L;
        values[9] = ((long) Integer.MAX_VALUE) - 5L;
        int registers[] = null;
        for (int i = 0; i < values.length; i++) {
            asm.codeBuffer.reset();
            asm.mov64BitConstant(ARMv8.r0, ARMv8.r1, values[i]);
            instructions[0] = asm.codeBuffer.getInt(0);
            instructions[1] = asm.codeBuffer.getInt(4);
            instructions[2] = asm.codeBuffer.getInt(8);
            instructions[3] = asm.codeBuffer.getInt(12);
            registers = generate();
            assert values[i] == connectRegs(registers[0], registers[1]);
        }
    }

    public void ignore_AddConstant() throws Exception {
        int[] instructions = new int[3];
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        initialiseExpectedValues();
        resetIgnoreValues();
        for (int srcReg = 0; srcReg < 3; srcReg++) {
            for (int destReg = 0; destReg < 3; destReg++) {
                initialiseTestValues();
                testValues[destReg] = true;
                for (int i = 0; i < scratchTestSet.length; i++) {
                    asm.codeBuffer.reset();
                    int value = scratchTestSet[i];
                    asm.movw(ARMv8Assembler.ConditionFlag.Always, ARMv8.cpuRegisters[srcReg], value & 0xffff);
                    asm.movt(ARMv8Assembler.ConditionFlag.Always, ARMv8.cpuRegisters[srcReg], (value >> 16) & 0xffff);
                    asm.add(ARMv8Assembler.ConditionFlag.Always, false, ARMv8.cpuRegisters[destReg], ARMv8.cpuRegisters[srcReg], 0, 0);
                    instructions[0] = asm.codeBuffer.getInt(0);
                    instructions[1] = asm.codeBuffer.getInt(4);
                    instructions[2] = asm.codeBuffer.getInt(8);
                    expectedValues[destReg] = value;
                    generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
                }
            }
        }
    }

    public void test_VPushPop() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 5; i++) {
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], i);
            asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.allRegisters[i + 16], ARMv8.cpuRegisters[i], null, CiKind.Float, CiKind.Int);
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], -i);
        }
        asm.vpush(ARMv8Assembler.ConditionFlag.Always, ARMv8.allRegisters[16], ARMv8.allRegisters[16 + 4], CiKind.Float, CiKind.Float);
        for (int i = 0; i < 5; i++) {
            asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.allRegisters[i + 16], ARMv8.cpuRegisters[i], null, CiKind.Float, CiKind.Int);
        }
        asm.vpop(ARMv8Assembler.ConditionFlag.Always, ARMv8.allRegisters[16], ARMv8.allRegisters[16 + 4], CiKind.Float, CiKind.Float);
        for (int i = 0; i < 5; i++) {
            asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.cpuRegisters[i], ARMv8.allRegisters[i + 16], null, CiKind.Int, CiKind.Float);
        }
        expectedValues[0] = 0;
        testValues[0] = true;
        expectedValues[1] = 1;
        testValues[1] = true;
        expectedValues[2] = 2;
        testValues[2] = true;
        expectedValues[3] = 3;
        testValues[3] = true;
        expectedValues[4] = 4;
        testValues[4] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_Vdiv() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], 10);
        asm.mov32BitConstant(ARMv8.cpuRegisters[1], 24);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, ARMv8.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s1, ARMv8.r1, null, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s2, false, true, ARMv8.s0, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s3, false, true, ARMv8.s1, CiKind.Float, CiKind.Int);

        asm.vdiv(ARMv8Assembler.ConditionFlag.Always, ARMv8.s2, ARMv8.s3, ARMv8.s2, CiKind.Float);

        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, true, true, ARMv8.s2, CiKind.Float, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r2, ARMv8.s0, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 24;
        testValues[1] = true;
        expectedValues[2] = 2;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_Vcvt_int2float() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], 10);
        asm.mov32BitConstant(ARMv8.cpuRegisters[1], 24);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, ARMv8.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s1, ARMv8.r1, null, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s2, false, true, ARMv8.s0, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s3, false, true, ARMv8.s1, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s2, true, true, ARMv8.s2, CiKind.Float, CiKind.Float);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s3, true, true, ARMv8.s3, CiKind.Float, CiKind.Float);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r0, ARMv8.s2, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r1, ARMv8.s3, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 24;
        testValues[1] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_Vcvt_int2double() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], 10);
        asm.mov32BitConstant(ARMv8.cpuRegisters[1], 24);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, ARMv8.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s1, ARMv8.r1, null, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s2, false, true, ARMv8.s0, CiKind.Double, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s3, false, true, ARMv8.s1, CiKind.Double, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, true, true, ARMv8.s2, CiKind.Float, CiKind.Double);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s1, true, true, ARMv8.s3, CiKind.Float, CiKind.Double);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r0, ARMv8.s0, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r2, ARMv8.s1, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 24;
        testValues[1] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }



    public void test_Vcvt_double2float() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov64BitConstant(ARMv8.cpuRegisters[0], ARMv8.cpuRegisters[1], Double.doubleToRawLongBits(-10));
        asm.mov64BitConstant(ARMv8.cpuRegisters[2], ARMv8.cpuRegisters[3], Double.doubleToRawLongBits(-24));
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, ARMv8.r0, ARMv8.r1, CiKind.Double, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s1, ARMv8.r2, ARMv8.r3, CiKind.Double, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s4, false, true, ARMv8.s0, CiKind.Float, CiKind.Double);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s5, false, true, ARMv8.s1, CiKind.Float, CiKind.Double);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r0, ARMv8.s4, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r2, ARMv8.s5, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = Float.floatToRawIntBits(-10);
        testValues[0] = true;
        expectedValues[2] =Float.floatToRawIntBits(-24);
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }


    public void test_VAdd() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], 12);
        asm.mov32BitConstant(ARMv8.cpuRegisters[1], 10);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, ARMv8.r0, null, CiKind.Float, CiKind.Int); // r2 has r0?
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s1, ARMv8.r1, null, CiKind.Float, CiKind.Int); // r4 and r5 contain r0 and r1
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s2, false, false, ARMv8.s0, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s4, false, false, ARMv8.s1, CiKind.Float, CiKind.Int);
        asm.vadd(ARMv8Assembler.ConditionFlag.Always, ARMv8.s2, ARMv8.s4, ARMv8.s2, CiKind.Float);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, true, true, ARMv8.s2, CiKind.Float, CiKind.Float);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r2, ARMv8.s0, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 22;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_VSub() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], 12);
        asm.mov32BitConstant(ARMv8.cpuRegisters[1], 10);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, ARMv8.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s1, ARMv8.r1, null, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s2, false, false, ARMv8.s0, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s4, false, false, ARMv8.s1, CiKind.Float, CiKind.Int);
        asm.vsub(ARMv8Assembler.ConditionFlag.Always, ARMv8.s2, ARMv8.s4, ARMv8.s2, CiKind.Float);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, true, true, ARMv8.s2, CiKind.Float, CiKind.Float);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r2, ARMv8.s0, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = -2;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_VcvtvMul() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], 12);
        asm.mov32BitConstant(ARMv8.cpuRegisters[1], 10);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, ARMv8.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s1, ARMv8.r1, null, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s2, false, false, ARMv8.s0, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s4, false, false, ARMv8.s1, CiKind.Float, CiKind.Int);
        asm.vmul(ARMv8Assembler.ConditionFlag.Always, ARMv8.s2, ARMv8.s4, ARMv8.s2, CiKind.Float);
        asm.vcvt(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, true, false, ARMv8.s2, CiKind.Float, CiKind.Float);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r2, ARMv8.s0, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 120;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_VldrStr() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], 12);
        asm.mov32BitConstant(ARMv8.cpuRegisters[1], 10);
        asm.push(ARMv8Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512 | 1024 | 2048); // instruction
        asm.vldr(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, ARMv8.r13, 0, CiKind.Float, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r2, ARMv8.s0, null, CiKind.Int, CiKind.Float);
        asm.vldr(ARMv8Assembler.ConditionFlag.Always, ARMv8.s4, ARMv8.r13, 0, CiKind.Float, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r4, ARMv8.s4, null, CiKind.Int, CiKind.Float);
        asm.vstr(ARMv8Assembler.ConditionFlag.Always, ARMv8.s4, ARMv8.r13, -8, CiKind.Float, CiKind.Int);
        asm.vstr(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, ARMv8.r13, -16, CiKind.Float, CiKind.Int);
        asm.vldr(ARMv8Assembler.ConditionFlag.Always, ARMv8.s10, ARMv8.r13, -8, CiKind.Float, CiKind.Int);
        asm.vldr(ARMv8Assembler.ConditionFlag.Always, ARMv8.s31, ARMv8.r13, -16, CiKind.Float, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r6, ARMv8.s10, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r8, ARMv8.s31, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 12;
        testValues[2] = true;
        expectedValues[4] = 12;
        testValues[4] = true;
        expectedValues[6] = 12;
        testValues[6] = true;
        expectedValues[8] = 12;
        testValues[8] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_Vldr() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], 12);
        asm.mov32BitConstant(ARMv8.cpuRegisters[1], 10);
        asm.push(ARMv8Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512); // 1 instruction
        asm.vldr(ARMv8Assembler.ConditionFlag.Always, ARMv8.s31, ARMv8.r13, 0, CiKind.Float, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r2, ARMv8.s31, null, CiKind.Int, CiKind.Float);
        asm.vldr(ARMv8Assembler.ConditionFlag.Always, ARMv8.s4, ARMv8.r13, 0, CiKind.Int, CiKind.Float);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r4, ARMv8.s4, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 12;
        testValues[2] = true;
        expectedValues[4] = 12;
        testValues[4] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_MVov() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], 12);
        asm.mov32BitConstant(ARMv8.cpuRegisters[1], 10);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, ARMv8.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r2, ARMv8.s0, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.s5, ARMv8.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r4, ARMv8.s5, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 12;
        testValues[2] = true;
        expectedValues[4] = 12;
        testValues[4] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    //TODO: Fix vmovimm
    public void broken_vmovimm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.vmovImm(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, 1, CiKind.Double);
        asm.vmovImm(ARMv8Assembler.ConditionFlag.Always, ARMv8.s0, 0, CiKind.Double);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r0, ARMv8.s0, null, CiKind.Int, CiKind.Double);
        asm.vmovImm(ARMv8Assembler.ConditionFlag.Always, ARMv8.s1, -100, CiKind.Double);
        asm.vmov(ARMv8Assembler.ConditionFlag.Always, ARMv8.r2, ARMv8.s1,null,  CiKind.Int, CiKind.Double);
        expectedValues[0] = 0;
        testValues[0] = true;
        expectedValues[2] = -100;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_FloatIngPointExperiments() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], 12);
        asm.mov32BitConstant(ARMv8.cpuRegisters[1], 10);
        asm.codeBuffer.emitInt(0xee000a10);
        asm.codeBuffer.emitInt(0xee001a90);
        asm.codeBuffer.emitInt(0xeeb81ac0);
        asm.codeBuffer.emitInt(0xeef81ae0);
        asm.codeBuffer.emitInt(0xee210a21);
        asm.codeBuffer.emitInt(0xeebd0a40);
        asm.codeBuffer.emitInt(0xee100a10);
        expectedValues[0] = 120;
        testValues[0] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_SubReg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 5; i++) {
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], expectedValues[i]);
        }
        for (int i = 0; i < 5; i++) {
            asm.sub(ARMv8Assembler.ConditionFlag.Always, false, ARMv8.cpuRegisters[i + 5], ARMv8.cpuRegisters[5 - (i + 1)], ARMv8.cpuRegisters[i], 0, 0);
            expectedValues[i + 5] = expectedValues[5 - (i + 1)] - expectedValues[i];
            testValues[i + 5] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_Mov() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 5; i++) {
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], expectedValues[i]);
            asm.mov(ARMv8Assembler.ConditionFlag.Always, true, ARMv8.cpuRegisters[i + 5], ARMv8.cpuRegisters[i]);
            expectedValues[i + 5] = expectedValues[i];
            testValues[i] = true;
            testValues[i + 5] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_Sub() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], expectedValues[i]);
            asm.sub(ARMv8Assembler.ConditionFlag.Always, true, ARMv8.cpuRegisters[i], ARMv8.cpuRegisters[i], i * 2, 0);
            expectedValues[i] = expectedValues[i] - i * 2;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_Str() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMv8.cpuRegisters[12], 0);
        for (int i = 0; i < 10; i++) {
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], expectedValues[i]);
            testValues[i] = true;
            asm.str(ARMv8Assembler.ConditionFlag.Always, 1, 0, 0, ARMv8.cpuRegisters[i], ARMv8.cpuRegisters[13], ARMv8.cpuRegisters[12], i * 4, 0);
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], -2 * (expectedValues[i]));
            asm.ldr(ARMv8Assembler.ConditionFlag.Always, 1, 0, 0, ARMv8.cpuRegisters[i], ARMv8.cpuRegisters[13], ARMv8.cpuRegisters[12], i * 4, 0);
        }
        generateAndTest( expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_Ldr() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], expectedValues[i]);
            testValues[i] = true;
        }
        asm.push(ARMv8Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        for (int i = 0; i < 10; i++) {
            asm.add(ARMv8Assembler.ConditionFlag.Always, false, ARMv8.cpuRegisters[i], ARMv8.cpuRegisters[i], i * 2, 0);
            asm.movw(ARMv8Assembler.ConditionFlag.Always, ARMv8.r12, i * 4);
            asm.movt(ARMv8Assembler.ConditionFlag.Always, ARMv8.r12, 0);
            asm.ldr(ARMv8Assembler.ConditionFlag.Always, 1, 1, 0, ARMv8.cpuRegisters[i], ARMv8.cpuRegisters[13], ARMv8.cpuRegisters[12], 0, 0);
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_Decq() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], expectedValues[i]);
            asm.decq(ARMv8.cpuRegisters[i]);
            expectedValues[i] -= 1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_Incq() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], expectedValues[i]);
            asm.incq(ARMv8.cpuRegisters[i]);
            expectedValues[i] += 1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_Subq() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], expectedValues[i]);
            if (i % 2 == 1) {
                asm.subq(ARMv8.cpuRegisters[i], 2 * expectedValues[i]);
                expectedValues[i] -= 2 * expectedValues[i];
            } else {
                asm.subq(ARMv8.cpuRegisters[i], expectedValues[i]);
                expectedValues[i] -= expectedValues[i];
            }
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_addq() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], expectedValues[i]);
            asm.addq(ARMv8.cpuRegisters[i], expectedValues[i]);
            expectedValues[i] += expectedValues[i];
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_Ldrsh() throws Exception {
        int[] testval = { 0x03020100, 0x8fed9ba9};
        int mask = 0xffff;
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // load r0 and r1 with sensible values for ignoring the loading of bytes.
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], testval[0]);
        asm.mov32BitConstant(ARMv8.cpuRegisters[1], testval[1]);
        asm.push(ARMv8Assembler.ConditionFlag.Always, 1 | 2); // values now lie on the stack
        // we now try to extract the "signed halfwords"
        // from the stack and place them into r0..r3
        for (int i = 0; i < 4; i++) {
            // in this test we are using the stack register as the base value!
            asm.ldrshw(ARMv8Assembler.ConditionFlag.Always, 1, 1, 0, ARMv8.cpuRegisters[i], ARMv8.cpuRegisters[13], i * 2);
            testValues[i] = true;
            if (i < 2) {
                expectedValues[i] = testval[0] & (mask << (16 * (i % 2)));
            } else {
                expectedValues[i] = testval[1] & (mask << (16 * (i % 2)));
            }
            if ((expectedValues[i] & 0x8000) != 0) {
                expectedValues[i] = expectedValues[i] - 0x10000; // sign extension workaround.
            }
            expectedValues[i] = expectedValues[i] >> 16 * (i % 2);
        }

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_StrdAndLdrd() throws Exception {
        MaxineARMTester.disableDebug();
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        for (int i = 0; i < 10; i += 2) {
            asm.codeBuffer.reset();
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], expectedValues[i]);
            asm.mov32BitConstant(ARMv8.cpuRegisters[i + 1], expectedValues[i + 1]);
            asm.strd(ARMv8Assembler.ConditionFlag.Always, ARMv8.cpuRegisters[i], ARMv8.r13, 0);
            asm.mov32BitConstant(ARMv8.cpuRegisters[i], 0);
            asm.mov32BitConstant(ARMv8.cpuRegisters[i + 1], 0);
            asm.ldrd(ARMv8Assembler.ConditionFlag.Always, ARMv8.cpuRegisters[i], ARMv8.r13, 0);
            testValues[i] = true;
            testValues[i + 1] = true;
            if (i != 0) {
                testValues[i - 1] = false;
                testValues[i - 2] = false;
            }
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
        }
    }

    public void ignore_PushAndPop() throws Exception {
        int registers = 1;
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        initialiseExpectedValues();
        for (int i = 0; i < 16; i++) {
            if (i % 2 == 0) {
                expectedValues[i] = i;
            } else {
                expectedValues[i] = -i;
            }
            if (i < 13) {
                testValues[i] = true;
            }
        }

        for (int bitmask = 1; bitmask <= 0xfff; bitmask = bitmask | (bitmask + 1), registers++) {
            asm.codeBuffer.reset();
            for (int i = 0; i < 13; i++) { // we are not breaking the stack (r13)
                asm.mov32BitConstant(ARMv8.cpuRegisters[i], expectedValues[i]); // 2 instructions movw, movt
                // all registers initialized.
            }
            asm.push(ARMv8Assembler.ConditionFlag.Always, bitmask); // store all registers referred to
            // by bitmask on the stack
            for (int i = 0; i < 13; i++) {
                asm.add(ARMv8Assembler.ConditionFlag.Always, false, ARMv8.cpuRegisters[i], ARMv8.cpuRegisters[i], 1, 0);
            }
            // r0..r12 should now all have +1 more than their previous values stored on the stack
            // restore the same registers that were placed on the stack
            asm.pop(ARMv8Assembler.ConditionFlag.Always, bitmask);
            for (int i = 0; i < 13; i++) {
                if (i < registers) {
                    expectedValues[i] = expectedValues[i];
                } else {
                    expectedValues[i] = expectedValues[i] + 1;
                }
            }
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
        }
    }

    public void ignore_MovRor() throws Exception {
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        for (int srcReg = 0; srcReg < 16; srcReg++) {
            for (int destReg = 0; destReg < 16; destReg++) {
                for (int shift = 0; shift <= 31; shift++) {
                    for (int i = 0; i < ARMv8Assembler.ConditionFlag.values().length; i++) { // test encodings
                        asm.movror(ARMv8Assembler.ConditionFlag.values()[i], false, ARMv8.cpuRegisters[destReg], ARMv8.cpuRegisters[srcReg], shift);
                        // rotate right two bits 0x30003fff?
                        assertTrue(asm.codeBuffer.getInt(0) == (0x01A00060 | (shift << 7) | (destReg << 12) | srcReg | ARMv8Assembler.ConditionFlag.values()[i].value() << 28));
                        asm.codeBuffer.reset();
                        asm.movror(ARMv8Assembler.ConditionFlag.values()[i], true, ARMv8.cpuRegisters[destReg], ARMv8.cpuRegisters[srcReg], shift);
                        // rotate right 30 bits? to get 0x0000ffff
                        assertTrue(asm.codeBuffer.getInt(0) == (0x01B00060 | (shift << 7) | srcReg | (destReg << 12) | ARMv8Assembler.ConditionFlag.values()[i].value() << 28));
                        asm.codeBuffer.reset();
                    }
                }
            }
        }
        int mask = 1;
        for (int shift = 1; shift <= 31; shift++) {
            asm.codeBuffer.reset();
            asm.movw(ARMv8Assembler.ConditionFlag.Always, ARMv8.cpuRegisters[0], 0xffff); // load 0x0000ffff
            asm.movt(ARMv8Assembler.ConditionFlag.Always, ARMv8.cpuRegisters[0], 0x0);
            asm.movror(ARMv8Assembler.ConditionFlag.Always, true, ARMv8.cpuRegisters[1], ARMv8.cpuRegisters[0], shift);
            // not ignoring ROR with ZEROshift as that needs to know the carry bit of the registerA RRX
            asm.movror(ARMv8Assembler.ConditionFlag.Always, true, ARMv8.cpuRegisters[2], ARMv8.cpuRegisters[1], 32 - shift);
            // rotate right 30 bits?
            // implies ... APSR.N = , APSR.Z = , APSR.C =

            expectedValues[0] = 0x0000ffff;
            testValues[0] = true;
            expectedValues[1] = (0x0000ffff >> shift) | (((expectedValues[0] & mask) << (32 - shift)));
            testValues[1] = true;
            expectedValues[2] = 0x0000ffff;
            testValues[2] = true;
            expectedValues[16] = 0x0;
            testValues[16] = false;
            setBitMask(16, MaxineARMTester.BitsFlag.NZCBits);
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
            mask = mask | (mask + 1);
        }
    }

    public void ignore_Movw() throws Exception {
        int value;
        setAllBitMasks(MaxineARMTester.BitsFlag.Lower16Bits);
        for (int destReg = 0; destReg < 13; destReg++) {
            resetIgnoreValues();
            testValues[destReg] = true;
            for (int j = 0; j < valueTestSet.length; j++) {
                value = valueTestSet[j];
                expectedValues[destReg] = value;
                asm.movw(ARMv8Assembler.ConditionFlag.Always, ARMv8.cpuRegisters[destReg], value);
                generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
                assert asm.codeBuffer.getInt(0) == (0x03000000 | (ARMv8Assembler.ConditionFlag.Always.value() << 28) | (destReg << 12) | (value & 0xfff) | ((value & 0xf000) << 4));
                asm.codeBuffer.reset();
            }
        }
    }

    public void ignore_Movt() throws Exception {
        int value;
        int j;
        setAllBitMasks(MaxineARMTester.BitsFlag.Upper16Bits);
        for (int destReg = 0; destReg < 13; destReg++) {
            resetIgnoreValues();
            testValues[destReg] = true;
            for (j = 0; j < valueTestSet.length; j++) {
                value = valueTestSet[j];
                expectedValues[destReg] = (value & 0xffff) << 16;
                asm.movt(ARMv8Assembler.ConditionFlag.Always, ARMv8.cpuRegisters[destReg], value & 0xffff);
                generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
                assert asm.codeBuffer.getInt(0) == (0x03400000 | (ARMv8Assembler.ConditionFlag.Always.value() << 28) | (destReg << 12) | (value & 0xfff) | (value & 0xf000) << 4);
                asm.codeBuffer.reset();
            }
        }
    }

    public void ignore_Flags() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMv8.cpuRegisters[0], 30);
        asm.sub(ARMv8Assembler.ConditionFlag.Always, true, ARMv8.cpuRegisters[0], ARMv8.cpuRegisters[0], 10, 0);
        asm.sub(ARMv8Assembler.ConditionFlag.Always, true, ARMv8.cpuRegisters[0], ARMv8.cpuRegisters[0], 10, 0);
        asm.sub(ARMv8Assembler.ConditionFlag.Always, true, ARMv8.cpuRegisters[0], ARMv8.cpuRegisters[0], 10, 0);
        asm.sub(ARMv8Assembler.ConditionFlag.Always, true, ARMv8.cpuRegisters[0], ARMv8.cpuRegisters[0], 10, 0);
        expectedValues[0] = -10;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void ignore_Ldrd() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < expectedLongValues.length; i++) {
            System.out.println(i + " " + expectedLongValues[i]);
            asm.mov64BitConstant(ARMv8.cpuRegisters[i * 2], ARMv8.cpuRegisters[(i * 2) + 1], expectedLongValues[i]);
            testValues[i] = true;
        }
        asm.push(ARMv8Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8);
        for (int i = 0; i < expectedLongValues.length * 2; i++) {
             asm.mov32BitConstant(ARMv8.cpuRegisters[i],0);
        }
        for (int i = 0; i < expectedLongValues.length; i++) {
            asm.movw(ARMv8Assembler.ConditionFlag.Always, ARMv8.r12, i * 8);
            asm.movt(ARMv8Assembler.ConditionFlag.Always, ARMv8.r12, 0);
            asm.ldrd(ConditionFlag.Always, 0, 0, 0, ARMv8.RSP.asRegister(), ARMv8.cpuRegisters[i * 2], ARMv8.r12);
        }
        generateAndTest(expectedLongValues, testValues, bitmasks);
    }

    public void test_casInt() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        CiRegister cmpReg = ARMv8.r0;
        CiRegister newReg = ARMv8.r1;

        // r0=10, r1=20, r2=30, r3=40, r4=50
        for (int i = 1; i < 5; i++) {
            masm.mov32BitConstant(ARMv8.cpuRegisters[i], (i + 1) * 10);
        }
        masm.mov32BitConstant(ARMv8.cpuRegisters[0], 50);
        masm.push(ARMv8Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16);
        CiAddress addr = new CiAddress(CiKind.Int, ARMv8.r13.asValue(), 20);
        masm.casIntAsmTest(newReg, cmpReg, addr);
        expectedValues[1] = 20;
        testValues[1] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_casLong() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        CiRegister cmpReg = ARMv8.r0;
        CiRegister newReg = ARMv8.r2;

        // r0=10, r1=0
        // r2=30, r3=0
        // r4=50, r5=0
        // r6=70, r7=0
        // r8=90, r9=0
        for (int i = 2; i < 10; i += 2) {
            masm.mov64BitConstant(ARMv8.cpuRegisters[i], ARMv8.cpuRegisters[i + 1], (i + 1) * 10);
        }
        masm.mov64BitConstant(ARMv8.cpuRegisters[0], ARMv8.cpuRegisters[1], 90);
        masm.push(ARMv8Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        CiAddress addr = new CiAddress(CiKind.Int, ARMv8.r13.asValue(), 32);
        masm.casLongAsmTest(newReg, cmpReg, addr);
        expectedValues[0] = 30;
        testValues[0] = true;
        expectedValues[1] = 0;
        testValues[1] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_decrementl() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        // r0=10, r1=20
        // r2=30, r3=40
        // r4=50, r5=60
        // r6=70, r7=80
        // r8=90, r9=100
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(ARMv8.cpuRegisters[i], (i + 1) * 10);
        }
        masm.push(ARMv8Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        CiAddress addr = new CiAddress(CiKind.Int, ARMv8.r13.asValue(), 16);
        masm.decrementl(addr, 10);
        masm.pop(ARMv8Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 20;
        testValues[1] = true;
        expectedValues[2] = 30;
        testValues[2] = true;
        expectedValues[3] = 40;
        testValues[3] = true;
        expectedValues[4] = 40;
        testValues[4] = true;
        expectedValues[5] = 60;
        testValues[5] = true;
        expectedValues[6] = 70;
        testValues[6] = true;
        expectedValues[7] = 80;
        testValues[7] = true;
        expectedValues[8] = 90;
        testValues[8] = true;
        expectedValues[9] = 100;
        testValues[9] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_incrementl() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMTester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        // r0=10, r1=20
        // r2=30, r3=40
        // r4=50, r5=60
        // r6=70, r7=80
        // r8=90, r9=100
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(ARMv8.cpuRegisters[i], (i + 1) * 10);
        }
        masm.push(ARMv8Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        CiAddress addr = new CiAddress(CiKind.Int, ARMv8.r13.asValue(), 16);
        masm.incrementl(addr, 10);
        masm.pop(ARMv8Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 20;
        testValues[1] = true;
        expectedValues[2] = 30;
        testValues[2] = true;
        expectedValues[3] = 40;
        testValues[3] = true;
        expectedValues[4] = 60;
        testValues[4] = true;
        expectedValues[5] = 60;
        testValues[5] = true;
        expectedValues[6] = 70;
        testValues[6] = true;
        expectedValues[7] = 80;
        testValues[7] = true;
        expectedValues[8] = 90;
        testValues[8] = true;
        expectedValues[9] = 100;
        testValues[9] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }*/
}
