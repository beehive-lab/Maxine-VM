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
 * Access to configuration information in the VM.
 *
 * @see VMConfiguration
 */
public final class TeleVMConfiguration extends TeleTupleObject {

    private TeleHeapScheme teleHeapScheme;
    private TeleLayoutScheme teleLayoutScheme;
    private TeleMonitorScheme teleMonitorScheme;
    private TeleReferenceScheme teleReferenceScheme;
    private TeleRunScheme teleRunScheme;

    public TeleVMConfiguration(TeleVM vm, RemoteReference reference) {
        super(vm, reference);
    }

    public TeleHeapScheme heapScheme() {
        if (teleHeapScheme == null) {
            final RemoteReference heapSchemeReference = fields().VMConfiguration_heapScheme.readRemoteReference(reference());
            teleHeapScheme = (TeleHeapScheme) objects().makeTeleObject(heapSchemeReference);
        }
        return teleHeapScheme;
    }

    public TeleLayoutScheme layoutScheme() {
        if (teleLayoutScheme == null) {
            final RemoteReference layoutSchemeReference = fields().VMConfiguration_layoutScheme.readRemoteReference(reference());
            teleLayoutScheme = (TeleLayoutScheme) objects().makeTeleObject(layoutSchemeReference);
        }
        return teleLayoutScheme;
    }

    public TeleMonitorScheme monitorScheme() {
        if (teleMonitorScheme == null) {
            final RemoteReference monitorSchemeReference = fields().VMConfiguration_monitorScheme.readRemoteReference(reference());
            TeleObject monitorScheme = objects().makeTeleObject(monitorSchemeReference);
            if (monitorScheme instanceof TeleMonitorScheme) {
                teleMonitorScheme = (TeleMonitorScheme) monitorScheme;
            }
        }
        return teleMonitorScheme;
    }

    public TeleReferenceScheme referenceScheme() {
        if (teleReferenceScheme == null) {
            final RemoteReference referenceSchemeReference = fields().VMConfiguration_referenceScheme.readRemoteReference(reference());
            TeleObject referenceScheme = objects().makeTeleObject(referenceSchemeReference);
            if (referenceScheme instanceof TeleReferenceScheme) {
                teleReferenceScheme = (TeleReferenceScheme) referenceScheme;
            }
        }
        return teleReferenceScheme;
    }

    public TeleRunScheme runScheme() {
        if (teleRunScheme == null) {
            final RemoteReference runSchemeReference = fields().VMConfiguration_runScheme.readRemoteReference(reference());
            TeleObject runScheme = objects().makeTeleObject(runSchemeReference);
            if (runScheme instanceof TeleRunScheme) {
                teleRunScheme = (TeleRunScheme) runScheme;
            }
        }
        return teleRunScheme;
    }

    @Override
    public String maxineRole() {
        return "VM configuration";
    }
}
