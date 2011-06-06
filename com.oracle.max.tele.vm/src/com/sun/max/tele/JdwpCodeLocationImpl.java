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
package com.sun.max.tele;

import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * Class representing a location that can either point to a machine code instruction or a Java bytecode.
 *
 * @author Thomas Wuerthinger
 */
class JdwpCodeLocationImpl implements JdwpCodeLocation{

    private MethodProvider method;
    private long position;
    private boolean isMachineCode;

    JdwpCodeLocationImpl(MethodProvider method) {
        this(method, 0);
    }

    JdwpCodeLocationImpl(MethodProvider method, int position) {
        this(method, position, false);
    }

    JdwpCodeLocationImpl(MethodProvider method, long position, boolean isMachineCode) {
        this.method = method;
        this.position = position;
        this.isMachineCode = isMachineCode;

        long max = Integer.MIN_VALUE;
        long min = Integer.MAX_VALUE;
        for (LineTableEntry entry : method.getLineTable()) {
            max = Math.max(entry.getCodeIndex(), max);
            min = Math.min(entry.getCodeIndex(), min);
        }

        if (max == Integer.MIN_VALUE) {
            assert position == -1;
        } else {
            assert position >= min && position <= max;
        }
    }

    public MethodProvider method() {
        return method;
    }

    public long position() {
        return position;
    }

    public boolean isMachineCode() {
        return isMachineCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JdwpCodeLocationImpl)) {
            return false;
        }
        final JdwpCodeLocationImpl cl = (JdwpCodeLocationImpl) obj;
        return cl.method().equals(method()) && cl.position == position && cl.isMachineCode == isMachineCode;
    }

    @Override
    public int hashCode() {
        return (int) (position << 8) + method.hashCode() + (isMachineCode ? 1 : 0);
    }

    @Override
    public String toString() {
        return "Location[" + method.toString() + ", " + position + ", " + isMachineCode + "]";
    }
}
