/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ir;

import com.sun.c1x.value.ValueType;
import com.sun.c1x.util.InstructionVisitor;
import com.sun.c1x.util.InstructionClosure;
import com.sun.c1x.util.Util;

/**
 * The <code>HiWord</code> instruction represents the upper portion
 * of a two-slot Java value on the operand stack or in a local. It
 * is only used in debugging mode.
 *
 * @author Ben L. Titzer
 */
public class HiWord extends Instruction {

    private final Instruction _lowWord;

    /**
     * Constructs a new HiWord instance corresponding to the specified low word.
     * @param lowWord the low word for this high word
     */
    public HiWord(Instruction lowWord) {
        super(ValueType.ILLEGAL_TYPE);
        _lowWord = lowWord;
    }

    /**
     * Gets the low word to which this high word corresponds.
     * @return the low word for this high word
     */
    public final Instruction lowWord() {
        return _lowWord.subst();
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to dispatch to
     */
    public void accept(InstructionVisitor v) {
        v.visitHiWord(this);
    }

    public void makeIllegal() {
        // XXX: this instruction already has a default type of illegal
        setType(ValueType.ILLEGAL_TYPE);
    }

    public void inputValuesDo(InstructionClosure c) {
        Util.shouldNotReachHere();
    }
}
