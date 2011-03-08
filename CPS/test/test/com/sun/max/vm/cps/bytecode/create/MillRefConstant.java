/*
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.com.sun.max.vm.cps.bytecode.create;

/**
 * The common super class of field and method reference constants.
 *
 * @see MillFieldRefConstant
 * @see MillMethodRefConstant
 *
 * @author Bernd Mathiske
 * @version 1.0
 */
abstract class MillRefConstant extends MillConstant {

    final int classIndex;
    final int nameAndTypeIndex;

    protected MillRefConstant(byte tag, MillClassConstant clazz, MillNameAndTypeConstant nameAndType) {
        super(tag, 5, clazz.hashValue ^ nameAndType.hashValue);
        this.classIndex = clazz.index;
        this.nameAndTypeIndex = nameAndType.index;
    }

    /**
     * Compares two Objects for equality.
     *
     * @param obj The reference object with which to compare.
     * @return {@code true} if this object is the same
     *         as the {@code obj} argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MillRefConstant)) {
            return false;
        }
        final MillRefConstant c = (MillRefConstant) obj;
        return tag == c.tag && classIndex == c.classIndex && nameAndTypeIndex == c.nameAndTypeIndex;
    }

}
