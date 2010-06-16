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
package com.sun.max.vm.classfile.create;

import com.sun.max.lang.*;
import com.sun.max.vm.classfile.*;

/**
 * ClassLoader that makes a class from an array of bytes.
 *
 * @author Bernd Mathiske
 */
public class MillClassLoader extends ClassLoader {

    private final byte[] classfileBytes;

    MillClassLoader(byte[] classfileBytes) {
        this.classfileBytes = classfileBytes.clone();
    }

    @Override
    public Class<?> findClass(String name) {
        final Class result = defineClass(name, classfileBytes, 0, classfileBytes.length);
        ClassfileReader.defineClassActor(name, this, classfileBytes, null, null, false);
        return result;
    }

    public static Class makeClass(String name, byte[] classfileBytes) {
        final MillClassLoader classLoader = new MillClassLoader(classfileBytes);
        synchronized (classLoader) {
            final Class result = Classes.load(classLoader, name);
            classLoader.resolveClass(result);
            return result;
        }
    }

}
