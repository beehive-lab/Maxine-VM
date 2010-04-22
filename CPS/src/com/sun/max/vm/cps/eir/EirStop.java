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
package com.sun.max.vm.cps.eir;

import com.sun.max.lang.*;
import com.sun.max.vm.collect.*;

/**
 * @author Bernd Mathiske
 * @author Paul Caprioli
 */
public abstract class EirStop<EirInstructionVisitor_Type extends EirInstructionVisitor, EirTargetEmitter_Type extends EirTargetEmitter>
                      extends EirInstruction<EirInstructionVisitor_Type, EirTargetEmitter_Type> {

    private EirJavaFrameDescriptor javaFrameDescriptor;

    public final EirJavaFrameDescriptor javaFrameDescriptor() {
        return javaFrameDescriptor;
    }

    public void setEirJavaFrameDescriptor(EirJavaFrameDescriptor javaFrameDescriptor) {
        this.javaFrameDescriptor = javaFrameDescriptor;
    }

    public EirStop(EirBlock block) {
        super(block);
    }

    public abstract void addFrameReferenceMap(WordWidth stackSlotWidth, ByteArrayBitMap map);

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        EirJavaFrameDescriptor javaFrameDescriptor = this.javaFrameDescriptor;
        while (javaFrameDescriptor != null) {
            for (EirOperand operand : javaFrameDescriptor.locals) {
                visitor.run(operand);
            }
            javaFrameDescriptor = javaFrameDescriptor.parent();
        }
    }

}
