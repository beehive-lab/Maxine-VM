/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.max.vm.jdk;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.type.*;

@METHOD_SUBSTITUTIONS(Proxy.class)
public final class JDK_java_lang_reflect_Proxy {

    @ALIAS(declaringClassName = "java.lang.reflect.Proxy$ProxyClassFactory")
    public final static String proxyClassNamePrefix = "$Proxy";

    @ALIAS(declaringClass = Proxy.class, descriptor = "Ljava/lang/reflect/WeakCache;")
    private static Object proxyClassCache = null;

    public static final Set<Class> bootProxyClasses = new HashSet<Class>();

    private JDK_java_lang_reflect_Proxy() {
    }

    @SUBSTITUTE
    public static boolean isProxyClass(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException();
        }
        return Proxy.class.isAssignableFrom(cl) &&
            (bootProxyClasses.contains(cl) ||
             asWeakCacheAlias(proxyClassCache).containsValue(cl));
    }

    private static class WeakCacheAlias {
        @ALIAS(declaringClassName = "java.lang.reflect.WeakCache")
        public native boolean containsValue(Object o);
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native WeakCacheAlias asWeakCacheAlias(Object o);

    @SUBSTITUTE
    private static Class defineClass0(ClassLoader cl, String name, byte[] bytes, int offset, int length) {
        if (cl == null) {
            cl = BootClassLoader.BOOT_CLASS_LOADER;
        }
        return ClassfileReader.defineClassActor(name, cl, bytes, offset, length, null, null, false).toJava();
    }
}
