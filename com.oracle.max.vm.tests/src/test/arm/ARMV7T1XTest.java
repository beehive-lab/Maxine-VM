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
import com.sun.max.vm.actor.Actor;
import com.sun.max.vm.actor.member.StaticMethodActor;
import com.sun.max.vm.classfile.CodeAttribute;
import com.sun.max.vm.classfile.LineNumberTable;
import com.sun.max.vm.classfile.LocalVariableTable;
import com.sun.max.vm.classfile.StackMapTable;
import com.sun.max.vm.classfile.constant.PoolConstant;
import com.sun.max.vm.classfile.constant.Utf8Constant;
import com.sun.max.vm.compiler.CompilationBroker;
import com.sun.max.vm.compiler.RuntimeCompiler;
import com.sun.max.vm.hosted.JavaPrototype;
import com.sun.max.vm.hosted.VMConfigurator;
import com.sun.max.vm.profile.MethodInstrumentation;
import com.sun.max.vm.type.Kind;
import com.sun.max.vm.type.SignatureDescriptor;

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
    StaticMethodActor anMethod = null;
    CodeAttribute codeAttr = null;

    public void initialiseFrameForCompilation() {
        /* initialise StaticMethodActor
           SignatureDescriptor
         */
        System.out.println("The frame initialisation in ARMV7T1XTest is a bodge just to allow testing");
        codeAttr = new CodeAttribute(null,
                    new byte[50],
                (char) 10, // TODO: compute max stack
                (char) 8,
                CodeAttribute.NO_EXCEPTION_HANDLER_TABLE,
                LineNumberTable.EMPTY,
                LocalVariableTable.EMPTY,
                null);
        String intrinsic = new String();

        anMethod = new StaticMethodActor( null,          SignatureDescriptor.create("(Ljava/util/Map;)V") , Actor.JAVA_METHOD_FLAGS,codeAttr,intrinsic);

    }

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
        MaxineARMTester.debug = false;
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
            boolean success = true;
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
                if(2*(registerValues[1]-registerValues[0]) != (registerValues[2]-registerValues[1]))
                     success = false;
            }
            assert(success == true);
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
                masm.mov32BitConstant(ARMV7.cpuRegisters[i],i);

            }
            masm.push(ARMV7Assembler.ConditionFlag.Always,4|8); // this is to check/debug issues about wrong address loaded
            masm.push(ARMV7Assembler.ConditionFlag.Always,1); // index 5
            masm.push(ARMV7Assembler.ConditionFlag.Always,2); // index 4
            masm.push(ARMV7Assembler.ConditionFlag.Always,4); // index 3
            masm.push(ARMV7Assembler.ConditionFlag.Always,8); // index 2
            masm.push(ARMV7Assembler.ConditionFlag.Always,16); // index 1
            masm.push(ARMV7Assembler.ConditionFlag.Always,32); // index 0


            for(i = 0;i <= 5;i++)
                masm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);
            theCompiler.pokeInt(ARMV7.cpuRegisters[0], 5);
            theCompiler.pokeInt(ARMV7.cpuRegisters[1], 4);
            theCompiler.pokeInt(ARMV7.cpuRegisters[2], 3);
            theCompiler.pokeInt(ARMV7.cpuRegisters[3], 2);
            theCompiler.pokeInt(ARMV7.cpuRegisters[4], 1);
            theCompiler.pokeInt(ARMV7.cpuRegisters[5], 0);
            for(i = 0;i <= 5;i++)
                masm.mov32BitConstant(ARMV7.cpuRegisters[i],-25);
            theCompiler.peekInt(ARMV7.cpuRegisters[0], 5);
            theCompiler.peekInt(ARMV7.cpuRegisters[1], 4);
            theCompiler.peekInt(ARMV7.cpuRegisters[2], 3);
            theCompiler.peekInt(ARMV7.cpuRegisters[3], 2);
            theCompiler.peekInt(ARMV7.cpuRegisters[4], 1);
            theCompiler.peekInt(ARMV7.cpuRegisters[5], 0);

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
         * Method: pokeDouble(CiRegister src, int index)
         *
         */
        
        public void testPokeDouble() throws Exception {
//TODO: Test goes here...
            long []registerValues = null;
            boolean success = true;
            long gotVal= 0;
            int instructions [] = null;
            int i,assemblerStatements;
            ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();

            expectedValues[0] = (long)Double.doubleToRawLongBits(Double.MIN_VALUE);
            expectedValues[1] = (long)Double.doubleToRawLongBits(Double.MAX_VALUE);
            expectedValues[2] = (long)Double.doubleToRawLongBits(0.0);
            expectedValues[3] = (long)Double.doubleToRawLongBits(-1.0);
            expectedValues[4] = (long)Double.doubleToRawLongBits(-100.75);

            for( i = 0 ; i <5; i++) {
                theCompiler.assignDoubleTest(ARMV7.allRegisters[16+i],Double.longBitsToDouble(expectedValues[i]));
            }
            masm.vpush(ARMV7Assembler.ConditionFlag.Always,ARMV7.d0,ARMV7.d0); // this is to check/debug issues about wrong address loaded
            masm.vpush(ARMV7Assembler.ConditionFlag.Always,ARMV7.d0,ARMV7.d0); // index 8
            masm.vpush(ARMV7Assembler.ConditionFlag.Always,ARMV7.d1,ARMV7.d1); // index 6
            masm.vpush(ARMV7Assembler.ConditionFlag.Always,ARMV7.d2,ARMV7.d2); // index 4
            masm.vpush(ARMV7Assembler.ConditionFlag.Always,ARMV7.d3,ARMV7.d3); // index 2
            masm.vpush(ARMV7Assembler.ConditionFlag.Always,ARMV7.d4,ARMV7.d4); // index 0



            for(i = 0;i <= 9;i++)
                masm.mov32BitConstant(ARMV7.cpuRegisters[i],-25);
            expectedValues[0] = (long)Double.doubleToRawLongBits(-100.1);
            expectedValues[1] = (long)Double.doubleToRawLongBits(-200.2);
            expectedValues[2] = (long)Double.doubleToRawLongBits(1.123456);
            expectedValues[3] = (long)Double.doubleToRawLongBits(99.9876543);
            expectedValues[4] = (long)Double.doubleToRawLongBits(3000000.000);

            for( i = 0 ; i <5; i++) {
                System.out.println("DOUBLES "+ Double.longBitsToDouble(expectedValues[i]) + " AS HEX "+ Long.toString(expectedValues[i],16));
                theCompiler.assignDoubleTest(ARMV7.allRegisters[16+i],Double.longBitsToDouble(expectedValues[i]));
            }
            theCompiler.pokeDouble(ARMV7.d0,8);
            theCompiler.pokeDouble(ARMV7.d1, 6);
            theCompiler.pokeDouble(ARMV7.d2,4);
            theCompiler.pokeDouble(ARMV7.d3,2);
            theCompiler.pokeDouble(ARMV7.d4,0);
            for(i = 0;i < 5;i++)
                theCompiler.assignDoubleTest(ARMV7.allRegisters[16+i],Double.longBitsToDouble((long)i));
            theCompiler.peekDouble(ARMV7.d0,8);
            theCompiler.peekDouble(ARMV7.d1, 6);
            theCompiler.peekDouble(ARMV7.d2,4);
            theCompiler.peekDouble(ARMV7.d3,2);
            theCompiler.peekDouble(ARMV7.d4,0);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r0,ARMV7.d0);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r2,ARMV7.d1);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r4,ARMV7.d2);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r6,ARMV7.d3);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r8,ARMV7.d4);


        /* DEST REGISTER STORES THE LEAST SIGNIFICANT WORD AND DEST+1 STORES THE MOST SIGNIFICANT WORD OF THE 2WORD IE
        BIT LONG VALUE
         */
            assemblerStatements =  masm.codeBuffer.position()/4;
            instructions = new int [assemblerStatements];
            registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
            for( i = 0; i < 10;i+=2) {
                gotVal = 0;
                gotVal = 0xffffffffL&registerValues[i];
                gotVal |= (0xffffffffL&registerValues[i+1]) << 32;
                if(gotVal == expectedValues[i/2]) {
                    System.out.println("OK got Correct Value "+ Double.longBitsToDouble(expectedValues[i/2]));

                } else
                {
                    success = false;
                    System.out.println("FAILED incorrect value " +
                            Long.toString(gotVal,16)+ " " + Long.toString(expectedValues[i/2],16) +
                            " EXPECTED " + Double.longBitsToDouble(expectedValues[i/2]) + " GOT " + Double.longBitsToDouble(gotVal));
                }



            }
            assert(success == true);
        }

        /**
         *
         * Method: peekFloat(CiRegister dst, int index)
         *
         */
        
        public void testPeekFloat() throws Exception {
//TODO: Test goes here...
            long []registerValues = null;
            boolean success = true;
            long gotVal= 0;
            int instructions [] = null;
            int i,assemblerStatements;
            ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();

            expectedValues[0] = (long)Float.floatToRawIntBits(Float.MIN_VALUE);
            expectedValues[1] = (long)Float.floatToRawIntBits(Float.MAX_VALUE);
            expectedValues[2] = (long)Float.floatToRawIntBits(0.0f);
            expectedValues[3] = (long)Float.floatToRawIntBits(-1.0f);
            expectedValues[4] = (long)Float.floatToRawIntBits(2.5f);
            expectedValues[5] = (long)Float.floatToRawIntBits(-100.75f);

            for( i = 0 ; i <6; i++) {
                masm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);
                System.out.println(" AS LONGS "+ expectedValues[i]+ " AS FLOATS "+ Float.intBitsToFloat((int)expectedValues[i]));

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
            theCompiler.peekFloat(ARMV7.s0,5);
            theCompiler.peekFloat(ARMV7.s1,4);
            theCompiler.peekFloat(ARMV7.s2, 3);
            theCompiler.peekFloat(ARMV7.s3,2);
            theCompiler.peekFloat(ARMV7.s4,1);
            theCompiler.peekFloat(ARMV7.s5, 0);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r0,ARMV7.s0);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r1,ARMV7.s1);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r2,ARMV7.s2);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r3,ARMV7.s3);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r4,ARMV7.s4);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r5,ARMV7.s5);


        /* DEST REGISTER STORES THE LEAST SIGNIFICANT WORD AND DEST+1 STORES THE MOST SIGNIFICANT WORD OF THE 2WORD IE
        BIT LONG VALUE
         */
            assemblerStatements =  masm.codeBuffer.position()/4;
            instructions = new int [assemblerStatements];
            registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
            for( i = 0; i < 6;i++) {
                if(registerValues[i] == expectedValues[i]) {
                    System.out.println("OK REG " + i + " got Correct Value " + Float.intBitsToFloat((int)expectedValues[i]));

                } else
                {
                    success = false;
                    System.out.println("FAILED incorrect value " + Long.toString(registerValues[i],16)+
                            " " + Long.toString(expectedValues[i],16)+ " EXPECTED "+ Float.intBitsToFloat((int) expectedValues[i])+
                    " GOT " + Float.intBitsToFloat((int)registerValues[i]));
                }

            }
            assert(success == true);

        }

        /**
         *
         * Method: pokeFloat(CiRegister src, int index)
         *
         */
        
        public void testPokeFloat() throws Exception {
//TODO: Test goes here...
            long []registerValues = null;
            boolean success = true;
            long gotVal= 0;
            int instructions [] = null;
            int i,assemblerStatements;
            ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();

            expectedValues[0] = (long)Float.floatToRawIntBits(Float.MIN_VALUE);
            expectedValues[1] = (long)Float.floatToRawIntBits(Float.MAX_VALUE);
            expectedValues[2] = (long)Float.floatToRawIntBits(0.0f);
            expectedValues[3] = (long)Float.floatToRawIntBits(-1.0f);
            expectedValues[4] = (long)Float.floatToRawIntBits(2.5f);
            expectedValues[5] = (long)Float.floatToRawIntBits(-100.75f);

            for( i = 0 ; i <6; i++) {
                masm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);
                System.out.println(" AS LONGS "+ expectedValues[i]+ " AS FLOATS "+ Float.intBitsToFloat((int)expectedValues[i]));

            }
            masm.push(ARMV7Assembler.ConditionFlag.Always,4|8); // this is to check/debug issues about wrong address loaded
            masm.push(ARMV7Assembler.ConditionFlag.Always,1); // index 5
            masm.push(ARMV7Assembler.ConditionFlag.Always,2); // index 4
            masm.push(ARMV7Assembler.ConditionFlag.Always,4); // index 3
            masm.push(ARMV7Assembler.ConditionFlag.Always,8); // index 2
            masm.push(ARMV7Assembler.ConditionFlag.Always,16); // index 1
            masm.push(ARMV7Assembler.ConditionFlag.Always,32); // index 0

            float value = -111.111111f;
            for( i = 0; i < 6;i++) {
                expectedValues[i] = (long)Float.floatToRawIntBits(value);
                value = value + -1.2f;
                masm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);
                masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.allRegisters[i+32],ARMV7.cpuRegisters[i]);
            }

            theCompiler.pokeFloat(ARMV7.s0,5);
            theCompiler.pokeFloat(ARMV7.s1,4);
            theCompiler.pokeFloat(ARMV7.s2, 3);
            theCompiler.pokeFloat(ARMV7.s3,2);
            theCompiler.pokeFloat(ARMV7.s4,1);
            theCompiler.pokeFloat(ARMV7.s5, 0);
            theCompiler.peekFloat(s6,5);
            theCompiler.peekFloat(s7,4);
            theCompiler.peekFloat(s8,3);
            theCompiler.peekFloat(s9,2);
            theCompiler.peekFloat(s10,1);
            theCompiler.peekFloat(s11,0);


            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r0,ARMV7.s6);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r1,ARMV7.s7);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r2,ARMV7.s8);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r3,ARMV7.s9);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r4,ARMV7.s10);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r5,ARMV7.s11);


        /* DEST REGISTER STORES THE LEAST SIGNIFICANT WORD AND DEST+1 STORES THE MOST SIGNIFICANT WORD OF THE 2WORD IE
        BIT LONG VALUE
         */
            assemblerStatements =  masm.codeBuffer.position()/4;
            instructions = new int [assemblerStatements];
            registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
            for( i = 0; i < 6;i++) {
                if(registerValues[i] == expectedValues[i]) {
                    System.out.println("OK REG " + i + " got Correct Value " + Float.intBitsToFloat((int)expectedValues[i]));

                } else
                {
                    success = false;
                    System.out.println("FAILED incorrect value " + Long.toString(registerValues[i],16)+
                            " " + Long.toString(expectedValues[i],16)+ " EXPECTED "+ Float.intBitsToFloat((int)expectedValues[i])+
                            " GOT " + Float.intBitsToFloat((int)registerValues[i]));
                }

            }
            assert(success == true);
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
         * Method: assignDouble(CiRegister dst, double value)
         *
         */
        
        public void testAssignDouble() throws Exception {
//TODO: Test goes here...
            long []registerValues = null;
            boolean success = true;
            long gotVal= 0;
            int instructions [] = null;
            int i,assemblerStatements;
            ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();

            expectedValues[0] = (long)Double.doubleToRawLongBits(Double.MIN_VALUE);
            expectedValues[1] = (long)Double.doubleToRawLongBits(Double.MAX_VALUE);
            expectedValues[2] = (long)Double.doubleToRawLongBits(0.0);
            expectedValues[3] = (long)Double.doubleToRawLongBits(-1.0);
            expectedValues[4] = (long)Double.doubleToRawLongBits(-100.75);

            for( i = 0 ; i <5; i++) {
                System.out.println("DOUBLES "+ Double.longBitsToDouble(expectedValues[i]) + " AS HEX "+ Long.toString(expectedValues[i],16));
                theCompiler.assignDoubleTest(ARMV7.allRegisters[16+i],Double.longBitsToDouble(expectedValues[i]));
            }
            for(i = 0;i <= 9;i++)
                masm.mov32BitConstant(ARMV7.cpuRegisters[i],-25);


            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r0,ARMV7.d0);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r2,ARMV7.d1);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r4,ARMV7.d2);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r6,ARMV7.d3);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r8,ARMV7.d4);


        /* DEST REGISTER STORES THE LEAST SIGNIFICANT WORD AND DEST+1 STORES THE MOST SIGNIFICANT WORD OF THE 2WORD IE
        BIT LONG VALUE
         */
            assemblerStatements =  masm.codeBuffer.position()/4;
            instructions = new int [assemblerStatements];
            registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
            for( i = 0; i < 10;i+=2) {
                gotVal = 0;
                gotVal = 0xffffffffL&registerValues[i];
                gotVal |= (0xffffffffL&registerValues[i+1]) << 32;
                if(gotVal == expectedValues[i/2]) {
                    System.out.println("OK got Correct Value "+ Double.longBitsToDouble(expectedValues[i/2]));

                } else
                {
                    success = false;
                    System.out.println("FAILED incorrect value " +
                            Long.toString(gotVal,16)+ " " + Long.toString(expectedValues[i/2],16) +
                            " EXPECTED " + Double.longBitsToDouble(expectedValues[i/2]) + " GOT " + Double.longBitsToDouble(gotVal));
                }



            }
            assert(success == true);
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
    /**
     *
     * Method: peekDouble(CiRegister dst, int index)
     *
     */

    public void testPeekDouble() throws Exception {
//TODO: Test goes here...
        // testing assign, peekDouble ...
        long []registerValues = null;
        boolean success = true;
        long gotVal= 0;
        int instructions [] = null;
        int i,assemblerStatements;
        ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();

        expectedValues[0] = (long)Double.doubleToRawLongBits(Double.MIN_VALUE);
        expectedValues[1] = (long)Double.doubleToRawLongBits(Double.MAX_VALUE);
        expectedValues[2] = (long)Double.doubleToRawLongBits(0.0);
        expectedValues[3] = (long)Double.doubleToRawLongBits(-1.0);
        expectedValues[4] = (long)Double.doubleToRawLongBits(-100.75);

        for( i = 0 ; i <5; i++) {
            System.out.println("DOUBLES "+ Double.longBitsToDouble(expectedValues[i]) + " AS HEX "+ Long.toString(expectedValues[i],16));
            theCompiler.assignDoubleTest(ARMV7.allRegisters[16+i],Double.longBitsToDouble(expectedValues[i]));
        }
        masm.vpush(ARMV7Assembler.ConditionFlag.Always,ARMV7.d0,ARMV7.d0); // this is to check/debug issues about wrong address loaded
        masm.vpush(ARMV7Assembler.ConditionFlag.Always,ARMV7.d0,ARMV7.d0); // index 8
        masm.vpush(ARMV7Assembler.ConditionFlag.Always,ARMV7.d1,ARMV7.d1); // index 6
        masm.vpush(ARMV7Assembler.ConditionFlag.Always,ARMV7.d2,ARMV7.d2); // index 4
        masm.vpush(ARMV7Assembler.ConditionFlag.Always,ARMV7.d3,ARMV7.d3); // index 2
        masm.vpush(ARMV7Assembler.ConditionFlag.Always,ARMV7.d4,ARMV7.d4); // index 0



        for(i = 0;i <= 9;i++)
            masm.mov32BitConstant(ARMV7.cpuRegisters[i],-25);

        theCompiler.peekDouble(ARMV7.d0,8);
        theCompiler.peekDouble(ARMV7.d1, 6);
        theCompiler.peekDouble(ARMV7.d2,4);
        theCompiler.peekDouble(ARMV7.d3,2);
        theCompiler.peekDouble(ARMV7.d4,0);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r0,ARMV7.d0);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r2,ARMV7.d1);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r4,ARMV7.d2);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r6,ARMV7.d3);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always,ARMV7.r8,ARMV7.d4);


        /* DEST REGISTER STORES THE LEAST SIGNIFICANT WORD AND DEST+1 STORES THE MOST SIGNIFICANT WORD OF THE 2WORD IE
        BIT LONG VALUE
         */
        assemblerStatements =  masm.codeBuffer.position()/4;
        instructions = new int [assemblerStatements];
        registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
        for( i = 0; i < 10;i+=2) {
                gotVal = 0;
                gotVal = 0xffffffffL&registerValues[i];
                gotVal |= (0xffffffffL&registerValues[i+1]) << 32;
                if(gotVal == expectedValues[i/2]) {
                    System.out.println("OK got Correct Value "+ Double.longBitsToDouble(expectedValues[i/2]));

                } else
                {
                    success = false;
                    System.out.println("FAILED incorrect value " +
                            Long.toString(gotVal,16)+ " " + Long.toString(expectedValues[i/2],16) +
                            " EXPECTED " + Double.longBitsToDouble(expectedValues[i/2]) + " GOT " + Double.longBitsToDouble(gotVal));
                }



        }
        assert(success == true);
    }

    public void testdo_lconst() throws Exception {
        /*assignLong(scratch, value);
        incStack(2);
        pokeLong(scratch, 0);
        */
        long []registerValues = null;
        boolean success = true;
        long gotVal= 0;
        int instructions [] = null;
        int i,assemblerStatements;
        System.out.println("running testdo_lconst");

        ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();
        expectedValues[0] = 0xffffffffL&0xffffffff0000ffffL;
        expectedValues[1] = 0xffffffffL&(0xffffffff0000ffffL >>32);
        expectedValues[8] = 0;
        expectedValues[9] = 1;
        masm.mov(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.r2,ARMV7.r13); // copy stack pointer to r2
        masm.mov32BitConstant(ARMV7.r8,0);
        masm.mov32BitConstant(ARMV7.r9,1);
        // r8 and r9 are used as temporaries, they are pushed onto stack and popped back after the operation
        // we cannot use scratch on ARMV7 as its only 32bit  and we need 64.

        theCompiler.do_lconstTests(0xffffffff0000ffffL);
        masm.mov(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.r3,ARMV7.r13); // copy revised stack pointer to r3
        theCompiler.peekLong(ARMV7.r0,0);
        assemblerStatements =  masm.codeBuffer.position()/4;
        instructions = new int [assemblerStatements];
        registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
        gotVal = 0;
        gotVal = 0xffffffffL&registerValues[0];
        gotVal |= (0xffffffffL&registerValues[1]) << 32;
        System.out.println(Long.toHexString(registerValues[0]) + " " +Long.toHexString(registerValues[1]));
        System.out.println(Long.toHexString(gotVal) + " EXPECTED  0xffffffff0000ffffL")  ;
        System.out.println(Long.toHexString(registerValues[8]) + " EXPECTED "+Long.toHexString(expectedValues[8]));
        System.out.println(Long.toHexString(registerValues[9]) + " EXPECTED "+Long.toHexString(expectedValues[9]));
        System.out.println("STACK " + registerValues[2] + " "+ registerValues[3]);
        assert(gotVal == 0xffffffff0000ffffL);
        //assert(registerValues[8] == 0);
        //assert(registerValues[9] == 1);
        assert(registerValues[2] - registerValues[3] == 8);
         System.out.println("Passed testdo_lconst");
    }
    public void testdo_dconst() throws Exception {
        /*assignLong(scratch, value);
        incStack(2);
        pokeLong(scratch, 0);
        */
        long []registerValues = null;
        boolean success = true;
        long gotVal= 0;
        double myVal = 3.14123;
        int instructions [] = null;
        int i,assemblerStatements;
        System.out.println("running testdo_dconst");

        ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();
        gotVal = Double.doubleToRawLongBits(myVal);
        expectedValues[0] = 0xffffffffL&gotVal;
        expectedValues[1] = 0xffffffffL&(gotVal >>32);
        expectedValues[8] = 0;
        expectedValues[9] = 1;
        masm.mov(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.r2,ARMV7.r13); // copy stack pointer to r2
        masm.mov32BitConstant(ARMV7.r8,0);
        masm.mov32BitConstant(ARMV7.r9,1);
        // r8 and r9 are used as temporaries, they are pushed onto stack and popped back after the operation
        // we cannot use scratch on ARMV7 as its only 32bit  and we need 64.          NO LONGER RELEVANT r8/r9 not allocatable

        theCompiler.do_dconstTests(myVal);
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r3, ARMV7.r13); // copy revised stack pointer to r3
        theCompiler.peekLong(ARMV7.r0,0);
        assemblerStatements =  masm.codeBuffer.position()/4;
        instructions = new int [assemblerStatements];
        registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
        gotVal = 0;
        gotVal = 0xffffffffL&registerValues[0];
        gotVal |= (0xffffffffL&registerValues[1]) << 32;
        System.out.println(Long.toHexString(registerValues[0]) + " " +Long.toHexString(registerValues[1]));
        long tmp = Double.doubleToRawLongBits(myVal);
        System.out.println(Long.toHexString(gotVal) + " EXPECTED  " + Long.toHexString(tmp))  ;
        System.out.println(Long.toHexString(registerValues[8]) + " EXPECTED " + Long.toHexString(expectedValues[8]));
        System.out.println(Long.toHexString(registerValues[9]) + " EXPECTED "+Long.toHexString(expectedValues[9]));
        System.out.println("STACK " + registerValues[2] + " "+ registerValues[3]);
        assert(gotVal == Double.doubleToRawLongBits(myVal));
        //assert(registerValues[8] == 0);  r8 and r9 are not allocatable anymore ....
        //assert(registerValues[9] == 1);
        assert(registerValues[2] - registerValues[3] == 8);
        System.out.println("Passed testdo_dconst");
    }
    public void testdo_fconst() throws Exception {
        /*assignLong(scratch, value);
        incStack(2);
        pokeLong(scratch, 0);
        */
        long []registerValues = null;
        boolean success = true;
        long gotVal= 0;
        float myVal = 3.14123f;
        int instructions [] = null;
        int i,assemblerStatements;
        System.out.println("running testdo_fconst");

        ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();
        gotVal = (long)Float.floatToRawIntBits(myVal);
        expectedValues[0] = gotVal;


        masm.mov(ARMV7Assembler.ConditionFlag.Always,false,ARMV7.r2,ARMV7.r13); // copy stack pointer to r2
        masm.mov32BitConstant(ARMV7.r8,0);
        masm.mov32BitConstant(ARMV7.r9,1);
        // r8 and r9 are used as temporaries, they are pushed onto stack and popped back after the operation
        // we cannot use scratch on ARMV7 as its only 32bit  and we need 64.

        theCompiler.do_fconstTests(myVal);
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r3, ARMV7.r13); // copy revised stack pointer to r3
        theCompiler.peekInt(ARMV7.r0,0);
        assemblerStatements =  masm.codeBuffer.position()/4;
        instructions = new int [assemblerStatements];
        registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
        gotVal = 0;
        gotVal = registerValues[0];

        System.out.println(Long.toHexString(registerValues[0]));

        System.out.println(Long.toHexString(gotVal) + " EXPECTED  " + Integer.toHexString(Float.floatToRawIntBits(myVal)))  ;
        System.out.println("STACK " + registerValues[2] + " "+ registerValues[3]);
        assert(gotVal == (long)Float.floatToRawIntBits(myVal));
        assert(registerValues[2] - registerValues[3] == 4);
        System.out.println("Passed testdo_fconst");
    }
    public void testdo_load() throws Exception {
        /*assignLong(scratch, value);
        incStack(2);
        pokeLong(scratch, 0);
        */
        System.out.println("Cannot test do_load until frame is initialised") ;
        initialiseFrameForCompilation();
        theCompiler.do_initFrameTests(anMethod, codeAttr);
        theCompiler.emitPrologueTests();
        long []registerValues = null;
        boolean success = true;
        long gotVal= 0;
        int instructions [] = null;
        int i,assemblerStatements;
        ARMV7MacroAssembler masm = theCompiler.getMacroAssemblerUNITTEST();


        expectedValues[0] = -2;
        expectedValues[1] = -1;
        expectedValues[2] = 0;
        expectedValues[3] = 1;
        expectedValues[4] = 2;
        expectedValues[5] = 3;
        expectedValues[6] = 4;
        expectedValues[7] = 5;
        expectedValues[8] =6;
        expectedValues[9] = 7;
        expectedValues[10] = 8;



        for( i = 0 ; i <11; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i],(int)expectedValues[i]);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always,1|2|4|8|16|32|64|128|256|512);
        masm.push(ARMV7Assembler.ConditionFlag.Always,1|2|4|8|16|32|64|128|256|512);
        masm.push(ARMV7Assembler.ConditionFlag.Always,1|2|4|8|16|32|64|128|256|512);
        masm.push(ARMV7Assembler.ConditionFlag.Always,1|2|4|8|16|32|64|128|256|512);




        for(i = 0;i <= 10;i++)
            masm.mov32BitConstant(ARMV7.cpuRegisters[i],-25);
        for(i = 0; i < 5;i++) {
            theCompiler.do_loadTests(i, Kind.INT);
            masm.pop(ARMV7Assembler.ConditionFlag.Always,1);
            masm.mov32BitConstant(ARMV7.r0,100+i);
            masm.push(ARMV7Assembler.ConditionFlag.Always,1);
            theCompiler.do_storeTests(i, Kind.INT);
        }
        theCompiler.do_loadTests(5,Kind.LONG);
        masm.pop(ARMV7Assembler.ConditionFlag.Always,1|2);

        // Apologies hard coded assignLong here ....
        masm.movw(ARMV7Assembler.ConditionFlag.Always,ARMV7.r0,(int)(172L&0xffff));
        masm.movt(ARMV7Assembler.ConditionFlag.Always,ARMV7.r0,(int)((172L>>16)&0xffff));
        masm.movw(ARMV7Assembler.ConditionFlag.Always,ARMV7.r1,(int)(((172L>>32)&0xffff)));
        masm.movt(ARMV7Assembler.ConditionFlag.Always,ARMV7.r1,(int)(((172L>>48)&0xffff)));
        masm.push(ARMV7Assembler.ConditionFlag.Always,1|2);
        theCompiler.do_storeTests(5,Kind.LONG);

        for(i = 4;i>=0;i--)
            theCompiler.do_loadTests(i, Kind.INT);
        theCompiler.do_loadTests(5,Kind.LONG);





        masm.pop(ARMV7Assembler.ConditionFlag.Always,1|2|4|8|16|32|64);

        //theCompiler.fixup();
        assemblerStatements =  masm.codeBuffer.position()/4;
        instructions = new int [assemblerStatements];
        expectedValues[0] = 172;
        expectedValues[1] = 0;
        expectedValues[2] = 100;
        expectedValues[3] = 101;
        expectedValues[4] = 102;
        expectedValues[5] = 103;
        expectedValues[6] = 104;

        registerValues  = generateAndTest(assemblerStatements,expectedValues,testvalues,bitmasks);
        for( i = 0; i <= 6;i++) {
            if(registerValues[i] != expectedValues[i])  {
                System.out.println("REG VALS ["+i+"] HEX " + Long.toString(registerValues[i],16)+ "  DEC " +registerValues[i] );
                success = false;
            }
        }


        assert(success == true);

    }

}




