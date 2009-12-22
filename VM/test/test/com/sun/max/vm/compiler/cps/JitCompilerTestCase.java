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
package test.com.sun.max.vm.compiler.cps;

import java.io.*;

import test.com.sun.max.vm.jit.*;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.template.source.*;
import com.sun.max.vm.type.*;

/**
 * Jit Compiler test case.
 *
 * @author Laurent Daynes
 */
public abstract class JitCompilerTestCase extends CompilerTestCase<JitTargetMethod> {
    public static final String UNRESOLVED_CLASS_NAME = UnresolvedAtTestTime.class.getName();
    public JitCompilerTestCase() {
        compiler = null;
    }

    public JitCompilerTestCase(String name) {
        super(name);
    }

    private static Class< ? > testCaseClass = null;

    @Override
    protected void setUp() {
        // We want to reset the compiler only when we change the JitCompilerTestCase sub-class.
        // This is mostly to handle AutoTest
        if (testCaseClass != this.getClass()) {
            resetCompiler();
            testCaseClass = this.getClass();
        }
    }

    private JITTestSetup jitTestSetup() {
        assert compilerTestSetup() instanceof JITTestSetup;
        return (JITTestSetup) compilerTestSetup();
    }

    private JitCompiler newJitCompiler(TemplateTable templateTable) {
        return jitTestSetup().newJitCompiler(templateTable);
    }

    @Override
    protected Disassembler disassemblerFor(TargetMethod targetMethod) {
        return jitTestSetup().disassemblerFor(targetMethod);
    }

    /**
     * Gets the default sources for the implementation of bytecode templates. Test programs should override this method
     * to test with a different set.
     *
     * @return
     */
    protected Class[] templateSources() {
        return new Class[]{UnoptimizedBytecodeTemplateSource.class};
    }

    private static JitCompiler compiler = null;

    private JitCompiler compiler() {
        if (compiler == null) {
            final TemplateTable templateTable = new TemplateTable(templateSources());
            compiler = newJitCompiler(templateTable);
        }
        return compiler;
    }

    protected void resetCompiler() {
        compiler = null;
    }

    protected Class initializeClassInTarget(final Class classToInitialize) {
        return MaxineVM.usingTarget(new Function<Class>() {
            public Class call() {
                assert !MaxineVM.isHostedOnly(classToInitialize);
                final Class targetClass = Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, classToInitialize.getName());
                final ClassActor classActor = ClassActor.fromJava(targetClass);
                MakeClassInitialized.makeClassInitialized(classActor);
                return targetClass;
            }
        });
    }

    @Override
    protected JitTargetMethod compileMethod(final ClassMethodActor classMethodActor) {
        Trace.line(2, "Compiling " + classMethodActor.name);
        return MaxineVM.usingTarget(new Function<JitTargetMethod>() {
            public JitTargetMethod call() {
                try {
                    final JitTargetMethod method = compiler().compile(classMethodActor);
                    assertNotNull(method);
                    if (Trace.hasLevel(1)) {
                        if (jitTestSetup().disassembleCompiledMethods()) {
                            traceBundleAndDisassemble(method);
                            Trace.line(1);
                            Trace.line(1);
                        }
                    }
                    return method;
                } catch (AssertionError e) {
                    throw ProgramError.unexpected("assertion failure while compiling: " + classMethodActor, e);
                }
            }
        });
    }

    @Override
    protected JitTargetMethod compileMethod(String methodName) {
        return compileMethod(methodName, SignatureDescriptor.VOID);
    }

    @Override
    protected void disassemble(TargetMethod targetMethod) {
        final JitTargetMethod jitTargetMethod = (JitTargetMethod) targetMethod;
        final byte[] targetCode = targetMethod.code();
        if (targetCode.length == 0) {
            return;
        }
        final int level = Trace.level();
        try {
            Trace.off();
            final Sequence<CodeTranslation> codeTranslations = codeTranslations(jitTargetMethod);
            final Disassembler disassembler = disassemblerFor(targetMethod);
            if (disassembler == null) {
                return;
            }
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            // Scan the code stream first without printing to fill out it label map:
            disassembler.scan(new BufferedInputStream(new ByteArrayInputStream(targetCode)));

            // Now re-scan and print the code stream in bytecode blocks:
            final CodeAttribute codeAttribute = targetMethod.classMethodActor().codeAttribute();
            final ConstantPool constantPool = codeAttribute.constantPool;
            for (CodeTranslation codeTranslation : codeTranslations) {
                final BytecodeBlock bytecodeBlock = codeTranslation.toBytecodeBlock(codeAttribute.code());

                if (bytecodeBlock != null) {
                    final String bytecode = BytecodePrinter.toString(constantPool, bytecodeBlock, "", "\n", 0);
                    INDENT_WRITER.println();
                    INDENT_WRITER.println(bytecode);
                }
                if (codeTranslation.targetCodeLength != 0) {
                    disassembler.setCurrentPosition(codeTranslation.targetCodePosition);
                    disassembler.scanAndPrint(new BufferedInputStream(new ByteArrayInputStream(targetCode, codeTranslation.targetCodePosition, codeTranslation.targetCodeLength)), buffer);
                    INDENT_WRITER.printLines(new ByteArrayInputStream(buffer.toByteArray()));
                }
                buffer.reset();
            }
        } catch (IOException e) {
            ProgramError.unexpected("disassembly failed for target method " + targetMethod + " :" + e.getMessage());
        } catch (AssemblyException e) {
            ProgramError.unexpected("disassembly failed for target method " + targetMethod + " :" + e.getMessage());
        } finally {
            Trace.on(level);
        }
    }

    /**
     * Gets a sequence of objects correlating bytecode ranges with the ranges of target code in this target method. The
     * returned sequence objects are exclusive of each other in terms of their target code ranges and they cover
     * every target code position in this target method.
     * @param jitTargetMethod the JIT target method
     * @return a sequence of code translations for each bytecode
     */
    public static Sequence<CodeTranslation> codeTranslations(JitTargetMethod jitTargetMethod) {
        final AppendableSequence<CodeTranslation> translations = new ArrayListSequence<CodeTranslation>();
        int startBytecodePosition = 0;
        int[] positionMap = jitTargetMethod.bytecodeToTargetCodePositionMap();
        int startTargetCodePosition = positionMap[0];
        assert startTargetCodePosition != 0;
        translations.append(new CodeTranslation(0, 0, 0, startTargetCodePosition));
        for (int bytecodePosition = 1; bytecodePosition != positionMap.length; ++bytecodePosition) {
            final int targetCodePosition = positionMap[bytecodePosition];
            if (targetCodePosition != 0) {
                final CodeTranslation codeTranslation = new CodeTranslation(startBytecodePosition, bytecodePosition - startBytecodePosition, startTargetCodePosition, targetCodePosition - startTargetCodePosition);
                translations.append(codeTranslation);
                startTargetCodePosition = targetCodePosition;
                startBytecodePosition = bytecodePosition;
            }
        }
        if (startTargetCodePosition < jitTargetMethod.code().length) {
            translations.append(new CodeTranslation(0, 0, startTargetCodePosition, jitTargetMethod.code().length - startTargetCodePosition));
        }
        return translations;
    }

    /**
     * Correlates a bytecode range with a target code range. The target code range is typically the template code
     * produced by the JIT compiler for a single JVM instruction encoded in the bytecode range.
     *
     * @author Doug Simon
     */
    static class CodeTranslation {

        final int bytecodePosition;
        final int bytecodeLength;
        final int targetCodePosition;
        final int targetCodeLength;

        /**
         * Creates an object that correlates a bytecode range with a target code range.
         *
         * @param bytecodePosition the first position in the bytecode range. This value is invalid if
         *            {@code bytecodeLength == 0}.
         * @param bytecodeLength the length of the bytecode range
         * @param targetCodePosition the first position in the target code range. This value is invalid if
         *            {@code targetCodeLength == 0}.
         * @param targetCodeLength the length of the target code range
         */
        public CodeTranslation(int bytecodePosition, int bytecodeLength, int targetCodePosition, int targetCodeLength) {
            this.bytecodeLength = bytecodeLength;
            this.bytecodePosition = bytecodePosition;
            this.targetCodeLength = targetCodeLength;
            this.targetCodePosition = targetCodePosition;
        }

        /**
         * Gets an object encapsulating the sub-range of a given bytecode array represented by this code translation.
         *
         * @param bytecode
         * @return null if {@code bytecodeLength() == 0}
         */
        public BytecodeBlock toBytecodeBlock(byte[] bytecode) {
            if (bytecodeLength == 0) {
                return null;
            }
            return new BytecodeBlock(bytecode, bytecodePosition, bytecodePosition + bytecodeLength - 1);
        }

        @Override
        public String toString() {
            final String bytecode = bytecodeLength == 0 ? "[]" : "[" + bytecodePosition + " - " + (bytecodePosition + bytecodeLength - 1) + "]";
            final String targetCode = targetCodeLength == 0 ? "[]" : "[" + targetCodePosition + " - " + (targetCodePosition + targetCodeLength - 1) + "]";
            return bytecode + " -> " + targetCode;
        }
    }
}
