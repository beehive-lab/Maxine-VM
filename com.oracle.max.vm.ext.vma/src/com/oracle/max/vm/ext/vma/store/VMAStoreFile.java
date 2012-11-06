/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.store;

import java.io.*;

/**
 * Specification of the directory to be used for a persistent logs.
 * The default file name is {@link #DEFAULT_STOREDIR} but this
 * can be changed using the {@link #STOREDIR_PROPERTY} system property.
 *
 */
public class VMAStoreFile {
    public static final String STOREDIR_PROPERTY = "max.vma.store.dir";
    public static final String DEFAULT_STOREDIR = "vmastore";
    public static final String GLOBAL_STORE = "vm";
    public static final String DEFAULT_STOREFILE = DEFAULT_STOREDIR + File.separator + GLOBAL_STORE;

    public static String getStoreDir() {
        String storeDir = System.getProperty(STOREDIR_PROPERTY);
        if (storeDir == null) {
            storeDir = DEFAULT_STOREDIR;
        }
        return storeDir;
    }
}
