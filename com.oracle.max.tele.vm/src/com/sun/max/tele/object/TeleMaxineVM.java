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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.vm.*;


/**
 * Local surrogate for the distinguished object int the VM that represents the VM itself.
 *
 * @see MaxineVM
 */
public final class TeleMaxineVM extends TeleTupleObject {

    TeleVMConfiguration teleVMConfiguration;

    public TeleMaxineVM(TeleVM vm, RemoteReference reference) {
        super(vm, reference);
    }

    /**
     * @return the VM object that holds configuration information for the particular VM build.
     */
    public TeleVMConfiguration teleVMConfiguration() {
        if (teleVMConfiguration == null) {
            final RemoteReference configReference = fields().MaxineVM_config.readReference(reference());
            teleVMConfiguration = (TeleVMConfiguration) objects().makeTeleObject(configReference);
        }
        return teleVMConfiguration;
    }

    @Override
    public String maxineRole() {
        return "VM global context";
    }

}
