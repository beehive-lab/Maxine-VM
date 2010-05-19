/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.hotspot.c1x;

import com.sun.cri.ci.CiConstant;
import com.sun.cri.ri.RiConstantPool;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiSignature;
import com.sun.cri.ri.RiType;

public class HotSpotConstantPool implements RiConstantPool {

    private final Object constantPoolOop;

    public HotSpotConstantPool(Object o) {
        this.constantPoolOop = o;
    }

    @Override
    public CiConstant encoding() {
        // TODO: Check if this is correct.
        return CiConstant.forObject(constantPoolOop);
    }

    @Override
    public Object lookupConstant(int cpi) {
        return VMEntries.RiConstantPool_lookupConstant(constantPoolOop, cpi);
    }

    @Override
    public RiMethod lookupMethod(int cpi, int opcode) {
        return VMEntries.RiConstantPool_lookupMethod(constantPoolOop, cpi, (byte) opcode);
    }

    @Override
    public RiSignature lookupSignature(int cpi) {
        return VMEntries.RiConstantPool_lookupSignature(constantPoolOop, cpi);
    }

    @Override
    public RiType lookupType(int cpi, int opcode) {
        return VMEntries.RiConstantPool_lookupType(constantPoolOop, cpi);
    }

    @Override
    public RiField lookupField(int cpi, int opcode) {
        return VMEntries.RiConstantPool_lookupField(constantPoolOop, cpi);
    }

}
