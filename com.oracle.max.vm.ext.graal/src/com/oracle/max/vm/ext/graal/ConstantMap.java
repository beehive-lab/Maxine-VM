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
import com.sun.max.annotate.*;
import com.sun.max.program.*;


public class ConstantMap {

    private static ConcurrentHashMap<CiConstant, Constant> ciToGraalPermMap = new ConcurrentHashMap<CiConstant, Constant>();
    private static ConcurrentHashMap<Constant, CiConstant> graalToCiPermMap = new ConcurrentHashMap<Constant, CiConstant>();

    @RESET
    private static ConcurrentHashMap<CiConstant, Constant> ciToGraalMap;
    @RESET
    private static ConcurrentHashMap<Constant, CiConstant> graalToCiMap;

    private static ConcurrentHashMap<CiConstant, Constant> getCiToGraalMap() {
        if (ciToGraalMap == null) {
            getMaps();
            ciToGraalMap = new ConcurrentHashMap<CiConstant, Constant>();
        }
        return ciToGraalMap;
    }

    private static ConcurrentHashMap<Constant, CiConstant> getGraalToCiMap() {
        if (graalToCiMap == null) {
            getMaps();
        }
        return graalToCiMap;
    }

    private static void getMaps() {
        ciToGraalMap = new ConcurrentHashMap<CiConstant, Constant>();
        ciToGraalMap.putAll(ciToGraalPermMap);
        graalToCiMap = new ConcurrentHashMap<Constant, CiConstant>();
        graalToCiMap.putAll(graalToCiPermMap);
    }

    static {
        MapUtil.populate(Constant.class, Constant.class, new Handler());
    }

    @HOSTED_ONLY
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
                graalToCiPermMap.put((Constant) object, (CiConstant) correspondingObject);
                ciToGraalPermMap.put((CiConstant) correspondingObject, (Constant) object);
            }
        }

    }

    public static Constant toGraal(CiConstant ciConstant) {
        if (ciConstant == null) {
            return null;
        }
        Constant result = getCiToGraalMap().get(ciConstant);
        if (result == null) {
            com.oracle.graal.api.meta.Kind graalKind = KindMap.toGraalKind(ciConstant.kind);
            // Checkstyle: stop
            switch (graalKind) {
                case Boolean: result = Constant.forBoolean(ciConstant.asBoolean()); break;
                case Byte: result = Constant.forByte((byte) ciConstant.asInt()); break;
                case Char: result = Constant.forChar((char) ciConstant.asInt()); break;
                case Short: result = Constant.forShort((short) ciConstant.asInt()); break;
                case Int: result = Constant.forInt(ciConstant.asInt()); break;
                case Long: result = Constant.forLong(ciConstant.asLong()); break;
                case Float: result = Constant.forFloat(ciConstant.asFloat()); break;
                case Double: result = Constant.forDouble(ciConstant.asDouble()); break;
                case Object: result = Constant.forObject(ciConstant.asObject()); break;

            }
            // Checkstyle: resume
            ciToGraalMap.put(ciConstant, result);
        }
        return result;
    }

    public static CiConstant toCi(Constant constant) {
        if (constant == null) {
            return null;
        }
        CiConstant result = getGraalToCiMap().get(constant);
        if (result == null) {
            CiKind ciKind = KindMap.toCiKind(constant.getKind());
            // Checkstyle: stop
            switch (ciKind) {
                case Boolean: result = CiConstant.forBoolean(constant.asBoolean()); break;
                case Byte: result = CiConstant.forByte((byte) constant.asInt()); break;
                case Char: result = CiConstant.forChar((char) constant.asInt()); break;
                case Short: result = CiConstant.forShort((short) constant.asInt()); break;
                case Int: result = CiConstant.forInt(constant.asInt()); break;
                case Jsr: result = CiConstant.forJsr(constant.asInt()); break;
                case Long: result = CiConstant.forLong(constant.asLong()); break;
                case Float: result = CiConstant.forFloat(constant.asFloat()); break;
                case Double: result = CiConstant.forDouble(constant.asDouble()); break;
                case Object: result = CiConstant.forObject(constant.asObject()); break;
            }
            // Checkstyle: resume
            graalToCiMap.put(constant,  result);
        }
        return result;
    }

}
