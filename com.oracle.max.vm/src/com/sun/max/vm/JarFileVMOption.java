/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * Common superclass for VM options with a JAR file and optional arguments, e.g. {@code -javaagent}.
 */
public class JarFileVMOption extends VMOption {
    private List<VMStringOption> optionList = new ArrayList<VMStringOption>();
    public final String optionName;

    @HOSTED_ONLY
    public JarFileVMOption(String optionName, String help) {
        super(optionName, help);
        this.optionName = optionName;
    }

    /**
     * Return the number of instances of this option.
     * @return the number of instances of this option.
     */
    public int count() {
        return optionList.size();
    }

    /**
     * Returns the ith option value.
     * @param index into list of option values
     * @return the ith option value
     */
    public String getValue(int i) {
        return optionList.get(i).getValue();
    }

    @Override
    public boolean parseValue(Pointer optionValue) {
        VMStringOption option = new VMStringOption(prefix);
        optionList.add(option);
        return option.parseValue(optionValue);
    }

    @Override
    public void printHelp() {
        VMOptions.printHelpForOption(category(), prefix, ":<jarpath>[=<options>]", help);
    }

}
