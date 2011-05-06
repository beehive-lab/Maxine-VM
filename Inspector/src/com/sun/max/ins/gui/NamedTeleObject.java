/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.gui;

import com.sun.max.tele.object.*;

/**
 * Binds a name to a {@link TeleObject} in the VM.
 *
 * @author Michael Van De Vanter
  */
public class NamedTeleObject implements Comparable<NamedTeleObject> {

    private final String name;

    private final TeleObject teleObject;

    public NamedTeleObject(String name, TeleObject teleObject) {
        this.name = name;
        this.teleObject = teleObject;
    }

    public String name() {
        return name;
    }

    public TeleObject teleObject() {
        return teleObject;
    }

    public int compareTo(NamedTeleObject o) {
        return name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return name;
    }
}
