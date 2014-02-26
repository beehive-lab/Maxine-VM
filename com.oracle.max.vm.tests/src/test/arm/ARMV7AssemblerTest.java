package test.arm;

import com.oracle.max.asm.target.armv7.ARMV7;
import com.oracle.max.asm.target.armv7.ARMV7Assembler;
import com.sun.cri.ci.*;
import com.sun.max.ide.MaxTestCase;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: yaman
 * Date: 10/12/13
 * Time: 14:55
 * To change this template use File | Settings | File Templates.
 */
public class ARMV7AssemblerTest extends MaxTestCase {
    ARMV7Assembler asm;
    CiTarget armv7;
    ARMCodeWriter code;

    static final class Pair {
        public final int first;
        public final int second;
        public Pair(int first, int second) {
            this.first = first;
            this.second = second;
        }
    }


    public ARMV7AssemblerTest() {
        // TODO: get correct spillSlotSize (using 4 for now)
        armv7 = new CiTarget(new ARMV7(),
                false,
                4,
                0,
                4096,
                0,
                false,
                false,
                false);

        asm = new ARMV7Assembler(armv7, null);
        code = null;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ARMV7AssemblerTest.class);
    }

    public void testPatchJumpTarget() throws Exception {

    }
    // public void adc(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int immed_8, final int rotate_amount)

    public void testAdc() throws Exception {
        // public CiRegister(int number, int encoding, int spillSlotSize, String name, RegisterFlag... flags)
      /*  int instructions[] = new int[3];
        int value,i,j,k;
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        for(j = 0; j < 17;j++) testvalues[j] = false;
        int srcReg = 0;
            for(int destReg = 4; destReg < 6; destReg++) {
                testvalues[destReg] = true;
                testvalues[destReg-1] = false;
                for(j = 14; j <16;j++){
                    asm.movt(ARMV7Assembler.ConditionFlag.Always,ARMV7.cpuRegisters[destReg],valueTestSet[j]);
                    asm.adc(ARMV7Assembler.ConditionFlag.Always,true,ARMV7.cpuRegisters[destReg],ARMV7.cpuRegisters[srcReg],valueTestSet[j] <<7,2);
                    // The left shift of 7 means that the valueTestValues used will always be less than 8bits (255)

                }
            }
        // testing conditionflags (signed/unsigned)
        for( i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.adc(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1], 0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02A10000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.adc(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1], 0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02B10000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
        */
    }
    private static int valueTestSet[] = {0,1,2,4,8,16,32,64,128,256,512,1024,2048,4096,8192,16384,
            32768,65535};
    private static long scratchTestSet[] = {0,1,0xff,0xffff,0xffffff,0xfffffff,0x00000000ffffffffL };
    // Each test should set the contents of this array appropriately, it enables the instruction under test to select the specific bit values for
    // comparison ie for example testing upper or lower 16bits for movt,movw and for testing specifc bits in the status register etc
    // concerning whether a carry has been set
    private static MaxineARMTester.BitsFlag bitmasks[] = {MaxineARMTester.BitsFlag.All32Bits,    MaxineARMTester.BitsFlag.All32Bits,
            MaxineARMTester.BitsFlag.All32Bits,MaxineARMTester.BitsFlag.All32Bits,MaxineARMTester.BitsFlag.All32Bits,MaxineARMTester.BitsFlag.All32Bits,
            MaxineARMTester.BitsFlag.All32Bits,MaxineARMTester.BitsFlag.All32Bits,MaxineARMTester.BitsFlag.All32Bits,MaxineARMTester.BitsFlag.All32Bits,
            MaxineARMTester.BitsFlag.All32Bits,MaxineARMTester.BitsFlag.All32Bits,MaxineARMTester.BitsFlag.All32Bits,MaxineARMTester.BitsFlag.All32Bits,
            MaxineARMTester.BitsFlag.All32Bits,MaxineARMTester.BitsFlag.All32Bits,MaxineARMTester.BitsFlag.All32Bits};

    private static void setBitMasks(int i, MaxineARMTester.BitsFlag mask) {
            if ((i < 0)|| i>= bitmasks.length)  {
                 for(i = 0; i < bitmasks.length;i++)
                     bitmasks[i] = mask;
            }else bitmasks[i] = mask;
    }
    private static void setTestValues(int i,boolean value){
        if ((i < 0)|| i>= testvalues.length)  {
            for(i = 0; i < testvalues.length;i++)
                testvalues[i] = value;
        }else testvalues[i] = value;
    }
    private static long expectedValues[] = { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16}; // therse values will be udpdated
    // to those expected to be found in a register after simulated execution of code
    private static void initialiseExpectedValues() {
            for(int i = 0; i < 17;i++)expectedValues[i] = (long) i;
    }
    private static boolean  testvalues[] = new boolean[17];
    private boolean generateAndTest(int assemblerStatements, long expected[],boolean tests[],MaxineARMTester.BitsFlag masks[])
    {
        int instructions[] = new int[assemblerStatements];
        for(int j = 0; j < assemblerStatements;j++) {
            instructions[j] = asm.codeBuffer.getInt(j*4);
        }
        ARMCodeWriter code = new ARMCodeWriter(assemblerStatements,instructions);
        MaxineARMTester r = new MaxineARMTester(expected,tests,masks);

        r.assembleStartup();
        r.assembleEntry();
        r.compile();
        r.link();
        r.objcopy();
        return r.runSimulation();
    }
    public void testVCVT() {
        int assemblerStatements = 11;
        int i,instructions [] = new int [assemblerStatements];
        initialiseExpectedValues();
        setBitMasks(-1, MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1, false);
        System.out.println("TESTING  VCVT VMUL.... needs more extensive testing of encodings and asserts HIGH probability of bugs!!!");
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMV7.cpuRegisters[0], (int) 12);
        asm.mov32BitConstant(ARMV7.cpuRegisters[1], (int) 10);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r0);  // r2 has r0?
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, ARMV7.r1); // r4 and r5 contain r0 and r1
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always,ARMV7.d1,false,false,ARMV7.s0);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always,ARMV7.d2,false,false,ARMV7.s1);
        asm.vmul(ARMV7Assembler.ConditionFlag.Always,ARMV7.d1,ARMV7.d2,ARMV7.d1);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always,ARMV7.s0,true,false,ARMV7.d1);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r2,ARMV7.s0);

        expectedValues[0] = 12;testvalues[0] = true;
        expectedValues[1] = 10;testvalues[1] = true;
        expectedValues[2] = 120;testvalues[2] = true;


        generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);


    }
    public void testVLDRSTR() {

        int assemblerStatements = 15;
        int i,instructions [] = new int [assemblerStatements];
        initialiseExpectedValues();
        setBitMasks(-1, MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1, false);
        System.out.println("TESTING  VLDR  VSTR.... needs more extensive testing of encodings and asserts sample!!!");
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMV7.cpuRegisters[0], (int) 12);
        asm.mov32BitConstant(ARMV7.cpuRegisters[1], (int) 10);
        asm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512|1024|2048);               // 1 instruction
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r13, 0);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s0);  // r2 has r0?
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.d2, ARMV7.r13, 0) ;
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.d2); // r4 and r5 contain r0 and r1

        asm.vstr(ARMV7Assembler.ConditionFlag.Always, ARMV7.d2, ARMV7.r13, -8) ;
        asm.vstr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r13, -16);
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.d5, ARMV7.r13, -8);
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s31, ARMV7.r13, -16);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r6, ARMV7.d5);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r8,ARMV7.s31);
        expectedValues[0] = 12;testvalues[0] = true;
        expectedValues[1] = 10;testvalues[1] = true;
        expectedValues[2] = 12;testvalues[2] = true;

        expectedValues[4] = 12;testvalues[4] = true;
        expectedValues[5] = 10;testvalues[5] = true;
        expectedValues[6] = 12;testvalues[6] = true;
        expectedValues[7] = 10;testvalues[7] = true;
        expectedValues[8] = 12;testvalues[8] = true;

        generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
    }
    public void testVLDR() {

        int assemblerStatements = 9;
        int i,instructions [] = new int [assemblerStatements];
        initialiseExpectedValues();
        setBitMasks(-1, MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1, false);
        System.out.println("TESTING  VLDR .... needs more extensive tesating of encodings and asserts sample!!!");
        asm.codeBuffer.reset();

        asm.mov32BitConstant(ARMV7.cpuRegisters[0], (int) 12);
        asm.mov32BitConstant(ARMV7.cpuRegisters[1], (int) 10);
        asm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);               // 1 instruction
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s31, ARMV7.r13, 0);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s31);  // r2 has r0?
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.d2, ARMV7.r13, 0) ;

        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.d2); // r4 and r5 contain r0 and r1

        expectedValues[0] = 12;testvalues[0] = true;
        expectedValues[1] = 10;testvalues[1] = true;
        expectedValues[2] = 12;testvalues[2] = true;

        expectedValues[4] = 12;testvalues[4] = true;
        expectedValues[5] = 10;testvalues[5] = true;


        generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
    }
    public void testVMOV() {

        int assemblerStatements = 8;
        int i,instructions [] = new int [assemblerStatements];
        initialiseExpectedValues();
        setBitMasks(-1, MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1, false);
        System.out.println("TESTING  VMOV .... needs more extensive tesating of encodings and asserts sample!!!");
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMV7.cpuRegisters[0], (int) 12);
        asm.mov32BitConstant(ARMV7.cpuRegisters[1], (int) 10);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.d0, ARMV7.r0);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.d0); // r2 and r3 contain r0 and r1
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s5, ARMV7.r0);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.s5);
        expectedValues[0] = 12;testvalues[0] = true;
        expectedValues[1] = 10;testvalues[1] = true;
        expectedValues[2] = 12;testvalues[2] = true;
        expectedValues[3] = 10;testvalues[3] = true;
        expectedValues[4] = 12;testvalues[4] = true;

        generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
    }
    public void testFloatIngPointExperiments() throws Exception {
        /*
        Will create a few simple examples (using hardcoded emitInt()) to verify concepts/principles then
        will add the instructions
         */
        int assemblerStatements = 11;
        int i,instructions []= new int[assemblerStatements];
        initialiseExpectedValues();
        setBitMasks(-1, MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1, false);
        System.out.println("TESTING  FLOAT sample!!!");
        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMV7.cpuRegisters[0], (int) 12);
        asm.mov32BitConstant(ARMV7.cpuRegisters[1], (int) 10);
        asm.codeBuffer.emitInt(0xee000a10);
        asm.codeBuffer.emitInt(0xee001a90);
        asm.codeBuffer.emitInt(0xeeb81ac0);
        asm.codeBuffer.emitInt(0xeef81ae0);
        asm.codeBuffer.emitInt(0xee210a21);
        asm.codeBuffer.emitInt(0xeebd0a40);
        asm.codeBuffer.emitInt(0xee100a10);
        expectedValues[0] = 120;
        testvalues[0]= true;
        generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);



    }
    public void testSubReg() throws Exception {

        int assemblerStatements = 30;
        int i,instructions []= new int[assemblerStatements];
        initialiseExpectedValues();
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING  SUB of a register value need to test shifts of the register!!!");
        asm.codeBuffer.reset();


        for(i=0; i < 5; i++) {
            asm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);

        }
        for(i = 0; i < 5;i++)   {
            asm.sub(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.cpuRegisters[i+5],ARMV7.cpuRegisters[5-(i+1)],ARMV7.cpuRegisters[i],0,0);
            expectedValues[i+5] = expectedValues[5-(i+1)]-expectedValues[i];
            testvalues[i+5] = true;
        }
        generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
    }

    public void testMOV() throws Exception {

        int assemblerStatements = 30;
        int instructions []= new int[assemblerStatements];
        initialiseExpectedValues();
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING  MOV no rotation!!!");
        asm.codeBuffer.reset();

        for(int i=0; i < 5; i++) {
            asm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[i+5], ARMV7.cpuRegisters[i]);
            expectedValues[i+5] = expectedValues[i];
            testvalues[i] = true;
            testvalues[i+5] = true;

        }

        generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
    }
    public void testSub() throws Exception {

        int assemblerStatements = 30;
        int instructions []= new int[assemblerStatements];
        initialiseExpectedValues();
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING  SUB of an immediate value!!!");
        asm.codeBuffer.reset();

        for(int i=0; i < 10; i++) {
            asm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);
            asm.sub(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[i],i*2,0);
            expectedValues[i] = expectedValues[i] - i*2;
            testvalues[i] = true;

        }
        generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
    }


    public void teststr() throws Exception
    {

        int assemblerStatements = 62;
        int instructions []= new int[assemblerStatements];
        long mask = 0xff;
        initialiseExpectedValues();
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING  STR!!!");

        asm.codeBuffer.reset();
        asm.mov32BitConstant(ARMV7.cpuRegisters[12],0);
        for(int i = 0; i < 10;i++) {
            /* Basically, write some (expected) values to registers, store them, change the register contents,
            load them back from memory, if they match the expected values then we're ok
            Note the use of P indexed, and U add bits, we naughtily use the stack register as a memory region where we could
            write to, but we don't adjust the index register, ie W bit is unset??
             */
            asm.mov32BitConstant(ARMV7.cpuRegisters[i], (int) expectedValues[i]);   // 2 instructions
            testvalues[i] = true;
            asm.str(ARMV7Assembler.ConditionFlag.Always,1,0,0,ARMV7.cpuRegisters[i],ARMV7.cpuRegisters[13],ARMV7.cpuRegisters[12],i*4,0);
            asm.mov32BitConstant(ARMV7.cpuRegisters[i],-2*(int)(expectedValues[i]));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always,1,0,0,ARMV7.cpuRegisters[i],ARMV7.cpuRegisters[13],ARMV7.cpuRegisters[12],i*4,0);
        }


        generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);

    }

    public void testldr() throws Exception
    {
        /*
        str and ldr refer to the store/load register

         */
        int assemblerStatements = 61;
        int instructions []= new int[assemblerStatements];
        long mask = 0xff;
        initialiseExpectedValues();
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING  LDR!!!");

        asm.codeBuffer.reset();
        for(int i = 0; i < 10;i++) {                  // 2*10 = 20
            asm.mov32BitConstant(ARMV7.cpuRegisters[i], (int) expectedValues[i]);   // 2 instructions
            testvalues[i] = true;

        }
        asm.push(ARMV7Assembler.ConditionFlag.Always,1|2|4|8|16|32|64|128|256|512);               // 1 instruction
        for(int i = 0; i < 10;i++)   { // 3 * 10 = 30
            asm.add(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.cpuRegisters[i],ARMV7.cpuRegisters[i],i*2,0);
            asm.movw(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, i * 4);
            asm.movt(ARMV7Assembler.ConditionFlag.Always,ARMV7.r12,0);
            asm.ldr(ARMV7Assembler.ConditionFlag.Always,1,1,0,ARMV7.cpuRegisters[i],ARMV7.cpuRegisters[13],
                    ARMV7.cpuRegisters[12],0,0);
        }


        generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);

    }

    public void testdecq() throws Exception
    {
        /*
        strd and ldrd refer to the store/load register dual
        the versions encoded here are for a base register value plus/minus an 8 bit immediate.
        int instructions[] = new int
         */
        int assemblerStatements = 50;
        int instructions []= new int[assemblerStatements];
        long mask = 0xff;
        initialiseExpectedValues();
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING  DECQ");

        asm.codeBuffer.reset();
        for(int i = 0; i < 10;i++) {
            asm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);   // 2 instructions
            asm.decq(ARMV7.cpuRegisters[i]);               // 3 instruction
            expectedValues[i] -= 1;
            testvalues[i] = true;

        }

        generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);

    }
    public void testincq() throws Exception
    {
        /*
        strd and ldrd refer to the store/load register dual
        the versions encoded here are for a base register value plus/minus an 8 bit immediate.
        int instructions[] = new int
         */
        int assemblerStatements = 30;
        int instructions []= new int[assemblerStatements];
        long mask = 0xff;
        initialiseExpectedValues();
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING  INCQ");

        asm.codeBuffer.reset();
        for(int i = 0; i < 10;i++) {
            asm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);   // 2 instructions
            asm.incq(ARMV7.cpuRegisters[i]);               // 3 instruction
            expectedValues[i] += 1;
            testvalues[i] = true;

        }
        generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);







    }
    public void testsubq() throws Exception
    {
        /*
        strd and ldrd refer to the store/load register dual
        the versions encoded here are for a base register value plus/minus an 8 bit immediate.
        int instructions[] = new int
         */
        int assemblerStatements = 50;
        int instructions []= new int[assemblerStatements];
        long mask = 0xff;
        initialiseExpectedValues();
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING  SUBQ -- please test with a -ve constant NOT DONE");

        asm.codeBuffer.reset();
        for(int i = 0; i < 10;i++) {
            asm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);   // 2 instructions
            if(i%2 == 1 ) {
                asm.subq(ARMV7.cpuRegisters[i],(int)(2*expectedValues[i]));               // 3 instruction
                expectedValues[i] -= (2*expectedValues[i]);
            }else {
                asm.subq(ARMV7.cpuRegisters[i],(int)expectedValues[i]);
                expectedValues[i] -= expectedValues[i];
            }
            testvalues[i] = true;

        }
        generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);






    }
    public void testaddq() throws Exception
    {
        /*
        strd and ldrd refer to the store/load register dual
        the versions encoded here are for a base register value plus/minus an 8 bit immediate.
        int instructions[] = new int
         */
        int assemblerStatements = 50;
        int instructions []= new int[assemblerStatements];
        long mask = 0xff;
        initialiseExpectedValues();
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING  ADDQ -- please test with a -ve constant NOT DONE");

        asm.codeBuffer.reset();
        for(int i = 0; i < 10;i++) {
            asm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);   // 2 instructions
            asm.addq(ARMV7.cpuRegisters[i],(int)expectedValues[i]);               // 3 instruction
            expectedValues[i] += expectedValues[i];
            testvalues[i] = true;

        }

        for(int j = 0; j < assemblerStatements;j++) {
            instructions[j] = asm.codeBuffer.getInt(j*4);
        }
        ARMCodeWriter.debug = false;

        code = new ARMCodeWriter(assemblerStatements,instructions);
        MaxineARMTester r = new MaxineARMTester(expectedValues,testvalues,bitmasks);

        r.assembleStartup();
        r.assembleEntry();
        r.compile();
        r.link();
        r.objcopy();
        r.runSimulation();





    }
    public void testldrsh() throws Exception
    {
        /*
        strd and ldrd refer to the store/load register dual
        the versions encoded here are for a base register value plus/minus an 8 bit immediate.
        int instructions[] = new int
         */
        int assemblerStatements = 9;
        int instructions []= new int[assemblerStatements];
        long testval [] = {0x03020100L,0x8fed9ba9L};
        long mask = 0xffff;
        initialiseExpectedValues();
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING  LDRSH -- please test with a -ve offset NOT DONE");

        asm.codeBuffer.reset();
        // load r0 and r1 with sensible values for testing the loading of bytes.
        asm.mov32BitConstant(ARMV7.cpuRegisters[0],(int)testval[0]);
        asm.mov32BitConstant(ARMV7.cpuRegisters[1],(int)testval[1]);
        asm.push(ARMV7Assembler.ConditionFlag.Always,1|2);   // values now lie on the stack

        for(int i = 0; i < 4;i++) {
            asm.ldrshw(ARMV7Assembler.ConditionFlag.Always,1,1,0,ARMV7.cpuRegisters[i],ARMV7.cpuRegisters[13],i*2); // stack pointer advanced by 8
            testvalues[i] = true;
            if(i < 2)
                expectedValues[i] =  (testval[0]& (mask<< (16*(i%2)))) >> 16*(i);
            else
                expectedValues[i] =  (testval[1]& (mask<< (16*(i%2)))) >> 16*(i%2);
            if((expectedValues[i] & 0x8000) != 0) {
                expectedValues[i] =  expectedValues[i] | 0xffff0000L; // sign extension for negative vals
                expectedValues[i] =   expectedValues[i] -0x100000000L;
            }

        }

        for(int j = 0; j < assemblerStatements;j++) {
            instructions[j] = asm.codeBuffer.getInt(j*4);
        }
        ARMCodeWriter.debug = false;

        code = new ARMCodeWriter(assemblerStatements,instructions);
        MaxineARMTester r = new MaxineARMTester(expectedValues,testvalues,bitmasks);

        r.assembleStartup();
        r.assembleEntry();
        r.compile();
        r.link();
        r.objcopy();
        r.runSimulation();





    }
    public void testldrb() throws Exception
    {
        /*
        strd and ldrd refer to the store/load register dual
        the versions encoded here are for a base register value plus/minus an 8 bit immediate.
        int instructions[] = new int
         */
        int assemblerStatements = 13;
        int instructions []= new int[assemblerStatements];
        long testval [] = {0x03020100L,0xffedcba9L};
        long mask = 0xff;
        initialiseExpectedValues();
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING  LDRDB -- please test with a shifted -ve offset NOT DONE");

        asm.codeBuffer.reset();
            // load r0 and r1 with sensible values for testing the loading of bytes.
        asm.mov32BitConstant(ARMV7.cpuRegisters[0],(int)testval[0]);
        asm.mov32BitConstant(ARMV7.cpuRegisters[1],(int)testval[1]);
        asm.push(ARMV7Assembler.ConditionFlag.Always,1|2);   // values now lie on the stack

        for(int i = 0; i < 8;i++) {
            asm.ldrb(ARMV7Assembler.ConditionFlag.Always,1,1,0,ARMV7.cpuRegisters[i],ARMV7.cpuRegisters[13],i); // stack pointer advanced by 8
            testvalues[i] = true;
            if(i < 4)
                expectedValues[i] =  (testval[0]& (mask<< (8*(i%4)))) >> 8*(i);
            else
                expectedValues[i] =  (testval[1]& (mask<< (8*(i%4)))) >> 8*(i%4
                );

        }

        for(int j = 0; j < assemblerStatements;j++) {
            instructions[j] = asm.codeBuffer.getInt(j*4);
        }
        ARMCodeWriter.debug = false;

        code = new ARMCodeWriter(assemblerStatements,instructions);
        MaxineARMTester r = new MaxineARMTester(expectedValues,testvalues,bitmasks);

        r.assembleStartup();
        r.assembleEntry();
        r.compile();
        r.link();
        r.objcopy();
        r.runSimulation();





    }
    public void teststrdAndldrd() throws Exception
    {
        /*
        strd and ldrd refer to the store/load register dual
        the versions encoded here are for a base register value plus/minus an 8 bit immediate.
        int instructions[] = new int
         */
        int instructions []= new int[10];
        initialiseExpectedValues();
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING STRD and LDRD -- please test with a negative offset NOT DONE");
        for(int i = 0; i < 10;i+=2) {
            System.out.println("REG "+i+ " expected vals "+ expectedValues[i]+ " " + expectedValues[i+1] );
            asm.codeBuffer.reset();
            asm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);
            asm.mov32BitConstant(ARMV7.cpuRegisters[i+1],(int)expectedValues[i+1]);  // load a vlaue into 2 registers
            asm.strd(ARMV7Assembler.ConditionFlag.Always,ARMV7.cpuRegisters[i],ARMV7.r13,0); // put them on the stack on the stack!
            asm.mov32BitConstant(ARMV7.cpuRegisters[i],0);     // zero the value in the registers
            asm.mov32BitConstant(ARMV7.cpuRegisters[i+1],0);
            asm.ldrd(ARMV7Assembler.ConditionFlag.Always,ARMV7.cpuRegisters[i],ARMV7.r13,0);
            testvalues[i] = true;
            testvalues[i+1] = true;
            if(i != 0)  {
                testvalues[i-1] = false;
                testvalues[i-2] = false;
            }
            for(int j = 0; j < 10;j++) {
                 instructions[j] = asm.codeBuffer.getInt(j*4);
            }
            ARMCodeWriter.debug = false;

            code = new ARMCodeWriter(10,instructions);
            MaxineARMTester r = new MaxineARMTester(expectedValues,testvalues,bitmasks);

            r.assembleStartup();
            r.assembleEntry();
            r.compile();
            r.link();
            r.objcopy();
            r.runSimulation();


        }

    }
    public void testpushAndPop() throws Exception {
        int instructions[]  = new int [42];
        long value;
        int i,registers = 1;
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);


        System.out.println("TESTING PUSH AND POP");
        for(i = 0; i < 16;i++) {
            expectedValues[i] = (long) i; // re-initialise in case another test has been run before us
            if(i < 13) testvalues[i] = true;  // test register values r0..r12
        }

        for(int bitmask = 1; bitmask <= 0xfff; bitmask = bitmask |(bitmask+1),registers++)  {
            asm.codeBuffer.reset();
            initialiseExpectedValues();
            System.out.println("REGISTERS " + registers + " BITMASK "+ bitmask);
            for(i = 0; i < 13;i++)   {  // we are not breaking the stack (r13)
                asm.mov32BitConstant(ARMV7.cpuRegisters[i], (int) expectedValues[i]); // 2 instructions movw, movt
                // all registers initialised.
            }
            asm.push(ARMV7Assembler.ConditionFlag.Always,bitmask); // store all registers referred to
            // by bitmask on the stack
            for(i = 0; i < 13;i++) asm.add(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.cpuRegisters[i],ARMV7.cpuRegisters[i],1,0);
            // r0..r12 should now all have +1 more than their previous values stored on the stack

            // restore the same registers that were placed on the stack
            asm.pop(ARMV7Assembler.ConditionFlag.Always,bitmask);

            for(i = 0; i < 13; i++) {
                if(i < registers)
                    expectedValues[i] = (long) i;
                else
                    expectedValues[i] = (long) (i+1);
            }
            for(i = 0; i < (2*13+1+13+1);i++) instructions[i] = asm.codeBuffer.getInt(i*4);

            ARMCodeWriter.debug = false;
            code = new ARMCodeWriter(41,instructions);

            try {
                MaxineARMTester r = new MaxineARMTester(expectedValues,testvalues,bitmasks);

                r.assembleStartup();
                r.assembleEntry();
                r.compile();
                r.link();
                r.objcopy();
                r.runSimulation();
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
                System.exit(-1);
            }

        }
        ARMCodeWriter.debug = false;



    }
    public void testadd() throws Exception {
        int instructions[] = new int[3];
        long value;
        int  i,j,immediate,result;

        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        initialiseExpectedValues();
        setTestValues(-1,false);

        System.out.println("TESTING ADD ");

        for(int srcReg = 0; srcReg < 3;srcReg++)
           for(int destReg = 0; destReg < 3;destReg++)    {

               if(destReg > 0) testvalues[destReg-1] = false;
               testvalues[destReg] = true;


               for ( i = 0; i < scratchTestSet.length;i++) {

                    asm.codeBuffer.reset();
                    value = scratchTestSet[i];
                    asm.movw(ARMV7Assembler.ConditionFlag.Always,ARMV7.cpuRegisters[srcReg],(int)(value &0xffff));  // load 0x0000ffff
                    asm.movt(ARMV7Assembler.ConditionFlag.Always,ARMV7.cpuRegisters[srcReg],(int)((value >>16)&0xffff));
                    asm.add(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.cpuRegisters[destReg],ARMV7.cpuRegisters[srcReg],
                            0, 0);
                    instructions[0] = asm.codeBuffer.getInt(0);
                    instructions[1] = asm.codeBuffer.getInt(4);
                    instructions[2] = asm.codeBuffer.getInt(8);

                    if(scratchTestSet[i] >= 0x80000000L) {
                        //System.out.println("negative ");
                        expectedValues[destReg] = scratchTestSet[i] -0x100000000L;
                    }
                    else expectedValues[destReg] = scratchTestSet[i];


                    //System.out.println("SRCREG " + srcReg+ " DESTREG " + destReg + " EXPECT " + expectedValues[destReg]);

                    //ARMCodeWriter.debug = true;
                    code = new ARMCodeWriter(3,instructions);
                    ARMCodeWriter.debug = false;
                    MaxineARMTester r = new MaxineARMTester(expectedValues,testvalues,bitmasks);
                    r.assembleStartup();
                    r.assembleEntry();
                    r.compile();
                    r.link();
                    r.objcopy();
                    r.runSimulation();
                }
               if(destReg == 2)testvalues[2] = false;
           }

    }
    /*public void testsetUpScratch() throws Exception {
        int instructions[] = new int[1];
        int value,i,j;
        setBitMasks(-1,MaxineARMTester.BitsFlag.All32Bits);
        for(j = 0;j <17;j++)  testvalues[j] = false;
        testvalues[12-1] = true;
        i = 0;

            for( j = 0; j< scratchTestSet.length;j++) {

                //value < 65536;value++) {
                value = scratchTestSet[j];
                expectedValues[destReg] = ((long)value);
                asm.setUpScratch(new CiAddress(), value);
                instructions[0] = asm.codeBuffer.getInt(0);

                code = new ARMCodeWriter(1,instructions);
                MaxineARMTester r = new MaxineARMTester(expectedValues,testvalues,bitmasks);
                r.assembleStartup();
                r.assembleEntry();
                r.compile();
                r.link();
                r.objcopy();
                r.runSimulation();

                assertTrue(asm.codeBuffer.getInt(0) == (0x03400000 | (ARMV7Assembler.ConditionFlag.Always.value() <<28) |(destReg << 12)| (value & 0xfff) | ((value & 0xf000) << 4)));
                asm.codeBuffer.reset();
            }
        }

    } */


    public void testMovror() throws Exception {
        int instructions[] = new int[4];
        int value,i,j,shift;
        setBitMasks(-1, MaxineARMTester.BitsFlag.All32Bits);
        setTestValues(-1,false);
        System.out.println("TESTING MOVROR  -- register") ;
        for(int srcReg = 0; srcReg < 16;srcReg++)
            for(int destReg = 0; destReg < 16;destReg++)
                for(shift = 0; shift <= 31;shift++)
                    for(i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) { // test encodings
                        asm.movror(ARMV7Assembler.ConditionFlag.values()[i], false, ARMV7.cpuRegisters[destReg], ARMV7.cpuRegisters[srcReg],
                        shift); // rotate right two bits 0x30003fff?

                        assertTrue(asm.codeBuffer.getInt(0) == ( 0x01A00060|(shift<<7)|(destReg << 12)|srcReg |ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
                        asm.codeBuffer.reset();
                        asm.movror(ARMV7Assembler.ConditionFlag.values()[i], true, ARMV7.cpuRegisters[destReg], ARMV7.cpuRegisters[srcReg],shift);  /// rotate right 30 bits?  to get 0x0000ffff
                        assertTrue(asm.codeBuffer.getInt(0) == ( 0x01B00060 |(shift<<7)|(srcReg)|(destReg << 12)| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
                        asm.codeBuffer.reset();
        }
        long mask = 1;

        for(shift = 1; shift <= 31;shift++) {


            System.out.println("MOVROR SHIFT " + shift + " MASK " + mask);
            asm.codeBuffer.reset();
            asm.movw(ARMV7Assembler.ConditionFlag.Always,ARMV7.cpuRegisters[0],0xffff);  // load 0x0000ffff
            asm.movt(ARMV7Assembler.ConditionFlag.Always,ARMV7.cpuRegisters[0],0x0);
            asm.movror(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[1], ARMV7.cpuRegisters[0],shift);

            // not testing ROR with ZEROshift as that needs to know the carry bit of the registerA RRX
            asm.movror(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[2], ARMV7.cpuRegisters[1],32-shift);  /// rotate right 30 bits?
            //  implies ... APSR.N = , APSR.Z = , APSR.C =

            expectedValues[0] = 0x0000ffff;testvalues[0] = true;
            expectedValues[1] = (0x0000ffff >> (shift))| (((expectedValues[0]&mask) << (32-shift))); testvalues[1] = true;
            expectedValues[2] = 0x0000ffff;testvalues[2] = true;
            expectedValues[16] = 0x0; testvalues[16] = false;
            if (expectedValues[1] >= 0x80000000L) {
                //System.out.println("negative ");
                expectedValues[1] = expectedValues[1] -0x100000000L;
            }

            setBitMasks(16,MaxineARMTester.BitsFlag.NZCBits);

            for(i = 0; i < 4;i++) {instructions[i] =  asm.codeBuffer.getInt(i*4);
              //  System.out.println("INSTRUCTION in HEX " + Integer.toString(instructions[i],16));
            }


            code = new ARMCodeWriter(4,instructions);
            MaxineARMTester r = new MaxineARMTester(expectedValues,testvalues,bitmasks);
            r.assembleStartup();
            r.assembleEntry();
            r.compile();
            r.link();
            r.objcopy();
            r.runSimulation();
            mask = mask | (mask + 1);
        }

    }

    public void testmovw() throws Exception {
        int instructions[] = new int[1];
        int value,i,j;
        setBitMasks(-1,MaxineARMTester.BitsFlag.Lower16Bits);
        for(j = 0;j <17;j++)  testvalues[j] = false;

            /* APN we are only doing condition code testing for .Always
               we might extend this testing later
               ARMV7Assembler.ConditionFlag.values()[i]
               only testing the movw to registers 0 to 12 inclusive
             */
        for(int destReg = 0;destReg < 13;destReg++)                {
            System.out.println("MOVW DESTREG " + destReg);
            if(destReg > 0) {
                testvalues[destReg-1] = false;
            }
            testvalues[destReg] = true;
            for( j = 0; j< valueTestSet.length;j++) {

                //value < 65536;value++) {
                 value = valueTestSet[j];
                 expectedValues[destReg] = value;
                 asm.movw(ARMV7Assembler.ConditionFlag.Always,ARMV7.cpuRegisters[destReg],value);
                 instructions[0] = asm.codeBuffer.getInt(0);



                 code = new ARMCodeWriter(1,instructions);
                 MaxineARMTester r = new MaxineARMTester(expectedValues,testvalues,bitmasks);
	             r.assembleStartup();
	             r.assembleEntry();
	             r.compile();
	             r.link();
	             r.objcopy();
	             r.runSimulation();

                 assertTrue(asm.codeBuffer.getInt(0) == (0x03000000 | (ARMV7Assembler.ConditionFlag.Always.value() <<28) |(destReg << 12)| (value & 0xfff) | ((value & 0xf000) << 4)));
                 asm.codeBuffer.reset();
            }
        }

    }

    public void testmovt() throws Exception {
        int instructions[] = new int[1];
        int value,i,j;
        setBitMasks(-1,MaxineARMTester.BitsFlag.Upper16Bits);
        for(j = 0;j <17;j++)  testvalues[j] = false;
        i = 0;
            /* APN we are only doing condition code testing for .Always
                we might extend this testing later
               only testing the movw to registers 0 to 12 inclusive
             */
        for(int destReg = 0;destReg < 13;destReg++)                {
            System.out.println("MOVT DESTREG " + destReg);
            if(destReg > 0) {
                testvalues[destReg-1] = false;
            }
            testvalues[destReg] = true;
            for( j = 0; j< valueTestSet.length;j++) {

                //value < 65536;value++) {
                value = valueTestSet[j];
                expectedValues[destReg] = ((long)value) << 16;
                asm.movt(ARMV7Assembler.ConditionFlag.Always, ARMV7.cpuRegisters[destReg], value);
                instructions[0] = asm.codeBuffer.getInt(0);

                code = new ARMCodeWriter(1,instructions);
                MaxineARMTester r = new MaxineARMTester(expectedValues,testvalues,bitmasks);
                r.assembleStartup();
                r.assembleEntry();
                r.compile();
                r.link();
                r.objcopy();
                r.runSimulation();

                assertTrue(asm.codeBuffer.getInt(0) == (0x03400000 | (ARMV7Assembler.ConditionFlag.Always.value() <<28) |(destReg << 12)| (value & 0xfff) | ((value & 0xf000) << 4)));
                asm.codeBuffer.reset();
            }
        }

    }


    public void testAdclsl() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.adclsl(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00A10002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.adclsl(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00B10002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testAdd() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.add(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1], 0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02810000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.add(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1], 0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02910000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    /*
    public void testAddror() throws Exception {

        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.addror(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00810062 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.addror(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00910062 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testBiclsr() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.biclsr(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01C10332 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.biclsr(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01D10332 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testCmnasr() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.cmnasr(ARMV7Assembler.ConditionFlag.values()[i], armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01700251 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }

    }

    public void testCmnror() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.cmnror(ARMV7Assembler.ConditionFlag.values()[i], armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01700271 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testCmpasr() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.cmpasr(ARMV7Assembler.ConditionFlag.values()[i], armv7.arch.registers[0], armv7.arch.registers[1],
                    0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01500041 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testEorlsr() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.eorlsr(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00210332| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.eorlsr(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00310332 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    */
    /*public void testMvnror() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.mvnror(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01E00271| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.mvnror(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01F00271 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    } */

    /*public void testOrrlsl() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.orrlsl(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01810002| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.orrlsl(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01910002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    } */

    /*public void testRsb() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.rsb(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02610000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.rsb(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02710000| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }
      */
    /*public void testRsblsl() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.rsblsl(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00610002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.rsblsl(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00710002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    } */

    /*public void testRsc() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.rsc(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00E10002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.rsc(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00F10002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    } */
        /*
    public void testRsclsr() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.rsclsr(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00E10332 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.rsclsr(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00F10332 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }     */
            /*
    public void testSbcror() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.sbcror(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00C10372 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.sbcror(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00D10372 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }         */


     /*
    public void testTst() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.tst(ARMV7Assembler.ConditionFlag.values()[i], armv7.arch.registers[0], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x03100000| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testTstlsr() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.tstlsr(ARMV7Assembler.ConditionFlag.values()[i], armv7.arch.registers[0], armv7.arch.registers[0], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01100020| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testSmlal() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.smlal(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00E10293| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.smlal(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00F10293 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testUmull() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.umull(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00810293| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.umull(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00910293 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testLdradd() throws Exception {

    }

    public void testLdrsubw() throws Exception {

    }

    public void testLdraddrorpost() throws Exception {

    }

    public void testStrsubasr() throws Exception {

    }

    public void testStrsubrorw() throws Exception {

    }

    public void testStraddpost() throws Exception {

    }

    public void testSwi() throws Exception {

    }

    public void testCheckConstraint() throws Exception {

    } */
}
