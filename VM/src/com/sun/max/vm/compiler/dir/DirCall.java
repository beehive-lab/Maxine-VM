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
package com.sun.max.vm.compiler.dir;

import java.util.*;

/**
 * A call to a builtin or a method.
 * 
 * @author Bernd Mathiske
 */
public abstract class DirCall extends DirInstruction {

    private final DirVariable _result;
    private final DirValue[] _arguments;
    private DirCatchBlock _catchBlock;

    /**
     * @param result the variable which will hold the result of the callee execution
     * @param arguments arguments for the callee
     * @param catchBlock the block where execution continues in case of an exception thrown by the callee
     */
    public DirCall(DirVariable result, DirValue[] arguments, DirCatchBlock catchBlock) {
        super();
        _result = result;
        _arguments = arguments;
        _catchBlock = catchBlock;
    }

    public DirVariable result() {
        return _result;
    }

    public DirValue[] arguments() {
        return _arguments;
    }

    public void setArgument(int index, DirValue value) {
        assert value != null;
        _arguments[index] = value;
    }

    @Override
    public DirCatchBlock catchBlock() {
        return _catchBlock;
    }

    @Override
    public void substituteBlocks(Map<DirBlock, DirBlock> blockMap) {
        final DirBlock block = blockMap.get(_catchBlock);
        if (block != null && block != _catchBlock) {
            _catchBlock = (DirCatchBlock) block;
        }
    }

}
