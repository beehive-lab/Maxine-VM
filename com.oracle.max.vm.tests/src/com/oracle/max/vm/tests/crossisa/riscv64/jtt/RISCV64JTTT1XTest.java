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
package com.oracle.max.vm.tests.crossisa.riscv64.jtt;

import static com.oracle.max.asm.target.riscv64.RISCV64.*;
import static com.sun.max.vm.MaxineVM.*;
import static org.objectweb.asm.util.MaxineByteCode.getByteArray;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import com.oracle.max.asm.target.riscv64.*;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.riscv64.*;
import com.oracle.max.vm.tests.crossisa.riscv64.asm.MaxineRISCV64Tester;
import com.oracle.max.vm.tests.crossisa.riscv64.asm.RISCV64CodeWriter;
import com.sun.max.platform.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

import org.junit.*;

public class RISCV64JTTT1XTest {

    private T1X t1x;
    private C1X c1x;
    private RISCV64T1XCompilation theCompiler;
    private StaticMethodActor anMethod = null;
    private CodeAttribute codeAttr = null;
    private MaxineRISCV64Tester tester = new MaxineRISCV64Tester();
    private static boolean POST_CLEAN_FILES = false;

    public void initialiseFrameForCompilation(byte[] code, String sig) {
        codeAttr = new CodeAttribute(null, code, (char) 40, (char) 20, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create(sig), Modifier.PUBLIC | Modifier.STATIC, codeAttr, new String());
    }

    static final class Args {

        public int first;
        public int second;
        public int third;
        public int fourth;
        public long lfirst;
        public long lsecond;
        public float ffirst;
        public float fsecond;

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
            this.fsecond = fsecond;
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

        Args(float ffirst, float fsecond) {
            this.first = Float.floatToRawIntBits(ffirst);
            this.second = Float.floatToRawIntBits(fsecond);
            this.ffirst = ffirst;
            this.fsecond = fsecond;
        }

    }

    private static final OptionSet options = new OptionSet(false);
    private static VMConfigurator vmConfigurator = null;
    private static boolean initialised = false;

    private void generateAndTest(int numberOfArguments) throws Exception {
        RISCV64CodeWriter code = new RISCV64CodeWriter(theCompiler.getMacroAssembler().codeBuffer);
        code.createCodeFile(numberOfArguments);
        tester.cleanFiles();
        tester.cleanProcesses();
        tester.compile();
        tester.runRegisteredSimulation();
        tester.cleanProcesses();
        if (POST_CLEAN_FILES) {
            tester.cleanFiles();
        }
        Assert.assertTrue(tester.validateLongRegisters());
        Assert.assertTrue(tester.validateFloatRegisters());
    }

    public RISCV64JTTT1XTest() {
        initTests();
    }

    @Before
    public void initTests() {
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
                Platform.set(Platform.parse("linux-riscv64"));
                vmConfigurator.create();
                vm().compilationBroker.setOffline(true);
                JavaPrototype.initialize(false);
                initialised = true;
            }

            t1x = (T1X) CompilationBroker.addCompiler("t1x", baselineCompilerName);
            c1x = (C1X) CompilationBroker.addCompiler("c1x", optimizingCompilerName);
            c1x.initialize(Phase.HOSTED_TESTING);
            theCompiler = (RISCV64T1XCompilation) t1x.getT1XCompilation();
            tester.resetTestValues();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    @Test
    public void t1x_jtt_BC_iadd() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iadd.test(50, -49);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iadd");
        initialiseFrameForCompilation(code, "(II)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x10, 50);
        masm.mov32BitConstant(x11, -49);
        masm.push(64, x10, x11);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(2);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_iadd2() throws Exception {
        byte[] argsOne = {1, 0, 33, 1, -128, 127};
        byte[] argsTwo = {2, -1, 67, -1, 1, 1};
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iadd2");
        for (int i = 0; i < argsOne.length; i++) {
            int answer = jtt.bytecode.BC_iadd2.test(argsOne[i], argsTwo[i]);
            tester.setExpectedValue(x10, answer);
            initialiseFrameForCompilation(code, "(BB)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, argsOne[i]);
            masm.mov32BitConstant(x6, argsTwo[i]);
            masm.push(64, x5, x6);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(2);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iadd3() throws Exception {
        short[] argsOne = {1, 0, 33, 1, -128, 127, -32768, 32767};
        short[] argsTwo = {2, -1, 67, -1, 1, 1, 1, 1};
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iadd3");
        int expectedValue;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd3.test(argsOne[i], argsTwo[i]);
            tester.setExpectedValue(x10, expectedValue);
            initialiseFrameForCompilation(code, "(SS)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, argsOne[i]);
            masm.mov32BitConstant(x6, argsTwo[i]);
            masm.push(64, x5, x6);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(2);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_imul() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_imul.test(10, 12);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_imul");
        initialiseFrameForCompilation(code, "(II)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 10);
        masm.mov32BitConstant(x6, 12);
        masm.push(64, x5, x6);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "imul");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(2);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_isub() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_isub.test(100, 50);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_isub");
        initialiseFrameForCompilation(code, "(II)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 100);
        masm.mov32BitConstant(x6, 50);
        masm.push(64, x5, x6);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "isub");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(2);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_ineg() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ineg.test(100);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ineg");
        initialiseFrameForCompilation(code, "(I)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 100);
        masm.push(64, x5);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ineg");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(1);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_ineg_1() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ineg.test(-100);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ineg");
        initialiseFrameForCompilation(code, "(I)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, -100);
        masm.push(64, x5);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ineg");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(1);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_ior() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ior.test(50, 100);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ior");
        initialiseFrameForCompilation(code, "(II)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 50);
        masm.mov32BitConstant(x6, 100);
        masm.push(64, x5, x6);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ior");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(2);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_ixor() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ixor.test(50, 39);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ixor");
        initialiseFrameForCompilation(code, "(II)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 50);
        masm.mov32BitConstant(x6, 39);
        masm.push(64, x5, x6);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ixor");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(2);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_iand() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iand.test(50, 39);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iand");
        initialiseFrameForCompilation(code, "(II)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 50);
        masm.mov32BitConstant(x6, 39);
        masm.push(64, x5, x6);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iand");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(2);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_ishl() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ishl.test(10, 2);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ishl");
        initialiseFrameForCompilation(code, "(II)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 10);
        masm.mov32BitConstant(x6, 2);
        masm.push(64, x5, x6);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishl");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(2);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_ishr() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ishr.test(2048, 2);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ishr");
        initialiseFrameForCompilation(code, "(II)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 2048);
        masm.mov32BitConstant(x6, 2);
        masm.push(64, x5, x6);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishr");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(2);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_ishr_1() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_ishr.test(-2147483648, 16);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_ishr");
        initialiseFrameForCompilation(code, "(II)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, -2147483648);
        masm.mov32BitConstant(x6, 16);
        masm.push(64, x5, x6);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ishr");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(2);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_iushr() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iushr.test(-2147483648, 16);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iushr");
        initialiseFrameForCompilation(code, "(II)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, -2147483648);
        masm.mov32BitConstant(x6, 16);
        masm.push(64, x5, x6);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iushr");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(2);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_i2b() throws Exception {
        vm().compilationBroker.setOffline(initialised);
        CompilationBroker.singleton.setSimulateAdapter(true);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        byte answer = jtt.bytecode.BC_i2b.test(255);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2b");
        initialiseFrameForCompilation(code, "(I)B");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        CompilationBroker.singleton.setSimulateAdapter(false);
        masm.nop(4);
        masm.mov32BitConstant(x5, 255);
        masm.push(64, x5);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(1);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_i2b_1() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        byte answer = jtt.bytecode.BC_i2b.test(-1);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2b");
        initialiseFrameForCompilation(code, "(I)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, -1);
        masm.push(64, x5);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(1);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_i2b_2() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        byte answer = jtt.bytecode.BC_i2b.test(128);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2b");
        initialiseFrameForCompilation(code, "(I)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 128);
        masm.push(64, x5);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2b");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(1);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_i2s() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        short answer = jtt.bytecode.BC_i2s.test(65535);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 65535);
        masm.push(64, x5);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(1);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_i2s_1() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        short answer = jtt.bytecode.BC_i2s.test(32768);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 32768);
        masm.push(64, x5);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(1);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_i2s_2() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        short answer = jtt.bytecode.BC_i2s.test(-1);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, -1);
        masm.push(64, x5);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(1);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_i2s_3() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        short answer = jtt.bytecode.BC_i2s.test(Integer.MAX_VALUE);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, Integer.MAX_VALUE);
        masm.push(64, x5);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(1);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_i2s_4() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        short answer = jtt.bytecode.BC_i2s.test(0xcafebabe);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2s");
        initialiseFrameForCompilation(code, "(I)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 0xcafebabe);
        masm.push(64, x5);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2s");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(1);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_i2c() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        char answer = jtt.bytecode.BC_i2c.test(-1);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2c");
        initialiseFrameForCompilation(code, "(I)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, -1);
        masm.push(64, x5);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2c");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(1);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_i2c_1() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        char answer = jtt.bytecode.BC_i2c.test(65535);
        tester.setExpectedValue(x10, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_i2c");
        initialiseFrameForCompilation(code, "(I)I");
        RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(x5, 65535);
        masm.push(64, x5);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "i2c");
        theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
        generateAndTest(1);
        theCompiler.cleanup();
    }

    @Test
    public void t1x_jtt_BC_ireturn() throws Exception {
        int[] args = {-1, 256};
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (int i = 0; i < args.length; i++) {
            int expectedValue = jtt.bytecode.BC_ireturn.test(args[i]);
            tester.setExpectedValue(x10, expectedValue);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ireturn");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, args[i]);
            masm.push(64, x5);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_tableswitch() throws Exception {
        List<Args> pairs = new LinkedList<>();
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
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_tableswitch");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_tableswitch_2() throws Exception {
        List<Args> pairs = new LinkedList<>();
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
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_tableswitch2");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_fdiv() throws Exception {
        float[] argOne = {14.0f};
        float[] argTwo = {7.0f};
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "freturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "fdiv");

        byte[] code = getByteArray("test", "jtt.bytecode.BC_fdiv");
        for (int i = 0; i < argOne.length; i++) {
            initialiseFrameForCompilation(code, "(FF)F");
            float answer = jtt.bytecode.BC_fdiv.test(argOne[i], argTwo[i]);
            tester.setExpectedValue(f10, answer);
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, Float.floatToRawIntBits(argOne[i]));
            masm.mov32BitConstant(x6, Float.floatToRawIntBits(argTwo[i]));
            masm.fmvwx(f0, x5);
            masm.fmvwx(f1, x6);
            masm.fpush(32, f0, f1);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(2);
        }
    }

    @Test
    public void t1x_jtt_BC_tableswitch_3() throws Exception {
        List<Args> pairs = new LinkedList<>();
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
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_tableswitch3");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_tableswitch_4() throws Exception {
        List<Args> pairs = new LinkedList<>();
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
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_tableswitch4");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_lookupswitch_1() throws Exception {
        List<Args> pairs = new LinkedList<>();
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
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_lookupswitch01");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_lookupswitch_2() throws Exception {
        List<Args> pairs = new LinkedList<>();
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
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_lookupswitch02");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_lookupswitch_3() throws Exception {
        List<Args> pairs = new LinkedList<>();
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
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_lookupswitch03");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_lookupswitch_4() throws Exception {
        List<Args> pairs = new LinkedList<>();
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
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_lookupswitch04");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iinc_1() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(1, 2));
        pairs.add(new Args(2, 3));
        pairs.add(new Args(4, 5));
        pairs.add(new Args(1, 0));
        CompilationBroker.singleton.setSimulateAdapter(true);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iinc_1.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iinc_1");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_1");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iinc_2() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(1, 3));
        pairs.add(new Args(2, 4));
        pairs.add(new Args(4, 6));
        pairs.add(new Args(-2, 0));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iinc_2.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iinc_2");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_2");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iinc_3() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(1, 52));
        pairs.add(new Args(2, 53));
        pairs.add(new Args(4, 55));
        pairs.add(new Args(-1, 50));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iinc_3.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iinc_3");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_3");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iinc_4() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(1, 513));
        pairs.add(new Args(2, 514));
        pairs.add(new Args(4, 516));
        pairs.add(new Args(-1, 511));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iinc_4.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iinc_4");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iinc_4");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iload_0() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(-1, -1));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(1000345, 1000345));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iload_0.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_0");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iload_0_1() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 1));
        pairs.add(new Args(-1, 0));
        pairs.add(new Args(2, 3));
        pairs.add(new Args(1000345, 1000346));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iload_0_1.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_0_1");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0_1");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iload_0_2() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(-1, -1));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(1000345, 1000345));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iload_0_2.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_0_2");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5); // local slot is argument x0
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_0_2");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iload_1() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(1, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(1, 2));
        pairs.add(new Args(1, 1000345));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iload_1.test(pair.first, pair.second);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_1");
            initialiseFrameForCompilation(code, "(II)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.mov32BitConstant(x6, pair.second);
            masm.push(64, x5);
            masm.push(64, x6);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(2);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iload_1_1() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(-1, -1));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(1000345, 1000345));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iload_1_1.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_1_1");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_1_1");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iload_1_2() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(1, 0));
        pairs.add(new Args(1, -1));
        pairs.add(new Args(-1, 2));
        pairs.add(new Args(1000345, 1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iload_1_2.test(pair.first, pair.second);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_1_2");
            initialiseFrameForCompilation(code, "(II)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.mov32BitConstant(x6, pair.second);
            masm.push(64, x5, x6);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(2);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iload_2() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(1, 1, 0));
        pairs.add(new Args(1, 1, -1));
        pairs.add(new Args(1, 1, 2));
        pairs.add(new Args(1, 1, 1000345));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iload_2.test(pair.first, pair.second, pair.third);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_2");
            initialiseFrameForCompilation(code, "(III)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.mov32BitConstant(x6, pair.second);
            masm.mov32BitConstant(x7, pair.third);
            masm.push(64, x5);
            masm.push(64, x6);
            masm.push(64, x7);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_2");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(3);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iload_3() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(1, 1, 1, 0));
        pairs.add(new Args(1, 1, 1, -1));
        pairs.add(new Args(1, 1, 1, 2));
        pairs.add(new Args(1, 1, 1, 1000345));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iload_3.test(pair.first, pair.second, pair.third, pair.fourth);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iload_3");
            initialiseFrameForCompilation(code, "(IIII)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.mov32BitConstant(x6, pair.second);
            masm.mov32BitConstant(x7, pair.third);
            masm.mov32BitConstant(x28, pair.fourth);
            masm.push(64, x5);
            masm.push(64, x6);
            masm.push(64, x7);
            masm.push(64, x28);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iload_3");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(4);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iconst() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(2, 2));
        pairs.add(new Args(3, 3));
        pairs.add(new Args(4, 4));
        pairs.add(new Args(5, 5));
        pairs.add(new Args(6, 375));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "putfieldLong");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iconst.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iconst");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iconst");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ifeq() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 2));
        pairs.add(new Args(1, -2));
        pairs.add(new Args(6, 375));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ifeq.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifeq");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ifeq_2() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 1));
        pairs.add(new Args(1, 0));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ifeq_2.test(pair.first) ? 1 : 0;
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifeq_2");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ifeq_3() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ifeq_3.test(pair.first) ? 1 : 0;
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifeq_3");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq_3");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ifge() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(-1, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ifge.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifge");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifeq_3");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ifgt() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(-1, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ifgt.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifgt");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifgt");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ifle() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(-1, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ifle.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifle");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifle");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ifne() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(-1, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ifne.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifne");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifne");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_iflt() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(-1, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_iflt.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_iflt");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ificmplt1() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ificmplt1.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ificmplt1");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ificmplt2() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ificmplt2.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ificmplt2");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ificmpne1() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ificmpne1.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ificmpne1");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ificmpne2() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 0));
        pairs.add(new Args(1, 1));
        pairs.add(new Args(2, -1));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ificmpne2.test(pair.first);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ificmpne2");
            initialiseFrameForCompilation(code, "(I)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.push(64, x5);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iflt");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(1);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ifge_3() throws Exception {
        List<Args> pairs = new LinkedList<>();
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
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifge_3");
            initialiseFrameForCompilation(code, "(II)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.mov32BitConstant(x6, pair.second);
            masm.push(64, x5, x6);
            t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ifgt");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(2);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_ifge_2() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(0, 2));
        pairs.add(new Args(1, -2));
        pairs.add(new Args(6, 375));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "add");
        for (Args pair : pairs) {
            int answer = jtt.bytecode.BC_ifge_2.test(pair.first, pair.second) ? 1 : 0;
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.bytecode.BC_ifge_2");
            initialiseFrameForCompilation(code, "(II)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.mov32BitConstant(x6, pair.second);
            masm.push(64, x5);
            masm.push(64, x6);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(2);
            theCompiler.cleanup();
        }
    }

    @Test
    public void t1x_jtt_BC_fcmpl() throws Exception {
        List<Args> pairs = new LinkedList<>();
        pairs.add(new Args(2.5f, 2.5f));
        pairs.add(new Args(5.0f, 3.0f));
        pairs.add(new Args(6.0f, 37.5f));
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "fcmpl");
        for (Args pair : pairs) {
            int answer = jtt.jasm.BC_fcmpl.test(pair.ffirst, pair.fsecond);
            tester.setExpectedValue(x10, answer);
            byte[] code = getByteArray("test", "jtt.jasm.BC_fcmpl");
            initialiseFrameForCompilation(code, "(FF)I");
            RISCV64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(x5, pair.first);
            masm.mov32BitConstant(x6, pair.second);
            masm.push(64, x5);
            masm.push(64, x6);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, code);
            generateAndTest(2);
            theCompiler.cleanup();
        }
    }

}
