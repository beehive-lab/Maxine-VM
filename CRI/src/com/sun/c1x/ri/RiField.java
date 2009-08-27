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
package com.sun.c1x.ri;

import com.sun.c1x.ci.*;


/**
 * The <code>RiField</code> interface represents a reference to a field, including both
 * resolved and unresolved fields. Fields, like methods and types, are resolved through
 * {@link RiConstantPool constant pools}, and their actual implementation is provided
 * by the {@link RiRuntime runtime} to the compiler. Note that some operations are only
 * available on resolved fields.
 *
 * @author Ben L. Titzer
 */
public interface RiField {
    /**
     * Gets the name of this field as a string.
     * @return the name of this field
     */
    String name();

    /**
     * Gets the type of this field as a compiler interface type.
     * @return the type of this field
     */
    RiType type();

    /**
     * Gets the basic type of this field.
     * @return the basic type
     */
    CiKind basicType();

    /**
     * Gets the holder of this field as a compiler interface type.
     * @return the holder of this field
     */
    RiType holder();

    /**
     * Checks whether this field is loaded (i.e. resolved).
     * @return {@code true} if this field is resolved
     */
    boolean isLoaded();

    /**
     * Checks whether this field is volatile.
     * ONLY AVAILABLE FOR RESOLVED FIELDS.
     * @return {@code true} if this field is volatile
     */
    boolean isVolatile();

    /**
     * Checks whether this field is constant.
     * ONLY AVAILABLE FOR RESOLVED FIELDS.
     * @return {@code true} if this field is constant
     */
    boolean isConstant();

    /**
     * Checks whether this field is static
     * ONLY AVAILABLE FOR RESOLVED FIELDS.
     * @return {@code true} if this field is static
     */
    boolean isStatic();

    /**
     * Gets the offset of this field from the origin of an object.
     * ONLY AVAILABLE FOR RESOLVED FIELDS.
     * @return the offset of the field from origin of an object.
     */
    int offset();

    /**
     * Gets the constant value of this field, if it is a constant (i.e. {@code static final} and <i>initialized</i>).
     * ONLY AVAILABLE FOR RESOLVED FIELDS.
     * @return the constant value of this field
     */
    CiConstant constantValue();
}
