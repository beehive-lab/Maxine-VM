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
package com.sun.max.vm.jvmti;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;

/**
 * Support for the deprecated but still used -XrunNAME option.
 */
public class RunAgentVMOption extends AgentVMOption {
    private static final String optionValueTemplate = "NAME[:<options>]";

    @HOSTED_ONLY
    public RunAgentVMOption() {
        super("-Xrun", optionValueTemplate,
              "(deprecated) load native agent library NAME, e.g. -Xrunjdwp",
              false);
    }

    @Override
    public boolean parseValue(Pointer optionValue) {
        Info info = checkSpace();
        Pointer p = optionValue;
        info.libStart = p;
        int b = 0;
        while ((b = p.readByte(0)) != (byte) 0) {
            if (b == ':') {
                p.setByte(0, (byte) 0); // zero terminate library name
                info.optionStart = p.plus(1);
                break;
            }
            p = p.plus(1);
        }
        return finishParse(info);
    }

    @Override
    public void printHelp() {
        VMOptions.printHelpForOption(category(), prefix, optionValueTemplate, help);
    }

}
