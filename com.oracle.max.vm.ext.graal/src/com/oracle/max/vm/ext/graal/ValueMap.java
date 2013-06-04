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
package com.oracle.max.vm.ext.graal;

import static com.oracle.max.vm.ext.graal.MaxGraal.unimplemented;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.sun.cri.ci.*;
import com.sun.max.unsafe.*;


public class ValueMap {

    public static Value toGraal(CiValue ciValue) {
        if (ciValue == null) {
            return null;
        }
        if (ciValue instanceof CiConstant) {
            return ConstantMap.toGraal((CiConstant) ciValue);
        } else if (ciValue instanceof CiRegisterValue) {
            CiRegisterValue ciReg = (CiRegisterValue) ciValue;
            return RegisterMap.toGraal(ciReg.reg).asValue(KindMap.toGraalKind(ciReg.kind));
        } else if (ciValue instanceof CiStackSlot) {
            CiStackSlot ciStackSlot = (CiStackSlot) ciValue;
            return StackSlot.get(KindMap.toGraalKind(ciStackSlot.kind), ciStackSlot.index() * Word.size(), ciStackSlot.inCallerFrame());
        } else {
            unimplemented("ValueMap.toGraal");
        }
        return null;
    }

    public static CiValue toCi(Value value, int totalFrameSize) {
        if (value == null) {
            return null;
        }
        if (value instanceof Constant) {
            return ConstantMap.toCi((Constant) value);
        } else if (value instanceof RegisterValue) {
            return RegisterMap.toCi(((RegisterValue) value).getRegister()).asValue();
        } else if (value instanceof StackSlot) {
            StackSlot stackSlot = (StackSlot) value;
            int offset = stackSlot.getOffset(totalFrameSize);
            return CiStackSlot.get(KindMap.toCiKind(stackSlot.getKind()), offset / Word.size(), stackSlot.isInCallerFrame());
        } else if (value.getKind() == Kind.Illegal) {
            return CiValue.IllegalValue;
        } else {
            unimplemented("ValueMap.toCi");
        }
        return null;
    }

    public static CiValue[] toCi(Value[] values, int totalFrameSize) {
        CiValue[] result = new CiValue[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = toCi(values[i], totalFrameSize);
        }
        return result;
    }
}
