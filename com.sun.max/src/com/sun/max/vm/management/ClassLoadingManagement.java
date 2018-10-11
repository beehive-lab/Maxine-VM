/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.management;

import com.sun.max.vm.*;
import com.sun.max.vm.type.*;

/**
 * This class provides the entry point to all the class loading management functions in Maxine.
 */

public class ClassLoadingManagement {
    public static boolean setVerboseClass(boolean enable) {
        final boolean previous = VMOptions.verboseOption.verboseClass;
        VMOptions.verboseOption.verboseClass = enable;
        return previous;
    }

    public static int getTotalClassCount() {
        return ClassRegistry.getTotalLoadedClassCount();
    }

    public static int getUnloadedClassCount() {
        return ClassRegistry.getUnloadedClassCount();
    }
}
