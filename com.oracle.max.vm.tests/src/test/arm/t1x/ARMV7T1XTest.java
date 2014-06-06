package test.arm.t1x;

import java.io.*;
import java.util.*;

import test.arm.asm.*;

import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.ConditionFlag;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.armv7.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

public class ARMV7T1XTest extends MaxTestCase {

    private ARMV7Assembler asm;
    private CiTarget armv7;
    private ARMCodeWriter code;
    private T1X t1x;
    private ARMV7T1XCompilation theCompiler;
    private StaticMethodActor anMethod = null;
    private CodeAttribute codeAttr = null;
    private static boolean POST_CLEAN_FILES = true;

    public void initialiseFrameForCompilation() {
        // TODO: compute max stack
        codeAttr = new CodeAttribute(null, new byte[15], (char) 10, (char) 8, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create("(Ljava/util/Map;)V"), Actor.JAVA_METHOD_FLAGS, codeAttr, new String());
    }

    public void initialiseFrameForCompilation(byte [] code) {
        // TODO: compute max stack
        codeAttr = new CodeAttribute(null, code, (char) 10, (char) 8, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create("(Ljava/util/Map;)V"), Actor.JAVA_METHOD_FLAGS, codeAttr, new String());
    }
    static final class Pair {

        public final int first;
        public final int second;

        public Pair(int first, int second) {
            this.first = first;
            this.second = second;
        }
    }

    private static final OptionSet options = new OptionSet(false);
    private static VMConfigurator vmConfigurator = null;
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

    private static int[] valueTestSet = {0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65535};
    private static long[] scratchTestSet = {0, 1, 0xff, 0xffff, 0xffffff, 0xfffffff, 0x00000000ffffffffL};
    private static MaxineARMTester.BitsFlag[] bitmasks = {MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits,
        MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits,
        MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits,
        MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits};

    private static int[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static boolean[] ignorevalues = new boolean[17];

    private int[] generateAndTest(int assemblerStatements, int[] expected, boolean[] ignores, MaxineARMTester.BitsFlag[] masks) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(assemblerStatements, theCompiler.getMacroAssembler().codeBuffer);
        code.createCodeFile();
        MaxineARMTester r = new MaxineARMTester(expected, ignores, masks);
        r.cleanFiles();
        r.cleanProcesses();
        r.assembleStartup();
        r.assembleEntry();
        r.compile();
        r.link();
        r.objcopy();
        int[] simulatedRegisters = r.runRegisteredSimulation();
        r.cleanProcesses();
        if (POST_CLEAN_FILES) {
            r.cleanFiles();
        }
        return simulatedRegisters;
    }

    public ARMV7T1XTest() {
        try {
            String[] args = new String[2];
            args[0] = new String("t1x");
            args[1] = new String("HelloWorld");
            if (options != null) {
                options.parseArguments(args);
            }
            if (vmConfigurator == null) {
                vmConfigurator = new VMConfigurator(options);
            }
            String compilerName = new String("com.oracle.max.vm.ext.t1x.T1X");
            RuntimeCompiler.baselineCompilerOption.setValue(compilerName);
            if (initialised == false) {
                vmConfigurator.create();
                initialised = true;
            }
            t1x = (T1X) CompilationBroker.instantiateCompiler(compilerName);
            theCompiler = (ARMV7T1XCompilation) t1x.getT1XCompilation();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ARMV7T1XTest.class);
    }

    public void testDecStack() throws Exception {
        int assemblerStatements;
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        theCompiler.incStack(3);
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r0, ARMV7.r13); // copy stack value into r0
        theCompiler.decStack(1);
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r1, ARMV7.r13); // copy stack value onto r1
        theCompiler.decStack(2);
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r2, ARMV7.r13);
        assemblerStatements = masm.codeBuffer.position() / 4;
        int[] simulatedValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 16; i++) {
            assert 2 * (simulatedValues[1] - simulatedValues[0]) == (simulatedValues[2] - simulatedValues[1]) : "Register " + i + " Value " + simulatedValues[i];
        }
    }

    public void testIncStack() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r0, ARMV7.r13); // copy stack value into r0
        theCompiler.incStack(1);
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r1, ARMV7.r13); // copy stack value onto r1
        theCompiler.incStack(2);
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r2, ARMV7.r13);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] simulatedValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 16; i++) {
            assert 2 * (simulatedValues[0] - simulatedValues[1]) == (simulatedValues[1] - simulatedValues[2]) : "Register " + i + " Value " + simulatedValues[i];
        }
    }

    public void testAdjustReg() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 0);
        masm.mov32BitConstant(ARMV7.r1, 1);
        masm.mov32BitConstant(ARMV7.r2, Integer.MIN_VALUE);
        masm.mov32BitConstant(ARMV7.r3, Integer.MAX_VALUE);
        masm.mov32BitConstant(ARMV7.r4, 0);
        masm.mov32BitConstant(ARMV7.r5, 0);
        masm.incrementl(ARMV7.r0, 1);
        masm.incrementl(ARMV7.r1, -1);
        masm.incrementl(ARMV7.r2, -1);
        masm.incrementl(ARMV7.r3, 1);
        masm.incrementl(ARMV7.r4, Integer.MAX_VALUE);
        masm.incrementl(ARMV7.r5, 0);
        masm.mov32BitConstant(ARMV7.r6, -10);
        masm.incrementl(ARMV7.r6, -1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] simulatedValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        expectedValues[0] = 1;
        expectedValues[1] = 0;
        expectedValues[2] = Integer.MAX_VALUE;
        expectedValues[3] = Integer.MIN_VALUE;
        expectedValues[4] = Integer.MAX_VALUE;
        expectedValues[5] = 0;
        expectedValues[6] = -11;
        for (int i = 0; i < 7; i++) {
            assert simulatedValues[i] == expectedValues[i] : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void testPokeInt() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = Integer.MIN_VALUE;
        expectedValues[1] = Integer.MAX_VALUE;
        expectedValues[2] = 0;
        expectedValues[3] = -1;
        expectedValues[4] = 40;
        expectedValues[5] = -40;
        for (int i = 0; i < 6; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], i);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4 | 8);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 2);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 16);
        for (int i = 0; i <= 5; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], expectedValues[i]);
        }
        theCompiler.pokeInt(ARMV7.cpuRegisters[0], 5);
        theCompiler.pokeInt(ARMV7.cpuRegisters[1], 4);
        theCompiler.pokeInt(ARMV7.cpuRegisters[2], 3);
        theCompiler.pokeInt(ARMV7.cpuRegisters[3], 2);
        theCompiler.pokeInt(ARMV7.cpuRegisters[4], 1);
        theCompiler.pokeInt(ARMV7.cpuRegisters[5], 0);
        for (int i = 0; i <= 5; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -25);
        }
        theCompiler.peekInt(ARMV7.cpuRegisters[0], 5);
        theCompiler.peekInt(ARMV7.cpuRegisters[1], 4);
        theCompiler.peekInt(ARMV7.cpuRegisters[2], 3);
        theCompiler.peekInt(ARMV7.cpuRegisters[3], 2);
        theCompiler.peekInt(ARMV7.cpuRegisters[4], 1);
        theCompiler.peekInt(ARMV7.cpuRegisters[5], 0);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 6; i++) {
            assert registerValues[i] == expectedValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedValues[i], 16);
        }
    }

    public void testAssignLong() throws Exception {
        long returnValue = 0;
        int i;
        int assemblerStatements;
        long[] expectedLongValues = new long[10];
        expectedLongValues[0] = Long.MIN_VALUE;
        expectedLongValues[2] = Long.MAX_VALUE;
        expectedLongValues[4] = 0xabdef01023456789L;
        expectedLongValues[6] = 111;
        expectedLongValues[8] = 0;
        for (i = 0; i < 10; i += 2) {
            theCompiler.assignmentTests(ARMV7.cpuRegisters[i], expectedLongValues[i]);
        }
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                returnValue = 0xffffffffL & registerValues[i];
            } else {
                returnValue |= (0xffffffffL & registerValues[i]) << 32;
                assert returnValue == expectedLongValues[i - 1] : "Failed incorrect value " + Long.toString(returnValue, 16) + " " + Long.toString(expectedLongValues[i - 1], 16);
            }
        }
    }

    public void testPeekLong() throws Exception {
        long returnValue = 0;
        int assemblerStatements;
        long[] expectedLongValues = new long[10];
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedLongValues[0] = Long.MIN_VALUE;
        expectedLongValues[2] = Long.MAX_VALUE;
        expectedLongValues[4] = 0xabdef01023456789L;
        expectedLongValues[6] = 111;
        expectedLongValues[8] = 0;
        for (int i = 0; i < 10; i += 2) {
            theCompiler.assignmentTests(ARMV7.cpuRegisters[i], expectedLongValues[i]);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4 | 8); // this is to check/debug issues about wrong address //
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2); // index 4
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4 | 8);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 16 | 32); // index 2
        masm.push(ARMV7Assembler.ConditionFlag.Always, 64 | 128); // index 1
        masm.push(ARMV7Assembler.ConditionFlag.Always, 256 | 512); // index 0
        for (int i = 0; i <= 10; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -25);
        }
        theCompiler.peekLong(ARMV7.cpuRegisters[0], 8);
        theCompiler.peekLong(ARMV7.cpuRegisters[2], 6);
        theCompiler.peekLong(ARMV7.cpuRegisters[4], 4);
        theCompiler.peekLong(ARMV7.cpuRegisters[6], 2);
        theCompiler.peekLong(ARMV7.cpuRegisters[8], 0);
        assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                returnValue = 0xffffffffL & registerValues[i];
            } else {
                returnValue |= (0xffffffffL & registerValues[i]) << 32;
                assert returnValue == expectedLongValues[i - 1] : "Failed incorrect value " + Long.toString(returnValue, 16) + " " + Long.toString(expectedLongValues[i - 1], 16);
            }
        }
    }

    public void testPokeLong() throws Exception {
        long returnValue = 0;
        long[] expectedLongValues = new long[10];
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedLongValues[0] = Long.MIN_VALUE;
        expectedLongValues[2] = Long.MAX_VALUE;
        expectedLongValues[4] = 0xabdef01023456789L;
        expectedLongValues[6] = 111;
        expectedLongValues[8] = 0;
        for (int i = 0; i < 10; i += 2) {
            theCompiler.assignmentTests(ARMV7.cpuRegisters[i], -99);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4 | 8); // this is to check/debug issues about wrong address //
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2); // index 4
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4 | 8);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 256 | 512); // index 0
        for (int i = 0; i < 10; i += 2) {
            theCompiler.assignmentTests(ARMV7.cpuRegisters[i], expectedLongValues[i]);
        }
        theCompiler.pokeLong(ARMV7.cpuRegisters[0], 8);
        theCompiler.pokeLong(ARMV7.cpuRegisters[2], 6);
        theCompiler.pokeLong(ARMV7.cpuRegisters[4], 4);
        theCompiler.pokeLong(ARMV7.cpuRegisters[6], 2);
        theCompiler.pokeLong(ARMV7.cpuRegisters[8], 0);
        for (int i = 0; i <= 10; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -5);
        }
        theCompiler.peekLong(ARMV7.cpuRegisters[0], 8);
        theCompiler.peekLong(ARMV7.cpuRegisters[2], 6);
        theCompiler.peekLong(ARMV7.cpuRegisters[4], 4);
        theCompiler.peekLong(ARMV7.cpuRegisters[6], 2);
        theCompiler.peekLong(ARMV7.cpuRegisters[8], 0);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                returnValue = 0xffffffffL & registerValues[i];
            } else {
                returnValue |= (0xffffffffL & registerValues[i]) << 32;
                assert returnValue == expectedLongValues[i - 1] : "Failed incorrect value " + Long.toString(returnValue, 16) + " " + Long.toString(expectedLongValues[i - 1], 16);
            }
        }
    }

    public void testPeekInt() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = Integer.MIN_VALUE;
        expectedValues[1] = Integer.MAX_VALUE;
        expectedValues[2] = 0;
        expectedValues[3] = -1;
        expectedValues[4] = 40;
        expectedValues[5] = -40;
        for (int i = 0; i < 6; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], expectedValues[i]);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4 | 8);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1); // index 5
        masm.push(ARMV7Assembler.ConditionFlag.Always, 2); // index 4
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4); // index 3
        masm.push(ARMV7Assembler.ConditionFlag.Always, 8); // index 2
        masm.push(ARMV7Assembler.ConditionFlag.Always, 16); // index 1
        masm.push(ARMV7Assembler.ConditionFlag.Always, 32); // index 0
        for (int i = 0; i <= 5; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -25);
        }
        theCompiler.peekInt(ARMV7.cpuRegisters[0], 5);
        theCompiler.peekInt(ARMV7.cpuRegisters[1], 4);
        theCompiler.peekInt(ARMV7.cpuRegisters[2], 3);
        theCompiler.peekInt(ARMV7.cpuRegisters[3], 2);
        theCompiler.peekInt(ARMV7.cpuRegisters[4], 1);
        theCompiler.peekInt(ARMV7.cpuRegisters[5], 0);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 6; i++) {
            assert registerValues[i] == expectedValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedValues[i], 16);
        }
    }

    public void testPokeDouble() throws Exception {
        long returnValue = 0;
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        long[] expectedLongValues = new long[5];
        expectedLongValues[0] = Double.doubleToRawLongBits(Double.MIN_VALUE);
        expectedLongValues[1] = Double.doubleToRawLongBits(Double.MAX_VALUE);
        expectedLongValues[2] = Double.doubleToRawLongBits(0.0);
        expectedLongValues[3] = Double.doubleToRawLongBits(-1.0);
        expectedLongValues[4] = Double.doubleToRawLongBits(-100.75);
        for (int i = 0; i < 5; i++) {
            theCompiler.assignDoubleTest(ARMV7.allRegisters[16 + i], Double.longBitsToDouble(expectedLongValues[i]));
        }
        masm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.d0, ARMV7.d0);
        masm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.d0, ARMV7.d0); // index 8
        masm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.d1, ARMV7.d1); // index 6
        masm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.d2, ARMV7.d2); // index 4
        masm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.d3, ARMV7.d3); // index 2
        masm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.d4, ARMV7.d4); // index 0
        for (int i = 0; i <= 9; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -25);
        }
        expectedLongValues[0] = Double.doubleToRawLongBits(-100.1);
        expectedLongValues[1] = Double.doubleToRawLongBits(-200.2);
        expectedLongValues[2] = Double.doubleToRawLongBits(1.123456);
        expectedLongValues[3] = Double.doubleToRawLongBits(99.9876543);
        expectedLongValues[4] = Double.doubleToRawLongBits(3000000.000);
        for (int i = 0; i < 5; i++) {
            theCompiler.assignDoubleTest(ARMV7.allRegisters[16 + i], Double.longBitsToDouble(expectedLongValues[i]));
        }
        theCompiler.pokeDouble(ARMV7.d0, 8);
        theCompiler.pokeDouble(ARMV7.d1, 6);
        theCompiler.pokeDouble(ARMV7.d2, 4);
        theCompiler.pokeDouble(ARMV7.d3, 2);
        theCompiler.pokeDouble(ARMV7.d4, 0);
        for (int i = 0; i < 5; i++) {
            theCompiler.assignDoubleTest(ARMV7.allRegisters[16 + i], Double.longBitsToDouble(i));
        }
        theCompiler.peekDouble(ARMV7.d0, 8);
        theCompiler.peekDouble(ARMV7.d1, 6);
        theCompiler.peekDouble(ARMV7.d2, 4);
        theCompiler.peekDouble(ARMV7.d3, 2);
        theCompiler.peekDouble(ARMV7.d4, 0);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, ARMV7.d0);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.d1);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.d2);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r6, ARMV7.d3);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, ARMV7.d4);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 10; i += 2) {
            returnValue = 0xffffffffL & registerValues[i];
            returnValue |= (0xffffffffL & registerValues[i + 1]) << 32;
            assert returnValue == expectedLongValues[i / 2] : "Failed incorrect value " + Long.toString(returnValue, 16) + " " + Long.toString(expectedLongValues[i / 2], 16) + " Expected " +
                            Double.longBitsToDouble(expectedLongValues[i / 2]) + " GOT " + Double.longBitsToDouble(returnValue);
        }
    }

    public void testPeekFloat() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        long[] expectedLongValues = new long[6];
        expectedLongValues[0] = Float.floatToRawIntBits(Float.MIN_VALUE);
        expectedLongValues[1] = Float.floatToRawIntBits(Float.MAX_VALUE);
        expectedLongValues[2] = Float.floatToRawIntBits(0.0f);
        expectedLongValues[3] = Float.floatToRawIntBits(-1.0f);
        expectedLongValues[4] = Float.floatToRawIntBits(2.5f);
        expectedLongValues[5] = Float.floatToRawIntBits(-100.75f);
        for (int i = 0; i < 6; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], (int) expectedLongValues[i]);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4 | 8);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1); // index 5
        masm.push(ARMV7Assembler.ConditionFlag.Always, 2); // index 4
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 8); // index 2
        masm.push(ARMV7Assembler.ConditionFlag.Always, 16); // index 1
        masm.push(ARMV7Assembler.ConditionFlag.Always, 32); // index 0
        for (int i = 0; i <= 5; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -25);
            theCompiler.peekFloat(ARMV7.s0, 5);
        }
        theCompiler.peekFloat(ARMV7.s1, 4);
        theCompiler.peekFloat(ARMV7.s2, 3);
        theCompiler.peekFloat(ARMV7.s3, 2);
        theCompiler.peekFloat(ARMV7.s4, 1);
        theCompiler.peekFloat(ARMV7.s5, 0);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, ARMV7.s0);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r1, ARMV7.s1);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s2);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r3, ARMV7.s3);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.s4);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r5, ARMV7.s5);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 6; i++) {
            assert registerValues[i] == expectedLongValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedLongValues[i], 16) + " Expected " +
                            Float.intBitsToFloat((int) expectedLongValues[i]) + " GOT " + Float.intBitsToFloat(registerValues[i]);
        }
    }

    public void testPokeFloat() throws Exception {
        long[] expectedLongValues = new long[6];
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedLongValues[0] = Float.floatToRawIntBits(Float.MIN_VALUE);
        expectedLongValues[1] = Float.floatToRawIntBits(Float.MAX_VALUE);
        expectedLongValues[2] = Float.floatToRawIntBits(0.0f);
        expectedLongValues[3] = Float.floatToRawIntBits(-1.0f);
        expectedLongValues[4] = Float.floatToRawIntBits(2.5f);
        expectedLongValues[5] = Float.floatToRawIntBits(-100.75f);
        for (int i = 0; i < 6; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], (int) expectedLongValues[i]);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4 | 8); // this is to check/debug issues about wrong address //
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1); // index 5
        masm.push(ARMV7Assembler.ConditionFlag.Always, 2); // index 4
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 8); // index 2
        masm.push(ARMV7Assembler.ConditionFlag.Always, 16); // index 1
        masm.push(ARMV7Assembler.ConditionFlag.Always, 32); // index 0
        float value = -111.111111f;
        for (int i = 0; i < 6; i++) {
            expectedLongValues[i] = Float.floatToRawIntBits(value);
            value = value + -1.2f;
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], (int) expectedLongValues[i]);
            masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.allRegisters[i + 32], ARMV7.cpuRegisters[i]);
        }
        theCompiler.pokeFloat(ARMV7.s0, 5);
        theCompiler.pokeFloat(ARMV7.s1, 4);
        theCompiler.pokeFloat(ARMV7.s2, 3);
        theCompiler.pokeFloat(ARMV7.s3, 2);
        theCompiler.pokeFloat(ARMV7.s4, 1);
        theCompiler.pokeFloat(ARMV7.s5, 0);
        theCompiler.peekFloat(ARMV7.s6, 5);
        theCompiler.peekFloat(ARMV7.s7, 4);
        theCompiler.peekFloat(ARMV7.s8, 3);
        theCompiler.peekFloat(ARMV7.s9, 2);
        theCompiler.peekFloat(ARMV7.s10, 1);
        theCompiler.peekFloat(ARMV7.s11, 0);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, ARMV7.s6);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r1, ARMV7.s7);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s8);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r3, ARMV7.s9);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.s10);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r5, ARMV7.s11);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 6; i++) {
            assert registerValues[i] == expectedLongValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedLongValues[i], 16) + " Expected " +
                            Float.intBitsToFloat((int) expectedLongValues[i]) + " got " + Float.intBitsToFloat(registerValues[i]);
        }
    }

    public void testAssignDouble() throws Exception {
        long returnValue = 0;
        long[] expectedLongValues = new long[5];
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedLongValues[0] = Double.doubleToRawLongBits(Double.MIN_VALUE);
        expectedLongValues[1] = Double.doubleToRawLongBits(Double.MAX_VALUE);
        expectedLongValues[2] = Double.doubleToRawLongBits(0.0);
        expectedLongValues[3] = Double.doubleToRawLongBits(-1.0);
        expectedLongValues[4] = Double.doubleToRawLongBits(-100.75);
        for (int i = 0; i < 5; i++) {
            theCompiler.assignDoubleTest(ARMV7.allRegisters[16 + i], Double.longBitsToDouble(expectedLongValues[i]));
        }
        for (int i = 0; i <= 9; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -25);
        }
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, ARMV7.d0);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.d1);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.d2);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r6, ARMV7.d3);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, ARMV7.d4);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 10; i += 2) {
            returnValue = 0xffffffffL & registerValues[i];
            returnValue |= (0xffffffffL & registerValues[i + 1]) << 32;
            assert returnValue == expectedLongValues[i / 2] : "Failed incorrect value " + Long.toString(returnValue, 16) + " " + Long.toString(expectedLongValues[i / 2], 16) + " Expected " +
                            Double.longBitsToDouble(expectedLongValues[i / 2]) + " GOT " + Double.longBitsToDouble(returnValue);
        }
    }

    public void testPeekDouble() throws Exception {
        long returnValue = 0;
        long[] expectedLongValues = new long[5];
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedLongValues[0] = Double.doubleToRawLongBits(Double.MIN_VALUE);
        expectedLongValues[1] = Double.doubleToRawLongBits(Double.MAX_VALUE);
        expectedLongValues[2] = Double.doubleToRawLongBits(0.0);
        expectedLongValues[3] = Double.doubleToRawLongBits(-1.0);
        expectedLongValues[4] = Double.doubleToRawLongBits(-100.75);
        for (int i = 0; i < 5; i++) {
            theCompiler.assignDoubleTest(ARMV7.allRegisters[16 + i], Double.longBitsToDouble(expectedLongValues[i]));
        }
        masm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.d0, ARMV7.d0);
        masm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.d0, ARMV7.d0); // index 8
        masm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.d1, ARMV7.d1); // index 6
        masm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.d2, ARMV7.d2); // index 4
        masm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.d3, ARMV7.d3); // index 2
        masm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.d4, ARMV7.d4); // index 0
        for (int i = 0; i <= 9; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -25);
        }
        theCompiler.peekDouble(ARMV7.d0, 8);
        theCompiler.peekDouble(ARMV7.d1, 6);
        theCompiler.peekDouble(ARMV7.d2, 4);
        theCompiler.peekDouble(ARMV7.d3, 2);
        theCompiler.peekDouble(ARMV7.d4, 0);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, ARMV7.d0);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.d1);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.d2);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r6, ARMV7.d3);
        masm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, ARMV7.d4);

        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 10; i += 2) {
            returnValue = 0xffffffffL & registerValues[i];
            returnValue |= (0xffffffffL & registerValues[i + 1]) << 32;
            assert returnValue == expectedLongValues[i / 2] : "Failed incorrect value " + Long.toString(returnValue, 16) + " " + Long.toString(expectedLongValues[i / 2], 16) + " Expected " +
                            Double.longBitsToDouble(expectedLongValues[i / 2]) + " got " + Double.longBitsToDouble(returnValue);
        }
    }

    public void testDoLconst() throws Exception {
        long returnValue = 0;
        long[] expectedLongValues = new long[8];
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedLongValues[0] = 0xffffffffL & 0xffffffff0000ffffL;
        expectedLongValues[1] = 0xffffffffL & (0xffffffff0000ffffL >> 32);
        expectedLongValues[6] = 0;
        expectedLongValues[7] = 1;
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r2, ARMV7.r13); // copy stack pointer to r2
        masm.mov32BitConstant(ARMV7.r6, 0);
        masm.mov32BitConstant(ARMV7.r7, 1); // r6 and r7 are used as temporaries,
        theCompiler.do_lconstTests(0xffffffff0000ffffL);
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r3, ARMV7.r13); // copy revised stack pointer to r3
        theCompiler.peekLong(ARMV7.r0, 0);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        returnValue = 0xffffffffL & registerValues[0];
        returnValue |= (0xffffffffL & registerValues[1]) << 32;
        assert returnValue == 0xffffffff0000ffffL;
        assert registerValues[2] - registerValues[3] == 8; // stack pointer has increased by 8 due to pushing the
    }

    public void testDoDconst() throws Exception {
        long returnValue = 0;
        double myVal = 3.14123;
        long[] expectedLongValues = new long[8];
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        returnValue = Double.doubleToRawLongBits(myVal);
        expectedLongValues[0] = 0xffffffffL & returnValue;
        expectedLongValues[1] = 0xffffffffL & (returnValue >> 32);
        expectedLongValues[6] = 0;
        expectedLongValues[7] = 1;
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r2, ARMV7.r13); // copy stack pointer to r2
        masm.mov32BitConstant(ARMV7.r6, 0);
        masm.mov32BitConstant(ARMV7.r7, 1); // r6 and r7 are used as temporaries,
        theCompiler.do_dconstTests(myVal);
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r3, ARMV7.r13); // copy revised stack pointer to r3
        theCompiler.peekLong(ARMV7.r0, 0);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        returnValue = 0;
        returnValue = 0xffffffffL & registerValues[0];
        returnValue |= (0xffffffffL & registerValues[1]) << 32;
        assert returnValue == Double.doubleToRawLongBits(myVal);
        assert registerValues[2] - registerValues[3] == 8;
    }

    public void testDoFconst() throws Exception {
        long returnValue = 0;
        float myVal = 3.14123f;
        long[] expectedLongValues = new long[1];
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        returnValue = Float.floatToRawIntBits(myVal);
        expectedLongValues[0] = returnValue;
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r2, ARMV7.r13); // copy stack pointer to r2
        masm.mov32BitConstant(ARMV7.r6, 0);
        masm.mov32BitConstant(ARMV7.r7, 1); // r6 and r7 are used as temporaries,
        theCompiler.do_fconstTests(myVal);
        masm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r3, ARMV7.r13); // copy revised stack pointer to r3
        theCompiler.peekInt(ARMV7.r0, 0);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        returnValue = registerValues[0];
        assert returnValue == Float.floatToRawIntBits(myVal);
        assert registerValues[2] - registerValues[3] == 4;
    }

    public void testDoLoad() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.do_initFrameTests(anMethod, codeAttr);
        theCompiler.emitPrologueTests();
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = -2;
        expectedValues[1] = -1;
        expectedValues[2] = 0;
        expectedValues[3] = 1;
        expectedValues[4] = 2;
        expectedValues[5] = 3;
        expectedValues[6] = 4;
        expectedValues[7] = 5;
        expectedValues[8] = 6;
        expectedValues[9] = 7;
        expectedValues[10] = 8;
        for (int i = 0; i < 11; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], expectedValues[i]);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        for (int i = 0; i <= 10; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -25);
        }
        for (int i = 0; i < 5; i++) {
            theCompiler.do_loadTests(i, Kind.INT);
            masm.pop(ARMV7Assembler.ConditionFlag.Always, 1);
            masm.mov32BitConstant(ARMV7.r0, 100 + i);
            masm.push(ARMV7Assembler.ConditionFlag.Always, 1);
            theCompiler.do_storeTests(i, Kind.INT);
        }
        theCompiler.do_loadTests(5, Kind.LONG);
        masm.pop(ARMV7Assembler.ConditionFlag.Always, 1 | 2);
        masm.movw(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, (int) (172L & 0xffff));
        masm.movt(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, (int) ((172L >> 16) & 0xffff));
        masm.movw(ARMV7Assembler.ConditionFlag.Always, ARMV7.r1, (int) (((172L >> 32) & 0xffff)));
        masm.movt(ARMV7Assembler.ConditionFlag.Always, ARMV7.r1, (int) (((172L >> 48) & 0xffff)));
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2);
        theCompiler.do_storeTests(5, Kind.LONG);
        for (int i = 4; i >= 0; i--) {
            theCompiler.do_loadTests(i, Kind.INT);
        }
        theCompiler.do_loadTests(5, Kind.LONG);
        masm.pop(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        expectedValues[0] = 172;
        expectedValues[1] = 0;
        expectedValues[2] = 100;
        expectedValues[3] = 101;
        expectedValues[4] = 102;
        expectedValues[5] = 103;
        expectedValues[6] = 104;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i <= 6; i++) {
            assert registerValues[i] == expectedValues[i] : "Reg Values [" + i + "] Hex " + Long.toString(registerValues[i], 16) + "  Dex " + registerValues[i];
        }
    }

    public void testAdd() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.do_initFrameTests(anMethod, codeAttr);
        theCompiler.emitPrologueTests();
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        theCompiler.do_iconstTests(1);
        theCompiler.do_iconstTests(2);
        theCompiler.do_iaddTests();
        expectedValues[0] = 3;
        expectedValues[1] = 2;
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        assert expectedValues[0] == registerValues[0];
    }

    public void testMul() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.do_initFrameTests(anMethod, codeAttr);
        theCompiler.emitPrologueTests();
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        theCompiler.do_iconstTests(3); // push the constant 1 onto the operand stack
        theCompiler.do_iconstTests(4); // push the constant 2 onto the operand stack
        theCompiler.do_imulTests();
        expectedValues[0] = 12;
        expectedValues[1] = 4;
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        assert expectedValues[0] == registerValues[0];
    }

    public void testPeekWord() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = 134480128;
        expectedValues[1] = 671351040;
        expectedValues[2] = 407111936;
        for (int i = 0; i < 3; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], expectedValues[i]);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1); // index 5
        masm.push(ARMV7Assembler.ConditionFlag.Always, 2); // index 4
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4); // index 3
        for (int i = 0; i <= 3; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -25);
        }
        theCompiler.peekWord(ARMV7.cpuRegisters[0], 2);
        theCompiler.peekWord(ARMV7.cpuRegisters[1], 1);
        theCompiler.peekWord(ARMV7.cpuRegisters[2], 0);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 3; i++) {
            assert registerValues[i] == expectedValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedValues[i], 16);
        }
    }

    public void testPokeWord() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = 134480128;
        expectedValues[1] = 671351040;
        expectedValues[2] = 407111936;
        for (int i = 0; i < 3; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], i);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4 | 8);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 2);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 16);
        for (int i = 0; i <= 5; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], expectedValues[i]);
        }
        theCompiler.pokeWord(ARMV7.cpuRegisters[0], 2);
        theCompiler.pokeWord(ARMV7.cpuRegisters[1], 1);
        theCompiler.pokeWord(ARMV7.cpuRegisters[2], 0);
        for (int i = 0; i < 3; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -25);
        }
        theCompiler.peekWord(ARMV7.cpuRegisters[0], 2);
        theCompiler.peekWord(ARMV7.cpuRegisters[1], 1);
        theCompiler.peekWord(ARMV7.cpuRegisters[2], 0);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 3; i++) {
            assert registerValues[i] == expectedValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedValues[i], 16);
        }
    }

    public void testPeekObject() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = 134480128;
        expectedValues[1] = 671351040;
        expectedValues[2] = 407111936;
        for (int i = 0; i < 3; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], expectedValues[i]);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1); // index 5
        masm.push(ARMV7Assembler.ConditionFlag.Always, 2); // index 4
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4); // index 3
        for (int i = 0; i <= 3; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -25);
        }
        theCompiler.peekObject(ARMV7.cpuRegisters[0], 2);
        theCompiler.peekObject(ARMV7.cpuRegisters[1], 1);
        theCompiler.peekObject(ARMV7.cpuRegisters[2], 0);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 3; i++) {
            assert registerValues[i] == expectedValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedValues[i], 16);
        }
    }

    public void testPokeObject() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = 134480128;
        expectedValues[1] = 671351040;
        expectedValues[2] = 407111936;
        for (int i = 0; i < 3; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], i);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1); // index 5
        masm.push(ARMV7Assembler.ConditionFlag.Always, 2); // index 4
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4); // index 3
        for (int i = 0; i < 3; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], expectedValues[i]);
        }
        theCompiler.pokeObject(ARMV7.cpuRegisters[0], 2);
        theCompiler.pokeObject(ARMV7.cpuRegisters[1], 1);
        theCompiler.pokeObject(ARMV7.cpuRegisters[2], 0);
        for (int i = 0; i < 3; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -25);
        }
        theCompiler.peekObject(ARMV7.cpuRegisters[0], 2);
        theCompiler.peekObject(ARMV7.cpuRegisters[1], 1);
        theCompiler.peekObject(ARMV7.cpuRegisters[2], 0);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        for (int i = 0; i < 3; i++) {
            assert registerValues[i] == expectedValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedValues[i], 16);
        }
    }

    public void failingtestIfEq() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = 10;
        expectedValues[1] = 20;
        expectedValues[2] = 1;
        expectedValues[3] = 2;
        for (int i = 0; i < 4; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], expectedValues[i]);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 2);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 8);
        for (int i = 0; i < 4; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -1);
        }

        theCompiler.peekInt(ARMV7.cpuRegisters[0], 0);
        theCompiler.assignInt(ARMV7.cpuRegisters[1], 2);
        theCompiler.decStack(1);
        masm.cmpl(ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[1]);
        ConditionFlag cc = ConditionFlag.Equal; //Testing the jump (eq)
        theCompiler.assignInt(ARMV7.r12, 99); // APN deliberate ... make scratch have nonzero value
        masm.jcc(cc, masm.codeBuffer.position() + 1, false); // 1 as a false will insert one instructions!!!
        theCompiler.assignInt(ARMV7.cpuRegisters[2], 20);
        theCompiler.assignInt(ARMV7.cpuRegisters[3], 10);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        assert registerValues[3] == 10 : "Failed incorrect value " + Long.toString(registerValues[3], 16) + " 10";
        assert registerValues[2] == -1 : "Failed incorrect value " + Long.toString(registerValues[2], 16) + " -1";

        expectedValues[0] = 10;
        expectedValues[1] = 20;
        expectedValues[2] = 1;
        expectedValues[3] = 1;
        for (int i = 0; i < 4; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], expectedValues[i]);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 2);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 4);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 8);
        for (int i = 0; i < 4; i++) {
            masm.mov32BitConstant(ARMV7.cpuRegisters[i], -1);
        }

        theCompiler.peekInt(ARMV7.cpuRegisters[0], 0);
        theCompiler.assignInt(ARMV7.cpuRegisters[1], 1);
        theCompiler.decStack(1);
        masm.cmpl(ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[1]);
        cc = ConditionFlag.NotEqual; //Testing the fallthrough (ne)
        masm.jcc(cc, masm.codeBuffer.position() + 8, true);
        theCompiler.assignInt(ARMV7.cpuRegisters[2], 20);
        theCompiler.assignInt(ARMV7.cpuRegisters[3], 10);
        assemblerStatements = masm.codeBuffer.position() / 4;
        registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        assert registerValues[3] == 10 : "Failed incorrect value " + Long.toString(registerValues[0], 16) + " 10";
        assert registerValues[2] == 20 : "Failed incorrect value " + Long.toString(registerValues[0], 16) + " 20";
    }

    static final class BranchInfo {

        private int bc;
        private int start;
        private int end;
        private int expected;
        private int step;

        private BranchInfo(int bc, int start, int end, int expected, int step) {
            this.bc = bc;
            this.end = end;
            this.start = start;
            this.expected = expected;
            this.step = step;
        }

        public int getBytecode() {
            return bc;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getExpected() {
            return expected;
        }

        public int getStep() {
            return step;
        }
    }

    private static final List<BranchInfo> branches = new LinkedList<>();
    static {
        branches.add(new BranchInfo(Bytecodes.IF_ICMPLT, 0, 10, 10, 1));
        branches.add(new BranchInfo(Bytecodes.IF_ICMPLE, 0, 10, 11, 1));
        branches.add(new BranchInfo(Bytecodes.IF_ICMPGT, 5, 0, 0, -1));
        branches.add(new BranchInfo(Bytecodes.IF_ICMPGE, 5, 0, -1, -1));
        branches.add(new BranchInfo(Bytecodes.IF_ICMPNE, 5, 6, 6, 1));
        branches.add(new BranchInfo(Bytecodes.IF_ICMPEQ, 0, 0, 2, 2));
    }


    public void testBranchBytecodes() throws Exception {
        /*
        Based on pg41 JVMSv1.7 ...
        iconst_0
        istore_1
        goto 8 wrong it needs to be 6
        iinc 1 1
        iload_1
        bipush 100
        if_icmplt 5 this is WRONG it needs to be -6
        // no return.
         corresponding to
         int i;
         for(i = 0; i < 100;i++)  {
             ; // empty loop body
         }
         return;
         */
        for (BranchInfo bi : branches) {
            expectedValues[0] = bi.getExpected();
            byte[] instructions = new byte[16];
            if (bi.getStart() == 0) {
                instructions[0] = (byte) Bytecodes.ICONST_0;
            } else {
                instructions[0] = (byte) Bytecodes.ICONST_5;
            }
            instructions[1] = (byte) Bytecodes.ISTORE_1;
            instructions[2] = (byte) Bytecodes.GOTO;
            instructions[3] = (byte) 0;
            instructions[4] = (byte) 6;
            instructions[5] = (byte) Bytecodes.IINC;
            instructions[6] = (byte) 1;
            instructions[7] = (byte) bi.getStep();
            instructions[8] = (byte) Bytecodes.ILOAD_1;
            instructions[9] = (byte) Bytecodes.BIPUSH;
            instructions[10] = (byte) bi.getEnd();
            instructions[11] = (byte) bi.getBytecode();
            instructions[12] = (byte) 0xff;
            instructions[13] = (byte) 0xfa;
            instructions[14] = (byte) Bytecodes.ILOAD_1;
            instructions[15] = (byte) Bytecodes.NOP;

            // instructions[14] = (byte) Bytecodes.RETURN;
            initialiseFrameForCompilation(instructions);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, instructions, 15);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.pop(ARMV7Assembler.ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + Long.toString(registerValues[0], 16) + " " + Long.toString(expectedValues[0], 16);
            theCompiler.cleanup();
        }
    }

    /**
     * This test does not yet actually tests local variables.
     */
    public void testLocals() throws Exception {
        expectedValues[0] = 10;
        expectedValues[1] = 20;
        expectedValues[2] = 30;
        expectedValues[3] = 40;
        byte[] instructions = new byte[8];
        instructions[0] = (byte) Bytecodes.BIPUSH;
        instructions[1] = (byte) 10;
        // instructions[2] = (byte) Bytecodes.ISTORE_0;
        instructions[2] = (byte) Bytecodes.BIPUSH;
        instructions[3] = (byte) 20;
        // instructions[5] = (byte) Bytecodes.ISTORE_1;
        instructions[4] = (byte) Bytecodes.BIPUSH;
        instructions[5] = (byte) 30;
        // instructions[8] = (byte) Bytecodes.ISTORE_2;
        instructions[6] = (byte) Bytecodes.BIPUSH;
        instructions[7] = (byte) 40;
        // instructions[11] = (byte) Bytecodes.ISTORE_3;
        // instructions[12] = (byte) Bytecodes.ILOAD_0;
        // instructions[13] = (byte) Bytecodes.ILOAD_1;
        // instructions[14] = (byte) Bytecodes.ILOAD_2;
        // instructions[15] = (byte) Bytecodes.ILOAD_3;
        initialiseFrameForCompilation(instructions);
        theCompiler.offlineT1XCompile(anMethod, codeAttr, instructions, 8);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        theCompiler.peekInt(ARMV7.r3, 0);
        theCompiler.peekInt(ARMV7.r2, 1);
        theCompiler.peekInt(ARMV7.r1, 2);
        theCompiler.peekInt(ARMV7.r0, 3);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        assert registerValues[1] == expectedValues[1] : "Failed incorrect value " + registerValues[1] + " " + expectedValues[1];
        assert registerValues[2] == expectedValues[2] : "Failed incorrect value " + registerValues[2] + " " + expectedValues[2];
        assert registerValues[3] == expectedValues[3] : "Failed incorrect value " + registerValues[3] + " " + expectedValues[3];
        theCompiler.cleanup();
    }

    public void testSwitchTable() throws Exception {
        // int i = 1;
        // int j, k , l, m;
        // switch(i) {
        // case 0: j=10;
        // case 1: k=20;
        // case 2: l=30;
        // default: m=40;
        // }

        // int chooseNear(int i) {
        // switch (i) {
        // } }
        // compiles to:
        // case 0: return 0;
        // case 1: return 1;
        // case 2: return 2;
        // default: return -1;
        // Method int chooseNear(int)
        // 0 iload_1 // Push local variable 1 (argument i)
        // 1 tableswitch 0 to 2: // Valid indices are 0 through 2
        // 0: 28
        // 1: 30
        // 2: 32
        // default:34
        // 28 iconst_0
        // 29 ireturn
        // 30 iconst_1
        // 31 ireturn
        // 32 iconst_2
        // 33 ireturn
        // 34 iconst_m1
        // 35 ireturn

        int[] values = new int[] { 10, 20, 30, 40};
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values.length; j++) {
                if (i > j) {
                    expectedValues[j] = 0;
                } else {
                    expectedValues[j] = values[j];
                }
            }

            byte[] instructions = new byte[36];
            if (i == 0) {
                instructions[0] = (byte) Bytecodes.ICONST_0;
            } else if (i == 1) {
                instructions[0] = (byte) Bytecodes.ICONST_1;
            } else if (i == 2) {
                instructions[0] = (byte) Bytecodes.ICONST_2;
            } else {
                instructions[0] = (byte) Bytecodes.ICONST_3;
            }
            instructions[1] = (byte) Bytecodes.ISTORE_1;
            instructions[2] = (byte) Bytecodes.ILOAD_1;

            instructions[3] = (byte) Bytecodes.TABLESWITCH;
            instructions[4] = (byte) 0;
            instructions[5] = (byte) 0;
            instructions[6] = (byte) 0;
            instructions[7] = (byte) 0x1f;

            instructions[8] = (byte) 0;
            instructions[9] = (byte) 0;
            instructions[10] = (byte) 0;
            instructions[11] = (byte) 0;

            instructions[12] = (byte) 0;
            instructions[13] = (byte) 0;
            instructions[14] = (byte) 0;
            instructions[15] = (byte) 0x2;

            instructions[16] = (byte) 0;
            instructions[17] = (byte) 0;
            instructions[18] = (byte) 0;
            instructions[19] = (byte) 0x19;

            instructions[20] = (byte) 0;
            instructions[21] = (byte) 0;
            instructions[22] = (byte) 0;
            instructions[23] = (byte) 0x1b;

            instructions[24] = (byte) 0;
            instructions[25] = (byte) 0;
            instructions[26] = (byte) 0;
            instructions[27] = (byte) 0x1d;

            instructions[28] = (byte) Bytecodes.BIPUSH;
            instructions[29] = (byte) values[0];

            instructions[30] = (byte) Bytecodes.BIPUSH;
            instructions[31] = (byte) values[1];

            instructions[32] = (byte) Bytecodes.BIPUSH;
            instructions[33] = (byte) values[2];

            instructions[34] = (byte) Bytecodes.BIPUSH;
            instructions[35] = (byte) values[3];

            initialiseFrameForCompilation(instructions);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, instructions, 36);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            theCompiler.peekInt(ARMV7.r3, 0);
            theCompiler.peekInt(ARMV7.r2, 1);
            theCompiler.peekInt(ARMV7.r1, 2);
            theCompiler.peekInt(ARMV7.r0, 3);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, ignorevalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            assert registerValues[1] == expectedValues[1] : "Failed incorrect value " + registerValues[1] + " " + expectedValues[1];
            assert registerValues[2] == expectedValues[2] : "Failed incorrect value " + registerValues[2] + " " + expectedValues[2];
            assert registerValues[3] == expectedValues[3] : "Failed incorrect value " + registerValues[3] + " " + expectedValues[3];
            theCompiler.cleanup();
        }
    }

    public void ignoreCallDirect() throws Exception {
    }

    public void ignoreCallIndirect() throws Exception {
    }

    public void ignoreNullCheck() throws Exception {
    }

    public void ignoreEmitPrologue() throws Exception {
    }

    public void ignoreEmitUnprotectMethod() throws Exception {
    }

    public void ignoreEmitEpilogue() throws Exception {
    }

    public void ignoreDo_preVolatileFieldAccess() throws Exception {
    }

    public void ignoreDo_postVolatileFieldAccess() throws Exception {
    }

    public void ignoreDo_tableswitch() throws Exception {
    }

    public void ignoreDo_lookupswitch() throws Exception {
    }

    public void ignoreCleanup() throws Exception {
    }

    public void ignoreAddObjectLiteralPatch() throws Exception {
    }

    public void ignoreFixup() throws Exception {
    }

    public void ignoreMovqDisp() throws Exception {
    }

    public void ignoreFindDataPatchPosns() throws Exception {
    }

    public void ignoreInvokeKind() throws Exception {
    }

    public void ignoreAssignmentTests() throws Exception {
    }

    public void ignoreAddJCC() throws Exception {
    }

    public void ignoreAddJMP() throws Exception {
    }

    public void ignoreAddJumpTableEntry() throws Exception {
    }

    public void ignoreAddLookupTableEntry() throws Exception {
    }

    public void ignoreAddObjectLiteral() throws Exception {
    }

    public void ignoreAlignDirectCall() throws Exception {
    }

    public void ignoreFramePointerAdjustment() throws Exception {
    }

    public void ignoreAssignObjectReg() throws Exception {
    }

    public void ignoreAssignWordReg() throws Exception {
    }

    public void ignoreAssignObject() throws Exception {
    }

    public void ignoreLoadWord() throws Exception {
    }

    public void ignoreLoadObject() throws Exception {
    }

    public void ignoreStoreWord() throws Exception {
    }

    public void ignoreStoreObject() throws Exception {
    }

}
