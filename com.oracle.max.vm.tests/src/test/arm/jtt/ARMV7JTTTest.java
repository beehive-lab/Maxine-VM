package test.arm.jtt;

import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.util.*;

import jtt.bytecode.*;

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
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
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

    public void initialiseFrameForCompilation(ConstantPool cp, byte[] code, String sig, int flags) {
        // TODO: compute max stack
        codeAttr = new CodeAttribute(cp, code, (char) 40, (char) 20, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create(sig), flags, codeAttr, new String());
    }

    static final class Args {

        public int first;
        public int second;
        public int third;
        public int fourth;
        public long lfirst;
        public long lsecond;
        public long ffirst;
        public long fsecond;


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

        public Args(long lfirst, float fsecond) {
            this.lfirst = lfirst;
            this.fsecond = (long)fsecond;
        }


        public Args(long lfirst, int second) {
            this.lfirst = lfirst;
            this.second = second;
        }

        public Args(int first, long lfirst) {
            this.first = first;
            this.lfirst = lfirst;
        }

        public Args(int first, int second, long lfirst) {
            this.first = first;
            this.second = second;
            this.lfirst = lfirst;
        }

        public Args(int first, int second, int third, long lfirst) {
            this.first = first;
            this.second = second;
            this.third = third;
            this.lfirst = lfirst;
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

    private String getKlassName(String klass) {
        return "^" + klass + "^";
    }

    private Object[] generateObjectsAndTestStubs(String functionPrototype, int entryPoint, byte[] theCode, int assemblerStatements, int[] expected, boolean[] tests, MaxineARMTester.BitsFlag[] masks)
                    throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(assemblerStatements, theCode);
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

    private void initialiseCodeBuffers(List<TargetMethod> methods) {
        int minimumValue = Integer.MAX_VALUE;
        int maximumValue = Integer.MIN_VALUE;
        int offset;
        entryPoint = -1; // Offset in the global array of the method we call from C.
        for (TargetMethod m : methods) {
            byte[] b = m.code();
            if (entryPoint == -1) {
                entryPoint = m.codeAt(0).toInt();
            }
            if ((m.codeAt(0)).toInt() < minimumValue) {
                minimumValue = m.codeAt(0).toInt(); // Update minimum offset in address space
            }
            if ((m.codeAt(0)).toInt() + b.length > maximumValue) {
                maximumValue = m.codeAt(0).toInt() + b.length; // Update maximum offset in address space
            }
        }

        if (MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt() < minimumValue) {
            minimumValue = MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt();
        }

        if ((MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt() + MaxineVM.vm().stubs.staticTrampoline().code().length) > maximumValue) {
            maximumValue = MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt() + MaxineVM.vm().stubs.staticTrampoline().code().length;
        }

        codeBytes = new byte[maximumValue - minimumValue];
        for (TargetMethod m : methods) {
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
            byte[] b = m.code();
            if (entryPoint == -1) {
                if (methodName.equals(m.classMethodActor().simpleName())) {
                    entryPoint = m.codeAt(0).toInt();
                }
            }
            if ((m.codeAt(0)).toInt() < minimumValue) {
                minimumValue = m.codeAt(0).toInt();
            }
            if ((m.codeAt(0)).toInt() + b.length > maximumValue) {
                maximumValue = m.codeAt(0).toInt() + b.length;
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
        for (TargetMethod m : methods) {
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

    public void ignore_jtt_UsageOfStaticMethods() throws Exception {
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
        masm.mov32BitConstant(ARMV7.r0, 12);
        masm.mov32BitConstant(ARMV7.r1, 12);
        masm.push(ConditionFlag.Always, 2); // local slot 1 is argument (r2)
        masm.push(ConditionFlag.Always, 1); // local slot 0 is return (int is one slot) last push to stack is 0
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_C1X_jtt_BC_imul() throws Exception {
        initTests();
        int argsOne[] = { 1, 0, 33, 1, -2147483648, 2147483647, -2147483648};
        int argsTwo[] = { 12, -1, 67, -1, 1, -1, -1};
        String klassName = getKlassName("jtt.bytecode.BC_imul");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_imul.java", "int test(int, int)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_imul.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Integer) registerValues[0] == (new Integer(expectedValue).intValue()) : "Failed incorrect value " + registerValues[0] + " " + (new Integer(expectedValue).intValue());
            theCompiler.cleanup();
        }
    }

    public void ignore_T1X_jtt_BC_iadd2() throws Exception {
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
            initialiseFrameForCompilation(code, "(BB)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, argsOne[i]);
            masm.mov32BitConstant(ARMV7.r1, argsTwo[i]);
            masm.mov32BitConstant(ARMV7.r2, -99);
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            masm.push(ConditionFlag.Always, 4);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == answer : ("Failed incorrect value " + registerValues[0] + " " + answer);
            theCompiler.cleanup();
        }
    }

    public void ignore_T1X_jtt_BC_iadd3() throws Exception {
        initTests();
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
            masm.mov32BitConstant(ARMV7.r0, argsOne[i]);
            masm.mov32BitConstant(ARMV7.r1, argsTwo[i]);
            masm.mov32BitConstant(ARMV7.r2, -99);
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            masm.push(ConditionFlag.Always, 4);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_imul() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        masm.push(ConditionFlag.Always, 4);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "imul");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_isub() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        masm.push(ConditionFlag.Always, 4);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "isub");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_ineg() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ineg");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_ineg_1() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ineg");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_iadd() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        masm.push(ConditionFlag.Always, 4);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_ior() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        masm.push(ConditionFlag.Always, 4);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ior");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_ixor() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        masm.push(ConditionFlag.Always, 4);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ixor");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_T1X_jtt_BC_iand() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        masm.push(ConditionFlag.Always, 4);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iand");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_C1X_jtt_BC_iand() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_iand");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(0, -1));
        pairs.add(new Args(3, 63));
        pairs.add(new Args(6, 4));
        pairs.add(new Args(-2147483648, 1));
        initialiseCodeBuffers(methods, "BC_iand.java", "int test(int, int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_iand.test(pair.first, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int, int", Integer.toString(pair.first) + "," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ishl() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        masm.push(ConditionFlag.Always, 4);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishl");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }
    public void ignore_jttC1X_BC_ishl() throws Exception {

        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_ishl");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        /*
        * @Harness: java
                * @Runs: (1,2)=4; (0,-1)=0; (31,1)=62; (6, 4)=96; (-2147483648,1)=0
                */
        pairs.add(new Args(1, 2));
        pairs.add(new Args(0, -1));
        pairs.add(new Args(31, 1));
        pairs.add(new Args(6, 4));
        pairs.add(new Args(-2147483648, 1));
        pairs.add(new Args(-9999, 12));



        initialiseCodeBuffers(methods, "BC_ishl.java", "int test(int, int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            int expectedValue = jtt.bytecode.BC_ishl.test(pair.first, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int, int", Integer.toString(pair.first) + "," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            int returnValue = registerValues[0];
            System.out.println("ISHL EXPECTED " + expectedValue + " GOT "+ returnValue);

            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] +  " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }
    public void ignore_jttC1X_BC_ishr() throws Exception {

        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_ishr");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        /*
        * @Harness: java
                 @Runs: (1,2)=0; (67,2)=16; (31,1)=15; (6, 4)=0; (-2147483648,16)=-32768
                */
        pairs.add(new Args(1, 2));
        pairs.add(new Args(0, -1));
        pairs.add(new Args(31, 1));
        pairs.add(new Args(6, 4));
        pairs.add(new Args(-2147483648, 16));
        pairs.add(new Args(67, 2));



        initialiseCodeBuffers(methods, "BC_ishr.java", "int test(int, int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            int expectedValue = jtt.bytecode.BC_ishr.test(pair.first, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int, int", Integer.toString(pair.first) + "," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            System.out.println("ISHR EXPECTED " + expectedValue + " GOT "+ returnValue);

            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] +  " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }
    public void ignore_jttC1X_BC_iushr() throws Exception {

        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_iushr");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        /*
        * @Harness: java
                * @Runs: (1,2)=4; (0,-1)=0; (31,1)=62; (6, 4)=96; (-2147483648,1)=0
                */
        pairs.add(new Args(1, 2));
        pairs.add(new Args(0, -1));
        pairs.add(new Args(31, 1));
        pairs.add(new Args(6, 4));
        pairs.add(new Args(-2147483648, 1));
        pairs.add(new Args(-9999, 12));
        pairs.add(new Args(-2048, 6));
        pairs.add(new Args(99944444, 2));



        initialiseCodeBuffers(methods, "BC_iushr.java", "int test(int, int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            int expectedValue = jtt.bytecode.BC_iushr.test(pair.first, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int, int", Integer.toString(pair.first) + "," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            int returnValue = registerValues[0];
            System.out.println("IUSHR EXPECTED " + expectedValue + " GOT "+ returnValue);

            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] +  " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ishr() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        masm.push(ConditionFlag.Always, 4);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishr");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_ishr_1() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        masm.push(ConditionFlag.Always, 4);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishr");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_iushr() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        masm.push(ConditionFlag.Always, 4);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iushr");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_T1X_jtt_BC_i2b() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_C1X_jtt_BC_i2b() throws Exception {
        initTests();
        int argsOne[] = { -1, 2, 255, 128};
        String klassName = getKlassName("jtt.bytecode.BC_i2b");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_i2b.java", "byte test(int)");
        int assemblerStatements = codeBytes.length / 4;
        short expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_i2b.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("char", "int", Integer.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).byteValue() == expectedValue : "Failed incorrect value " + ((Integer) registerValues[0]).byteValue() + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_i2b_1() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_i2b_2() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_i2s() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_i2s_1() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_i2s_2() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_i2c() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2c");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_i2c_1() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2c");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_ireturn() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_T1X_jtt_BC_ireturn() throws Exception {
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
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int assemblerStatements = masm.codeBuffer.position() / 4;
        int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_C1X_jtt_BC_ireturn() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        int argOne[] = { 0, 1, -1, 256};
        String klassName = getKlassName("jtt.bytecode.BC_ireturn");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_ireturn.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = jtt.bytecode.BC_ireturn.test(argOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : ("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_tableswitch() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        int argOne[] = { 7, -1, 0, 1, 2, 3, 4, 5, 6, 0};
        String klassName = getKlassName("jtt.bytecode.BC_tableswitch");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_tableswitch.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = jtt.bytecode.BC_tableswitch.test(argOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : ("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_tableswitch() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_tableswitch_2() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    // TODO: Fix this, there is no assertion
    public void ignore_jtt_BC_XXXXfdiv() throws Exception {
        boolean failed = false;
        MaxineARMTester.DEBUGOBJECTS = false;
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        float argOne[] = { 14.0f};
        float argTwo[] = { 7.0f};
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_fdiv");
        for (int i = 0; i < argOne.length; i++) {
            initialiseFrameForCompilation(code, "(FF)F", Modifier.PUBLIC | Modifier.STATIC);
            float answer = jtt.bytecode.BC_fdiv.test(argOne[i], argTwo[i]);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.s0, Float.floatToRawIntBits(argOne[i]));
            masm.mov32BitConstant(ARMV7.s1, -Float.floatToRawIntBits(argTwo[i]));
            masm.vpush(ConditionFlag.Always, ARMV7.s0, ARMV7.s1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "fdiv");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.vpop(ConditionFlag.Always, ARMV7.s0, ARMV7.s0);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float", Float.toString(argOne[i]) + Float.toString(argTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, 0, masm.codeBuffer.close(false), assemblerStatements, expectedValues, testvalues, bitmasks);
            if (((Float) registerValues[33]).floatValue() != answer) {
                failed = true;
            }
            System.out.println(" FDIV T1X " + ((Float) registerValues[33]).floatValue());
        }
        MaxineARMTester.DEBUGOBJECTS = false;
        assert failed == false;
        // assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " +
// expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore_jtt_BC_tableswitch_3() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_tableswitch_4() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_lookupswitch_1() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_lookupswitch_2() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_lookupswitch_3() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_lookupswitch_4() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_iinc_1() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_1");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_iinc_2() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_iinc_3() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_iinc_4() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_4");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_iload_0() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_iload_0_1() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0_1");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_iload_0_2() throws Exception {
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

    public void ignore_jtt_BC_iload_1() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(1, 2));
        pairs.add(new Args(1, 1000345));
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            masm.push(ConditionFlag.Always, 4);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_iload_1_1() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_1_1");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_iload_2() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            masm.push(ConditionFlag.Always, 4);
            masm.push(ConditionFlag.Always, 8);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_iload_3() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            masm.push(ConditionFlag.Always, 4);
            masm.push(ConditionFlag.Always, 8);
            masm.push(ConditionFlag.Always, 16);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_T1X_jtt_BC_iconst() throws Exception {
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
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "putfieldLong");
        t1x.createOfflineIntrinsicTemplate(c1x, T1XIntrinsicTemplateSource.class, t1x.intrinsicTemplates, "com_sun_max_unsafe_Pointer$setLong$IIJ");

        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iconst.test(pair.first);
            expectedValues[0] = answer;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iconst");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, -99);
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iconst");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_iconst() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_iconst");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(3, 3));
        pairs.add(new Args(4, 4));
        pairs.add(new Args(5, 5));
        pairs.add(new Args(6, 375));
        initialiseCodeBuffers(methods, "BC_iconst.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            int expectedValue = jtt.bytecode.BC_iconst.test(pair.first);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ifeq() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ifeq_2() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ifeq_3() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ifge() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ifgt() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifgt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ifle() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifle");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ifne() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifne");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_iflt() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ificmplt1() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ificmplt2() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ificmpne1() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ificmpne2() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ifge_3() throws Exception {
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifgt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    // TODO: Fix extra arg stack issue??
    public void ignore_jtt_BC_ifge_2() throws Exception {
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
            expectedValues[0] = answer ? 1 : 0;
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ifge_2");
            initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, pair.first);
            masm.mov32BitConstant(ARMV7.r1, pair.second);
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            masm.push(ConditionFlag.Always, 2); // Extra arg to fix stack issue?
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : (" Failed incorrect values " + registerValues[0] + " " + expectedValues[0]);
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_invokestatic() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        List<Args> pairs = new LinkedList<Args>();
        String klassName = getKlassName("jtt.bytecode.BC_invokestatic");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(-2, -2));
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_invokestatic.test(pair.first);
            expectedValues[0] = answer;
            String functionPrototype = ARMCodeWriter.preAmble("void", "int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_f2d() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_f2d");
        MaxineARMTester.DEBUGOBJECTS = false;
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        float[] arguments = { -2.2f, 0.0f, 1.0f, 01.06f};
        double expectedDouble = -9;
        for (int i = 0; i < arguments.length; i++) {
            double answer = jtt.bytecode.BC_f2d.test(arguments[i]);
            expectedDouble = answer;
            String functionPrototype = ARMCodeWriter.preAmble("double", "float", Float.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert ((Double) registerValues[17]).doubleValue() == expectedDouble : ("Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + expectedDouble);
            MaxineARMTester.DEBUGOBJECTS = false;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_i2f() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_i2f");
        MaxineARMTester.DEBUGOBJECTS = false;
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods, "BC_i2f.java", "float test(int)");
        int assemblerStatements = codeBytes.length / 4;
        int[] arguments = { -2, 0, 1, -1, -99};
        float expectedFloat = -9;
        for (int i = 0; i < arguments.length; i++) {
            expectedFloat = jtt.bytecode.BC_i2f.test(arguments[i]);
            MaxineARMTester.DEBUGOBJECTS = false;
            String functionPrototype = ARMCodeWriter.preAmble("float", "int", Integer.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert ((Float) registerValues[33]).floatValue() == expectedFloat : ("Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + expectedFloat);
            MaxineARMTester.DEBUGOBJECTS = false;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_d2f() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_d2f");
        MaxineARMTester.DEBUGOBJECTS = false;
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        double[] arguments = { -2.2d, 0.0d, 1.0d, 01.06d};
        float expectedFloat = -9;
        for (int i = 0; i < arguments.length; i++) {
            expectedFloat = jtt.bytecode.BC_d2f.test(arguments[i]);
            MaxineARMTester.DEBUGOBJECTS = false;
            String functionPrototype = ARMCodeWriter.preAmble("float", "double", Double.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert ((Float) registerValues[33]).floatValue() == expectedFloat : ("Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + expectedFloat);
            MaxineARMTester.DEBUGOBJECTS = false;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_d2i01() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_d2i01");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods, "BC_d2i01.java", "int test(double)");
        int assemblerStatements = codeBytes.length / 4;
        double[] arguments = { 0.0d, 1.0d, 01.06d, -2.2d};
        int expectedInt = -9;
        for (int i = 0; i < arguments.length; i++) {
            expectedInt = jtt.bytecode.BC_d2i01.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).intValue() == expectedInt : "Failed incorrect value " + ((Integer) registerValues[0]).intValue() + " " + expectedInt;
            theCompiler.cleanup();
        }
    }

    public void ignore_T1X_jtt_BC_d2i02() throws Exception {
        initTests();
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
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            masm.push(ConditionFlag.Always, 2);

            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifge_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            String functionPrototype = ARMCodeWriter.preAmble("double ", "int", Integer.toString(i));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, 0, masm.codeBuffer.copyData(0, masm.codeBuffer.position()), assemblerStatements, expectedValues, testvalues,
                            bitmasks);
            assert ((Double) registerValues[17]).doubleValue() == answer : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_d2i02() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_d2i02");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods, "BC_d2i02.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < 5; i++) {
            int answer = jtt.bytecode.BC_d2i02.test(i);
            String functionPrototype = ARMCodeWriter.preAmble("double ", "int", Integer.toString(i));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).intValue() == answer : ("Failed incorrect value " + ((Integer) registerValues[0]).intValue() + "  " + answer);
            theCompiler.cleanup();
        }
    }

    public void ignore_BC_anewarray() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_anewarray");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods, "BC_anewarray.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        int[] arguments = { 0, 1};
        for (int i = 0; i < arguments.length; i++) {
            int answer = jtt.bytecode.BC_anewarray.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(arguments[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void ignore_BC_new() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_new");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods, "BC_new.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        int[] arguments = { 0, 1};
        for (int i = 0; i < arguments.length; i++) {
            int answer = jtt.bytecode.BC_new.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(arguments[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_f2i01() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_f2i01");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        float[] arguments = { -2.2f, 0.0f, 1.0f, 1.06f};
        for (int i = 0; i < arguments.length; i++) {
            int answer = jtt.bytecode.BC_f2i01.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(arguments[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_f2i02() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_f2i02");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < 5; i++) {
            float answer = jtt.bytecode.BC_f2i02.test(i);
            String functionPrototype = ARMCodeWriter.preAmble("float", "int", Integer.toString(i));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert ((Float) registerValues[33]).floatValue() == answer : "Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_i2d() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_i2d");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        int[] arguments = { -2, 0, 1, 2, 99};
        for (int i = 0; i < arguments.length; i++) {
            double answer = jtt.bytecode.BC_i2d.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "int", Integer.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert ((Double) registerValues[17]).doubleValue() == answer : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_ifge_2() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        int argOne[] = { 0, 1, 6, 7};
        int argTwo[] = { 2, -2, 375, 50};
        String klassName = getKlassName("jtt.bytecode.BC_ifge_2");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_ifge_2.java", "boolean test(int, int)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_ifge_2.test(argOne[i], argTwo[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int", Integer.toString(argOne[i]) + new String(", ") + Integer.toString(argTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).intValue() == expectedValue : "Failed incorrect value " + ((Integer) registerValues[0]).intValue() + "   " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_dcmp01() throws Exception {

        CompilationBroker.OFFLINE = initialised;
        double argOne[] = { 5.0d, -3.1d, 5.0d, -5.0d, 0d, -0.1d, -5.0d, 25.5d, 0.5d};
        double argTwo[] = { 78.00d, 78.01d, 3.3d, -7.2d, 78.00d, 78.001d, -3.2d, 25.5d, 1.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp01");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dcmp01.java", "boolean test(double, double)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp01.test(argOne[i], argTwo[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double , double", Double.toString(argOne[i]) + new String(", ") + Double.toString(argTwo[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + "   " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_dcmp02() throws Exception {
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp02");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dcmp02.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp02.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_dcmp03() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp03");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dcmp03.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp03.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_dcmp04() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp04");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dcmp04.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp04.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_dcmp05() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp05");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dcmp05.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp05.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_dcmp06() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp06");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dcmp06.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp06.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_dcmp07() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp07");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dcmp07.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp07.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_dcmp08() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp08");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dcmp08.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp08.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_dcmp09() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp09");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dcmp09.java", "boolean test(double)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp09.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_dcmp10() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_dcmp10");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dcmp10.java", "boolean test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < 9; i++) {
            boolean answer = jtt.bytecode.BC_dcmp10.test(i);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(i));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_fcmp01() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        float argOne[] = { 5.0f, -3.0f, 5.0f, -5.0f, 0f, -0.1f};
        float argTwo[] = { 78.00f, 78.01f, 3.3f, -7.2f, 78.00f, 78.001f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp01");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fcmp01.java", "boolean test(float, float)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp01.test(argOne[i], argTwo[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float, float", Float.toString(argOne[i]) + new String(",") + Float.toString(argTwo[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_fcmp02() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp02");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fcmp02.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp02.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_fcmp03() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp03");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fcmp03.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp03.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_fcmp04() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp04");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fcmp04.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp04.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_fcmp05() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp05");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fcmp05.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp05.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_fcmp06() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp06");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fcmp06.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp06.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_fcmp07() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp07");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fcmp07.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp07.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_fcmp08() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp08");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fcmp08.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp08.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_fcmp09() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp09");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fcmp09.java", "boolean test(float)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp09.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_fcmp10() throws Exception {
        initTests();
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_fcmp10");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fcmp10.java", "boolean test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < 9; i++) {
            boolean answer = jtt.bytecode.BC_fcmp10.test(i);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(i));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_fmul() throws Exception {
        initTests();
        float argsOne[] = { 311.0f, 2f, -2.5f};
        float argsTwo[] = { 10f, 20.1f, -6.01f};
        String klassName = getKlassName("jtt.bytecode.BC_fmul");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fmul.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[33] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_fadd() throws Exception {
        initTests();
        float argsOne[] = { 311.0f, 2f, -2.5f, 0.0f, 1.0f};
        float argsTwo[] = { 10f, 20.1f, -6.01f, 0.0f, 1.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fadd");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fadd.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_fsub() throws Exception {
        initTests();
        float argsOne[] = { 311.0f, 2f, -2.5f};
        float argsTwo[] = { 10f, 20.1f, -6.01f};
        String klassName = getKlassName("jtt.bytecode.BC_fsub");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fsub.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_fdiv() throws Exception {
        initTests();
        float argsOne[] = { 311.0f, 2f};
        float argsTwo[] = { 10f, 20.1f};
        String klassName = getKlassName("jtt.bytecode.BC_fdiv");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fdiv.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_getstatic_i() throws Exception {
        boolean failed = false;
        initTests();
        int argsOne[] = { 0, -1, 2, 4};
        String klassName = getKlassName("jtt.bytecode.BC_getstatic_i");
        CompilationBroker.OFFLINE = true;

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_getstatic_i.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            int intValue = BC_getstatic_i.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int  ", Integer.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if (((Integer) registerValues[0]).intValue() != intValue) {
                failed = true;
            }
            Log.println("C1X getstatic_i expected " + intValue + " got " + ((Integer) registerValues[0]).intValue());
            theCompiler.cleanup();
        }
        assert (failed == false);
    }

    public void ignore_T1X_jtt_BC_getstatic_i() throws Exception {
        int argsOne[] = { 0, -1, 2, 4};
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_getstatic_i");

        ClassActor anActor = HostedVMClassLoader.HOSTED_VM_CLASS_LOADER.makeClassActor("jtt.bytecode.BC_getstatic_i");
        for (int i = 0; i < argsOne.length; i++) {
            int answer = jtt.bytecode.BC_getstatic_i.test(argsOne[i]);
            expectedValues[0] = answer;
            initialiseFrameForCompilation(anActor.constantPool(), code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ARMV7.r0, argsOne[i]);
            masm.mov32BitConstant(ARMV7.r2, -99);
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int assemblerStatements = masm.codeBuffer.position() / 4;
            int[] registerValues = generateAndTest(assemblerStatements, expectedValues, testvalues, bitmasks);
            Log.println("T1X getstatic_i expected " + answer + " got " + registerValues[0]);

            assert registerValues[0] == answer : ("Failed incorrect value " + registerValues[0] + " " + answer);
            theCompiler.cleanup();
        }

    }

    public void ignore_C1X_jtt_BC_frem() throws Exception {
        initTests();
        float argsOne[] = { 311.0f, 2f};
        float argsTwo[] = { 10f, 20.1f};
        String klassName = getKlassName("jtt.bytecode.BC_frem");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_frem.java", "float test(float, float)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_frem.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_irem() throws Exception {
        initTests();
        int argsOne[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int argsTwo[] = { 2, 2, 2, 3, 3, 3, 3, 3, 3, 3};
        String klassName = getKlassName("jtt.bytecode.BC_irem");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_irem.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Integer) registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_drem() throws Exception {
        initTests();
        double argsOne[] = { 311.0D, 2D};
        double argsTwo[] = { 10D, 20.1D};
        String klassName = getKlassName("jtt.bytecode.BC_drem");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_drem.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ddiv() throws Exception {
        initTests();
        double argsOne[] = { 311.0D, 2D};
        double argsTwo[] = { 10D, 20.1D};
        String klassName = getKlassName("jtt.bytecode.BC_ddiv");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_ddiv.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_iadd3() throws Exception {
        initTests();
        short argsOne[] = { 1, 0, 33, 1, -128, 127, -32768, 32767};
        short argsTwo[] = { 2, -1, 67, -1, 1, 1, 1, 1};
        String klassName = getKlassName("jtt.bytecode.BC_iadd");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_iadd3.java", "int test(short, short)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd3.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "short , short ", Short.toString(argsOne[i]) + "," + Short.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Integer) registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_i2s() throws Exception {
        initTests();
        int argsOne[] = { 1, -1, 34, 1, 65535, 32768, -32768};
        String klassName = getKlassName("jtt.bytecode.BC_i2s");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_i2s.java", "short test(int)");
        int assemblerStatements = codeBytes.length / 4;
        short expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_i2s.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("short", "int  ", Integer.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).shortValue() == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_iadd() throws Exception {
        initTests();
        int argsOne[] = { 1, 0, 33, 1, -2147483648, -2147483647, 2147483647};
        int argsTwo[] = { 2, -1, 67, -1, 1, -2, 1};
        String klassName = getKlassName("jtt.bytecode.BC_iadd");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_iadd.java", "int test(int, int)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Integer) registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_iadd2() throws Exception {
        initTests();
        byte argsOne[] = { 1, 0, 33, 1, -128, 127};
        byte argsTwo[] = { 2, -1, 67, -1, 1, 1};
        String klassName = getKlassName("jtt.bytecode.BC_iadd");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_iadd2.java", "int test(byte, byte)");
        int assemblerStatements = codeBytes.length / 4;
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd2.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            Log.println("IADD2 test " + i + " returned " + ((Integer) registerValues[0]).intValue() + " expected " + expectedValue);
            assert (Integer) registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_dadd() throws Exception {
        initTests();
        double argsOne[] = { 0.0D, 1.0D, 253.11d};
        double argsTwo[] = { 0.0D, 1.0D, 54.43D};
        String klassName = getKlassName("jtt.bytecode.BC_dadd");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dadd.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_fload() throws Exception {
        initTests();
        float argsOne[] = { 0.0f, 1.1f, -1.4f, 256.33f, 1000.001f};
        String klassName = getKlassName("jtt.bytecode.BC_fload");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fload.java", "float test(float)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fload.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", " float ", Float.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_fload2() throws Exception {
        initTests();
        float argsOne[] = { 0.0f, 1.1f, -1.4f, 256.33f, 1000.001f};
        float argsTwo[] = { 17.1f, 2.5f, 45.32f, -44.5f, -990.9f};
        String klassName = getKlassName("jtt.bytecode.BC_fload_2");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fload_2.java", "float test(float, float)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fload_2.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", " float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_freturn() throws Exception {
        initTests();
        float argsOne[] = { 0.0f, 1.1f, -1.4f, 256.33f, 1000.001f};
        String klassName = getKlassName("jtt.bytecode.BC_freturn");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_freturn.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", " float ", Float.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_dreturn() throws Exception {
        initTests();
        double argsOne[] = { 0.0D, 1.1D, -1.4d, 256.33d, 1000.001d};
        String klassName = getKlassName("jtt.bytecode.BC_dreturn");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dreturn.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", " double ", Double.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_dmul() throws Exception {
        initTests();
        double argsOne[] = { 311.0D, 11.2D};
        double argsTwo[] = { 10D, 2.0D};
        String klassName = getKlassName("jtt.bytecode.BC_dmul");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dmul.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_dsub() throws Exception {
        initTests();
        double argsOne[] = { 0.0D, 1.0D, 253.11d};
        double argsTwo[] = { 0.0D, 1.0D, 54.43d};
        String klassName = getKlassName("jtt.bytecode.BC_dsub");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dsub.java", "double test(double, double)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dsub.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_dsub2() throws Exception {
        initTests();
        double argsOne[] = { 1.0D, 2.0d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dsub");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dsub2.java", "double test(double)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dsub2.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double ", Double.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_fneg() throws Exception {
        initTests();
        float argsOne[] = { 0.0f, -1.01f, 7263.8734f, 0.0f, 7263.8743f};
        String klassName = getKlassName("jtt.bytecode.BC_fneg");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_fneg.java", "float test(float)");
        assert (entryPoint != -1);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fneg.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float ", Float.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_C1X_jtt_BC_dneg2() throws Exception {
        initTests();
        double argsOne[] = { 1.0d, -1.0d, -0.0D, 0.0d, -2.0d, 2.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dneg2");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dneg2.java", "double test(double)");
        assert (entryPoint != -1);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dneg2.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", " double ", Double.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_XXdneg9() throws Exception {
        initTests();
        double argsOne[] = { 0.0D, -1.01D, 7263.8734d, 0.0d, -1.01d, 7263.8743d, 0.0d};
        double argsTwo[] = { 1.0d, -2.01D, 8263.8734d, 1.0d, -2.01d, 8263.8734d, 1.0d};
        int argsThree[] = { 0, 0, 0, 1, 1, 1, 0};
        String klassName = getKlassName("jtt.bytecode.BC_dneg");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        initialiseCodeBuffers(methods, "BC_dneg.java", "double test(double, double, int)");
        assert (entryPoint != -1);
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dneg.test(argsOne[i], argsTwo[i], argsThree[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double, int", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]) + "," + Integer.toString(argsThree[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_lload_0() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lload_0");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 1L));
        pairs.add(new Args(-3L, -3L));
        pairs.add(new Args(10000L, 10000L));
        pairs.add(new Args(549755814017L, 549755814017L));
        initialiseCodeBuffers(methods, "BC_lload_0.java", "long test(long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lload_0.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_lload_4() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lload_4");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 1));
        pairs.add(new Args(-3L, 1));
        pairs.add(new Args(10000L, 1));
        pairs.add(new Args(549755814017L, 1));
        initialiseCodeBuffers(methods, "BC_lload_4.java", "int test(long, int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lload_4.test(pair.lfirst, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, int", Long.toString(pair.lfirst) + "," + Long.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            // returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_lload_1() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lload_1");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1L));
        pairs.add(new Args(1, -3L));
        pairs.add(new Args(1, 10000L));
        initialiseCodeBuffers(methods, "BC_lload_1.java", "long test(int, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lload_1.test(pair.first, pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "int, long long", Integer.toString(pair.first) + "," + Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " r2 " + registerValues[2] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_lload_2() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lload_2");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1, 1L));
        pairs.add(new Args(1, 1, -3L));
        pairs.add(new Args(1, 1, 10000L));
        initialiseCodeBuffers(methods, "BC_lload_2.java", "long test(int, int, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lload_2.test(pair.first, pair.second, pair.lfirst);
            String functionPrototype = ARMCodeWriter
                            .preAmble("long long", "int, int, long long", Integer.toString(pair.first) + "," + Integer.toString(pair.second) + "," + Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_lload_3() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lload_3");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1, 1, 1L));
        pairs.add(new Args(1, 1, 1, -3L));
        pairs.add(new Args(1, 1, 1, 10000L));
        initialiseCodeBuffers(methods, "BC_lload_3.java", "long test(int, int, int, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lload_3.test(pair.first, pair.second, pair.third, pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "int, int, int, long long",
                            Integer.toString(pair.first) + "," + Integer.toString(pair.second) + "," + Integer.toString(pair.third) + "," + Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_ladd() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_ladd");
        List<TargetMethod> methods = Compile.compile(new String[]{klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(33L, 67L));
        pairs.add(new Args(1L, -1L));
        pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(2147483647L, 1L));
        pairs.add(new Args(-2147483647L, -2L));
        pairs.add(new Args(214723483647L, 1L));

        initialiseCodeBuffers(methods, "BC_ladd.java", "long test(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_ladd.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL ," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }

    }

    public void ignore_jtt_BC_lor() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.bytecode.BC_lor");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(31L, 67L));
        pairs.add(new Args(6L, 4L));
        pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(Long.MAX_VALUE, 0));
        pairs.add(new Args(0,Long.MAX_VALUE));
        pairs.add(new Args(0,Long.MIN_VALUE));
        pairs.add(new Args(Long.MIN_VALUE,0));
        pairs.add(new Args(Long.MAX_VALUE, 0));
        pairs.add(new Args(0,Long.MAX_VALUE-20L));
        pairs.add(new Args(0,Long.MIN_VALUE+20L));
        pairs.add(new Args(Long.MIN_VALUE,20L));
        pairs.add(new Args(0xdeadbeefd0daf0baL,0xdeadd0d0deadd0d0L));
        pairs.add(new Args(0xdeadbeefd0daf0baL,0xd0d0d0d0d0d0d0d0L));


        initialiseCodeBuffers(methods, "BC_lor.java", "long test(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lor.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond)+"LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
           // long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            long returnValue = connectRegs(registerValues[0],registerValues[1]);

            if(expectedValue != returnValue) {
                failed = true;//assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
                System.out.println("GOT " + returnValue + " EXPECTED " + expectedValue + " ARGS " +pair.lfirst + " " + pair.lsecond );
            }
            theCompiler.cleanup();

        }

        assert(failed == false);
    }

    public void ignore_jtt_BC_lxor() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lxor");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(31L, 63L));
        pairs.add(new Args(6L, 4L));
        pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(0xdeadbeefd0daf0baL,0xdeadd0d0deadd0d0L));
        pairs.add(new Args(0xdeadbeefd0daf0baL,0xd0d0d0d0d0d0d0d0L));
        initialiseCodeBuffers(methods, "BC_lxor.java", "long test(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lxor.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "," + Long.toString(pair.lsecond));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_land() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.bytecode.BC_land");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(31L, 63L));
        pairs.add(new Args(6L, 4L));
        pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 45));
        pairs.add(new Args(0xdeadbeefd0daf0baL,0xdeadd0d0deadd0d0L));
        pairs.add(new Args(0xdeadbeefd0daf0baL,0xd0d0d0d0d0d0d0d0L));


        initialiseCodeBuffers(methods, "BC_land.java", "long test(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_land.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL ," + Long.toString(pair.lsecond)+"LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            long returnValue = connectRegs(registerValues[0],registerValues[1]);

            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LAND GOT " +  returnValue + " EXPECTED " + expectedValue + " ARG " +pair.lfirst + " " + pair.lsecond );
                //assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            }
            theCompiler.cleanup();
        }
        assert(failed == false);
    }

    public void ignore_jtt_BC_lshl() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lshl");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2));
        pairs.add(new Args(1L, 0));
        pairs.add(new Args(1L, 1));
        pairs.add(new Args(1L, -1));


        pairs.add(new Args(0L, -1));
        pairs.add(new Args(31L, 1));
        pairs.add(new Args(6L, 4));
        pairs.add(new Args(-2147483648L, 1));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 45));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 70));
        pairs.add(new Args(0xdeadbeefd0daf0baL, -70));


        initialiseCodeBuffers(methods, "BC_lshl.java", "long test(long, int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lshl.test(pair.lfirst, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, int", Long.toString(pair.lfirst) + "LL," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            long returnValue = connectRegs(registerValues[0],registerValues[1]);
            //System.out.println("LSHL EXPECTED " + expectedValue + " GOT "+ returnValue);

            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_lshr() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lshr");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2));
        pairs.add(new Args(1L, 0));
        pairs.add(new Args(1L, -1));


        pairs.add(new Args(67L, 2));
        pairs.add(new Args(31L, 1));
        pairs.add(new Args(6L, 4));
        pairs.add(new Args(-2147483648L, 16));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 45));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 70));

        initialiseCodeBuffers(methods, "BC_lshr.java", "long test(long, int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lshr.test(pair.lfirst, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, int", Long.toString(pair.lfirst) + "LL," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            long returnValue = connectRegs(registerValues[0],registerValues[1]);
            //System.out.println("LSHR EXPECTED " + expectedValue + " GOT "+ returnValue);

            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }
    public void ignore_jtt_loop01() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        CompilationBroker.SIMULATEADAPTER = true;

        String klassName = getKlassName("jtt.loop.Loop01");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        CompilationBroker.SIMULATEADAPTER = false;

        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(62, 2));


        initialiseCodeBuffers(methods, "Loop01.java", "boolean test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            boolean answer = jtt.loop.Loop01.test(pair.first);
            int expectedValue = answer ? 1 : 0;

            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            int returnValue = registerValues[0];
            //System.out.println("LSHR EXPECTED " + expectedValue + " GOT "+ returnValue);
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LOOP01 GOT "  +returnValue + " EXPECT " + expectedValue );
            }
            theCompiler.cleanup();
        }
        assert(failed == false);
    }
    public void ignore_jtt_loopStack() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        CompilationBroker.SIMULATEADAPTER = true;

        String klassName = getKlassName("jtt.loop.LoopStack");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        CompilationBroker.SIMULATEADAPTER = false;

        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(3, 2));


        initialiseCodeBuffers(methods, "LoopStack.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            int expectedValue = jtt.loop.LoopStack.test(pair.first);

            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            int returnValue = registerValues[0];
            //System.out.println("LSHR EXPECTED " + expectedValue + " GOT "+ returnValue);
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LOOPSTACK GOT "  +returnValue + " EXPECT " + expectedValue );
            }
            theCompiler.cleanup();
        }
        assert(failed == false);
    }
    public void ignore_jtt_loop02() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        CompilationBroker.SIMULATEADAPTER = true;

        String klassName = getKlassName("jtt.loop.Loop02");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        CompilationBroker.SIMULATEADAPTER = false;

        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(62, 2));


        initialiseCodeBuffers(methods, "Loop02.java", "boolean test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            boolean answer = jtt.loop.Loop02.test(pair.first);
            int expectedValue = answer ? 1 : 0;

            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            int returnValue = registerValues[0];
            //System.out.println("LSHR EXPECTED " + expectedValue + " GOT "+ returnValue);
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LOOP02 GOT "  +returnValue + " EXPECT " + expectedValue );
            }
            theCompiler.cleanup();
        }
        assert(failed == false);
    }
    public void ignore_jtt_loop03() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.loop.Loop03");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(62, 2));
        pairs.add(new Args(10, 2));



        initialiseCodeBuffers(methods, "Loop03.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            int expectedValue = jtt.loop.Loop03.test(pair.first);

            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            int returnValue = registerValues[0];
            //System.out.println("LSHR EXPECTED " + expectedValue + " GOT "+ returnValue);

            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }
    public void ignore_jtt_loop04() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.loop.Loop04");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(10, 2));


        initialiseCodeBuffers(methods, "Loop04.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            int expectedValue = jtt.loop.Loop04.test(pair.first);

            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            int returnValue = registerValues[0];
            //System.out.println("LSHR EXPECTED " + expectedValue + " GOT "+ returnValue);
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LOOP04 GOT "  +returnValue + " EXPECT " + expectedValue );
            }
            theCompiler.cleanup();
        }
        assert(failed == false);
    }
    public void ignore_jtt_loop11() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.loop.Loop11");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(5, 2));


        initialiseCodeBuffers(methods, "Loop11.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            int expectedValue = jtt.loop.Loop11.test(pair.first);

            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            int returnValue = registerValues[0];
            //System.out.println("LSHR EXPECTED " + expectedValue + " GOT "+ returnValue);
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LOOP11 GOT "  +returnValue + " EXPECT " + expectedValue );
            }
            theCompiler.cleanup();
        }
        assert(failed == false);
    }
    public void ignore_jtt_loopPHI() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.loop.LoopPhi");
        CompilationBroker.SIMULATEADAPTER = true;

        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        CompilationBroker.SIMULATEADAPTER = false;

        List<Args> pairs = new LinkedList<Args>();
        /*
        Fails on arg 5000  and 10 this is because we don't have a good enough "STOP" condition
        ie we need to run for enough cycles in order to complete the simulation
         */
        //pairs.add(new Args(5000, 2));
       // pairs.add(new Args(0, 0));
       // pairs.add(new Args(1, -1));
      // pairs.add(new Args(2, 2));
      // pairs.add(new Args(3, 2));
      //  pairs.add(new Args(4, 2));
        pairs.add(new Args(5, 2));
       // pairs.add(new Args(6, 2));
       // pairs.add(new Args(7, 2));
       // pairs.add(new Args(8, 2));
       // pairs.add(new Args(9, 2));

        //pairs.add(new Args(10, 2));



        initialiseCodeBuffers(methods, "LoopPhi.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            int expectedValue = jtt.loop.LoopPhi.test(pair.first);

            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            int returnValue = registerValues[0];
            //System.out.println("LSHR EXPECTED " + expectedValue + " GOT "+ returnValue);
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LOOPPHI GOT "  +returnValue + " EXPECT " + expectedValue + " ARG "+ pair.first );
            }else System.out.println("ARG "+ pair.first + " OK");
            theCompiler.cleanup();
        }
        assert(failed == false);
    }
    public void test_jtt_BC_irem() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        CompilationBroker.SIMULATEADAPTER = true;

        String klassName = getKlassName("jtt.bytecode.BC_irem");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        CompilationBroker.SIMULATEADAPTER = false;

        List<Args> pairs = new LinkedList<Args>();

/*
 * @Harness: java
 * @Runs: (1,2)=1; (2,-1)=0; (256,4)=0; (135,7)=2
 */
        pairs.add(new Args(1, 2));
        pairs.add(new Args(2, -1));
        pairs.add(new Args(256, 4));
        pairs.add(new Args(135,7));
        pairs.add(new Args(Integer.MIN_VALUE,-1));
        pairs.add(new Args(-1,1));
        pairs.add(new Args(0,1));
        pairs.add(new Args(1000,1));
        pairs.add(new Args(Integer.MIN_VALUE,1));
        pairs.add(new Args(Integer.MAX_VALUE,1));


        initialiseCodeBuffers(methods, "BC_irem.java", "int test(int, int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            int expectedValue = jtt.bytecode.BC_irem.test(pair.first,pair.second);

            String functionPrototype = ARMCodeWriter.preAmble("int ", " int, int ", Integer.toString(pair.first) + ", "+ Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            int returnValue = registerValues[0];
            //System.out.println("LSHR EXPECTED " + expectedValue + " GOT "+ returnValue);
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("BC_irem GOT "  +returnValue + " EXPECT " + expectedValue );
                System.out.println("REG1 "+ registerValues[1]);
                System.out.println("REG8 "+ registerValues[8]);


            } else {
                System.out.println("SUCCESS BC_irem GOT "  +returnValue + " EXPECT " + expectedValue );
                System.out.println("REG1 "+ registerValues[1]);
                System.out.println("REG8 "+ registerValues[8]);

            }
            theCompiler.cleanup();
        }
        assert(failed == false);
    }
    public void ignore_jtt_loopInline() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        CompilationBroker.SIMULATEADAPTER = true;
        boolean failed = false;
        String klassName = getKlassName("jtt.loop.LoopInline");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        CompilationBroker.SIMULATEADAPTER = false;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(3, 2));

        pairs.add(new Args(10, 2));


        initialiseCodeBuffers(methods, "LoopInline.java", "int test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            int expectedValue = jtt.loop.LoopInline.test(pair.first);

            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            //long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            int returnValue = registerValues[0];
            //System.out.println("LSHR EXPECTED " + expectedValue + " GOT "+ returnValue);
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LOOPInline GOT "  +returnValue + " EXPECT " + expectedValue + " ARG " + pair.first );
            }
            theCompiler.cleanup();
        }
        assert(failed == false);
    }
    private static final BigInteger TWO_64 = BigInteger.ONE.shiftLeft(64);

    public String asUnsignedDecimalString(long l) {
        BigInteger b = BigInteger.valueOf(l);
        if (b.signum() < 0) {
            b = b.add(TWO_64);
        }
        return b.toString();
    }
public long connectRegs(int reg0, int reg1) {
        long returnVal = 0;
        long tmp = 0;
        //returnVal = ((long) reg1 << 32 | ((long)reg0 &0xffffffffL));
        //System.out.println(" REG0 " + reg0 + " REG1 " + reg1);
        if(reg1 < 0) {
            // -ve long number

            returnVal = ((long) reg1) << 32;


            if(reg0 < 0) {
                returnVal += 1L<< 31;
                returnVal += ((long)reg0)+(1L << 31);
            }else {
                returnVal += reg0;
            }
        } else {
           // +ve long number
            returnVal = ((long)reg1) << 32;
            if(reg0 < 0) {
                returnVal += 1L << 31;
                returnVal += ((long)reg0)+(1L << 31);
            } else {
                returnVal += reg0;
            }
        }

        return returnVal;
    }
    public long OLDconnectRegs(int reg0, int reg1) {
        long returnVal = 0;

        String r0 = String.format("%32s", Integer.toBinaryString(reg0)).replace(' ', '0');
        String r1 = String.format("%32s", Integer.toBinaryString(reg1)).replace(' ', '0');
        // System.out.println("Reg0 " + reg0 + " R0 " + r0);
        // System.out.println("Reg1 " + reg1 + " R1 " + r1);
        String ret = (reg1 == 0 ? r0 : r1.concat(r0));
        StringBuffer newString = null;
        try {
            if (reg1 == 0) {
                int ret1 = Integer.parseInt(ret, 2);
                returnVal = Math.abs(ret1);
            } else {
                returnVal = Long.parseLong(ret, 2);
            }
        } catch (NumberFormatException e) {
            newString = new StringBuffer();
            if (ret.startsWith("1")) {
                for (int i = 0; i < ret.length(); i++) {
                    if (ret.charAt(i) == '1') {
                        newString.append('0');
                    } else if (ret.charAt(i) == '0') {
                        newString.append('1');
                    }
                }
                returnVal = Long.parseLong(newString.toString(), 2) + 1;
                if (returnVal != 2147483648L) {
                    returnVal = -1 * returnVal;
                }
            }
        }
        return returnVal;
    }
    public void ignore_jtt_BC_connectREGS() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.bytecode.BC_connectREGS");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2));
        pairs.add(new Args(-1L,1));

        pairs.add(new Args(0L,1));
        pairs.add(new Args((long)Integer.MAX_VALUE, 45));
        pairs.add(new Args((long)Integer.MAX_VALUE+5L, 45));

        pairs.add(new Args(Long.MAX_VALUE, 45));

        pairs.add(new Args((long)Integer.MIN_VALUE, 16));
        pairs.add(new Args((long)Integer.MIN_VALUE-5L, 16));

        pairs.add(new Args((long)Integer.MAX_VALUE, 45));
        pairs.add(new Args(Long.MIN_VALUE, 16));
        pairs.add(new Args(Long.MIN_VALUE+5L, 16));

        pairs.add(new Args(Long.MAX_VALUE, 45));
        pairs.add(new Args(Long.MAX_VALUE-5L, 45));


        initialiseCodeBuffers(methods, "BC_connectREGS.java", "long test(long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = BC_connectREGS.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst)+"LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("connectREGS EXPECTED " + expectedValue + " GOT "+ returnValue + " REG0 " +  registerValues[0] + " REG1  " + registerValues[1]);
            }

            //assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
        assert (failed == false);
    }
    /*
    public static boolean le(long a, long b) { return a <=b};
    public static boolean ge(long a, long b) { return a >=b};
    public static boolean eq(long a, long b) { return a ==b};
    public static boolean ne(long a, long b) { return a <=b};
    public static boolean lt(long a, long b) { return a <b};
    public static boolean gt(long a, long b) { return a >b};*/

    public void ignore_jtt_BC_LONGle() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.bytecode.BC_connectREGS");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L,1L));

        pairs.add(new Args(0L,0L));
        pairs.add(new Args((long)Integer.MAX_VALUE,(long)Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE,Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE,Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));

        initialiseCodeBuffers(methods, "BC_connectREGS.java", "boolean le(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            boolean returnValue = false;
            boolean expectedValue = BC_connectREGS.le(pair.lfirst,pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst)+"LL," + Long.toString(pair.lsecond)+ "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if( registerValues[0] == 1 ) returnValue = true;
            else if( registerValues[0] == 0) returnValue = false;
            else failed = true;
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LONG le EXPECTED " + expectedValue + " GOT "+
     returnValue + " REG0 " +  registerValues[0] + " REG1  " +
                        registerValues[1] + " FOR " + pair.lfirst + " " + pair.lsecond);
            }

            //assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
        assert (failed == false);
    }


    public void ignore_jtt_BC_LONGge() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.bytecode.BC_connectREGS");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 1L));
        pairs.add(new Args(-1L,1L));

        pairs.add(new Args(0L,0L));
        pairs.add(new Args((long)Integer.MAX_VALUE,(long)Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE,Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE,Long.MIN_VALUE));

        initialiseCodeBuffers(methods, "BC_connectREGS.java", "boolean ge(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            boolean returnValue = false;
            boolean expectedValue = BC_connectREGS.ge(pair.lfirst,pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst)+"LL," + Long.toString(pair.lsecond)+ "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if( registerValues[0] == 1 ) returnValue = true;
            else if( registerValues[0] == 0) returnValue = false;
            else failed = true;
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LONG ge EXPECTED " + expectedValue + " GOT "+ returnValue + " REG0 " +  registerValues[0] + " REG1  "
                        + registerValues[1]+ " FOR "+ pair.lfirst+ " " + pair.lsecond);
            }

            //assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
        assert (failed == false);
    }

    public void ignore_jtt_BC_LONGeq() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.bytecode.BC_connectREGS");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L,1L));

        pairs.add(new Args(0L,0L));
        pairs.add(new Args((long)Integer.MAX_VALUE,(long)Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE,Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE,Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));

        initialiseCodeBuffers(methods, "BC_connectREGS.java", "boolean eq(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            boolean returnValue = false;
            boolean expectedValue = BC_connectREGS.eq(pair.lfirst,pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst)+"LL," + Long.toString(pair.lsecond)+ "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if( registerValues[0] == 1 ) returnValue = true;
            else if( registerValues[0] == 0) returnValue = false;
            else failed = true;
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LONG EQ EXPECTED " + expectedValue + " GOT "+ returnValue + " REG0 " +  registerValues[0] + " REG1  " + registerValues[1]
                + " FOR " + pair.lfirst + "  " + pair.lsecond);

            }

            //assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
        assert (failed == false);
    }
    public void ignore_jtt_BC_LONGne() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.bytecode.BC_connectREGS");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L,1L));

        pairs.add(new Args(0L,0L));
        pairs.add(new Args((long)Integer.MAX_VALUE,(long)Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE,Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE,Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));

        initialiseCodeBuffers(methods, "BC_connectREGS.java", "boolean ne(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            boolean returnValue = false;
            boolean expectedValue = BC_connectREGS.ne(pair.lfirst,pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst)+"LL," + Long.toString(pair.lsecond)+ "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if( registerValues[0] == 1 ) returnValue = true;
            else if( registerValues[0] == 0) returnValue = false;
            else failed = true;
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LONG NE EXPECTED " + expectedValue + " GOT "+ returnValue + " REG0 " +  registerValues[0] + " REG1  " + registerValues[1]
                        + " FOR " + pair.lfirst + "  " + pair.lsecond);

            }

            //assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
        assert (failed == false);
    }
    public void ignore_jtt_BC_LONGgt() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.bytecode.BC_connectREGS");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L,1L));

        pairs.add(new Args(0L,0L));
        pairs.add(new Args((long)Integer.MAX_VALUE,(long)Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE,Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE,Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));

        initialiseCodeBuffers(methods, "BC_connectREGS.java", "boolean gt(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            boolean returnValue = false;
            boolean expectedValue = BC_connectREGS.gt(pair.lfirst,pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst)+"LL," + Long.toString(pair.lsecond)+ "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if( registerValues[0] == 1 ) returnValue = true;
            else if( registerValues[0] == 0) returnValue = false;
            else failed = true;
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LONG GT EXPECTED " + expectedValue + " GOT "+ returnValue + " REG0 " +  registerValues[0] + " REG1  " + registerValues[1]
                        + " FOR " + pair.lfirst + "  " + pair.lsecond);

            }

            //assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
        assert (failed == false);
    }
    public void ignore_jtt_BC_LONGlt() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.bytecode.BC_connectREGS");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L,1L));

        pairs.add(new Args(0L,0L));
        pairs.add(new Args((long)Integer.MAX_VALUE,(long)Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE,Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE,Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));

        initialiseCodeBuffers(methods, "BC_connectREGS.java", "boolean lt(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            boolean returnValue = false;
            boolean expectedValue = BC_connectREGS.lt(pair.lfirst,pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst)+"LL," + Long.toString(pair.lsecond)+ "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            if( registerValues[0] == 1 ) returnValue = true;
            else if( registerValues[0] == 0) returnValue = false;
            else failed = true;
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("LONG LT EXPECTED " + expectedValue + " GOT "+ returnValue + " REG0 " +  registerValues[0] + " REG1  " + registerValues[1]
                        + " FOR " + pair.lfirst + "  " + pair.lsecond);

            }

            //assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
        assert (failed == false);
    }
    public void ignore_jtt_BC_movlong() throws Exception {
        boolean failed = false;
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_movlong");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;

        long [] values= new long [10];
        values[0] = 0L;
        values[1] = -1L;
        values[2] = (long)Integer.MIN_VALUE;
        values[3] = (long)Integer.MAX_VALUE;
        values[4] = Long.MAX_VALUE;
        values[5] = Long.MIN_VALUE;
        values[6] = Long.MIN_VALUE + 5;
        values[7] = Long.MAX_VALUE -5;
        values[8] = ((long)Integer.MIN_VALUE) +5L;
        values[9] = ((long)Integer.MAX_VALUE) -5L;

        initialiseCodeBuffers(methods, "BC_movlong.java", "long test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (int i = 0; i <values.length;i++ ) {
            long expectedValue = BC_movlong.test(i);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "int", Integer.toString(i));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);

            if( returnValue != expectedValue) {
                failed = true;
                System.out.println(i+ "MOVLONG EXPECTED " + expectedValue + " GOT "+ returnValue + " REG0 " +  registerValues[0] +
                        " REG1 " +registerValues[1]);

            }
            theCompiler.cleanup();
        }
        assert(failed == false);
    }

    public void ignore_jtt_BC_lushr() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lushr");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2));
        pairs.add(new Args(67L, 2));
        pairs.add(new Args(31L, 1));
        pairs.add(new Args(6L, 4));
        pairs.add(new Args(-2147483648L, 16));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 45));

        initialiseCodeBuffers(methods, "BC_lushr.java", "long test(long, int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lushr.test(pair.lfirst, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("unsigned long long", "unsigned long long, int", asUnsignedDecimalString(pair.lfirst) + "," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            //System.out.println("LUSHR EXPECTED " + expectedValue + " GOT "+ returnValue);

            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_lcmp() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lcmp");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(77L, 78L));
        pairs.add(new Args(-1L, 0L));
        pairs.add( new Args(0L,0L));
        pairs.add( new Args(Long.MAX_VALUE,Long.MIN_VALUE));

        initialiseCodeBuffers(methods, "BC_lcmp.java", "boolean test(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            boolean expectedValue = jtt.bytecode.BC_lcmp.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "," + Long.toString(pair.lsecond));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            boolean returnValue = registerValues[0] > 0 ? true : false;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }
    public void ignore_jtt_BC_lcmp2() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lcmp2");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(77L, 78L));
        pairs.add(new Args(-1L, 0L));
        pairs.add( new Args(0L,0L));
        pairs.add( new Args(Long.MAX_VALUE,Long.MIN_VALUE));

        initialiseCodeBuffers(methods, "BC_lcmp2.java", "boolean test(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            boolean expectedValue = jtt.bytecode.BC_lcmp2.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "," + Long.toString(pair.lsecond));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            boolean returnValue = registerValues[0] > 0 ? true : false;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }
    // TODO: Figure out why -2147483648L and 2147483648L have
    // the same reg0,reg1 values
    public void ignore_jtt_BC_lmul() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.bytecode.BC_lmul");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(33L, 67L));
        pairs.add(new Args(1L, -1L));
        // pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(2147483647L, -1L));
        pairs.add(new Args(-2147483648L, -1L));
        pairs.add(new Args(1000000L, 1000000L));
        initialiseCodeBuffers(methods, "BC_lmul.java", "long test(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lmul.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL ," + Long.toString(pair.lsecond)+"LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("EXPECTED " + expectedValue  + " GOT " + returnValue + " REG 0 was " + registerValues[0] + " REG1 " + registerValues[1]);
            }
            //assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
        assert(failed == false);
    }

    public void ignore_jtt_BC_lneg() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.bytecode.BC_lneg");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(7263L, -7263L));
        pairs.add(new Args(-2147483648L, 2147483648L));
        initialiseCodeBuffers(methods, "BC_lneg.java", "long test(long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lneg.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst)+"LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("EXPECTED " + expectedValue  + " GOT " + returnValue + " REG 0 was " + registerValues[0] + " REG1 " + registerValues[1]);
            }//assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
           assert(failed == false);
    }

    public void ignore_jtt_BC_lreturn() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lreturn");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args(1L, 1L));
        pairs.add(new Args(-1L, -1L));
        pairs.add(new Args(256L, 256L));
        pairs.add(new Args(1000000000000L, 1000000000000L));
        initialiseCodeBuffers(methods, "BC_lreturn.java", "long test(long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lreturn.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_lsub() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_lsub");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, -2L));
        pairs.add(new Args(0L, 1L));
        pairs.add(new Args(33L, -67L));
        pairs.add(new Args(1L, 1L));
        pairs.add(new Args(-2147483648L, -1L));
        pairs.add(new Args(2147483647L, -1L));
        pairs.add(new Args(-2147483647L, 2L));
        pairs.add(new Args(Long.MAX_VALUE, 2L));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(0L, Long.MIN_VALUE));
        pairs.add(new Args(Long.MIN_VALUE, Long.MIN_VALUE));



        initialiseCodeBuffers(methods, "BC_lsub.java", "long test(long, long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lsub.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL ," + Long.toString(pair.lsecond)+ "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_jtt_BC_i2l() throws Exception {
        boolean failed = false;
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_i2l");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(Integer.MIN_VALUE, (long)Integer.MIN_VALUE));

        pairs.add(new Args(1, 1L));
        pairs.add(new Args(2, 2L));
        pairs.add(new Args(3, 3L));
        pairs.add(new Args(-1, -1L));
        pairs.add(new Args(Integer.MIN_VALUE/2, (long)Integer.MIN_VALUE));

        pairs.add(new Args(Integer.MAX_VALUE, (long)Integer.MAX_VALUE));
        initialiseCodeBuffers(methods, "BC_i2l.java", "long test(int)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_i2l.test(pair.first);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);

            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("REG 0 was " + registerValues[0] + " REG1 " + registerValues[1]);
                System.out.println("i2l GOT " + returnValue+ " EXPECTED " + expectedValue + " ARGS " + pair.first );
            }
            //assert returnValue ==  Math.abs(expectedValue) : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
            assert(failed == false);
        }
        assert(failed == false);
    }

    public void ignore_jtt_BC_l2i() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        boolean failed = false;
        String klassName = getKlassName("jtt.bytecode.BC_l2i");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 1));
        pairs.add(new Args(2L, 2));
        pairs.add(new Args(3L, 3));
        pairs.add(new Args(-1L, -1));
        pairs.add(new Args(-2147483647L, -2147483647));
        pairs.add(new Args(-2147483648L, -2147483648));
        pairs.add(new Args(2147483647L, 2147483647));
        pairs.add(new Args(Long.MAX_VALUE, Integer.MAX_VALUE));
        pairs.add(new Args(Long.MIN_VALUE, Integer.MIN_VALUE));
        pairs.add(new Args(0, 0));



        initialiseCodeBuffers(methods, "BC_l2i.java", "int test(long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            int expectedValue = jtt.bytecode.BC_l2i.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble( "int", "long long", Long.toString(pair.lfirst)+"LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            if(returnValue != expectedValue) {
                failed = true;
                System.out.println("L2I GOT "+ returnValue + " EXPECTED "+ expectedValue + " ARG "+ pair.lfirst);
                //assert Math.abs(returnValue) ==  Math.abs(expectedValue) : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            }
            theCompiler.cleanup();
        }
        assert(failed == false);
    }

    //TODO: Implement functionality
    public void ignore_jtt_BC_l2f() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("jtt.bytecode.BC_l2f");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, 0.0f));
        pairs.add(new Args(1L, 1.0f));
        pairs.add(new Args(-74652389L, -74652389.00f));
        initialiseCodeBuffers(methods, "BC_l2f.java", "int test(long)");
        int assemblerStatements = codeBytes.length / 4;
        for (Args pair : pairs) {
            float expectedValue = jtt.bytecode.BC_l2f.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble( "float", "long long", Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, assemblerStatements, expectedValues, testvalues, bitmasks);
            float returnValue = registerValues[0];
            assert returnValue ==  expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void ignore_generic_compilation1() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("com.sun.max.vm.jni.JniFunctions");
        List<TargetMethod> methods = Compile.compileMethod(new String[] { klassName}, "C1X", "RegisterNatives");
        //List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.OFFLINE = true;
        theCompiler.cleanup();
    }
    public void ignore_generic_compilation() throws Exception {
        CompilationBroker.OFFLINE = initialised;
        String klassName = getKlassName("com.sun.max.vm.MaxineVM");
        //List<TargetMethod> methods = Compile.compileMethod(new String[] { klassName}, "C1X", "patchReturnAddress");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods, "MaxineVM.java", "int run(Pointer, int," +                " Pointer, Word, Word, Word, Pointer, Pointer, Pointer, Pointer, int, Pointer)");

        CompilationBroker.OFFLINE = true;
        theCompiler.cleanup();
    }
}
