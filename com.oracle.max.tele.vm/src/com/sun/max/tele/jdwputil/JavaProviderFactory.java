/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.tele.jdwputil;

import java.util.*;
import java.util.logging.*;

import com.sun.max.jdwp.vm.proxy.*;

public class JavaProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(JavaProviderFactory.class.getName());

    private Map<Class, ReferenceTypeProvider> referenceTypeCache = new HashMap<Class, ReferenceTypeProvider>();
    private VMAccess vm;
    private ClassLoaderProvider classLoaderProvider;

    public JavaProviderFactory(VMAccess vm, ClassLoaderProvider classLoaderProvider) {
        this.vm = vm;
        this.classLoaderProvider = classLoaderProvider;

        assert vm != null;
    }

    public ReferenceTypeProvider getReferenceTypeProvider(Class c) {

        if (!referenceTypeCache.containsKey(c)) {
            final ReferenceTypeProvider referenceTypeProvider = createReferenceTypeProvider(c);
            LOGGER.info("Created reference type provider " + referenceTypeProvider + "for Java class " + c);
            referenceTypeCache.put(c, referenceTypeProvider);
        }

        return referenceTypeCache.get(c);
    }

    private ReferenceTypeProvider createReferenceTypeProvider(Class c) {
        if (c.isInterface()) {
            return new JavaInterfaceProvider(c, vm, classLoaderProvider);
        } else if (c.isArray()) {
            return new JavaArrayTypeProvider(c, vm, classLoaderProvider);
        }
        return new JavaClassProvider(c, vm, classLoaderProvider);
    }
}
