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
package com.sun.max.vm.classfile.create;

/**
 * This class represents Utf8 string entries in constant pools.
 * 
 * @see MillClass#makeUtf8Constant
 * 
 * @author Bernd Mathiske
 * @version 1.0
 */
public class MillUtf8Constant extends MillConstant {

    final String string;

    MillUtf8Constant(String string) {
        super(CONSTANT_Utf8, 3 + string.length(), string.hashCode());
        this.string = string;
    }

    /**
     * Compares two objects for equality.
     * 
     * @param other
     *            The reference object with which to compare.
     * @return {@code true} if this object is the same as the
     *         {@code obj} argument; {@code false} otherwise.
     */
    @Override  public boolean equals(Object other) {
        if (!(other instanceof MillUtf8Constant)) {
            return false;
        }
        return string.equals(((MillUtf8Constant) other).string);
    }

}
