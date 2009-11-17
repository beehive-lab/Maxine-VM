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
package test.com.sun.max.vm.compiler;

import java.io.*;

import junit.extensions.*;
import junit.framework.Test;

import org.junit.*;

import com.sun.max.*;
import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.ide.*;
import com.sun.max.profile.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.interpreter.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.prototype.*;

public abstract class CompilerTestSetup<Method_Type> extends TestSetup {

    private static CompilerTestSetup compilerTestSetup = null;

    public static CompilerTestSetup compilerTestSetup() {
        return compilerTestSetup;
    }

    protected abstract VMConfiguration createVMConfiguration();

    protected CompilerTestSetup(Test test) {
        super(test);
        compilerTestSetup = this;
    }

    private static JavaPrototype javaPrototype = null;

    public static JavaPrototype javaPrototype() {
        return javaPrototype;
    }

    /**
     * Gets a disassembler for a given target method.
     *
     * @param targetMethod a compiled method whose {@linkplain TargetMethod#code() code} is to be disassembled
     * @return a disassembler for the ISA specific code in {@code targetMethod} or null if no such disassembler is available
     */
    public final Disassembler disassemblerFor(TargetMethod targetMethod) {
        return Disassemble.createDisassembler(VMConfiguration.target().platform().processorKind, targetMethod.codeStart(), InlineDataDecoder.createFrom(targetMethod.encodedInlineDataDescriptors()));
    }

    protected void chainedSetUp() {
        javaPrototype = createJavaPrototype();
        javaPrototype.vmConfiguration().initializeSchemes(Phase.RUNNING);
        compilerScheme().compileSnippets();
    }

    protected JavaPrototype createJavaPrototype() {
        final PrototypeGenerator prototypeGenerator = new PrototypeGenerator(new OptionSet());
        Trace.on(1);
        return prototypeGenerator.createJavaPrototype(createVMConfiguration(), false);
    }

    private boolean setupGuard;

    /**
     * This is final so that errors occurring during setup are caught and reported - the JUnit
     * harness seems to silently swallow such errors and exit.
     */
    @Before
    @Override
    protected final void setUp() {
        try {
            assert !setupGuard;
            setupGuard = true;
            chainedSetUp();
        } catch (Error error) {
            error.printStackTrace();
            throw error;
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } finally {
            setupGuard = false;
        }
    }

    @After
    @Override
    protected final void tearDown() {
        javaPrototype.vmConfiguration().finalizeSchemes(Phase.RUNNING);
        ClassfileReader.writeClassfilesToJar(new File(JavaProject.findVcsProjectDirectory(), "loaded-classes.jar"));
        if (Trace.hasLevel(1)) {
            GlobalMetrics.report(Trace.stream());
        }
    }

    private static void writeGeneratedClassfilesToJar() throws IOException {
        final Map<String, byte[]> generatedClassfiles = BootClassLoader.BOOT_CLASS_LOADER.generatedClassfiles();
        if (generatedClassfiles.isEmpty()) {
            return;
        }
        final File jarFile = new File(JavaProject.findVcsProjectDirectory(), "generated-classes.jar");
        if (!jarFile.getParentFile().exists()) {
            if (!jarFile.getParentFile().mkdir()) {
                throw new IOException("could not create missing directory " + jarFile.getParentFile());
            }
        }
        final JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile));
        jarOutputStream.setLevel(Deflater.BEST_COMPRESSION);
        for (Map.Entry<String, byte[]> entry : generatedClassfiles.entrySet()) {
            final String classfilePath = entry.getKey().replace('.', '/') + ".class";
            final JarEntry jarEntry = new JarEntry(classfilePath);
            jarEntry.setTime(System.currentTimeMillis());
            jarOutputStream.putNextEntry(jarEntry);
            jarOutputStream.write(entry.getValue());
            jarOutputStream.closeEntry();
        }
        jarOutputStream.close();
        System.out.println("saved generated classfiles in " + jarFile.getAbsolutePath());
    }

    public static BootstrapCompilerScheme compilerScheme() {
        return javaPrototype().vmConfiguration().bootCompilerScheme();
    }

    public abstract Method_Type translate(ClassMethodActor classMethodActor);

    protected IrInterpreter<? extends IrMethod> createInterpreter() {
        return null;
    }

    public static PackageLoader packageLoader() {
        return javaPrototype.packageLoader();
    }

}
