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
package com.sun.max.tele.jdwputil;

import java.lang.reflect.*;

import com.sun.max.jdwp.vm.core.*;
import com.sun.max.jdwp.vm.proxy.*;

public class JavaArrayTypeProvider extends JavaClassProvider implements ArrayTypeProvider {

    private ReferenceTypeProvider elementType;
    private Class clazz;
    private VMAccess vm;

    JavaArrayTypeProvider(Class c, VMAccess vm, ClassLoaderProvider classLoader) {
        super(c, vm, classLoader);
        this.elementType = vm.getReferenceType(c.getComponentType());
        this.clazz = c;
        this.vm = vm;
    }

    public ReferenceTypeProvider elementType() {
        return elementType;
    }

    public ArrayProvider newInstance(int length) {
        final Object array = Array.newInstance(clazz.getComponentType(), length);
        final Provider result = vm.createJavaObjectValue(array, clazz).asProvider();
        assert result instanceof ArrayProvider;
        return (ArrayProvider) result;
    }

}
