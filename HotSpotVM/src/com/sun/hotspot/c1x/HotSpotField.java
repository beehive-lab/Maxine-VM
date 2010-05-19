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
import com.sun.cri.ci.CiKind;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiType;

public class HotSpotField implements RiField {

    private final RiType holder;
    private final Object nameSymbol;
    private final RiType type;
    private final int offset;

    public HotSpotField(RiType holder, Object nameSymbol, RiType type, int offset) {
        this.holder = holder;
        this.nameSymbol = nameSymbol;
        this.type = type;
        this.offset = offset;
    }

    @Override
    public int accessFlags() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public CiConstant constantValue(Object object) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RiType holder() {
        return holder;
    }

    @Override
    public boolean isConstant() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isResolved() {
        return offset != -1;
    }

    @Override
    public CiKind kind() {
        return type().kind();
    }

    @Override
    public String name() {
        return VMEntries.RiSignature_symbolToString(nameSymbol);
    }

    @Override
    public RiType type() {
        return type;
    }

}
