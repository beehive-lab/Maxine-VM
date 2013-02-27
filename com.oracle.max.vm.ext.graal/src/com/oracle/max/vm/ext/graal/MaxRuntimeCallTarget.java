/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;

class MaxRuntimeCallTarget implements RuntimeCallTarget {
    private final MaxRuntimeCall descriptor;
    private final CallingConvention callingConvention;

    @HOSTED_ONLY
    MaxRuntimeCallTarget(MaxRuntimeCall descriptor) {
        this.descriptor = descriptor;
        RegisterConfig registerConfig = MaxRegisterConfig.get(MaxineVM.vm().registerConfigs.standard);
        JavaType resType = MaxRuntimeCallsMap.runtime.lookupJavaType(descriptor.getResultType());
        JavaType[] argTypes = MetaUtil.lookupJavaTypes(MaxRuntimeCallsMap.runtime, descriptor.getArgumentTypes());
        this.callingConvention = registerConfig.getCallingConvention(CallingConvention.Type.RuntimeCall, resType, argTypes,
                        MaxRuntimeCallsMap.runtime.getTarget(), false);
    }

    @Override
    public CallingConvention getCallingConvention() {
        return callingConvention;
    }

    @Override
    public long getMaxCallTargetOffset() {
        // TODO Check
        return -1;
    }

    @Override
    public Descriptor getDescriptor() {
        return descriptor;
    }

    public MethodActor getMethodActor() {
        return descriptor.getMethodActor();
    }

}
