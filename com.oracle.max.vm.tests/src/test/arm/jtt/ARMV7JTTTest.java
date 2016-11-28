package test.arm.jtt;

import java.io.*;
import static com.sun.max.vm.MaxineVM.vm;
import java.lang.reflect.*;
import java.math.*;
import java.util.*;

import org.objectweb.asm.util.*;

import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.*;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.maxri.*;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.armv7.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

import jtt.bytecode.*;
import test.arm.asm.*;

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
        codeAttr = new CodeAttribute(null, new byte[15], (char) 40, (char) 20, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create("(Ljava/util/Map;)V"), Actor.JAVA_METHOD_FLAGS, codeAttr, new String());
    }

    public void initialiseFrameForCompilation(byte[] code, String sig) {
        codeAttr = new CodeAttribute(null, code, (char) 40, (char) 20, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create(sig), Actor.JAVA_METHOD_FLAGS, codeAttr, new String());
    }

    public void initialiseFrameForCompilation(byte[] code, String sig, int flags) {
        codeAttr = new CodeAttribute(null, code, (char) 40, (char) 20, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create(sig), flags, codeAttr, new String());
    }

    public void initialiseFrameForCompilation(ConstantPool cp, byte[] code, String sig, int flags) {
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
            this.fsecond = (long) fsecond;
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

    private Object[] generateObjectsAndTestStubs(String functionPrototype, int entryPoint, byte[] theCode, int[] expected, boolean[] tests, MaxineARMTester.BitsFlag[] masks) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(theCode);
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

    private int[] generateAndTestStubs(String functionPrototype, int entryPoint, byte[] theCode, int[] expected, boolean[] tests, MaxineARMTester.BitsFlag[] masks) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(theCode);
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

    private int[] generateAndTest(int[] expected, boolean[] tests, MaxineARMTester.BitsFlag[] masks) throws Exception {
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
        int[] simulatedRegisters = r.runRegisteredSimulation();
        r.cleanProcesses();
        if (POST_CLEAN_FILES) {
            r.cleanFiles();
        }
        return simulatedRegisters;
    }

    private int[] generateAndTest(long[] expected, boolean[] tests, MaxineARMTester.BitsFlag[] masks) throws Exception {
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
                vm().compilationBroker.setOffline(true);
                JavaPrototype.initialize(false);
                initialised = true;
            }
            t1x = (T1X) CompilationBroker.addCompiler("t1x", baselineCompilerName);
            c1x = (C1X) CompilationBroker.addCompiler("c1x", optimizingCompilerName);
            c1x.initialize(Phase.HOSTED_TESTING);
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

    public void test_C1X_jtt_BC_imul() throws Exception {
        initTests();
        int argsOne[] = { 1, 0, 33, 1, -2147483648, 2147483647, -2147483648};
        int argsTwo[] = { 12, -1, 67, -1, 1, -1, -1};
        String klassName = getKlassName("jtt.bytecode.BC_imul");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_imul.java", "int test(int, int)");
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_imul.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Integer) registerValues[0] == (new Integer(expectedValue).intValue()) : "Failed incorrect value " + registerValues[0] + " " + (new Integer(expectedValue).intValue());
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iadd2() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, argsOne[i]);
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, argsTwo[i]);
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == answer : ("Failed incorrect value " + registerValues[0] + " " + answer);
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iadd3() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, argsOne[i]);
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, argsTwo[i]);
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_imul() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_imul.test(10, 12);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_imul");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 10);
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, 12);
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "imul");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_isub() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_isub.test(100, 50);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_isub");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 100);
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, 50);
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "isub");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    private void createTemplates() {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "LDC");
    }

    public void test_T1X_jtt_loop_loop01() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.SIMULATE_ADAPTER = true;
        createTemplates();
        String klassName = getKlassName("jtt.loop.Loop01");
        vm().compilationBroker.setOffline(true);
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "T1X");
        CompilationBroker.SIMULATE_ADAPTER = false;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(62, 2));
        initialiseCodeBuffers(methods, "Loop01.java", "boolean test(int)");

        for (Args pair : pairs) {
            boolean answer = jtt.loop.Loop01.test(pair.first);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ineg() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ineg.test(100);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ineg");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 100);
        masm.push(ConditionFlag.Always, 1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ineg");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ineg_1() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ineg.test(-100);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ineg");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, -100);
        masm.push(ConditionFlag.Always, 1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ineg");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_iadd() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iadd.test(50, -49);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iadd");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 50);
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, -49);
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ior() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ior.test(50, 100);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ior");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 50);
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, 100);
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ior");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ixor() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ixor.test(50, 39);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ixor");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 50);
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, 39);
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ixor");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_iand() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iand.test(50, 39);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iand");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 50);
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, 39);
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iand");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_C1X_jtt_BC_iand() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_iand");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(0, -1));
        pairs.add(new Args(3, 63));
        pairs.add(new Args(6, 4));
        pairs.add(new Args(-2147483648, 1));
        initialiseCodeBuffers(methods, "BC_iand.java", "int test(int, int)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_iand.test(pair.first, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int, int", Integer.toString(pair.first) + "," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ishl() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ishl.test(10, 2);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ishl");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 10);
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, 2);
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);

        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishl");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_C1X_jtt_BC_ishl() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_ishl");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(0, -1));
        pairs.add(new Args(31, 1));
        pairs.add(new Args(6, 4));
        pairs.add(new Args(-2147483648, 1));
        pairs.add(new Args(-9999, 12));
        initialiseCodeBuffers(methods, "BC_ishl.java", "int test(int, int)");

        for (Args pair : pairs) {
            int expectedValue = jtt.bytecode.BC_ishl.test(pair.first, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int, int", Integer.toString(pair.first) + "," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_ishr() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_ishr");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(0, -1));
        pairs.add(new Args(31, 1));
        pairs.add(new Args(6, 4));
        pairs.add(new Args(-2147483648, 16));
        pairs.add(new Args(67, 2));
        initialiseCodeBuffers(methods, "BC_ishr.java", "int test(int, int)");

        for (Args pair : pairs) {
            int expectedValue = jtt.bytecode.BC_ishr.test(pair.first, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int, int", Integer.toString(pair.first) + "," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_iushr() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_iushr");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(0, -1));
        pairs.add(new Args(31, 1));
        pairs.add(new Args(6, 4));
        pairs.add(new Args(-2147483648, 1));
        pairs.add(new Args(-9999, 12));
        pairs.add(new Args(-2048, 6));
        pairs.add(new Args(99944444, 2));
        initialiseCodeBuffers(methods, "BC_iushr.java", "int test(int, int)");

        for (Args pair : pairs) {
            int expectedValue = jtt.bytecode.BC_iushr.test(pair.first, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int, int", Integer.toString(pair.first) + "," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ishr() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ishr.test(2048, 2);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ishr");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 2048);
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, 2);
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishr");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ishr_1() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ishr.test(-2147483648, 16);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ishr");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, -2147483648);
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, 16);
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishr");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_iushr() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iushr.test(-2147483648, 16);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iushr");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, -2147483648);
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, 16);
        masm.push(ConditionFlag.Always, 1);
        masm.push(ConditionFlag.Always, 2);

        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iushr");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2b() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.SIMULATE_ADAPTER = true;
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        byte answer = jtt.bytecode.BC_i2b.test(255);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2b");
        initialiseFrameForCompilation(code, "(I)B", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        CompilationBroker.SIMULATE_ADAPTER = false;
        masm.nop(4);
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 255);
        masm.push(ConditionFlag.Always, 1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_C1X_jtt_BC_i2b() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.SIMULATE_ADAPTER = true;
        initTests();
        int argsOne[] = { 0, /*-1, 2, 255, 128*/};
        String klassName = getKlassName("jtt.bytecode.BC_i2b");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.SIMULATE_ADAPTER = true;
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_i2b.java", "byte test(int)");
        byte expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_i2b.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("char", "int", Integer.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).byteValue() == expectedValue : "Failed incorrect value " + ((Integer) registerValues[0]).byteValue() + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_i2b_1() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2b.test(-1);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2b");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, -1);
        masm.push(ConditionFlag.Always, 1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2b_2() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2b.test(128);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2b");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 128);
        masm.push(ConditionFlag.Always, 1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2s() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2s.test(65535);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 65535);
        masm.push(ConditionFlag.Always, 1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2s_1() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2s.test(32768);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 32768);
        masm.push(ConditionFlag.Always, 1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2s_2() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2s.test(-1);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, -1);
        masm.push(ConditionFlag.Always, 1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2c() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2c.test(-1);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2c");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, -1);
        masm.push(ConditionFlag.Always, 1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2c");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2c_1() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2c.test(65535);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_i2c");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 65535);
        masm.push(ConditionFlag.Always, 1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2c");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ireturn() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        int answer = jtt.bytecode.BC_ireturn.test(-1);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ireturn");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, -1);
        masm.push(ConditionFlag.Always, 1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void ignore2_T1X_jtt_BC_ireturn() throws Exception {
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        int answer = jtt.bytecode.BC_ireturn.test(256);
        expectedValues[0] = answer;
        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_ireturn");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, 256);
        masm.push(ConditionFlag.Always, 1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(ConditionFlag.Always, 1);
        int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        theCompiler.cleanup();
    }

    public void test_C1X_jtt_BC_ireturn() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        int argOne[] = { 0, 1, -1, 256};
        String klassName = getKlassName("jtt.bytecode.BC_ireturn");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_ireturn.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = jtt.bytecode.BC_ireturn.test(argOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : ("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lookupswitch01() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        int argOne[] = { 0, 1, 44, 67, 68, 96, 97, 98, 106, 107, 108, 132, 133, 134, 211, 212, 213};
        String klassName = getKlassName("jtt.bytecode.BC_lookupswitch01");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_lookupswitch01.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = jtt.bytecode.BC_lookupswitch01.test(argOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : ("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lookupswitch02() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        int argOne[] = { 0, 1, 44, 66, 67, 68, 96, 97, 98, 106, 107, 108, 132, 133, 134, 211, 212, 213, -121, -122, -123};
        String klassName = getKlassName("jtt.bytecode.BC_lookupswitch02");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_lookupswitch02.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = jtt.bytecode.BC_lookupswitch02.test(argOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : ("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lookupswitch03() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        int argOne[] = { 0, 1, 44, 66, 67, 68, 96, 97, 98, 106, 107, 108, 132, 133, 134, 211, 212, 213, -121, -122, -123};
        String klassName = getKlassName("jtt.bytecode.BC_lookupswitch03");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_lookupswitch03.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = jtt.bytecode.BC_lookupswitch03.test(argOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : ("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lookupswitch04() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        int argOne[] = { 0, 1, 44, 66, 67, 68, 96, 97, 98, 106, 107, 108, 132, 133, 134, 211, 212, 213, -121, -122, -123};
        String klassName = getKlassName("jtt.bytecode.BC_lookupswitch04");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_lookupswitch04.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = jtt.bytecode.BC_lookupswitch01.test(argOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : ("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_tableswitch() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        int argOne[] = { 7, -1, 0, 1, 2, 3, 4, 5, 6, 0};
        String klassName = getKlassName("jtt.bytecode.BC_tableswitch");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_tableswitch.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = jtt.bytecode.BC_tableswitch.test(argOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : ("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_tableswitch2() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        int argOne[] = { 7, -1, 0, 1, 2, 3, 4, 5, 6, 0};
        String klassName = getKlassName("jtt.bytecode.BC_tableswitch2");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_tableswitch2.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = jtt.bytecode.BC_tableswitch2.test(argOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : ("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_tableswitch3() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        int argOne[] = { 01, -2, -3, 0, -4, 3, 1, 2, 10};
        String klassName = getKlassName("jtt.bytecode.BC_tableswitch3");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_tableswitch3.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = jtt.bytecode.BC_tableswitch3.test(argOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : ("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_tableswitch4() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        int argOne[] = { -1, 11, 0, 1, -5, -4, -3, -8};
        String klassName = getKlassName("jtt.bytecode.BC_tableswitch4");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_tableswitch4.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = jtt.bytecode.BC_tableswitch4.test(argOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : ("Failed incorrect value " + registerValues[0] + " " + expectedValue);
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_tableswitch() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_tableswitch_2() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_XXXXfdiv() throws Exception {
        MaxineARMTester.DEBUGOBJECTS = false;
        initTests();
        MaxineByteCode xx = new MaxineByteCode();
        float argOne[] = { 14.0f};
        float argTwo[] = { 7.0f};
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "freturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "freturnUnlock");

        byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_fdiv");
        for (int i = 0; i < argOne.length; i++) {
            initialiseFrameForCompilation(code, "(FF)F", Modifier.PUBLIC | Modifier.STATIC);
            float answer = jtt.bytecode.BC_fdiv.test(argOne[i], argTwo[i]);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.s0, Float.floatToRawIntBits(argOne[i]));
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.s1, Float.floatToRawIntBits(argTwo[i]));
            masm.vpush(ConditionFlag.Always, ARMV7.s0, ARMV7.s0, CiKind.Float, CiKind.Float);
            masm.vpush(ConditionFlag.Always, ARMV7.s1, ARMV7.s1, CiKind.Float, CiKind.Float);

            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "fdiv");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.vpop(ConditionFlag.Always, ARMV7.s0, ARMV7.s0, CiKind.Float, CiKind.Float);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float", Float.toString(argOne[i]) + "," + Float.toString(argTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, 0, masm.codeBuffer.close(false), expectedValues, testvalues, bitmasks);
            assert (((Float) registerValues[33]).floatValue() == answer) : " FDIV T1X " + ((Float) registerValues[33]).floatValue() + " " + answer;
        }
    }

    public void test_T1X_jtt_BC_tableswitch_3() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_tableswitch_4() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_lookupswitch_1() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_lookupswitch_2() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_lookupswitch_3() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_lookupswitch_4() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iinc_1() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(2, 3));
        pairs.add(new Args(4, 5));
        pairs.add(new Args(1, 0));
        CompilationBroker.SIMULATE_ADAPTER = true;
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            MaxineByteCode xx = new MaxineByteCode();
            int answer = jtt.bytecode.BC_iinc_1.test(pair.first);
            expectedValues[0] = answer;
            System.out.println("iinc_1 " + answer);
            byte[] code = xx.getByteArray("test", "jtt.bytecode.BC_iinc_1");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_1");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iinc_2() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iinc_3() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iinc_4() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_4");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iload_0() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iload_0_1() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0_1");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iload_0_2() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1); // local slot is argument r0
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iload_1() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, pair.second);
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_t1X_jtt_BC_iload_1_1() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_1_1");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iload_2() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, pair.second);
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r2, pair.third);
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            masm.push(ConditionFlag.Always, 4);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iload_3() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, pair.second);
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r2, pair.third);
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r3, pair.fourth);
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            masm.push(ConditionFlag.Always, 4);
            masm.push(ConditionFlag.Always, 8);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iconst() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iconst");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_iconst() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_iconst");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(3, 3));
        pairs.add(new Args(4, 4));
        pairs.add(new Args(5, 5));
        pairs.add(new Args(6, 375));
        initialiseCodeBuffers(methods, "BC_iconst.java", "int test(int)");

        for (Args pair : pairs) {
            int expectedValue = jtt.bytecode.BC_iconst.test(pair.first);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ifeq() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ifeq_2() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ifeq_3() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ifge() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ifgt() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifgt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ifle() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifle");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ifne() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifne");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iflt() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ificmplt1() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ificmplt2() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ificmpne1() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_ificmpne2() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ifge_3() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, pair.second);
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifgt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ifge_2() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, pair.first);
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r1, pair.second);
            masm.push(ConditionFlag.Always, 1);
            masm.push(ConditionFlag.Always, 2);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : (" Failed incorrect values " + registerValues[0] + " " + expectedValues[0]);
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_invokestatic() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        List<Args> pairs = new LinkedList<Args>();
        String klassName = getKlassName("jtt.bytecode.BC_invokestatic");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods);
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(-2, -2));
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_invokestatic.test(pair.first);
            expectedValues[0] = answer;
            String functionPrototype = ARMCodeWriter.preAmble("void", "int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_f2d() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_f2d");
        MaxineARMTester.DEBUGOBJECTS = false;
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods);
        float[] arguments = { -2.2f, 0.0f, 1.0f, 01.06f};
        double expectedDouble = -9;
        for (int i = 0; i < arguments.length; i++) {
            double answer = jtt.bytecode.BC_f2d.test(arguments[i]);
            expectedDouble = answer;
            String functionPrototype = ARMCodeWriter.preAmble("double", "float", Float.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Double) registerValues[17]).doubleValue() == expectedDouble : ("Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + expectedDouble);
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_i2f() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_i2f");
        MaxineARMTester.DEBUGOBJECTS = false;
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods, "BC_i2f.java", "float test(int)");
        int[] arguments = { -100, 0, 1, -1, -99};
        float expectedFloat = -9;
        for (int i = 0; i < arguments.length; i++) {
            expectedFloat = jtt.bytecode.BC_i2f.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "int", Integer.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Float) registerValues[33]).floatValue() == expectedFloat : ("Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + expectedFloat);
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_f2b() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.SIMULATE_ADAPTER = true;
        initTests();
        String klassName = getKlassName("jtt.bytecode.BC_f2b");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.SIMULATE_ADAPTER = true;
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_f2b.java", "byte test(float)");
        float[] arguments = { -2.2f, 0.0f, 1.0f, 100.06f};
        byte expectedByte;
        for (int i = 0; i < arguments.length; i++) {
            expectedByte = jtt.bytecode.BC_f2b.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("char", "float", Float.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (((Integer) registerValues[0]).byteValue()) == expectedByte : ("Failed incorrect value " + (((Integer) registerValues[0]).byteValue()) + " " + expectedByte);
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_b2f() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.SIMULATE_ADAPTER = true;
        initTests();
        String klassName = getKlassName("jtt.bytecode.BC_b2f");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        CompilationBroker.SIMULATE_ADAPTER = true;
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_b2f.java", "float test(byte)");
        byte[] arguments = { -100, 0, 100};
        float expectedFloat;
        for (int i = 0; i < arguments.length; i++) {
            expectedFloat = jtt.bytecode.BC_b2f.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "signed char", Byte.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (((Float) registerValues[33]).floatValue()) == expectedFloat : ("Failed incorrect value " + (((Float) registerValues[33]).floatValue()) + " " + expectedFloat);
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_d2f() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_d2f");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods);
        double[] arguments = { -2.2d, 0.0d, 1.0d, 01.06d};
        float expectedFloat = -9;
        for (int i = 0; i < arguments.length; i++) {
            expectedFloat = jtt.bytecode.BC_d2f.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "double", Double.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Float) registerValues[33]).floatValue() == expectedFloat : ("Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + expectedFloat);
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_d2i01() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_d2i01");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods, "BC_d2i01.java", "int test(double)");
        double[] arguments = { 0.0d, 1.0d, 01.06d, -2.2d};
        int expectedInt = -9;
        for (int i = 0; i < arguments.length; i++) {
            expectedInt = jtt.bytecode.BC_d2i01.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).intValue() == expectedInt : "Failed incorrect value " + ((Integer) registerValues[0]).intValue() + " " + expectedInt;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_d2i02() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, i);
            masm.push(ConditionFlag.Always, 1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifge_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            String functionPrototype = ARMCodeWriter.preAmble("double ", "int", Integer.toString(i));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, 0, masm.codeBuffer.copyData(0, masm.codeBuffer.position()), expectedValues, testvalues, bitmasks);
            assert ((Double) registerValues[17]).doubleValue() == answer : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void broken_jtt_BC_d2i02() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_d2i02");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods, "BC_d2i02.java", "int test(int)");
        for (int i = 0; i < 5; i++) {
            int answer = jtt.bytecode.BC_d2i02.test(i);
            String functionPrototype = ARMCodeWriter.preAmble("double ", "int", Integer.toString(i));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).intValue() == answer : ("Failed incorrect value " + ((Integer) registerValues[0]).intValue() + "  " + answer);
            theCompiler.cleanup();
        }
    }

    public void test_BC_anewarray() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_anewarray");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods, "BC_anewarray.java", "int test(int)");
        int[] arguments = { 0, 1};
        for (int i = 0; i < arguments.length; i++) {
            int answer = jtt.bytecode.BC_anewarray.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(arguments[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_BC_new() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_new");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods, "BC_new.java", "int test(int)");
        int[] arguments = { 0, 1};
        for (int i = 0; i < arguments.length; i++) {
            int answer = jtt.bytecode.BC_new.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(arguments[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_f2i01() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_f2i01");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods);
        float[] arguments = { 0.0f/* , 0.0f, 1.0f, 1.06f */};
        for (int i = 0; i < arguments.length; i++) {
            int answer = jtt.bytecode.BC_f2i01.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(arguments[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void broken_jtt_BC_f2i02() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_f2i02");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods);
        for (int i = 0; i < 5; i++) {
            float answer = jtt.bytecode.BC_f2i02.test(i);
            String functionPrototype = ARMCodeWriter.preAmble("float", "int", Integer.toString(i));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).intValue() == answer : "Failed incorrect value " + ((Float) registerValues[33]).floatValue() + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_i2d() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_i2d");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods);
        int[] arguments = { -2, 0, 1, 2, 99};
        for (int i = 0; i < arguments.length; i++) {
            double answer = jtt.bytecode.BC_i2d.test(arguments[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "int", Integer.toString(arguments[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Double) registerValues[17]).doubleValue() == answer : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_ifge_2() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        int argOne[] = { 0, 1, 6, 7};
        int argTwo[] = { 2, -2, 375, 50};
        String klassName = getKlassName("jtt.bytecode.BC_ifge_2");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_ifge_2.java", "boolean test(int, int)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_ifge_2.test(argOne[i], argTwo[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int", Integer.toString(argOne[i]) + new String(", ") + Integer.toString(argTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).intValue() == expectedValue : "Failed incorrect value " + ((Integer) registerValues[0]).intValue() + "   " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp01() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        double argOne[] = { 5.0d, -3.1d, 5.0d, -5.0d, 0d, -0.1d, -5.0d, 25.5d, 0.5d};
        double argTwo[] = { 78.00d, 78.01d, 3.3d, -7.2d, 78.00d, 78.001d, -3.2d, 25.5d, 1.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp01");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dcmp01.java", "boolean test(double, double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp01.test(argOne[i], argTwo[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double , double", Double.toString(argOne[i]) + new String(", ") + Double.toString(argTwo[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + "   " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp02() throws Exception {
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp02");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dcmp02.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp02.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_MSB32() throws Exception {
        long[] input = new long[] { 1l, 2l, 0l, -1l, 61440l, 2147483648l};
        int[] output = new int[] { 0, 1, -1, 31, 15, 31};

        String klassName = getKlassName("jtt.max.MostSignificantBit32");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "MostSignificantBit32.java", "int test(long)");
        for (int i = 0; i < input.length; i++) {
            @SuppressWarnings("unused")
            int answer = jtt.max.MostSignificantBit32.test(input[i]);
            int expectedValue = output[i];
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long", Long.toString(input[i]) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_LSB32() throws Exception {
        long[] input = new long[] { 1l, 2l, 0l, -1l, 61440l};
        int[] output = new int[] { 0, 1, -1, 0, 12};
        String klassName = getKlassName("jtt.max.LeastSignificantBit32");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "LeastSignificantBit32.java", "int test(long)");
        for (int i = 0; i < input.length; i++) {
            @SuppressWarnings("unused")
            int answer = jtt.max.LeastSignificantBit64.test(input[i]);
            int expectedValue = output[i];
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long", Long.toString(input[i]) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp03() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp03");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dcmp03.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp03.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp04() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp04");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dcmp04.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp04.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp05() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp05");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dcmp05.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp05.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp06() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp06");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dcmp06.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp06.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp07() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp07");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dcmp07.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp07.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp08() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp08");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dcmp08.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp08.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp09() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        double argOne[] = { -1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp09");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dcmp09.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_dcmp09.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp10() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_dcmp10");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dcmp10.java", "boolean test(int)");
        for (int i = 0; i < 9; i++) {
            boolean answer = jtt.bytecode.BC_dcmp10.test(i);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(i));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp01() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        float argOne[] = { 5.0f, -3.0f, 5.0f, -5.0f, 0f, -0.1f, 0.75f};
        float argTwo[] = { 78.00f, 78.01f, 3.3f, -7.2f, 78.00f, 78.001f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp01");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fcmp01.java", "boolean test(float, float)");
        for (int i = 0; i < argOne.length; i++) {
            if (i == argOne.length - 1) {
                argOne[i] = Float.parseFloat("NaN");
            }
            boolean answer = jtt.bytecode.BC_fcmp01.test(argOne[i], argTwo[i]);
            int expectedValue = answer ? 1 : 0;
            String tmp = null;
            if (Float.isNaN(argOne[i])) {
                tmp = new String("0.0f/0.0f");
            } else {
                tmp = Float.toString(argOne[i]);
            }
            String functionPrototype = ARMCodeWriter.preAmble("int", "float, float", tmp + new String(",") + Float.toString(argTwo[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp02() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp02");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fcmp02.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp02.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp03() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp03");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fcmp03.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp03.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp04() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp04");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fcmp04.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp04.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp05() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp05");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fcmp05.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp05.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp06() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp06");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fcmp06.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp06.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp07() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp07");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fcmp07.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp07.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp08() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp08");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fcmp08.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp08.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp09() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        float argOne[] = { -1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp09");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fcmp09.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = jtt.bytecode.BC_fcmp09.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp10() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_fcmp10");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fcmp10.java", "boolean test(int)");
        for (int i = 0; i < 9; i++) {
            boolean answer = jtt.bytecode.BC_fcmp10.test(i);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "int", Integer.toString(i));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_fmul() throws Exception {
        initTests();
        float argsOne[] = { 311.0f, 2f, -2.5f};
        float argsTwo[] = { 10f, 20.1f, -6.01f};
        String klassName = getKlassName("jtt.bytecode.BC_fmul");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods);
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fmul.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[33] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_fadd() throws Exception {
        initTests();
        float argsOne[] = { 311.0f, 2f, -2.5f, 0.0f, 1.0f};
        float argsTwo[] = { 10f, 20.1f, -6.01f, 0.0f, 1.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fadd");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods);
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fadd.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[33] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_fsub() throws Exception {
        initTests();
        float argsOne[] = { 311.0f, 2f, -2.5f};
        float argsTwo[] = { 10f, 20.1f, -6.01f};
        String klassName = getKlassName("jtt.bytecode.BC_fsub");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods);
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fsub.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[33] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_fdiv() throws Exception {
        initTests();
        float argsOne[] = { 311.0f, 2f};
        float argsTwo[] = { 10f, 20.1f};
        String klassName = getKlassName("jtt.bytecode.BC_fdiv");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods);
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fdiv.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_getstatic_i() throws Exception {
        initTests();
        int argsOne[] = { 0, -1, 2, 4};
        String klassName = getKlassName("jtt.bytecode.BC_getstatic_i");
        vm().compilationBroker.setOffline(true);
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_getstatic_i.java", "int test(int)");
        for (int i = 0; i < argsOne.length; i++) {
            int intValue = BC_getstatic_i.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int  ", Integer.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).intValue() == intValue : "Failed incorrect value r0 " + ((Integer) registerValues[0]).intValue() + " " + intValue;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_getstatic_i() throws Exception {
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
            masm.mov32BitConstant(ConditionFlag.Always, ARMV7.r0, argsOne[i]);
            masm.push(ConditionFlag.Always, 1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(ConditionFlag.Always, 1);
            int[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            Log.println("T1X getstatic_i expected " + answer + " got " + registerValues[0]);
            assert registerValues[0] == answer : ("Failed incorrect value " + registerValues[0] + " " + answer);
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_frem() throws Exception {
        initTests();
        float argsOne[] = { 311.0f, 2f};
        float argsTwo[] = { 10f, 20.1f};
        String klassName = getKlassName("jtt.bytecode.BC_frem");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_frem.java", "float test(float, float)");
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_frem.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_irem() throws Exception {
        initTests();
        int argsOne[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int argsTwo[] = { 2, 2, 2, 3, 3, 3, 3, 3, 3, 3};
        String klassName = getKlassName("jtt.bytecode.BC_irem");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods);

        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_irem.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Integer) registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void broken_jtt_BC_drem() throws Exception {
        initTests();
        double argsOne[] = { 311.0D, 2D};
        double argsTwo[] = { 10D, 20.1D};
        String klassName = getKlassName("jtt.bytecode.BC_drem");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods);
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_drem.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_ddiv() throws Exception {
        initTests();
        double argsOne[] = { 311.0D, 2D, -10.0D};
        double argsTwo[] = { 10D, 20.1D, 5.0D};
        String klassName = getKlassName("jtt.bytecode.BC_ddiv");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods);
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_ddiv.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_FLOATDIV_jtt_BC_ldiv() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        initTests();
        CompilationBroker.SIMULATE_ADAPTER = true;
        String klassName = getKlassName("jtt.bytecode.BC_ldiv");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(31L, 67L));
        pairs.add(new Args(6L, 4L));
        pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(Long.MAX_VALUE, 0));
        pairs.add(new Args(0, Long.MAX_VALUE));
        pairs.add(new Args(0, Long.MIN_VALUE));
        pairs.add(new Args(Long.MIN_VALUE, 0));
        pairs.add(new Args(Long.MAX_VALUE, 0));
        pairs.add(new Args(0, Long.MAX_VALUE - 20L));
        pairs.add(new Args(0, Long.MIN_VALUE + 20L));
        pairs.add(new Args(Long.MIN_VALUE, 20L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 0xdeadd0d0deadd0d0L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 0xd0d0d0d0d0d0d0d0L));
        initialiseCodeBuffers(methods, "BC_ldiv.java", "long test(long, long)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_ldiv.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_FLOATIDIV_jtt_BC_div() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.SIMULATE_ADAPTER = true;
        initTests();
        int argsOne[] = { 1, 2, 256, 135, -2147483648, -2147483648};
        int argsTwo[] = { 2, -1, 4, 7, -1, 1};
        String klassName = getKlassName("jtt.bytecode.BC_idiv");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        CompilationBroker.SIMULATE_ADAPTER = false;
        initialiseCodeBuffers(methods, "BC_idiv.java", "int test(int, int)");
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_idiv.test(argsOne[i], argsTwo[i]);
            System.out.println("FLOAT" + expectedValue);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues != null : "Failed to return array of register values";
            assert (Integer) registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_iadd3() throws Exception {
        initTests();
        short argsOne[] = { 1, 0, 33, 1, -128, 127, -32768, 32767};
        short argsTwo[] = { 2, -1, 67, -1, 1, 1, 1, 1};
        String klassName = getKlassName("jtt.bytecode.BC_iadd");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_iadd3.java", "int test(short, short)");
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd3.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "short , short ", Short.toString(argsOne[i]) + "," + Short.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Integer) registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_i2s() throws Exception {
        initTests();
        int argsOne[] = { 1, -1, 34, 1, 65535, 32768, -32768};
        String klassName = getKlassName("jtt.bytecode.BC_i2s");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_i2s.java", "short test(int)");
        short expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_i2s.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("short", "int  ", Integer.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).shortValue() == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_iadd() throws Exception {
        initTests();
        int argsOne[] = { 1, 0, 33, 1, -2147483648, -2147483647, 2147483647, 4080};
        int argsTwo[] = { 2, -1, 67, -1, 1, -2, 1, 134217728};
        String klassName = getKlassName("jtt.bytecode.BC_iadd");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_iadd.java", "int test(int, int)");
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Integer) registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_ladd() throws Exception {
        initTests();
        int argsOne[] = { 1, 0, 33, 1, -2147483648, -2147483647, 2147483647, 4080};
        int argsTwo[] = { 2, -1, 67, -1, 1, -2, 1, 134217728};
        String klassName = getKlassName("jtt.bytecode.BC_iadd");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_iadd.java", "int test(int, int)");

        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Integer) registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_iadd2() throws Exception {
        initTests();
        byte argsOne[] = { 1, 0, 33, 1, -128, 127};
        byte argsTwo[] = { 2, -1, 67, -1, 1, 1};
        String klassName = getKlassName("jtt.bytecode.BC_iadd");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_iadd2.java", "int test(byte, byte)");

        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd2.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("int", "int , int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            Log.println("IADD2 test " + i + " returned " + ((Integer) registerValues[0]).intValue() + " expected " + expectedValue);
            assert (Integer) registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_fload() throws Exception {
        initTests();
        float argsOne[] = { 0.0f, 1.1f, -1.4f, 256.33f, 1000.001f};
        String klassName = getKlassName("jtt.bytecode.BC_fload");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fload.java", "float test(float)");

        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fload.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", " float ", Float.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_fload2() throws Exception {
        initTests();
        float argsOne[] = { 0.0f, 1.1f, -1.4f, 256.33f, 1000.001f};
        float argsTwo[] = { 17.1f, 2.5f, 45.32f, -44.5f, -990.9f};
        String klassName = getKlassName("jtt.bytecode.BC_fload_2");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fload_2.java", "float test(float, float)");

        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fload_2.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", " float , float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_freturn() throws Exception {
        initTests();
        float argsOne[] = { 0.0f, 1.1f, -1.4f, 256.33f, 1000.001f};
        String klassName = getKlassName("jtt.bytecode.BC_freturn");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods);

        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_freturn.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", " float ", Float.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_dreturn() throws Exception {
        initTests();
        double argsOne[] = { 0.0D, 1.1D, -1.4d, 256.33d, 1000.001d};
        String klassName = getKlassName("jtt.bytecode.BC_dreturn");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods);

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dreturn.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", " double ", Double.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_dmul() throws Exception {
        initTests();
        double argsOne[] = { 311.0D, 11.2D};
        double argsTwo[] = { 10D, 2.0D};
        String klassName = getKlassName("jtt.bytecode.BC_dmul");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods);

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dmul.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_dsub() throws Exception {
        initTests();
        double argsOne[] = { 0.0D, 1.0D, 253.11d, 0.0D};
        double argsTwo[] = { 0.0D, 1.0D, 54.43d, 1.0D};
        String klassName = getKlassName("jtt.bytecode.BC_dsub");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dsub.java", "double test(double, double)");

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dsub.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_dsub2() throws Exception {
        initTests();
        double argsOne[] = { 1.0D, 2.0d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dsub2");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dsub2.java", "double test(double)");
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dsub2.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double ", Double.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_fneg() throws Exception {
        initTests();
        float argsOne[] = { 0.0f, -1.01f, 7263.8734f, 0.0f, 7263.8743f};
        String klassName = getKlassName("jtt.bytecode.BC_fneg");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fneg.java", "float test(float)");
        assert (entryPoint != -1);

        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = jtt.bytecode.BC_fneg.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("float", "float ", Float.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Float) registerValues[33] == floatValue : "Failed incorrect value " + registerValues[0] + " " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dneg2() throws Exception {
        initTests();
        double argsOne[] = { 1.0d, -1.0d, -0.0D, 0.0d, -2.0d, 2.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dneg2");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dneg2.java", "double test(double)");
        assert (entryPoint != -1);

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dneg2.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", " double ", Double.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_XXdneg9() throws Exception {
        initTests();
        double argsOne[] = { 0.0D, -1.01D, 7263.8734d, 0.0d, -1.01d, 7263.8743d, 0.0d};
        double argsTwo[] = { 1.0d, -2.01D, 8263.8734d, 1.0d, -2.01d, 8263.8734d, 1.0d};
        int argsThree[] = { 0, 0, 0, 1, 1, 1, 0};
        String klassName = getKlassName("jtt.bytecode.BC_dneg");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dneg.java", "double test(double, double, int)");
        assert (entryPoint != -1);

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dneg.test(argsOne[i], argsTwo[i], argsThree[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double, int", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]) + "," + Integer.toString(argsThree[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lload_0() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lload_0");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 1L));
        pairs.add(new Args(-3L, -3L));
        pairs.add(new Args(10000L, 10000L));
        pairs.add(new Args(549755814017L, 549755814017L));
        initialiseCodeBuffers(methods, "BC_lload_0.java", "long test(long)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lload_0.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lload_4() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lload_4");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 1));
        pairs.add(new Args(-3L, 1));
        pairs.add(new Args(10000L, 1));
        pairs.add(new Args(549755814017L, 1));
        initialiseCodeBuffers(methods, "BC_lload_4.java", "int test(long, int)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lload_4.test(pair.lfirst, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, int", Long.toString(pair.lfirst) + "," + Long.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lload_1() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lload_1");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1L));
        pairs.add(new Args(1, -3L));
        pairs.add(new Args(1, 10000L));
        initialiseCodeBuffers(methods, "BC_lload_1.java", "long test(int, long)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lload_1.test(pair.first, pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "int, long long", Integer.toString(pair.first) + "," + Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " r2 " + registerValues[2] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lload_2() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lload_2");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1, 1L));
        pairs.add(new Args(1, 1, -3L));
        pairs.add(new Args(1, 1, 10000L));
        initialiseCodeBuffers(methods, "BC_lload_2.java", "long test(int, int, long)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lload_2.test(pair.first, pair.second, pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "int, int, long long",
                            Integer.toString(pair.first) + "," + Integer.toString(pair.second) + "," + Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lload_3() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lload_3");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1, 1, 1L));
        pairs.add(new Args(1, 1, 1, -3L));
        pairs.add(new Args(1, 1, 1, 10000L));
        initialiseCodeBuffers(methods, "BC_lload_3.java", "long test(int, int, int, long)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lload_3.test(pair.first, pair.second, pair.third, pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "int, int, int, long long",
                            Integer.toString(pair.first) + "," + Integer.toString(pair.second) + "," + Integer.toString(pair.third) + "," + Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_ladd() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_ladd");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
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

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_ladd.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL ," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lor() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lor");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(31L, 67L));
        pairs.add(new Args(6L, 4L));
        pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(Long.MAX_VALUE, 0));
        pairs.add(new Args(0, Long.MAX_VALUE));
        pairs.add(new Args(0, Long.MIN_VALUE));
        pairs.add(new Args(Long.MIN_VALUE, 0));
        pairs.add(new Args(Long.MAX_VALUE, 0));
        pairs.add(new Args(0, Long.MAX_VALUE - 20L));
        pairs.add(new Args(0, Long.MIN_VALUE + 20L));
        pairs.add(new Args(Long.MIN_VALUE, 20L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 0xdeadd0d0deadd0d0L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 0xd0d0d0d0d0d0d0d0L));
        initialiseCodeBuffers(methods, "BC_lor.java", "long test(long, long)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lor.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lxor() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lxor");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(31L, 63L));
        pairs.add(new Args(6L, 4L));
        pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 0xdeadd0d0deadd0d0L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 0xd0d0d0d0d0d0d0d0L));
        initialiseCodeBuffers(methods, "BC_lxor.java", "long test(long, long)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lxor.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "," + Long.toString(pair.lsecond));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_land() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_land");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(31L, 63L));
        pairs.add(new Args(6L, 4L));
        pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 45));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 0xdeadd0d0deadd0d0L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 0xd0d0d0d0d0d0d0d0L));
        initialiseCodeBuffers(methods, "BC_land.java", "long test(long, long)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_land.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL ," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_land_const() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_land_const");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(31, 31));
        pairs.add(new Args(6, 6));
        initialiseCodeBuffers(methods, "BC_land_const.java", "long test(int)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_land_const.test(pair.first);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "int", Long.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lshl() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lshl");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
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
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lshl.test(pair.lfirst, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, int", Long.toString(pair.lfirst) + "LL," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lshr() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lshr");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
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
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lshr.test(pair.lfirst, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, int", Long.toString(pair.lfirst) + "LL," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_loop01() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.SIMULATE_ADAPTER = true;
        String klassName = getKlassName("jtt.loop.Loop01");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        CompilationBroker.SIMULATE_ADAPTER = false;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(62, 2));
        initialiseCodeBuffers(methods, "Loop01.java", "boolean test(int)");

        for (Args pair : pairs) {
            boolean answer = jtt.loop.Loop01.test(pair.first);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_charComp() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.SIMULATE_ADAPTER = true;
        String klassName = getKlassName("jtt.bytecode.BC_charComp");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        CompilationBroker.SIMULATE_ADAPTER = false;
        char argOne = 'c';
        char argTwo[] = { 'a', 'c', 'd'};
        initialiseCodeBuffers(methods, "BC_charComp.java", "boolean test(int, char, char)");

        for (int j = 0; j < argTwo.length; j++)
            for (int i = -2; i < 4; i++) {
                boolean answer = jtt.bytecode.BC_charcomp.test(i, argOne, argTwo[j]);
                int expectedValue = answer ? 1 : 0;
                String functionPrototype = ARMCodeWriter.preAmble("int ", " int, char, char",
                                Integer.toString(i) + ", " + "'" + Character.toString(argOne) + "'" + ", " + "'" + Character.toString(argTwo[j]) + "'");
                int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
                int returnValue = registerValues[0];
                assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
                theCompiler.cleanup();
            }
    }

    public void test_jtt_loop02() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.SIMULATE_ADAPTER = true;
        String klassName = getKlassName("jtt.loop.Loop02");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        CompilationBroker.SIMULATE_ADAPTER = false;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(62, 2));
        initialiseCodeBuffers(methods, "Loop02.java", "boolean test(int)");

        for (Args pair : pairs) {
            boolean answer = jtt.loop.Loop02.test(pair.first);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_loop03() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.loop.Loop03");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(62, 2));
        pairs.add(new Args(10, 2));
        initialiseCodeBuffers(methods, "Loop03.java", "int test(int)");

        for (Args pair : pairs) {
            int expectedValue = jtt.loop.Loop03.test(pair.first);
            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_loop04() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.loop.Loop04");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(10, 2));
        initialiseCodeBuffers(methods, "Loop04.java", "int test(int)");

        for (Args pair : pairs) {
            int expectedValue = jtt.loop.Loop04.test(pair.first);
            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_loop11() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.loop.Loop11");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(5, 2));
        initialiseCodeBuffers(methods, "Loop11.java", "int test(int)");

        for (Args pair : pairs) {
            int expectedValue = jtt.loop.Loop11.test(pair.first);
            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_loopPHI() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.loop.LoopPhi");
        CompilationBroker.SIMULATE_ADAPTER = true;
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        CompilationBroker.SIMULATE_ADAPTER = false;
        List<Args> pairs = new LinkedList<Args>();
        // pairs.add(new Args(5000, 2));
        // pairs.add(new Args(0, 0));
        // pairs.add(new Args(1, -1));
        // pairs.add(new Args(2, 2));
        // pairs.add(new Args(3, 2));
        // pairs.add(new Args(4, 2));
        pairs.add(new Args(5, 2));
        // pairs.add(new Args(6, 2));
        // pairs.add(new Args(7, 2));
        // pairs.add(new Args(8, 2));
        // pairs.add(new Args(9, 2));
        // pairs.add(new Args(10, 2));
        initialiseCodeBuffers(methods, "LoopPhi.java", "int test(int)");

        for (Args pair : pairs) {
            int expectedValue = jtt.loop.LoopPhi.test(pair.first);
            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_irem2() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.SIMULATE_ADAPTER = true;
        String klassName = getKlassName("jtt.bytecode.BC_irem");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        CompilationBroker.SIMULATE_ADAPTER = false;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(2, -1));
        pairs.add(new Args(256, 4));
        pairs.add(new Args(135, 7));
        pairs.add(new Args(Integer.MIN_VALUE, -1));
        pairs.add(new Args(-1, 1));
        pairs.add(new Args(0, 1));
        pairs.add(new Args(1000, 1));
        pairs.add(new Args(Integer.MIN_VALUE, 1));
        pairs.add(new Args(Integer.MAX_VALUE, 1));
        initialiseCodeBuffers(methods, "BC_irem.java", "int test(int, int)");

        for (Args pair : pairs) {
            int expectedValue = jtt.bytecode.BC_irem.test(pair.first, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("int ", " int, int ", Integer.toString(pair.first) + ", " + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_loopInline() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.SIMULATE_ADAPTER = true;
        String klassName = getKlassName("jtt.loop.LoopInline");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        CompilationBroker.SIMULATE_ADAPTER = false;
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(3, 2));
        pairs.add(new Args(10, 2));
        initialiseCodeBuffers(methods, "LoopInline.java", "int test(int)");

        for (Args pair : pairs) {
            int expectedValue = jtt.loop.LoopInline.test(pair.first);
            String functionPrototype = ARMCodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    private static final BigInteger TWO_64 = BigInteger.ONE.shiftLeft(64);

    public String asUnsignedDecimalString(long l) {
        BigInteger b = BigInteger.valueOf(l);
        if (b.signum() < 0) {
            b = b.add(TWO_64);
        }
        return b.toString();
    }

    @SuppressWarnings("cast")
    public long connectRegs(int reg0, int reg1) {
        long returnVal = 0;
        if (reg1 < 0) {
            returnVal = ((long) reg1) << 32;
            if (reg0 < 0) {
                returnVal += 1L << 31;
                returnVal += ((long) reg0) + (1L << 31);
            } else {
                returnVal += reg0;
            }
        } else {
            returnVal = ((long) reg1) << 32;
            if (reg0 < 0) {
                returnVal += 1L << 31;
                returnVal += ((long) reg0) + (1L << 31);
            } else {
                returnVal += reg0;
            }
        }
        return returnVal;
    }

    @SuppressWarnings("cast")
    public void test_jtt_BC_long_tests() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2));
        pairs.add(new Args(-1L, 1));
        pairs.add(new Args(0L, 1));
        pairs.add(new Args((long) Integer.MAX_VALUE, 45));
        pairs.add(new Args((long) Integer.MAX_VALUE + 5L, 45));
        pairs.add(new Args(Long.MAX_VALUE, 45));
        pairs.add(new Args((long) Integer.MIN_VALUE, 16));
        pairs.add(new Args((long) Integer.MIN_VALUE - 5L, 16));
        pairs.add(new Args((long) Integer.MAX_VALUE, 45));
        pairs.add(new Args(Long.MIN_VALUE, 16));
        pairs.add(new Args(Long.MIN_VALUE + 5L, 16));
        pairs.add(new Args(Long.MAX_VALUE, 45));
        pairs.add(new Args(Long.MAX_VALUE - 5L, 45));
        initialiseCodeBuffers(methods, "BC_long_tests.java", "long test(long)");

        for (Args pair : pairs) {
            long expectedValue = BC_long_tests.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_long_le() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));
        initialiseCodeBuffers(methods, "BC_long_tests.java", "boolean le(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue = BC_long_tests.le(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            boolean returnValue = (registerValues[0] == 1 ? true : false);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_long_ge() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 1L));
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        initialiseCodeBuffers(methods, "BC_long_tests.java", "boolean ge(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue = BC_long_tests.ge(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            boolean returnValue = (registerValues[0] == 1 ? true : false);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_long_eq() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));
        initialiseCodeBuffers(methods, "BC_long_tests.java", "boolean eq(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue = BC_long_tests.eq(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            boolean returnValue = (registerValues[0] == 1 ? true : false);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_long_ne() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));
        initialiseCodeBuffers(methods, "BC_long_tests.java", "boolean ne(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue = BC_long_tests.ne(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            boolean returnValue = (registerValues[0] == 1 ? true : false);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_long_gt() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));
        initialiseCodeBuffers(methods, "BC_long_tests.java", "boolean gt(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue = BC_long_tests.gt(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            boolean returnValue = (registerValues[0] == 1 ? true : false);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_long_lt() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));
        initialiseCodeBuffers(methods, "BC_long_tests.java", "boolean lt(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue = BC_long_tests.lt(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            boolean returnValue = (registerValues[0] == 1 ? true : false);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lushr() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lushr");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2));
        pairs.add(new Args(67L, 2));
        pairs.add(new Args(31L, 1));
        pairs.add(new Args(6L, 4));
        pairs.add(new Args(-2147483648L, 16));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 45));
        initialiseCodeBuffers(methods, "BC_lushr.java", "long test(long, int)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lushr.test(pair.lfirst, pair.second);
            String functionPrototype = ARMCodeWriter.preAmble("unsigned long long", "unsigned long long, int", asUnsignedDecimalString(pair.lfirst) + "," + Integer.toString(pair.second));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lcmp() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lcmp");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(77L, 78L));
        pairs.add(new Args(-1L, 0L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        initialiseCodeBuffers(methods, "BC_lcmp.java", "boolean test(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue = jtt.bytecode.BC_lcmp.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "," + Long.toString(pair.lsecond));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            boolean returnValue = registerValues[0] > 0 ? true : false;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lmul() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lmul");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
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

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lmul.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL ," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lneg() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lneg");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(7263L, -7263L));
        pairs.add(new Args(-2147483648L, 2147483648L));
        initialiseCodeBuffers(methods, "BC_lneg.java", "long test(long)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lneg.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lreturn() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lreturn");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args(1L, 1L));
        pairs.add(new Args(-1L, -1L));
        pairs.add(new Args(256L, 256L));
        pairs.add(new Args(1000000000000L, 1000000000000L));
        initialiseCodeBuffers(methods, "BC_lreturn.java", "long test(long)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lreturn.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_lsub() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_lsub");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
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
        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_lsub.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL ," + Long.toString(pair.lsecond) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_i2l() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_i2l");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(Integer.MIN_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(1, 1L));
        pairs.add(new Args(2, 2L));
        pairs.add(new Args(3, 3L));
        pairs.add(new Args(-1, -1L));
        pairs.add(new Args(Integer.MIN_VALUE / 2, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Integer.MAX_VALUE, (long) Integer.MAX_VALUE));
        initialiseCodeBuffers(methods, "BC_i2l.java", "long test(int)");

        for (Args pair : pairs) {
            long expectedValue = jtt.bytecode.BC_i2l.test(pair.first);
            String functionPrototype = ARMCodeWriter.preAmble("long long", "int", Integer.toString(pair.first));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            long returnValue = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == Math.abs(expectedValue) : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_l2i() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_l2i");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 1));
        pairs.add(new Args(2L, 2));
        pairs.add(new Args(3L, 3));
        pairs.add(new Args(-1L, -1));
        pairs.add(new Args(134217728L, 134217728));
        pairs.add(new Args(-2147483647L, -2147483647));
        pairs.add(new Args(-2147483648L, -2147483648));
        pairs.add(new Args(2147483647L, 2147483647));
        pairs.add(new Args(Long.MAX_VALUE, Integer.MAX_VALUE));
        pairs.add(new Args(Long.MIN_VALUE, Integer.MIN_VALUE));
        pairs.add(new Args(0, 0));
        initialiseCodeBuffers(methods, "BC_l2i.java", "int test(long)");

        for (Args pair : pairs) {
            int expectedValue = jtt.bytecode.BC_l2i.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("int", "long long", Long.toString(pair.lfirst) + "LL");
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            int returnValue = registerValues[0];
            assert Math.abs(returnValue) == Math.abs(expectedValue) : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    @SuppressWarnings("cast")
    public void broken_jtt_BC_l2f() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_l2f");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, 0.0f));
        pairs.add(new Args(1L, 1.0f));
        pairs.add(new Args((long) Integer.MAX_VALUE, (float) Integer.MAX_VALUE));
        pairs.add(new Args((long) Long.MAX_VALUE, (float) Long.MAX_VALUE));
        pairs.add(new Args(-74652389L, -74652389.00f));
        pairs.add(new Args((long) Integer.MIN_VALUE, (float) Integer.MIN_VALUE));
        pairs.add(new Args((long) Long.MIN_VALUE, (float) Long.MIN_VALUE));
        initialiseCodeBuffers(methods, "BC_l2f.java", "float test(long)");

        for (Args pair : pairs) {
            float expectedValue = jtt.bytecode.BC_l2f.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("float", "long long", Long.toString(pair.lfirst));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            float returnValue = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    @SuppressWarnings("cast")
    public void broken_jtt_BC_l2d() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("jtt.bytecode.BC_l2d");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, 0.0f));
        pairs.add(new Args(1L, 1.0f));
        pairs.add(new Args((long) Integer.MAX_VALUE, (float) Integer.MAX_VALUE));
        pairs.add(new Args((long) Long.MAX_VALUE, (float) Long.MAX_VALUE));
        pairs.add(new Args(-74652389L, -74652389.00f));
        pairs.add(new Args((long) Integer.MIN_VALUE, (float) Integer.MIN_VALUE));
        pairs.add(new Args((long) Long.MIN_VALUE, (float) Long.MIN_VALUE));
        initialiseCodeBuffers(methods, "BC_l2d.java", "double test(long)");

        for (Args pair : pairs) {
            double expectedValue = jtt.bytecode.BC_l2d.test(pair.lfirst);
            String functionPrototype = ARMCodeWriter.preAmble("double", "long long", Long.toString(pair.lfirst) + "LL");
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            double returnValue = ((Double) registerValues[17]).doubleValue();
            assert returnValue == expectedValue : "Failed incorrect value d0 " + ((Double) registerValues[17]).doubleValue() + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_generic_compilation() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        String klassName = getKlassName("sun.misc.FloatingDecimal.developLongDigits");
        // List<TargetMethod> methods = Compile.compileMethod(new String[] { klassName}, "C1X", "patchReturnAddress");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        initialiseCodeBuffers(methods, "MaxineVM.java", "int run(Pointer, int," + " Pointer, Word, Word, Word, Pointer, Pointer, Pointer, Pointer, int, Pointer)");
        vm().compilationBroker.setOffline(true);
        theCompiler.cleanup();
    }

    public void test_jtt_BC_fload_5() throws Exception {
        initTests();
        float argsOne[] = { 0.0f, 1.1f};
        float argsTwo[] = { 17.1f, 2.5f};
        float argsThree[] = { 0.0f, 1.1f};
        float argsFour[] = { 17.1f, 2.5f};
        float argsFive[] = { 0.0f, 1.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fload_5");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fload_5.java", "boolean test(float, float, float, float, float)");

        for (int i = 0; i < argsOne.length; i++) {
            boolean rt = jtt.bytecode.BC_fload_5.test(argsOne[i], argsTwo[i], argsThree[i], argsFour[i], argsFive[i]);
            int expectedValue = rt ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "float, float, float, float, float",
                            Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]) + "," + Float.toString(argsThree[i]) + "," + Float.toString(argsFour[i]) + "," + Float.toString(argsFive[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).intValue() == expectedValue : "Failed incorrect value " + ((Integer) registerValues[0]).intValue() + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_dload_9() throws Exception {
        initTests();
        double argsOne[] = { 0.0D, 1.1D};
        double argsTwo[] = { 17.1D, 2.5D};
        double argsThree[] = { 0.0D, 1.1D};
        double argsFour[] = { 17.1D, 2.5D};
        double argsFive[] = { 17.1D, 2.5D};
        double argsSix[] = { 17.1D, 2.5D};
        double argsSeven[] = { 17.1D, 2.5D};
        double argsEight[] = { 17.1D, 2.5D};
        double argsNine[] = { 0.0D, 1.0D};
        String klassName = getKlassName("jtt.bytecode.BC_dload_9");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dload_9.java", "boolean test(double, double, double, double, double, double, double, double, double)");

        for (int i = 0; i < argsOne.length; i++) {
            boolean rt = jtt.bytecode.BC_dload_9.test(argsOne[i], argsTwo[i], argsThree[i], argsFour[i], argsFive[i], argsSix[i], argsSeven[i], argsEight[i], argsNine[i]);
            int expectedValue = rt ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double, double, double, double, double, double, double, double, double",
                            Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]) + "," + Double.toString(argsThree[i]) + "," + Double.toString(argsFour[i]) + "," +
                                            Double.toString(argsFive[i]) + "," + Double.toString(argsSix[i]) + "," + Double.toString(argsSeven[i]) + "," + Double.toString(argsEight[i]) + "," +
                                            Double.toString(argsNine[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).intValue() == expectedValue : "Failed incorrect value " + ((Integer) registerValues[0]).intValue() + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_dload_10() throws Exception {
        initTests();
        double argsOne[] = { 0.0D, 1.1D};
        double argsTwo[] = { 17.1D, 2.5D};
        double argsThree[] = { 0.0D, 1.1D};
        double argsFour[] = { 17.1D, 2.5D};
        double argsFive[] = { 17.1D, 2.5D};
        double argsSix[] = { 17.1D, 2.5D};
        double argsSeven[] = { 17.1D, 2.5D};
        double argsEight[] = { 17.1D, 2.5D};
        double argsNine[] = { 0.0D, 1.0D};
        float argsTen[] = { 0.0f, 1.0f};

        String klassName = getKlassName("jtt.bytecode.BC_dload_10");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dload_10.java", "boolean test(double, double, double, double, double, double, double, double, double, float)");

        for (int i = 0; i < argsOne.length; i++) {
            boolean rt = jtt.bytecode.BC_dload_10.test(argsOne[i], argsTwo[i], argsThree[i], argsFour[i], argsFive[i], argsSix[i], argsSeven[i], argsEight[i], argsNine[i], argsTen[i]);
            int expectedValue = rt ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double, double, double, double, double, double, double, double, double, float",
                            Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]) + "," + Double.toString(argsThree[i]) + "," + Double.toString(argsFour[i]) + "," +
                                            Double.toString(argsFive[i]) + "," + Double.toString(argsSix[i]) + "," + Double.toString(argsSeven[i]) + "," + Double.toString(argsEight[i]) + "," +
                                            Double.toString(argsNine[i]) + "," + Float.toString(argsTen[i]));

            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).intValue() == expectedValue : "Failed incorrect value " + ((Integer) registerValues[0]).intValue() + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_dload() throws Exception {
        initTests();
        double argsOne[] = { 0.0D, 1.1D, -1.4D, 256.33D, 1000.001D};
        String klassName = getKlassName("jtt.bytecode.BC_dload");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dload.java", "double test(double)");

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dload.test(argsOne[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", " double ", Double.toString(argsOne[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value got:" + ((Double) registerValues[16]).doubleValue() + " expected: " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_dload_2() throws Exception {
        initTests();
        double argsOne[] = { 0.0D, 1.1D, -1.4D, 256.33D, 1000.001D};
        double argsTwo[] = { 0.0D, 1.1D, -1.4D, 256.33D, 1000.001D};
        String klassName = getKlassName("jtt.bytecode.BC_dload_2");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_dload_2.java", "double test(double, double)");

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dload_2.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", " double, double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value got:" + ((Double) registerValues[17]).doubleValue() + " expected: " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_fdload() throws Exception {
        initTests();
        double argsOne[] = { Double.MAX_VALUE, Double.MIN_VALUE};
        float argsTwo[] = { 0.0f, 2.5f};
        double argsThree[] = { Double.MIN_VALUE, Double.MAX_VALUE};
        float argsFour[] = { 17.1f, 2.5f};
        float argsFive[] = { 0.0f, 1.0f};

        String klassName = getKlassName("jtt.bytecode.BC_fdload");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods, "BC_fdload.java", "boolean test(double, float, double, float, float)");

        for (int i = 0; i < argsOne.length; i++) {
            boolean rt = jtt.bytecode.BC_fdload.test(argsOne[i], argsTwo[i], argsThree[i], argsFour[i], argsFive[i]);
            int expectedValue = rt ? 1 : 0;
            String functionPrototype = ARMCodeWriter.preAmble("int", "double, float, double, float, float", Double.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]) + "," +
                            Double.toString(argsThree[i]) + "," + Float.toString(argsFour[i]) + "," + Float.toString(argsFive[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert ((Integer) registerValues[0]).intValue() == expectedValue : "Failed incorrect value " + ((Integer) registerValues[0]).intValue() + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_jtt_BC_dadd() throws Exception {
        initTests();
        double argsOne[] = { 0.0D, 1.0D, 253.11d};
        double argsTwo[] = { 0.0D, 1.0D, 54.43D};
        String klassName = getKlassName("jtt.bytecode.BC_dadd");
        List<TargetMethod> methods = Compile.compile(new String[] { klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initialiseCodeBuffers(methods);

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = jtt.bytecode.BC_dadd.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMCodeWriter.preAmble("double", "double , double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            Object[] registerValues = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (Double) registerValues[17] == doubleValue : "Failed incorrect value " + ((Double) registerValues[17]).doubleValue() + " " + doubleValue;
            theCompiler.cleanup();
        }
    }
}
