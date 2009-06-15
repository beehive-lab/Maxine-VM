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
package com.sun.max.vm.compiler.target.sparc;

import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;

/**
 * @author Bernd Mathiske
 */
public interface SPARCTargetMethod {

    public static final class Static {
        private Static() {
        }

        public static InstructionSet instructionSet() {
            return InstructionSet.SPARC;
        }

        public static int registerReferenceMapSize() {
            return Unsigned.idiv(GPR.SYMBOLIZER.numberOfValues(), Bytes.WIDTH);
        }

        public static void patchCallSite(TargetMethod targetMethod, int callOffset, Word callEntryPoint) {
            final Pointer callSite = targetMethod.codeStart().plus(callOffset).asPointer();
            final SPARC64Assembler assembler = new SPARC64Assembler(callSite.toLong());
            final Label label = new Label();
            assembler.fixLabel(label, callEntryPoint.asAddress().toLong());
            assembler.call(label);
            try {
                final byte[] code = assembler.toByteArray();
                Bytes.copy(code, 0, targetMethod.code(), callOffset, code.length);
            } catch (AssemblyException assemblyException) {
                ProgramError.unexpected("patching call site failed", assemblyException);
            }
        }
    }

}
