/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
/**
 *
 */
package test.com.sun.max.vm.cps;

import java.lang.reflect.*;

import com.sun.max.test.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.ir.interpreter.eir.*;
import com.sun.max.vm.cps.ir.interpreter.eir.amd64.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.value.*;

/**
 * The {@code CIRTestHarness} class implements a test harness that is capable
 * of running a Java program through the CIR compiler and interpreter and checking that the
 * outputs match the expected outputs (either a value or an exception).
 *
 * @author Ben L. Titzer
 */
public class EIRTestExecutor implements JavaExecHarness.Executor {

    private static EirGenerator generator;

    public static Utf8Constant testMethod = SymbolTable.makeSymbol("test");

    private static void initialize(boolean loadingPackages) {
        JavaPrototype.initialize(loadingPackages);
        final EirGeneratorScheme compilerScheme = (EirGeneratorScheme) CPSCompiler.Static.compiler();
        compilerScheme.compileSnippets();
        generator = compilerScheme.eirGenerator();
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
        final EirMethod method = (EirMethod) generator.makeIrMethod(classMethodActor, true);
        final EirInterpreter interpreter = new AMD64EirInterpreter((AMD64EirGenerator) generator);
        final Value result = interpreter.execute(method, args);
        return result.asBoxedJavaValue();
    }

}
