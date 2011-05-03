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

import java.util.*;

import com.sun.c1x.debug.TTY.Filter;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;

/**
 * This class encapsulates options that control the behavior of the T1X compiler.
 *
 * @author Doug Simon
 */
public final class T1XOptions {

    // Checkstyle: stop
    private static final boolean ____ = false;
    // Checkstyle: resume

    public static boolean EagerRefMaps                       = ____;

    public static boolean TraceMethods                       = ____;

    /**
     * See {@link Filter#Filter(String, Object)}.
     */
    public static String  PrintFilter                        = null;

    // printing settings
    public static boolean PrintMetrics                       = ____;
    public static boolean PrintTimers                        = ____;
    public static boolean PrintCompilation                   = ____;
    public static boolean PrintJsrRetRewrites                = ____;
    public static boolean PrintBytecodeHistogram             = ____;

    public static boolean PrintCFGToFile                     = ____;

    static {
        VMOptions.addFieldOptions("-T1X:", T1XOptions.class, getHelpMap());
    }

    @HOSTED_ONLY
    public static Map<String, String> getHelpMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("PrintCompilation",
                "Print message for each T1X compilation.");
        map.put("PrintMetrics",
                "Print T1X compilation metrics upon VM exit.");
        map.put("PrintTimers",
                "Print T1X compilation times upon VM exit.");
        map.put("PrintFilter",
                "Filter compiler tracing to methods whose fully qualified name " +
                "matches <arg>. If <arg> starts with \"~\", then <arg> (without " +
                "the \"~\") is interpreted as a regular expression. Otherwise, " +
                "<arg> is interpreted as a simple substring.");

        map.put("EagerRefMaps",
                "Generate ref maps for methods compiled by T1X at compile time " +
                "instead of lazily during a GC.");

        map.put("TraceMethods",
                "Trace calls to T1X compiled methods.");
        map.put("PrintJsrRetRewrites",
                "Print a message when T1X rewrites a method to inline jsr/ret subroutines.");

        for (String name : map.keySet()) {
            try {
                T1XOptions.class.getField(name);
            } catch (Exception e) {
                throw new InternalError("The name '" + name + "' does not denote a field in " + T1XOptions.class);
            }
        }
        return map;
    }
}
