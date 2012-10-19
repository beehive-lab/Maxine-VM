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
package com.oracle.max.vm.ext.vma.store.txt;

/**
 * A {@link VMATextStore) with support for defining compact (short) forms of threads, classes, fields. methods.
 */
public abstract class CVMATextStore extends VMATextStore {

    /**
     * Add a definition of a short form of a class to the store.
     * @param name full name of the class
     * @param clId object id of the defining classloader
     * @param shortName the short name to be used
     */
    public abstract void addClassShortFormDef(String name, long clId, String shortName);

    /**
     * Add a definition of a short form of a thread to the store.
     * @param name full name of the thread (may contain spaces)
     * @param shortName the short name to be used
     */
    public abstract void addThreadShortFormDef(String name, String shortName);

    /**
     * Add a definition of a short form of a field or method to the store.
     * @param name name of the field or method
     * @param shortName the short name to be used
     */
    public abstract void addMemberShortFormDef(VMATextStoreFormat.Key key, String classShortForm, String name, String shortName);


}
