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

import com.oracle.graal.api.meta.*;
import com.sun.cri.ci.*;
import com.sun.max.program.*;


public class ConstantMap {

    private static ConcurrentHashMap<CiConstant, Constant> ciToGraal = new ConcurrentHashMap<CiConstant, Constant>();
    private static ConcurrentHashMap<Constant, CiConstant> graalToCi = new ConcurrentHashMap<Constant, CiConstant>();

    private static class Handler extends MapUtil.ClassHandler {

        @Override
        Class< ? > correspondingDefiningClass(Class< ? > klass) {
            if (klass == Constant.class) {
                return CiConstant.class;
            } else if (klass == CiConstant.class) {
                return Constant.class;
            } else {
                ProgramError.unexpected("ConstantMap.correspondingDefiningClass");
                return null;
            }
        }

        @Override
        Class< ? > correspondingFieldClass(Class< ? > klass) {
            return correspondingDefiningClass(klass);
        }

        @Override
        void map(Object object, Object correspondingObject) {
            if (object instanceof Constant) {
                graalToCi.put((Constant) object, (CiConstant) correspondingObject);
                ciToGraal.put((CiConstant) correspondingObject, (Constant) object);
            }
        }

    }

    static {
        MapUtil.populate(Constant.class, Constant.class, new Handler());
    }

    public static Constant toGraal(CiConstant ciConstant) {
        if (ciConstant == null) {
            return null;
        }
        Constant result = ciToGraal.get(ciConstant);
        if (result == null) {
            com.oracle.graal.api.meta.Kind graalKind = KindMap.toGraalKind(ciConstant.kind);
            switch (graalKind) {
                case Boolean: result = Constant.forBoolean(ciConstant.asBoolean()); break;
                case Byte: result = Constant.forByte((byte) ciConstant.asInt()); break;
                case Char: result = Constant.forChar((char) ciConstant.asInt()); break;
                case Short: result = Constant.forShort((short) ciConstant.asInt()); break;
                case Int: result = Constant.forInt(ciConstant.asInt()); break;
                case Jsr: result = Constant.forJsr(ciConstant.asJsr()); break;
                case Long: result = Constant.forLong(ciConstant.asLong()); break;
                case Float: result = Constant.forFloat(ciConstant.asFloat()); break;
                case Double: result = Constant.forDouble(ciConstant.asDouble()); break;
                case Object: result = Constant.forObject(ciConstant.asObject()); break;

            }
            ciToGraal.put(ciConstant, result);
        }
        return result;
    }

    public static CiConstant toCi(Constant constant) {
        if (constant == null) {
            return null;
        }
        CiConstant result = graalToCi.get(constant);
        if (result == null) {
            CiKind ciKind = KindMap.toCiKind(constant.getKind());
            switch (ciKind) {
                case Boolean: result = CiConstant.forBoolean(constant.asBoolean()); break;
                case Byte: result = CiConstant.forByte((byte) constant.asInt()); break;
                case Char: result = CiConstant.forChar((char) constant.asInt()); break;
                case Short: result = CiConstant.forShort((short) constant.asInt()); break;
                case Jsr: result = CiConstant.forJsr(constant.asInt()); break;
                case Long: result = CiConstant.forLong(constant.asLong()); break;
                case Float: result = CiConstant.forFloat(constant.asFloat()); break;
                case Double: result = CiConstant.forDouble(constant.asDouble()); break;
                case Object: result = CiConstant.forObject(constant.asObject()); break;
            }
            graalToCi.put(constant,  result);
        }
        return result;
    }


    /*
    private static class CiConstantProxy {
        @INTRINSIC(UNSAFE_CAST) public static native CiConstantProxy asCiConstantProxy(Object object);

        @ALIAS(declaringClass = CiConstant.class)
        private Object object;
        @ALIAS(declaringClass = CiConstant.class)
        private long primitive;
    }
    */

    public static void main(String[] args) {
        System.console();
    }
}
