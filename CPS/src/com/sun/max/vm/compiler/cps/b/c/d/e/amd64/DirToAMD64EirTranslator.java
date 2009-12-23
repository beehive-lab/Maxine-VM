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
package com.sun.max.vm.compiler.cps.b.c.d.e.amd64;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cps.dir.*;
import com.sun.max.vm.compiler.cps.dir.eir.amd64.*;
import com.sun.max.vm.compiler.cps.eir.*;
import com.sun.max.vm.compiler.cps.eir.amd64.*;
import com.sun.max.vm.compiler.cps.tir.*;

/**
 * @author Bernd Mathiske
 */
public class DirToAMD64EirTranslator extends AMD64EirGenerator {

    public DirToAMD64EirTranslator(AMD64EirGeneratorScheme eirGeneratorScheme) {
        super(eirGeneratorScheme);
    }

    @Override
    protected void generateIrMethod(EirMethod eirMethod) {
        final DirGeneratorScheme dirGeneratorScheme = (DirGeneratorScheme) compilerScheme();
        final DirMethod dirMethod = dirGeneratorScheme.dirGenerator().makeIrMethod(eirMethod.classMethodActor());

        final DirToAMD64EirMethodTranslation translation = new DirToAMD64EirMethodTranslation(this, eirMethod, dirMethod);
        translation.translateMethod();

        eirMethod.setGenerated(translation.eirBlocks(), translation.literalPool, translation.parameterEirLocations, translation.resultEirLocation(), translation.frameSize(), translation.stackBlocksSize());
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
        tirMethod.setGenerated(translation.eirBlocks(), translation.literalPool, translation.parameterEirLocations, translation.resultEirLocation(), translation.frameSize(), translation.stackBlocksSize());
        return tirMethod;
    }

}
