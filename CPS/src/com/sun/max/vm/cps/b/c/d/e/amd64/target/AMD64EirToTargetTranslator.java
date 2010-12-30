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
package com.sun.max.vm.cps.b.c.d.e.amd64.target;

import static com.sun.max.vm.MaxineVM.*;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.cps.target.amd64.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Laurent Daynes
 */
public final class AMD64EirToTargetTranslator extends EirToTargetTranslator {

    public AMD64EirToTargetTranslator(TargetGeneratorScheme targetGeneratorScheme) {
        super(targetGeneratorScheme, ISA.AMD64, AMD64TargetMethodUtil.registerReferenceMapSize());
    }

    @Override
    public AMD64OptimizedTargetMethod createIrMethod(ClassMethodActor classMethodActor) {
        final AMD64OptimizedTargetMethod targetMethod = new AMD64OptimizedTargetMethod(classMethodActor);
        notifyAllocation(targetMethod);
        return targetMethod;
    }

    @Override
    protected EirTargetEmitter createEirTargetEmitter(EirMethod eirMethod) {
        AdapterGenerator adapterGenerator = AdapterGenerator.forCallee(eirMethod.classMethodActor(), eirMethod.abi.targetABI().callEntryPoint);
        return new AMD64EirTargetEmitter((AMD64EirABI) eirMethod.abi, eirMethod.frameSize(), vm().safepoint, adapterGenerator);
    }

}
