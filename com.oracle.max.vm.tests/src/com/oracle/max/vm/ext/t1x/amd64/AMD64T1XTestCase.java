/*
 * Copyright (c) 2017, 2019, APT Group, School of Computer Science,
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
package com.oracle.max.vm.ext.t1x.amd64;

import static com.sun.max.vm.MaxineVM.vm;

import java.util.*;

import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.t1x.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

public class AMD64T1XTestCase extends MaxTestCase {

    private AMD64MacroAssembler asm;
    private AMD64T1XCompilation theCompiler;
    private CiTarget amd64;
    private T1X t1x;
    //private C1X c1x;
    private StaticMethodActor anMethod = null;
    private CodeAttribute codeAttr = null;
    private static boolean POST_CLEAN_FILES = true;
    private static final OptionSet options = new OptionSet(false);
    private static VMConfigurator vmConfigurator = null;
    private static boolean initialised = false;

    public AMD64T1XTestCase() {
        try {
            String[] args = new String[2];
            args[0] = new String("t1xp");
            args[1] = new String("HelloWorld");
            if (options != null) {
                options.parseArguments(args);
            }
            if (vmConfigurator == null) {
                vmConfigurator = new VMConfigurator(options);
            }
            String baselineCompilerName = new String("com.oracle.max.vm.ext.t1x.T1X");
            String optimizingCompilerName = new String("com.oracle.max.vm.ext.c1x.C1X");
            //String optimizingCompilerName = new String("com.oracle.max.vm.ext.graal.MaxGraal");

            RuntimeCompiler.baselineCompilerOption.setValue(baselineCompilerName);
            RuntimeCompiler.optimizingCompilerOption.setValue(optimizingCompilerName);
            if (!initialised) {
                vmConfigurator.create();
                vm().compilationBroker.setOffline(true);
                JavaPrototype.initialize(false);
                initialised = true;
            }
            t1x = (T1X) CompilationBroker.addCompiler("t1x", baselineCompilerName);
            T1XOptions.PrintCompilation = true;
            C1X c1x = (C1X) CompilationBroker.addCompiler("c1x", optimizingCompilerName);
            //graal = (MaxGraal)CompilationBroker.addCompiler("Graal", optimizingCompilerName);
            //graal.initialize(Phase.HOSTED_COMPILING);
            c1x.initialize(Phase.HOSTED_COMPILING);
            theCompiler = (AMD64T1XCompilation) t1x.getT1XCompilation();
            //t1x.initialize(Phase.HOSTED_COMPILING);
            //theCompiler.setDebug(false);
            Trace.on();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }
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

    public void test_nothing() {
        return;
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

    public void test_emitPrologueTests() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        //theCompiler.emitPrologueTests();
    }

    public void do_nothing() {
        return;
    }

    public void test_BranchBytecodes() throws Exception {
//        initialiseFrameForCompilation();
//        System.out.println("Compiler: " + theCompiler + " method: " + anMethod + " codeattr: " + codeAttr);
//        theCompiler.initFrame(anMethod, codeAttr);
        /*
         * Based on pg41 JVMSv1.7 ... iconst_0 istore_1 goto 8 wrong it needs to be 6 iinc 1 1 iload_1 bipush 100
         * if_icmplt 5 this is WRONG it needs to be -6 // no return. corresponding to int i; for(i = 0; i < 100;i++) { ;
         * // empty loop body } return;
         */
        boolean [] testvalues = {true};
        for (BranchInfo bi : branches) {
            //expectedValues[0] = bi.getExpected();
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
            instructions[15] = (byte) Bytecodes.NOP;

            // instructions[14] = (byte) Bytecodes.RETURN;
            initialiseFrameForCompilation(instructions, "(II)I");
            theCompiler.offlineT1XCompile(anMethod, codeAttr, instructions, 15);


            AMD64MacroAssembler masm = theCompiler.getMacroAssembler();
            System.out.println("CB size=" + masm.codeBuffer.position());

            for (int i = 0; i < masm.codeBuffer.position(); i++) {
                int n = masm.codeBuffer.getByte(i);
                System.out.println(Integer.toHexString(n));
            }
            //masm.pop(Aarch64Assembler.ConditionFlag.AL, 1);
            //masm.pop(32, Aarch64.r0);
            //long[] registerValues = generateAndTest(expectedValues, testvalues, bitmasks);
            //assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + Long.toString(registerValues[0], 16) + " " + Long.toString(expectedValues[0], 16);
            theCompiler.cleanup();
        }
    }
}
