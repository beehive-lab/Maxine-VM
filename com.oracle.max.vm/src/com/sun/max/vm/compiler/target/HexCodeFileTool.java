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
package com.sun.max.vm.compiler.target;

import java.lang.reflect.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;

/**
 * Tool that produces a textual version of a {@link CiHexCodeFile}.
 * Reflection is used to try and produced a disassembly of the input.
 * If this fails, the {@linkplain CiHexCodeFile#toEmbeddedString() embedded}
 * string format of the input is returned instead.
 */
public class HexCodeFileTool {

    /**
     * Produces a textual version of a {@link CiHexCodeFile}, using a disassembler
     * via reflection if possible.
     */
    public static String toText(CiHexCodeFile hcf) {
        String embeddedString = hcf.toEmbeddedString();
        String res = embeddedString;
        if (!initialized) {
            Class<?> disasmClass = null;
            try {
                disasmClass = Class.forName("com.oracle.max.hcfdis.HexCodeFileDis");
                disasmMethod = disasmClass.getDeclaredMethod("processEmbeddedString", String.class);
            } catch (ClassNotFoundException e) {
                // Disassembler is not on the class path
            } catch (NoSuchMethodException e) {
                // If the class was found, but the method not, then it should be investigated.
                // Most likely the method was renamed and/or had its signature changed.
                e.printStackTrace();
            }
            initialized = true;
        }
        if (disasmMethod != null) {
            try {
                res = (String) disasmMethod.invoke(null, embeddedString);
            } catch (Exception e) {
                // This should not occur but when it does, it's sufficient to notify the user and continue
                e.printStackTrace();
            }
        }
        return res;
    }

    @RESET
    private static Method disasmMethod;

    @RESET
    private static boolean initialized;
}
