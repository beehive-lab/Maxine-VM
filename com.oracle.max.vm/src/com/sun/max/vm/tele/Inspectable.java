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
package com.sun.max.vm.tele;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;

/**
 * Holder for magic word that communicates whether this VM is being inspected and possibly
 * other flags.
 */
public final class Inspectable {

    private Inspectable() {
    }

    /**
     * Option to make the VM run inspectable when VM not created by Inspector.
     */

    public static boolean MakeInspectable;
    static {
        VMOptions.addFieldOption("-XX:", "MakeInspectable", Inspectable.class, "Make it possible for Inspector to attach to running VM", Phase.PRISTINE);
    }

    /**
     * Constant denoting that the VM process is being inspected.
     */
    public static final int INSPECTED = 0x0000001;

    /**
     * If a non-zero value is put here remotely, or by command line option, then the
     * additional steps to facilitate inspection should be activated.
     */
    @INSPECTED
    private static int flags;

    private static boolean optionChecked;

    /**
     * Determines if the VM process is being inspected.
     */
    public static boolean isVmInspected() {
        if ((flags & INSPECTED) != 0) {
            return true;
        }
        if (optionChecked || MaxineVM.isHosted()) {
            // A hosted VM is never inspected, plus it avoids setting optionChecked
            // during a build, which would then need resetting
            return false;
        }
        if (MakeInspectable) {
            flags = INSPECTED;
        }
        optionChecked = true;
        return (flags & INSPECTED) != 0;
    }

}
