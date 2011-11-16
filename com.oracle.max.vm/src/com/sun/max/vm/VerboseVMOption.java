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
package com.sun.max.vm;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * Implements the "-verbose[:class|gc|jni|comp]" VM option.
 */
public class VerboseVMOption extends VMOption {

    /**
     * Determines if information should be displayed about each class loaded.
     */
    public boolean verboseClass;

    /**
     * Determines if information should be displayed about each compiler event.
     */
    public boolean verboseCompilation;

    /**
     * Determines if information should be displayed about each garbage collection event.
     */
    public boolean verboseGC;

    /**
     * Determines if information should be displayed about the {@linkplain System#getProperties() system properties} when
     * they initialized during VM startup.
     */
    public boolean verboseProperties;

    /**
     * Determines if information should be displayed about use of native methods and other Java Native Interface activity.
     */
    public boolean verboseJNI;

    @HOSTED_ONLY
    public VerboseVMOption() {
        super("-verbose", "Enables verbose output. ");
    }

    @Override
    public boolean parseValue(Pointer optionValue) {
        if (CString.length(optionValue).isZero()) {
            verboseClass = true;
            verboseGC = true;
            verboseJNI = true;
            verboseCompilation = true;
            verboseProperties = true;
        } else if (CString.equals(optionValue, ":gc")) {
            verboseGC = true;
        } else if (CString.equals(optionValue, ":class")) {
            verboseClass = true;
        } else if (CString.equals(optionValue, ":jni")) {
            verboseJNI = true;
        } else if (CString.equals(optionValue, ":comp")) {
            verboseCompilation = true;
        } else if (CString.equals(optionValue, ":props")) {
            verboseProperties = true;
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void printHelp() {
        VMOptions.printHelpForOption(category(), "-verbose[:class|gc|jni|comp|props]", "", help);
    }
}
