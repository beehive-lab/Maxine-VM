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
package com.sun.max.vm.cps.cir.dir;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.dir.*;

/**
 * A DIR generator that translates from CIR.
 *
 * @author Bernd Mathiske
 */
public class CirToDirTranslator extends DirGenerator {

    public CirToDirTranslator(DirGeneratorScheme dirGeneratorScheme) {
        super(dirGeneratorScheme);
    }

    @Override
    protected void generateIrMethod(DirMethod dirMethod, boolean install) {
        final CirGeneratorScheme cirGeneratorScheme = (CirGeneratorScheme) compilerScheme();
        final CirGenerator cirGenerator = cirGeneratorScheme.cirGenerator();
        final CirMethod cirMethod = cirGenerator.makeIrMethod(dirMethod.classMethodActor(), install);

        Trace.begin(3, "CIR->DIR " + cirMethod.getQualifiedName());

        final CirClosure closure = cirMethod.copyClosure();
        CirBlockUpdating.apply(closure);

        if (MaxineVM.isHosted()) {
            if (cirMethod.classMethodActor().isTemplate()) {
                CirTemplateChecker.apply(cirGenerator, cirMethod, closure);
            }
        }

        final CirToDirMethodTranslation translation = new CirToDirMethodTranslation(cirMethod.resultKind(), closure, dirMethod, this);
        final List<DirBlock> dirBlocks = translation.translateMethod();

        final DirVariable[] dirParameters = new DirVariable[closure.parameters().length - 2]; // exclude continuations
        for (int i = 0; i < dirParameters.length; i++) {
            dirParameters[i] = translation.cirToDirVariable(closure.parameters()[i]);
        }

        dirMethod.setGenerated(dirParameters, dirBlocks, translation.usesMakeStackVariable);

        Trace.end(3, "CIR->DIR " + cirMethod.getQualifiedName());
    }
}
