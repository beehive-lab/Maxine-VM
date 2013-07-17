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
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.classfile.constant.*;


public class MaxJavaType implements JavaType {

    protected RiType riType;

    @RESET
    private static ConcurrentHashMap<RiType, MaxJavaType> map;

    private static ConcurrentHashMap<RiType, MaxJavaType> getMap() {
        if (map == null) {
            map = new ConcurrentHashMap<RiType, MaxJavaType>();
        }
        return map;
    }

    public static MaxJavaType get(RiType riType) {
        if (riType == null) {
            return null;
        }
        MaxJavaType result = getMap().get(riType);
        if (result == null) {
            if (riType instanceof RiResolvedType) {
                result = new MaxResolvedJavaType((RiResolvedType) riType);
            } else {
                result = new MaxJavaType(riType);
            }
            map.put(riType, result);
        }
        return result;
    }

    public static RiType getRiType(JavaType javaType) {
        return ((MaxJavaType) javaType).riType;
    }

    protected MaxJavaType(RiType riType) {
        this.riType = riType;
    }

    @Override
    public String getName() {
        return riType.name();
    }

    @Override
    public JavaType getComponentType() {
        return get(riType.componentType());
    }

    @Override
    public JavaType getArrayClass() {
        return get(riType.arrayOf());
    }

    @Override
    public Kind getKind() {
        return KindMap.toGraalKind(riType.kind(false));
    }

    @Override
    public ResolvedJavaType resolve(ResolvedJavaType accessingClass) {
        if (this instanceof ResolvedJavaType) {
            return (ResolvedJavaType) this;
        }
        UnresolvedType uType = (UnresolvedType) riType;
        RiType uRiType = UnresolvedType.toRiType(uType.typeDescriptor, MaxResolvedJavaType.getRiType(accessingClass));
        return MaxResolvedJavaType.get((RiResolvedType) uRiType);
    }

}
