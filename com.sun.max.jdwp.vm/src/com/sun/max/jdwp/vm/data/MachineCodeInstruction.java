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
package com.sun.max.jdwp.vm.data;

/**
 * This class represents a machine code instruction of a target method.
 *
 */
public class MachineCodeInstruction extends AbstractSerializableObject {

    private final String mnemonic;
    private final int position;
    private final long address;
    private final String label;
    private final byte[] bytes;
    private final String operands;
    private final long targetAddress;

    public MachineCodeInstruction(String mnemonic, int position, long address, String label, byte[] bytes, String operands, long targetAddress) {
        this.mnemonic = mnemonic;
        this.position = position;
        this.address = address;
        this.label = label;
        this.bytes = bytes;
        this.operands = operands;
        this.targetAddress = targetAddress;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public int getPosition() {
        return position;
    }

    public long getAddress() {
        return address;
    }

    public String getLabel() {
        return label;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getOperands() {
        return operands;
    }

    public long getTargetAddress() {
        return targetAddress;
    }
}
