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
/*VCSID=713828bc-3a76-44bc-80a3-ba3dda7c647d*/
package com.sun.max.vm.compiler.b.c;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.bir.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.debug.*;

/**
 * Abstract interpretation state associated with a block.
 *
 * @author Bernd Mathiske
 */
public class BlockState {

    private final BirBlock _birBlock;
    private AppendableSequence<CirBlock> _cirBlockList;

    BlockState(BirBlock birBlock) {
        _birBlock = birBlock;
    }

    public BirBlock birBlock() {
        return _birBlock;
    }

    /**
     * Determines if a CIR block has been created for the BIR block.
     */
    public boolean hasCirBlock() {
        return _cirBlockList != null;
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
        if (_cirBlockList == null) {
            _cirBlockList = new LinkSequence<CirBlock>();
            _cirBlockList.append(new CirBlock(_birBlock.role()));
        }
        return _cirBlockList;
    }

    public void addCirBlock(CirBlock cirBlock) {
        if (_cirBlockList == null) {
            Debug.println("_cirBlockList is null");
            assert false;
        }
        _cirBlockList.append(cirBlock);
    }

    private JavaFrame _frame = null;

    public JavaFrame frame() {
        return _frame;
    }

    public void setFrame(JavaFrame frame) {
        _frame = frame;
    }

    private JavaStack _stack = null;

    public JavaStack stack() {
        return _stack;
    }

    public void setStack(JavaStack stack) {
        _stack = stack;
    }
}
