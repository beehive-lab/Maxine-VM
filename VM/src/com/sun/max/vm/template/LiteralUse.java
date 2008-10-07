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
package com.sun.max.vm.template;

import com.sun.max.asm.*;
import com.sun.max.vm.code.*;


public abstract class LiteralUse {
    private final int _offsetToTemplate;
    private final LiteralModifier _literalModifier;
    private int _literalOffset;

    public LiteralUse(int offset, LiteralModifier literalModifier) {
        _offsetToTemplate = offset;
        _literalModifier = literalModifier;
        _literalOffset = 0;
    }

    protected void fixLiteralOffset(int literalOffset) {
        _literalOffset = literalOffset;
    }

    /*
     * Modifier f
     */
    public LiteralModifier literalModifier() {
        return _literalModifier;
    }

    /**
     * 
     * @return offset to the first byte of the first instruction of the template using this literal in the code buffer where the template was emitted.
     */
    public int offsetToTemplate() {
        return _offsetToTemplate;
    }

    /**
     * Fix the offset to the literal used by the instruction using this literal.
     * @param codeStart address to first byte of the code.
     * @throws AssemblyException
     */
    public abstract void fix(long codeStart) throws AssemblyException;

    public int offsetToLiteral() throws AssemblyException {
        if (_literalOffset >= 0) {
            throw new AssemblyException("Literal not fixed");
        }
        return _literalOffset;
    }
}
