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
package com.sun.max.vm;

import static com.sun.max.vm.VMOptions.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;

/**
 * Implements the VM options controlling assertions.
 */
public class AssertionsVMOption extends VMOption {

    public static AssertionsVMOption ASSERTIONS = register(new AssertionsVMOption(), MaxineVM.Phase.STARTING);

    /**
     * The following fields match those in {@link java.lang.AssertionStatusDirectives}.
     */
    public static ArrayList<String> packages = new ArrayList<String>();
    public static ArrayList<Boolean> packageEnabled = new ArrayList<Boolean>();
    public static ArrayList<String> classes = new ArrayList<String>();
    public static ArrayList<Boolean> classEnabled = new ArrayList<Boolean>();
    public static boolean deflt;
    // extra field for system class special case
    public static boolean systemDeflt;

    @HOSTED_ONLY
    public AssertionsVMOption() {
        super("-ea", "");
    }

    @Override
    public boolean matches(Pointer arg) {
        return isEnabling(arg) || isDisabling(arg) || isSystemEnabling(arg) || isSystemDisabling(arg);
    }

    @Override
    public boolean parseValue(Pointer optionValue) {
        // Since there are many variants we have to parse from VMOption.optionStart
        if (isSystemEnabling(optionStart)) {
            systemDeflt = true;
        } else if (isSystemDisabling(optionStart)) {
            systemDeflt = false;
        } else {
            boolean enabling = isEnabling(optionStart);
            if (optionValue.getByte() == 0) {
                // easy case, no value provided
                deflt = enabling;
            } else {
                // [:<package name>"..." | :<class name> ]
                char ch = (char) optionValue.getByte();
                if (ch != ':') {
                    return false;
                }
                Pointer ptr = optionValue.plus(1);
                Pointer valueStart = ptr;
                int count = 0;
                while (true) {
                    ch = (char) ptr.getByte();
                    if (ch == 0) {
                        break;
                    }
                    ptr = ptr.plus(1);
                    count++;
                }
                // ptr now pointing at the 0
                boolean isPackage = false;
                if (count >= 3) {
                    // check for ...
                    if (isWild(ptr.minus(3))) {
                        // if (count == 3) denotes default package (empty string)
                        ptr = ptr.minus(3);
                        ptr.setByte((byte) 0);
                        isPackage = true;
                    } // else denotes a class
                }
                try {
                    String name = CString.utf8ToJava(valueStart);
                    if (isPackage) {
                        // ClassLoader.desiredAssertionStatus denotes default package with null not the empty string!
                        if (name.length() == 0) {
                            name = null;
                        }
                        packages.add(name);
                        packageEnabled.add(enabling);
                    } else {
                        classes.add(name);
                        classEnabled.add(enabling);
                    }
                } catch (Utf8Exception ex) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isWild(Pointer ptr) {
        return ptr.getByte(0) == '.' && ptr.getByte(1) == '.' && ptr.getByte(2) == '.';
    }

    private static boolean isSystemEnabling(Pointer arg) {
        return CString.equals(arg, "-esa") || CString.equals(arg, "-enablesystemassertions");
    }

    private static boolean isSystemDisabling(Pointer arg) {
        return CString.equals(arg, "-dsa") || CString.equals(arg, "-disablesystemassertions");
    }

    private static boolean isEnabling(Pointer arg) {
        return CString.startsWith(arg, "-ea") || CString.startsWith(arg, "-enableassertions");
    }

    private static boolean isDisabling(Pointer arg) {
        return CString.startsWith(arg, "-da") || CString.startsWith(arg, "-disableassertions");
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
