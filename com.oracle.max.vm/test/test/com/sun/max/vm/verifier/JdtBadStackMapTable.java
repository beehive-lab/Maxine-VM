/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This class triggers a bug in the Eclipse JDT compiler up to version 0.883_R34x at least.
 * The StackMapTable produced for {@link #willNotVerify()} is broken and causes a VerifyError
 * under HotSpot when the '-Xverify:all' and '-XX:-FailOverToOldVerifier' options are used.
 */
class JdtBadStackMapTable {
    public static void main(String[] args) {
        System.out.println("ok");
    }

    private static int willNotVerify() {
        int limit = 100;
        int match;
        final int result = 200;

        do {
            if (limit > 0) {
                continue;
            }

            match = 0;

            while (++match < 100) {
                // empty
            }

        } while (--limit != 0);

        return result;
    }
}
