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

import static com.oracle.max.asm.target.armv7.ARMV7.*;
import static com.oracle.max.asm.target.armv7.ARMV7Assembler.ConditionFlag.Always;
import static com.sun.max.vm.MaxineVM.*;
import static org.objectweb.asm.util.MaxineByteCode.getByteArray;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.armv7.*;
import com.oracle.max.vm.tests.crossisa.armv7.asm.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;
import com.sun.max.platform.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

public class ARMV7JTTT1XTest extends MaxTestCase {

    private T1X                 t1x;
    private C1X                 c1x;
    private ARMV7T1XCompilation theCompiler;
    private StaticMethodActor anMethod = null;
    private CodeAttribute codeAttr = null;

    public void initialiseFrameForCompilation(byte[] code, String sig, int flags) {
        codeAttr = new CodeAttribute(null, code, (char) 40, (char) 20, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create(sig), flags, codeAttr, new String());
    }

    static final class Args {

        public int first;
        public int second;
        public int third;
        public int fourth;

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

    }

    private static final OptionSet options = new OptionSet(false);
    private static VMConfigurator vmConfigurator = null;
    private static boolean initialised = false;

    private MaxineARMv7Tester simulateTest() throws Exception {
        ARMV7CodeWriter code = new ARMV7CodeWriter(theCompiler.getMacroAssembler().codeBuffer);
        code.createCodeFile();
        MaxineARMv7Tester r = new MaxineARMv7Tester();
        r.cleanFiles();
        r.cleanProcesses();
        r.compile();
        r.runSimulation();
        r.reset();
        return r;
    }

    public ARMV7JTTT1XTest() {
        initTests();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ARMV7JTTT1XTest.class);
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

    public void pushArguments(ARMV7MacroAssembler masm, CiRegister... registers) {
        // Place 42 in r2 and push it on the stack as padding to force
        // the expected 8-byte stack alignment
        masm.movImm32(Always, r12, 42);
        for (CiRegister register: registers) {
            masm.push(Always, r12);
            masm.push(Always, register);
        }
    }

    public void vpushArguments(ARMV7MacroAssembler masm, CiRegister... registers) {
        // Place 42 in r2 and push it on the stack as padding to force
        // the expected 8-byte stack alignment
        masm.movImm32(Always, r12, Float.floatToRawIntBits(42));
        masm.vmov(Always, s31, r12, null, CiKind.Float, CiKind.Int);
        for (CiRegister register: registers) {
            masm.vpush(Always, s31, s31, CiKind.Float, CiKind.Float);
            masm.vpush(Always, register, register, CiKind.Float, CiKind.Float);
        }
    }

    public void test_T1X_jtt_BC_iadd2() throws Exception {
        byte[] argsOne = {1, 0, 33, 1, -128, 127};
        byte[] argsTwo = {2, -1, 67, -1, 1, 1};
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iadd2");
        for (int i = 0; i < argsOne.length; i++) {
            int answer = jtt.bytecode.BC_iadd2.test(argsOne[i], argsTwo[i]);
            initialiseFrameForCompilation(code, "(BB)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, argsOne[i]);
            masm.movImm32(Always, r1, argsTwo[i]);
            pushArguments(masm, r0, r1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iadd3() throws Exception {
        initTests();
        short[] argsOne = {1, 0, 33, 1, -128, 127, -32768, 32767};
        short[] argsTwo = {2, -1, 67, -1, 1, 1, 1, 1};
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iadd3");
        int expectedValue = 0;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd3.test(argsOne[i], argsTwo[i]);
            initialiseFrameForCompilation(code, "(SS)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, argsOne[i]);
            masm.movImm32(Always, r1, argsTwo[i]);
            pushArguments(masm, r0, r1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == expectedValue : "Failed incorrect value " + registerValues[0] + " " + expectedValue;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_imul() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_imul.test(10, 12);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_imul");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 10);
        masm.movImm32(Always, r1, 12);
        pushArguments(masm, r0, r1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "imul");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_isub() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_isub.test(100, 50);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_isub");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 100);
        masm.movImm32(Always, r1, 50);
        pushArguments(masm, r0, r1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "isub");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ineg() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ineg.test(100);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ineg");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 100);
        masm.push(Always, r0);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ineg");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ineg_1() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ineg.test(-100);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ineg");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, -100);
        masm.push(Always, r0);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ineg");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_iadd() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iadd.test(50, -49);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iadd");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 50);
        masm.movImm32(Always, r1, -49);
        pushArguments(masm, r0, r1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ior() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ior.test(50, 100);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ior");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 50);
        masm.movImm32(Always, r1, 100);
        pushArguments(masm, r0, r1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ior");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ixor() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ixor.test(50, 39);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ixor");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 50);
        masm.movImm32(Always, r1, 39);
        pushArguments(masm, r0, r1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ixor");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_iand() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iand.test(50, 39);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iand");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 50);
        masm.movImm32(Always, r1, 39);
        pushArguments(masm, r0, r1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iand");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ishl() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ishl.test(10, 2);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ishl");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 10);
        masm.movImm32(Always, r1, 2);
        pushArguments(masm, r0, r1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishl");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ishr() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ishr.test(2048, 2);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ishr");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 2048);
        masm.movImm32(Always, r1, 2);
        pushArguments(masm, r0, r1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishr");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ishr_1() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ishr.test(-2147483648, 16);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ishr");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, -2147483648);
        masm.movImm32(Always, r1, 16);
        pushArguments(masm, r0, r1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishr");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_iushr() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iushr.test(-2147483648, 16);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iushr");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, -2147483648);
        masm.movImm32(Always, r1, 16);
        pushArguments(masm, r0, r1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iushr");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2b() throws Exception {
        initTests();
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.singleton.setSimulateAdapter(true);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        byte answer = jtt.bytecode.BC_i2b.test(255);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2b");
        initialiseFrameForCompilation(code, "(I)B", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        CompilationBroker.singleton.setSimulateAdapter(false);
        masm.nop(4);
        masm.movImm32(Always, r0, 255);
        masm.push(Always, r0);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2b_1() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2b.test(-1);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2b");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, -1);
        masm.push(Always, r0);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2b_2() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2b.test(128);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2b");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 128);
        masm.push(Always, r0);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2s() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2s.test(65535);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 65535);
        masm.push(Always, r0);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2s_1() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2s.test(32768);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 32768);
        masm.push(Always, r0);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2s_2() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2s.test(-1);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, -1);
        masm.push(Always, r0);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2c() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2c.test(-1);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2c");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, -1);
        masm.push(Always, r0);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2c");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_i2c_1() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_i2c.test(65535);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2c");
        initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 65535);
        masm.push(Always, r0);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2c");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(Always, r0);
        MaxineARMv7Tester tester = simulateTest();
        int[] registerValues = tester.getSimulatedIntRegisters();
        assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
        theCompiler.cleanup();
    }

    public void test_T1X_jtt_BC_ireturn() throws Exception {
        initTests();
        int[] args = {-1, 256};
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (int i = 0; i < args.length; i++) {
            int expectedValue = jtt.bytecode.BC_ireturn.test(args[i]);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ireturn");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, args[i]);
            masm.push(Always, r0);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int simulatedValue = tester.getSimulatedIntRegisters()[0];
            assert simulatedValue == expectedValue : "Failed incorrect value " + simulatedValue + " expected: " + expectedValue;
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
            int answer = jtt.bytecode.BC_tableswitch.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_tableswitch");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_tableswitch2.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_tableswitch2");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_fdiv() throws Exception {
        initTests();
        float[] argOne = {14.0f};
        float[] argTwo = {7.0f};
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "freturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "fdiv");

        byte[] code = getByteArray("test", "jtt.bytecode.BC_fdiv");
        for (int i = 0; i < argOne.length; i++) {
            initialiseFrameForCompilation(code, "(FF)F", Modifier.PUBLIC | Modifier.STATIC);
            float answer = jtt.bytecode.BC_fdiv.test(argOne[i], argTwo[i]);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, Float.floatToRawIntBits(argOne[i]));
            masm.movImm32(Always, r1, Float.floatToRawIntBits(argTwo[i]));
            masm.vmov(Always, s0, r0, null, CiKind.Float, CiKind.Int);
            masm.vmov(Always, s1, r1, null, CiKind.Float, CiKind.Int);
            vpushArguments(masm, s0, s1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            MaxineARMv7Tester tester = simulateTest();
            float[] registerValues = tester.getSimulatedFloatRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_tableswitch3.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_tableswitch3");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_tableswitch4.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_tableswitch4");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_lookupswitch01.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_lookupswitch01");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_lookupswitch02.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_lookupswitch02");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_lookupswitch03.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_lookupswitch03");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_lookupswitch04.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_lookupswitch04");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
        CompilationBroker.singleton.setSimulateAdapter(true);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iinc_1.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iinc_1");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_1");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_iinc_2.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iinc_2");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_iinc_3.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iinc_3");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_iinc_4.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iinc_4");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_4");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_iload_0.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_0");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_iload_0_1.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_0_1");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0_1");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_iload_0_2.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_0_2");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0); // local slot is argument r0
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_iload_1.test(pair.first, pair.second);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_1");
            initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.movImm32(Always, r1, pair.second);
            masm.push(Always, r0);
            masm.push(Always, r1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_iload_1_1.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_1_1");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_1_1");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_iload_1_2() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(1, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(1000345, 1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iload_1_2.test(pair.first, pair.second);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_1_2");
            initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.movImm32(Always, r1, pair.second);
            pushArguments(masm, r0, r1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_iload_2.test(pair.first, pair.second, pair.third);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_2");
            initialiseFrameForCompilation(code, "(III)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.movImm32(Always, r1, pair.second);
            masm.movImm32(Always, r2, pair.third);
            masm.push(Always, r0);
            masm.push(Always, r1);
            masm.push(Always, r2);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_2");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_iload_3.test(pair.first, pair.second, pair.third, pair.fourth);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_3");
            initialiseFrameForCompilation(code, "(IIII)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.movImm32(Always, r1, pair.second);
            masm.movImm32(Always, r2, pair.third);
            masm.movImm32(Always, r3, pair.fourth);
            masm.push(Always, r0);
            masm.push(Always, r1);
            masm.push(Always, r2);
            masm.push(Always, r3);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_iconst.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iconst");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iconst");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_ifeq.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifeq");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_ifeq_2.test(pair.first) ? 1 : 0;
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifeq_2");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_ifeq_3.test(pair.first) ? 1 : 0;
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifeq_3");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_ifge.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifge");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq_3");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_ifgt.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifgt");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifgt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_ifle.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifle");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifle");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_ifne.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifne");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifne");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_iflt.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iflt");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_ificmplt1.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ificmplt1");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_ificmplt2.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ificmplt2");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_ificmpne1.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ificmpne1");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

    public void test_T1X_jtt_BC_ificmpne2() throws Exception {
        initTests();
        List<Args> pairs = new LinkedList<Args>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ificmpne2.test(pair.first);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ificmpne2");
            initialiseFrameForCompilation(code, "(I)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.push(Always, r0);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_ifge_3.test(pair.first, pair.second) ? 1 : 0;
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifge_3");
            initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.movImm32(Always, r1, pair.second);
            pushArguments(masm, r0, r1);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifgt");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : "Failed incorrect value " + registerValues[0] + " " + answer;
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
            int answer = jtt.bytecode.BC_ifge_2.test(pair.first, pair.second) ? 1 : 0;
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifge_2");
            initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.movImm32(Always, r0, pair.first);
            masm.movImm32(Always, r1, pair.second);
            masm.push(Always, r0);
            masm.push(Always, r1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length);
            masm.pop(Always, r0);
            MaxineARMv7Tester tester = simulateTest();
            int[] registerValues = tester.getSimulatedIntRegisters();
            assert registerValues[0] == answer : " Failed incorrect values " + registerValues[0] + " " + answer;
            theCompiler.cleanup();
        }
    }

}
