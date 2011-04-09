/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.cri.ci;

import com.sun.cri.ri.*;

/**
 * A implementation of {@link RiField} for an unresolved field.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class CiUnresolvedField implements RiField {

    public final String name;
    public final RiType holder;
    public final RiType type;

    public CiUnresolvedField(RiType holder, String name, RiType type) {
        this.name = name;
        this.type = type;
        this.holder = holder;
    }

    public String name() {
        return name;
    }

    public RiType type() {
        return type;
    }

    public CiKind kind() {
        return type.kind();
    }

    public RiType holder() {
        return holder;
    }

    public boolean isResolved() {
        return false;
    }

    public int accessFlags() {
        throw unresolved("accessFlags()");
    }

    public CiConstant constantValue(CiConstant receiver) {
        return null;
    }

    private CiUnresolvedException unresolved(String operation) {
        throw new CiUnresolvedException(operation + " not defined for unresolved field " + name + " in " + holder);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    /**
     * Converts this compiler interface field to a string.
     */
    @Override
    public String toString() {
        return CiUtil.format("%H.%n [unresolved]", this, false);
    }
}
