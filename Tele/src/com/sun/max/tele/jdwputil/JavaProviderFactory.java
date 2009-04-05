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

import java.util.*;
import java.util.logging.*;

import com.sun.max.jdwp.vm.proxy.*;


public class JavaProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(JavaProviderFactory.class.getName());

    private Map<Class, ReferenceTypeProvider> _referenceTypeCache = new HashMap<Class, ReferenceTypeProvider>();
    private VMAccess _vm;
    private ClassLoaderProvider _classLoaderProvider;

    public JavaProviderFactory(VMAccess vm, ClassLoaderProvider classLoaderProvider) {
        _vm = vm;
        _classLoaderProvider = classLoaderProvider;

        assert _vm != null;
    }

    public ReferenceTypeProvider getReferenceTypeProvider(Class c) {

        if (!_referenceTypeCache.containsKey(c)) {
            final ReferenceTypeProvider referenceTypeProvider = createReferenceTypeProvider(c);
            LOGGER.info("Created reference type provider " + referenceTypeProvider + "for Java class " + c);
            _referenceTypeCache.put(c, referenceTypeProvider);
        }

        return _referenceTypeCache.get(c);
    }

    private ReferenceTypeProvider createReferenceTypeProvider(Class c) {
        if (c.isInterface()) {
            return new JavaInterfaceProvider(c, _vm, _classLoaderProvider);
        } else if (c.isArray()) {
            return new JavaArrayTypeProvider(c, _vm, _classLoaderProvider);
        }
        return new JavaClassProvider(c, _vm, _classLoaderProvider);
    }
}
