/*
 * Copyright (c) 2009, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm;

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * Implements the VM options controlling assertions.
 *
 * @author Doug Simon
 */
public class AssertionsVMOption extends VMOption {

    public static AssertionsVMOption ASSERTIONS = register(new AssertionsVMOption(), MaxineVM.Phase.STARTING);

    @HOSTED_ONLY
    public AssertionsVMOption() {
        super("-ea", "");
    }

    @Override
    public boolean matches(Pointer arg) {
        return
            CString.startsWith(arg, "-ea") || CString.startsWith(arg, "-enableassertions") ||
            CString.startsWith(arg, "-da") || CString.startsWith(arg, "-disableassertions") ||
            CString.equals(arg, "-esa") || CString.equals(arg, "-enablesystemassertions") ||
            CString.equals(arg, "-dsa") || CString.equals(arg, "-disablesystemassertions");
    }

    @Override
    public boolean parseValue(Pointer optionValue) {
        // TODO: Implement this!
        return true;
    }

    @Override
    public void printHelp() {
        VMOptions.printHelpForOption(category(), "-ea[:<packagename>...|:<classname>]", "", null);
        VMOptions.printHelpForOption(category(), "-enableassertions[:<packagename>...|:<classname>]", "", "enable assertions");
        VMOptions.printHelpForOption(category(), "-da[:<packagename>...|:<classname>]", "", null);
        VMOptions.printHelpForOption(category(), "-disableassertions[:<packagename>...|:<classname>]", "", "disable assertions");
        VMOptions.printHelpForOption(category(), "-esa | -enablesystemassertions", "", "enable system assertions");
        VMOptions.printHelpForOption(category(), "-dsa | -disablesystemassertions", "", "disable system assertions");
    }
}
