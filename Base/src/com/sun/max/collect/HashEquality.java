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
package com.sun.max.collect;

import com.sun.max.*;

/**
 * Defines the semantics of key comparison in a {@linkplain HashMapping hash based data structure} that delegate to
 * {@link Object#equals(Object)} and {@link Object#hashCode()}. There is a {@linkplain #instance(Class) singleton}
 * instance of this class.
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class HashEquality<T> implements HashEquivalence<T> {

    private HashEquality() {
    }

    public boolean equivalent(T object1, T object2) {
        if (object1 == null) {
            return object2 == null;
        }
        return object1.equals(object2);
    }

    public int hashCode(T object) {
        if (object == null) {
            return 0;
        }
        return object.hashCode();
    }

    private static final HashEquality equality = new HashEquality<Object>();

    public static <T> HashEquality<T> instance(Class<HashEquality<T>> type) {
        return Utils.cast(type, equality);
    }
}
