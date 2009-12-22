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
package com.sun.max.vm.compiler.cps.b.c;

import com.sun.max.vm.compiler.cps.cir.operator.*;

/**
 * {@link HCirOperatorVisitor} defines general purpose visitor for visiting
 * {@link JavaOperator}s.  See {@link HCirOperatorLowering} for a
 * usage example.
 *
 * @author Yi Guo
 * @author Aziz Ghuloum
 */
public abstract class HCirOperatorVisitor {
    public abstract void visit(JavaOperator op);
    public abstract void visit(GetField op);
    public abstract void visit(PutField op);
    public abstract void visit(GetStatic op);
    public abstract void visit(PutStatic op);
    public abstract void visit(InvokeVirtual op);
    public abstract void visit(InvokeInterface op);
    public abstract void visit(InvokeSpecial op);
    public abstract void visit(InvokeStatic op);
    public abstract void visit(CheckCast op);
    public abstract void visit(New op);
    public abstract void visit(ArrayStore op);
    public abstract void visit(ArrayLoad op);
    public abstract void visit(InstanceOf op);
    public abstract void visit(NewArray op);
    public abstract void visit(MultiANewArray op);
    public abstract void visit(ArrayLength op);
    public abstract void visit(MonitorEnter op);
    public abstract void visit(MonitorExit op);
    public abstract void visit(Mirror op);
    public abstract void visit(CallNative op);
}
