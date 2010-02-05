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

import java.util.*;

/**
 * Miscellaneous operations on {@link Map}s.
 *
 * @author Doug Simon
 */
public final class Maps {

    private Maps() {

    }

    /**
     * Gets the first key in a given map that is mapped to a given value. Given that many
     * keys can map to the same value, the key returned is the first encountered when
     * iterating over the map's {@linkplain Map#entrySet() entry set}.
     */
    public static <Key_Type, Value_Type> Key_Type key(Map<Key_Type, Value_Type> map, Value_Type value) {
        for (Map.Entry<Key_Type, Value_Type> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

}
