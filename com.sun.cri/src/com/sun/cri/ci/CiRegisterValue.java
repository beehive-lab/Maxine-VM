/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.cri.ci;


/**
 * Denotes a register that stores a value of a fixed kind. There is exactly one (canonical) instance of {@code
 * CiRegisterValue} for each ({@link CiRegister}, {@link CiKind}) pair. Use {@link CiRegister#asValue(CiKind)} to
 * retrieve the canonical {@link CiRegisterValue} instance for a given (register,kind) pair.
 */
public final class CiRegisterValue extends CiValue {

    private static final long serialVersionUID = -8085239665007333977L;
    /**
     * The register.
     */
    public final CiRegister reg;

    /**
     * Should only be called from {@link CiRegister#CiRegister} to ensure canonicalization.
     */
    public CiRegisterValue(CiKind kind, CiRegister register) {
        super(kind);
        this.reg = register;
    }

    CiRegisterValue(CiRegisterValue regValue) {
        super(regValue.kind);
        assert regValue.kind.isLong() || regValue.kind.isDouble() : regValue.kind;
        String name = regValue.reg.name.substring(0, 1) + (new Integer(regValue.reg.name.substring(1, 2)) + 1);
        this.reg = new CiRegister(regValue.reg.number + 1, regValue.reg.getEncoding(), regValue.reg.spillSlotSize, name, regValue.reg.flags);
        highPart = true;
    }

    @Override
    public int hashCode() {
        return kind.ordinal() ^ reg.number;
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public boolean equalsIgnoringKind(CiValue other) {
        if (other instanceof CiRegisterValue) {
            return ((CiRegisterValue) other).reg == reg;
        }
        return false;
    }

    @Override
    public CiRegisterValue getClone() {
        if (kind.isLong() || kind.isDouble()) {
            return new CiRegisterValue(this);
        } else {
            return null;
        }
    }

    @Override
    public String name() {
        return reg.name;
    }

    @Override
    public CiRegister asRegister() {
        return reg;
    }
}
