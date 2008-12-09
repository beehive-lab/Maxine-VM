/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package test.com.sun.max.vm.jit.amd64;

import java.io.*;

import junit.framework.*;
import test.com.sun.max.vm.compiler.*;
import test.com.sun.max.vm.compiler.amd64.*;

import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.asm.dis.amd64.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.b.c.d.e.amd64.target.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * A playfield for experimenting with adapter frame implementation and printing out the resulting code. Not a regression test.
 *
 * @author Laurent Daynes
 */
public class JITTest_adapterFrameGenerator extends CompilerTestCase {

    public void generateAdapters(ClassMethodActor classMethodActor) throws AssemblyException {
        final EirGenerator eirGenerator = ((AMD64EirGeneratorScheme) AMD64TranslatorTestSetup.targetGenerator().compilerScheme()).eirGenerator();
        final EirABIsScheme eirABIsScheme = eirGenerator.eirABIsScheme();
        final EirABI eirAbi = eirABIsScheme.getABIFor(classMethodActor);

        Trace.line(1, "JIT => optimizing adapter for method " + classMethodActor.name());
        generateAdapters(new AMD64Assembler(), AMD64AdapterFrameGenerator.jitToOptimizingCompilerAdapterFrameGenerator(classMethodActor, eirAbi));
        Trace.line(1, "\n\nOptimizing => JIT  adapter for method " + classMethodActor.name());
        generateAdapters(new AMD64Assembler(), AMD64AdapterFrameGenerator.optimizingToJitCompilerAdapterFrameGenerator(classMethodActor, eirAbi));
    }

    public void generateAdapters(AMD64Assembler assembler, AMD64AdapterFrameGenerator adapterFrameGenerator)  throws AssemblyException {
        final Label methodEntryPoint = new Label();
        adapterFrameGenerator.emitPrologue(assembler);
        assembler.bindLabel(methodEntryPoint);
        assembler.add(AMD64GeneralRegister64.RAX, AMD64GeneralRegister64.RCX);
        assembler.nop();
        assembler.nop();
        assembler.ret();
        adapterFrameGenerator.emitEpilogue(assembler);
        final byte[] code = assembler.toByteArray();
        assert methodEntryPoint.position() % 8 == 0;
        final AMD64Disassembler disassembler = new AMD64Disassembler(0L, null);
        final BufferedInputStream bufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(code));

        try {
            while (bufferedInputStream.available() > 0) {
                disassembler.print(Trace.stream(), disassembler.scanOne(bufferedInputStream));
            }
        } catch (Throwable throwable) {
            System.err.println("could not disassemble any further: " + throwable);
        }
    }


    public int method1(int x, int y) {
        return x * y;
    }

    public void test_adapter_method1() throws AssemblyException {
        generateAdapters(getClassMethodActor("method1", SignatureDescriptor.create(int.class, int.class, int.class)));
    }

    public void test_jni_function_entryPoints() {
        final CriticalMethod[] jniFunctions = JniNativeInterface.jniFunctions();
        final CompilerScheme compiler = VMConfiguration.target().compilerScheme();
        for (CriticalMethod m : jniFunctions) {
            final ClassMethodActor classMethodActor = m.classMethodActor();
            final TargetMethod targetMethod = (TargetMethod) compiler.compile(classMethodActor, CompilationDirective.DEFAULT);
            final Pointer entryPoint = targetMethod.getEntryPoint(CallEntryPoint.C_ENTRY_POINT).asPointer();
            assert targetMethod.abi().callEntryPoint().equals(CallEntryPoint.C_ENTRY_POINT);
            assert entryPoint.equals(targetMethod.codeStart());
        }
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(JITTest_adapterFrameGenerator.suite());
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(JITTest_adapterFrameGenerator.class.getSimpleName());
        suite.addTestSuite(JITTest_adapterFrameGenerator.class);
        return new AMD64JITTestSetup(suite);
    }

}
