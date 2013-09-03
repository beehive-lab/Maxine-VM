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

import java.util.concurrent.*;

import com.oracle.graal.api.code.*;
import com.sun.cri.ci.*;
import com.sun.max.program.*;


public class RegisterMap {
    private static ConcurrentHashMap<CiRegister, Register> ciToGraal = new ConcurrentHashMap<CiRegister, Register>();
    private static ConcurrentHashMap<Register, CiRegister> graalToCi = new ConcurrentHashMap<Register, CiRegister>();

    private static class Handler extends MapUtil.ClassHandler {

        @Override
        Class< ? > correspondingDefiningClass(Class< ? > klass) {
            if (klass == com.oracle.max.asm.target.amd64.AMD64.class) {
                return com.oracle.graal.amd64.AMD64.class;
            } else if (klass == com.oracle.graal.amd64.AMD64.class) {
                return com.oracle.max.asm.target.amd64.AMD64.class;
            } else {
                return correspondingFieldClass(klass);
            }
        }

        @Override
        Class< ? > correspondingFieldClass(Class< ? > klass) {
            if (klass == Register.class) {
                return CiRegister.class;
            } else if (klass == CiRegister.class) {
                return Register.class;
            }  else {
                ProgramError.unexpected("RegisterMap.correspondingFieldClass");
                return null;
            }
        }

        @Override
        void map(Object object, Object correspondingObject) {
            if (object instanceof Register) {
                graalToCi.put((Register) object, (CiRegister) correspondingObject);
                ciToGraal.put((CiRegister) correspondingObject, (Register) object);
            } else {
                ProgramError.unexpected("RegisterMap.map");
            }
        }

    }

    static {
        MapUtil.populate(com.oracle.graal.amd64.AMD64.class, Register.class, new Handler());
        MapUtil.populate(Register.class, Register.class, new Handler());
    }

    public static Register toGraal(CiRegister ciReg) {
        Register result = ciToGraal.get(ciReg);
        ProgramError.check(result != null, "RegisterMap.toGraal failure");
        return result;
    }

    public static CiRegister toCi(Register reg) {
        CiRegister result = graalToCi.get(reg);
        ProgramError.check(result != null, "RegisterMap.toCi failure");
        return result;
    }

    public static void main(String[] args) {

    }

}
