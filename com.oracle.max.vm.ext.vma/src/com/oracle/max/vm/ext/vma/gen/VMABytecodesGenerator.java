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
package com.oracle.max.vm.ext.vma.gen;

import static com.sun.max.vm.t1x.T1XTemplateGenerator.*;

import java.lang.reflect.*;
import java.util.regex.*;

import com.sun.cri.bytecode.*;


/*
 */
public class VMABytecodesGenerator {

    public static void main(String[] args) {
        boolean first = true;
        for (Field f : Bytecodes.class.getDeclaredFields()) {
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && !f.getName().contains("UNUSED"))  {
                generate(f, first);
            }
            first = false;
        }
        out.printf(";%n");
    }

    private static void generate(Field f, boolean first) {
        final String name = f.getName();
        if (!first) {
            out.printf(",%n");
        }
        out.printf("    %s(Bytecodes.%s, \"%s\")", name, name, getMethodStyleName(name));
    }

    private static String getMethodStyleName(String name) {
        // no automatic way to do this
        String result;
        if ((result = checkChangeCase(name, "FIELD")) != null) {
            return result;
        } else if ((result = checkChangeCase(name, "STATIC")) != null) {
            return result;
        } else if ((result = checkChangeCase(name, "NEW")) != null) {
            return result;
        } else if ((result = checkChangeCase(name, "SPECIAL")) != null) {
            return result;
        } else if ((result = checkChangeCase(name, "VIRTUAL")) != null) {
            return result;
        } else if ((result = checkChangeCase(name, "INTERFACE")) != null) {
            return result;
        } else if (name.equals("NEWARRAY") || name.equals("ANEWARRAY")) {
            return "NewArray";
        } else if (name.equals("MULTIANEWARRAY")) {
            return "MultiNewArray";
        } else if (Pattern.matches(".{1}ALOAD", name)) {
            return "ArrayLoad";
        } else if (Pattern.matches(".{1}ASTORE", name)) {
            return "ArrayStore";
        } else {
            return name;
        }
    }

    private static String checkChangeCase(String name, String suffix) {
        if (name.endsWith(suffix)) {
            int index = name.indexOf(suffix);
            assert index >= 0;
            return toFirstUpper(name.substring(0, index).toLowerCase()) + toFirstUpper(suffix.toLowerCase());
        } else {
            return null;
        }
    }

}
