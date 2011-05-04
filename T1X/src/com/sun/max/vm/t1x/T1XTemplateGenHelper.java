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
package com.sun.max.vm.t1x;

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;

/**
 * {@HOSTED_ONLY} support for automatically generating template method sources.
 */

@HOSTED_ONLY
public class T1XTemplateGenHelper {
    public static final String[] types = {"boolean", "byte", "char", "short", "int", "float", "long", "double", "Reference", "Word", "void"};
    /**
     * As {@link #types} but with first character uppercase.
     */
    public static Map<String, String> uTypes = new HashMap<String, String>();

    /**
     * As {@link #types} but with first character lower case.
     */
    public static Map<String, String> lTypes = new HashMap<String, String>();
    /**
     * As {@link #types} but with {@code Reference} transposed to {@code Object}.
     */
    public static Map<String, String> oTypes = new HashMap<String, String>();

    /**
     * As {@link #otypes} but with first character upper case.
     */
    public static Map<String, String> uoTypes = new HashMap<String, String>();

    /**
     * As {@link #types} but all upper case.
     */
    public static Map<String, String> auTypes = new HashMap<String, String>();

    public static final String[] lockVariants = new String[] {"", "unlockClass", "unlockReceiver"};

    static {
        for (String type : types) {
            uTypes.put(type, type.substring(0, 1).toUpperCase() + type.substring(1));
            lTypes.put(type, type.substring(0, 1).toLowerCase() + type.substring(1));
            oTypes.put(type, type.equals("Reference") ? "Object" : type);
            uoTypes.put(type, type.equals("Reference") ? "Object" : uTypes.get(type));
            auTypes.put(type, type.toUpperCase());
        }
    }

    public static PrintStream o = System.out;

    public static String lType(String type) {
        return lTypes.get(type);
    }

    public static String uType(String type) {
        return uTypes.get(type);
    }

    public static String oType(String type) {
        return oTypes.get(type);
    }

    public static String uoType(String type) {
        return uoTypes.get(type);
    }

    public static String auType(String type) {
        return auTypes.get(type);
    }

    public static String toFirstUpper(String s) {
        if (s.length() == 0) {
            return s;
        } else {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }

    public static String prefixDollar(String s) {
        if (s.length() == 0) {
            return s;
        } else {
            return "$" + s;
        }
    }

    public static boolean isRefOrWord(String k) {
        return k.equals("Reference") || k.equals("Word");
    }

    public static boolean isTwoStackWords(String k) {
        return k.equals("long") || k.equals("double");
    }

    /**
     * The string that precedes the generic template tag name to indicate type.
     * E.g. The {@code A} in {@code ALOAD}, {@code ILOAD}.
     * @param k
     * @return
     */
    public static String tagPrefix(String k) {
        return k.equals("Reference") ? "A" : (k.equals("void") ? "" : uType(k).substring(0, 1));
    }

    /**
     * The string that precedes the generic template method name to indicate type.
     * @param k
     * @return
     */
    public static String opPrefix(String k) {
        return  k.equals("Reference") ? "a" : (k.equals("void") ? "v" : lType(k).substring(0, 1));
    }

    public static boolean hasGetPutOps(String k) {
        return !k.equals("void");
    }
    public static boolean hasArrayOps(String k) {
        return !(k.equals("void") || k.equals("byte") || k.equals("Word"));
    }

    public static boolean hasPOps(String k) {
        return !(k.equals("void") || k.equals("boolean")/* || k.equals("char")*/);
    }

    public static boolean hasPCmpSwpOps(String k) {
        return k.equals("int") || k.equals("Reference") || k.equals("Word");
    }

    public static boolean hasI2Ops(String k) {
        return !(k.equals("int") || k.equals("void") || k.equals("Reference") || k.equals("Word") || k.equals("boolean"));
    }

    public static boolean hasReturnOps(String k) {
        return k.equals("int") || k.equals("long") || k.equals("float") || k.equals("double") ||
               k.equals("Word") || k.equals("Reference") || k.equals("void");
    }

    public static boolean hasInvokeOps(String k) {
        return k.equals("void") || k.equals("float") || k.equals("long") || k.equals("double") || k.equals("Word");
    }

    public static void generateAutoComment() {
        o.printf("    // GENERATED -- EDIT AND RUN main() TO MODIFY%n");
    }

    public static void newLine() {
        o.println();
    }

}
