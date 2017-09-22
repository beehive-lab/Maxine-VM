/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
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
package test.arm.t1x;

import static com.sun.max.vm.MaxineVM.*;

import java.io.*;
import java.util.*;

import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.aarch64.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

import test.arm.asm.*;

public class Aarch64T1XTest extends MaxTestCase {

    private Aarch64Assembler      asm;
    private CiTarget              aarch64;
    private ARMCodeWriter         code;
    private T1X                   t1x;
    private C1X                   c1x;
    private Aarch64T1XCompilation theCompiler;
    private StaticMethodActor anMethod = null;
    private CodeAttribute codeAttr = null;
    private static boolean POST_CLEAN_FILES = true;

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

    static final class Pair {

        public final int first;
        public final int second;

        Pair(int first, int second) {
            this.first = first;
            this.second = second;
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
    private static MaxineAarch64Tester.BitsFlag[] bitmasks = new MaxineAarch64Tester.BitsFlag[MaxineAarch64Tester.NUM_REGS];
    static {
        for (int i = 0; i < MaxineAarch64Tester.NUM_REGS; i++) {
            bitmasks[i] = MaxineAarch64Tester.BitsFlag.All32Bits;
        }
    }
    private static boolean[] testValues = new boolean[MaxineAarch64Tester.NUM_REGS];

    private static void setIgnoreValue(int i, boolean value, boolean all) {
        testValues[i] = value;
    }

    private static void resetIgnoreValues() {
        for (int i = 0; i < testValues.length; i++) {
            testValues[i] = false;
        }
    }

    // The following values will be updated
    // to those expected to be found in a register after simulated execution of code.
    private static long[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    private static long[] expectedLongValues = {Long.MAX_VALUE - 100, Long.MAX_VALUE};

    private static void initialiseExpectedValues() {
        for (int i = 0; i < MaxineAarch64Tester.NUM_REGS; i++) {
            expectedValues[i] = i;
        }
    }

    private static void initialiseTestValues() {
        for (int i = 0; i < MaxineAarch64Tester.NUM_REGS; i++) {
            testValues[i] = false;
        }
    }
    private long[] generateAndTest(long[] expected, boolean[] tests, MaxineAarch64Tester.BitsFlag[] masks) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(theCompiler.getMacroAssembler().codeBuffer);
        code.createCodeFile();
        MaxineAarch64Tester r = new MaxineAarch64Tester(expected, tests, masks);
        r.cleanFiles();
        r.cleanProcesses();
        r.assembleStartup();
        r.assembleEntry();
        r.compile();
        r.link();
        r.objcopy();
        long[] simulatedRegisters = r.runRegisteredSimulation();
        r.cleanProcesses();
        if (POST_CLEAN_FILES) {
            r.cleanFiles();
        }
        return simulatedRegisters;
    }

    public Aarch64T1XTest() {
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
                vm().compilationBroker.setOffline(true);
                JavaPrototype.initialize(false);
                initialised = true;
            }
            t1x = (T1X) CompilationBroker.addCompiler("t1x", baselineCompilerName);
           // c1x = (C1X) CompilationBroker.addCompiler("c1x", optimizingCompilerName);

            //c1x.initializeOffline(Phase.HOSTED_COMPILING);
            theCompiler = (Aarch64T1XCompilation) t1x.getT1XCompilation();
            theCompiler.setDebug(false);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Aarch64T1XTest.class);
    }

//    public void test_DecStack() throws Exception {
//        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
//        theCompiler.incStack(3);
//        masm.mov(64, Aarch64.r0, Aarch64.sp); // copy stack value into r0
//        theCompiler.decStack(1);
//        masm.mov(64, Aarch64.r1, Aarch64.sp); // copy stack value onto r1
//        theCompiler.decStack(2);
//        masm.mov(64, Aarch64.r2, Aarch64.sp);
//
//        long[] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);
//        for (int i = 0; i < 16; i++) {
//            assert 2 * (simulatedValues[1] - simulatedValues[0]) == (simulatedValues[2] - simulatedValues[1]) : "Register " + i + " Value " + simulatedValues[i];
//        }
//    }
//
//    public void test_IncStack() throws Exception {
//        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
//        masm.mov(64, Aarch64.r0, Aarch64.sp); // copy stack value into r0
//        theCompiler.incStack(1);
//        masm.mov(64, Aarch64.r1, Aarch64.sp); // copy stack value onto r1
//        theCompiler.incStack(2);
//        masm.mov(64, Aarch64.r2, Aarch64.sp);
//
//        long[] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);
//        for (int i = 0; i < 16; i++) {
//            assert 2 * (simulatedValues[0] - simulatedValues[1]) == (simulatedValues[1] - simulatedValues[2]) : "Register " + i + " Value " + simulatedValues[i];
//        }
//    }

    public void test_AdjustReg() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();
        masm.mov32BitConstant(Aarch64.r0, 0);
        masm.mov32BitConstant(Aarch64.r1, Integer.MAX_VALUE);
        masm.mov32BitConstant(Aarch64.r2, Integer.MIN_VALUE);
        masm.mov32BitConstant(Aarch64.r3, 0);
        masm.mov32BitConstant(Aarch64.r4, Integer.MAX_VALUE);
        masm.mov32BitConstant(Aarch64.r5, 0);

        masm.increment32(Aarch64.r0, 1);
        masm.increment32(Aarch64.r1, 1);
        masm.increment32(Aarch64.r2, -1);
        masm.increment32(Aarch64.r3, Integer.MAX_VALUE);
        masm.increment32(Aarch64.r4, Integer.MIN_VALUE);
        masm.increment32(Aarch64.r5, 0);

        expectedValues[0] = 1;
        expectedValues[1] = Integer.MIN_VALUE;
        expectedValues[2] = Integer.MAX_VALUE;
        expectedValues[3] = Integer.MAX_VALUE;
        expectedValues[4] = -1;
        expectedValues[5] = 0;
        long[] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);
        for (int i = 0; i < 6; i++) {
            System.out.println("Register " + i + " " + (int) simulatedValues[i] + " expected " + (int) expectedValues[i]);
        }

        for (int i = 0; i < 6; i++) {
            assert (int) simulatedValues[i] == (int) expectedValues[i]
            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void work_PokeInt() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Integer.MAX_VALUE;
        expectedValues[1] = Integer.MIN_VALUE;
        expectedValues[2] = -123456789;
        expectedValues[3] = 0;
        expectedValues[4] = 123456789;
        theCompiler.incStack(4);
        for (int i = 0; i < 5; i++) {
            masm.mov32BitConstant(Aarch64.r16, (int) expectedValues[i]);
            theCompiler.pokeInt(Aarch64.r16, i);
        }

        theCompiler.peekInt(Aarch64.r0, 0);
        theCompiler.peekInt(Aarch64.r1, 1);
        theCompiler.peekInt(Aarch64.r2, 2);
        theCompiler.peekInt(Aarch64.r3, 3);
        theCompiler.peekInt(Aarch64.r4, 4);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert (int) expectedValues[i] == (int) simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void test_PokeInt() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Integer.MAX_VALUE;
        expectedValues[1] = Integer.MIN_VALUE;
        expectedValues[2] = -123456789;
        expectedValues[3] = 0;
        expectedValues[4] = 123456789;
        theCompiler.incStack(4);
        for (int i = 0; i < 5; i++) {
            masm.mov32BitConstant(Aarch64.r16, (int) expectedValues[i]);
            theCompiler.pokeInt(Aarch64.r16, i);
        }

        theCompiler.peekInt(Aarch64.r0, 0);
        theCompiler.peekInt(Aarch64.r1, 1);
        theCompiler.peekInt(Aarch64.r2, 2);
        theCompiler.peekInt(Aarch64.r3, 3);
        theCompiler.peekInt(Aarch64.r4, 4);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert (int) expectedValues[i] == (int) simulatedValues[i]
                    : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void ignore_AssignLong() throws Exception {

    }

    public void test_PeekLong() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Long.MAX_VALUE;
        expectedValues[1] = Long.MIN_VALUE;
        expectedValues[2] = -12345678987654321L;
        expectedValues[3] = 12345678987654321L;
        expectedValues[4] = 1;

        for (int i = 0; i < 5; i++) {
            testValues[i] = true;
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            masm.push(64, Aarch64.r16);
        }


        theCompiler.peekWord(Aarch64.r4, 0);
        theCompiler.peekWord(Aarch64.r3, 1);
        theCompiler.peekWord(Aarch64.r2, 2);
        theCompiler.peekWord(Aarch64.r1, 3);
        theCompiler.peekWord(Aarch64.r0, 4);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert expectedValues[i] == simulatedValues[i]
                    : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void work_PeekLong() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Long.MAX_VALUE;
        expectedValues[2] = Long.MIN_VALUE;
        expectedValues[4] = -12345678987654321L;
        expectedValues[6] = 12345678987654321L;
        expectedValues[8] = 1;

        for (int i = 0; i < 10; i += 2) {
            testValues[i] = true;
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            masm.push(64, Aarch64.r16);
            masm.push(64, Aarch64.zr);
        }

        theCompiler.peekLong(Aarch64.r8, 0);
        theCompiler.peekLong(Aarch64.r6, 2);
        theCompiler.peekLong(Aarch64.r4, 4);
        theCompiler.peekLong(Aarch64.r2, 6);
        theCompiler.peekLong(Aarch64.r0, 8);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 10; i += 2) {
            System.out.println(i + " sim: " + simulatedValues[i] + ", exp: " + expectedValues[i]);
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

//    public void test_PokeLong() throws Exception {
//      initialiseExpectedValues();
//      resetIgnoreValues();
//     Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
//     masm.codeBuffer.reset();
//
//      expectedValues[0] = Long.MAX_VALUE;
//      expectedValues[1] = Long.MIN_VALUE;
//      expectedValues[2] = -12345678987654321L;
//      expectedValues[3] = 0;
//      expectedValues[4] = 12345678987654321L;
//      theCompiler.incStack(8);
//      for (int i = 0; i < 5; i++) {
//          masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
//          theCompiler.pokeLong(Aarch64.r16, i);
//      }
//
//      theCompiler.peekLong(Aarch64.r0, 0);
//      theCompiler.peekLong(Aarch64.r1, 1);
//      theCompiler.peekLong(Aarch64.r2, 2);
//      theCompiler.peekLong(Aarch64.r3, 3);
//      theCompiler.peekLong(Aarch64.r4, 4);
//
//      long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);
//
//      for (int i = 0; i < 5; i++) {
//          assert expectedValues[i] == simulatedValues[i]
//                  : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
//      }
//    }

    public void test_PeekInt() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Integer.MAX_VALUE;
        expectedValues[1] = Integer.MIN_VALUE;
        expectedValues[2] = -123456789;
        expectedValues[3] = 123456789;
        expectedValues[4] = 1;

        for (int i = 0; i < 5; i++) {
            testValues[i] = true;
            masm.mov32BitConstant(Aarch64.r16, (int) expectedValues[i]);
            masm.push(32, Aarch64.r16);
        }

        theCompiler.peekInt(Aarch64.r4, 0);
        theCompiler.peekInt(Aarch64.r3, 1);
        theCompiler.peekInt(Aarch64.r2, 2);
        theCompiler.peekInt(Aarch64.r1, 3);
        theCompiler.peekInt(Aarch64.r0, 4);


        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            System.out.println("i " + (int) simulatedValues[i]);
        }
        for (int i = 0; i < 5; i++) {
            assert (int) expectedValues[i] == (int) simulatedValues[i]
                    : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }


    public void test_PeekFloat() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Float.floatToRawIntBits(Float.MAX_VALUE);
        expectedValues[1] = Float.floatToRawIntBits(Float.MIN_VALUE);
        expectedValues[2] = Float.floatToRawIntBits(0.0f);
        expectedValues[3] = Float.floatToRawIntBits(-1.0F);
        expectedValues[4] = Float.floatToRawIntBits(-123.89F);

        theCompiler.incStack(5);
        for (int i = 0; i < 5; i++) {
            testValues[i] = true;
            masm.mov32BitConstant(Aarch64.r16, (int) expectedValues[i]);
            masm.fmovCpu2Fpu(32, Aarch64.d16, Aarch64.r16);
            theCompiler.pokeFloat(Aarch64.d16, i);
        }

        theCompiler.peekFloat(Aarch64.d4, 4);
        theCompiler.peekFloat(Aarch64.d3, 3);
        theCompiler.peekFloat(Aarch64.d2, 2);
        theCompiler.peekFloat(Aarch64.d1, 1);
        theCompiler.peekFloat(Aarch64.d0, 0);

        masm.fmovFpu2Cpu(32, Aarch64.r4, Aarch64.d4);
        masm.fmovFpu2Cpu(32, Aarch64.r3, Aarch64.d3);
        masm.fmovFpu2Cpu(32, Aarch64.r2, Aarch64.d2);
        masm.fmovFpu2Cpu(32, Aarch64.r1, Aarch64.d1);
        masm.fmovFpu2Cpu(32, Aarch64.r0, Aarch64.d0);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            System.out.println(i + " sim: " + Float.intBitsToFloat((int) simulatedValues[i]) + ", exp: " + Float.intBitsToFloat((int) expectedValues[i]));
            assert Float.intBitsToFloat((int) simulatedValues[i]) == Float.intBitsToFloat((int) expectedValues[i])
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }


    public void ignore_PokeFloat() throws Exception {
        /* not used - test incorporated in test_PeekFloat */
    }

    public void ignore_AssignDouble() throws Exception {

    }


    public void ignore_PokeDouble() throws Exception {
        /* not used - test incorporated in test_PeekDouble */
    }

    public void test_PeekDouble() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Double.doubleToRawLongBits(Double.MAX_VALUE);
        expectedValues[2] = Double.doubleToRawLongBits(Double.MIN_VALUE);
        expectedValues[4] = Double.doubleToRawLongBits(0.0);
        expectedValues[6] = Double.doubleToRawLongBits(-1.0);
        expectedValues[8] = Double.doubleToRawLongBits(-123.89);

        theCompiler.incStack(10);
        for (int i = 0; i < 10; i += 2) {
            testValues[i] = true;
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            masm.fmovCpu2Fpu(64, Aarch64.d16, Aarch64.r16);
            theCompiler.pokeDouble(Aarch64.d16, i);
        }

        theCompiler.peekDouble(Aarch64.d8, 8);
        theCompiler.peekDouble(Aarch64.d6, 6);
        theCompiler.peekDouble(Aarch64.d4, 4);
        theCompiler.peekDouble(Aarch64.d2, 2);
        theCompiler.peekDouble(Aarch64.d0, 0);

        masm.fmovFpu2Cpu(64, Aarch64.r8, Aarch64.d8);
        masm.fmovFpu2Cpu(64, Aarch64.r6, Aarch64.d6);
        masm.fmovFpu2Cpu(64, Aarch64.r4, Aarch64.d4);
        masm.fmovFpu2Cpu(64, Aarch64.r2, Aarch64.d2);
        masm.fmovFpu2Cpu(64, Aarch64.r0, Aarch64.d0);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 10; i += 2) {
            System.out.println(i + " sim: " + simulatedValues[i] + ", exp: " + expectedValues[i] + " dbl: " + Double.longBitsToDouble(expectedValues[i]));
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void ignore_DoLconst() throws Exception {

    }

    public void ignore_DoDconst() throws Exception {

    }

    public void ignore_DoFconst() throws Exception {

    }

    public void ignore_DoLoad() throws Exception {

    }

    public void ignore_Add() throws Exception {

    }

    public void ignore_Mul() throws Exception {

    }


    public void test_push_pop() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();
        for (int i = 0; i < 6; i++) {
            expectedValues[i] = i;
            testValues[i] = true;
            masm.mov64BitConstant(Aarch64.r16, i);
            masm.push(64, Aarch64.r16);
        }
        masm.pop(64, Aarch64.r5);
        masm.pop(64, Aarch64.r4);
        masm.pop(64, Aarch64.r3);
        masm.pop(64, Aarch64.r2);
        masm.pop(64, Aarch64.r1);
        masm.pop(64, Aarch64.r0);

        long[] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 6; i++) {
            assert expectedValues[i] == simulatedValues[i]
                    : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void work_PeekWord() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Long.MAX_VALUE;
        expectedValues[1] = Long.MIN_VALUE;
        expectedValues[2] = -123456789;
        expectedValues[3] = 0;
        expectedValues[4] = 123456789;

        for (int i = 0; i < 5; i++) {
            testValues[i] = true;
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            masm.push(64, Aarch64.r16);
        }

        theCompiler.peekWord(Aarch64.r4, 0);
        theCompiler.peekWord(Aarch64.r3, 1);
        theCompiler.peekWord(Aarch64.r2, 2);
        theCompiler.peekWord(Aarch64.r1, 3);
        theCompiler.peekWord(Aarch64.r0, 4);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }


    public void work_PokeWord() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Long.MAX_VALUE;
        expectedValues[1] = Long.MIN_VALUE;
        expectedValues[2] = -123456789;
        expectedValues[3] = 0;
        expectedValues[4] = 123456789;
        theCompiler.incStack(4);
        for (int i = 0; i < 5; i++) {
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            theCompiler.pokeWord(Aarch64.r16, i);
        }

        theCompiler.peekWord(Aarch64.r0, 0);
        theCompiler.peekWord(Aarch64.r1, 1);
        theCompiler.peekWord(Aarch64.r2, 2);
        theCompiler.peekWord(Aarch64.r3, 3);
        theCompiler.peekWord(Aarch64.r4, 4);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void work_PeekObject() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = 123456789;
        expectedValues[1] = 975318642;
        expectedValues[2] = 135792468;
        expectedValues[3] = Long.MAX_VALUE;

        for (int i = 0; i < 4; i++) {
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            masm.push(64, Aarch64.r16);
        }

        theCompiler.peekObject(Aarch64.r3, 0);
        theCompiler.peekObject(Aarch64.r2, 1);
        theCompiler.peekObject(Aarch64.r1, 2);
        theCompiler.peekObject(Aarch64.r0, 3);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 4; i++) {
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void work_PokeObject() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = 123456789;
        expectedValues[1] = 975318642;
        expectedValues[2] = 135792468;
        expectedValues[3] = Long.MAX_VALUE;
        theCompiler.incStack(4);
        for (int i = 0; i < 4; i++) {
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            theCompiler.pokeObject(Aarch64.r16, i);
        }

        theCompiler.peekObject(Aarch64.r0, 0);
        theCompiler.peekObject(Aarch64.r1, 1);
        theCompiler.peekObject(Aarch64.r2, 2);
        theCompiler.peekObject(Aarch64.r3, 3);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 4; i++) {
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void failingtestIfEq() throws Exception {

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

    public void ignore_BranchBytecodes() throws Exception {

    }

    public void ignore_Locals() throws Exception {

    }

    public void ignore_ByteCodeLoad() throws Exception {

    }

    public void ignore_SwitchTable() throws Exception {

    }

    public void ignore_LookupTable() throws Exception {

    }
}
