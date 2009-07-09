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
package com.sun.c1x.value;

import com.sun.c1x.ci.CiType;

/**
 * The <code>ClassType</code> class represents an abstract value corresponding to a class
 * constant. This constant is special since it is a constant object reference when
 * the class is loaded, otherwise it is not.
 *
 * @author Ben L. Titzer
 */
public class ClassType extends ValueType {

    final CiType type;
    ConstType constant;

    /**
     * Creates a new ClassType representing the specified compiler interface type.
     * @param type the compiler interface type
     */
    public ClassType(CiType type) {
        super(BasicType.Object);
        this.type = type;
        if (type.isLoaded()) {
            constant = new ConstType(BasicType.Object, type.javaClass(), true);
        }
    }

    /**
     * Checks if this is {@linkplain ClassType class type}.
     */
    @Override
    public boolean isClass() {
        return true;
    }

    /**
     * Gets the compiler interface type represented by this type.
     */
    public CiType ciType() {
        return type;
    }

    /**
     * Checks whether this value type is a constant. In the case of ClassType instances,
     * this is a constant if the class is loaded.
     * @return <code>true</code> if this value type is a constant
     */
    @Override
    public boolean isConstant() {
        return type.isLoaded();
    }

    /**
     * Converts this value type into a constant. In the case of ClassType instances,
     * this is a constant if the class is loaded.
     * @return a constant representing this value type if it is a constant
     */
    @Override
    public ConstType asConstant() {
        return constant;
    }
}
