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
package com.sun.max.vm.jvmti;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;

/**
 * Support for the (repeatable) agentlib and agentpath command line options.
 * Tricky because these options are parsed in the {@link MaxineVM.Phase#PRISTINE}
 * phase and no heap is available.
 */

public abstract class AgentVMOption extends VMOption {

    static class Info {
        boolean isAbsolute;
        Pointer libStart;
        Pointer optionStart = Pointer.zero();
    }

    /**
     * Pre-allocated storage for individual options.
     */
    protected static Info[] infoArray = new Info[JVMTI.MAX_ENVS];

    protected static int index;

    static {
        for (int i = 0; i < infoArray.length; i++) {
            infoArray[i] = new Info();
        }
    }

    private String usage;

    protected boolean expectsAbsolute;

    @HOSTED_ONLY
    protected AgentVMOption(String prefix, String optionValueTemplate, String help, boolean expectsAbsolute) {
        super(prefix, help);
        usage = prefix + optionValueTemplate;
        this.expectsAbsolute = expectsAbsolute;
    }

    protected Info checkSpace() {
        if (index >= infoArray.length) {
            Log.println("too many -agentpath/-agentlib/-Xrun options");
            MaxineVM.native_exit(1);
        }
        infoArray[index].isAbsolute = expectsAbsolute;
        return infoArray[index];
    }

    protected void usageError() {
        Log.println(usage);
        MaxineVM.native_exit(1);
    }

    protected boolean finishParse(Info info) {
        if (info.optionStart.isZero()) {
            info.optionStart = Memory.allocate(Size.fromInt(1));
            info.optionStart.setByte((byte) 0);
        }
        index++;
        return true;
    }

    @Override
    public boolean parseValue(Pointer optionValue) {
        Info info = checkSpace();
        Pointer p = optionValue;
        if (p.readByte(0) != ':') {
            usageError();
        }
        p = p.plus(1);
        info.libStart = p;
        int b = 0;
        while ((b = p.readByte(0)) != (byte) 0) {
            if (b == '=') {
                p.setByte(0, (byte) 0); // zero terminate library string
                info.optionStart = p.plus(1);
                break;
            }
            p = p.plus(1);
        }
        return finishParse(info);
    }

    public static int count() {
        return index;
    }

    /**
     * Get address of library string for i'th occurrence.
     * @param i
     * @return
     */
    public static Info getInfo(int i) {
        return infoArray[i];
    }


}
