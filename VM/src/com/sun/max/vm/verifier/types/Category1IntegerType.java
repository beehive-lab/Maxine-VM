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
/*VCSID=078859d9-5042-4081-8a96-382ded2fc0c1*/
package com.sun.max.vm.verifier.types;

/**
 * Abstract super class for all the primitive types whose values are represented as integer at runtime. All these types
 * are {@linkplain #isAssignableFrom(VerificationType) compatible} with each other for the purpose of verification.
 * 
 * @author Doug Simon
 */
public abstract class Category1IntegerType extends Category1Type {

    @Override
    public final boolean isAssignableFromDifferentType(VerificationType from) {
        return from instanceof Category1IntegerType;
    }

    @Override
    protected
    final VerificationType mergeWithDifferentType(VerificationType other) {
        if (isAssignableFrom(other)) {
            return INTEGER;
        }
        return TOP;
    }

    @Override
    public final int classfileTag() {
        return ITEM_Integer;
    }

    @Override
    public abstract String toString();
}
