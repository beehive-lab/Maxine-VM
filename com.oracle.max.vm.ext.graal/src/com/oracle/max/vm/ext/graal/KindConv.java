/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

/**
 * Graal and Maxine disagree on how to represent the {@code Kind} abstraction.
 * One fix would be to convert Maxine over to using the Graal {@code Kind} everywhere but
 * that would have no intrinsic value and would be quite disruptive. The solution here is to
 * provide an explict conversion mechanism. Since a {@code Kind} is immutable the conversion
 * if efficient.
 *
 * Graal uses one enum to represent a kind, whereas Maxine uses an enum and a class.
 *
 */
public class KindConv {

    /**
     * Maps from a Maxine kind to a Graal kind.
     * Indexed by {@link com.sun.max.vm.type.KindEnum#ordinal()}.
     */
    private static com.oracle.graal.api.meta.Kind[] maxToGraal;

    /**
     * Maps from a Graal kind to a Maxine kind.
     * Indexed by {@link com.oracle.graal.api.meta.Kind#ordinal()}.
     *
     */
    private static com.sun.max.vm.type.Kind[] graalToMax;

    static {
        for (com.oracle.graal.api.meta.Kind graalKind : com.oracle.graal.api.meta.Kind.values()) {
            graalToMax[graalKind.ordinal()] = null;
        }
        for (com.sun.max.vm.type.KindEnum maxKind : com.sun.max.vm.type.KindEnum.values()) {
            maxToGraal[maxKind.ordinal()] = null;
        }
    }

    public static com.oracle.graal.api.meta.Kind toGraalKind(com.sun.max.vm.type.Kind maxKind) {
        com.sun.max.vm.type.KindEnum maxKindEnum = maxKind.asEnum;
        com.oracle.graal.api.meta.Kind result = maxToGraal[maxKindEnum.ordinal()];
        if (result == null) {
            // CheckStyle : Stop
            switch (maxKindEnum) {
                case BOOLEAN: result = com.oracle.graal.api.meta.Kind.Boolean; break;
                case BYTE: result = com.oracle.graal.api.meta.Kind.Byte; break;
                case CHAR: result = com.oracle.graal.api.meta.Kind.Char; break;
                case SHORT: result = com.oracle.graal.api.meta.Kind.Short; break;
                case INT: result = com.oracle.graal.api.meta.Kind.Int; break;
                case LONG: result = com.oracle.graal.api.meta.Kind.Long; break;
                case FLOAT: result = com.oracle.graal.api.meta.Kind.Float; break;
                case DOUBLE: result = com.oracle.graal.api.meta.Kind.Double; break;
                case VOID: result = com.oracle.graal.api.meta.Kind.Void; break;
                case REFERENCE: result = com.oracle.graal.api.meta.Kind.Object; break;
                case WORD: assert false;
            }
            // CheckStyle : Resume
            maxToGraal[maxKindEnum.ordinal()] = result;
        }
        return result;
    }

    public static com.sun.max.vm.type.Kind toMaxKind(com.oracle.graal.api.meta.Kind graalKind) {
        com.sun.max.vm.type.Kind result = graalToMax[graalKind.ordinal()];
        if (result == null) {
            // Checkstyle : stop
            switch (graalKind) {
                case Boolean: result = com.sun.max.vm.type.Kind.BOOLEAN; break;
                case Byte: result = com.sun.max.vm.type.Kind.BYTE; break;
                case Char: result = com.sun.max.vm.type.Kind.CHAR; break;
                case Short: result = com.sun.max.vm.type.Kind.SHORT; break;
                case Int: result = com.sun.max.vm.type.Kind.INT; break;
                case Long: result = com.sun.max.vm.type.Kind.LONG; break;
                case Float: result = com.sun.max.vm.type.Kind.FLOAT; break;
                case Double: result = com.sun.max.vm.type.Kind.DOUBLE; break;
                case Void: result = com.sun.max.vm.type.Kind.VOID; break;
                case Object: result = com.sun.max.vm.type.Kind.REFERENCE; break;
                case Jsr: case Illegal: assert false;
            }
            // Checkstyle : resume
            graalToMax[graalKind.ordinal()] = result;
        }
        return result;
    }

}
