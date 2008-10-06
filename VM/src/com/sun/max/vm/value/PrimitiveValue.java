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
/*VCSID=e54b03e1-11ed-4803-8ebd-41da71b818ba*/
package com.sun.max.vm.value;

/**
 * Primitive values.
 *
 * @author Bernd Mathiske
 */
public abstract class PrimitiveValue<PrimitiveValue_Type extends Value<PrimitiveValue_Type>> extends Value<PrimitiveValue_Type> {

    protected PrimitiveValue() {
        super();
    }

    @Override
    public int hashCode() {
        return toInt();
    }

    @Override
    protected int compareSameKind(PrimitiveValue_Type other) {
        final long thisLong = toLong();
        final long otherLong = other.toLong();
        return (thisLong < otherLong) ? -1 : (thisLong == otherLong ? 0 : 1);
    }
}
