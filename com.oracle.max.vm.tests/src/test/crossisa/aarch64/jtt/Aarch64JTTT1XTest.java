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

import static com.oracle.max.asm.target.aarch64.Aarch64.*;
import static com.sun.max.vm.MaxineVM.*;
import static org.objectweb.asm.util.MaxineByteCode.getByteArray;
import static test.crossisa.CrossISATester.BitsFlag.*;

import java.lang.reflect.Modifier;

import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.aarch64.*;
import com.sun.cri.ci.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

import org.junit.*;
import test.crossisa.CrossISATester;
import test.crossisa.aarch64.asm.*;

public class Aarch64JTTT1XTest {

    private T1X t1x;
    private C1X c1x;
    private Aarch64T1XCompilation theCompiler;
    private StaticMethodActor anMethod = null;
    private CodeAttribute codeAttr = null;
    private static boolean POST_CLEAN_FILES = false;

    public void initialiseFrameForCompilation(byte[] code, String sig, int flags) {
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
    private static CrossISATester.BitsFlag[] bitmasks = new CrossISATester.BitsFlag[cpuRegisters.length];
    private static long[] expectedValues = new long[cpuRegisters.length];
    private static boolean[] testValues = new boolean[cpuRegisters.length > fpuRegisters.length ? cpuRegisters.length : fpuRegisters.length];

    private void resetTestValues() {
        for (int i = 0; i < testValues.length; i++) {
            testValues[i] = false;
        }
    }

    private void generateAndTest() throws Exception {
        Aarch64CodeWriter code = new Aarch64CodeWriter(theCompiler.getMacroAssembler().codeBuffer);
        code.createCodeFile();
        MaxineAarch64Tester r = new MaxineAarch64Tester(expectedValues, testValues, bitmasks);
        r.cleanFiles();
        r.cleanProcesses();
        r.assembleStartup();
        r.compile();
        r.link();
        r.runRegisteredSimulation();
        r.cleanProcesses();
        if (POST_CLEAN_FILES) {
            r.cleanFiles();
        }
        Assert.assertTrue(r.validateLongRegisters());
    }

    public Aarch64JTTT1XTest() {
        initTests();
    }

    private void initTests() {
        try {
            resetTestValues();

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

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    private static void setExpectedValue(CiRegister cpuRegister, int expectedValue) {
        final int index = cpuRegister.number;
        expectedValues[index] = expectedValue;
        testValues[index] = true;
        bitmasks[index] = Lower32Bits;
    }

    @Test
    public void T1X_jtt_BC_iadd() throws Exception {
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        int answer = jtt.bytecode.BC_iadd.test(50, -49);
        setExpectedValue(r0, answer);
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iadd");
        initialiseFrameForCompilation(code, "(II)I", Modifier.PUBLIC | Modifier.STATIC);
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(r0, 50);
        masm.mov32BitConstant(r1, -49);
        masm.push(r0, r1);
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
        masm.pop(r0);
        generateAndTest();
        theCompiler.cleanup();
    }

    @Test
    public void T1X_jtt_BC_iadd2() throws Exception {
        byte[] argsOne = {1, 0, 33, 1, -128, 127};
        byte[] argsTwo = {2, -1, 67, -1, 1, 1};
        initTests();
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iadd2");
        for (int i = 0; i < argsOne.length; i++) {
            int answer = jtt.bytecode.BC_iadd2.test(argsOne[i], argsTwo[i]);
            setExpectedValue(r0, answer);
            initialiseFrameForCompilation(code, "(BB)I", Modifier.PUBLIC | Modifier.STATIC);
            Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(r0, argsOne[i]);
            masm.mov32BitConstant(r1, argsTwo[i]);
            masm.push(r0, r1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(r0);
            generateAndTest();
            theCompiler.cleanup();
        }
    }

    @Test
    public void T1X_jtt_BC_iadd3() throws Exception {
        initTests();
        short[] argsOne = {1, 0, 33, 1, -128, 127, -32768, 32767};
        short[] argsTwo = {2, -1, 67, -1, 1, 1, 1, 1};
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturnUnlock");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "iadd");
        byte[] code = getByteArray("test", "jtt.bytecode.BC_iadd3");
        int expectedValue;
        for (int i = 0; i < argsOne.length; i++) {
            expectedValue = jtt.bytecode.BC_iadd3.test(argsOne[i], argsTwo[i]);
            setExpectedValue(r0, expectedValue);
            initialiseFrameForCompilation(code, "(SS)I", Modifier.PUBLIC | Modifier.STATIC);
            Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.mov32BitConstant(r0, argsOne[i]);
            masm.mov32BitConstant(r1, argsTwo[i]);
            masm.push(r0, r1);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, code, code.length - 1);
            masm.pop(r0);
            generateAndTest();
            theCompiler.cleanup();
        }
    }
}
