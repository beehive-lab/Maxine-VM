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


public class MaxJavaField implements JavaField {

    private static ConcurrentHashMap<RiField, MaxJavaField> map = new ConcurrentHashMap<RiField, MaxJavaField>();

    protected RiField riField;

    static MaxJavaField get(RiField riField) {
        MaxJavaField result = map.get(riField);
        if (result == null) {
            if (riField instanceof RiResolvedField) {
                result = new MaxResolvedJavaField((RiResolvedField) riField);
            } else {
                result = new MaxJavaField(riField);
            }
            map.put(riField, result);
        }
        return result;
    }

    public static RiField getRiField(JavaField javaField) {
        return ((MaxJavaField) javaField).riField;
    }

    protected MaxJavaField(RiField riField) {
        this.riField = riField;
    }

    @Override
    public String getName() {
        return riField.name();
    }

    @Override
    public JavaType getType() {
        return MaxJavaType.get(riField.type());
    }

    @Override
    public Kind getKind() {
        return KindMap.toGraalKind(riField.kind(false));
    }

    @Override
    public JavaType getDeclaringClass() {
        return MaxJavaType.get(riField.holder());
    }

}
