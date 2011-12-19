/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.method;

import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.object.TeleTargetMethod.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;

/**
 * A remote reference constrained to point only to data in object format
 * stored in a code cache region. The absolute origin of the object is delegated to
 * an instance of {@link TeleTargetMethod}, which reads those values from the
 * appropriate fields in the corresponding instance of {@link TargetMethod}.
 * <p>
 * There is no presumption in this implementation concerning the possibility
 * that the locations stored in the {@TargetMethod} might change.
 *
 * @see TargetMethod
 */
abstract class AbstractCodeCacheRemoteReference extends RemoteTeleReference {

    private final TeleTargetMethod teleTargetMethod;
    private final CodeCacheReferenceKind kind;

    public AbstractCodeCacheRemoteReference(TeleVM vm, TeleTargetMethod teleTargetMethod, CodeCacheReferenceKind kind) {
        super(vm);
        this.teleTargetMethod = teleTargetMethod;
        this.kind = kind;
    }

    @Override
    public Address raw() {
        return teleTargetMethod.codeCacheObjectOrigin(kind);
    }

    @Override
    public boolean isLive() {
        return memoryStatus() == ObjectMemoryStatus.LIVE;
    }

    protected TeleTargetMethod teleTargetMethod() {
        return teleTargetMethod;
    }

}
