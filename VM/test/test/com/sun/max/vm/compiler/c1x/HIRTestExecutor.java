/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package test.com.sun.max.vm.compiler.c1x;

import java.lang.reflect.*;

import test.com.sun.max.vm.compiler.*;

import com.sun.c1x.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ri.*;
import com.sun.max.program.option.*;
import com.sun.max.test.*;
import com.sun.max.test.JavaExecHarness.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.prototype.*;


public class HIRTestExecutor implements Executor {
    private static HIRGenerator generator;
    public static Utf8Constant testMethod = SymbolTable.makeSymbol("test");
    public static final MaxRiRuntime runtime = new MaxRiRuntime();

    private static void initialize(boolean loadingPackages) {
        Iterable<Option<?>> opt = JavaTester.options.getOptions();
        for (Option x : opt) {
            if (x.getName().equals("c1x-optlevel")) {
                C1XOptions.setOptimizationLevel((Integer) x.getDefaultValue());
            }
        }
        new PrototypeGenerator(new OptionSet()).createJavaPrototype(false);
        ClassActor.prohibitPackagePrefix(null); // allow extra classes when testing, but not actually prototyping/bootstrapping
        final CiTarget target = createTarget();
        final C1XCompiler compiler = new C1XCompiler(runtime, target);

        // create MaxineRuntime
        generator = new HIRGenerator(runtime, target, compiler);
    }

    private static CiTarget createTarget() {
        return C1XCompilerScheme.createTarget(MaxRiRuntime.globalRuntime, VMConfiguration.hostOrTarget());
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
        final CiConstant[] args = new CiConstant[vals.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = CiConstant.fromBoxedJavaValue(vals[i]);
        }
        final ClassMethodActor classMethodActor = (ClassMethodActor) c.slot2;
        final IR method = generator.makeHirMethod(runtime.getRiMethod(classMethodActor));
        final IRInterpreter interpreter = new IRInterpreter(runtime, generator.compiler);
        final CiConstant result = interpreter.execute(method, args);
        return result.boxedValue();
    }

    public static class HIRGenerator {
        private RiRuntime riRuntime;
        private CiTarget target;
        private C1XCompiler compiler;

        /**
         * Creates a new HIR generator.
         * @param runtime the runtime
         * @param target the target
         * @param compiler the compiler
         */
        public HIRGenerator(RiRuntime runtime, CiTarget target, C1XCompiler compiler) {
            this.riRuntime = runtime;
            this.target = target;
            this.compiler = compiler;
        }

        /**
         * @param classMethodActor the method for which to make the IR
         * @return the IR for the method
         */
        public IR makeHirMethod(RiMethod classMethodActor) {
            C1XCompilation compilation = new C1XCompilation(compiler, target, riRuntime, null, classMethodActor);
            return compilation.emitHIR();
        }
    }
}
