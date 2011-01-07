/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.com.sun.max.vm.compiler.c1x;

import java.lang.reflect.*;

import test.com.sun.max.vm.compiler.*;

import com.sun.c1x.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.graph.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.platform.*;
import com.sun.max.test.*;
import com.sun.max.test.JavaExecHarness.Executor;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.hosted.*;

public class HIRTestExecutor implements Executor {
    private static HIRGenerator generator;
    public static Utf8Constant testMethod = SymbolTable.makeSymbol("test");
    public static MaxRiRuntime runtime;

    private static void initialize(boolean loadingPackages) {
        C1XOptions.setOptimizationLevel(Integer.parseInt(JavaTester.options.getStringValue("c1x-optlevel")));
        JavaPrototype.initialize(loadingPackages);
        C1XCompilerScheme compilerScheme = new C1XCompilerScheme();

        runtime = compilerScheme.runtime;
        // create MaxineRuntime
        generator = new HIRGenerator(runtime, Platform.platform().target, compilerScheme.compiler());
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
            args[i] = IRInterpreter.fromBoxedJavaValue(vals[i]);
        }
        final ClassMethodActor classMethodActor = (ClassMethodActor) c.slot2;
        final IR method = generator.makeHirMethod(classMethodActor);
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
            C1XCompilation compilation = new C1XCompilation(compiler, classMethodActor, -1);
            return compilation.emitHIR();
        }
    }
}
