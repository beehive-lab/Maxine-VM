/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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

package com.sun.max.vm.methodhandle;

import java.lang.invoke.*;

import com.sun.max.vm.methodhandle.MaxMethodHandles.*;

/**
 * Key for method handle intrinsic methods.
 *
 */
public class MethodHandleIntrinsicKey {

    private final MethodType type;

    private final MethodHandleIntrinsicID name;

    public MethodHandleIntrinsicKey(MethodType mt, MethodHandleIntrinsicID spn) {
        type = mt;
        name = spn;
    }

    public MethodType type() {
        return type;
    }

    public MethodHandleIntrinsicID name() {
        return name;
    }

    public boolean equals(MethodHandleIntrinsicKey m) {
        if (type.equals(m.type) && name.equals(m.name)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MethodHandleIntrinsicKey)) {
            return false;
        }

        return equals((MethodHandleIntrinsicKey) o);
    }

    @Override
    public int hashCode() {
        return type.hashCode() ^ name.ordinal();
    }
}
