/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
 * An entry in the variable table as specified by the JDWP protocol.
 *
 * @author Thomas Wuerthinger
 *
 */
public class VariableTableEntry extends AbstractSerializableObject {
    private long codeIndex;
    private int length;
    private String name;
    private int slot;
    private String signature;
    private String genericSignature;

    public VariableTableEntry(long codeIndex, int length, String name, int slot, String signature, String genericSignature) {
        this.codeIndex = codeIndex;
        this.length = length;
        this.name = name;
        this.slot = slot;
        this.signature = signature;
        this.genericSignature = genericSignature;
    }

    public long getCodeIndex() {
        return codeIndex;
    }

    public int getLength() {
        return length;
    }

    public String getName() {
        return name;
    }

    public int getSlot() {
        return slot;
    }

    public String getSignature() {
        return signature;
    }

    public String getGenericSignature() {
        return genericSignature;
    }
}
