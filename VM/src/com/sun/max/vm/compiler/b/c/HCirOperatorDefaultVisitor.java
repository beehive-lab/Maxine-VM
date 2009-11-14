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
package com.sun.max.vm.compiler.b.c;

import com.sun.max.vm.compiler.cir.operator.*;

public class HCirOperatorDefaultVisitor extends HCirOperatorVisitor {
    protected void visitDefault(JavaOperator op) {
    }

    @Override
    public void visit(JavaOperator op) {
        visitDefault(op);
    }

    @Override
    public void visit(GetField op) {
        visitDefault(op);
    }

    @Override
    public void visit(PutField op) {
        visitDefault(op);
    }

    @Override
    public void visit(GetStatic op) {
        visitDefault(op);
    }

    @Override
    public void visit(PutStatic op) {
        visitDefault(op);
    }

    @Override
    public void visit(InvokeVirtual op) {
        visitDefault(op);
    }

    @Override
    public void visit(InvokeInterface op) {
        visitDefault(op);
    }

    @Override
    public void visit(InvokeSpecial op) {
        visitDefault(op);
    }

    @Override
    public void visit(InvokeStatic op) {
        visitDefault(op);
    }

    @Override
    public void visit(CheckCast op) {
        visitDefault(op);
    }

    @Override
    public void visit(New op) {
        visitDefault(op);
    }

    @Override
    public void visit(ArrayStore op) {
        visitDefault(op);
    }

    @Override
    public void visit(ArrayLoad op) {
        visitDefault(op);
    }

    @Override
    public void visit(InstanceOf op) {
        visitDefault(op);
    }

    @Override
    public void visit(NewArray op) {
        visitDefault(op);
    }

    @Override
    public void visit(MultiANewArray op) {
        visitDefault(op);
    }
    @Override
    public void visit(ArrayLength op) {
        visitDefault(op);
    }

    @Override
    public void visit(MonitorEnter op) {
        visitDefault(op);
    }

    @Override
    public void visit(MonitorExit op) {
        visitDefault(op);
    }

    @Override
    public void visit(Mirror op) {
        visitDefault(op);
    }

    @Override
    public void visit(CallNative op) {
        visitDefault(op);
    }
}
