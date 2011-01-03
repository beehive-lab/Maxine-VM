/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.monitor;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AbstractMonitorScheme extends AbstractVMScheme  implements MonitorScheme {

    @HOSTED_ONLY
    protected AbstractMonitorScheme() {
    }

    private int counter;

    @INLINE
    public final int createHashCode(Object object) {
        if (MaxineVM.isHosted()) {
            return System.identityHashCode(object);
        }

        int hashCode = Reference.fromJava(object).toOrigin().unsignedShiftedRight(3).toInt() ^ counter++;
        // Ensure the hash code is positive. Even though the specification does not require this, at
        // least one application (NetBeans) assumes this is the case (see https://netbeans.org/bugzilla/show_bug.cgi?id=178688).
        return hashCode & ~0x80000000;
    }

}
