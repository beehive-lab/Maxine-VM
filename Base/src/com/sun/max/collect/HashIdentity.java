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
/*VCSID=990736d5-535a-4c93-b353-df4b1a1d9f1a*/
package com.sun.max.collect;

import com.sun.max.lang.*;

/**
 * Defines the semantics of key comparison in a {@linkplain HashMapping hash based data structure} where reference
 * identity is used for {@linkplain #equivalent(Object, Object) equivalence} and the hash code are obtained from
 * {@link System#identityHashCode(Object)}. There is a {@linkplain #instance(Class) singleton} instance of this class.
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class HashIdentity<Object_Type> implements HashEquivalence<Object_Type> {

    private HashIdentity() {
    }

    public boolean equivalent(Object_Type object1, Object_Type object2) {
        return object1 == object2;
    }

    public int hashCode(Object_Type object) {
        if (object == null) {
            return 0;
        }
        return System.identityHashCode(object);
    }

    private static final HashIdentity _identity = new HashIdentity<Object>();

    public static <Object_Type> HashIdentity<Object_Type> instance(Class<HashIdentity<Object_Type>> type) {
        return StaticLoophole.cast(type, _identity);
    }
}
