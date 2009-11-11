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
package com.sun.max.vm.compiler.dir.transform;

import com.sun.max.vm.compiler.dir.*;

/**
 * @author Bernd Mathiske
 */
public class DirAdapter implements DirVisitor {

    public void visitInstruction(DirInstruction dirInstruction) {
    }

    public void visitCall(DirCall dirCall) {
        visitInstruction(dirCall);
    }

    public void visitBuiltinCall(DirBuiltinCall dirBuiltinCall) {
        visitCall(dirBuiltinCall);
    }

    public void visitMethodCall(DirMethodCall dirMethodCall) {
        visitCall(dirMethodCall);
    }

    public void visitGoto(DirGoto dirGoto) {
        visitInstruction(dirGoto);
    }

    public void visitJump(DirJump dirJump) {
        visitInstruction(dirJump);
    }

    public void visitAssign(DirAssign dirMove) {
        visitInstruction(dirMove);
    }

    public void visitReturn(DirReturn dirReturn) {
        visitInstruction(dirReturn);
    }

    public void visitSwitch(DirSwitch dirSwitch) {
        visitInstruction(dirSwitch);
    }

    public void visitThrow(DirThrow dirThrow) {
        visitInstruction(dirThrow);
    }

    public void visitSafepoint(DirSafepoint dirSafepoint) {
        visitInstruction(dirSafepoint);
    }

    public void visitGuardpoint(DirGuardpoint dirGuardpoint) {
        visitInstruction(dirGuardpoint);
    }

}
