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

import java.io.*;
import java.lang.reflect.*;

import com.sun.c1x.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.target.*;
import com.sun.max.program.option.*;
import com.sun.max.program.option.OptionSet.*;
import com.sun.max.test.*;
import com.sun.max.test.JavaExecHarness.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.prototype.*;


public class HIRTestExecutor implements Executor {

    private static final OptionSet options = new OptionSet(false);
    private static final Option<Boolean> targetOption = options.newBooleanOption("target", false,
                                                                                 "Compile the method(s) all the way to target code.");

    private static final Option<Integer> traceOption = options.newIntegerOption("trace", 0,
        "Set the tracing level of the Maxine VM and runtime.");
    private static final Option<Integer> verboseOption = options.newIntegerOption("verbose", 1,
        "Set the verbosity level of the testing framework.");
    private static final Option<Boolean> printBailoutOption = options.newBooleanOption("print-bailout", false,
        "Print bailout exceptions.");
    private static final Option<File> outFileOption = options.newFileOption("o", (File) null,
        "A file to which output should be sent. If not specified, then output is sent to stdout.");
    private static final Option<Boolean> clinitOption = options.newBooleanOption("clinit", true,
        "Compile class initializer (<clinit>) methods");
    private static final Option<Boolean> failFastOption = options.newBooleanOption("fail-fast", true,
        "Stop compilation upon the first bailout.");
    private static final Option<Boolean> c1xOptionsOption = options.newBooleanOption("c1x-options", false,
        "Print settings of C1XOptions.");
    private static final Option<Boolean> averageOption = options.newBooleanOption("average", true,
        "Report only the average compilation speed.");
    private static final Option<Boolean> helpOption = options.newBooleanOption("help", false,
        "Show help message and exit.");

    static {
        // add all the fields from C1XOptions as options
        options.addFieldOptions(C1XOptions.class, "XX");
        // add a special option "c1x-optlevel" which adjusts the optimization level
        options.addOption(new Option<Integer>("c1x-optlevel", -1, OptionTypes.INT_TYPE, "Set the overall optimization level of C1X (-1 to use default settings)") {
            @Override
            public void setValue(Integer value) {
                C1XOptions.setOptimizationLevel(value);
            }
        }, Syntax.REQUIRES_EQUALS);
    }

    private static HIRGenerator generator;
    public static Utf8Constant testMethod = SymbolTable.makeSymbol("test");
    public static final MaxRiRuntime runtime = new MaxRiRuntime();
    public static final MaxInterpreterInterface interpreterInterface = new MaxInterpreterInterface(runtime);

    private static void initialize(boolean loadingPackages) {
        new PrototypeGenerator(new OptionSet()).createJavaPrototype(false);
        ClassActor.prohibitPackagePrefix(null); // allow extra classes when testing, but not actually prototyping/bootstrapping
        final Target target = createTarget();
        final C1XCompiler compiler = new C1XCompiler(runtime, target);

        // create MaxineRuntime
        generator = new HIRGenerator(runtime, target, compiler);
    }

    private static Target createTarget() {
        // TODO: configure architecture according to host platform
        final Architecture arch = Architecture.findArchitecture("amd64");
        return new Target(arch, arch.registers, arch.registers, 1024, true);
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
        final IRInterpreter interpreter = new IRInterpreter(runtime, interpreterInterface);
        final CiConstant result = interpreter.execute(method, args);
        return result.boxedValue();
    }

    public static class HIRGenerator {
<<<<<<< local
        private RiRuntime ciRuntime;
=======
        private RiRuntime riRuntime;
>>>>>>> other
        private Target target;
        private C1XCompiler compiler;

        /**
         * Creates a new HIR generator.
         * @param runtime the runtime
         * @param target the target
         * @param compiler the compiler
         */
        public HIRGenerator(RiRuntime runtime, Target target, C1XCompiler compiler) {
<<<<<<< local
            this.ciRuntime = runtime;
=======
            this.riRuntime = runtime;
>>>>>>> other
            this.target = target;
            this.compiler = compiler;
        }

        /**
         * @param classMethodActor the method for which to make the IR
         * @return the IR for the method
         */
        public IR makeHirMethod(RiMethod classMethodActor) {
<<<<<<< local
            C1XCompilation compilation = new C1XCompilation(compiler, target, ciRuntime, classMethodActor);
=======
            C1XCompilation compilation = new C1XCompilation(compiler, target, riRuntime, classMethodActor);
>>>>>>> other
            compilation.compile();
            return compilation.hir();
        }
    }
}
