/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.actor.member;


/**
 * As of Java 1.8 interfaces are allowed to define default method implementations.
 *
 * To deal with the vtable entries of the classes implementing interfaces with default methods we introduce proxy to
 * default methods, that essentially point to the default method
 */
public class ProxyToDefaultMethodActor extends VirtualMethodActor {

    public ProxyToDefaultMethodActor(InterfaceMethodActor interfaceMethodActor) {
        super(interfaceMethodActor.name, interfaceMethodActor.descriptor(), interfaceMethodActor.flags(),
              interfaceMethodActor.codeAttribute(), interfaceMethodActor.intrinsic());
    }

    @Override
    public boolean isProxyToDefault() {
        return true;
    }

}
