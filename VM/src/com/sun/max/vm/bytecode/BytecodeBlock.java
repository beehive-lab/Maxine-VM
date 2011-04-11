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
package com.sun.max.vm.bytecode;

/**
 * Extends the notion of a {@linkplain BCIRange BCI range} to refer to a specific underlying
 * {@linkplain #code() code array}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class BytecodeBlock extends BCIRange {

    private final byte[] code;

    public BytecodeBlock(byte[] bytecode, int startBCI, int endBCI) {
        super(startBCI, endBCI);
        code = bytecode;
        assert check();
    }

    public BytecodeBlock(byte[] bytecode) {
        super(0, bytecode.length - 1);
        code = bytecode;
        assert check();
    }

    private boolean check() {
        assert code != null;
        assert code.length > 0;
        assert start >= 0;
        assert end >= start;
        assert end <= code.length;
        return true;
    }

    /**
     * Gets the code to which the range of BCIs in this object refer.
     */
    public byte[] code() {
        return code;
    }

    /**
     * Gets the number of BCIs covered by this block.
     */
    public int size() {
        return end - start + 1;
    }
}
