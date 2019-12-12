/*
 * Copyright (c) 2018, 2019, APT Group, School of Computer Science,
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
package com.oracle.max.vm.tests.crossisa.aarch64.adapters;

import static com.sun.max.vm.MaxineVM.*;

import java.io.*;

import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.aarch64.*;
import com.oracle.max.vm.tests.crossisa.aarch64.asm.*;
import com.sun.max.ide.*;
import com.sun.max.platform.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.AdapterGenerator.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * This test harness enables the testing of method calls from either of Maxine's calling conventions to
 * the other via adapter calls and adapter frames. We also test the prologues, address patching and some
 * aspects of the frame layout. By explicitly laying out the code, we know a priori the offsets of the
 * method call entry point (baseline/optimised) and the adapter entry point. Currently the code layout is:
 * <pre>
 * +-----------+
 * |  Prelude  |
 * +-----------+
 * |   Method  |
 * +-----------+
 * |  Adapter  |
 * +-----------+
 * </pre>
 * The prelude sets up the environment for the method call and mimics a call from one of the calling conventions.
 * The method is a synthetic method complete with prologue for the other of Maxine's calline conventions.
 * The method performs the computation which enables the test to be validated. Since the method follows
 * the prelude and the calling conventions entry point in a Maxine method is fixed, a known branch offset
 * can be emitted in the prelude to enter the method prologue at the correct offset.
 * The Adapter follows the method, this enables the adapter call in the method prologue to be patched once
 * the method has been compiled.
 * NB it is possible to layout the code in the order of adapter, prelude, method then all of the critical relative offsets
 * are known before compilation and branches to a known location can be emitted. However the current proposed layout
 * enables the additional test of address patching.
 *
 */
public abstract class Aarch64AdapterTest extends MaxTestCase {

    /** the code attribute for the method. */
    private CodeAttribute codeAttr;

    /** the target method for the test. */
    private StaticMethodActor method;


    protected Aarch64MacroAssembler masm;

    /** The Java signature of the target method. */
    private String javaSignature;

    private static final OptionSet options = new OptionSet(false);
    private static VMConfigurator vmConfigurator = null;
    private static boolean initialised = false;

    protected T1X t1x;
    protected Aarch64T1XCompilationTest t1xCompiler;

    protected static MaxineAarch64Tester.BitsFlag[] bitmasks       = new MaxineAarch64Tester.BitsFlag[MaxineAarch64Tester.NUM_REGS];
    static {
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
    }

    protected static long[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    protected static boolean[] testValues = new boolean[MaxineAarch64Tester.NUM_REGS];

    /**
     * Creates an adapter test for a method with the given signature.
     * @param signature
     * @throws Exception
     */
    public Aarch64AdapterTest(String signature) throws Exception {
        this();
        javaSignature = signature;
        initTest();
    }

    protected Aarch64AdapterTest() {
        String [] args = {"adapters", "unit test", "aarch64"};
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
        if (!initialised) {
            Platform.set(Platform.parse("linux-aarch64"));
            vmConfigurator.create();
            AdapterGenerator.initialiseForOfflineCrossISAtesting();
            CompilationBroker.setNeedOfflineAdapters(true);
            vm().compilationBroker.setOffline(true);
            vm().phase = Phase.HOSTED_TESTING;
            JavaPrototype.initialize(false);
            initialised = true;
        }
        t1x = (T1X) CompilationBroker.addCompiler("t1x", baselineCompilerName);
        //c1x = (C1X) CompilationBroker.addCompiler("c1x", optimizingCompilerName);
        //c1x.initialize(Phase.HOSTED_TESTING);
        t1x.initialize(Phase.HOSTED_TESTING);
        t1xCompiler = (Aarch64T1XCompilationTest) t1x.getT1XCompilation();
        masm = new Aarch64MacroAssembler(Platform.target(), null);
    }

    /**
     * @return
     */
    protected ClassMethodActor method() {
        return method;
    }

    /**
     *
     * @return
     */
    protected CodeAttribute codeAttribute() {
        return codeAttr;
    }

    /**
     * @throws Exception
     */
    private void initTest() throws Exception {
        initMethod(new byte[15], javaSignature);
        t1xCompiler.do_initFrameTests(method, codeAttr);
    }


    private void generateCode() throws Exception {
        Adapter adapter = createAdapter(new Sig(method));
        FileOutputStream fos = new FileOutputStream("adapter_test.bin");
        fos.write(createPrelude());
        fos.write(createMethod(adapter));
        fos.write(adapter.code());
        fos.close();
    }

    /**
     * Called by sub classes to run the test.
     * @param expected
     * @param tests
     * @param masks
     * @return
     * @throws Exception
     */
    protected long[] generateAndTest(long[] expected, boolean[] tests, MaxineAarch64Tester.BitsFlag[] masks) throws Exception {
        generateCode();
        MaxineAarch64Tester r = new MaxineAarch64Tester(expected, tests, masks);
        r.cleanFiles();
        r.cleanProcesses();
        r.assemble("adapter_test.s", "adapter_test.o");
        r.link("test_aarch64_adapters.ld", "startup_aarch64.o", "adapter_test.o");
        long[] simulatedRegisters = r.runRegisteredSimulation();
        r.cleanProcesses();
        //r.cleanFiles();
        return simulatedRegisters;
    }


    /**
     *
     * @param code
     * @param signature
     */
    protected void initMethod(byte [] code, String signature) {
        SignatureDescriptor sigDescriptor = SignatureDescriptor.create(signature);
        int numLocals = sigDescriptor.numberOfSlots;
        codeAttr = new CodeAttribute(null, code, (char) 2, (char) numLocals, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        method = new StaticMethodActor(null, SignatureDescriptor.create(signature), Actor.JAVA_METHOD_FLAGS, codeAttr, new String());
    }


    /**
     * Create the code which calls the method under test. This code mimics the
     * caller and should set up the appropriate incoming parameters e.g. for a
     * optimised method calling baseline, parameter registers should be seeded,
     * and spilled parameters (if any) should be stacked. The prelude then emits a branch
     * with link into the method at the appropriate entry point (in this example optimised
     * entry point). The entry point will be ep + 8 (4 for bl + 4 for ret) as the method
     * immediately follows the prelude in the object file. e.g.
     * <code>
     * mov x0, 0xFF      // seed register
     * bl (8 + 8)       // bl over the ret + 8 (optimised ep)
     * ret lr           // return to startup_aarch64.o
     * </code>
     * @return
     */
    public abstract byte [] createPrelude();

    /**
     * Create the code of the method which is called via the adapter. The code needs to
     * copy the result to the return register before epilogue emission and patch the
     * branch to the adapter in the prologue. Since the adapter immediately follows
     * the method in the object file the address to patch is:
     *  <code>code.length - adapter.callOffsetInPrologue()</code>
     * where code is from the methods codeBuffer.
     *
     * @param adapter
     * @return
     */
    public abstract byte [] createMethod(Adapter adapter);

    /**
     * Creates the appropriate Adapter for the test (i.e. Opt2Baseline/Baseline2Opt
     * and the signature)
     * @param sig
     * @return
     * @throws Exception
     */
    public abstract Adapter createAdapter(Sig sig) throws Exception;


}
