/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
 * @author Doug Simon
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
