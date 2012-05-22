/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.cri.ci;

public final class CiMonitorValue extends CiValue {
    public final CiValue owner;
    public final CiValue lockData;
    public final boolean eliminated;

    public CiMonitorValue(CiValue owner, CiValue lockData, boolean eliminated) {
        super(CiKind.Illegal);
        assert !owner.isIllegal();
        this.owner = owner;
        this.lockData = lockData == null ? CiValue.IllegalValue : lockData;
        this.eliminated = eliminated;
    }

    @Override
    public String name() {
        return "monitor";
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public boolean equalsIgnoringKind(CiValue other) {
        if (this == other) {
            return true;
        }
        if (other instanceof CiMonitorValue) {
            CiMonitorValue mon = (CiMonitorValue) other;
            return mon.owner.equalsIgnoringKind(owner) &&
                   mon.eliminated == eliminated &&
                   mon.lockData.equalsIgnoringKind(lockData);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
