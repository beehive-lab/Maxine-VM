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
import com.sun.cri.ri.*;

public class HotSpotType implements RiType {

    final Object klassOop;

    public HotSpotType(Object o) {
        this.klassOop = o;
        assert klassOop != null;
    }

    @Override
    public int accessFlags() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public RiType arrayOf() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RiType componentType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RiType exactType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CiConstant getEncoding(Representation r) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CiKind getRepresentationKind(Representation r) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasFinalizableSubclass() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasFinalizer() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasSubclass() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isArrayClass() {
        System.out.println("Checking for array class " + name());
        return VMEntries.RiType_isArrayClass(klassOop);
    }

    @Override
    public boolean isInitialized() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isInstance(Object obj) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isInstanceClass() {
        return VMEntries.RiType_isInstanceClass(klassOop);
    }

    @Override
    public boolean isInterface() {
        return VMEntries.RiType_isInterface(klassOop);
    }

    @Override
    public boolean isResolved() {
        return true;
    }

    @Override
    public boolean isSubtypeOf(RiType other) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Class< ? > javaClass() {
        return VMEntries.RiType_javaClass(klassOop);
    }

    @Override
    public CiKind kind() {
        return CiKind.Object;
    }

    @Override
    public String name() {
        return VMEntries.RiType_name(klassOop);
    }

    @Override
    public RiMethod resolveMethodImpl(RiMethod method) {
        // TODO Auto-generated method stub
        return null;
    }

}
