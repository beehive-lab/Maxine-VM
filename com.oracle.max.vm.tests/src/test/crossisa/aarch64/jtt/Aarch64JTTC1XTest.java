/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package test.crossisa.aarch64.jtt;

import com.oracle.max.asm.target.aarch64.Aarch64Assembler;
import com.oracle.max.vm.ext.c1x.C1X;
import com.oracle.max.vm.ext.maxri.Compile;
import com.oracle.max.vm.ext.t1x.T1X;
import com.oracle.max.vm.ext.t1x.aarch64.Aarch64T1XCompilation;
import com.sun.cri.ci.CiTarget;
import com.sun.max.ide.MaxTestCase;
import com.sun.max.io.Files;
import com.sun.max.program.option.OptionSet;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.actor.Actor;
import com.sun.max.vm.actor.member.StaticMethodActor;
import com.sun.max.vm.classfile.CodeAttribute;
import com.sun.max.vm.classfile.LineNumberTable;
import com.sun.max.vm.classfile.LocalVariableTable;
import com.sun.max.vm.classfile.constant.ConstantPool;
import com.sun.max.vm.compiler.CompilationBroker;
import com.sun.max.vm.compiler.RuntimeCompiler;
import com.sun.max.vm.compiler.target.TargetMethod;
import com.sun.max.vm.hosted.JavaPrototype;
import com.sun.max.vm.hosted.VMConfigurator;
import com.sun.max.vm.type.SignatureDescriptor;
import jtt.bytecode.*;
import test.crossisa.aarch64.asm.Aarch64CodeWriter;
import test.crossisa.aarch64.asm.MaxineAarch64Tester;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.sun.max.vm.MaxineVM.Phase;
import static com.sun.max.vm.MaxineVM.vm;

public class Aarch64JTTC1XTest extends MaxTestCase {

    private Aarch64Assembler asm;
    private CiTarget aarch64;
    private T1X t1x;
    private C1X c1x;
    private Aarch64CodeWriter code;
    private Aarch64T1XCompilation theCompiler;
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

        Args(int first, int second) {
            this.first = first;
            this.second = second;
        }

        Args(int first, int second, int third) {
            this(first, second);
            this.third = third;
        }

        Args(int first, int second, int third, int fourth) {
            this(first, second, third);
            this.fourth = fourth;
        }

        Args(long lfirst, long lsecond) {
            this.lfirst = lfirst;
            this.lsecond = lsecond;
        }

        Args(long lfirst, float fsecond) {
            this.lfirst = lfirst;
            this.fsecond = (long) fsecond;
        }

        Args(long lfirst, int second) {
            this.lfirst = lfirst;
            this.second = second;
        }

        Args(int first, long lfirst) {
            this.first = first;
            this.lfirst = lfirst;
        }

        Args(int first, int second, long lfirst) {
            this.first = first;
            this.second = second;
            this.lfirst = lfirst;
        }

        Args(int first, int second, int third, long lfirst) {
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

    private static int[] valueTestSet = {0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65535};
    private static long[] scratchTestSet = {0, 1, 0xff, 0xffff, 0xffffff, 0xfffffff, 0x00000000ffffffffL};
    // Checkstyle: stop
    private static MaxineAarch64Tester.BitsFlag[] bitmasks = {MaxineAarch64Tester.BitsFlag.All32Bits, MaxineAarch64Tester.BitsFlag.All32Bits, MaxineAarch64Tester.BitsFlag.All32Bits,
                    MaxineAarch64Tester.BitsFlag.All32Bits, MaxineAarch64Tester.BitsFlag.All32Bits, MaxineAarch64Tester.BitsFlag.All32Bits, MaxineAarch64Tester.BitsFlag.All32Bits,
                    MaxineAarch64Tester.BitsFlag.All32Bits, MaxineAarch64Tester.BitsFlag.All32Bits, MaxineAarch64Tester.BitsFlag.All32Bits, MaxineAarch64Tester.BitsFlag.All32Bits,
                    MaxineAarch64Tester.BitsFlag.All32Bits, MaxineAarch64Tester.BitsFlag.All32Bits, MaxineAarch64Tester.BitsFlag.All32Bits, MaxineAarch64Tester.BitsFlag.All32Bits,
                    MaxineAarch64Tester.BitsFlag.All32Bits, MaxineAarch64Tester.BitsFlag.All32Bits};
    // Checkstyle: start
    private static long[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static long[] expectedLongValues = {Long.MIN_VALUE, Long.MAX_VALUE};
    private static boolean[] testvalues = new boolean[17];

    private String getKlassName(String klass) {
        return "^" + klass + "^";
    }

    private long[] generateAndTestStubs(String functionPrototype, int entryPoint, byte[] theCode, long[] expected, boolean[] tests, MaxineAarch64Tester.BitsFlag[] masks) throws Exception {
        Aarch64CodeWriter code = new Aarch64CodeWriter(theCode);
        code.createStaticCodeStubsFile(functionPrototype, theCode, entryPoint);
        MaxineAarch64Tester r = new MaxineAarch64Tester(expected, tests, masks);
        r.cleanFiles();
        r.cleanProcesses();
        r.assembleStartup();
        r.compile();
        r.link();
        long[] simulatedRegisters = r.runRegisteredSimulation();
        r.cleanProcesses();
        if (POST_CLEAN_FILES) {
            r.cleanFiles();
        }
        return simulatedRegisters;
    }

    private MaxineAarch64Tester generateObjectsAndTestStubs(String functionPrototype, int entryPoint, byte[] theCode) throws Exception {
        Aarch64CodeWriter code = new Aarch64CodeWriter(theCode);
        code.createStaticCodeStubsFile(functionPrototype, theCode, entryPoint);
        MaxineAarch64Tester r = new MaxineAarch64Tester();
        r.cleanFiles();
        r.cleanProcesses();
        r.compile();
        r.runSimulation();
        r.reset();
        return r;
    }


    public Aarch64JTTC1XTest() {
        initTests();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Aarch64JTTC1XTest.class);
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
            theCompiler = (Aarch64T1XCompilation) t1x.getT1XCompilation();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private void initializeCodeBuffers(List<TargetMethod> methods) {
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

    private void initializeCodeBuffers(List<TargetMethod> methods, String fileName, String methodName) {
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

    public void test_C1X_jtt_BC_dcmp02() throws Exception {
        double[] argOne = {-1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp02");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initializeCodeBuffers(methods, "BC_dcmp02.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = BC_dcmp02.test(argOne[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = Aarch64CodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            long[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp04() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        double[] argOne = {-1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp04");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initializeCodeBuffers(methods, "BC_dcmp04.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_dcmp04.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = Aarch64CodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            long[]   registerValues   = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp07() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        double[] argOne = {-1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp07");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initializeCodeBuffers(methods, "BC_dcmp07.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_dcmp07.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = Aarch64CodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            long[]   registerValues   = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp09() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        double[] argOne = {-1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp09");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        vm().compilationBroker.setOffline(true);
        initializeCodeBuffers(methods, "BC_dcmp09.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_dcmp09.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = Aarch64CodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            long[]   registerValues   = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_d2i01() throws Exception {
        String klassName = getKlassName("jtt.bytecode.BC_d2i01");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_d2i01.java", "int test(double)");
        double[] arguments = {0.0d, 1.0d, 01.06d, -2.2d};
        int expectedInt = -9;
        for (int i = 0; i < arguments.length; i++) {
            expectedInt = BC_d2i01.test(arguments[i]);
            String functionPrototype = Aarch64CodeWriter.preAmble("int", "double", Double.toString(arguments[i]));
            long[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes, expectedValues, testvalues, bitmasks);
            assert (int) registerValues[0] == expectedInt : "Failed incorrect value " + (int) registerValues[0] + " " + expectedInt;
            theCompiler.cleanup();
        }
    }

}
