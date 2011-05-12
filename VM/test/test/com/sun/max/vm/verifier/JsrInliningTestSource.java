/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.verifier;

import com.sun.max.ide.*;
import com.sun.max.program.*;

/**
 * This is a class that contains methods containing various {@code finally} clauses.
 * To ensure that the class file produced for this source file uses the JSR and
 * RET bytecodes, then the following options must be passed to javac:
 * <p>
 * <pre>
 *     -source 1.4 -target 1.4 -XDjsrlimit=0
 * </pre>
 *
 */
public class JsrInliningTestSource {

    public int simple() {
        int a;
        try {
            a = 1;
        } finally {
            a = 2;
        }
        return a;
    }

    public int nested() {
        int a;
        try {
            a = 1;
        } finally {
            try {
                a = 2;
            } finally {
                a = 3;
            }
        }
        return a;
    }

    // Cannot use @SuppressWarnings("finally") as it is not Java 1.4 compliant
    //@SuppressWarnings("finally")
    public static int loops() {
        int loops = 0;
        try {
            final boolean b = true;

            while (b) {
                ++loops;
                try {
                    return loops;
                } catch (Exception ex) {
                }
            }
        } finally {
            return loops;
        }
    }

    // Cannot use @SuppressWarnings("finally") as it is not Java 1.4 compliant
    //@SuppressWarnings("finally")
    public static int oops(int i) {
        int p = i;
        while (p > 10) {
            try {
                --p;
            } finally {
                if (p < 4) {
                    return p;
                }
                continue;
            }
        }
        return p;
    }

    /**
     * This class includes a number of methods that do not pass the standard Sun verifier (as found in Hotspot).
     * They are drawn from the multitude of publications showing the short-comings of
     * bytecode verification as specified in the JVM specification (prior to JSR 202) and as implemented in
     * the type inferencing based verifier (i.e. j2se/src/share/native/common/check_code.c).
     */
    public static class Unverifiable {

        public int m1(boolean b) {
            int i;
            try {
                if (b) {
                    return 1;
                }
                i = 2;
            } finally {
                if (b) {
                    i = 3;
                }
            }
            return i;
        }

        public int m2(boolean b) {
            int i;
        L:
            {
                try {
                    if (b) {
                        return 1;
                    }
                    i = 2;
                    if (b) {
                        break L;
                    }
                } finally {
                    if (b) {
                        i = 3;
                    }
                }
                i = 4;
            }
            return i;
        }
    }

    public static boolean compile() {
        final String thisClassName = JsrInliningTestSource.class.getName();
        if (ToolChain.compile(JsrInliningTestSource.class, thisClassName, new String[]{"-noinlinejsr"})) {
            return true;
        }
        ProgramWarning.message("compilation failed for: " + thisClassName);
        return false;
    }

    public static void main(String[] args) throws Exception {
        if (compile()) {
            System.out.println("Successfully compiled " + JsrInliningTestSource.class);
        }
    }

    public JsrInliningTestSource() {
    }
}
