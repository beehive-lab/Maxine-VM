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

import com.sun.cri.ci.*;

/**
 * Pushes a value to the stack representing the
 * current execution address. The exact address pushed is platform dependent as the
 * mechanism for obtaining this address is platform dependent. For example on SPARC,
 * this may be translated to {@code rd %pc %o1} which sets the address
 * of {@code %pc} prior to execution of the instruction into {@code %o1}.
 * On x86, the RIP-relative instruction {@code lea rax 0}
 * loads the address of the instruction immediately
 * following the {@code lea} instruction into register {@code rax}. Either of these
 * satisfies the requirements of the VM purpose for which this instruction
 * exists which is to initiate a stack walk from the current execution location.
 *
 * @author Doug Simon
 */
public final class LoadPC extends Instruction {

    /**
     * Creates a new LoadPC instance.
     */
    public LoadPC() {
        super(CiKind.Word);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitLoadPC(this);
    }
}
