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
package com.sun.max.vm.cps.b.c.d.e.amd64;

import java.util.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.dir.eir.amd64.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.tir.*;

/**
 * @author Bernd Mathiske
 */
public class DirToAMD64EirTranslator extends AMD64EirGenerator {

    public DirToAMD64EirTranslator(AMD64EirGeneratorScheme eirGeneratorScheme) {
        super(eirGeneratorScheme);
    }

    @Override
    protected void generateIrMethod(EirMethod eirMethod, boolean install) {
        final DirGeneratorScheme dirGeneratorScheme = (DirGeneratorScheme) compilerScheme();
        final DirMethod dirMethod = dirGeneratorScheme.dirGenerator().makeIrMethod(eirMethod.classMethodActor(), install);

        final DirToAMD64EirMethodTranslation translation = new DirToAMD64EirMethodTranslation(this, eirMethod, dirMethod);
        translation.translateMethod();

        ArrayList<EirBlock> eirBlocks = translation.eirBlocks();
        eirMethod.setGenerated(eirBlocks.toArray(new EirBlock[eirBlocks.size()]), translation.literalPool, translation.parameterEirLocations, translation.resultEirLocation(), translation.frameSize(), translation.stackBlocksSize());
    }

    private TreeEirMethod createTreeEirMethod(ClassMethodActor classMethodActor) {
        final TreeEirMethod eirMethod = new TreeEirMethod(classMethodActor, eirABIsScheme().treeABI);
        notifyAllocation(eirMethod);
        return eirMethod;
    }

    @Override
    public EirMethod makeIrMethod(DirMethod dirMethod) {
        final TreeEirMethod tirMethod = createTreeEirMethod(dirMethod.classMethodActor());
        final DirToAMD64EirMethodTranslation translation = new DirToAMD64EirMethodTranslation(this, tirMethod, dirMethod);
        translation.translateMethod();
        ArrayList<EirBlock> eirBlocks = translation.eirBlocks();
        tirMethod.setGenerated(eirBlocks.toArray(new EirBlock[eirBlocks.size()]), translation.literalPool, translation.parameterEirLocations, translation.resultEirLocation(), translation.frameSize(), translation.stackBlocksSize());
        return tirMethod;
    }

}
