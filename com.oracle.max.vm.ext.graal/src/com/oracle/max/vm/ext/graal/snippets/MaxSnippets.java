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
package com.oracle.max.vm.ext.graal.snippets;

import com.oracle.graal.api.code.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.snippets.MaxReplacementsImpl.MaxSnippetGraphBuilderConfiguration;
import com.oracle.max.vm.ext.graal.snippets.amd64.*;
import com.oracle.max.vm.ext.graal.stubs.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;

/**
 * Installs all the snippets used by Maxine.
 */
@HOSTED_ONLY
public class MaxSnippets {
    public static MaxReplacementsImpl initialize(MaxRuntime runtime) {
        // Snippets cannot have optimistic assumptions.
        Assumptions assumptions = new Assumptions(false);
        MaxRegisterConfig.initialize(runtime.maxTargetDescription().arch);
        MaxForeignCallsMap.initialize(runtime);
        MaxSnippetGraphBuilderConfiguration maxSnippetGraphBuilderConfiguration = new MaxSnippetGraphBuilderConfiguration();
        MaxConstantPool.setGraphBuilderConfig(maxSnippetGraphBuilderConfiguration);
        MaxReplacementsImpl maxReplacementsImpl = new MaxReplacementsImpl(runtime, assumptions, runtime.maxTargetDescription(),
                        maxSnippetGraphBuilderConfiguration);
        MaxIntrinsics.initialize(runtime, maxReplacementsImpl, runtime.maxTargetDescription());
        maxReplacementsImpl.installAndRegisterSnippets(TestSnippets.class);
        maxReplacementsImpl.installAndRegisterSnippets(NewSnippets.class);
        maxReplacementsImpl.installAndRegisterSnippets(FieldSnippets.class);
        maxReplacementsImpl.installAndRegisterSnippets(ArraySnippets.class);
        maxReplacementsImpl.installAndRegisterSnippets(TypeSnippets.class);
        // MonitorSnippets do the null check explicitly, so we don't want the normal explicit null check
        // in the scheme implementation.
        boolean explicitNullChecks = VMConfiguration.vmConfig().monitorScheme().setExplicitNullChecks(false);
        maxReplacementsImpl.installAndRegisterSnippets(MonitorSnippets.class);
        VMConfiguration.vmConfig().monitorScheme().setExplicitNullChecks(explicitNullChecks);
        maxReplacementsImpl.installAndRegisterSnippets(MaxInvokeLowerings.class);
        maxReplacementsImpl.installAndRegisterSnippets(MaxMiscLowerings.class);
        maxReplacementsImpl.installAndRegisterSnippets(ArithmeticSnippets.class);
        maxReplacementsImpl.installAndRegisterSnippets(AMD64ConvertSnippetsWrapper.class);
        maxReplacementsImpl.installAndRegisterSnippets(BoxingSnippetsWrapper.class);
        maxReplacementsImpl.installAndRegisterSnippets(NativeStubSnippets.class);

        MaxConstantPool.setGraphBuilderConfig(null);

        MaxUnsafeAccessLowerings.registerLowerings(runtime.lowerings());

        return maxReplacementsImpl;
    }



}
