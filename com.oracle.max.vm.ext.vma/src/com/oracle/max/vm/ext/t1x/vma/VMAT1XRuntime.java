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
package com.oracle.max.vm.ext.t1x.vma;

import static com.sun.max.vm.compiler.CallEntryPoint.*;

import com.oracle.max.vm.ext.t1x.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;

/**
 * A companion class to {@link T1XRuntime} that contains additional methods and variants needed by the VMA templates.
 */
@NEVER_INLINE
public class VMAT1XRuntime {
    public static VirtualMethodActor resolveSpecialMethod(ResolutionGuard.InPool guard) {
        return Snippets.resolveSpecialMethod(guard);
    }

    public static Address initializeStaticMethod(StaticMethodActor staticMethod) {
        Snippets.makeHolderInitialized(staticMethod);
        return Snippets.makeEntrypoint(staticMethod, BASELINE_ENTRY_POINT);
    }

    public static Address initializeSpecialMethod(VirtualMethodActor virtualMethod) {
        return Snippets.makeEntrypoint(virtualMethod, BASELINE_ENTRY_POINT);
    }

    public static StaticMethodActor resolveStaticMethod(ResolutionGuard.InPool guard) {
        return Snippets.resolveStaticMethod(guard);
    }

    public static Address selectInterfaceMethod(Object receiver, InterfaceMethodActor interfaceMethodActor) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
    }

    public static Address selectNonPrivateVirtualMethod(Object receiver, VirtualMethodActor virtualMethodActor) {
        return Snippets.selectNonPrivateVirtualMethod(receiver, virtualMethodActor).asAddress();
    }

}
