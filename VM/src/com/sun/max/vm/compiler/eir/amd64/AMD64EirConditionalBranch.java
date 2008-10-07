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
/*VCSID=b16b4dd3-4885-4c41-970b-daa73b8f833a*/
package com.sun.max.vm.compiler.eir.amd64;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AMD64EirConditionalBranch extends AMD64EirLocalControlTransfer {

    private final EirBlock _next;

    public EirBlock next() {
        return _next;
    }

    AMD64EirConditionalBranch(EirBlock block, EirBlock target, EirBlock next) {
        super(block, target);
        _next = next;
        next.addPredecessor(block);
    }

    @Override
    public void visitSuccessorBlocks(EirBlock.Procedure procedure) {
        super.visitSuccessorBlocks(procedure);
        if (_next != null) {
            procedure.run(_next);
        }
    }

    @Override
    public EirBlock selectSuccessorBlock(PoolSet<EirBlock> eligibleBlocks) {
        if (eligibleBlocks.contains(_next)) {
            return _next;
        }
        return super.selectSuccessorBlock(eligibleBlocks);
    }

    @Override
    public String toString() {
        String s = super.toString();
        if (_next != null) {
            s += " | #" + _next.serial();
        }
        return s;
    }
}
