/*
 * Copyright (c) 2017-2019, APT Group, School of Computer Science,
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
package com.oracle.max.vm.tests.crossisa.armv7.jtt;

import static com.sun.max.vm.MaxineVM.*;

import java.io.*;
import java.math.*;
import java.util.*;

import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.maxri.*;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.armv7.*;
import com.oracle.max.vm.tests.crossisa.armv7.asm.*;
import com.sun.max.ide.*;
import com.sun.max.platform.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.code.Code;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;

import jtt.bytecode.*;

public class ARMV7JTTC1XTest extends MaxTestCase {

    private T1X                 t1x;
    private C1X                 c1x;
    private ARMV7T1XCompilation theCompiler;
    private int entryPoint = -1;
    private byte[] codeBytes = null;

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

    private String getKlassName(String klass) {
        return "^" + klass + "^";
    }

    private MaxineARMv7Tester generateObjectsAndTestStubs(String functionPrototype, int entryPoint, byte[] theCode) throws Exception {
        ARMV7CodeWriter code = new ARMV7CodeWriter(theCode);
        code.createStaticCodeStubsFile(functionPrototype, theCode, entryPoint);
        MaxineARMv7Tester r = new MaxineARMv7Tester();
        r.cleanFiles();
        r.cleanProcesses();
        r.compile();
        r.runSimulation();
        r.reset();
        return r;
    }

    private int[] generateAndTestStubs(String functionPrototype, int entryPoint, byte[] theCode) throws Exception {
        ARMV7CodeWriter code = new ARMV7CodeWriter(theCode);
        code.createStaticCodeStubsFile(functionPrototype, theCode, entryPoint);
        MaxineARMv7Tester r = new MaxineARMv7Tester();
        r.cleanFiles();
        r.cleanProcesses();
        r.compile();
        r.runSimulation();
        r.reset();
        return r.getSimulatedIntRegisters();
    }

    public ARMV7JTTC1XTest() {
        initTests();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ARMV7JTTC1XTest.class);
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
            if (!initialised) {
                Platform.set(Platform.parse("linux-arm"));
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

    private void initializeCodeBuffers(List<TargetMethod> methods, String fileName, String methodName) throws IOException {
        int minimumValue = Integer.MAX_VALUE;
        int maximumValue = Integer.MIN_VALUE;
        int offset;
        entryPoint = -1; // offset in the global array of the method we call from C.
        assert !methods.isEmpty();
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
            if (!methodName.equals(m.classMethodActor.simpleName())) {
                continue;
            }
            byte[] b = m.code();
            offset = m.codeAt(0).toInt() - minimumValue;
            System.arraycopy(b, 0, codeBytes, offset, b.length);
            m.offlineCopyCode(minimumValue, codeBytes);
        }
        // Compile Stubs class to get access to the target method of patchStaticTrampolineCallSiteARMV7 needed to patch
        // static trampolines
        String klassName = getKlassName("com.sun.max.vm.compiler.target.Stubs");
        Compile.compile(new String[] {klassName}, "C1X");
        // Fixup static trampoline's direct call to patchStaticTrampolineCallSiteARMV7 to avoid infinite loops.
        // Note that the patchStaticTrampolineCallSiteARMV7 is not copied to the codeBuffer, because of its big chain of
        // dependencies on other classes and methods that would need to be copied as well.  That said the actual
        // patching is never performed, braking tests that rely on it.
        MaxineVM.vm().stubs.staticTrampoline().offlineFixupTrampolineCallSite();
        byte[] b = MaxineVM.vm().stubs.staticTrampoline().code();
        offset = MaxineVM.vm().stubs.staticTrampoline().codeAt(0).toInt() - minimumValue;
        System.arraycopy(b, 0, codeBytes, offset, b.length);
        entryPoint = entryPoint - minimumValue;
    }

    public void test_C1X_jtt_BC_imul() throws Exception {
        Code.resetBootCodeRegion();
        int[] argsOne = {1, 0, 33, 1, -2147483648, 2147483647, -2147483648};
        int[] argsTwo = {12, -1, 67, -1, 1, -1, -1};
        String klassName = getKlassName("jtt.bytecode.BC_imul");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_imul.java", "int test(int, int)");
        int expectedValue;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = BC_imul.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int, int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_iand() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_iand");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(0, -1));
        pairs.add(new Args(3, 63));
        pairs.add(new Args(6, 4));
        pairs.add(new Args(-2147483648, 1));
        initializeCodeBuffers(methods, "BC_iand.java", "int test(int, int)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_iand.test(pair.first, pair.second);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int, int", Integer.toString(pair.first) + "," + Integer.toString(pair.second));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_ishl() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_ishl");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(0, -1));
        pairs.add(new Args(31, 1));
        pairs.add(new Args(6, 4));
        pairs.add(new Args(-2147483648, 1));
        pairs.add(new Args(-9999, 12));
        initializeCodeBuffers(methods, "BC_ishl.java", "int test(int, int)");

        for (Args pair : pairs) {
            int    expectedValue     = BC_ishl.test(pair.first, pair.second);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int, int", Integer.toString(pair.first) + "," + Integer.toString(pair.second));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_ishr() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_ishr");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(0, -1));
        pairs.add(new Args(31, 1));
        pairs.add(new Args(6, 4));
        pairs.add(new Args(-2147483648, 16));
        pairs.add(new Args(67, 2));
        initializeCodeBuffers(methods, "BC_ishr.java", "int test(int, int)");

        for (Args pair : pairs) {
            int    expectedValue     = BC_ishr.test(pair.first, pair.second);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int, int", Integer.toString(pair.first) + "," + Integer.toString(pair.second));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_iushr() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_iushr");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(0, -1));
        pairs.add(new Args(31, 1));
        pairs.add(new Args(6, 4));
        pairs.add(new Args(-2147483648, 1));
        pairs.add(new Args(-9999, 12));
        pairs.add(new Args(-2048, 6));
        pairs.add(new Args(99944444, 2));
        initializeCodeBuffers(methods, "BC_iushr.java", "int test(int, int)");

        for (Args pair : pairs) {
            int    expectedValue     = BC_iushr.test(pair.first, pair.second);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int, int", Integer.toString(pair.first) + "," + Integer.toString(pair.second));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_i2b() throws Exception {
        Code.resetBootCodeRegion();
        CompilationBroker.singleton.setSimulateAdapter(true);
        Code.resetBootCodeRegion();
        int[] argsOne = {0, /*-1, 2, 255, 128*/};
        String klassName = getKlassName("jtt.bytecode.BC_i2b");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        CompilationBroker.singleton.setSimulateAdapter(true);
        initializeCodeBuffers(methods, "BC_i2b.java", "byte test(int)");
        byte expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = BC_i2b.test(argsOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("char", "int", Integer.toString(argsOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_ireturn() throws Exception {
        Code.resetBootCodeRegion();
        int[] argOne = {0, 1, -1, 256};
        String klassName = getKlassName("jtt.bytecode.BC_ireturn");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_ireturn.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = BC_ireturn.test(argOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lookupswitch01() throws Exception {
        Code.resetBootCodeRegion();
        int[] argOne = {0, 1, 44, 67, 68, 96, 97, 98, 106, 107, 108, 132, 133, 134, 211, 212, 213};
        String klassName = getKlassName("jtt.bytecode.BC_lookupswitch01");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_lookupswitch01.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = BC_lookupswitch01.test(argOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lookupswitch02() throws Exception {
        Code.resetBootCodeRegion();
        int[] argOne = {0, 1, 44, 66, 67, 68, 96, 97, 98, 106, 107, 108, 132, 133, 134, 211, 212, 213, -121, -122, -123};
        String klassName = getKlassName("jtt.bytecode.BC_lookupswitch02");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_lookupswitch02.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = BC_lookupswitch02.test(argOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lookupswitch03() throws Exception {
        Code.resetBootCodeRegion();
        int[] argOne = {0, 1, 44, 66, 67, 68, 96, 97, 98, 106, 107, 108, 132, 133, 134, 211, 212, 213, -121, -122, -123};
        String klassName = getKlassName("jtt.bytecode.BC_lookupswitch03");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_lookupswitch03.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = BC_lookupswitch03.test(argOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lookupswitch04() throws Exception {
        Code.resetBootCodeRegion();
        int[] argOne = {0, 1, 44, 66, 67, 68, 96, 97, 98, 106, 107, 108, 132, 133, 134, 211, 212, 213, -121, -122, -123};
        String klassName = getKlassName("jtt.bytecode.BC_lookupswitch04");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_lookupswitch04.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = BC_lookupswitch04.test(argOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_tableswitch() throws Exception {
        Code.resetBootCodeRegion();
        int[] argOne = {7, -1, 0, 1, 2, 3, 4, 5, 6, 0};
        String klassName = getKlassName("jtt.bytecode.BC_tableswitch");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_tableswitch.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = BC_tableswitch.test(argOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_tableswitch2() throws Exception {
        Code.resetBootCodeRegion();
        int[] argOne = {7, -1, 0, 1, 2, 3, 4, 5, 6, 0};
        String klassName = getKlassName("jtt.bytecode.BC_tableswitch2");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_tableswitch2.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = BC_tableswitch2.test(argOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_tableswitch3() throws Exception {
        Code.resetBootCodeRegion();
        int[] argOne = {01, -2, -3, 0, -4, 3, 1, 2, 10};
        String klassName = getKlassName("jtt.bytecode.BC_tableswitch3");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_tableswitch3.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = BC_tableswitch3.test(argOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_tableswitch4() throws Exception {
        Code.resetBootCodeRegion();
        int[] argOne = {-1, 11, 0, 1, -5, -4, -3, -8};
        String klassName = getKlassName("jtt.bytecode.BC_tableswitch4");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_tableswitch4.java", "int test(int)");

        int expectedValue = 0;
        for (int i = 0; i < argOne.length; i++) {
            expectedValue = BC_tableswitch4.test(argOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(argOne[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_iconst() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_iconst");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(3, 3));
        pairs.add(new Args(4, 4));
        pairs.add(new Args(5, 5));
        pairs.add(new Args(6, 375));
        initializeCodeBuffers(methods, "BC_iconst.java", "int test(int)");

        for (Args pair : pairs) {
            int    expectedValue     = BC_iconst.test(pair.first);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(pair.first));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_invokestatic() throws Exception {
        Code.resetBootCodeRegion();
        List<Args> pairs = new LinkedList<Args>();
        String klassName = getKlassName("jtt.bytecode.BC_invokestatic");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_invokestatic.java", "int test(int)");
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(-2, -2));
        for (Args pair : pairs) {
            int answer = BC_invokestatic.test(pair.first);
            String functionPrototype = ARMV7CodeWriter.preAmble("void", "int", Integer.toString(pair.first));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_f2d() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_f2d");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_f2d.java", "double test(float)");
        float[] arguments = {-2.2f, 0.0f, 1.0f, 01.06f};
        double expectedDouble;
        for (int i = 0; i < arguments.length; i++) {
            expectedDouble = BC_f2d.test(arguments[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", "float", Float.toString(arguments[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double returnValue = tester.getSimulatedDoubleRegisters()[0];
            assert returnValue == expectedDouble : "Failed incorrect value " + returnValue + " " + expectedDouble;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_i2f() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_i2f");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_i2f.java", "float test(int)");
        int[] arguments = {-100, 0, 1, -1, -99};
        float expectedFloat;
        for (int i = 0; i < arguments.length; i++) {
            expectedFloat = BC_i2f.test(arguments[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", "int", Integer.toString(arguments[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float returnValue = tester.getSimulatedFloatRegisters()[0];
            assert returnValue == expectedFloat : "Failed incorrect value " + returnValue + " expected: " + expectedFloat;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_f2b() throws Exception {
        Code.resetBootCodeRegion();
        CompilationBroker.singleton.setSimulateAdapter(true);
        String klassName = getKlassName("jtt.bytecode.BC_f2b");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        CompilationBroker.singleton.setSimulateAdapter(true);
        initializeCodeBuffers(methods, "BC_f2b.java", "byte test(float)");
        float[] arguments = {-2.2f, 0.0f, 1.0f, 100.06f};
        byte expectedByte;
        for (int i = 0; i < arguments.length; i++) {
            expectedByte = BC_f2b.test(arguments[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("char", "float", Float.toString(arguments[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedByte : "Failed incorrect value " + registerValues[0] + " " + expectedByte;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_b2f() throws Exception {
        Code.resetBootCodeRegion();
        CompilationBroker.singleton.setSimulateAdapter(true);
        String klassName = getKlassName("jtt.bytecode.BC_b2f");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        CompilationBroker.singleton.setSimulateAdapter(true);
        initializeCodeBuffers(methods, "BC_b2f.java", "float test(byte)");
        byte[] arguments = {-100, 0, 100};
        float expectedFloat;
        for (int i = 0; i < arguments.length; i++) {
            expectedFloat = BC_b2f.test(arguments[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", "signed char", Byte.toString(arguments[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float simulatedValue = tester.getSimulatedFloatRegisters()[0];
            assert simulatedValue == expectedFloat : "Failed incorrect value " + simulatedValue + " expected " + expectedFloat;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_d2f() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_d2f");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_d2f.java", "float test(double)");
        double[] arguments = {-2.2d, 0.0d, 1.0d, 01.06d};
        float expectedFloat = -9;
        for (int i = 0; i < arguments.length; i++) {
            expectedFloat = BC_d2f.test(arguments[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", "double", Double.toString(arguments[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float simulatedValue = tester.getSimulatedFloatRegisters()[0];
            assert simulatedValue == expectedFloat : "Failed incorrect value " + simulatedValue + " expected " + expectedFloat;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_d2i01() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_d2i01");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_d2i01.java", "int test(double)");
        double[] arguments = {0.0d, 1.0d, 01.06d, -2.2d};
        int expectedInt = -9;
        for (int i = 0; i < arguments.length; i++) {
            expectedInt = BC_d2i01.test(arguments[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "double", Double.toString(arguments[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedInt : "Failed incorrect value " + registerValues[0] + " " + expectedInt;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_anewarray() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_anewarray");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_anewarray.java", "int test(int)");
        int[] arguments = {0, 1};
        for (int i = 0; i < arguments.length; i++) {
            int    answer            = BC_anewarray.test(arguments[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(arguments[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_new() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_new");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_new.java", "int test(int)");
        int[] arguments = {0, 1};
        for (int i = 0; i < arguments.length; i++) {
            int    answer            = BC_new.test(arguments[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(arguments[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_f2i01() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_f2i01");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_f2i01.java", "int test(float)");
        float[] arguments = {0.0f/* , 0.0f, 1.0f, 1.06f */};
        for (int i = 0; i < arguments.length; i++) {
            int    answer            = BC_f2i01.test(arguments[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "float", Float.toString(arguments[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_i2d() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_i2d");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_i2d.java", "double test(int)");
        int[] arguments = {-2, 0, 1, 2, 99};
        for (int i = 0; i < arguments.length; i++) {
            double answer = BC_i2d.test(arguments[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", "int", Integer.toString(arguments[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double simulatedValue = tester.getSimulatedDoubleRegisters()[0];
            assert simulatedValue == answer : "Failed incorrect value " + simulatedValue + " expected " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_ifge_2() throws Exception {
        Code.resetBootCodeRegion();
        int[] argOne = {0, 1, 6, 7};
        int[] argTwo = {2, -2, 375, 50};
        String klassName = getKlassName("jtt.bytecode.BC_ifge_2");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_ifge_2.java", "boolean test(int, int)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer = BC_ifge_2.test(argOne[i], argTwo[i]);
            int expectedValue = answer ? 1 : 0;
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int, int", Integer.toString(argOne[i]) + new String(", ") + Integer.toString(argTwo[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + "   " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp01() throws Exception {
        Code.resetBootCodeRegion();
        double[] argOne = {5.0d, -3.1d, 5.0d, -5.0d, 0d, -0.1d, -5.0d, 25.5d, 0.5d};
        double[] argTwo = {78.00d, 78.01d, 3.3d, -7.2d, 78.00d, 78.001d, -3.2d, 25.5d, 1.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp01");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dcmp01.java", "boolean test(double, double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_dcmp01.test(argOne[i], argTwo[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "double, double", Double.toString(argOne[i]) + new String(", ") + Double.toString(argTwo[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + "   " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp02() throws Exception {
        Code.resetBootCodeRegion();
        double[] argOne = {-1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp02");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dcmp02.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_dcmp02.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_MSB32() throws Exception {
        Code.resetBootCodeRegion();
        long[] input = new long[] {1L, 2L, 0L, -1L, 61440L, 2147483648L};
        int[] output = new int[] {0, 1, -1, 31, 15, 31};

        String klassName = getKlassName("jtt.max.MostSignificantBit32");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "MostSignificantBit32.java", "int test(long)");
        for (int i = 0; i < input.length; i++) {
            @SuppressWarnings("unused")
            int answer = jtt.max.MostSignificantBit32.test(input[i]);
            int    expectedValue     = output[i];
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "long long", Long.toString(input[i]) + "LL");
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_LSB32() throws Exception {
        long[] input = new long[] {1L, 2L, 0L, -1L, 61440L};
        Code.resetBootCodeRegion();
        int[] output = new int[] {0, 1, -1, 0, 12};
        String klassName = getKlassName("jtt.max.LeastSignificantBit32");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "LeastSignificantBit32.java", "int test(long)");
        for (int i = 0; i < input.length; i++) {
            @SuppressWarnings("unused")
            int answer = jtt.max.LeastSignificantBit64.test(input[i]);
            int    expectedValue     = output[i];
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "long long", Long.toString(input[i]) + "LL");
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp03() throws Exception {
        Code.resetBootCodeRegion();
        double[] argOne = {-1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp03");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dcmp03.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_dcmp03.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp04() throws Exception {
        Code.resetBootCodeRegion();
        double[] argOne = {-1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp04");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dcmp04.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_dcmp04.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp05() throws Exception {
        Code.resetBootCodeRegion();
        double[] argOne = {-1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp05");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dcmp05.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_dcmp05.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp06() throws Exception {
        Code.resetBootCodeRegion();
        double[] argOne = {-1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp06");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dcmp06.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_dcmp06.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp07() throws Exception {
        Code.resetBootCodeRegion();
        double[] argOne = {-1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp07");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dcmp07.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_dcmp07.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp08() throws Exception {
        Code.resetBootCodeRegion();
        double[] argOne = {-1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp08");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dcmp08.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_dcmp08.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp09() throws Exception {
        Code.resetBootCodeRegion();
        double[] argOne = {-1.0d, 1.0d, 0.0d, -0.0d, 5.1d, -5.1d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dcmp09");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dcmp09.java", "boolean test(double)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_dcmp09.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "double", Double.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dcmp10() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_dcmp10");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dcmp10.java", "boolean test(int)");
        for (int i = 0; i < 9; i++) {
            boolean answer            = BC_dcmp10.test(i);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(i));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp01() throws Exception {
        Code.resetBootCodeRegion();
        float[] argOne = {5.0f, -3.0f, 5.0f, -5.0f, 0f, -0.1f, 0.75f};
        float[] argTwo = {78.00f, 78.01f, 3.3f, -7.2f, 78.00f, 78.001f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp01");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fcmp01.java", "boolean test(float, float)");
        for (int i = 0; i < argOne.length; i++) {
            if (i == argOne.length - 1) {
                argOne[i] = Float.parseFloat("NaN");
            }
            boolean answer = BC_fcmp01.test(argOne[i], argTwo[i]);
            int expectedValue = answer ? 1 : 0;
            String tmp = null;
            if (Float.isNaN(argOne[i])) {
                tmp = new String("0.0f/0.0f");
            } else {
                tmp = Float.toString(argOne[i]);
            }
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "float, float", tmp + new String(",") + Float.toString(argTwo[i]));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp02() throws Exception {
        Code.resetBootCodeRegion();
        float[] argOne = {-1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp02");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fcmp02.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_fcmp02.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp03() throws Exception {
        Code.resetBootCodeRegion();
        float[] argOne = {-1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp03");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fcmp03.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_fcmp03.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp04() throws Exception {
        Code.resetBootCodeRegion();
        float[] argOne = {-1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp04");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fcmp04.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_fcmp04.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp05() throws Exception {
        Code.resetBootCodeRegion();
        float[] argOne = {-1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp05");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fcmp05.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_fcmp05.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp06() throws Exception {
        Code.resetBootCodeRegion();
        float[] argOne = {-1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp06");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fcmp06.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_fcmp06.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp07() throws Exception {
        Code.resetBootCodeRegion();
        float[] argOne = {-1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp07");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fcmp07.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_fcmp07.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp08() throws Exception {
        Code.resetBootCodeRegion();
        float[] argOne = {-1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp08");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fcmp08.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_fcmp08.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp09() throws Exception {
        Code.resetBootCodeRegion();
        float[] argOne = {-1.0f, 1.0f, 0.0f, -0.0f, 5.1f, -5.1f, 0.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fcmp09");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fcmp09.java", "boolean test(float)");
        for (int i = 0; i < argOne.length; i++) {
            boolean answer            = BC_fcmp09.test(argOne[i]);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "float", Float.toString(argOne[i]));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fcmp10() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_fcmp10");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fcmp10.java", "boolean test(int)");
        for (int i = 0; i < 9; i++) {
            boolean answer            = BC_fcmp10.test(i);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "int", Integer.toString(i));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fmul() throws Exception {
        Code.resetBootCodeRegion();
        float[] argsOne = {311.0f, 2f, -2.5f};
        float[] argsTwo = {10f, 20.1f, -6.01f};
        String klassName = getKlassName("jtt.bytecode.BC_fmul");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fmul.java", "float test(float, float)");
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = BC_fmul.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", "float, float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float simulatedValue = tester.getSimulatedFloatRegisters()[0];
            assert simulatedValue == floatValue : "Failed incorrect value " + simulatedValue + " expected " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fadd() throws Exception {
        Code.resetBootCodeRegion();
        float[] argsOne = {311.0f, 2f, -2.5f, 0.0f, 1.0f};
        float[] argsTwo = {10f, 20.1f, -6.01f, 0.0f, 1.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fadd");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fadd.java", "float test(float, float)");
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = BC_fadd.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", "float, float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float simulatedValue = tester.getSimulatedFloatRegisters()[0];
            assert simulatedValue == floatValue : "Failed incorrect value " + simulatedValue + " expected " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fsub() throws Exception {
        Code.resetBootCodeRegion();
        float[] argsOne = {311.0f, 2f, -2.5f};
        float[] argsTwo = {10f, 20.1f, -6.01f};
        String klassName = getKlassName("jtt.bytecode.BC_fsub");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fsub.java", "float test(float, float)");
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = BC_fsub.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", "float, float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float simulatedValue = tester.getSimulatedFloatRegisters()[0];
            assert simulatedValue == floatValue : "Failed incorrect value " + simulatedValue + " expected " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fdiv() throws Exception {
        Code.resetBootCodeRegion();
        float[] argsOne = {311.0f, 2f};
        float[] argsTwo = {10f, 20.1f};
        String klassName = getKlassName("jtt.bytecode.BC_fdiv");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fdiv.java", "float test(float, float)");
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = BC_fdiv.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", "float, float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float simulatedValue = tester.getSimulatedFloatRegisters()[0];
            assert simulatedValue == floatValue : "Failed incorrect value " + simulatedValue + " expected " + floatValue;
            theCompiler.cleanup();
        }
    }

    /* frem depends on the runtime call runtimeArithmeticFrem which cannot be tested offline yet */
    public void broken_C1X_jtt_BC_frem() throws Exception {
        Code.resetBootCodeRegion();
        float[] argsOne = {311.0f, 2f};
        float[] argsTwo = {10f, 20.1f};
        String klassName = getKlassName("jtt.bytecode.BC_frem");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_frem.java", "float test(float, float)");
        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = BC_frem.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", "float, float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float simulatedValue = tester.getSimulatedFloatRegisters()[0];
            assert simulatedValue == floatValue : "Failed incorrect value " + simulatedValue + " expected " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_irem() throws Exception {
        Code.resetBootCodeRegion();
        int[] argsOne = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] argsTwo = {2, 2, 2, 3, 3, 3, 3, 3, 3, 3};
        String klassName = getKlassName("jtt.bytecode.BC_irem");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_irem.java", "int test(int, int)");

        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = BC_irem.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int, int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    /* drem depends on the runtime call runtimeArithmeticDrem which cannot be tested offline yet */
    public void broken_jtt_BC_drem() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {311.0D, 2D};
        double[] argsTwo = {10D, 20.1D};
        String klassName = getKlassName("jtt.bytecode.BC_drem");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_drem.java", "double test(double, double)");
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = BC_drem.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", "double, double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double simulatedValue = tester.getSimulatedDoubleRegisters()[0];
            assert simulatedValue == doubleValue : "Failed incorrect value " + simulatedValue + " expected " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_ddiv() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {311.0D, 2D, -10.0D};
        double[] argsTwo = {10D, 20.1D, 5.0D};
        String klassName = getKlassName("jtt.bytecode.BC_ddiv");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_ddiv.java", "double test(double, double)");
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = BC_ddiv.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", "double, double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double simulatedValue = tester.getSimulatedDoubleRegisters()[0];
            assert simulatedValue == doubleValue : "Failed incorrect value " + simulatedValue + " expected " + doubleValue;
            theCompiler.cleanup();
        }
    }

    /* ldiv depends on the runtime call runtimeArithmeticLdiv which cannot be used offline */
    public void broken_C1X_FLOATDIV_jtt_BC_ldiv() throws Exception {
        Code.resetBootCodeRegion();
        CompilationBroker.singleton.setSimulateAdapter(true);
        String klassName = getKlassName("jtt.bytecode.BC_ldiv");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
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
        initializeCodeBuffers(methods, "BC_ldiv.java", "long test(long, long)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_ldiv.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_idiv() throws Exception {
        Code.resetBootCodeRegion();
        CompilationBroker.singleton.setSimulateAdapter(true);
        int[] argsOne = {1, 2, 256, 135, -2147483648, -2147483648};
        int[] argsTwo = {2, -1, 4, 7, -1, 1};
        String klassName = getKlassName("jtt.bytecode.BC_idiv");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        CompilationBroker.singleton.setSimulateAdapter(false);
        initializeCodeBuffers(methods, "BC_idiv.java", "int test(int, int)");
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = BC_idiv.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int, int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues != null : "Failed to return array of register values";
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_iadd3() throws Exception {
        Code.resetBootCodeRegion();
        short[] argsOne = {1, 0, 33, 1, -128, 127, -32768, 32767};
        short[] argsTwo = {2, -1, 67, -1, 1, 1, 1, 1};
        String klassName = getKlassName("jtt.bytecode.BC_iadd3");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_iadd3.java", "int test(short, short)");
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = BC_iadd3.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "short, short ", Short.toString(argsOne[i]) + "," + Short.toString(argsTwo[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_i2s() throws Exception {
        Code.resetBootCodeRegion();
        int[] argsOne = {1, -1, 34, 1, 65535, 32768, -32768};
        String klassName = getKlassName("jtt.bytecode.BC_i2s");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_i2s.java", "short test(int)");
        short expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = BC_i2s.test(argsOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("short", "int  ", Integer.toString(argsOne[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_iadd() throws Exception {
        Code.resetBootCodeRegion();
        int[] argsOne = {1, 0, 33, 1, -2147483648, -2147483647, 2147483647, 4080};
        int[] argsTwo = {2, -1, 67, -1, 1, -2, 1, 134217728};
        String klassName = getKlassName("jtt.bytecode.BC_iadd");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_iadd.java", "int test(int, int)");
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = BC_iadd.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int, int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_iadd2() throws Exception {
        Code.resetBootCodeRegion();
        byte[] argsOne = {1, 0, 33, 1, -128, 127};
        byte[] argsTwo = {2, -1, 67, -1, 1, 1};
        String klassName = getKlassName("jtt.bytecode.BC_iadd2");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_iadd2.java", "int test(byte, byte)");

        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = BC_iadd2.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "int, int ", Integer.toString(argsOne[i]) + "," + Integer.toString(argsTwo[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fload() throws Exception {
        Code.resetBootCodeRegion();
        float[] argsOne = {0.0f, 1.1f, -1.4f, 256.33f, 1000.001f};
        String klassName = getKlassName("jtt.bytecode.BC_fload");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fload.java", "float test(float)");

        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = BC_fload.test(argsOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", " float ", Float.toString(argsOne[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float simulatedValue = tester.getSimulatedFloatRegisters()[0];
            assert simulatedValue == floatValue : "Failed incorrect value " + simulatedValue + " expected " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fload2() throws Exception {
        Code.resetBootCodeRegion();
        float[] argsOne = {0.0f, 1.1f, -1.4f, 256.33f, 1000.001f};
        float[] argsTwo = {17.1f, 2.5f, 45.32f, -44.5f, -990.9f};
        String klassName = getKlassName("jtt.bytecode.BC_fload_2");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fload_2.java", "float test(float, float)");

        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = BC_fload_2.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", " float, float ", Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float simulatedValue = tester.getSimulatedFloatRegisters()[0];
            assert simulatedValue == floatValue : "Failed incorrect value " + simulatedValue + " expected " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_freturn() throws Exception {
        Code.resetBootCodeRegion();
        float[] argsOne = {0.0f, 1.1f, -1.4f, 256.33f, 1000.001f};
        String klassName = getKlassName("jtt.bytecode.BC_freturn");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_freturn.java", "float test(float)");

        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = BC_freturn.test(argsOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", " float ", Float.toString(argsOne[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float simulatedValue = tester.getSimulatedFloatRegisters()[0];
            assert simulatedValue == floatValue : "Failed incorrect value " + simulatedValue + " expected " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dreturn() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {0.0D, 1.1D, -1.4d, 256.33d, 1000.001d};
        String klassName = getKlassName("jtt.bytecode.BC_dreturn");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dreturn.java", "double test(double)");

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = BC_dreturn.test(argsOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", " double ", Double.toString(argsOne[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double simulatedValue = tester.getSimulatedDoubleRegisters()[0];
            assert simulatedValue == doubleValue : "Failed incorrect value " + simulatedValue + " expected " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dmul() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {311.0D, 11.2D};
        double[] argsTwo = {10D, 2.0D};
        String klassName = getKlassName("jtt.bytecode.BC_dmul");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dmul.java", "double test(double, double)");

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = BC_dmul.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", "double, double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double simulatedValue = tester.getSimulatedDoubleRegisters()[0];
            assert simulatedValue == doubleValue : "Failed incorrect value " + simulatedValue + " expected " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dsub() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {0.0D, 1.0D, 253.11d, 0.0D};
        double[] argsTwo = {0.0D, 1.0D, 54.43d, 1.0D};
        String klassName = getKlassName("jtt.bytecode.BC_dsub");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dsub.java", "double test(double, double)");

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = BC_dsub.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", "double, double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double simulatedValue = tester.getSimulatedDoubleRegisters()[0];
            assert simulatedValue == doubleValue : "Failed incorrect value " + simulatedValue + " expected " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dsub2() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {1.0D, 2.0d, 0.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dsub2");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dsub2.java", "double test(double)");
        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = BC_dsub2.test(argsOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", "double ", Double.toString(argsOne[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double simulatedValue = tester.getSimulatedDoubleRegisters()[0];
            assert simulatedValue == doubleValue : "Failed incorrect value " + simulatedValue + " expected " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fneg() throws Exception {
        Code.resetBootCodeRegion();
        float[] argsOne = {0.0f, -1.01f, 7263.8734f, 0.0f, 7263.8743f};
        String klassName = getKlassName("jtt.bytecode.BC_fneg");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fneg.java", "float test(float)");
        assert entryPoint != -1;

        for (int i = 0; i < argsOne.length; i++) {
            float floatValue = BC_fneg.test(argsOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", "float ", Float.toString(argsOne[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float simulatedValue = tester.getSimulatedFloatRegisters()[0];
            assert simulatedValue == floatValue : "Failed incorrect value " + simulatedValue + " expected " + floatValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dneg2() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {1.0d, -1.0d, -0.0D, 0.0d, -2.0d, 2.0d};
        String klassName = getKlassName("jtt.bytecode.BC_dneg2");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dneg2.java", "double test(double)");
        assert entryPoint != -1;

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = BC_dneg2.test(argsOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", " double ", Double.toString(argsOne[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double simulatedValue = tester.getSimulatedDoubleRegisters()[0];
            assert simulatedValue == doubleValue : "Failed incorrect value " + simulatedValue + " expected " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dneg() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {0.0D, -1.01D, 7263.8734d, 0.0d, -1.01d, 7263.8743d, 0.0d};
        double[] argsTwo = {1.0d, -2.01D, 8263.8734d, 1.0d, -2.01d, 8263.8734d, 1.0d};
        int[] argsThree = {0, 0, 0, 1, 1, 1, 0};
        String klassName = getKlassName("jtt.bytecode.BC_dneg");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dneg.java", "double test(double, double, int)");
        assert entryPoint != -1;

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = BC_dneg.test(argsOne[i], argsTwo[i], argsThree[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", "double, double, int", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]) + "," + Integer.toString(argsThree[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double simulatedValue = tester.getSimulatedDoubleRegisters()[0];
            assert simulatedValue == doubleValue : "Failed incorrect value " + simulatedValue + " expected " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lload_0() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lload_0");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 1L));
        pairs.add(new Args(-3L, -3L));
        pairs.add(new Args(10000L, 10000L));
        pairs.add(new Args(549755814017L, 549755814017L));
        initializeCodeBuffers(methods, "BC_lload_0.java", "long test(long)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_lload_0.test(pair.lfirst);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lload_4() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lload_4");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 1));
        pairs.add(new Args(-3L, 1));
        pairs.add(new Args(10000L, 1));
        pairs.add(new Args(549755814017L, 1));
        initializeCodeBuffers(methods, "BC_lload_4.java", "int test(long, int)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_lload_4.test(pair.lfirst, pair.second);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "long long, int", Long.toString(pair.lfirst) + "," + Long.toString(pair.second));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lload_1() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lload_1");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1L));
        pairs.add(new Args(1, -3L));
        pairs.add(new Args(1, 10000L));
        initializeCodeBuffers(methods, "BC_lload_1.java", "long test(int, long)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_lload_1.test(pair.first, pair.lfirst);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "int, long long", Integer.toString(pair.first) + "," + Long.toString(pair.lfirst));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " r2 " + registerValues[2] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lload_2() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lload_2");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1, 1L));
        pairs.add(new Args(1, 1, -3L));
        pairs.add(new Args(1, 1, 10000L));
        initializeCodeBuffers(methods, "BC_lload_2.java", "long test(int, int, long)");

        for (Args pair : pairs) {
            long expectedValue = BC_lload_2.test(pair.first, pair.second, pair.lfirst);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "int, int, long long",
                            Integer.toString(pair.first) + "," + Integer.toString(pair.second) + "," + Long.toString(pair.lfirst));
            int[]  registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue    = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lload_3() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lload_3");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1, 1, 1L));
        pairs.add(new Args(1, 1, 1, -3L));
        pairs.add(new Args(1, 1, 1, 10000L));
        initializeCodeBuffers(methods, "BC_lload_3.java", "long test(int, int, int, long)");

        for (Args pair : pairs) {
            long expectedValue = BC_lload_3.test(pair.first, pair.second, pair.third, pair.lfirst);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "int, int, int, long long",
                            Integer.toString(pair.first) + "," + Integer.toString(pair.second) + "," + Integer.toString(pair.third) + "," + Long.toString(pair.lfirst));
            int[]  registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue    = 0xffffffffL & registerValues[0];
            returnValue |= (0xffffffffL & registerValues[1]) << 32;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_ladd() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_ladd");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(33L, 67L));
        pairs.add(new Args(1L, -1L));
        pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(2147483647L, 1L));
        pairs.add(new Args(-2147483647L, -2L));
        pairs.add(new Args(214723483647L, 1L));
        initializeCodeBuffers(methods, "BC_ladd.java", "long test(long, long)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_ladd.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lor() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lor");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
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
        initializeCodeBuffers(methods, "BC_lor.java", "long test(long, long)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_lor.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lxor() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lxor");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(31L, 63L));
        pairs.add(new Args(6L, 4L));
        pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 0xdeadd0d0deadd0d0L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 0xd0d0d0d0d0d0d0d0L));
        initializeCodeBuffers(methods, "BC_lxor.java", "long test(long, long)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_lxor.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "," + Long.toString(pair.lsecond));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0] | ((0xffffffffL & registerValues[1]) << 32);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_land() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_land");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(31L, 63L));
        pairs.add(new Args(6L, 4L));
        pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 45));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 0xdeadd0d0deadd0d0L));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 0xd0d0d0d0d0d0d0d0L));
        initializeCodeBuffers(methods, "BC_land.java", "long test(long, long)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_land.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_land_const() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_land_const");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1, 1));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(31, 31));
        pairs.add(new Args(6, 6));
        initializeCodeBuffers(methods, "BC_land_const.java", "long test(int)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_land_const.test(pair.first);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "int", Long.toString(pair.first));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lshl() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lshl");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
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
        initializeCodeBuffers(methods, "BC_lshl.java", "long test(long, int)");
        for (Args pair : pairs) {
            long   expectedValue     = BC_lshl.test(pair.lfirst, pair.second);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long, int", Long.toString(pair.lfirst) + "LL," + Integer.toString(pair.second));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lshr() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lshr");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
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
        initializeCodeBuffers(methods, "BC_lshr.java", "long test(long, int)");
        for (Args pair : pairs) {
            long   expectedValue     = BC_lshr.test(pair.lfirst, pair.second);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long, int", Long.toString(pair.lfirst) + "LL," + Integer.toString(pair.second));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_loop01() throws Exception {
        Code.resetBootCodeRegion();
        CompilationBroker.singleton.setSimulateAdapter(true);
        String klassName = getKlassName("jtt.loop.Loop01");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        CompilationBroker.singleton.setSimulateAdapter(false);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(62, 2));
        initializeCodeBuffers(methods, "Loop01.java", "boolean test(int)");

        for (Args pair : pairs) {
            boolean answer            = jtt.loop.Loop01.test(pair.first);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long    returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_charcomp() throws Exception {
        Code.resetBootCodeRegion();
        CompilationBroker.singleton.setSimulateAdapter(true);
        String klassName = getKlassName("jtt.bytecode.BC_charcomp");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        CompilationBroker.singleton.setSimulateAdapter(false);
        char argOne = 'c';
        char[] argTwo = {'a', 'c', 'd'};
        initializeCodeBuffers(methods, "BC_charcomp.java", "boolean test(int, char, char)");

        for (int j = 0; j < argTwo.length; j++) {
            for (int i = -2; i < 4; i++) {
                boolean answer = BC_charcomp.test(i, argOne, argTwo[j]);
                int expectedValue = answer ? 1 : 0;
                String functionPrototype = ARMV7CodeWriter.preAmble("int ", " int, char, char",
                                Integer.toString(i) + ", " + "'" + Character.toString(argOne) + "'" + ", " + "'" + Character.toString(argTwo[j]) + "'");
                int[]  registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
                long   returnValue    = registerValues[0];
                assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
                theCompiler.cleanup();
            }
        }
    }

    public void test_C1X_jtt_loop02() throws Exception {
        Code.resetBootCodeRegion();
        CompilationBroker.singleton.setSimulateAdapter(true);
        String klassName = getKlassName("jtt.loop.Loop02");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        CompilationBroker.singleton.setSimulateAdapter(false);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(62, 2));
        initializeCodeBuffers(methods, "Loop02.java", "boolean test(int)");

        for (Args pair : pairs) {
            boolean answer            = jtt.loop.Loop02.test(pair.first);
            int     expectedValue     = answer ? 1 : 0;
            String  functionPrototype = ARMV7CodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long    returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_loop03() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.loop.Loop03");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(62, 2));
        pairs.add(new Args(10, 2));
        initializeCodeBuffers(methods, "Loop03.java", "int test(int)");

        for (Args pair : pairs) {
            int    expectedValue     = jtt.loop.Loop03.test(pair.first);
            String functionPrototype = ARMV7CodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_loop04() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.loop.Loop04");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(10, 2));
        initializeCodeBuffers(methods, "Loop04.java", "int test(int)");

        for (Args pair : pairs) {
            int    expectedValue     = jtt.loop.Loop04.test(pair.first);
            String functionPrototype = ARMV7CodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_loop11() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.loop.Loop11");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(5, 2));
        initializeCodeBuffers(methods, "Loop11.java", "int test(int)");

        for (Args pair : pairs) {
            int    expectedValue     = jtt.loop.Loop11.test(pair.first);
            String functionPrototype = ARMV7CodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void infinite_C1X_jtt_loopPHI() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.loop.LoopPhi");
        CompilationBroker.singleton.setSimulateAdapter(true);
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        CompilationBroker.singleton.setSimulateAdapter(false);
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
        initializeCodeBuffers(methods, "LoopPhi.java", "int test(int)");

        for (Args pair : pairs) {
            int    expectedValue     = jtt.loop.LoopPhi.test(pair.first);
            String functionPrototype = ARMV7CodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_irem2() throws Exception {
        Code.resetBootCodeRegion();
        CompilationBroker.singleton.setSimulateAdapter(true);
        String klassName = getKlassName("jtt.bytecode.BC_irem");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        CompilationBroker.singleton.setSimulateAdapter(false);
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
        initializeCodeBuffers(methods, "BC_irem.java", "int test(int, int)");

        for (Args pair : pairs) {
            int    expectedValue     = BC_irem.test(pair.first, pair.second);
            String functionPrototype = ARMV7CodeWriter.preAmble("int ", " int, int ", Integer.toString(pair.first) + ", " + Integer.toString(pair.second));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void infinite_C1X_jtt_loopInline() throws Exception {
        Code.resetBootCodeRegion();
        CompilationBroker.singleton.setSimulateAdapter(true);
        String klassName = getKlassName("jtt.loop.LoopInline");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        CompilationBroker.singleton.setSimulateAdapter(false);
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(3, 2));
        pairs.add(new Args(10, 2));
        initializeCodeBuffers(methods, "LoopInline.java", "int test(int)");

        for (Args pair : pairs) {
            int    expectedValue     = jtt.loop.LoopInline.test(pair.first);
            String functionPrototype = ARMV7CodeWriter.preAmble("int ", " int", Integer.toString(pair.first));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0];
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

    public long connectRegs(long reg0, long reg1) {
        long returnVal;
        if (reg1 < 0) {
            returnVal = reg1 << 32;
            if (reg0 < 0) {
                returnVal += 1L << 31;
                returnVal += reg0 + (1L << 31);
            } else {
                returnVal += reg0;
            }
        } else {
            returnVal = reg1 << 32;
            if (reg0 < 0) {
                returnVal += 1L << 31;
                returnVal += reg0 + (1L << 31);
            } else {
                returnVal += reg0;
            }
        }
        return returnVal;
    }

    public void test_C1X_jtt_BC_long_tests() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
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
        initializeCodeBuffers(methods, "BC_long_tests.java", "long test(long)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_long_tests.test(pair.lfirst);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst) + "LL");
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_long_le() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));
        initializeCodeBuffers(methods, "BC_long_tests.java", "boolean le(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue     = BC_long_tests.le(pair.lfirst, pair.lsecond);
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            boolean returnValue       = registerValues[0] == 1;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_long_ge() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 1L));
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        initializeCodeBuffers(methods, "BC_long_tests.java", "boolean ge(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue     = BC_long_tests.ge(pair.lfirst, pair.lsecond);
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            boolean returnValue       = registerValues[0] == 1;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_long_eq() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));
        initializeCodeBuffers(methods, "BC_long_tests.java", "boolean eq(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue     = BC_long_tests.eq(pair.lfirst, pair.lsecond);
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            boolean returnValue       = registerValues[0] == 1;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_long_ne() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));
        initializeCodeBuffers(methods, "BC_long_tests.java", "boolean ne(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue     = BC_long_tests.ne(pair.lfirst, pair.lsecond);
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            boolean returnValue       = registerValues[0] == 1;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_long_gt() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));
        initializeCodeBuffers(methods, "BC_long_tests.java", "boolean gt(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue     = BC_long_tests.gt(pair.lfirst, pair.lsecond);
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            boolean returnValue       = registerValues[0] == 1;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_long_lt() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_long_tests");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        pairs.add(new Args(1L, 1L));
        initializeCodeBuffers(methods, "BC_long_tests.java", "boolean lt(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue     = BC_long_tests.lt(pair.lfirst, pair.lsecond);
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            boolean returnValue       = registerValues[0] == 1;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lushr() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lushr");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2));
        pairs.add(new Args(67L, 2));
        pairs.add(new Args(31L, 1));
        pairs.add(new Args(6L, 4));
        pairs.add(new Args(-2147483648L, 16));
        pairs.add(new Args(0xdeadbeefd0daf0baL, 45));
        initializeCodeBuffers(methods, "BC_lushr.java", "long test(long, int)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_lushr.test(pair.lfirst, pair.second);
            String functionPrototype = ARMV7CodeWriter.preAmble("unsigned long long", "unsigned long long, int", asUnsignedDecimalString(pair.lfirst) + "," + Integer.toString(pair.second));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lcmp() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lcmp");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(77L, 78L));
        pairs.add(new Args(-1L, 0L));
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args(Long.MAX_VALUE, Long.MIN_VALUE));
        initializeCodeBuffers(methods, "BC_lcmp.java", "boolean test(long, long)");

        for (Args pair : pairs) {
            boolean expectedValue     = BC_lcmp.test(pair.lfirst, pair.lsecond);
            String  functionPrototype = ARMV7CodeWriter.preAmble("int", "long long, long long", Long.toString(pair.lfirst) + "," + Long.toString(pair.lsecond));
            int[]   registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            boolean returnValue       = registerValues[0] > 0;
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lmul() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lmul");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(1L, 2L));
        pairs.add(new Args(0L, -1L));
        pairs.add(new Args(33L, 67L));
        pairs.add(new Args(1L, -1L));
        // pairs.add(new Args(-2147483648L, 1L));
        pairs.add(new Args(2147483647L, -1L));
        pairs.add(new Args(-2147483648L, -1L));
        pairs.add(new Args(1000000L, 1000000L));
        initializeCodeBuffers(methods, "BC_lmul.java", "long test(long, long)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_lmul.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lneg() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lneg");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args(-1L, 1L));
        pairs.add(new Args(7263L, -7263L));
        pairs.add(new Args(-2147483648L, 2147483648L));
        initializeCodeBuffers(methods, "BC_lneg.java", "long test(long)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_lneg.test(pair.lfirst);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst) + "LL");
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lreturn() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lreturn");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0L, 0L));
        pairs.add(new Args(1L, 1L));
        pairs.add(new Args(-1L, -1L));
        pairs.add(new Args(256L, 256L));
        pairs.add(new Args(1000000000000L, 1000000000000L));
        initializeCodeBuffers(methods, "BC_lreturn.java", "long test(long)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_lreturn.test(pair.lfirst);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long", Long.toString(pair.lfirst));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_lsub() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_lsub");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
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
        initializeCodeBuffers(methods, "BC_lsub.java", "long test(long, long)");
        for (Args pair : pairs) {
            long   expectedValue     = BC_lsub.test(pair.lfirst, pair.lsecond);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "long long, long long", Long.toString(pair.lfirst) + "LL," + Long.toString(pair.lsecond) + "LL");
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_i2l() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_i2l");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(Integer.MIN_VALUE, (long) Integer.MIN_VALUE));
        pairs.add(new Args(1, 1L));
        pairs.add(new Args(2, 2L));
        pairs.add(new Args(3, 3L));
        pairs.add(new Args(-1, -1L));
        pairs.add(new Args(Integer.MIN_VALUE / 2, (long) Integer.MIN_VALUE));
        pairs.add(new Args(Integer.MAX_VALUE, (long) Integer.MAX_VALUE));
        initializeCodeBuffers(methods, "BC_i2l.java", "long test(int)");

        for (Args pair : pairs) {
            long   expectedValue     = BC_i2l.test(pair.first);
            String functionPrototype = ARMV7CodeWriter.preAmble("long long", "int", Integer.toString(pair.first));
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = connectRegs(registerValues[0], registerValues[1]);
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_l2i() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_l2i");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
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
        initializeCodeBuffers(methods, "BC_l2i.java", "int test(long)");

        for (Args pair : pairs) {
            int    expectedValue     = BC_l2i.test(pair.lfirst);
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "long long", Long.toString(pair.lfirst) + "LL");
            int[]  registerValues    = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            long   returnValue       = registerValues[0];
            assert returnValue == expectedValue : "Failed incorrect value r0 " + registerValues[0] + " r1 " + registerValues[1] + " " + expectedValue + " " + returnValue;
            theCompiler.cleanup();
        }
    }

    /* l2f depends on the runtime call runtimel2float which cannot be tested offline yet */
    public void broken_C1X_jtt_BC_l2f() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_l2f");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0L, 0.0f));
        pairs.add(new Args(1L, 1.0f));
        pairs.add(new Args((long) Integer.MAX_VALUE, (float) Integer.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, (float) Long.MAX_VALUE));
        pairs.add(new Args(-74652389L, -74652389.00f));
        pairs.add(new Args((long) Integer.MIN_VALUE, (float) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MIN_VALUE, (float) Long.MIN_VALUE));
        initializeCodeBuffers(methods, "BC_l2f.java", "float test(long)");

        for (Args pair : pairs) {
            float  expectedValue     = BC_l2f.test(pair.lfirst);
            String functionPrototype = ARMV7CodeWriter.preAmble("float", "long long", Long.toString(pair.lfirst) + "LL");
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            float  returnValue       = tester.getSimulatedFloatRegisters()[0];
            assert returnValue == expectedValue : "Failed incorrect value " + returnValue + " expected: " + expectedValue;
            theCompiler.cleanup();
        }
    }

    /* l2d depends on the runtime call runtimel2double which cannot be tested offline yet */
    public void broken_C1X_jtt_BC_l2d() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("jtt.bytecode.BC_l2d");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        List<Args> pairs = new ArrayList<>(8);
        pairs.add(new Args(0L, 0.0f));
        pairs.add(new Args(1L, 1.0f));
        pairs.add(new Args((long) Integer.MAX_VALUE, (float) Integer.MAX_VALUE));
        pairs.add(new Args(Long.MAX_VALUE, (float) Long.MAX_VALUE));
        pairs.add(new Args(-74652389L, -74652389.00f));
        pairs.add(new Args((long) Integer.MIN_VALUE, (float) Integer.MIN_VALUE));
        pairs.add(new Args(Long.MIN_VALUE, (float) Long.MIN_VALUE));
        initializeCodeBuffers(methods, "BC_l2d.java", "double test(long)");

        for (Args pair : pairs) {
            double expectedValue = BC_l2d.test(pair.lfirst);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", "long long", Long.toString(pair.lfirst) + "LL");
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double returnValue = tester.getSimulatedDoubleRegisters()[0];
            assert returnValue == expectedValue : "Failed incorrect value of d0 " + returnValue + " expected " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_generic_compilation() throws Exception {
        Code.resetBootCodeRegion();
        String klassName = getKlassName("com.sun.max.vm.MaxineVM");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "MaxineVM.java", "int run(Pointer, int," + " Pointer, Word, Word, Word, Pointer, Pointer, Pointer, Pointer, int, Pointer)");
        theCompiler.cleanup();
    }

    public void test_C1X_jtt_BC_fload_5() throws Exception {
        Code.resetBootCodeRegion();
        float[] argsOne = {0.0f, 1.1f};
        float[] argsTwo = {17.1f, 2.5f};
        float[] argsThree = {0.0f, 1.1f};
        float[] argsFour = {17.1f, 2.5f};
        float[] argsFive = {0.0f, 1.0f};
        String klassName = getKlassName("jtt.bytecode.BC_fload_5");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fload_5.java", "boolean test(float, float, float, float, float)");

        for (int i = 0; i < argsOne.length; i++) {
            boolean rt = BC_fload_5.test(argsOne[i], argsTwo[i], argsThree[i], argsFour[i], argsFive[i]);
            int expectedValue = rt ? 1 : 0;
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "float, float, float, float, float",
                            Float.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]) + "," + Float.toString(argsThree[i]) + "," + Float.toString(argsFour[i]) + "," + Float.toString(argsFive[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dload_9() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {0.0D, 1.1D};
        double[] argsTwo = {17.1D, 2.5D};
        double[] argsThree = {0.0D, 1.1D};
        double[] argsFour = {17.1D, 2.5D};
        double[] argsFive = {17.1D, 2.5D};
        double[] argsSix = {17.1D, 2.5D};
        double[] argsSeven = {17.1D, 2.5D};
        double[] argsEight = {17.1D, 2.5D};
        double[] argsNine = {0.0D, 1.0D};
        String klassName = getKlassName("jtt.bytecode.BC_dload_9");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dload_9.java", "boolean test(double, double, double, double, double, double, double, double, double)");

        for (int i = 0; i < argsOne.length; i++) {
            boolean rt = BC_dload_9.test(argsOne[i], argsTwo[i], argsThree[i], argsFour[i], argsFive[i], argsSix[i], argsSeven[i], argsEight[i], argsNine[i]);
            int expectedValue = rt ? 1 : 0;
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "double, double, double, double, double, double, double, double, double",
                            Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]) + "," + Double.toString(argsThree[i]) + "," + Double.toString(argsFour[i]) + "," +
                                            Double.toString(argsFive[i]) + "," + Double.toString(argsSix[i]) + "," + Double.toString(argsSeven[i]) + "," + Double.toString(argsEight[i]) + "," +
                                            Double.toString(argsNine[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dload_10() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {0.0D, 1.1D};
        double[] argsTwo = {17.1D, 2.5D};
        double[] argsThree = {0.0D, 1.1D};
        double[] argsFour = {17.1D, 2.5D};
        double[] argsFive = {17.1D, 2.5D};
        double[] argsSix = {17.1D, 2.5D};
        double[] argsSeven = {17.1D, 2.5D};
        double[] argsEight = {17.1D, 2.5D};
        double[] argsNine = {0.0D, 1.0D};
        float[] argsTen = {0.0f, 1.0f};

        String klassName = getKlassName("jtt.bytecode.BC_dload_10");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dload_10.java", "boolean test(double, double, double, double, double, double, double, double, double, float)");

        for (int i = 0; i < argsOne.length; i++) {
            boolean rt = BC_dload_10.test(argsOne[i], argsTwo[i], argsThree[i], argsFour[i], argsFive[i], argsSix[i], argsSeven[i], argsEight[i], argsNine[i], argsTen[i]);
            int expectedValue = rt ? 1 : 0;
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "double, double, double, double, double, double, double, double, double, float",
                            Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]) + "," + Double.toString(argsThree[i]) + "," + Double.toString(argsFour[i]) + "," +
                                            Double.toString(argsFive[i]) + "," + Double.toString(argsSix[i]) + "," + Double.toString(argsSeven[i]) + "," + Double.toString(argsEight[i]) + "," +
                                            Double.toString(argsNine[i]) + "," + Float.toString(argsTen[i]));

            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dload() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {0.0D, 1.1D, -1.4D, 256.33D, 1000.001D};
        String klassName = getKlassName("jtt.bytecode.BC_dload");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dload.java", "double test(double)");

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = BC_dload.test(argsOne[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", " double ", Double.toString(argsOne[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double simulatedValue = tester.getSimulatedDoubleRegisters()[0];
            assert simulatedValue == doubleValue : "Failed incorrect value got:" + simulatedValue + " expected: " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dload_2() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {0.0D, 1.1D, -1.4D, 256.33D, 1000.001D};
        double[] argsTwo = {0.0D, 1.1D, -1.4D, 256.33D, 1000.001D};
        String klassName = getKlassName("jtt.bytecode.BC_dload_2");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dload_2.java", "double test(double, double)");

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = BC_dload_2.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", " double, double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double simulatedValue = tester.getSimulatedDoubleRegisters()[0];
            assert simulatedValue == doubleValue : "Failed incorrect value got:" + simulatedValue + " expected: " + doubleValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_fdload() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {Double.MAX_VALUE, Double.MIN_VALUE};
        float[] argsTwo = {0.0f, 2.5f};
        double[] argsThree = {Double.MIN_VALUE, Double.MAX_VALUE};
        float[] argsFour = {17.1f, 2.5f};
        float[] argsFive = {0.0f, 1.0f};

        String klassName = getKlassName("jtt.bytecode.BC_fdload");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_fdload.java", "boolean test(double, float, double, float, float)");

        for (int i = 0; i < argsOne.length; i++) {
            boolean rt = BC_fdload.test(argsOne[i], argsTwo[i], argsThree[i], argsFour[i], argsFive[i]);
            int expectedValue = rt ? 1 : 0;
            String functionPrototype = ARMV7CodeWriter.preAmble("int", "double, float, double, float, float", Double.toString(argsOne[i]) + "," + Float.toString(argsTwo[i]) + "," +
                            Double.toString(argsThree[i]) + "," + Float.toString(argsFour[i]) + "," + Float.toString(argsFive[i]));
            int[] registerValues = generateAndTestStubs(functionPrototype, entryPoint, codeBytes);
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_C1X_jtt_BC_dadd() throws Exception {
        Code.resetBootCodeRegion();
        double[] argsOne = {0.0D, 1.0D, 253.11d};
        double[] argsTwo = {0.0D, 1.0D, 54.43D};
        String klassName = getKlassName("jtt.bytecode.BC_dadd");
        List<TargetMethod> methods = Compile.compile(new String[] {klassName}, "C1X");
        initializeCodeBuffers(methods, "BC_dadd.java", "double test(double, double)");

        for (int i = 0; i < argsOne.length; i++) {
            double doubleValue = BC_dadd.test(argsOne[i], argsTwo[i]);
            String functionPrototype = ARMV7CodeWriter.preAmble("double", "double, double ", Double.toString(argsOne[i]) + "," + Double.toString(argsTwo[i]));
            MaxineARMv7Tester tester = generateObjectsAndTestStubs(functionPrototype, entryPoint, codeBytes);
            double simulatedValue = tester.getSimulatedDoubleRegisters()[0];
            assert simulatedValue == doubleValue : "Failed incorrect value got:" + simulatedValue + " expected: " + doubleValue;
            theCompiler.cleanup();
        }
    }
}
