package test.arm.t1x;

import java.io.*;
import java.util.*;

import test.arm.asm.*;

import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.asm.target.aarch64.Aarch64Assembler.ConditionFlag;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.t1x.aarch64.*;

public class Aarch64T1XTest extends MaxTestCase {

    private Aarch64Assembler asm;
    private CiTarget aarch64;
    private ARMCodeWriter code;
    private T1X t1x;
    private C1X c1x;
    private AARCH64T1XCompilation theCompiler;
    private StaticMethodActor anMethod = null;
    private CodeAttribute codeAttr = null;
    private static boolean POST_CLEAN_FILES = true;

    public void initialiseFrameForCompilation() {
        // TODO: compute max stack
        codeAttr = new CodeAttribute(null, new byte[15], (char) 40, (char) 20, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create("(Ljava/util/Map;)V"), Actor.JAVA_METHOD_FLAGS, codeAttr, new String());
    }

    public void initialiseFrameForCompilation(byte[] code, String sig) {
        // TODO: compute max stack
        codeAttr = new CodeAttribute(null, code, (char) 40, (char) 20, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create(sig), Actor.JAVA_METHOD_FLAGS, codeAttr, new String());
    }

    public void initialiseFrameForCompilation(byte[] code, String sig, int flags) {
        // TODO: compute max stack
        codeAttr = new CodeAttribute(null, code, (char) 40, (char) 20, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create(sig), flags, codeAttr, new String());
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

    private static int[] valueTestSet = { 0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65535};
    private static long[] scratchTestSet = { 0, 1, 0xff, 0xffff, 0xffffff, 0xfffffff, 0x00000000ffffffffL};
    private static MaxineARMTester.BitsFlag[] bitmasks = new MaxineARMTester.BitsFlag[MaxineARMTester.NUM_REGS];
    static {
        for (int i = 0; i < MaxineARMTester.NUM_REGS; i++) {
            bitmasks[i] = MaxineARMTester.BitsFlag.All32Bits;
        }
    }
    private static long[] expectedValues = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static boolean[] testvalues = new boolean[17];

    private long[] generateAndTest(long[] expected, boolean[] tests, MaxineARMTester.BitsFlag[] masks) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(theCompiler.getMacroAssembler().codeBuffer);
        code.createCodeFile();
        MaxineARMTester r = new MaxineARMTester(expected, tests, masks);
        r.cleanFiles();
        r.cleanProcesses();
        r.assembleStartup();
        r.assembleEntry();
        r.compile();
        r.link();
        r.objcopy();
        long[] simulatedRegisters = r.runRegisteredSimulation();
        r.cleanProcesses();
        if (POST_CLEAN_FILES) {
            r.cleanFiles();
        }
        return simulatedRegisters;
    }

    public Aarch64T1XTest() {
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
            String baselineCompilerName = new String("com.oracle.max.vm.ext.t1x.T1X");
            String optimizingCompilerName = new String("com.oracle.max.vm.ext.c1x.C1X");
            RuntimeCompiler.baselineCompilerOption.setValue(baselineCompilerName);
            RuntimeCompiler.optimizingCompilerOption.setValue(optimizingCompilerName);
            if (initialised == false) {
                vmConfigurator.create();
                CompilationBroker.OFFLINE = true;
                JavaPrototype.initialize(false);
                initialised = true;
            }
            t1x = (T1X) CompilationBroker.addCompiler("t1x", baselineCompilerName);
           // c1x = (C1X) CompilationBroker.addCompiler("c1x", optimizingCompilerName);

            //c1x.initializeOffline(Phase.HOSTED_COMPILING);
            theCompiler = (AARCH64T1XCompilation) t1x.getT1XCompilation();
            theCompiler.setDebug(false);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Aarch64T1XTest.class);
    }

    public void ignore_DecStack() throws Exception {

    }

    public void test_IncStack() throws Exception {
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov(64, Aarch64.r0, Aarch64.sp); // copy stack value into r0
        theCompiler.incStack(1);
        masm.mov(64, Aarch64.r1, Aarch64.sp); // copy stack value onto r1
        theCompiler.incStack(2);
        masm.mov(64, Aarch64.r2, Aarch64.sp);

        long[] simulatedValues = generateAndTest(expectedValues, testvalues, bitmasks);
        for (int i = 0; i < 16; i++) {
            assert 2 * (simulatedValues[0] - simulatedValues[1]) == (simulatedValues[1] - simulatedValues[2]) : "Register " + i + " Value " + simulatedValues[i];
        }
    }

    public void ignore_AdjustReg() throws Exception {

    }

    public void ignore_PokeInt() throws Exception {

    }

    public void ignore_AssignLong() throws Exception {

    }

    public void work_PeekLong() throws Exception {

    }

    public void work_PokeLong() throws Exception {

    }

    public void ignore_PeekInt() throws Exception {

    }

    public void work_PokeDouble() throws Exception {

    }

    public void work_PeekFloat() throws Exception {

    }

    public void work_PokeFloat() throws Exception {

    }

    public void ignore_AssignDouble() throws Exception {

    }

    public void work_PeekDouble() throws Exception {

    }

    public void ignore_DoLconst() throws Exception {

    }

    public void ignore_DoDconst() throws Exception {

    }

    public void ignore_DoFconst() throws Exception {

    }

    public void ignore_DoLoad() throws Exception {

    }

    public void ignore_Add() throws Exception {

    }

    public void ignore_Mul() throws Exception {

    }

    public void ignore_PeekWord() throws Exception {

    }

    public void ignore_PokeWord() throws Exception {

    }

    public void ignore_PeekObject() throws Exception {

    }

    public void ignore_PokeObject() throws Exception {

    }

    public void failingtestIfEq() throws Exception {

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

    public void ignore_BranchBytecodes() throws Exception {

    }

    public void ignore_Locals() throws Exception {

    }

    public void ignore_ByteCodeLoad() throws Exception {

    }

    public void ignore_SwitchTable() throws Exception {

    }

    public void ignore_LookupTable() throws Exception {

    }
}
