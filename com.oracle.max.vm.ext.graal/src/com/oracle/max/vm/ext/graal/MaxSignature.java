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


public class MaxSignature implements Signature {

    private RiSignature riSignature;

    private static ConcurrentHashMap<RiSignature, MaxSignature> map = new ConcurrentHashMap<RiSignature, MaxSignature>();


    static MaxSignature get(RiSignature signature) {
        MaxSignature result = map.get(signature);
        if (result == null) {
            result = new MaxSignature(signature);
            map.put(signature, result);
        }
        return result;
    }

    private MaxSignature(RiSignature signature) {
        this.riSignature = signature;
    }

    @Override
    public int getParameterCount(boolean receiver) {
        return riSignature.argumentCount(receiver);
    }

    @Override
    public JavaType getParameterType(int index, ResolvedJavaType accessingClass) {
        RiResolvedType accessingType = accessingClass == null ? null : MaxResolvedJavaType.getRiResolvedType(accessingClass);
        return MaxJavaType.get(riSignature.argumentTypeAt(index, accessingType));
    }

    @Override
    public Kind getParameterKind(int index) {
        return KindMap.toGraalKind(riSignature.argumentKindAt(index, false));
    }

    @Override
    public JavaType getReturnType(ResolvedJavaType accessingClass) {
        RiResolvedType accessingType = accessingClass == null ? null : MaxResolvedJavaType.getRiResolvedType(accessingClass);
        return MaxJavaType.get(riSignature.returnType(accessingType));
    }

    @Override
    public Kind getReturnKind() {
        return KindMap.toGraalKind(riSignature.returnKind(false));
    }

    @Override
    public int getParameterSlots(boolean withReceiver) {
        return riSignature.argumentSlots(withReceiver);
    }


}
