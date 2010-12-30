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
package com.sun.max.vm.cps.eir;

import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.value.*;

/**
 * Abstract location for Eir variables.
 *
 * An Eir variable may be a constant, in which case it may have the immediate or literal location, depending on whether the constant value fits in an immediate operand
 * of the instruction that use the variable. An Eir variable may also be located on the stack, or in a register.
 * @author Bernd Mathiske
 */
public abstract class EirLocation {

    protected EirLocation() {
    }

    public abstract EirLocationCategory category();

    public EirStackSlot asStackSlot() {
        return null;
    }

    public EirLiteral asLiteral() {
        return (EirLiteral) this;
    }

    public EirImmediate asImmediate() {
        return (EirImmediate) this;
    }

    public EirRegister asRegister() {
        return null;
    }

    public abstract TargetLocation toTargetLocation();

    public abstract static class Constant extends EirLocation {
        private Value value;

        public Value value() {
            return value;
        }

        protected Constant(Value value) {
            this.value = value;
        }
    }

    public static final class Undefined extends EirLocation {
        private Undefined() {
        }

        @Override
        public EirLocationCategory category() {
            return EirLocationCategory.UNDEFINED;
        }

        @Override
        public TargetLocation toTargetLocation() {
            return TargetLocation.undefined;
        }
    }

    public static final Undefined UNDEFINED = new Undefined();

}
