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
package com.sun.max.asm.gen.risc;

import java.util.*;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.program.*;

/**
 * 
 *
 * @author Bernd Mathiske
 */
public interface RiscInstructionDescriptionVisitor {

    void visitField(RiscField field);

    void visitConstant(RiscConstant constant);

    void visitString(String string);

    void visitConstraint(InstructionConstraint constraint);

    public static final class Static {
        private Static() {
        }

        private static void visitSpecification(RiscInstructionDescriptionVisitor visitor, Object specification) {
            if (specification instanceof RiscField) {
                visitor.visitField((RiscField) specification);
            } else if (specification instanceof RiscConstant) {
                visitor.visitConstant((RiscConstant) specification);
            } else if (specification instanceof String) {
                visitor.visitString((String) specification);
            } else if (specification instanceof InstructionConstraint) {
                visitor.visitConstraint((InstructionConstraint) specification);
            } else {
                ProgramError.unexpected("unknown instructionDescription specification: " + specification);
            }
        }

        private static void visitSpecifications(RiscInstructionDescriptionVisitor visitor, List<Object> specifications) {
            for (Object specification : specifications) {
                visitSpecification(visitor, specification);
            }
        }

        public static void visitInstructionDescription(RiscInstructionDescriptionVisitor visitor, InstructionDescription instructionDescription) {
            visitSpecifications(visitor, instructionDescription.specifications());
        }
    }

}
