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
package test.com.sun.max.tele.interpreter;

import static com.sun.max.lang.Classes.*;
import junit.framework.*;
import test.com.sun.max.vm.cps.*;

import com.sun.max.tele.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.b.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.interpreter.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.reference.hosted.*;

/**
 *  @author Athul Acharya
 */
public class TeleInterpreterTestSetup extends CompilerTestSetup<ActorIrMethod> {

    @Override
    protected IrInterpreter<ActorIrMethod> createInterpreter() {
        return new TeleIrInterpreter(null);
    }

    public TeleInterpreterTestSetup(Test test, TeleVM teleVM) {
        super(test);
    }

    public TeleInterpreterTestSetup(Test test) {
        super(test);
    }

    @Override
    protected void initializeVM() {
        RuntimeCompiler.optimizingCompilerOption.setValue(BCompiler.class.getName());
        RuntimeCompiler.baselineCompilerOption.setValue(null);
        VMConfigurator vmConfigurator = new VMConfigurator(null);
        vmConfigurator.buildLevel.setValue(BuildLevel.DEBUG);
        vmConfigurator.referenceScheme.setValue(getPackageName(HostedReference.class));
        vmConfigurator.create(true);
    }

    @Override
    public ActorIrMethod translate(ClassMethodActor classMethodActor) {
        return new ActorIrMethod(classMethodActor);
    }

}
