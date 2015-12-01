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
package jtt.max;

import com.sun.max.unsafe.*;

/*
 * @Harness: java
 * @Runs: 0=true; 1=true; 2=true; 3=false
 */
public final class CodePointer02 {

    public static final long LONG_VALUE = 23L;

    public static boolean test(int arg) {
        switch (arg) {
            case 0: {
                CodePointer cp = CodePointer.from(LONG_VALUE);
                System.gc();
                return LONG_VALUE == cp.toLong();
            }
            case 1: {
                Address address = Address.fromLong(LONG_VALUE);
                CodePointer cp = CodePointer.from(address);
                System.gc();
                return LONG_VALUE == cp.toAddress().toLong();
            }
            case 2: {
                Pointer pointer = Pointer.fromLong(LONG_VALUE);
                CodePointer cp = CodePointer.from(pointer);
                System.gc();
                return LONG_VALUE == cp.toPointer().toLong();
            }
            default:
                return false;
        }
    }
}
