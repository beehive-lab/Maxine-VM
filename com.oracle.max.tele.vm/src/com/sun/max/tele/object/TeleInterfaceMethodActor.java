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

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.vm.actor.member.*;

/**
 * Canonical surrogate for a {@link InterfaceMethodActor} in the {@link TeleVM}.
 */
public final class TeleInterfaceMethodActor extends TeleMethodActor {

    // Keep construction minimal for both performance and synchronization.
    protected TeleInterfaceMethodActor(TeleVM vm, RemoteReference interfaceMethodActorReference) {
        super(vm, interfaceMethodActorReference);
    }

    /**
     * @return A local {@link InterfaceMethodActor} corresponding the the {@link TeleVM}'s {@link InterfaceMethodActor} for the method.
     */
    public InterfaceMethodActor interfaceMethodActor() {
        return (InterfaceMethodActor) methodActor();
    }

    @Override
    public TeleCodeAttribute getTeleCodeAttribute() {
        return null;
    }

    @Override
    public String maxineRole() {
        return "InterfaceMethodActor";
    }

    public TargetMethodAccess[] getTargetMethods() {
        return new TargetMethodAccess[0];
    }

    public String getName() {
        return actorName().string;
    }

}
