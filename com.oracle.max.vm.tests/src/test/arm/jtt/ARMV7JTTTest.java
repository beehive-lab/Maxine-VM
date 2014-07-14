package test.arm.jtt;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.vm.classfile.constant.IntegerConstant;
import com.sun.max.vm.code.CodeCacheValidation;
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
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.MaxineVM;

public class ARMV7JTTTest extends MaxTestCase {

    private ARMV7Assembler asm;
    private CiTarget armv7;
    private ARMCodeWriter code;
    private T1X t1x;
    private C1X c1x;
    private ARMV7T1XCompilation theCompiler;
    private StaticMethodActor anMethod = null;
    private CodeAttribute codeAttr = null;
    private static boolean POST_CLEAN_FILES = true;
    private static boolean OFFLINE_C1X_COMPILE = true;
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
    private static boolean[] IGNOREvalues = new boolean[17];

    private int[] generateAndTestStubs(String functionPrototype, int entryPoint, byte[] theCode, int assemblerStatements, int[] expected, boolean[] IGNOREs, MaxineARMTester.BitsFlag[] masks)
                    throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(assemblerStatements, theCode);
        // code.createCodeStubsFile(theCode,entryPoint);
        code.createStaticCodeStubsFile(functionPrototype, theCode, entryPoint);
        MaxineARMTester r = new MaxineARMTester(expected, IGNOREs, masks);
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

    private int[] generateAndTest(int assemblerStatements, int[] expected, boolean[] IGNOREs, MaxineARMTester.BitsFlag[] masks) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(assemblerStatements, theCompiler.getMacroAssembler().codeBuffer);
        code.createCodeFile();
        MaxineARMTester r = new MaxineARMTester(expected, IGNOREs, masks);
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
        if (!OFFLINE_C1X_COMPILE) {
            initTests();
        }
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
            RuntimeCompiler.baselineCompilerOption.setValue(baselineCompilerName);
            RuntimeCompiler.optimizingCompilerOption.setValue(optimizingCompilerName);
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

    private static void enableC1XOfflineCompiler() {
        OFFLINE_C1X_COMPILE = true;
    }

    private static void disableC1XOfflineCompiler() {
        OFFLINE_C1X_COMPILE = false;
    }

    public void IGNORE_jtt_UsageOfStaticMethods() throws Exception {
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int value = 99;
        int answer = jtt.bytecode.ARM_BC_test_return1.test(12);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.ARM_BC_test_return1");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ARMV7.r0, value);
        masm.mov32BitConstant(ARMV7.r1, 12);
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r2)
        masm.push(ConditionFlag.Always, 1); // local slot 0 is return (int is one slot) last push to stack is 0
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_imul() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_isub() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ineg() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ineg_1() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_iadd() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ior() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ixor() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_iand() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ishl() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ishr() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ishr_1() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_iushr() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2b() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2b_1() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2b_2() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2s() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2s_1() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2s_2() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2c() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_i2c_1() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ireturn() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_ireturn_1() throws Exception {
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
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void IGNORE_jtt_BC_tableswitch() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_tableswitch_2() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_tableswitch_3() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_tableswitch_4() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_lookupswitch_1() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_lookupswitch_2() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_lookupswitch_3() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_lookupswitch_4() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iinc_1() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iinc_2() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iinc_3() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iinc_4() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_0() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_0_1() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_0_2() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_1() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_1_1() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_2() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iload_3() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_iconst() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void IGNORE_jtt_BC_ifeq() throws Exception {
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
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ifeq_2() throws Exception {
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
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ifeq_3() throws Exception {
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
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    private void initialiseCodeBuffers(List<TargetMethod> methods) {
        int minimumValue = Integer.MAX_VALUE;
        int maximumValue = Integer.MIN_VALUE;
        int offset;
        entryPoint = -1; // offset in the global array of the method we call from C.
        for (TargetMethod m : methods) {
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

    public void IGNOREtest_jtt_BC_invokestatic() throws Exception {
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_invokestatic";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialised = true;
        initTests();
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
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
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_invokestatic.test(pair.first);
            expectedValues[0] = answer;
            /*
             * SEE BELOW, WE NEED TO PROVIDE A COMMA SEPARATED LIST OF TYPES "int" void (*pf)(***int***) = (void
             * (*))(code); print_uart0("changed test.c!\n"); AND WE NEED TO PROVIDE A COMMA SEPARATED LIST OF FUNCTION
             * ARGUMENT VALUES / VARIABLES (*pf)(****1*****); // Need to change this to something related to the test
             * itself asm volatile("forever: b forever");
             */
            String functionPrototype = ARMCodeWriter.preAmble("void", "int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_d2f() throws Exception {
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        String klassName = "jtt.bytecode.BC_d2f";
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialised = true;
        initTests();
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
            String functionPrototype = ARMCodeWriter.preAmble("float", "double", Double.toString(arguments[i]));
            System.out.println(functionPrototype);
            // good question here ... is the value returned in the float s0 or the core s0 register
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, IGNOREvalues, bitmasks);
            if (Float.intBitsToFloat(registerValues[0]) != expectedFloat) {
                System.out.println("Failed incorrect value " + registerValues[0] + " " + expectedValues[0]);
            }
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
        MaxineVM.exit(0);
    }
}
