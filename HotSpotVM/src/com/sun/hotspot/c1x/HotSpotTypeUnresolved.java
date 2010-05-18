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
package com.sun.hotspot.c1x;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * @author Thomas Wuerthinger
 */
public class HotSpotTypeUnresolved implements RiType {

    public final String name;

    /**
     * Creates a new unresolved type for a specified type descriptor.
     *
     * @param typeDescriptor the type's descriptor
     * @param pool the constant pool containing the unresolved type reference
     * @param cpi the index in {@code constantPool} of the unresolved type reference
     */
    public HotSpotTypeUnresolved(String name) {
    	this.name = name;
    }

    public String name() {
    	return name;
    }

    public Class<?> javaClass() {
        throw unresolved("javaClass");
    }

    public boolean hasSubclass() {
        throw unresolved("hasSubclass()");
    }

    public boolean hasFinalizer() {
        throw unresolved("hasFinalizer()");
    }

    public boolean hasFinalizableSubclass() {
        throw unresolved("hasFinalizableSubclass()");
    }

    public boolean isInterface() {
        throw unresolved("isInterface()");
    }

    public boolean isArrayClass() {
        throw unresolved("isArrayClass()");
    }

    public boolean isInstanceClass() {
        throw unresolved("isInstanceClass()");
    }

    public int accessFlags() {
        throw unresolved("accessFlags()");
    }

    public boolean isResolved() {
        return false;
    }

    public boolean isInitialized() {
        throw unresolved("isInitialized()");
    }

    public boolean isSubtypeOf(RiType other) {
        throw unresolved("isSubtypeOf()");
    }

    public boolean isInstance(Object obj) {
        throw unresolved("isInstance()");
    }

    public RiType componentType() {
    	// TODO: Implement
    	throw new UnsupportedOperationException();
    }

    public RiType exactType() {
        throw unresolved("exactType()");
    }
    
    public RiType arrayOf() {
    	// TODO: Implement
    	throw new UnsupportedOperationException();
    }

    public RiMethod resolveMethodImpl(RiMethod method) {
        throw unresolved("resolveMethodImpl()");
    }

    public CiKind kind() {
    	// TODO: Check if this is correct.
        return CiKind.Object;
    }

    private CiUnresolvedException unresolved(String operation) {
        throw new CiUnresolvedException(operation + " not defined for unresolved class " + name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public String toString() {
        return name() + " [unresolved]";
    }

    public CiConstant getEncoding(RiType.Representation r) {
        throw unresolved("getEncoding()");
    }

    public CiKind getRepresentationKind(RiType.Representation r) {
    	// TODO: Check if this is correct.
        return CiKind.Object;
    }
}
