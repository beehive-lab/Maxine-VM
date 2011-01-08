/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.cps.tir;

import static com.sun.max.lang.Classes.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;

import java.lang.reflect.*;

import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.test.*;
import com.sun.max.test.JavaExecHarness.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.hotpath.compiler.Console.*;
import com.sun.max.vm.cps.ir.interpreter.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.value.*;

public class HotpathExecutor implements JavaExecHarness.Executor {

    private static OptionSet optionSet = new OptionSet();
    private static Option<Boolean> enableHotpath = optionSet.newBooleanOption("HP", true, "Enable Hotpath.");
    private static Option<Boolean> enableHost = optionSet.newBooleanOption("E", false, "Execute on host vm.");
    private static Option<Integer> hotpathThreshold = optionSet.newIntegerOption("HP:T", 10, "Hotpath tracing threshold.");

    static {
        optionSet.addOptions(BirInterpreter.optionSet);
        optionSet.addOptions(HotpathProfiler.optionSet);
        optionSet.addOptions(TirRecorder.optionSet);
        optionSet.addOptions(Tracer.optionSet);
        optionSet.addOptions(AsynchronousProfiler.optionSet);
        optionSet.addOptions(IrInterpreter.options);
    }

    public Object execute(JavaTestCase testCase, Object[] arguments) throws InvocationTargetException {
        final StaticMethodActor staticMethodActor = (StaticMethodActor) testCase.slot2;
        Console.print(Color.LIGHTYELLOW, "Executing Testcase: " + staticMethodActor.toString() + ", Arguments: ");
        printValues(arguments);
        Console.println();
        final BirInterpreter.Profiler profiler;
        if (enableHotpath.getValue()) {
            profiler = new HotpathProfiler();
        } else {
            profiler = null;
        }
        final BirInterpreter interpreter = new BirInterpreter(profiler);
        final Value[] argumentsValues = Value.fromBoxedJavaValues(arguments);
        final Value resultValue;
        try {
            if (enableHost.getValue()) {
                resultValue = staticMethodActor.invoke(argumentsValues);
            } else {
                resultValue = interpreter.execute(staticMethodActor, argumentsValues);
            }
        } catch (InvocationTargetException e) {
            e.getCause().printStackTrace();
            throw e;
        } catch (ProgramError error) {
            error.printStackTrace();
            error.getCause().printStackTrace();
            throw new InvocationTargetException(error.getCause());
        } catch (IllegalAccessException e) {
            throw new InvocationTargetException(e);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
        Console.print(Color.LIGHTYELLOW, "      Result Value: ");
        printValues(resultValue);
        Console.println();
        return staticMethodActor.resultKind().convert(resultValue).asBoxedJavaValue();
    }

    private void printValues(Object... arguments) {
        Console.print("(");
        for (int i = 0; i < arguments.length; i++) {
            Console.print(Color.LIGHTGREEN, arguments[i].toString());
            if (i < arguments.length - 1) {
                Console.print(Color.WHITE, ", ");
            }
        }
        Console.print(")");
    }

    public void initialize(JavaTestCase testCase, boolean loadingPackages) {
        final ClassActor classActor = ClassActor.fromJava(testCase.clazz);
        final StaticMethodActor staticMethodActor = (StaticMethodActor) MethodActor.fromJava(getDeclaredMethod(testCase.clazz, "test"));
        if (staticMethodActor != null) {
            testCase.slot1 = classActor;
            testCase.slot2 = staticMethodActor;
        } else {
            ProgramError.unexpected("Could not find static test() method.");
        }
    }

    public static void main(String[] arguments) {
        setUp();
        optionSet.parseArguments(arguments);
        final Registry<TestHarness> registry = new Registry<TestHarness>(TestHarness.class, true);
        final JavaExecHarness harness = new JavaExecHarness(new HotpathExecutor());
        registry.registerObject("java", harness);
        final TestEngine engine = new TestEngine(registry);
        Console.println(Color.LIGHTGREEN, "Executing Tests ...");
        engine.parseAndRunTests(optionSet.getArguments());
        engine.report(System.out);
        AsynchronousProfiler.print();
    }

    // Checkstyle: stop
    public static void setUp() {
        Console.out().println(Color.LIGHTGREEN, "Initializing ...");
        VMConfigurator vm = new VMConfigurator(optionSet);
        Trace.addTo(optionSet);
        vm.create(true);
        JavaPrototype.initialize(false);
        vmConfig().initializeSchemes(Phase.RUNNING);
        CPSCompiler compiler = CPSCompiler.Static.compiler();
        compiler.compileSnippets();
        Console.println(Color.LIGHTGREEN, "Compiler Scheme: " + compiler);
    }

    protected static void createVMConfiguration() {
        Platform.set(platform().constrainedByInstructionSet(ISA.SPARC));
        VMConfigurator.installStandard(BuildLevel.DEBUG, new com.sun.max.vm.cps.b.c.d.Package());
    }
}
