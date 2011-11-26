/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.nodes.type;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class StampFactory {

    private static class BasicValueStamp implements Stamp {

        private final CiKind kind;

        public BasicValueStamp(CiKind kind) {
            this.kind = kind;
        }

        @Override
        public CiKind kind() {
            return kind;
        }

        @Override
        public boolean canBeNull() {
            return false;
        }

        @Override
        public RiType declaredType() {
            return null;
        }

        @Override
        public RiType exactType() {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BasicValueStamp) {
                BasicValueStamp basicValueType = (BasicValueStamp) obj;
                return kind == basicValueType.kind;
            }
            return false;
        }
    }

    private static final Stamp[] stampCache = new Stamp[CiKind.values().length];
    static {
        for (CiKind k : CiKind.values()) {
            stampCache[k.ordinal()] = new BasicValueStamp(k);
        }
    }

    public static Stamp illegal() {
        return forKind(CiKind.Illegal);
    }

    public static Stamp intValue() {
        return forKind(CiKind.Int);
    }

    public static Stamp forKind(CiKind kind) {
        return stampCache[kind.ordinal()];
    }
}
