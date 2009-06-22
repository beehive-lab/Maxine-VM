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
package com.sun.max.vm.compiler.b.c;

import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.bir.*;
import com.sun.max.vm.compiler.cir.*;

/**
 * Abstract interpretation state associated with a block.
 *
 * @author Bernd Mathiske
 */
public class BlockState {

    private final BirBlock birBlock;
    private AppendableSequence<CirBlock> cirBlockList;
    private JavaFrame frame = null;
    private JavaStack stack = null;

    BlockState(BirBlock birBlock) {
        this.birBlock = birBlock;
    }

    public BirBlock birBlock() {
        return birBlock;
    }

    /**
     * Determines if a CIR block has been created for the BIR block.
     */
    public boolean hasCirBlock() {
        return cirBlockList != null;
    }

    /**
     * Gets the CIR block for the BIR block, creating it first if necessary. This method should
     * only be called if the BIR block is known to be reachable otherwise CIR blocks will be created
     * for unreachable code, inevitably causing problems for subsequent CIR compilation.
     */
    public CirBlock cirBlock() {
        return cirBlockList().first();
    }

    public AppendableSequence<CirBlock>  cirBlockList() {
        if (cirBlockList == null) {
            cirBlockList = new LinkSequence<CirBlock>();
            cirBlockList.append(new CirBlock(birBlock.role()));
        }
        return cirBlockList;
    }

    public void addCirBlock(CirBlock cirBlock) {
        if (cirBlockList == null) {
            Log.println("cirBlockList is null");
            assert false;
        }
        cirBlockList.append(cirBlock);
    }

    public JavaFrame frame() {
        return frame;
    }

    public void setFrame(JavaFrame frame) {
        this.frame = frame;
    }

    public JavaStack stack() {
        return stack;
    }

    public void setStack(JavaStack stack) {
        this.stack = stack;
    }
}
