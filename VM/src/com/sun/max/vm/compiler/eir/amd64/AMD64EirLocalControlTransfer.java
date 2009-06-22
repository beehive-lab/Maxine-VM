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
package com.sun.max.vm.compiler.eir.amd64;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AMD64EirLocalControlTransfer extends AMD64EirOperation implements EirControlTransfer {

    private EirBlock target;

    public EirBlock target() {
        return target;
    }

    protected AMD64EirLocalControlTransfer(EirBlock block, EirBlock target) {
        super(block);
        this.target = target;
        this.target.addPredecessor(block);
    }

    @Override
    public void visitSuccessorBlocks(EirBlock.Procedure procedure) {
        procedure.run(target);
    }

    @Override
    public void substituteSuccessorBlocks(Mapping<EirBlock, EirBlock> map) {
        super.substituteSuccessorBlocks(map);
        if (map.containsKey(target)) {
            target = map.get(target);
        }
    }

    @Override
    public EirBlock selectSuccessorBlock(PoolSet<EirBlock> eligibleBlocks) {
        if (eligibleBlocks.contains(target())) {
            return target();
        }
        return null;
    }


    @Override
    public String toString() {
        String s = getClass().getSimpleName();
        if (target != null) {
            s += " -> #" + target.serial();
        }
        return s;
    }
}
