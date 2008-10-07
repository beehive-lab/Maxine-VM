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
/*VCSID=1001507b-dc6f-4ec7-ad6c-3296b993d338*/
package com.sun.max.vm.compiler.dir.transform;

import com.sun.max.vm.compiler.dir.*;

/**
 * Abstract helper for visiting the values defined and used by a DIR instruction.
 *
 * @author Doug Simon
 */
public abstract class DirValueVisitor implements DirVisitor {

    public abstract void visitDef(DirVariable variable);
    public abstract void visitUse(DirValue value);

    public void visitAssign(DirAssign dirAssign) {
        visitDef(dirAssign.destination());
        visitUse(dirAssign.source());
    }

    public void visitCall(DirCall dirCall) {
        if (dirCall.result() != null) {
            visitDef(dirCall.result());
        }
        for (DirValue argument : dirCall.arguments()) {
            visitUse(argument);
        }
    }

    public void visitBuiltinCall(DirBuiltinCall dirBuiltinCall) {
        visitCall(dirBuiltinCall);
    }

    public void visitGoto(DirGoto dirGoto) {
    }

    public void visitMethodCall(DirMethodCall dirMethodCall) {
        visitUse(dirMethodCall.method());
        visitCall(dirMethodCall);
    }

    public void visitReturn(DirReturn dirReturn) {
        if (dirReturn.returnValue() != null) {
            visitUse(dirReturn.returnValue());
        }
    }

    public void visitSwitch(DirSwitch dirSwitch) {
        visitUse(dirSwitch.tag());
        for (DirValue match : dirSwitch.matches()) {
            visitUse(match);
        }
    }

    public void visitThrow(DirThrow dirThrow) {
        visitUse(dirThrow.throwable());
    }

    public void visitSafepoint(DirSafepoint dirSafepoint) {
        for (DirValue value : dirSafepoint.javaFrameDescriptor().locals()) {
            visitUse(value);
        }
        for (DirValue value : dirSafepoint.javaFrameDescriptor().stackSlots()) {
            visitUse(value);
        }
    }
}
