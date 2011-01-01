/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.dir;

import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A value that is constant at compile time.
 *
 * @author Bernd Mathiske
 */
public class DirConstant extends DirValue {

    private final Value value;

    public DirConstant(Value value) {
        this.value = value;
    }

    public Kind kind() {
        return value.kind();
    }

    @Override
    public Value value() {
        return value;
    }

    public boolean isConstant() {
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DirConstant) {
            final DirConstant dirConstant = (DirConstant) other;
            return value.equals(dirConstant.value);
        }
        return false;
    }

    @Override
    public int hashCodeForBlock() {
        if (value.kind().isReference) {
            return super.hashCodeForBlock();
        }
        return super.hashCodeForBlock() ^ value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public static final DirConstant VOID = new DirConstant(VoidValue.VOID);
}
