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

public abstract class NativeAgentVMOption extends VMOption {

    /**
     * Pre-allocated storage for individual options.
     */
    private static NativeString[] nativeStrings = new NativeString[JVMTI.MAX_ENVS];

    private static int index;

    static {
        for (int i = 0; i < nativeStrings.length; i++) {
            nativeStrings[i] = new NativeString();
        }
    }

    private static String usage;

    @HOSTED_ONLY
    protected NativeAgentVMOption(String prefix, String optionValueTemplate, String help) {
        super(prefix, help);
        usage = prefix + optionValueTemplate;
    }

    @Override
    public boolean parseValue(Pointer optionValue) {
        if (index >= nativeStrings.length) {
            Log.println("too many -agentpath/-agentlib options");
            MaxineVM.native_exit(1);
        }
        Pointer p = optionValue;
        if (p.readByte(0) != ':') {
            Log.println(usage);
        }
        p = p.plus(1);
        nativeStrings[index].libStart = p;
        int b = 0;
        while ((b = p.readByte(0)) != (byte) 0) {
            if (b == '=') {
                p.setByte(0, (byte) 0); // zero terminate library string
                nativeStrings[index].optionStart = p.plus(1);
                break;
            }
            p = p.plus(1);
        }
        index++;
        return true;
    }

    public int count() {
        return index;
    }

    /**
     * Get address of library string for i'th occurrence.
     * @param i
     * @return
     */
    public Pointer getLibStart(int i) {
        return nativeStrings[i].libStart;
    }

    /**
     * Get address of option string for i'th occurrence or empty string if none.
     * @param i
     * @return
     */
    public Pointer getOptionStart(int i) {
        Pointer result = nativeStrings[i].optionStart;
        if (result.isZero()) {
            result = Memory.allocate(Size.fromInt(1));
            result.setByte((byte) 0);
        }
        return result;
    }

    static class NativeString {
        Pointer libStart;
        Pointer optionStart = Pointer.zero();
    }

}
