/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package jtt.exbytecode;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.builtin.*;

/*
 * @Harness: java
 * @Runs: 0=true; 1=true; 34=true
 */
public class EBC_lsa01 {
    public static boolean test(int i) {
        Pointer addr = MakeStackVariable.makeStackVariable(i);
        Pointer addr2 = MakeStackVariable.makeStackVariable(i + 1);

        if (i == 0) {
            if (!addr.isZero()) {
                return false;
            }
        } else if (addr.readInt(0) != i) {
            return false;
        }

        if (i + 1 == 0) {
            if (!addr2.isZero()) {
                return false;
            }
        } else if (addr2.readInt(0) != i + 1) {
            return false;
        }
        return true;
    }
}
