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
package com.oracle.max.vm.tests.crossisa.armv7.t1x;

import static com.oracle.max.asm.target.armv7.ARMV7.*;
import static com.oracle.max.asm.target.armv7.ARMV7Assembler.ConditionFlag.*;
import static com.sun.max.vm.MaxineVM.*;

import java.util.*;

import com.oracle.max.vm.tests.crossisa.armv7.asm.*;

import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.armv7.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;
import com.sun.max.platform.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

public class ARMV7T1XTest extends MaxTestCase {

    private T1X                     t1x;
    private C1X                     c1x;
    private ARMV7T1XCompilationTest theCompiler;
    private StaticMethodActor anMethod = null;
    private CodeAttribute codeAttr = null;

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

    private static final OptionSet options = new OptionSet(false);
    private static VMConfigurator vmConfigurator = null;
    private static boolean initialized = false;

    private static int[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    private int[] generateAndTest() throws Exception {
        return simulateTest().getSimulatedIntRegisters();
    }

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

    public ARMV7T1XTest() {
        try {
            String[] args = new String[2];
            args[0] = "t1x";
            args[1] = "HelloWorld";
            if (options != null) {
                options.parseArguments(args);
            }
            if (vmConfigurator == null) {
                vmConfigurator = new VMConfigurator(options);
            }
            String baselineCompilerName = "com.oracle.max.vm.ext.t1x.T1X";
            String optimizingCompilerName = "com.oracle.max.vm.ext.c1x.C1X";
            RuntimeCompiler.baselineCompilerOption.setValue(baselineCompilerName);
            RuntimeCompiler.optimizingCompilerOption.setValue(optimizingCompilerName);
            // Checkstyle: stop
            if (!initialized) {
                Platform.set(Platform.parse("linux-arm"));
                vmConfigurator.create();
                vm().compilationBroker.setOffline(true);
                vm().phase = Phase.HOSTED_TESTING;
                JavaPrototype.initialize(false);
                initialized = true;
            }

            // Checkstyle: start
            t1x = (T1X) CompilationBroker.addCompiler("t1x", baselineCompilerName);
            c1x = (C1X) CompilationBroker.addCompiler("c1x", optimizingCompilerName);
            c1x.initialize(Phase.HOSTED_TESTING);
            t1x.initialize(Phase.HOSTED_TESTING);
            theCompiler = (ARMV7T1XCompilationTest) t1x.getT1XCompilation();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ARMV7T1XTest.class);
    }

    public void test_DecStack() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        theCompiler.incStack(3);
        masm.mov(Always, false, r0, rsp); // copy stack value into r0
        theCompiler.decStack(1);
        masm.mov(Always, false, r1, rsp); // copy stack value onto r1
        theCompiler.decStack(2);
        masm.mov(Always, false, r2, rsp);
        int[] simulatedValues  = generateAndTest();
        for (int i = 0; i < 16; i++) {
            assert 2 * (simulatedValues[1] - simulatedValues[0]) == (simulatedValues[2] - simulatedValues[1]) : "Register " + i + " Value " + simulatedValues[i];
        }
    }

    public void test_IncStack() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov(Always, false, r0, rsp); // copy stack value into r0
        theCompiler.incStack(1);
        masm.mov(Always, false, r1, rsp); // copy stack value onto r1
        theCompiler.incStack(2);
        masm.mov(Always, false, r2, rsp);
        int[] simulatedValues  = generateAndTest();
        for (int i = 0; i < 16; i++) {
            assert 2 * (simulatedValues[0] - simulatedValues[1]) == (simulatedValues[1] - simulatedValues[2]) : "Register " + i + " Value " + simulatedValues[i];
        }
    }

    public void test_AdjustReg() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.movImm32(Always, r0, 0);
        masm.movImm32(Always, r1, 1);
        masm.movImm32(Always, r2, Integer.MIN_VALUE);
        masm.movImm32(Always, r3, Integer.MAX_VALUE);
        masm.movImm32(Always, r4, 0);
        masm.movImm32(Always, r5, 0);
        masm.incrementl(r0, 1);
        masm.incrementl(r1, -1);
        masm.incrementl(r2, -1);
        masm.incrementl(r3, 1);
        masm.incrementl(r4, Integer.MAX_VALUE);
        masm.incrementl(r5, 0);
        masm.movImm32(Always, r6, -10);
        masm.incrementl(r6, -1);
        int[] simulatedValues  = generateAndTest();
        expectedValues[0] = 1;
        expectedValues[1] = 0;
        expectedValues[2] = Integer.MAX_VALUE;
        expectedValues[3] = Integer.MIN_VALUE;
        expectedValues[4] = Integer.MAX_VALUE;
        expectedValues[5] = 0;
        expectedValues[6] = -11;
        for (int i = 0; i < 7; i++) {
            assert simulatedValues[i] == expectedValues[i] : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void test_AssignLong() throws Exception {
        long returnValue = 0;
        int i;
        long[] expectedLongValues = new long[10];
        expectedLongValues[0] = Long.MIN_VALUE;
        expectedLongValues[2] = Long.MAX_VALUE;
        expectedLongValues[4] = 0xabdef01023456789L;
        expectedLongValues[6] = 111;
        expectedLongValues[8] = 0;
        for (i = 0; i < 10; i += 2) {
            theCompiler.assignmentTests(cpuRegisters[i], expectedLongValues[i]);
        }
        int[] registerValues  = generateAndTest();
        for (i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                returnValue = 0xffffffffL & registerValues[i];
            } else {
                returnValue |= (0xffffffffL & registerValues[i]) << 32;
                assert returnValue == expectedLongValues[i - 1] : "Failed incorrect value " + Long.toString(returnValue, 16) + " " + Long.toString(expectedLongValues[i - 1], 16);
            }
        }
    }

    public void test_Poke_n_Peek_Long() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        long[] expectedLongValues = new long[10];
        expectedLongValues[0] = Long.MIN_VALUE;
        expectedLongValues[2] = Long.MAX_VALUE;
        expectedLongValues[4] = 0xabdef01023456789L;
        expectedLongValues[6] = 111;
        expectedLongValues[8] = 0;
        theCompiler.incStack(1);
        for (int i = 0; i < 10; i += 2) {
            theCompiler.assignmentTests(cpuRegisters[i], expectedLongValues[i]);
        }
        for (int i = 0; i < 10; i += 2) {
            theCompiler.pokeLong(cpuRegisters[i], 10 - i);
        }
        for (int i = 0; i <= 10; i++) {
            masm.movImm32(Always, cpuRegisters[i], -25);
        }
        for (int i = 0; i < 10; i += 2) {
            theCompiler.peekLong(cpuRegisters[i], 10 - i);
        }
        theCompiler.decStack(1);
        int[] registerValues  = generateAndTest();
        for (int i = 0; i < 10; i+=2) {
            final long returnValue = ((long) registerValues[i + 1] << 32) | (0xffffffffL & registerValues[i]) ;
            assert returnValue == expectedLongValues[i] : "Failed incorrect value " + Long.toString(returnValue, 16) + " " + Long.toString(expectedLongValues[i], 16);
        }
    }

    public void test_Poke_n_Peek_Int() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = Integer.MIN_VALUE;
        expectedValues[1] = Integer.MAX_VALUE;
        expectedValues[2] = 0;
        expectedValues[3] = -1;
        expectedValues[4] = 40;
        expectedValues[5] = -40;
        theCompiler.incStack(1);
        for (int i = 0; i <= 5; i++) {
            masm.movImm32(Always, cpuRegisters[i], expectedValues[i]);
        }
        for (int i = 0; i <= 5; i++) {
            theCompiler.pokeInt(cpuRegisters[i], 5 - i);
        }
        for (int i = 0; i <= 5; i++) {
            masm.movImm32(Always, cpuRegisters[i], -25);
        }
        for (int i = 0; i <= 5; i++) {
            theCompiler.peekInt(cpuRegisters[i], 5 - i);
        }
        theCompiler.decStack(1);
        int[] registerValues  = generateAndTest();
        for (int i = 0; i < 6; i++) {
            assert registerValues[i] == expectedValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedValues[i], 16);
        }
    }

    public void test_Poke_n_Peek_Double() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        double[] expectedValues = {Double.MIN_VALUE, Double.MAX_VALUE, 0.0D, -1.0D, -100.75D};
        CiRegister[] dRegs = new CiRegister[] {s0, s2, s4, s6, s8};
        theCompiler.incStack(1);
        for (int i = 0; i < 5; i++) {
            theCompiler.assignDoubleTest(dRegs[i], expectedValues[i]);
        }
        theCompiler.pokeDouble(dRegs[0], 8);
        theCompiler.pokeDouble(dRegs[1], 6);
        theCompiler.pokeDouble(dRegs[2], 4);
        theCompiler.pokeDouble(dRegs[3], 2);
        theCompiler.pokeDouble(dRegs[4], 0);
        for (int i = 0; i < 5; i++) {
            theCompiler.assignDoubleTest(dRegs[i], Double.longBitsToDouble(i));
        }
        theCompiler.peekDouble(dRegs[0], 8);
        theCompiler.peekDouble(dRegs[1], 6);
        theCompiler.peekDouble(dRegs[2], 4);
        theCompiler.peekDouble(dRegs[3], 2);
        theCompiler.peekDouble(dRegs[4], 0);
        theCompiler.decStack(1);
        double[] registerValues  = simulateTest().getSimulatedDoubleRegisters();
        for (int i = 0; i < 5; i ++) {
            assert registerValues[i] == expectedValues[i] :
                    "Failed with incorrect value, Expected " + expectedValues[i] + " GOT " + registerValues[i];
        }
    }

    public void test_Poke_n_Peek_Float() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        long[] expectedLongValues = new long[6];
        expectedLongValues[0] = Float.floatToRawIntBits(Float.MIN_VALUE);
        expectedLongValues[1] = Float.floatToRawIntBits(Float.MAX_VALUE);
        expectedLongValues[2] = Float.floatToRawIntBits(0.0f);
        expectedLongValues[3] = Float.floatToRawIntBits(-1.0f);
        expectedLongValues[4] = Float.floatToRawIntBits(2.5f);
        expectedLongValues[5] = Float.floatToRawIntBits(-100.75f);
        for (int i = 0; i < 6; i++) {
            masm.movImm32(Always, cpuRegisters[i], (int) expectedLongValues[i]);
        }
        masm.vmov(Always, s0, r0, null, CiKind.Float, CiKind.Int);
        masm.vmov(Always, s1, r1, null, CiKind.Float, CiKind.Int);
        masm.vmov(Always, s2, r2, null, CiKind.Float, CiKind.Int);
        masm.vmov(Always, s3, r3, null, CiKind.Float, CiKind.Int);
        masm.vmov(Always, s4, r4, null, CiKind.Float, CiKind.Int);
        masm.vmov(Always, s5, r5, null, CiKind.Float, CiKind.Int);
        theCompiler.pokeFloat(s0, 5);
        theCompiler.pokeFloat(s1, 4);
        theCompiler.pokeFloat(s2, 3);
        theCompiler.pokeFloat(s3, 2);
        theCompiler.pokeFloat(s4, 1);
        theCompiler.pokeFloat(s5, 0);
        for (int i = 0; i <= 5; i++) {
            masm.movImm32(Always, cpuRegisters[i], -25);
        }
        theCompiler.peekFloat(s0, 5);
        theCompiler.peekFloat(s1, 4);
        theCompiler.peekFloat(s2, 3);
        theCompiler.peekFloat(s3, 2);
        theCompiler.peekFloat(s4, 1);
        theCompiler.peekFloat(s5, 0);
        masm.vmov(Always, r0, s0, null, CiKind.Int, CiKind.Float);
        masm.vmov(Always, r1, s1, null, CiKind.Int, CiKind.Float);
        masm.vmov(Always, r2, s2, null, CiKind.Int, CiKind.Float);
        masm.vmov(Always, r3, s3, null, CiKind.Int, CiKind.Float);
        masm.vmov(Always, r4, s4, null, CiKind.Int, CiKind.Float);
        masm.vmov(Always, r5, s5, null, CiKind.Int, CiKind.Float);
        int[] registerValues  = generateAndTest();
        for (int i = 0; i < 6; i++) {
            assert registerValues[i] == expectedLongValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedLongValues[i], 16) + " Expected " +
                            Float.intBitsToFloat((int) expectedLongValues[i]) + " GOT " + Double.longBitsToDouble(registerValues[i]);
        }
    }

    public void test_PokeFloat() throws Exception {
        long[] expectedLongValues = new long[6];
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedLongValues[0] = Float.floatToRawIntBits(Float.MIN_VALUE);
        expectedLongValues[1] = Float.floatToRawIntBits(Float.MAX_VALUE);
        expectedLongValues[2] = Float.floatToRawIntBits(0.0f);
        expectedLongValues[3] = Float.floatToRawIntBits(-1.0f);
        expectedLongValues[4] = Float.floatToRawIntBits(2.5f);
        expectedLongValues[5] = Float.floatToRawIntBits(-100.75f);
        for (int i = 0; i < 6; i++) {
            masm.movImm32(Always, cpuRegisters[i], (int) expectedLongValues[i]);
        }
        masm.push(Always, 4 | 8); // this is to check/debug issues about wrong address //
        masm.push(Always, 1); // index 5
        masm.push(Always, 2); // index 4
        masm.push(Always, 4);
        masm.push(Always, 8); // index 2
        masm.push(Always, 16); // index 1
        masm.push(Always, 32); // index 0
        float value = -111.111111f;
        for (int i = 0; i < 6; i++) {
            expectedLongValues[i] = Float.floatToRawIntBits(value);
            value = value + -1.2f;
            masm.movImm32(Always, cpuRegisters[i], (int) expectedLongValues[i]);
            masm.vmov(Always, allRegisters[i + 16], cpuRegisters[i], null, CiKind.Float, CiKind.Int);
        }
        theCompiler.pokeFloat(s0, 5);
        theCompiler.pokeFloat(s1, 4);
        theCompiler.pokeFloat(s2, 3);
        theCompiler.pokeFloat(s3, 2);
        theCompiler.pokeFloat(s4, 1);
        theCompiler.pokeFloat(s5, 0);
        theCompiler.peekFloat(s6, 5);
        theCompiler.peekFloat(s7, 4);
        theCompiler.peekFloat(s8, 3);
        theCompiler.peekFloat(s9, 2);
        theCompiler.peekFloat(s10, 1);
        theCompiler.peekFloat(s11, 0);
        masm.vmov(Always, r0, s6, null, CiKind.Int, CiKind.Float);
        masm.vmov(Always, r1, s7, null, CiKind.Int, CiKind.Float);
        masm.vmov(Always, r2, s8, null, CiKind.Int, CiKind.Float);
        masm.vmov(Always, r3, s9, null, CiKind.Int, CiKind.Float);
        masm.vmov(Always, r4, s10, null, CiKind.Int, CiKind.Float);
        masm.vmov(Always, r5, s11, null, CiKind.Int, CiKind.Float);
        int[] registerValues  = generateAndTest();
        for (int i = 0; i < 6; i++) {
            assert registerValues[i] == expectedLongValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedLongValues[i], 16) + " Expected " +
                    Double.longBitsToDouble(expectedLongValues[i]) + " got " + Double.longBitsToDouble(registerValues[i]);
        }
    }

    public void test_AssignDouble() throws Exception {
        double[] expectedValues = new double[5];
        expectedValues[0] = Double.MIN_VALUE;
        expectedValues[1] = Double.MAX_VALUE;
        expectedValues[2] = 0.0D;
        expectedValues[3] = -1.0D;
        expectedValues[4] = -100.75D;
        for (int i = 0; i < 10; i+=2) {
            theCompiler.assignDoubleTest(floatRegisters[i], expectedValues[i/2]);
        }
        MaxineARMv7Tester tester = simulateTest();
        double[] registerValues  = tester.getSimulatedDoubleRegisters();
        for (int i = 0; i < 5; i ++) {
            assert registerValues[i] == expectedValues[i] : "Failed incorrect value Expected " + expectedValues[i] +
                    " GOT " + registerValues[i];
        }
    }

    public void test_DoLconst() throws Exception {
        long returnValue = 0;
        long[] expectedLongValues = new long[8];
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedLongValues[0] = 0xffffffffL & 0xffffffff0000ffffL;
        expectedLongValues[1] = 0xffffffffL & (0xffffffff0000ffffL >> 32);
        expectedLongValues[6] = 0;
        expectedLongValues[7] = 1;
        masm.mov(Always, false, r2, r13); // copy stack pointer to r2
        masm.movImm32(Always, r6, 0);
        masm.movImm32(Always, r7, 1); // r6 and r7 are used as temporaries,
        theCompiler.do_lconstTests(0xffffffff0000ffffL);
        masm.mov(Always, false, r3, r13); // copy revised stack pointer to r3
        theCompiler.peekLong(r0, 0);

        int[] registerValues  = generateAndTest();
        returnValue = 0xffffffffL & registerValues[0];
        returnValue |= (0xffffffffL & registerValues[1]) << 32;
        assert returnValue == 0xffffffff0000ffffL;
        assert registerValues[2] - registerValues[3] == 16; // stack pointer has increased by 8 due to pushing the
    }

    public void test_DoDconst() throws Exception {
        long returnValue = 0;
        double myVal = 3.14123;
        long[] expectedLongValues = new long[8];
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        returnValue = Double.doubleToRawLongBits(myVal);
        expectedLongValues[0] = 0xffffffffL & returnValue;
        expectedLongValues[1] = 0xffffffffL & (returnValue >> 32);
        expectedLongValues[6] = 0;
        expectedLongValues[7] = 1;
        masm.mov(Always, false, r2, r13); // copy stack pointer to r2
        masm.movImm32(Always, r6, 0);
        masm.movImm32(Always, r7, 1); // r6 and r7 are used as temporaries,
        theCompiler.do_dconstTests(myVal);
        masm.mov(Always, false, r3, r13); // copy revised stack pointer to r3
        theCompiler.peekLong(r0, 0);

        int[] registerValues  = generateAndTest();
        returnValue = 0;
        returnValue = 0xffffffffL & registerValues[0];
        returnValue |= (0xffffffffL & registerValues[1]) << 32;
        assert returnValue == Double.doubleToRawLongBits(myVal);
        assert registerValues[2] - registerValues[3] == 16 : registerValues[2] - registerValues[3];
    }

    public void test_DoFconst() throws Exception {
        long returnValue = 0;
        float myVal = 3.14123f;
        long[] expectedLongValues = new long[1];
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        returnValue = Float.floatToRawIntBits(myVal);
        expectedLongValues[0] = returnValue;
        masm.mov(Always, false, r2, r13); // copy stack pointer to r2
        masm.movImm32(Always, r6, 0);
        masm.movImm32(Always, r7, 1); // r6 and r7 are used as temporaries,
        theCompiler.do_fconstTests(myVal);
        masm.mov(Always, false, r3, r13); // copy revised stack pointer to r3
        theCompiler.peekInt(r0, 0);

        int[] registerValues  = generateAndTest();
        returnValue = registerValues[0];
        assert returnValue == Float.floatToRawIntBits(myVal);
        assert registerValues[2] - registerValues[3] == 8 : registerValues[2] - registerValues[3];
    }

    public void test_DoLoad() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.do_initFrameTests(anMethod, codeAttr);
        theCompiler.emitPrologueTests();
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        for (int i = 0; i <= 7; i++) {
            masm.movImm32(Always, cpuRegisters[i], i - 2);
        }
        masm.push(Always, r0, r1, r2, r3, r4, r5, r6, r7);
        for (int i = 0; i <= 7; i++) {
            masm.movImm32(Always, cpuRegisters[i], -25);
        }
        for (int i = 0; i < 6; i++) {
            masm.movImm32(Always, r0, 100 + i);
            masm.push(Always, r0);
            masm.push(Always, r0); // Double push to get the stack alignment right (8-byte)
            theCompiler.do_storeTests(i, Kind.INT);
        }
        long longValue = 0x0000001400000028L;
        masm.movw(Always, r0, (int) (longValue & 0xffff));
        masm.movt(Always, r0, (int) ((longValue >> 16) & 0xffff));
        masm.movw(Always, r1, (int) (((longValue >> 32) & 0xffff)));
        masm.movt(Always, r1, (int) (((longValue >> 48) & 0xffff)));
        masm.push(Always, r0, r1);
        masm.push(Always, r0, r1); // Double push to get the stack pointer for longs right
        theCompiler.do_storeTests(6, Kind.LONG);
        for (int i = 5; i >= 0; i--) {
            theCompiler.do_loadTests(i, Kind.INT);
        }
        theCompiler.do_loadTests(6, Kind.LONG);
        // First pop slots holding the long value
        masm.pop(Always, r8); // pop empty slots due to 8-byte alignment
        masm.pop(Always, r8);
        masm.pop(Always, r0); // pop actual slots
        masm.pop(Always, r1);
        for (int i = 2; i <= 7; i++) {
            masm.pop(Always, cpuRegisters[i]); // pop actual slot
            masm.pop(Always, r8); // pop empty slot due to 8-byte alignment
        }
        theCompiler.emitEpilogueTests();

        expectedValues[0] = (int) (longValue);
        expectedValues[1] = (int) (longValue >> 32);
        expectedValues[2] = 100;
        expectedValues[3] = 101;
        expectedValues[4] = 102;
        expectedValues[5] = 103;
        expectedValues[6] = 104;
        expectedValues[7] = 105;
        int[] registerValues  = generateAndTest();
        for (int i = 0; i <= 6; i++) {
            assert registerValues[i] == expectedValues[i] : "Reg val " + registerValues[i] + "  Exp " + expectedValues[i];
        }
    }

    public void test_Add() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.do_initFrameTests(anMethod, codeAttr);
        theCompiler.emitPrologueTests();
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.push(Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        masm.push(Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        theCompiler.do_iconstTests(1);
        theCompiler.do_iconstTests(2);
        theCompiler.do_iaddTests();
        theCompiler.emitEpilogueTests();
        expectedValues[0] = 3;
        expectedValues[1] = 2;

        int[] registerValues  = generateAndTest();
        assert expectedValues[0] == registerValues[0];
    }

    public void test_Mul() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.do_initFrameTests(anMethod, codeAttr);
        theCompiler.emitPrologueTests();
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.push(Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        masm.push(Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        theCompiler.do_iconstTests(3); // push the constant 1 onto the operand stack
        theCompiler.do_iconstTests(4); // push the constant 2 onto the operand stack
        theCompiler.do_imulTests();
        theCompiler.emitEpilogueTests();
        expectedValues[0] = 12;
        expectedValues[1] = 4;

        int[] registerValues  = generateAndTest();
        assert expectedValues[0] == registerValues[0];
    }

    public void test_Peek_n_Poke_Word() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = 134480128;
        expectedValues[1] = 671351040;
        expectedValues[2] = 407111936;
        for (int i = 0; i < 3; i++) {
            masm.movImm32(Always, cpuRegisters[i], expectedValues[i]);
        }
        theCompiler.pokeWord(r0, 2);
        theCompiler.pokeWord(r1, 1);
        theCompiler.pokeWord(r2, 0);

        for (int i = 0; i <= 3; i++) {
            masm.movImm32(Always, cpuRegisters[i], -25);
        }
        theCompiler.peekWord(r0, 2);
        theCompiler.peekWord(r1, 1);
        theCompiler.peekWord(r2, 0);

        int[] registerValues  = generateAndTest();
        for (int i = 0; i < 3; i++) {
            assert registerValues[i] == expectedValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedValues[i], 16);
        }
    }

    public void work_PokeWord() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = 134480128;
        expectedValues[1] = 671351040;
        expectedValues[2] = 407111936;

        for (int i = 0; i <= 5; i++) {
            masm.movImm32(Always, cpuRegisters[i], expectedValues[i]);
        }
        theCompiler.pokeWord(r0, 2);
        theCompiler.pokeWord(r1, 1);
        theCompiler.pokeWord(r2, 0);
        for (int i = 0; i < 3; i++) {
            masm.movImm32(Always, cpuRegisters[i], -25);
        }
        theCompiler.peekWord(r0, 2);
        theCompiler.peekWord(r1, 1);
        theCompiler.peekWord(r2, 0);

        int[] registerValues  = generateAndTest();
        for (int i = 0; i < 3; i++) {
            assert registerValues[i] == expectedValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedValues[i], 16);
        }
    }

    public void test_PeekObject() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = 134480128;
        expectedValues[1] = 671351040;
        expectedValues[2] = 407111936;
        for (int i = 0; i < 3; i++) {
            masm.movImm32(Always, cpuRegisters[i], expectedValues[i]);
        }
        theCompiler.pokeObject(r0, 2);
        theCompiler.pokeObject(r1, 1);
        theCompiler.pokeObject(r2, 0);

        for (int i = 0; i <= 3; i++) {
            masm.movImm32(Always, cpuRegisters[i], -25);
        }
        theCompiler.peekObject(r0, 2);
        theCompiler.peekObject(r1, 1);
        theCompiler.peekObject(r2, 0);

        int[] registerValues  = generateAndTest();
        for (int i = 0; i < 3; i++) {
            assert registerValues[i] == expectedValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedValues[i], 16);
        }
    }

    public void test_Peek_Poke_Mix() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        int a = 100;
        long b = Long.MAX_VALUE;
        int c = 200;
        long d = Long.MIN_VALUE;
        float e = Float.MIN_VALUE;
        float f = Float.MAX_VALUE;
        double g = Double.MAX_VALUE;

        theCompiler.incStack(10);
        for (int i = 0; i <= 9; i++) {
            masm.movImm32(Always, cpuRegisters[i], -25);
        }

        masm.movImm32(Always, r0, a);
        masm.movImm32(Always, r1, c);
        masm.movImm64(Always, r2, r3, b);
        masm.movImm64(Always, r4, r5, Double.doubleToLongBits(g));
        masm.movImm64(Always, r6, r7, d);
        masm.movImm32(Always, r8, Float.floatToIntBits(e));
        masm.movImm32(Always, r9, Float.floatToIntBits(f));
        masm.vmov(Always, s0, r8, null, CiKind.Float, CiKind.Int);
        masm.vmov(Always, s1, r9, null, CiKind.Float, CiKind.Int);
        masm.vmov(Always, s2, r4, r5, CiKind.Double, CiKind.Long);

        theCompiler.pokeInt(r0, 0);
        theCompiler.pokeLong(r2, 1);
        theCompiler.pokeInt(r1, 3);
        theCompiler.pokeLong(r6, 4);
        theCompiler.pokeFloat(s0, 6);
        theCompiler.pokeFloat(s1, 7);
        theCompiler.pokeDouble(s2, 8);

        for (int i = 0; i <= 9; i++) {
            masm.movImm32(Always, cpuRegisters[i], -25);
        }
        theCompiler.peekInt(r0, 0);
        theCompiler.peekLong(r2, 1);
        theCompiler.peekInt(r1, 3);
        theCompiler.peekLong(r6, 4);
        theCompiler.peekFloat(s0, 6);
        theCompiler.peekFloat(s1, 7);
        theCompiler.peekDouble(s2, 8);
        theCompiler.decStack(10);

        MaxineARMv7Tester tester = simulateTest();
        int[] registerIntValues = tester.getSimulatedIntRegisters();
        float[] registerFloatValues = tester.getSimulatedFloatRegisters();
        double[] registerDoubleValues = tester.getSimulatedDoubleRegisters();
        assert registerIntValues[0] == a : "Failed incorrect value " + registerIntValues[0] + " expected: " + a;
        assert registerIntValues[1] == c : "Failed incorrect value " + registerIntValues[1] + " expected: " + c;
        long returnValue = (0xffffffffL & registerIntValues[2]) | ((0xffffffffL & registerIntValues[3]) << 32);
        assert returnValue == b : "Failed incorrect value " + Long.toString(returnValue, 16) + " " + Long.toString(b, 16);
        returnValue = (0xffffffffL & registerIntValues[6]) | ((0xffffffffL & registerIntValues[7]) << 32);
        assert returnValue == d : "Failed incorrect value " + returnValue + " expected: " + d;
        assert registerFloatValues[0] == e : "Failed incorrect value " + registerFloatValues[0] + " expected: " + e;
        assert registerFloatValues[1] == f : "Failed incorrect value " + registerFloatValues[1] + " expected: " + f;
        assert registerDoubleValues[1] == g : "Failed incorrect value " + registerDoubleValues[1] + " expected: " + g;
    }

    public void test_PokeObject() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = 134480128;
        expectedValues[1] = 671351040;
        expectedValues[2] = 407111936;
        for (int i = 0; i < 3; i++) {
            masm.movImm32(Always, cpuRegisters[i], i);
        }
        masm.push(Always, r0); // index 5
        masm.push(Always, r1); // index 4
        masm.push(Always, r2); // index 3
        for (int i = 0; i < 3; i++) {
            masm.movImm32(Always, cpuRegisters[i], expectedValues[i]);
        }
        theCompiler.pokeObject(r0, 2);
        theCompiler.pokeObject(r1, 1);
        theCompiler.pokeObject(r2, 0);
        for (int i = 0; i < 3; i++) {
            masm.movImm32(Always, cpuRegisters[i], -25);
        }
        theCompiler.peekObject(r0, 2);
        theCompiler.peekObject(r1, 1);
        theCompiler.peekObject(r2, 0);

        int[] registerValues  = generateAndTest();
        for (int i = 0; i < 3; i++) {
            assert registerValues[i] == expectedValues[i] : "Failed incorrect value " + Long.toString(registerValues[i], 16) + " " + Long.toString(expectedValues[i], 16);
        }
    }

    public void test_IfEq() throws Exception {
        ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = 10;
        expectedValues[1] = 20;
        expectedValues[2] = 1;
        expectedValues[3] = 2;
        for (int i = 0; i < 4; i++) {
            masm.movImm32(Always, cpuRegisters[i], expectedValues[i]);
        }
        masm.cmpl(r0, r1);
        masm.jcc(Equal, masm.codeBuffer.position() + 16, false);
        masm.jcc(NotEqual, masm.codeBuffer.position() + 16, true);
        theCompiler.assignInt(r2, 20);
        theCompiler.assignInt(r3, 10);

        int[] registerValues  = generateAndTest();
        assert registerValues[3] == 10 : "Failed incorrect value " + registerValues[3] + " 10";
        assert registerValues[2] == 1 : "Failed incorrect value " + registerValues[2] + " 1";
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

    public void test_BranchBytecodes() throws Exception {
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "ireturn");
        for (BranchInfo bi : branches) {
            expectedValues[0] = bi.getExpected();
            byte[] instructions = new byte[16];
            if (bi.getStart() == 0) {
                instructions[0] = (byte) Bytecodes.ICONST_0;
            } else {
                instructions[0] = (byte) Bytecodes.ICONST_5;
            }
            instructions[1] = (byte) Bytecodes.ISTORE_1;
            instructions[2] = (byte) Bytecodes.GOTO;
            instructions[3] = (byte) 0;
            instructions[4] = (byte) 6;
            instructions[5] = (byte) Bytecodes.IINC;
            instructions[6] = (byte) 1;
            instructions[7] = (byte) bi.getStep();
            instructions[8] = (byte) Bytecodes.ILOAD_1;
            instructions[9] = (byte) Bytecodes.BIPUSH;
            instructions[10] = (byte) bi.getEnd();
            instructions[11] = (byte) bi.getBytecode();
            instructions[12] = (byte) 0xff;
            instructions[13] = (byte) 0xfa;
            instructions[14] = (byte) Bytecodes.ILOAD_1;
            instructions[15] = (byte) Bytecodes.IRETURN;

            initialiseFrameForCompilation(instructions, "(II)I", Actor.ACC_PUBLIC);
            theCompiler.offlineT1XCompile(anMethod, codeAttr, instructions, 15);
            ARMV7MacroAssembler masm = theCompiler.getMacroAssembler();
            masm.pop(Always, 1);

            int[] registerValues  = generateAndTest();
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            theCompiler.cleanup();
        }
    }

    /**
     * This test does not yet actually tests local variables.
     */
    public void broken_Locals() throws Exception {
        expectedValues[0] = 10;
        expectedValues[1] = 20;
        expectedValues[2] = 30;
        expectedValues[3] = 40;
        byte[] instructions = new byte[17];
        instructions[0] = (byte) Bytecodes.BIPUSH;
        instructions[1] = (byte) 10;
        instructions[2] = (byte) Bytecodes.ISTORE_0; // I0 stores 10
        instructions[3] = (byte) Bytecodes.BIPUSH;
        instructions[4] = (byte) 20;
        instructions[5] = (byte) Bytecodes.ISTORE_1; // I1 stores 20
        instructions[6] = (byte) Bytecodes.BIPUSH;
        instructions[7] = (byte) 30;
        instructions[8] = (byte) Bytecodes.ISTORE_2; // I2 stores 30
        instructions[9] = (byte) Bytecodes.BIPUSH;
        instructions[10] = (byte) 40;
        instructions[11] = (byte) Bytecodes.ISTORE_3; // I3 stores 40
        instructions[12] = (byte) Bytecodes.ILOAD_0;
        instructions[13] = (byte) Bytecodes.ILOAD_1;
        instructions[14] = (byte) Bytecodes.ILOAD_2;
        instructions[15] = (byte) Bytecodes.ILOAD_3;
        instructions[16] = (byte) Bytecodes.NOP;
        initialiseFrameForCompilation(instructions, "(II)I");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, instructions, 16);
        theCompiler.peekInt(r3, 0);
        theCompiler.peekInt(r2, 1);
        theCompiler.peekInt(r1, 2);
        theCompiler.peekInt(r0, 3);

        int[] registerValues  = generateAndTest();
        assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
        assert registerValues[1] == expectedValues[1] : "Failed incorrect value " + registerValues[1] + " " + expectedValues[1];
        assert registerValues[2] == expectedValues[2] : "Failed incorrect value " + registerValues[2] + " " + expectedValues[2];
        assert registerValues[3] == expectedValues[3] : "Failed incorrect value " + registerValues[3] + " " + expectedValues[3];
        theCompiler.cleanup();
    }

    public void broken_SwitchTable() throws Exception {
        // int i = 1;
        // int j, k , l, m;
        // switch(i) {
        // case 0: j=10;
        // case 1: k=20;
        // case 2: l=30;
        // default: m=40;
        // }

        // int chooseNear(int i) {
        // switch (i) {
        // } }
        // compiles to:
        // case 0: return 0;
        // case 1: return 1;
        // case 2: return 2;
        // default: return -1;
        // Method int chooseNear(int)
        // 0 iload_1 // Push local variable 1 (argument i)
        // 1 tableswitch 0 to 2: // Valid indices are 0 through 2
        // 0: 28
        // 1: 30
        // 2: 32
        // default:34
        // 28 iconst_0
        // 29 ireturn
        // 30 iconst_1
        // 31 ireturn
        // 32 iconst_2
        // 33 ireturn
        // 34 iconst_m1
        // 35 ireturn

        int[] values = new int[] {10, 20, 30, 40};
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values.length; j++) {
                if (i > j) {
                    expectedValues[j] = 0;
                } else {
                    expectedValues[j] = values[j];
                }
            }

            byte[] instructions = new byte[36];
            if (i == 0) {
                instructions[0] = (byte) Bytecodes.ICONST_0;
            } else if (i == 1) {
                instructions[0] = (byte) Bytecodes.ICONST_1;
            } else if (i == 2) {
                instructions[0] = (byte) Bytecodes.ICONST_2;
            } else {
                instructions[0] = (byte) Bytecodes.ICONST_3;
            }
            instructions[1] = (byte) Bytecodes.ISTORE_1;
            instructions[2] = (byte) Bytecodes.ILOAD_1;

            instructions[3] = (byte) Bytecodes.TABLESWITCH;
            instructions[4] = (byte) 0;
            instructions[5] = (byte) 0;
            instructions[6] = (byte) 0;
            instructions[7] = (byte) 0x1f;

            instructions[8] = (byte) 0;
            instructions[9] = (byte) 0;
            instructions[10] = (byte) 0;
            instructions[11] = (byte) 0;

            instructions[12] = (byte) 0;
            instructions[13] = (byte) 0;
            instructions[14] = (byte) 0;
            instructions[15] = (byte) 0x2;

            instructions[16] = (byte) 0;
            instructions[17] = (byte) 0;
            instructions[18] = (byte) 0;
            instructions[19] = (byte) 0x19;

            instructions[20] = (byte) 0;
            instructions[21] = (byte) 0;
            instructions[22] = (byte) 0;
            instructions[23] = (byte) 0x1b;

            instructions[24] = (byte) 0;
            instructions[25] = (byte) 0;
            instructions[26] = (byte) 0;
            instructions[27] = (byte) 0x1d;

            instructions[28] = (byte) Bytecodes.BIPUSH;
            instructions[29] = (byte) values[0];

            instructions[30] = (byte) Bytecodes.BIPUSH;
            instructions[31] = (byte) values[1];

            instructions[32] = (byte) Bytecodes.BIPUSH;
            instructions[33] = (byte) values[2];

            instructions[34] = (byte) Bytecodes.BIPUSH;
            instructions[35] = (byte) values[3];

            initialiseFrameForCompilation(instructions, "(II)I");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, instructions, 36);
            theCompiler.peekInt(r3, 0);
            theCompiler.peekInt(r2, 1);
            theCompiler.peekInt(r1, 2);
            theCompiler.peekInt(r0, 3);

            int[] registerValues  = generateAndTest();
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            assert registerValues[1] == expectedValues[1] : "Failed incorrect value " + registerValues[1] + " " + expectedValues[1];
            assert registerValues[2] == expectedValues[2] : "Failed incorrect value " + registerValues[2] + " " + expectedValues[2];
            assert registerValues[3] == expectedValues[3] : "Failed incorrect value " + registerValues[3] + " " + expectedValues[3];
            theCompiler.cleanup();
        }
    }

    public void broken_LookupTable() throws Exception {
        // int ii = 1;
        // int o, k, l, m;
        // switch (ii) {
        // case -100:
        // o = 10;
        // case 0:
        // k = 20;
        // case 100:
        // l = 30;
        // default:
        // m = 40;
        // }
        t1x.createOfflineTemplate(c1x, T1XTemplateSource.class, t1x.templates, "vreturn");
        int[] values = new int[] {10, 20, 30, 40};
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values.length; j++) {
                if (i > j) {
                    expectedValues[j] = 0;
                } else {
                    expectedValues[j] = values[j];
                }
            }

            byte[] instructions = new byte[49];
            if (i == 0) {
                instructions[0] = (byte) Bytecodes.BIPUSH;
                instructions[1] = (byte) -100;
            } else if (i == 1) {
                instructions[0] = (byte) Bytecodes.BIPUSH;
                instructions[1] = (byte) 0;
            } else if (i == 2) {
                instructions[0] = (byte) Bytecodes.BIPUSH;
                instructions[1] = (byte) 100;
            } else {
                instructions[0] = (byte) Bytecodes.BIPUSH;
                instructions[1] = (byte) 1;
            }
            instructions[2] = (byte) Bytecodes.ISTORE_1;
            instructions[3] = (byte) Bytecodes.ILOAD_1;

            instructions[4] = (byte) Bytecodes.LOOKUPSWITCH;
            instructions[5] = (byte) 0;
            instructions[6] = (byte) 0;
            instructions[7] = (byte) 0;

            instructions[8] = (byte) 0;
            instructions[9] = (byte) 0;
            instructions[10] = (byte) 0;
            instructions[11] = (byte) 0x2A;

            instructions[12] = (byte) 0;
            instructions[13] = (byte) 0;
            instructions[14] = (byte) 0;
            instructions[15] = (byte) 3;

            instructions[16] = (byte) 0xff;
            instructions[17] = (byte) 0xff;
            instructions[18] = (byte) 0xff;
            instructions[19] = (byte) 0x9c;

            instructions[20] = (byte) 0;
            instructions[21] = (byte) 0;
            instructions[22] = (byte) 0;
            instructions[23] = (byte) 0x24;

            instructions[24] = (byte) 0;
            instructions[25] = (byte) 0;
            instructions[26] = (byte) 0;
            instructions[27] = (byte) 0;

            instructions[28] = (byte) 0;
            instructions[29] = (byte) 0;
            instructions[30] = (byte) 0;
            instructions[31] = (byte) 0x26;

            instructions[32] = (byte) 0;
            instructions[33] = (byte) 0;
            instructions[34] = (byte) 0;
            instructions[35] = (byte) 0x64;

            instructions[36] = (byte) 0;
            instructions[37] = (byte) 0;
            instructions[38] = (byte) 0;
            instructions[39] = (byte) 0x28;

            instructions[40] = (byte) Bytecodes.BIPUSH;
            instructions[41] = (byte) values[0];

            instructions[42] = (byte) Bytecodes.BIPUSH;
            instructions[43] = (byte) values[1];

            instructions[44] = (byte) Bytecodes.BIPUSH;
            instructions[45] = (byte) values[2];

            instructions[46] = (byte) Bytecodes.BIPUSH;
            instructions[47] = (byte) values[3];

            instructions[48] = (byte) Bytecodes.RETURN;

            initialiseFrameForCompilation(instructions, "(II)I", Actor.ACC_PUBLIC);
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, instructions);
            theCompiler.peekInt(r3, 0);
            theCompiler.peekInt(r2, 1);
            theCompiler.peekInt(r1, 2);
            theCompiler.peekInt(r0, 3);

            int[] registerValues  = generateAndTest();
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            assert registerValues[1] == expectedValues[1] : "Failed incorrect value " + registerValues[1] + " " + expectedValues[1];
            assert registerValues[2] == expectedValues[2] : "Failed incorrect value " + registerValues[2] + " " + expectedValues[2];
            assert registerValues[3] == expectedValues[3] : "Failed incorrect value " + registerValues[3] + " " + expectedValues[3];
            theCompiler.cleanup();
        }
    }
}
