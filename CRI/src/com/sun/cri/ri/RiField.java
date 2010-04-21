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
package com.sun.cri.ri;

import com.sun.cri.ci.*;

/**
 * Represents a reference to a field, including both resolved and unresolved fields. Fields, like methods and types, are
 * resolved through {@link RiConstantPool constant pools}, and their actual implementation is provided by the
 * {@link RiRuntime runtime} to the compiler. Note that most operations are only available on resolved fields.
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
     * Gets the type of this field as a compiler-runtime interface type.
     * @return the type of this field
     */
    RiType type();

    /**
     * Gets the kind of this field.
     * @return the kind
     */
    CiKind kind();

    /**
     * Gets the holder of this field as a compiler-runtime interface type.
     * @return the holder of this field
     */
    RiType holder();

    /**
     * Checks whether this field is resolved.
     * @return {@code true} if this field is resolved
     */
    boolean isResolved();

    // NOTE: All operations beyond this point are only available on resolved fields..

    /**
     * Checks whether this field is volatile.
     * NOTE: ONLY AVAILABLE FOR RESOLVED FIELDS.
     * @return {@code true} if this field is volatile
     */
    boolean isVolatile();

    /**
     * Checks whether this field is constant.
     * NOTE: ONLY AVAILABLE FOR RESOLVED FIELDS.
     * @return {@code true} if this field is constant
     */
    boolean isConstant();

    /**
     * Checks whether this field is static.
     * NOTE: ONLY AVAILABLE FOR RESOLVED FIELDS.
     * @return {@code true} if this field is static
     */
    boolean isStatic();

    /**
     * Gets the constant value of this field, if it is a constant (that is, {@code static final} and <i>initialized</i>).
     * NOTE: ONLY AVAILABLE FOR RESOLVED FIELDS.
     * @return the constant value of this field
     */
    CiConstant constantValue();
}
