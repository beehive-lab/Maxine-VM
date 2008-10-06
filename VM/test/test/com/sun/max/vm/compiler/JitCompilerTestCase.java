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
/*VCSID=91abd692-8234-46a9-8a02-8be76c1e3954*/
package test.com.sun.max.vm.compiler;

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
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.jit.JitTargetMethod.*;
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
        _compiler = null;
    }

    public JitCompilerTestCase(String name) {
        super(name);
    }

    private static Class< ? > _testCaseClass = null;

    @Override
    protected void setUp() {
        // We want to reset the compiler only when we change the JitCompilerTestCase sub-class.
        // This is mostly to handle AutoTest
        if (_testCaseClass != this.getClass()) {
            resetCompiler();
            _testCaseClass = this.getClass();
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

    private static JitCompiler _compiler = null;

    private JitCompiler compiler() {
        if (_compiler == null) {
            final TemplateTable templateTable = new TemplateTable(templateSources());
            _compiler = newJitCompiler(templateTable);
        }
        return _compiler;
    }

    protected void resetCompiler() {
        _compiler = null;
    }

    protected Class initializeClassInTarget(final Class classToInitialize) {
        return MaxineVM.usingTarget(new Function<Class>() {
            public Class call() {
                assert !MaxineVM.isPrototypeOnly(classToInitialize);
                final Class targetClass = Classes.load(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, classToInitialize.getName());
                final ClassActor classActor = ClassActor.fromJava(targetClass);
                classActor.makeInitialized();
                return targetClass;
            }
        });
    }

    @Override
    protected JitTargetMethod compileMethod(final ClassMethodActor classMethodActor) {
        Trace.line(1, "Compiling " + classMethodActor.name());
        return MaxineVM.usingTarget(new Function<JitTargetMethod>() {
            public JitTargetMethod call() {
                try {
                    final JitTargetMethod method = compiler().compile(classMethodActor, CompilationDirective.DEFAULT);
                    assertNotNull(method);
                    traceBundleAndDisassemble(method);
                    Trace.line(1);
                    Trace.line(1);
                    return method;
                } catch (AssertionError e) {
                    throw ProgramError.unexpected("assertion failure while compiling: " + classMethodActor, e);
                }
            }
        });
    }

    @Override
    protected JitTargetMethod compileMethod(String methodName) {
        return compileMethod(methodName, SignatureDescriptor.create("()V"));
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
            final Sequence<CodeTranslation> codeTranslations = jitTargetMethod.codeTranslations();
            final Disassembler disassembler = disassemblerFor(targetMethod);
            if (disassembler == null) {
                return;
            }
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            final CodeAttribute codeAttribute = targetMethod.classMethodActor().codeAttribute();
            final ConstantPool constantPool = codeAttribute.constantPool();
            for (CodeTranslation codeTranslation : codeTranslations) {
                final BytecodeBlock bytecodeBlock = codeTranslation.toBytecodeBlock(codeAttribute.code());

                if (bytecodeBlock != null) {
                    final String bytecode = BytecodePrinter.toString(constantPool, bytecodeBlock, "", "\n", 0);
                    INDENT_WRITER.println();
                    INDENT_WRITER.println(bytecode);
                }
                if (codeTranslation.targetCodeLength() != 0) {
                    disassembler.setCurrentPosition(codeTranslation.targetCodePosition());
                    disassembler.scanAndPrint(new BufferedInputStream(new ByteArrayInputStream(targetCode, codeTranslation.targetCodePosition(), codeTranslation.targetCodeLength())), buffer);
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
}
