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
package com.sun.cri.ci;

import com.sun.cri.ri.*;

/**
 * A implementation of {@link RiField} for an unresolved field.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class CiUnresolvedField implements RiField {

    public final String name;
    public final RiType holder;
    public final RiType type;

    public CiUnresolvedField(RiType holder, String name, RiType type) {
        this.name = name;
        this.type = type;
        this.holder = holder;
    }

    public String name() {
        return name;
    }

    public RiType type() {
        return type;
    }

    public CiKind kind() {
        return type.kind();
    }

    public RiType holder() {
        return holder;
    }

    public boolean isResolved() {
        return false;
    }

    public int accessFlags() {
        throw unresolved("accessFlags()");
    }

    public CiConstant constantValue(Object object) {
        return null;
    }

    private CiUnresolvedException unresolved(String operation) {
        throw new CiUnresolvedException(operation + " not defined for unresolved field " + name + " in " + holder);
    }

    /**
     * Gets the hashcode for this compiler interface field. This is the
     * identity hash code for the field actor if the field is resolved,
     * otherwise the identity hash code for this object.
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * Checks whether this compiler interface field is equal to another object.
     * If the field is resolved, the objects are equivalent if the refer
     * to the same field actor. Otherwise they are equivalent if they
     * reference the same compiler interface field object.
     * @param o the object to check
     * @return {@code true} if this object is equal to the other
     */
    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    /**
     * Converts this compiler interface field to a string.
     */
    @Override
    public String toString() {
        return CiUtil.format("%H.%n [unresolved]", this, false);
    }
}
