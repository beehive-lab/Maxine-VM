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

import java.lang.reflect.*;

import com.sun.max.platform.*;
import com.sun.max.program.option.*;
import com.sun.max.test.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.ir.interpreter.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.value.*;


/**
 * The {@code CIRTestHarness} class implements a test harness that is capable
 * of running a Java program through the CIR compiler and interpreter and checking that the
 * outputs match the expected outputs (either a value or an exception).
 *
 * @author Ben L. Titzer
 */
public class CIRTestExecutor implements JavaExecHarness.Executor {

    private static CirGenerator generator;

    private static Utf8Constant testMethod = SymbolTable.makeSymbol("test");

    private static void initialize(boolean loadingPackages) {
        final PrototypeGenerator prototypeGenerator = new PrototypeGenerator(new OptionSet());
        final VMConfiguration cfg = VMConfigurations.createStandard(BuildLevel.DEBUG, Platform.host(),
                        new com.sun.max.vm.compiler.b.c.Package());
        final Prototype jpt = prototypeGenerator.createJavaPrototype(cfg, loadingPackages);
        final CirGeneratorScheme compilerScheme = (CirGeneratorScheme) jpt.vmConfiguration().compilerScheme();
        compilerScheme.compileSnippets();
        generator = compilerScheme.cirGenerator();
        ClassActor.prohibitPackagePrefix(null); // allow extra classes when testing, but not actually prototyping/bootstrapping
    }

    public void initialize(JavaExecHarness.JavaTestCase c, boolean loadingPackages) {
        if (generator == null) {
            initialize(loadingPackages);
        }

        final ClassActor classActor = ClassActor.fromJava(c.clazz);
        c.slot1 = classActor;
        c.slot2 = classActor.findLocalStaticMethodActor(testMethod);
    }

    public Object execute(JavaExecHarness.JavaTestCase c, Object[] vals) throws InvocationTargetException {
        final Value[] args = new Value[vals.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = Value.fromBoxedJavaValue(vals[i]);
        }
        final ClassMethodActor classMethodActor = (ClassMethodActor) c.slot2;
        final CirMethod method = generator.makeIrMethod(classMethodActor);
        final CirInterpreter interpreter = new CirInterpreter(generator);
        final Value result = interpreter.execute(method, args);
        return result.asBoxedJavaValue();
    }

}
