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

import java.io.*;
import java.util.*;

import com.sun.max.*;

/**
 * A subclass of {@link Properties} that {@linkplain Collections#sort(java.util.List) sorts} its properties as they
 * are saved.
 *
 * @author Doug Simon
 */
public class SortedProperties extends Properties {

    /**
     * Overridden so that the properties are {@linkplain #store(Writer, String) saved} sorted according their keys.
     */
    @Override
    public synchronized Enumeration<Object> keys() {
        final Enumeration<Object> keysEnum = super.keys();
        final Vector<String> keyList = new Vector<String>(size());
        while (keysEnum.hasMoreElements()) {
            keyList.add((String) keysEnum.nextElement());
        }
        Collections.sort(keyList);
        final Class<Enumeration<Object>> type = null;
        return Utils.cast(type, keyList.elements());
    }
}
