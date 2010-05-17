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
package com.sun.mockvm;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Thomas Wuerthinger
 * 
 */
public class MockUniverse {

    private static final Map<Class< ? >, MockType> typeCache = new HashMap<Class< ? >, MockType>();

    public static MockType lookupType(String name) {
        try {
            Class< ? > klass = Class.forName(name);
            return lookupType(klass);
        } catch (ClassNotFoundException e) {
            // Type not found in mock universe and also not via reflection
            System.out.println("WARNING: Type with name " + name + " not found!");
            return null;
        }
    }

    public static MockType lookupType(Class< ? > klass) {

        MockType result = typeCache.get(klass);
        if (result == null) {
            result = new MockType(klass);
            typeCache.put(klass, result);
        }

        return result;
    }

    public static MockType lookupTypeBySignature(String signature) {

        String name = null;

        char first = signature.charAt(0);
        if (first == '[') {
            return (MockType) (lookupTypeBySignature(signature.substring(1)).arrayOf());
        } else if (first == 'L') {
            name = signature.substring(1, signature.length() - 1).replace('/', '.');
        } else if (first == 'Z') {
            return lookupType(Boolean.TYPE);
        } else if (first == 'C') {
            return lookupType(Character.TYPE);
        } else if (first == 'F') {
            return lookupType(Float.TYPE);
        } else if (first == 'D') {
            return lookupType(Double.TYPE);
        } else if (first == 'B') {
            return lookupType(Byte.TYPE);
        } else if (first == 'S') {
            return lookupType(Short.TYPE);
        } else if (first == 'I') {
            return lookupType(Integer.TYPE);
        } else if (first == 'J') {
            return lookupType(Long.TYPE);
        } else if (first == 'V') {
            return lookupType(Void.TYPE);
        } else {
            assert false;
        }

        return lookupType(name);
    }
}
