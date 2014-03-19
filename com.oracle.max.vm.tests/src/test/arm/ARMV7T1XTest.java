package test.arm;

import static com.oracle.max.asm.target.armv7.ARMV7.*;

import com.oracle.max.asm.Buffer;
import com.oracle.max.asm.target.armv7.ARMV7MacroAssembler;
import com.oracle.max.vm.ext.t1x.*;

import com.oracle.max.asm.target.armv7.ARMV7;
import com.oracle.max.asm.target.armv7.ARMV7Assembler;

import com.oracle.max.vm.ext.t1x.armv7.ARMV7T1XCompilation;
import com.sun.cri.ci.CiTarget;
import com.sun.max.ide.MaxTestCase;
import com.sun.max.io.Files;
import com.sun.max.program.Trace;
import com.sun.max.program.option.OptionSet;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.VMOption;
import com.sun.max.vm.compiler.CompilationBroker;
import com.sun.max.vm.compiler.RuntimeCompiler;
import com.sun.max.vm.hosted.JavaPrototype;
import com.sun.max.vm.hosted.VMConfigurator;
import com.sun.max.vm.profile.MethodInstrumentation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 /**
 * Created with IntelliJ IDEA.
 * User: andyn
 * Date: 07/03/14
 * Time: 11:24
 * To change this template use File | Settings | File Templates.
 */
public class ARMV7T1XTest  extends MaxTestCase {
    ARMV7Assembler asm;
    CiTarget armv7;
    ARMCodeWriter code;
    T1X t1x;
    ARMV7T1XCompilation theCompiler;
    /*protected final T1XCompilationFactory t1XCompilationFactory;
    private final ThreadLocal<T1XCompilation> compilation = new ThreadLocal<T1XCompilation>() {
        @Override
    protected T1XCompilation initialValue() {
            return t1XCompilationFactory.newT1XCompilation(T1X.this);
        }
    };*/


    //static T1X t1x = CompilationBroker.instantiateCompiler(new String("T1X"));
    static final class Pair {
        public final int first;
        public final int second;
        public Pair(int first, int second) {
            this.first = first;
            this.second = second;
        }
    }
    private static final OptionSet options = new OptionSet(false);
    private static  VMConfigurator vmConfigurator = null;
    private static boolean initialised = false;

    private static String[] expandArguments(String[] args) throws IOException {
        List<String> result = new ArrayList<String>(args.length);
        for (String arg : args) {
            if (arg.charAt(0) == '@') {
                File file = new File(arg.substring(1));
                result.addAll(Files.readLines(file));
            } else {
                result.add(arg);
            }
        }
        return result.toArray(new String[result.size()]);
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

    private long [] generateAndTest(int assemblerStatements, long expected[],boolean tests[],MaxineARMTester.BitsFlag masks[])
    {
        int instructions[] = new int[assemblerStatements];
        for(int j = 0; j < assemblerStatements;j++) {
            instructions[j] = theCompiler.getMacroAssemblerUNITTEST().codeBuffer.getInt(j*4);
        }
        ARMCodeWriter.debug = false;

        ARMCodeWriter code = new ARMCodeWriter(assemblerStatements,instructions);
        MaxineARMTester r = new MaxineARMTester(expected,tests,masks);

        r.assembleStartup();
        r.assembleEntry();
        r.compile();
        r.link();
        r.objcopy();
        return r.runRegisterSimulation();
    }
    public ARMV7T1XTest() {
        try {
        System.out.println("ARMV7T1XTest begin instantiation ................................");
            //args = VMOption.extractVMArgs(args);
            String args[] = new String[2];
            args[0] = new String("t1x");
            args[1] = new String("HelloWorld");
            if(options != null) options.parseArguments(args);
            System.out.println("vmConfigurator");
            if(vmConfigurator == null)
                   vmConfigurator= new VMConfigurator(options);
            //options.setValuesAgain();
            //final String[] arguments = expandArguments(options.getArguments());


            String compilerName = new String("com.oracle.max.vm.ext.t1x.T1X");
            RuntimeCompiler.baselineCompilerOption.setValue(compilerName);
            System.out.println("RuntimeCompiler.baselineCompilerOption.setValue");




            if(initialised == false) {
                    vmConfigurator.create();
                    initialised = true;
            }
            System.out.println("vmConfigurator.created");

            // create the prototype

            //JavaPrototype.initialize(false);

            //System.out.println("JavaPrototype.initialize");

            //CompilationBroker cb = MaxineVM.vm().compilationBroker;
            //System.out.println("Compilation broker");
            //final RuntimeCompiler compiler = cb.baselineCompiler ;
            t1x = (T1X)CompilationBroker.instantiateCompiler(compilerName);
            //cb.optimizingCompiler.initialize(MaxineVM.Phase.HOSTED_COMPILING);
              //  cb.baselineCompiler.initialize(MaxineVM.Phase.HOSTED_COMPILING);




            System.out.println("intialise t1x");
            theCompiler = ((ARMV7T1XCompilation)t1x.getT1XCompilationUNITTEST());

            System.out.println("NORMAL NORMAL NORMAL ");

            System.out.println("!!!!!got theCompiler");




        }catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        junit.textui.TestRunner.run(ARMV7T1XTest.class);

    }

        /**
         *
         * Method: initFrame(ClassMethodActor method, CodeAttribute codeAttribute)
         *
         */
        public void testInitFrame() throws Exception  {
//TODO: Test goes here...

        }

        /**
         *
         * Method: decStack(int numberOfSlots)
         *
         */
        public void testDecStack() throws Exception {
//TODO: Test goes here...
            //int i,instructions [] = new int [assemblerStatements];
            int assemblerStatements;
            int instructions [] = null;
            long []registerValues = null;

            ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();
            theCompiler.incStack(3);
            masm.mov(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.r0,ARMV7.r13); // copy stack value into r0
            theCompiler.decStack(1);
            masm.mov(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.r1,ARMV7.r13); // copy stack value onto r1
            theCompiler.decStack(2);
            masm.mov(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.r2,ARMV7.r13);

            assemblerStatements =  masm.codeBuffer.position()/4;
            instructions = new int [assemblerStatements];

            registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
            for(int i = 0; i < 16;i++) {
                System.out.println("REGISTER " + i + " VALUE " + registerValues[i]) ;
                assert(2*(registerValues[1]-registerValues[0]) == (registerValues[2]-registerValues[1]));
            }
            System.out.println("decStack passed");

        }

        /**
         *
         * Method: incStack(int numberOfSlots)
         *
         */
        public void testIncStack() throws Exception {
            int assemblerStatements;
            int instructions [] = null;
            long []registerValues = null;

            ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();
            masm.mov(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.r0,ARMV7.r13); // copy stack value into r0
            theCompiler.incStack(1);
            masm.mov(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.r1,ARMV7.r13); // copy stack value onto r1
            theCompiler.incStack(2);
            masm.mov(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.r2,ARMV7.r13);

            assemblerStatements =  masm.codeBuffer.position()/4;
            instructions = new int [assemblerStatements];

            registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
            for(int i = 0; i < 16;i++) {
                System.out.println("REGISTER " + i + " VALUE " + registerValues[i]) ;
                assert(2*(registerValues[0]-registerValues[1]) == (registerValues[1]-registerValues[2]));
            }
            System.out.println("incStack passed");



        }

        /**
         *
         * Method: adjustReg(CiRegister reg, int delta)
         *
         */
        public void testAdjustReg() throws Exception {
// adjustReg is protected but it directly calls incrementl.
            boolean success = true;
            int assemblerStatements;
            int instructions [] = null;
            long []registerValues = null;

            ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();
            masm.mov32BitConstant(ARMV7.r0,0);
            masm.mov32BitConstant(ARMV7.r1,1);
            masm.mov32BitConstant(ARMV7.r2,Integer.MIN_VALUE);
            masm.mov32BitConstant(ARMV7.r3,Integer.MAX_VALUE);
            masm.mov32BitConstant(ARMV7.r4,0);
            masm.mov32BitConstant(ARMV7.r5,0);
            masm.incrementl(ARMV7.r0,1);
            masm.incrementl(ARMV7.r1,-1);
            masm.incrementl(ARMV7.r2,-1);
            masm.incrementl(ARMV7.r3,1);
            masm.incrementl(ARMV7.r4,Integer.MAX_VALUE);
            masm.incrementl(ARMV7.r5,0);
            masm.mov32BitConstant(ARMV7.r6,-10);
            masm.incrementl(ARMV7.r6,-1);


            assemblerStatements =  masm.codeBuffer.position()/4;
            instructions = new int [assemblerStatements];

            registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
            for(int i = 0; i < 16;i++) {
                System.out.println("REGISTER " + i + " VALUE " + registerValues[i]) ;

            }
            expectedValues[0] = 1;  // 0+1 = 1
            expectedValues[1] = 0;  // 1 + -1 = 0
            expectedValues[2] = Integer.MAX_VALUE;  // MIN + -1 = MAX
            expectedValues[3] = Integer.MIN_VALUE; // MAX + 1 = MIN
            expectedValues[4] = Integer.MAX_VALUE;
            expectedValues[5] = 0;
            expectedValues[6] = -11;
            for(int i = 0; i < 7;i++)   {
                          if(registerValues[i] != expectedValues[i]) {
                              System.out.println("REGISTER " + i + " " + registerValues[i] + " EXPECTED " + expectedValues[i]);
                               success = false;
                          } else { System.out.println("REGISTER " + i + " AS EXPECTED");}
            }
            assert(success = true);
            System.out.println("testadjustReg passed");


        }

        /**
         *
         * Method: peekObject(CiRegister dst, int index)
         *
         */
        public void testPeekObject() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: pokeObject(CiRegister src, int index)
         *
         */
        public void testPokeObject() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: peekWord(CiRegister dst, int index)
         *
         */
        public void testPeekWord() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: pokeWord(CiRegister src, int index)
         *
         */
        
        public void testPokeWord() throws Exception {
//TODO: Test goes here...
        }



        /**
         *
         * Method: pokeInt(CiRegister src, int index)
         *
         */
        
        public void testPokeInt() throws Exception {
//TODO: Test goes here...
        }


    /**
     *
     * Method: assignLong(CiRegister dst, long value)
     *
     */

    public void testAssignLong() throws Exception {
//TODO: Test goes here...
        long []registerValues = null;
        boolean success = true;
        long gotVal= 0;
        int instructions [] = null;
        int i,assemblerStatements;
        expectedValues[0] = Long.MIN_VALUE;
        expectedValues[2] = Long.MAX_VALUE;
        expectedValues[4] = 0xabdef01023456789L;
        expectedValues[6] = 111;
        expectedValues[8] = 0;

        for( i = 0 ; i <10; i+=2) {
            theCompiler.assignmentTests(ARMV7.cpuRegisters[i],expectedValues[i]);

        }






        /* DEST REGISTER STORES THE LEAST SIGNIFICANT WORD AND DEST+1 STORES THE MOST SIGNIFICANT WORD OF THE 2WORD IE
        BIT LONG VALUE
         */
        ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();
        assemblerStatements =  masm.codeBuffer.position()/4;
        instructions = new int [assemblerStatements];
        registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
        for( i = 0; i < 10;i++) {
            if(i%2 == 0) {
                gotVal = 0;
                gotVal = 0xffffffffL&registerValues[i];
            }       else {
                //gotVal =  gotVal << 32;
                gotVal |= (0xffffffffL&registerValues[i]) << 32;
                if(gotVal == expectedValues[i-1]) {
                    System.out.println("OK got Correct Value");

                } else
                {
                    success = false;
                    System.out.println("FAILED incorrect value " + Long.toString(gotVal,16)+ " " + Long.toString(expectedValues[i-1],16));
                }
            }
        }
        assert(success == true);

    }
    /**
     *
     * Method: peekLong(CiRegister dst, int index)
     *
     */
    public void testPeekLong() throws Exception {
//TODO: Test goes here...
        /* general strategy for this test is to
        assign a long
        push it onto the stack then do a peekLong
        check we correctly read the value
         */
        long []registerValues = null;
        boolean success = true;
        long gotVal= 0;
        int instructions [] = null;
        int i,assemblerStatements;
        ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();

        expectedValues[0] = Long.MIN_VALUE;
        expectedValues[2] = Long.MAX_VALUE;
        expectedValues[4] = 0xabdef01023456789L;
        expectedValues[6] = 111;
        expectedValues[8] = 0;

        for( i = 0 ; i <10; i+=2) {
            theCompiler.assignmentTests(ARMV7.cpuRegisters[i],expectedValues[i]);

        }
        masm.push(ARMV7Assembler.ConditionFlag.Always,4|8); // this is to check/debug issues about wrong address loaded
        masm.push(ARMV7Assembler.ConditionFlag.Always,1|2); // index 4
        masm.push(ARMV7Assembler.ConditionFlag.Always,4|8); // index 3
        masm.push(ARMV7Assembler.ConditionFlag.Always,16|32); //index 2
        masm.push(ARMV7Assembler.ConditionFlag.Always,64|128);// index 1
        masm.push(ARMV7Assembler.ConditionFlag.Always,256|512);//index 0

        for(i = 0;i <= 10;i++)
            masm.mov32BitConstant(ARMV7.cpuRegisters[i],-25);
        theCompiler.peekLong(ARMV7.cpuRegisters[0],8);
        theCompiler.peekLong(ARMV7.cpuRegisters[2],6);
        theCompiler.peekLong(ARMV7.cpuRegisters[4],4);
        theCompiler.peekLong(ARMV7.cpuRegisters[6],2);
        theCompiler.peekLong(ARMV7.cpuRegisters[8],0);


        /* DEST REGISTER STORES THE LEAST SIGNIFICANT WORD AND DEST+1 STORES THE MOST SIGNIFICANT WORD OF THE 2WORD IE
        BIT LONG VALUE
         */
        assemblerStatements =  masm.codeBuffer.position()/4;
        instructions = new int [assemblerStatements];
        registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
        System.out.println("STACK "+ Long.toString(registerValues[13],16) + " SCRATCH "  + Long.toString(registerValues[12],16));
        for( i = 0; i < 10;i++) {
            System.out.println("REGISTER VALS "+ i +  " " + Long.toString(registerValues[i],16));
            if(i%2 == 0) {
                gotVal = 0;
                gotVal = 0xffffffffL&registerValues[i];
            }       else {
                //gotVal =  gotVal << 32;
                gotVal |= (0xffffffffL&registerValues[i]) << 32;
                if(gotVal == expectedValues[i-1]) {
                    System.out.println("OK got Correct Value");

                } else
                {
                    success = false;
                    System.out.println("FAILED incorrect value " + Long.toString(gotVal,16)+ " " + Long.toString(expectedValues[i-1],16));
                }
            }
        }
        assert(success == true);


    }

        /**
         *
         * Method: pokeLong(CiRegister src, int index)
         *
         */
        
        public void testPokeLong() throws Exception {
//TODO: Test goes here...
            /* general strategy for this test is to
        assign a long
        push it onto the stack then do a peekLong
        check we correctly read the value
         */
            long []registerValues = null;
            boolean success = true;
            long gotVal= 0;
            int instructions [] = null;
            int i,assemblerStatements;
            ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();

            expectedValues[0] = Long.MIN_VALUE;
            expectedValues[2] = Long.MAX_VALUE;
            expectedValues[4] = 0xabdef01023456789L;
            expectedValues[6] = 111;
            expectedValues[8] = 0;

            for( i = 0 ; i <10; i+=2) {
                theCompiler.assignmentTests(ARMV7.cpuRegisters[i],-99);

            }
            masm.push(ARMV7Assembler.ConditionFlag.Always,4|8); // this is to check/debug issues about wrong address loaded
            masm.push(ARMV7Assembler.ConditionFlag.Always,1|2); // index 4
            masm.push(ARMV7Assembler.ConditionFlag.Always,4|8); // index 3
            masm.push(ARMV7Assembler.ConditionFlag.Always,16|32); //index 2
            masm.push(ARMV7Assembler.ConditionFlag.Always,64|128);// index 1
            masm.push(ARMV7Assembler.ConditionFlag.Always,256|512);//index 0

            for(i = 0;i < 10;i+=2)
                theCompiler.assignmentTests(ARMV7.cpuRegisters[i],expectedValues[i]);
            theCompiler.pokeLong(ARMV7.cpuRegisters[0], 8);
            theCompiler.pokeLong(ARMV7.cpuRegisters[2], 6);
            theCompiler.pokeLong(ARMV7.cpuRegisters[4], 4);
            theCompiler.pokeLong(ARMV7.cpuRegisters[6], 2);
            theCompiler.pokeLong(ARMV7.cpuRegisters[8], 0);
            for(i = 0; i <=10;i++)masm.mov32BitConstant(ARMV7.cpuRegisters[i],-5);
            theCompiler.peekLong(ARMV7.cpuRegisters[0],8);
            theCompiler.peekLong(ARMV7.cpuRegisters[2],6);
            theCompiler.peekLong(ARMV7.cpuRegisters[4],4);
            theCompiler.peekLong(ARMV7.cpuRegisters[6],2);
            theCompiler.peekLong(ARMV7.cpuRegisters[8],0);

        /* DEST REGISTER STORES THE LEAST SIGNIFICANT WORD AND DEST+1 STORES THE MOST SIGNIFICANT WORD OF THE 2WORD IE
        BIT LONG VALUE
         */
            assemblerStatements =  masm.codeBuffer.position()/4;
            instructions = new int [assemblerStatements];
            registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
            for( i = 0; i < 10;i++) {
                System.out.println("REGISTER VALS "+ i +  " " + Long.toString(registerValues[i],16));
                if(i%2 == 0) {
                    gotVal = 0;
                    gotVal = 0xffffffffL&registerValues[i];
                }       else {
                    //gotVal =  gotVal << 32;
                    gotVal |= (0xffffffffL&registerValues[i]) << 32;
                    if(gotVal == expectedValues[i-1]) {
                        System.out.println("OK got Correct Value");

                    } else
                    {
                        success = false;
                        System.out.println("FAILED incorrect value " + Long.toString(gotVal,16)+ " " + Long.toString(expectedValues[i-1],16));
                    }
                }
            }
            assert(success == true);

        }
    /**
     *
     * Method: peekInt(CiRegister dst, int index)
     *
     */

    public void testPeekInt() throws Exception {
//TODO: Test goes here...

        long []registerValues = null;
        boolean success = true;
        long gotVal= 0;
        int instructions [] = null;
        int i,assemblerStatements;
        ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();

        expectedValues[0] = Integer.MIN_VALUE;
        expectedValues[1] = Integer.MAX_VALUE;
        expectedValues[2] = 0;
        expectedValues[3] = -1;
        expectedValues[4] = 40;
        expectedValues[5] = -40;

        for( i = 0 ; i <6; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);

        }
        masm.push(ARMV7Assembler.ConditionFlag.Always,4|8); // this is to check/debug issues about wrong address loaded
        masm.push(ARMV7Assembler.ConditionFlag.Always,1); // index 5
        masm.push(ARMV7Assembler.ConditionFlag.Always,2); // index 4
        masm.push(ARMV7Assembler.ConditionFlag.Always,4); // index 3
        masm.push(ARMV7Assembler.ConditionFlag.Always,8); // index 2
        masm.push(ARMV7Assembler.ConditionFlag.Always,16); // index 1
        masm.push(ARMV7Assembler.ConditionFlag.Always,32); // index 0


        for(i = 0;i <= 5;i++)
            masm.mov32BitConstant(ARMV7.cpuRegisters[i],-25);
        theCompiler.peekInt(ARMV7.cpuRegisters[0],5);
        theCompiler.peekInt(ARMV7.cpuRegisters[1],4);
        theCompiler.peekInt(ARMV7.cpuRegisters[2],3);
        theCompiler.peekInt(ARMV7.cpuRegisters[3],2);
        theCompiler.peekInt(ARMV7.cpuRegisters[4],1);
        theCompiler.peekInt(ARMV7.cpuRegisters[5],0);


        /* DEST REGISTER STORES THE LEAST SIGNIFICANT WORD AND DEST+1 STORES THE MOST SIGNIFICANT WORD OF THE 2WORD IE
        BIT LONG VALUE
         */
        assemblerStatements =  masm.codeBuffer.position()/4;
        instructions = new int [assemblerStatements];
        registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
        for( i = 0; i < 6;i++) {
            if(registerValues[i] == expectedValues[i]) {
                System.out.println("OK REG " + i + " got Correct Value");

            } else
            {
                success = false;
                System.out.println("FAILED incorrect value " + Long.toString(registerValues[i],16)+ " " + Long.toString(expectedValues[i],16));
            }

        }
        assert(success == true);

    }
        /**
         *
         * Method: peekDouble(CiRegister dst, int index)
         *
         */
        
        public void testPeekDouble() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: pokeDouble(CiRegister src, int index)
         *
         */
        
        public void testPokeDouble() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: peekFloat(CiRegister dst, int index)
         *
         */
        
        public void testPeekFloat() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: pokeFloat(CiRegister src, int index)
         *
         */
        
        public void testPokeFloat() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: assignObjectReg(CiRegister dst, CiRegister src)
         *
         */
        
        public void testAssignObjectReg() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: assignWordReg(CiRegister dst, CiRegister src)
         *
         */
        
        public void testAssignWordReg() throws Exception {
//TODO: Test goes here...
        }


        /**
         *
         * Method: assignObject(CiRegister dst, Object value)
         *
         */
        
        public void testAssignObject() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: loadInt(CiRegister dst, int index)
         *
         */
        
        public void testLoadInt() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: loadLong(CiRegister dst, int index)
         *
         */
        
        public void testLoadLong() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: loadWord(CiRegister dst, int index)
         *
         */
        
        public void testLoadWord() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: loadObject(CiRegister dst, int index)
         *
         */
        
        public void testLoadObject() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: storeInt(CiRegister src, int index)
         *
         */
        
        public void testStoreInt() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: storeLong(CiRegister src, int index)
         *
         */
        
        public void testStoreLong() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: storeWord(CiRegister src, int index)
         *
         */
        
        public void testStoreWord() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: storeObject(CiRegister src, int index)
         *
         */
        
        public void testStoreObject() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: assignInt(CiRegister dst, int value)
         *
         */
        
        public void testAssignInt() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: assignFloat(CiRegister dst, float value)
         *
         */
        
        public void testAssignFloat() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: assignDouble(CiRegister dst, double value)
         *
         */
        
        public void testAssignDouble() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: callDirect()
         *
         */
        
        public void testCallDirect() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: callIndirect(CiRegister target, int receiverStackIndex)
         *
         */
        
        public void testCallIndirect() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: nullCheck(CiRegister src)
         *
         */
        
        public void testNullCheck() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: emitPrologue()
         *
         */
        
        public void testEmitPrologue() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: emitUnprotectMethod()
         *
         */
        
        public void testEmitUnprotectMethod() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: emitEpilogue()
         *
         */
        
        public void testEmitEpilogue() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: do_preVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor)
         *
         */
        
        public void testDo_preVolatileFieldAccess() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: do_postVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor)
         *
         */
        
        public void testDo_postVolatileFieldAccess() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: do_tableswitch()
         *
         */
        
        public void testDo_tableswitch() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: do_lookupswitch()
         *
         */
        
        public void testDo_lookupswitch() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: cleanup()
         *
         */
        
        public void testCleanup() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: branch(int opcode, int targetBCI, int bci)
         *
         */
        
        public void testBranch() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: addObjectLiteralPatch(int index, int patchPos)
         *
         */
        
        public void testAddObjectLiteralPatch() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: fixup()
         *
         */
        
        public void testFixup() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: movqDisp(int dispPos, int dispFromCodeStart)
         *
         */
        
        public void testMovqDisp() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: findDataPatchPosns(MaxTargetMethod source, int dispFromCodeStart)
         *
         */
        
        public void testFindDataPatchPosns() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: invokeKind(SignatureDescriptor signature)
         *
         */
        
        public void testInvokeKind() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: assignmentTests(CiRegister reg, long value)
         *
         */
        
        public void testAssignmentTests() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: addJCC(ConditionFlag cc, int pos, int targetBCI)
         *
         */
        
        public void testAddJCC() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: addJMP(int pos, int targetBCI)
         *
         */
        
        public void testAddJMP() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: addJumpTableEntry(int pos, int jumpTablePos, int targetBCI)
         *
         */
        
        public void testAddJumpTableEntry() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: addLookupTableEntry(int pos, int key, int lookupTablePos, int targetBCI)
         *
         */
        
        public void testAddLookupTableEntry() throws Exception {
//TODO: Test goes here...
        }

        /**
         *
         * Method: addObjectLiteral(int dispPos, int index)
         *
         */
        
        public void testAddObjectLiteral() throws Exception {
//TODO: Test goes here...
        }


        /**
         *
         * Method: alignDirectCall(int callPos)
         *
         */
        
        public void testAlignDirectCall() throws Exception {
//TODO: Test goes here...
/*
try {
   Method method = ARMV7T1XCompilation.getClass().getMethod("alignDirectCall", int.class);
   method.setAccessible(true);
   method.invoke(<Object>, <Parameters>);
} catch(NoSuchMethodException e) {
} catch(IllegalAccessException e) {
} catch(InvocationTargetException e) {
}
*/
        }

        /**
         *
         * Method: framePointerAdjustment()
         *
         */
        
        public void testFramePointerAdjustment() throws Exception {
//TODO: Test goes here...
/*
try {
   Method method = ARMV7T1XCompilation.getClass().getMethod("framePointerAdjustment");
   method.setAccessible(true);
   method.invoke(<Object>, <Parameters>);
} catch(NoSuchMethodException e) {
} catch(IllegalAccessException e) {
} catch(InvocationTargetException e) {
}
*/
        }

    }




