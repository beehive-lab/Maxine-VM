/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele;

import com.sun.max.tele.data.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.CodeLocation.CodeLocationFactory;
import com.sun.max.tele.method.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.type.*;

/**
 * Convenience methods for objects that refer to state in the VM.
 */
public abstract class AbstractVmHolder implements TeleVMAccess {

    private final TeleVM vm;

    private final String tracePrefix;

    /**
     * @return default prefix text for trace messages; identifies the class being traced.
     */
    protected String tracePrefix() {
        return tracePrefix;
    }

    protected AbstractVmHolder(TeleVM vm) {
        this.vm = vm;
        this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
    }

    public final TeleVM vm() {
        return vm;
    }

    public final VmClassAccess classes() {
        return vm.classes();
    }

    public final VmMemoryAccess memory() {
        return vm.memory();
    }

    public final VmReferenceManager referenceManager() {
        return vm.referenceManager();
    }

    public final VmFieldAccess fields() {
        return vm.fields();
    }

    public final VmObjectAccess objects() {
        return vm.objects();
    }

    public final VmHeapAccess heap() {
        return vm.heap();
    }

    public final VmCodeCacheAccess codeCache() {
        return vm.codeCache();
    }

    public final CodeLocationFactory codeLocationFactory() {
        return vm.codeLocationFactory();
    }

    public final VmBreakpointManager breakpointManager() {
        return vm.breakpointManager();
    }

    public final VmWatchpoint.WatchpointManager watchpointManager() {
        return vm.watchpointManager();
    }
}
