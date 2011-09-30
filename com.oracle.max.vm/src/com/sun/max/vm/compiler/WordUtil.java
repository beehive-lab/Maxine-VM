/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler;

import com.sun.cri.ci.*;
import com.sun.cri.xir.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;

/**
 * Utility functions to bring the {@link Word} type to the compiler interface.
 */
public class WordUtil {
    /**
     * {@link CiConstant} representation of {@link Word#zero()}.
     */
    public static final CiConstant ZERO = constant(Word.zero());

    /**
     * Creates an architecture-specific {@link Word} constant for the compiler.
     */
    public static CiConstant constant(Word value) {
        if (Word.width() == 64) {
            return CiConstant.forLong(value.asAddress().toLong());
        }
        return CiConstant.forInt(value.asAddress().toInt());
    }

    /**
     * Creates an architecture-specific {@link Word} XIR argument for the compiler.
     */
    public static XirArgument argument(Word value) {
        if (Word.width() == 64) {
            return XirArgument.forLong(value.asAddress().toLong());
        }
        return XirArgument.forInt(value.asAddress().toInt());
    }

    /**
     * The CiKind to use for Word in back-end operation that relate to processor instructions, which is architecture specific.
     */
    @FOLD
    public static CiKind archKind() {
        if (Word.width() == 64) {
            return CiKind.Long;
        }
        return CiKind.Int;
    }

    /**
     * The CiKind to use for Word in front-end operation where a Word is part of the Object type hierarchy.
     */
    @FOLD
    public static CiKind javaKind() {
        return CiKind.Word;
    }

    /**
     * Converts a {@link Kind} of the Maxine-world to a {@link CiKind} of the CRI world, replacing the kind Word with either the
     * Java-specific or architecture-specific CRI kind.
     * @param kind The Maxine {@link Kind} to convert.
     */
    public static CiKind ciKind(Kind kind, boolean architecture) {
        switch (kind.asEnum) {
            // Checkstyle: stop
            case BOOLEAN:   return CiKind.Boolean;
            case BYTE:      return CiKind.Byte;
            case SHORT:     return CiKind.Short;
            case CHAR:      return CiKind.Char;
            case INT:       return CiKind.Int;
            case FLOAT:     return CiKind.Float;
            case LONG:      return CiKind.Long;
            case DOUBLE:    return CiKind.Double;
            case REFERENCE: return CiKind.Object;
            case VOID:      return CiKind.Void;
            case WORD:      return architecture ? archKind() : javaKind();
            // Checkstyle: resume
        }
        throw new IllegalArgumentException("Unknown Kind");
    }

    /**
     * Converts an array of {@link Kind} of the Maxine-world to {@link CiKind} of the CRI world, replacing the kind Word with either the
     * Java-specific or architecture-specific CRI kind.
     * @param kind The Maxine {@link Kind} to convert.
     */
    public static CiKind[] ciKinds(Kind[] kind, boolean architecture) {
        CiKind[] result = new CiKind[kind.length];
        for (int i = 0; i < kind.length; i++) {
            result[i] = ciKind(kind[i], architecture);
        }
        return result;
    }
}
