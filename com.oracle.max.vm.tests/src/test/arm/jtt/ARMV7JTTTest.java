package test.arm.jtt;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.asm.sparc.FPR;
import com.sun.max.vm.Log;
import org.objectweb.asm.util.*;

import test.arm.asm.*;

import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.ConditionFlag;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.maxri.*;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.armv7.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

public class ARMV7JTTTest extends MaxTestCase {

    private ARMV7Assembler asm;
    private CiTarget armv7;
    private ARMCodeWriter code;
    private T1X t1x;
    private C1X c1x;
    private ARMV7T1XCompilation theCompiler;
    private StaticMethodActor anMethod = null;
    private CodeAttribute codeAttr = null;
    private static boolean POST_CLEAN_FILES = false;
    private int bufferSize = -1;
    private int entryPoint = -1;
    private byte[] codeBytes = null;

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

    static final class Args {

        public int first;
        public int second;
        public int third;
        public int fourth;
        public long lfirst;
        public long lsecond;

        public Args(int first, int second) {
            this.first = first;
            this.second = second;
        }

        public Args(int first, int second, int third) {
            this(first, second);
            this.third = third;
        }

        public Args(int first, int second, int third, int fourth) {
            this(first, second, third);
            this.fourth = fourth;
        }

        public Args(long lfirst, long lsecond) {
            this.lfirst = lfirst;
            this.lsecond = lsecond;
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
    private static MaxineARMTester.BitsFlag[] bitmasks = { MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits,
                    MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits,
                    MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits,
                    MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits, MaxineARMTester.BitsFlag.All32Bits};

    private static int[] expectedValues = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static long[] expectedLongValues = { Long.MIN_VALUE, Long.MAX_VALUE};
    private static boolean[] testvalues = new boolean[17];

    private Object[] generateObjectsAndTestStubs(String functionPrototype, int entryPoint, byte[] theCode, int assemblerStatements, int[] expected, boolean[] tests, MaxineARMTester.BitsFlag[] masks)
                    throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(assemblerStatements, theCode);
        // code.createCodeStubsFile(theCode,entryPoint);
        code.createStaticCodeStubsFile(functionPrototype, theCode, entryPoint);
        MaxineARMTester r = new MaxineARMTester(expected, tests, masks);
        r.cleanFiles();
        r.cleanProcesses();
        r.assembleStartup();
        r.assembleEntry();
        r.newCompile();
        r.link();
        r.objcopy();
        Object[] simulatedRegisters = r.runObjectRegisteredSimulation();
        r.cleanProcesses();
        if (POST_CLEAN_FILES) {
            r.cleanFiles();
        }
        return simulatedRegisters;
    }

    private int[] generateAndTestStubs(String functionPrototype, int entryPoint, byte[] theCode, int assemblerStatements, int[] expected, boolean[] tests, MaxineARMTester.BitsFlag[] masks)
                    throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(assemblerStatements, theCode);
        // code.createCodeStubsFile(theCode,entryPoint);
        code.createStaticCodeStubsFile(functionPrototype, theCode, entryPoint);
        MaxineARMTester r = new MaxineARMTester(expected, tests, masks);
        r.cleanFiles();
        r.cleanProcesses();
        r.assembleStartup();
        r.assembleEntry();
        r.newCompile();
        r.link();
        r.objcopy();
        int[] simulatedRegisters = r.runRegisteredSimulation();
        r.cleanProcesses();
        if (POST_CLEAN_FILES) {
            r.cleanFiles();
        }
        return simulatedRegisters;
    }

    private int[] generateAndTest(int assemblerStatements, int[] expected, boolean[] tests, MaxineARMTester.BitsFlag[] masks) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(assemblerStatements, theCompiler.getMacroAssembler().codeBuffer);
        code.createCodeFile();
        MaxineARMTester r = new MaxineARMTester(expected, tests, masks);
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

    private int[] generateAndTest(int assemblerStatements, long[] expected, boolean[] tests, MaxineARMTester.BitsFlag[] masks) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(assemblerStatements, theCompiler.getMacroAssembler().codeBuffer);
        code.createCodeFile();
        MaxineARMTester r = new MaxineARMTester(expected, tests, masks);
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

    public ARMV7JTTTest() {
        initTests();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ARMV7JTTTest.class);
    }

    private void initTests() {
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

            if (!RuntimeCompiler.baselineCompilerOption.isAssigned()) {
                RuntimeCompiler.baselineCompilerOption.setValue(baselineCompilerName);
                RuntimeCompiler.optimizingCompilerOption.setValue(optimizingCompilerName);

            }
            if (initialised == false) {
                vmConfigurator.create();
                CompilationBroker.OFFLINE = true;
                JavaPrototype.initialize(false);
                initialised = true;
            }
            t1x = (T1X) CompilationBroker.addCompiler("t1x", baselineCompilerName);
            c1x = (C1X) CompilationBroker.addCompiler("c1x", optimizingCompilerName);
            c1x.initializeOffline(Phase.HOSTED_COMPILING);
            theCompiler = (ARMV7T1XCompilation) t1x.getT1XCompilation();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public void IGNORE_jtt_UsageOfStaticMethods() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int value = 99;
        int answer = jtt.bytecode.ARM_BC_test_return1.test(12);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.ARM_BC_ignore_return1");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, value);
        masm.mov32BitConstant(ARMV7.r1, 12);
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r2)
        masm.push(ConditionFlag.Always, 1); // local slot 0 is return (int is one slot) last push to stack is 0
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_C1X_jtt_BC_imul() throws Exception {
        initTests();
        boolean failed = false;
/*
 * @Harness: java
 *
 * @Runs: (1,2)=2; (0,-1)=0; (33,67)=2211; (1,-1)=-1;
 *
 * @Runs: (-2147483648,1)=-2147483648; (2147483647,-1)=-2147483647;
 *
 * @Runs: (-2147483648,-1)=-2147483648
 */

        int argsOne[] = { 1, 0, 33, 1, -2147483648, 2147483647, -2147483648};
        int argsTwo[] = { 12, -1, 67, -1, 1, -1, -1};

        String klassName = "jtt.bytecode.BC_imul";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_imul.java", "int test(int, int)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_imul.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[0].equals(new Integer(expectedValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
            }
            Log.println("IMUL test " + i + " returned " + ((Integer) registerValues[0]).intValue() + " expected " + expectedValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;

    }

    public void IGNORE_T1Xjtt_BC_iadd2() throws Exception {

/*
 * @Harness: java
 *
 * @Runs: (1b,2b)=3; (0b,-1b)=-1; (33b,67b)=100; (1b, -1b)=0;
 *
 * @Runs: (-128b,1b)=-127; (127b,1b)=128;
 */
        boolean failed = false;
        byte argsOne[] = { 1, 0, 33, 1, -128, 127};
        byte argsTwo[] = { 2, -1, 67, -1, 1, 1};
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");

        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iadd2");

        for (int i = 0; i < argsOne.length; i++) {
            int answer = jtt.bytecode.BC_iadd2.test(argsOne[i], argsTwo[i]);
            expectedValues[0] = answer;
            // TODO is this really right?
            initialiseFrameForCompilation(code, "(BB)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, (int) argsOne[i]);
            masm.mov32BitConstant(ARMV7.r1, (int) argsTwo[i]);
            masm.mov32BitConstant(ARMV7.r2, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            masm.push(ConditionFlag.Always, 4); // local slot 0 is return (int is one slot) last push to stack is
            // t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            theCompiler.cleanup();
            if (registerValues[0] != answer) {
                failed = true;
            }
            Log.println("T1X iadd " + registerValues[0] + " " + answer);
        }
        assert (failed == false);

    }

    public void IGNORE_T1X_jtt_BC_iadd3() throws Exception {
        initTests();
        boolean failed = false;

        /*
         * @Harness: java
         *
         * @Runs: (1s,2s)=3; (0s,-1s)=-1; (33s,67s)=100; (1s, -1s)=0;
         *
         * @Runs: (-128s,1s)=-127; (127s,1s)=128;
         *
         * @Runs: (-32768s,1s)=-32767; (32767s,1s)=32768;
         */

        short argsOne[] = { 1, 0, 33, 1, -128, 127, -32768, 32767};
        short argsTwo[] = { 2, -1, 67, -1, 1, 1, 1, 1};
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");

        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iadd3");

        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd3.test(argsOne[i], argsTwo[i]);
            initialiseFrameForCompilation(code, "(SS)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, (int) argsOne[i]);
            masm.mov32BitConstant(ARMV7.r1, (int) argsTwo[i]);
            masm.mov32BitConstant(ARMV7.r2, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            masm.push(ConditionFlag.Always, 4); // local slot 0 is return (int is one slot) last push to stack is
            // t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            theCompiler.cleanup();
            if (registerValues[0] != expectedValue) {
                failed = true;
            }

            Log.println("T1X IADD3 test " + i + " returned " + registerValues[0] + " expected " + expectedValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_imul() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_imul.test(10, 12);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_imul");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 10);
        masm.mov32BitConstant(ARMV7.r1, 12);
        masm.mov32BitConstant(ARMV7.r2, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        masm.push(ConditionFlag.Always, 4); // local slot 0 is return (int is one slot) last push to stack is
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "imul");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_isub() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_isub.test(100, 50);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_isub");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 100);
        masm.mov32BitConstant(ARMV7.r1, 50);
        masm.mov32BitConstant(ARMV7.r2, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        masm.push(ConditionFlag.Always, 4); // local slot 0 is return (int is one slot) last push to stack is
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "isub");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ineg() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ineg.test(100);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ineg");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 100);
        masm.mov32BitConstant(ARMV7.r1, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ineg");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ineg_1() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ineg.test(-100);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ineg");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, -100);
        masm.mov32BitConstant(ARMV7.r1, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ineg");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_iadd() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iadd.test(50, 50);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iadd");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 50);
        masm.mov32BitConstant(ARMV7.r1, 50);
        masm.mov32BitConstant(ARMV7.r2, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        masm.push(ConditionFlag.Always, 4); // local slot 0 is return (int is one slot) last push to stack is
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ior() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ior.test(50, 100);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ior");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 50);
        masm.mov32BitConstant(ARMV7.r1, 100);
        masm.mov32BitConstant(ARMV7.r2, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        masm.push(ConditionFlag.Always, 4); // local slot 0 is return (int is one slot) last push to stack is
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ior");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ixor() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ixor.test(50, 39);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ixor");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 50);
        masm.mov32BitConstant(ARMV7.r1, 39);
        masm.mov32BitConstant(ARMV7.r2, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        masm.push(ConditionFlag.Always, 4); // local slot 0 is return (int is one slot) last push to stack is
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ixor");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_iand() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iand.test(50, 39);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iand");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 50);
        masm.mov32BitConstant(ARMV7.r1, 39);
        masm.mov32BitConstant(ARMV7.r2, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        masm.push(ConditionFlag.Always, 4); // local slot 0 is return (int is one slot) last push to stack is
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iand");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ishl() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ishl.test(10, 2);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ishl");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 10);
        masm.mov32BitConstant(ARMV7.r1, 2);
        masm.mov32BitConstant(ARMV7.r2, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        masm.push(ConditionFlag.Always, 4); // local slot 0 is return (int is one slot) last push to stack is
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishl");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ishr() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ishr.test(2048, 2);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ishr");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 2048);
        masm.mov32BitConstant(ARMV7.r1, 2);
        masm.mov32BitConstant(ARMV7.r2, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        masm.push(ConditionFlag.Always, 4); // local slot 0 is return (int is one slot) last push to stack is
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishr");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ishr_1() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ishr.test(-2147483648, 16);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ishr");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, -2147483648);
        masm.mov32BitConstant(ARMV7.r1, 16);
        masm.mov32BitConstant(ARMV7.r2, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        masm.push(ConditionFlag.Always, 4); // local slot 0 is return (int is one slot) last push to stack is
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishr");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_iushr() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iushr.test(-2147483648, 16);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iushr");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, -2147483648);
        masm.mov32BitConstant(ARMV7.r1, 16);
        masm.mov32BitConstant(ARMV7.r2, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        masm.push(ConditionFlag.Always, 4); // local slot 0 is return (int is one slot) last push to stack is
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iushr");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2b() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2b.test(255);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2b");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 255);
        masm.mov32BitConstant(ARMV7.r1, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2b_1() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2b.test(-1);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2b");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, -1);
        masm.mov32BitConstant(ARMV7.r1, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2b_2() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2b.test(128);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2b");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 128);
        masm.mov32BitConstant(ARMV7.r1, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2s() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2s.test(65535);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 65535);
        masm.mov32BitConstant(ARMV7.r1, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2s_1() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2s.test(32768);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 32768);
        masm.mov32BitConstant(ARMV7.r1, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2s_2() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2s.test(-1);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, -1);
        masm.mov32BitConstant(ARMV7.r1, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2c() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2c.test(-1);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2c");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, -1);
        masm.mov32BitConstant(ARMV7.r1, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2c");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2c_1() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2c.test(65535);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2c");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 65535);
        masm.mov32BitConstant(ARMV7.r1, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2c");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ireturn() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        int answer = jtt.bytecode.BC_ireturn.test(-1);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ireturn");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, -1);
        masm.mov32BitConstant(ARMV7.r1, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ireturn_1() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        int answer = jtt.bytecode.BC_ireturn.test(256);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ireturn");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, 256);
        masm.mov32BitConstant(ARMV7.r1, -99);
        masm.push(ConditionFlag.Always, 1); // local slot is argument r0
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_tableswitch() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 42));
        pairs.add(new Args(0, 10));
        pairs.add(new Args(1, 20));
        pairs.add(new Args(2, 30));
        pairs.add(new Args(3, 42));
        pairs.add(new Args(4, 40));
        pairs.add(new Args(5, 50));
        pairs.add(new Args(6, 42));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_tableswitch.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_tableswitch");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_tableswitch_2() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 11));
        pairs.add(new Args(0, 11));
        pairs.add(new Args(1, 11));
        pairs.add(new Args(5, 55));
        pairs.add(new Args(6, 66));
        pairs.add(new Args(7, 77));
        pairs.add(new Args(8, 11));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_tableswitch2.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_tableswitch2");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNOREignore_jtt_BC_XXXXfdiv() throws Exception {
        boolean failed = false;
        MaxineARMTester.DEBUGOBJECTS = true;
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        float argOne[] = { 14.0f};
        float argTwo[] = { 7.0f};
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_fdiv");

        for (int i = 0; i < argOne.length; i++) {
            float answer = jtt.bytecode.BC_fdiv.test(argOne[i], argTwo[i]);
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.s0, Float.floatToRawIntBits(argOne[i]));
            masm.mov32BitConstant(ARMV7.s1, -Float.floatToRawIntBits(argTwo[i]));
            masm.vpush(ConditionFlag.Always, ARMV7.s0, ARMV7.s1); // / MIGHT need to do this as two pushes?
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "fdiv");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.vpop(ConditionFlag.Always, ARMV7.s0, ARMV7.s0);
            int assemblerStatements = masm.codeBuffer.position() / 4;

            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float", Float.toString(argOne[i]) + Float.toString(argTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, 0, masm.codeBuffer.close(false), assemblerStatements, expectedValues, testvalues, bitmasks);
            System.out.println(" FDIV T1X " + ((Float) registerValues[33]).floatValue());
        }
        MaxineARMTester.DEBUGOBJECTS = false;

        // assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " +
// expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_tableswitch_3() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 11));
        pairs.add(new Args(-2, 22));
        pairs.add(new Args(-3, 99));
        pairs.add(new Args(-4, 99));
        pairs.add(new Args(1, 77));
        pairs.add(new Args(2, 99));
        pairs.add(new Args(10, 99));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_tableswitch3.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_tableswitch3");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_tableswitch_4() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 11));
        pairs.add(new Args(0, 11));
        pairs.add(new Args(1, 11));
        pairs.add(new Args(-5, 55));
        pairs.add(new Args(-4, 44));
        pairs.add(new Args(-3, 33));
        pairs.add(new Args(-8, 11));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_tableswitch4.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_tableswitch4");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_lookupswitch_1() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 42));
        pairs.add(new Args(1, 42));
        pairs.add(new Args(66, 42));
        pairs.add(new Args(67, 0));
        pairs.add(new Args(68, 42));
        pairs.add(new Args(96, 42));
        pairs.add(new Args(97, 1));
        pairs.add(new Args(98, 42));
        pairs.add(new Args(106, 42));
        pairs.add(new Args(107, 2));
        pairs.add(new Args(108, 42));
        pairs.add(new Args(132, 42));
        pairs.add(new Args(133, 3));
        pairs.add(new Args(134, 42));
        pairs.add(new Args(211, 42));
        pairs.add(new Args(212, 4));
        pairs.add(new Args(213, 42));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_lookupswitch01.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_lookupswitch01");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_lookupswitch_2() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 42));
        pairs.add(new Args(1, 42));
        pairs.add(new Args(66, 42));
        pairs.add(new Args(67, 0));
        pairs.add(new Args(68, 42));
        pairs.add(new Args(96, 42));
        pairs.add(new Args(97, 1));
        pairs.add(new Args(98, 42));
        pairs.add(new Args(106, 42));
        pairs.add(new Args(107, 2));
        pairs.add(new Args(108, 42));
        pairs.add(new Args(132, 42));
        pairs.add(new Args(133, 3));
        pairs.add(new Args(134, 42));
        pairs.add(new Args(211, 42));
        pairs.add(new Args(212, 4));
        pairs.add(new Args(213, 42));
        pairs.add(new Args(-121, 42));
        pairs.add(new Args(-122, 42));
        pairs.add(new Args(-123, 42));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_lookupswitch02.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_lookupswitch02");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_lookupswitch_3() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 42));
        pairs.add(new Args(1, 42));
        pairs.add(new Args(66, 42));
        pairs.add(new Args(67, 0));
        pairs.add(new Args(68, 42));
        pairs.add(new Args(96, 42));
        pairs.add(new Args(97, 1));
        pairs.add(new Args(98, 42));
        pairs.add(new Args(106, 42));
        pairs.add(new Args(107, 2));
        pairs.add(new Args(108, 42));
        pairs.add(new Args(132, 42));
        pairs.add(new Args(133, 3));
        pairs.add(new Args(134, 42));
        pairs.add(new Args(211, 42));
        pairs.add(new Args(212, 4));
        pairs.add(new Args(213, 42));
        pairs.add(new Args(-121, 42));
        pairs.add(new Args(-122, 5));
        pairs.add(new Args(-123, 42));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_lookupswitch03.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_lookupswitch03");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_lookupswitch_4() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 42));
        pairs.add(new Args(1, 42));
        pairs.add(new Args(66, 42));
        pairs.add(new Args(67, 0));
        pairs.add(new Args(68, 42));
        pairs.add(new Args(96, 42));
        pairs.add(new Args(97, 1));
        pairs.add(new Args(98, 42));
        pairs.add(new Args(106, 42));
        pairs.add(new Args(107, 2));
        pairs.add(new Args(108, 42));
        pairs.add(new Args(132, 42));
        pairs.add(new Args(133, 3));
        pairs.add(new Args(134, 42));
        pairs.add(new Args(211, 42));
        pairs.add(new Args(212, 4));
        pairs.add(new Args(213, 42));
        pairs.add(new Args(-121, 42));
        pairs.add(new Args(-122, 5));
        pairs.add(new Args(-123, 42));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_lookupswitch04.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_lookupswitch04");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iinc_1() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(2, 3));
        pairs.add(new Args(4, 5));
        pairs.add(new Args(1, 0));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iinc_1.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iinc_1");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_1");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iinc_2() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 3));
        pairs.add(new Args(2, 4));
        pairs.add(new Args(4, 6));
        pairs.add(new Args(-2, 0));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iinc_2.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iinc_2");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iinc_3() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 52));
        pairs.add(new Args(2, 53));
        pairs.add(new Args(4, 55));
        pairs.add(new Args(-1, 50));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iinc_3.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iinc_3");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iinc_4() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 513));
        pairs.add(new Args(2, 514));
        pairs.add(new Args(4, 516));
        pairs.add(new Args(-1, 511));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iinc_4.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iinc_4");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_4");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_0() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(-1, -1));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(1000345, 1000345));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iload_0.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iload_0");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_0_1() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 1));
        pairs.add(new Args(-1, 0));
        pairs.add(new Args(2, 3));
        pairs.add(new Args(1000345, 1000346));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iload_0_1.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iload_0_1");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0_1");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_0_2() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(-1, -1));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(1000345, 1000345));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iload_0_2.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iload_0_2");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_1() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 0));
        // pairs.add(new Args(1, -1));
        // pairs.add(new Args(1, 2));
        // pairs.add(new Args(1, 1000345));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iload_1.test(pair.first, pair.second);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iload_1");
            initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, pair.second);
            masm.mov32BitConstant(ARMV7.r2, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            masm.push(ConditionFlag.Always, 4); // local slot 1 is argument (r1)
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_1_1() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(-1, -1));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(1000345, 1000345));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iload_1_1.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iload_1_1");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_1_1");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_2() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1, 0));
        pairs.add(new Args(1, 1, -1));
        pairs.add(new Args(1, 1, 2));
        pairs.add(new Args(1, 1, 1000345));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iload_2.test(pair.first, pair.second, pair.third);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iload_2");
            initialiseFrameForCompilation(code, "(III)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, pair.second);
            masm.mov32BitConstant(ARMV7.r2, pair.third);
            masm.mov32BitConstant(ARMV7.r3, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            masm.push(ConditionFlag.Always, 4); // local slot 1 is argument (r1)
            masm.push(ConditionFlag.Always, 8); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_3() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1, 1, 0));
        pairs.add(new Args(1, 1, 1, -1));
        pairs.add(new Args(1, 1, 1, 2));
        pairs.add(new Args(1, 1, 1, 1000345));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iload_3.test(pair.first, pair.second, pair.third, pair.fourth);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iload_3");
            initialiseFrameForCompilation(code, "(IIII)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, pair.second);
            masm.mov32BitConstant(ARMV7.r2, pair.third);
            masm.mov32BitConstant(ARMV7.r3, pair.fourth);
            masm.mov32BitConstant(ARMV7.r4, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            masm.push(ConditionFlag.Always, 4); // local slot 1 is argument (r1)
            masm.push(ConditionFlag.Always, 8); // local slot 1 is argument (r1)
            masm.push(ConditionFlag.Always, 16); // local slot 1 is argument (r1
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iconst() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(3, 3));
        pairs.add(new Args(4, 4));
        pairs.add(new Args(5, 5));
        pairs.add(new Args(6, 375));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iconst.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iconst");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iconst");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ifeq() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 2));
        pairs.add(new Args(1, -2));
        pairs.add(new Args(6, 375));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_ifeq.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ifeq");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ifeq_2() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 1));
        pairs.add(new Args(1, 0));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_ifeq_2.test(pair.first);
            expectedValues[0] = answer ? 1 : 0;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ifeq_2");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ifeq_3() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_ifeq_3.test(pair.first);
            expectedValues[0] = answer ? 1 : 0;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ifeq_3");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ifge() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(-1, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_ifge.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ifge");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ifgt() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(-1, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_ifgt.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ifgt");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifgt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ifle() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(-1, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_ifle.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ifle");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifle");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ifne() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(-1, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_ifne.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ifne");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifne");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iflt() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(-1, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iflt.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iflt");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ificmplt1() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_ificmplt1.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ificmplt1");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ificmplt2() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_ificmplt2.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ificmplt2");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ificmpne1() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_ificmpne1.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ificmpne1");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ificmpne2() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_ificmpne2.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ificmpne2");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ifge_3() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 1));
        pairs.add(new Args(1, -0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(0, -100));
        pairs.add(new Args(-1, 0));
        pairs.add(new Args(-12, -12));

        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            expectedValues[0] = jtt.bytecode.BC_ifge_3.test(pair.first, pair.second) ? 1 : 0;

            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ifge_3");
            initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, pair.second);

            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)

            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifgt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ifge_2() throws Exception {
        boolean failed = false;
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 2));
        pairs.add(new Args(1, -2));
        pairs.add(new Args(6, 375));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_ifge_2.test(pair.first, pair.second);
            if (answer == true) {
                expectedValues[0] = 1;
            } else {
                expectedValues[0] = 0;
            }
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ifge_2");
            initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, pair.second);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            masm.push(ConditionFlag.Always, 2); // EXTRA ARg to fix stack issue?

            // t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifge");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValues[0]) {
                failed = true;
            }
            Log.println("IFGE_2 " + pair.first + " " + pair.second + " GOT " + registerValues[0] + " EXPECTED " + expectedValues[0]);
            theCompiler.cleanup();
        }
        assert (failed == false);
    }

    private void initialiseCodeBuffers(List<TargetMethod> methods) {
        int minimumValue = Integer.MAX_VALUE;
        int maximumValue = Integer.MIN_VALUE;
        int offset;
        entryPoint = -1; // offset in the global array of the method we call from C.
        for (TargetMethod m : methods) {

            System.out.println(m.classMethodActor().simpleName());
            System.out.println(m.classMethodActor().sourceFileName());

            byte[] b = m.code();
            if (entryPoint == -1) {
                entryPoint = m.codeAt(0).toInt();
            }
            if ((m.codeAt(0)).toInt() < minimumValue) {
                minimumValue = m.codeAt(0).toInt(); // UPDATE MINIMUM OFFSET IN ADDRESS SPACE
            }
            if ((m.codeAt(0)).toInt() + b.length > maximumValue) {
                maximumValue = m.codeAt(0).toInt() + b.length; // UPDATE MAXIMUM OFFSET IN ADDRESS SPACE
            }
        }

        if (MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt() < minimumValue) {
            minimumValue = MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt();
        }

        if ((MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt() + MaxineVM.vm().stubs.staticTrampoline().code().length) > maximumValue) {
            maximumValue = MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt() + MaxineVM.vm().stubs.staticTrampoline().code().length;
        }

        codeBytes = new byte[maximumValue - minimumValue];
        for (TargetMethod m : methods) { // CRUDE ATTEMPT TO COPY MACHINE CODE BUFFERS!
            m.linkDirectCalls();
            byte[] b = m.code();
            offset = m.codeAt(0).toInt() - minimumValue;
            for (int i = 0; i < b.length; i++) {
                codeBytes[offset + i] = b[i];
            }
        }
        byte[] b = MaxineVM.vm().stubs.staticTrampoline().code();
        offset = MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt() - minimumValue;
        for (int i = 0; i < b.length; i++) {
            codeBytes[i + offset] = b[i];
        }
        entryPoint = entryPoint - minimumValue;
    }

    private void initialiseCodeBuffers(List<TargetMethod> methods, String fileName, String methodName) {
        int minimumValue = Integer.MAX_VALUE;
        int maximumValue = Integer.MIN_VALUE;
        int offset;
        entryPoint = -1; // offset in the global array of the method we call from C.
        for (TargetMethod m : methods) {
            m.linkDirectCalls();
            if (!fileName.equals(m.classMethodActor.sourceFileName())) {
                continue;
            }
            System.out.println(m.classMethodActor().simpleName());
            System.out.println(m.classMethodActor().sourceFileName());

            byte[] b = m.code();
            if (entryPoint == -1) {
                if (methodName.equals(m.classMethodActor().simpleName())) {
                    entryPoint = m.codeAt(0).toInt();
                }
            }
            if ((m.codeAt(0)).toInt() < minimumValue) {
                minimumValue = m.codeAt(0).toInt(); // UPDATE MINIMUM OFFSET IN ADDRESS SPACE
            }
            if ((m.codeAt(0)).toInt() + b.length > maximumValue) {
                maximumValue = m.codeAt(0).toInt() + b.length; // UPDATE MAXIMUM OFFSET IN ADDRESS SPACE
            }
            int tmp = m.offlineMinDirectCalls();
            if (tmp < minimumValue) {
                minimumValue = tmp;
            }
            tmp = m.offlineMaxDirectCalls();
            if (tmp > maximumValue) {
                maximumValue = tmp;
            }
        }

        if (MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt() < minimumValue) {
            minimumValue = MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt();
        }

        if ((MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt() + MaxineVM.vm().stubs.staticTrampoline().code().length) > maximumValue) {
            maximumValue = MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt() + MaxineVM.vm().stubs.staticTrampoline().code().length;
        }

        codeBytes = new byte[maximumValue - minimumValue];
        for (TargetMethod m : methods) { // CRUDE ATTEMPT TO COPY MACHINE CODE BUFFERS!
            // m.linkDirectCalls();
            if (!fileName.equals(m.classMethodActor.sourceFileName())) {
                continue;
            }
            byte[] b = m.code();
            offset = m.codeAt(0).toInt() - minimumValue;
            for (int i = 0; i < b.length; i++) {
                codeBytes[offset + i] = b[i];
            }
            m.offlineCopyCode(minimumValue, codeBytes);
        }
        byte[] b = MaxineVM.vm().stubs.staticTrampoline().code();
        offset = MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt() - minimumValue;
        for (int i = 0; i < b.length; i++) {
            codeBytes[i + offset] = b[i];
        }
        entryPoint = entryPoint - minimumValue;
    }

    public void IGNORE_jtt_BC_invokestatic() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_invokestatic";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");

        CompilationBroker.OFFLINE = true;
        // initialised = true;
        // initTests();

        // ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        /*
         * WE NEED A CLEANED UP WAY TO .. COPY THE CODE INTO AN ALIGNED BUFFER, WE NEED TO BE ABLE TO MATCH THE
         * MaxTargetMethod with the one we want to call, and then to extract its entry point ... IVE CHEATED HERE AND
         * JUST USED THE FIRST ONE AS I KNOW THAT IS "test" IN THIS INSTANCE
         */
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(-2, -2));

        for (Args pair : pairs) {
            // MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_invokestatic.test(pair.first);
            expectedValues[0] = answer;
            /*
             * SEE BELOW, WE NEED TO PROVIDE A COMMA SEPARATED LIST OF TYPES "int" void (*pf)(***int***) = (void
             * (*))(code); print_uart0("changed test.c!\n"); AND WE NEED TO PROVIDE A COMMA SEPARATED LIST OF FUNCTION
             * ARGUMENT VALUES / VARIABLES (*pf)(****1*****); // Need to change this to something related to the test
             * itself asm volatile("forever: b forever");
             */
            String functionPrototype = ARMCodeWriter.preAmble("void", "int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            // theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_f2d() throws Exception {

        boolean failed = false;
        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_f2d";
        MaxineARMTester.DEBUGOBJECTS = false;
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        /*
         * WE NEED A CLEANED UP WAY TO .. COPY THE CODE INTO AN ALIGNED BUFFER, WE NEED TO BE ABLE TO MATCH THE
         * MaxTargetMethod with the one we want to call, and then to extract its entry point ... IVE CHEATED HERE AND
         * JUST USED THE FIRST ONE AS I KNOW THAT IS "test" IN THIS INSTANCE
         */
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        float[] arguments = { -2.2f, 0.0f, 1.0f, 01.06f};
        double expectedDouble = -9;
        for (int i = 0; i < arguments.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            double answer = jtt.bytecode.BC_f2d.test(arguments[i]);
            expectedDouble = answer;
            /*
             * SEE BELOW, WE NEED TO PROVIDE A COMMA SEPARATED LIST OF TYPES "int" void (*pf)(***int***) = (void
             * (*))(code); print_uart0("changed test.c!\n"); AND WE NEED TO PROVIDE A COMMA SEPARATED LIST OF FUNCTION
             * ARGUMENT VALUES / VARIABLES (*pf)(****1*****); // Need to change this to something related to the test
             * itself asm volatile("forever: b forever");
             */
            // MaxineARMTester.DEBUGOBJECTS = true;
            String functionPrototype = ARMCodeWriter.preAmble("double", "float", Float.toString(arguments[i]));
            // good question here ... is the value returned in the float s0 or the core s0 register
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (((Double) registerValues[17]).doubleValue() != expectedDouble) {
                System.out.println("Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + expectedDouble);
            }
            MaxineARMTester.DEBUGOBJECTS = false;
            if (!failed) {
                failed = ((Double) registerValues[17]).doubleValue() != expectedDouble;
            }
            theCompiler.cleanup();
        }
        assert (failed == false);

    }

    public void IGNORE_jtt_BC_i2f() throws Exception {

        boolean failed = false;
        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_i2f";
        MaxineARMTester.DEBUGOBJECTS = false;
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        /*
         * WE NEED A CLEANED UP WAY TO .. COPY THE CODE INTO AN ALIGNED BUFFER, WE NEED TO BE ABLE TO MATCH THE
         * MaxTargetMethod with the one we want to call, and then to extract its entry point ... IVE CHEATED HERE AND
         * JUST USED THE FIRST ONE AS I KNOW THAT IS "test" IN THIS INSTANCE
         */
        initialiseCodeBuffers(methods, "BC_i2f.java", "float test(int)");
        int assemblerStatements = codeBytes.length / 4;
        int[] arguments = { -2, 0, 1, -1, -99};
        float expectedFloat = -9;
        for (int i = 0; i < arguments.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            float answer = jtt.bytecode.BC_i2f.test(arguments[i]);
            expectedFloat = answer;
            /*
             * SEE BELOW, WE NEED TO PROVIDE A COMMA SEPARATED LIST OF TYPES "int" void (*pf)(***int***) = (void
             * (*))(code); print_uart0("changed test.c!\n"); AND WE NEED TO PROVIDE A COMMA SEPARATED LIST OF FUNCTION
             * ARGUMENT VALUES / VARIABLES (*pf)(****1*****); // Need to change this to something related to the test
             * itself asm volatile("forever: b forever");
             */
            MaxineARMTester.DEBUGOBJECTS = false;
            String functionPrototype = ARMCodeWriter.preAmble("float", "int", Integer.toString(arguments[i]));
            // good question here ... is the value returned in the float s0 or the core s0 register
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (((Float) registerValues[33]).floatValue() != expectedFloat) {
                System.out.println("Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + expectedFloat);
            }
            MaxineARMTester.DEBUGOBJECTS = false;
            if (!failed) {
                failed = ((Float) registerValues[33]).floatValue() != expectedFloat;
            }
            theCompiler.cleanup();
        }
        assert (failed == false);

    }

    public void IGNORE_jtt_BC_d2f() throws Exception {

        boolean failed = false;
        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_d2f";
        MaxineARMTester.DEBUGOBJECTS = false;
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        /*
         * WE NEED A CLEANED UP WAY TO .. COPY THE CODE INTO AN ALIGNED BUFFER, WE NEED TO BE ABLE TO MATCH THE
         * MaxTargetMethod with the one we want to call, and then to extract its entry point ... IVE CHEATED HERE AND
         * JUST USED THE FIRST ONE AS I KNOW THAT IS "test" IN THIS INSTANCE
         */
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        double[] arguments = { -2.2d, 0.0d, 1.0d, 01.06d};
        float expectedFloat = -9;
        for (int i = 0; i < arguments.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            float answer = jtt.bytecode.BC_d2f.test(arguments[i]);
            expectedFloat = answer;
            /*
             * SEE BELOW, WE NEED TO PROVIDE A COMMA SEPARATED LIST OF TYPES "int" void (*pf)(***int***) = (void
             * (*))(code); print_uart0("changed test.c!\n"); AND WE NEED TO PROVIDE A COMMA SEPARATED LIST OF FUNCTION
             * ARGUMENT VALUES / VARIABLES (*pf)(****1*****); // Need to change this to something related to the test
             * itself asm volatile("forever: b forever");
             */
            MaxineARMTester.DEBUGOBJECTS = true;
            String functionPrototype = ARMCodeWriter.preAmble("float", "double", Double.toString(arguments[i]));
            // good question here ... is the value returned in the float s0 or the core s0 register
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (((Float) registerValues[33]).floatValue() != expectedFloat) {
                System.out.println("Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + expectedFloat);
            }
            MaxineARMTester.DEBUGOBJECTS = false;
            if (!failed) {
                failed = ((Float) registerValues[33]).floatValue() != expectedFloat;
            }
            theCompiler.cleanup();
        }
        assert (failed == false);

    }

    public void IGNORE_jtt_BC_d2i01() throws Exception {

        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_d2i01";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");

        initialiseCodeBuffers(methods, "BC_d2i01.java", "int test(double)");
        int assemblerStatements = codeBytes.length / 4;
        double[] arguments = { 0.0d, 1.0d, 01.06d, -2.2d};
        int expectedInt = -9;
        for (int i = 0; i < arguments.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_d2i01.test(arguments[i]);
            expectedInt = answer;
            System.out.println("testing d2i01 " + arguments[i] + " " + answer);
            /*
             * SEE BELOW, WE NEED TO PROVIDE A COMMA SEPARATED LIST OF TYPES "int" void (*pf)(***int***) = (void
             * (*))(code); print_uart0("changed test.c!\n"); AND WE NEED TO PROVIDE A COMMA SEPARATED LIST OF FUNCTION
             * ARGUMENT VALUES / VARIABLES (*pf)(****1*****); // Need to change this to something related to the test
             * itself asm volatile("forever: b forever");
             */
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(arguments[i]));
            // good question here ... is the value returned in the float s0 or the core s0 register
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);

            assert ((Integer) registerValues[0]).intValue() == expectedInt : "Failed incorrect value " + ((Integer) registerValues[0]).intValue() + " " + expectedInt;
            theCompiler.cleanup();
        }

    }

    public void IGNORE_jtt_BC_d2i02t1x() throws Exception {
        initTests();

        boolean failed = false;
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (int i = 0; i < 5; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            double answer = jtt.bytecode.BC_d2i02.test(i);

            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_d2i02");
            initialiseFrameForCompilation(code, "(I)D", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, i);
            masm.mov32BitConstant(ARMV7.r1, -99);

            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r1)
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifge_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            String functionPrototype = ARMCodeWriter.preAmble("double ", "int", Integer.toString(i));
            entryPoint = 0;
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, masm.codeBuffer.copyData(0, masm.codeBuffer.position()), assemblerStatements, expectedValues,
                            testvalues, bitmasks);

            assert ((Double) registerValues[17]).doubleValue() == answer : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_d2i02() throws Exception {
        boolean failed = false;
        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_d2i02";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");

        initialiseCodeBuffers(methods, "BC_d2i02.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedInt = -9;
        for (int i = 0; i < 5; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_d2i02.test(i);
            /*
             * SEE BELOW, WE NEED TO PROVIDE A COMMA SEPARATED LIST OF TYPES "int" void (*pf)(***int***) = (void
             * (*))(code); print_uart0("changed test.c!\n"); AND WE NEED TO PROVIDE A COMMA SEPARATED LIST OF FUNCTION
             * ARGUMENT VALUES / VARIABLES (*pf)(****1*****); // Need to change this to something related to the test
             * itself asm volatile("forever: b forever");
             */
            String functionPrototype = ARMCodeWriter.preAmble("double ", "int", Integer.toString(i));
            // good question here ... is the value returned in the float s0 or the core s0 register
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!failed) {
                failed = (((Integer) registerValues[0]).intValue() != answer);
            }
            Log.println("D2I02 returned " + ((Integer) registerValues[0]).intValue() + " expected  " + answer);
            theCompiler.cleanup();
        }
        assert (!failed);

    }

    public void IGNORE_BC_anewarray() throws Exception {

        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_anewarray";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");

        initialiseCodeBuffers(methods, "BC_anewarray.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        int[] arguments = { 0, 1};
        int expectedInt = -9;
        for (int i = 0; i < arguments.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_anewarray.test(arguments[i]);
            expectedInt = answer;

            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(arguments[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);

            assert registerValues[0] == expectedInt : "Failed incorrect value " + registerValues[0] + " " + expectedInt;
            theCompiler.cleanup();
        }

    }

    public void IGNORE_BC_new() throws Exception {

        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_new";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");

        initialiseCodeBuffers(methods, "BC_new.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        int[] arguments = { 0, 1};
        int expectedInt = -9;
        for (int i = 0; i < arguments.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_new.test(arguments[i]);
            expectedInt = answer;

            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(arguments[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);

            assert registerValues[0] == expectedInt : "Failed incorrect value " + registerValues[0] + " " + expectedInt;
            theCompiler.cleanup();
        }

    }

    public void IGNORE_jtt_BC_f2i01() throws Exception {

        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_f2i01";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");

        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        float[] arguments = { -2.2f, 0.0f, 1.0f, 1.06f};
        int expectedInt = -9;
        for (int i = 0; i < arguments.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_f2i01.test(arguments[i]);
            expectedInt = answer;

            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(arguments[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);

            assert registerValues[0] == expectedInt : "Failed incorrect value " + registerValues[0] + " " + expectedInt;
            theCompiler.cleanup();
        }

    }

    public void IGNORE_jtt_BC_f2i02() throws Exception {

        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_f2i02";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
/*
 * @Harness: java
 *
 * @Runs: 0 = -2147483648; 1 = -2147483648; 2 = 0; 3 = 2147483647; 4 = 2147483647
 */

        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;

        int expectedInt = -9;
        for (int i = 0; i < 5; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            float answer = jtt.bytecode.BC_f2i02.test(i);

            String functionPrototype = ARMCodeWriter.preAmble("float", "int", Integer.toString(i));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);

            assert ((Float) registerValues[33]).floatValue() == answer : "Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + answer;
            theCompiler.cleanup();
        }

    }

    public void IGNORE_jtt_BC_i2d() throws Exception {

        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_i2d";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");

        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        int[] arguments = { -2, 0, 1, 2, 99};
        double expectedDouble = -9;
        for (int i = 0; i < arguments.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            double answer = jtt.bytecode.BC_i2d.test(arguments[i]);
            expectedDouble = answer;

            String functionPrototype = ARMCodeWriter.preAmble("double", "int", Integer.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);

            assert ((Double) registerValues[17]).doubleValue() == expectedDouble : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + expectedDouble;
            theCompiler.cleanup();
        }
    }

    public void IGNORE_C1X_jtt_BC_ifge_2() throws Exception {

        boolean failed = false;
        CompilationBroker.OFFLINE = initialised;

        // double argOne[] = {0.0d, -0.1}; * @Runs: (0d, -0.1d) = false; (78.00d, 78.001d) = true

        int argOne[] = { 0, 1, 6, 7};
        int argTwo[] = { 2, -2, 375, 50};

        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_ifge_2";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_ifge_2.java", "boolean test(int, int)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();

            boolean answer = jtt.bytecode.BC_ifge_2.test(argOne[i], argTwo[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int", Integer.toString(argOne[i]) + new String(", ") + Integer.toString(argTwo[i]));
            // good question here ... is the value returned in the float s0 or the core s0 register
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (((Integer) registerValues[0]).intValue() != expectedValue) {
                failed = true;
            }
            Log.println("C1X IFGE_2 " + ((Integer) registerValues[0]).intValue() + " expected " + expectedValue);
            // Log.println("DCMP less than passed test" + argOne[i] + " " + argTwo[i]);
            theCompiler.cleanup();
        }
        assert (failed == false);

    }


    public void IGNORE_jtt_BC_dcmp01() throws Exception {


        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        // double argOne[] = {0.0d, -0.1}; * @Runs: (0d, -0.1d) = false; (78.00d, 78.001d) = true

        double argOne[] = { 5.0d, -3.1d, 5.0d, -5.0d, 0d, -0.1d, -5.0d, 25.5d, 0.5d};
        double argTwo[] = { 78.00d, 78.01d, 3.3d, -7.2d, 78.00d, 78.001d, -3.2d, 25.5d, 1.0d};

        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_dcmp01";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_dcmp01.java", "boolean test(double, double)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_dcmp01.test(argOne[i], argTwo[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "double , double", Double.toString(argOne[i]) + new String(", ") + Double.toString(argTwo[i]));
            // good question here ... is the value returned in the float s0 or the core s0 register
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                failed = true;
            }
            Log.println("DCMP01 LESS THAN  " + argOne[i] + " " + argTwo[i] + " returned " + registerValues[0] + " expected " + expectedValue + " broken " + failed);
            theCompiler.cleanup();
        }
        assert (failed == false);
    }

    public void IGNORE_C1Xjtt_BC_dcmp02() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_dcmp02";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_dcmp02.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_dcmp02.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("DCMP02 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_dcmp03() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_dcmp03";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_dcmp03.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_dcmp03.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("DCMP03 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_dcmp04() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_dcmp04";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_dcmp04.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_dcmp04.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("DCMP04 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_dcmp05() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_dcmp05";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_dcmp05.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_dcmp05.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("DCMP05 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_dcmp06() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_dcmp06";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_dcmp06.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_dcmp06.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("DCMP06 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_dcmp07() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_dcmp07";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_dcmp07.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_dcmp07.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("DCMP07 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_dcmp08() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_dcmp08";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_dcmp08.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_dcmp08.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("DCMP08 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_dcmp09() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_dcmp09";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_dcmp09.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_dcmp09.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("DCMP09 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_dcmp10() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_dcmp10";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_dcmp10.java", "boolean test(int)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < 9; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_dcmp10.test(i);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(i));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("DCMP10 test " + i + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_fcmp01() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */
        float argOne[] = { 5.0f, -3.0f, 5.0f, -5.0f, 0f, -0.1f};
        float argTwo[] = { 78.00f, 78.01f, 3.3f, -7.2f, 78.00f, 78.001f};
        String klassName = "jtt.bytecode.BC_fcmp01";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_fcmp01.java", "boolean test(float, float)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_fcmp01.test(argOne[i], argTwo[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "float, float", Float.toString(argOne[i]) + new String(",") + Float.toString(argTwo[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("FCMP01 test " + argOne[i] + " " + argTwo[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_fcmp02() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_fcmp02";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_fcmp02.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_fcmp02.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("fCMP02 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_fcmp03() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_fcmp03";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_fcmp03.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_fcmp03.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("FCMP03 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_fcmp04() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_fcmp04";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_fcmp04.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_fcmp04.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("FCMP04 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_fcmp05() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_fcmp05";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_fcmp05.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_fcmp05.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("FCMP05 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_fcmp06() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_fcmp06";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_fcmp06.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_fcmp06.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("FCMP06 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_fcmp07() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_fcmp07";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_fcmp07.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_fcmp07.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("FCMP07 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_fcmp08() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_fcmp08";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_fcmp08.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_fcmp08.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("FCMP08 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_fcmp09() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        Log.println("dcmp seems to fail on comparisons involving NaN -- incorrect return values r0 not initialised");
        String klassName = "jtt.bytecode.BC_fcmp09";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_fcmp09.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_fcmp09.test(argOne[i]);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("FCMP09 test " + argOne[i] + " returned " + registerValues[0] + " expected " + expectedValue);

            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
    public void IGNORE_C1Xjtt_BC_fcmp10() throws Exception {

        initTests();
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;

        /*
         * @Harness: java
         *
         * @Runs: -1.0d = false; 1.0d = false; 0.0d = false; -0.0d = false
         */

        String klassName = "jtt.bytecode.BC_fcmp10";

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        initialiseCodeBuffers(methods, "BC_fcmp10.java", "boolean test(int)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < 9; i++) {
            MaxineByteCode xx = new MaxineByteCode();
            boolean answer = jtt.bytecode.BC_fcmp10.test(i);
            if (answer) {
                expectedValue = 1;
            } else {
                expectedValue = 0;
            }

            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(i));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (registerValues[0] != expectedValue) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            }
            Log.println("FCMP10 test " + i + " returned " + registerValues[0] + " expected " + expectedValue);
            if (registerValues[0] != expectedValue) {
                failed = true;
            }
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_fmul() throws Exception {
        initTests();
        boolean failed = false;

/*
 * @Harness: java
 *
 * @Runs: (311.0D, 10D) = 31.1D
 */
        float argsOne[] = { 311.0f, 2f, -2.5f};
        float argsTwo[] = { 10f, 20.1f, -6.01f};

        String klassName = "jtt.bytecode.BC_fmul";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        float expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fmul.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[33].equals(new Float(floatValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + registerValues[0] + " " + floatValue);
            }
            Log.println("FMUL test " + i + " returned " + ((Float) registerValues[33]).floatValue() + " expected " + floatValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_fadd() throws Exception {
        initTests();
        boolean failed = false;

/*
 * @Harness: java
 *
 * @Runs: (311.0D, 10D) = 31.1D
 */
// * @Runs: (0.0f, 0.0f) = 0.0f; (1.0f, 1.0f) = 2.0f; (253.11f, 54.43f) = 307.54f

        float argsOne[] = { 311.0f, 2f, -2.5f, 0.0f, 1.0f};
        float argsTwo[] = { 10f, 20.1f, -6.01f, 0.0f, 1.0f};

        String klassName = "jtt.bytecode.BC_fadd";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        float expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fadd.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[33].equals(new Float(floatValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + registerValues[0] + " " + floatValue);
            }
            Log.println("FADD test " + i + " returned " + ((Float) registerValues[33]).floatValue() + " expected " + floatValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_fsub() throws Exception {
        initTests();
        boolean failed = false;

/*
 * @Harness: java
 *
 * @Runs: (311.0D, 10D) = 31.1D
 */
        float argsOne[] = { 311.0f, 2f, -2.5f};
        float argsTwo[] = { 10f, 20.1f, -6.01f};

        String klassName = "jtt.bytecode.BC_fsub";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        float expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fsub.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[33].equals(new Float(floatValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + registerValues[0] + " " + floatValue);
            }
            Log.println("FSUB test " + i + " returned " + ((Float) registerValues[33]).floatValue() + " expected " + floatValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_fdiv() throws Exception {
        initTests();
        boolean failed = false;

/*
 * @Harness: java
 *
 * @Runs: (311.0D, 10D) = 31.1D
 */
        float argsOne[] = { 311.0f, 2f};
        float argsTwo[] = { 10f, 20.1f};

        String klassName = "jtt.bytecode.BC_fdiv";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        float expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fdiv.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[33].equals(new Float(floatValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + registerValues[0] + " " + floatValue);
            }
            Log.println("FDIV test " + i + " returned " + ((Float) registerValues[33]).floatValue() + " expected " + floatValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_frem() throws Exception {
        initTests();
        boolean failed = false;

/*
 * @Harness: java
 *
 * @Runs: (311.0D, 10D) = 31.1D
 */
        float argsOne[] = { 311.0f, 2f};
        float argsTwo[] = { 10f, 20.1f};

        String klassName = "jtt.bytecode.BC_frem";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        float expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_frem.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[33].equals(new Float(floatValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + registerValues[0] + " " + floatValue);
            }
            Log.println("FREM test " + i + " returned " + ((Float) registerValues[33]).floatValue() + " expected " + floatValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_irem() throws Exception {
        initTests();
        boolean failed = false;

/*
 * @Harness: java
 *
 * @Runs: (311.0D, 10D) = 31.1D
 */
        int argsOne[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int argsTwo[] = { 2, 2, 2, 3, 3, 3, 3, 3, 3, 3};

        String klassName = "jtt.bytecode.BC_irem";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_irem.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[0].equals(new Integer(expectedValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + ((Integer) registerValues[0]).intValue() + " " + expectedValue);
            }
            Log.println("IREM test " + i + " returned " + ((Integer) registerValues[0]).intValue() + " expected " + expectedValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_drem() throws Exception {
        initTests();
        boolean failed = false;

/*
 * @Harness: java
 *
 * @Runs: (311.0D, 10D) = 31.1D
 */
        double argsOne[] = { 311.0D, 2D};
        double argsTwo[] = { 10D, 20.1D};

        String klassName = "jtt.bytecode.BC_drem";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        double expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_drem.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[17].equals(new Double(doubleValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + registerValues[0] + " " + doubleValue);
            }
            Log.println("DREM test " + i + " returned " + ((Double) registerValues[17]).doubleValue() + " expected " + doubleValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_ddiv() throws Exception {
        initTests();
        boolean failed = false;

/*
 * @Harness: java
 *
 * @Runs: (311.0D, 10D) = 31.1D
 */
        double argsOne[] = { 311.0D, 2D};
        double argsTwo[] = { 10D, 20.1D};

        String klassName = "jtt.bytecode.BC_ddiv";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        double expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_ddiv.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[17].equals(new Double(doubleValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + registerValues[0] + " " + doubleValue);
            }
            Log.println("DDIV test " + i + " returned " + ((Double) registerValues[17]).doubleValue() + " expected " + doubleValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_C1X_jtt_BC_iadd3() throws Exception {
        initTests();
        boolean failed = false;

        /*
         * @Harness: java
         *
         * @Runs: (1s,2s)=3; (0s,-1s)=-1; (33s,67s)=100; (1s, -1s)=0;
         *
         * @Runs: (-128s,1s)=-127; (127s,1s)=128;
         *
         * @Runs: (-32768s,1s)=-32767; (32767s,1s)=32768;
         */

        short argsOne[] = { 1, 0, 33, 1, -128, 127, -32768, 32767};
        short argsTwo[] = { 2, -1, 67, -1, 1, 1, 1, 1};

        String klassName = "jtt.bytecode.BC_iadd";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_iadd3.java", "int test(short, short)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd3.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("int", "short , short ", Short.toString(argsOne[i]) + "," + Short.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[0].equals(new Integer(expectedValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + ((Integer) registerValues[0]).intValue() + " " + expectedValue);
            }
            Log.println("IADD3 test " + i + " returned " + ((Integer) registerValues[0]).intValue() + " expected " + expectedValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_C1X_jtt_BC_i2s() throws Exception {
        initTests();
        boolean failed = false;

/*
 * @Harness: java
 *
 * @Runs: -1 = -1s; 34 = 34s; 65535 = -1s; 32768 = -32768s
 */

        int argsOne[] = { 1, -1, 34, 1, 65535, 32768, -32768};

        String klassName = "jtt.bytecode.BC_i2s";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_i2s.java", "short test(int)");
        int assemblerStatements = codeBytes.length / 4;
        short expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_i2s.test(argsOne[i]);

            String functionPrototype = ARMCodeWriter.preAmble("short", "int  ", Integer.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (((Integer) registerValues[0]).shortValue() != expectedValue) { // r0.r15 + APSR then FPREGS
                failed = true;
            }
            Log.println("C1X I2S test " + i + " returned " + ((Integer) registerValues[0]).shortValue() + " expected " + expectedValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_C1X_jtt_BC_iadd() throws Exception {
        initTests();
        boolean failed = false;

        /*
         * @Harness: java
         *
         * @Runs: (1,2)=3; (0,-1)=-1; (33,67)=100; (1, -1)=0;
         *
         * @Runs: (-2147483648,1)=-2147483647; (2147483647,1)=-2147483648;
         *
         * @Runs: (-2147483647,-2)=2147483647
         */

        int argsOne[] = { 1, 0, 33, 1, -2147483648, -2147483647, 2147483647};
        int argsTwo[] = { 2, -1, 67, -1, 1, -2, 1};

        String klassName = "jtt.bytecode.BC_iadd";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_iadd.java", "int test(int, int)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[0].equals(new Integer(expectedValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + ((Integer) registerValues[0]).intValue() + " " + expectedValue);
            }
            Log.println("IADD test " + i + " returned " + ((Integer) registerValues[0]).intValue() + " expected " + expectedValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_C1X_jtt_BC_iadd2() throws Exception {
        initTests();
        boolean failed = false;

/*
 * @Harness: java
 *
 * @Runs: (1b,2b)=3; (0b,-1b)=-1; (33b,67b)=100; (1b, -1b)=0;
 *
 * @Runs: (-128b,1b)=-127; (127b,1b)=128;
 */

        byte argsOne[] = { 1, 0, 33, 1, -128, 127};
        byte argsTwo[] = { 2, -1, 67, -1, 1, 1};

        String klassName = "jtt.bytecode.BC_iadd";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_iadd2.java", "int test(byte, byte)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd2.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[0].equals(new Integer(expectedValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + ((Integer) registerValues[0]).intValue() + " " + expectedValue);
            }
            Log.println("IADD2 test " + i + " returned " + ((Integer) registerValues[0]).intValue() + " expected " + expectedValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_dadd() throws Exception {
        initTests();
        boolean failed = false;

        /*
         * @Harness: java
         *
         * @Runs: (0.0d, 0.0d) = 0.0d; (1.0d, 1.0d) = 2.0d; (253.11d, 54.43d) = 307.54d public class BC_dadd { public
         * static double test(double a, double b) { return a + b; } }
         */

        double argsOne[] = { 0.0D, 1.0D, 253.11d};
        double argsTwo[] = { 0.0D, 1.0D, 54.43D};

        String klassName = "jtt.bytecode.BC_dadd";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        double expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dadd.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[17].equals(new Double(doubleValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + registerValues[0] + " " + doubleValue);
            }
            Log.println("DADD test " + i + " returned " + ((Double) registerValues[17]).doubleValue() + " expected " + doubleValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_fload() throws Exception {
        initTests();
        boolean failed = false;

        /*
         * @Harness: java
         *
         * @Runs: 0.0f = 0.0f; 1.1f = 1.1f; -1.4f = -1.4f;
         *
         * @Runs: 256.33f = 256.33f; 1000.001f = 1000.001f
         */

        float argsOne[] = { 0.0f, 1.1f, -1.4f, 256.33f, 1000.001f};

        String klassName = "jtt.bytecode.BC_fload";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fload.java", "float test(float)");
        int assemblerStatements = codeBytes.length / 4;
        float expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fload.test(argsOne[i]);

            String functionPrototype = ARMCodeWriter.preAmble("float", " float ", Float.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[33].equals(new Float(floatValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + floatValue);
            }
            Log.println("FLOAD test " + i + " returned " + ((Float) registerValues[33]).floatValue() + " expected " + floatValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_fload2() throws Exception {
        initTests();
        boolean failed = false;

        /*
         * @Harness: java
         *
         * @Runs: 0.0f = 0.0f; 1.1f = 1.1f; -1.4f = -1.4f;
         *
         * @Runs: 256.33f = 256.33f; 1000.001f = 1000.001f
         */

        float argsOne[] = { 0.0f, 1.1f, -1.4f, 256.33f, 1000.001f};
        float argsTwo[] = { 17.1f, 2.5f, 45.32f, -44.5f, -990.9f};

        String klassName = "jtt.bytecode.BC_fload_2";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fload_2.java", "float test(float, float)");
        int assemblerStatements = codeBytes.length / 4;
        float expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fload_2.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("float", " float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[33].equals(new Float(floatValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + floatValue);
            }
            Log.println("FLOAD test " + i + " returned " + ((Float) registerValues[33]).floatValue() + " expected " + floatValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_freturn() throws Exception {
        initTests();
        boolean failed = false;

        /*
         * @Harness: java
         *
         * @Runs: 0.0f = 0.0f; 1.1f = 1.1f; -1.4f = -1.4f;
         *
         * @Runs: 256.33f = 256.33f; 1000.001f = 1000.001f
         */

        float argsOne[] = { 0.0f, 1.1f, -1.4f, 256.33f, 1000.001f};

        String klassName = "jtt.bytecode.BC_freturn";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        float expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_freturn.test(argsOne[i]);

            String functionPrototype = ARMCodeWriter.preAmble("float", " float ", Float.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[33].equals(new Float(floatValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + floatValue);
            }
            Log.println("FRETURN test " + i + " returned " + ((Float) registerValues[33]).floatValue() + " expected " + floatValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_dreturn() throws Exception {
        initTests();
        boolean failed = false;

        /*
         * @Harness: java
         *
         * @Runs: 0.0d = 0.0d; 1.1d = 1.1d; -1.4d = -1.4d; 256.33d = 256.33d; 1000.001d = 1000.001d
         */

        double argsOne[] = { 0.0D, 1.1D, -1.4d, 256.33d, 1000.001d};

        String klassName = "jtt.bytecode.BC_dreturn";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        double expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dreturn.test(argsOne[i]);

            String functionPrototype = ARMCodeWriter.preAmble("double", " double ", Double.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[17].equals(new Double(doubleValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + registerValues[0] + " " + doubleValue);
            }
            Log.println("DRETURN test " + i + " returned " + ((Double) registerValues[17]).doubleValue() + " expected " + doubleValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_dmul() throws Exception {
        initTests();
        boolean failed = false;

        /*
         * @Harness: java
         *
         * @Runs: (311.0D, 10D) = 3110.0D; (11.2D, 2.0D) = 22.4D
         *
         * public class BC_dmul { public static double test(double a, double b) { return a * b; } }
         */

        double argsOne[] = { 311.0D, 11.2D};
        double argsTwo[] = { 10D, 2.0D};

        String klassName = "jtt.bytecode.BC_dmul";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        double expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dmul.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[17].equals(new Double(doubleValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + registerValues[0] + " " + doubleValue);
            }
            Log.println("DMUL test " + i + " returned " + ((Double) registerValues[17]).doubleValue() + " expected " + doubleValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_dsub() throws Exception {
        initTests();
        boolean failed = false;

/*
 * @Harness: java
 *
 * @Runs: (0.0d, 0.0d) = 0.0d; (1.0d, 1.0d) = 0.0d; (253.11d, 54.43d) = 198.68d public class BC_dsub { public static
 * double test(double a, double b) { return a - b; } }
 */

        double argsOne[] = { 0.0D, 1.0D, 253.11d};
        double argsTwo[] = { 0.0D, 1.0D, 54.43d};

        String klassName = "jtt.bytecode.BC_dsub";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dsub.java", "double test(double, double)");
        int assemblerStatements = codeBytes.length / 4;
        double expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dsub.test(argsOne[i], argsTwo[i]);

            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[17].equals(new Double(doubleValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + registerValues[0] + " " + doubleValue);
            }
            Log.println("DSUB test " + i + " returned " + ((Double) registerValues[17]).doubleValue() + " expected " + doubleValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNORE_jtt_BC_dsub2() throws Exception {
        initTests();
        boolean failed = false;

/*
 * @Harness: java
 *
 * @Runs: (0.0d, 0.0d) = 0.0d; (1.0d, 1.0d) = 0.0d; (253.11d, 54.43d) = 198.68d public class BC_dsub { public static
 * double test(double a, double b) { return a - b; } }
 */

        double argsOne[] = { 1.0D, 2.0d, 0.0d};

        String klassName = "jtt.bytecode.BC_dsub";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dsub2.java", "double test(double)");
        int assemblerStatements = codeBytes.length / 4;
        double expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dsub2.test(argsOne[i]);

            String functionPrototype = ARMCodeWriter.preAmble("double", "double ", Double.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[17].equals(new Double(doubleValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + registerValues[0] + " " + doubleValue);
            }
            Log.println("DSUB2 test " + i + " returned " + ((Double) registerValues[17]).doubleValue() + " expected " + doubleValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void test_jtt_BC_fneg() throws Exception {
        initTests();
        boolean failed = false;
        /*
         * @Harness: java
         *
         * @Runs: (0.0d, 1.0d, 0) = -0.0d; (-1.01d, -2.01d, 0) = 1.01d; (7263.8734d, 8263.8734d, 0) = -7263.8734d;
         * (0.0d, 1.0d, 1) = -1.0d; (-1.01d, -2.01d, 1) = 2.01d; (7263.8734d, 8263.8734d, 1) = -8263.8734d
         *
         * public class BC_dneg { public static double test(double a, double b, int which) { double result1 = -a; double
         * result2 = -b; double result = 0.0; if (which == 0) { result = result1; } else { result = result2; } return
         * result; } }
         */
        // @Runs: 0.0f = -0.0f; -1.01f = 1.01f; 7263.8734f = -7263.8734f

        float argsOne[] = { 0.0f, -1.01f, 7263.8734f, 0.0f, 7263.8743f};

        String klassName = "jtt.bytecode.BC_fneg";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fneg.java", "float test(float)");
        assert (entryPoint != -1);
        int assemblerStatements = codeBytes.length / 4;
        float expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fneg.test(argsOne[i]);

            String functionPrototype = ARMCodeWriter.preAmble("float", "float ", Float.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[33].equals(new Float(floatValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + floatValue);
            }
            Log.println("FNEG test " + i + " returned " + ((Float) registerValues[33]).floatValue() + " expected " + floatValue);
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }


    public void test_C1Xjtt_BC_dneg2() throws Exception {
        initTests();
        boolean failed = false;
/*
 * @Harness: java
 * @Runs: -0.0d = `java.lang.Double.POSITIVE_INFINITY; 0.0d = `java.lang.Double.NEGATIVE_INFINITY
 */

        double argsOne[] = {1.0d,-1.0d, -0.0D, 0.0d ,-2.0d, 2.0d};


        String klassName = "jtt.bytecode.BC_dneg2";
        List<TargetMethod> methods = Compile.compile(new String[]{klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods,"BC_dneg2.java","double test(double)");
        assert(entryPoint != -1);
        int assemblerStatements = codeBytes.length / 4;
        double expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dneg2.test(argsOne[i]);

            String functionPrototype = ARMCodeWriter.preAmble("double", " double ", Double.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (!registerValues[17].equals(new Double(doubleValue))) { // r0.r15 + APSR then FPREGS
                failed = true;
                System.out.println("Failed incorrect value " + ((Double)registerValues[17]).doubleValue() + " " + doubleValue);
            }
            Log.println("DNEG2 test " + i + " returned " + ((Double) registerValues[17]).doubleValue() + " expected " + doubleValue);
            //assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }



    public void test_jtt_BC_XXdneg9() throws Exception {
        initTests();
        boolean failed = false;
        /*
         * @Harness: java
         *
         * @Runs: (0.0d, 1.0d, 0) = -0.0d; (-1.01d, -2.01d, 0) = 1.01d; (7263.8734d, 8263.8734d, 0) = -7263.8734d;
         * (0.0d, 1.0d, 1) = -1.0d; (-1.01d, -2.01d, 1) = 2.01d; (7263.8734d, 8263.8734d, 1) = -8263.8734d
         *
         * public class BC_dneg { public static double test(double a, double b, int which) { double result1 = -a; double
         * result2 = -b; double result = 0.0; if (which == 0) { result = result1; } else { result = result2; } return
         * result; } }
         */
        double argsOne[] = { 0.0D, -1.01D, 7263.8734d, 0.0d, -1.01d, 7263.8743d};
        double argsTwo[] = { 1.0d, -2.01D, 8263.8734d, 1.0d, -2.01d, 8263.8734d};
        int argsThree[] = { 0, 0, 0, 1, 1, 1};

        String klassName = "jtt.bytecode.BC_dneg";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dneg.java", "double test(double, double, int)");
        assert (entryPoint != -1);
        int assemblerStatements = codeBytes.length / 4;
        double expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dneg.test(argsOne[i], argsTwo[i], argsThree[i]);

            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double, int", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]) + "," + Integer.toString(argsThree[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if(registerValues[17] == null ) {
                failed = true;
                Log.println("QEMU crashed ... null registverValues");
            } else {
                if (!registerValues[17].equals(new Double(doubleValue))) { // r0.r15 + APSR then FPREGS
                    failed = true;
                    System.out.println("Failed incorrect value " + registerValues[0] + " " + doubleValue);
                }
                Log.println("DNEG test " + i + " returned " + ((Double) registerValues[17]).doubleValue() + " expected " + doubleValue);

            }
            // assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " +
// expectedValue;
            theCompiler.cleanup();
        }
        assert failed == false;
    }

    public void IGNOREtest_jtt_BC_lload_0() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = "jtt.bytecode.BC_lload_0";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 1L));
        pairs.add(new Args(-3L, -3L));
        pairs.add(new Args(10000L, 10000L));
        pairs.add(new Args(549755814017L, 549755814017L));
        initialiseCodeBuffers(methods, "BC_lload_0.java","long test(long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            long expectedValue = jtt.bytecode.BC_lload_0.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }
}