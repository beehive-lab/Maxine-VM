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

import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.stack.*;


/**
 * Data describing a single compilation of a method, stub, adaptor, or other routine in the VM.
 */
public interface MaxCompilation extends MaxMachineCodeRoutine<MaxCompilation> {

    /**
     * @return {@code true} if this compilation is still runnable, {@code false} if evicted from the code cache.
     */
    boolean isCodeLive();

    /**
     * @return a single character designator that conveys useful information about this
     * particular compilation, and which distinguishes it from other compilations of the
     * same method.
     */
    String shortDesignator();

    /**
     * @return a short string designator that conveys useful information about this
     * particular compilation, and which distinguishes it from other compilations of the
     * same method.
     */
    String longDesignator();

    /**
     * Gets accessor to the method descriptor in the VM for this compilation.
     *
     * @return access to the {@link ClassMethodActor} for the machine code in the VM, if it was
     * compiled from a Java method; null otherwise.
     */
    TeleClassMethodActor getTeleClassMethodActor();

    /**
     * @return local instance of {@link ClassMethodActor} corresponding to the machine code
     * in the VM, if it was compiled from a Java method; null otherwise.
     */
    ClassMethodActor classMethodActor();

    /**
     * Gets the local instance of the class description for the object that represents this
     * compilation in the VM.
     *
     * @return a local descriptor of the type of the object representing this compilation
     * in the VM.
     */
    ClassActor classActorForObjectType();

    /**
     * Determines whether there is machine code in this compilation at a specified memory
     * location in the VM.
     *
     * @param address an absolute memory location in the VM.
     * @return whether there is machine code at the address
     * @throws IllegalArgumentException if the location is not within the code cache
     * memory allocated for this compilation.
     */
    boolean isValidCodeLocation(Address address) throws IllegalArgumentException;

    VMFrameLayout frameLayout();
}
