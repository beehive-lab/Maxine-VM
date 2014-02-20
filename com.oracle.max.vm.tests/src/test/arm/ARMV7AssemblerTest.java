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

        for(int srcReg = 0; srcReg < 13;srcReg++)
           for(int destReg = 0; destReg < 13;destReg++)    {

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


                    expectedValues[destReg] = scratchTestSet[i];


                    System.out.println("EXPECT " + expectedValues[destReg]);

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

    public void testSub() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.sub(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02410000| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.sub(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02510000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

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

    }
}
