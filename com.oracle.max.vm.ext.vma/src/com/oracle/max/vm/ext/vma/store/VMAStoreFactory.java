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

import com.oracle.max.vm.ext.vma.store.txt.sbps.*;

/**
 * Factory for controlling which subclass of {@link VMAStore} is used.
 *
 * The default choice {@link SBPSVMATextStore}, but the choice
 * can be changed with the {@link LOGCLASS_PROPERTY} system property,
 * which should be the fully qualified name of the class.
 *
 * It can also be changed by invoking {@link setClass} prior to a call to {@link #create}.
 */

public class VMAStoreFactory {
    public static final String STORECLASS_PROPERTY = "max.vma.storeclass";

    private static Class<? extends VMAStore> storeClass;

    @SuppressWarnings("unchecked")
    public static VMAStore create(boolean perThread) {
        VMAStore result = null;
        try {
            if (storeClass == null) {
                final String storeClassProperty = System.getProperty(STORECLASS_PROPERTY);
                if (storeClassProperty == null) {
                    storeClass = SBPSVMATextStore.class;
                } else {
                    storeClass = (Class< ? extends VMAStore>) Class.forName(storeClassProperty);
                }
            }
            result = storeClass.newInstance();
        } catch (Exception exception) {
            System.err.println("Error instantiating " + storeClass.getName() + ": " + exception);
        }
        return result;
    }

    public static void setClass(Class<? extends VMAStore> storeClass) {
        VMAStoreFactory.storeClass = storeClass;
    }

}
