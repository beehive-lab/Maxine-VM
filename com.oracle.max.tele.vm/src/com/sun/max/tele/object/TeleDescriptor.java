/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.type.*;

/**
 * Canonical surrogate for a {@link Descriptor} in the VM.
 */
public abstract class TeleDescriptor extends TeleTupleObject {

    protected TeleDescriptor(TeleVM vm, RemoteReference descriptorReference) {
        super(vm, descriptorReference);
    }

    private Descriptor descriptor;

    /**
     * @return local equivalent of the {@link TypeDescriptor} in the VM.
     */
    public Descriptor descriptor() {
        if (descriptor == null) {
            final RemoteReference stringReference = fields().Descriptor_string.readRemoteReference(reference());
            final TeleString teleString = (TeleString) objects().makeTeleObject(stringReference);
            String string = teleString.getString();
            if (string != null) {
                if (this instanceof TeleTypeDescriptor) {
                    descriptor = JavaTypeDescriptor.parseTypeDescriptor(string);
                } else {
                    assert this instanceof TeleSignatureDescriptor;
                    descriptor = SignatureDescriptor.create(string);
                }
            }
        }
        return descriptor;
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return descriptor();
    }


    @Override
    public boolean hasTextualVisualization() {
        return true;
    }

    @Override
    public String textualVisualization() {
        final String stringValue = descriptor().string;
        return stringValue == null ? "<null>" : stringValue;
    }

}
