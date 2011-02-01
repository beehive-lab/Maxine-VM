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
package com.sun.max.vm.cps.jit;

import com.sun.max.lang.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A constant modifier is a helper for modifying target code.
 * The modifier is specific to an annotated target code and can be used to
 * modify copies of that target code to adapt the code to a different constant value.
 * Currently used by the template-based JIT.
 *
 * @author Laurent Daynes
 */
public class ConstantModifier extends InstructionModifier {
    /**
     * Original value of the constant.
     */
    private final Value constantValue;

    public ConstantModifier(int position, int size, Value value) {
        super(position, size);
        constantValue = value;
    }

    public Kind kind() {
        return constantValue.kind();
    }

    public WordWidth signedEffectiveWidth() {
        return constantValue.signedEffectiveWidth();
    }

    public Value getConstantValue() {
        return constantValue;
    }
}
