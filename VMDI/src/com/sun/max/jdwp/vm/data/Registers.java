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
 * This class represents a set of registers. Each register is uniquely identified by a String name. The register names are stored in a String array.
 * The register value of each register is at the corresponding position in the register value array. A register value is always represented as a long.
 *
 * @author Thomas Wuerthinger
 *
 */
public class Registers extends AbstractSerializableObject {

    private String name;
    private String[] names;
    private long[] values;

    public Registers(String name, String[] names, long[] values) {
        this.name = name;
        this.names = names;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public String[] getRegisterNames() {
        return names;
    }

    public long[] getRegisterValues() {
        return values;
    }
}
