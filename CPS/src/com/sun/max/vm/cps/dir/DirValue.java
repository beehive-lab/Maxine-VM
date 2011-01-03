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
package com.sun.max.vm.cps.dir;

import com.sun.max.vm.cps.dir.transform.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public abstract class DirValue implements IrValue {

    protected DirValue() {
    }

    public Value value() {
        throw new IllegalArgumentException();
    }

    public boolean isZeroConstant() {
        return this instanceof DirConstant && value().isZero();
    }

    public Value toStackValue() {
        return value().kind().stackKind.convert(value());
    }

    public int hashCodeForBlock() {
        return getClass().getName().hashCode();
    }

    public static final class Undefined extends DirValue {
        private Undefined() {
        }

        public Kind kind() {
            return Kind.VOID;
        }

        public boolean isConstant() {
            return false;
        }

        @Override
        public String toString() {
            return "UNDEFINED";
        }
    }

    public boolean isEquivalentTo(DirValue other, DirBlockEquivalence equivalence) {
        return other == this;
    }

    public static final Undefined UNDEFINED = new Undefined();
}
