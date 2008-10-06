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
/*VCSID=71a89e60-e2f3-4f9c-bf52-24c97aa7d5ea*/
package com.sun.max.vm.compiler.tir.pipeline;

import com.sun.max.vm.compiler.tir.*;


public class TirInstructionAdapter implements TirInstructionVisitor {
    public void visit(TirLocal local) {
        visit((TirInstruction) local);
    }

    public void visit(TirNestedLocal local) {
        visit((TirInstruction) local);
    }

    public void visit(TirConstant constant) {
        visit((TirInstruction) constant);
    }

    public void visit(TirTreeCall call) {
        visit((TirInstruction) call);
    }

    public void visit(TirCall call) {
        visit((TirInstruction) call);
    }

    public void visit(TirMethodCall call) {
        visit((TirCall) call);
    }

    public void visit(TirBuiltinCall call) {
        visit((TirCall) call);
    }

    public void visit(TirDirCall call) {
        visit((TirCall) call);
    }

    public void visit(TirGuard guard) {
        visit((TirInstruction) guard);
    }

    public void visit(TirInstruction instruction) {
        visit((TirMessage) instruction);
    }

    public void visit(TirMessage.TirTreeBegin treeBegin) {
        visit((TirMessage) treeBegin);
    }

    public void visit(TirMessage.TirTreeEnd treeEnd) {
        visit((TirMessage) treeEnd);
    }

    public void visit(TirMessage.TirTraceBegin traceBegin) {
        visit((TirMessage) traceBegin);
    }

    public void visit(TirMessage.TirTraceEnd traceEnd) {
        visit((TirMessage) traceEnd);
    }

    public void visit(TirMessage message) {
        // Nop.
    }
}
