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
package com.sun.c1x.lir;

import com.sun.c1x.gen.LIRGenerator;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;


public class LIRItem {

    public LIRItem(Instruction receiver, LIRGenerator lirGenerator) {
        // TODO Auto-generated constructor stub
    }

    public LIRItem(LIRGenerator lirGenerator) {
        // TODO Auto-generated constructor stub
    }

    public ValueType type() {
        // TODO Auto-generated method stub
        return null;
    }

    public void loadItemForce(LIROperand loc) {
        // TODO Auto-generated method stub

    }

    public void loadForStore(BasicType type) {
        // TODO Auto-generated method stub

    }

    public LIROperand result() {
        // TODO Auto-generated method stub
        return null;
    }

    public void loadItem() {
        // TODO Auto-generated method stub

    }

    public void loadByteItem() {
        // TODO Auto-generated method stub

    }

    public void setInstruction(Instruction length) {
        // TODO Auto-generated method stub

    }

    public boolean isConstant() {
        // TODO Auto-generated method stub
        return false;
    }

    public void dontLoadItem() {
        // TODO Auto-generated method stub

    }

    public void loadNonconstant() {
        // TODO Auto-generated method stub

    }

    public Instruction value() {
        // TODO Auto-generated method stub
        return null;
    }


}
