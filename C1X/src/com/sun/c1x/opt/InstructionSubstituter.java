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
package com.sun.c1x.opt;

import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;
import com.sun.c1x.graph.IR;

/**
 * This class allows instructions to be substituted within an IR graph. It allows
 * registering substitutions and iterates over the instructions of a program and replaces
 * the occurrence of each instruction with its substitution, if it has one.
 *
 * @author Ben L. Titzer
 */
public class InstructionSubstituter implements BlockClosure, ValueClosure {

    final IR ir;
    boolean hasSubstitution;

    public InstructionSubstituter(IR ir) {
        this.ir = ir;
    }

    public void apply(BlockBegin block) {
        Instruction last = null;
        if (block.exceptionHandlerStates() != null) {
            for (ValueStack s : block.exceptionHandlerStates()) {
                s.valuesDo(this);
            }
        }
        for (Instruction n = block; n != null; n = last.next()) {
            n.allValuesDo(this);
            if (n.subst != null && last != null) {
                // this instruction has a substitution, skip it
                last.resetNext(n.next());
            } else {
                last = n;
            }
        }
    }

    public void finish() {
        if (hasSubstitution) {
            ir.startBlock.iterateAnyOrder(this, false);
        }
    }

    public boolean hasSubst(Value i) {
        return i.subst != null;
    }

    public void setSubst(Value i, Value n) {
        if (i == n) {
            i.subst = null;
        } else {
            hasSubstitution = true;
            i.subst = n;
        }
    }

    public Value getSubst(Value i) {
        Value p = i;
        while (true) {
            if (p.subst == null) {
                break;
            }
            p = p.subst;
        }
        return p;
    }

    public Value apply(Value i) {
        if (i != null) {
            return getSubst(i);
        }
        return i;
    }
}
