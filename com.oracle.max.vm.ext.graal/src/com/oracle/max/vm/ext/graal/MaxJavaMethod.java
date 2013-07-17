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
import com.sun.max.vm.actor.member.*;


public class MaxJavaMethod implements JavaMethod {

    protected RiMethod riMethod;

    @RESET
    private static ConcurrentHashMap<RiMethod, MaxJavaMethod> map;

    private static ConcurrentHashMap<RiMethod, MaxJavaMethod> getMap() {
        if (map == null) {
            map = new ConcurrentHashMap<RiMethod, MaxJavaMethod>();
        }
        return map;
    }


    public static MaxJavaMethod get(RiMethod riMethod) {
        MaxJavaMethod result = getMap().get(riMethod);
        if (result == null) {
            if (riMethod instanceof RiResolvedMethod) {
                result = new MaxResolvedJavaMethod((RiResolvedMethod) riMethod);
            } else {
                result = new MaxJavaMethod(riMethod);
            }
            map.put(riMethod, result);
        }
        return result;
    }

    public static RiMethod getRiMethod(JavaMethod javaMethod) {
        return ((MaxJavaMethod) javaMethod).riMethod;
    }

    protected MaxJavaMethod(RiMethod riMethod) {
        this.riMethod = riMethod;
    }

    @Override
    public String getName() {
        if (this instanceof MaxResolvedJavaMethod) {
            MethodActor m = (MethodActor) riMethod;
            // When installing snippets the Fold annotation causes the method
            // to be looked by name in the Java class using standard reflection
            // in the Graal core. . LOCAL_SUBSTITUTION methods
            // have their name copied from the substitutee, which means, e.g.
            // that a Fold of MaxineVM.isHosted_ will actually invoke MaxineVM.isHosted,
            // which is quite wrong. So we fix that up here. It is not (yet) clear
            // whether it is safe to always do this or if we should restrict it
            // to snippet installation, or even to the fold itelf (latter requires Graal change).
            if (m.isLocalSubstitute() && (m.flags() & MethodActor.FOLD) != 0) {
                return riMethod.name() + "_";
            }
        }
        return riMethod.name();
    }

    @Override
    public JavaType getDeclaringClass() {
        return MaxJavaType.get(riMethod.holder());
    }

    @Override
    public Signature getSignature() {
        return MaxSignature.get(riMethod.signature());
    }

}
