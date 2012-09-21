/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;

import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.type.*;


/**
 * Access to the classes loaded in the VM, along with related information.
 */
public interface MaxClasses extends MaxEntity<MaxClasses> {


    /**
     * @return  {@link TypeDescriptor}s for all classes loaded in the VM.
     */
    Set<TypeDescriptor> typeDescriptors();

    /**
     * @return an ordered set of {@link TypeDescriptor}s for classes loaded in
     *         the VM, plus classes found on the class path.
     */
    Iterable<TypeDescriptor> loadableTypeDescriptors();

    /**
     * Updates the set of types that are available by scanning the class path. This
     * scan will be performed automatically the first time
     * {@link #loadableTypeDescriptors()} is called. However, it should also be
     * performed any time the set of classes available on the class path may
     * have changed.
     */
    void updateLoadableTypeDescriptorsFromClasspath();

    /**
     * @param id  Class ID of a {@link ClassActor} in the VM.
     * @return surrogate for the {@link ClassActor} in the VM, null if not known.
     * @see ClassActor
     */
    TeleClassActor findTeleClassActor(int id);

    /**
     * @param typeDescriptor A {@link TypeDescriptor} local to the inspection
     * @return surrogate for the equivalent {@link ClassActor} in the VM, null if not known.
     * @see ClassActor
     */
    TeleClassActor findTeleClassActor(TypeDescriptor typeDescriptor);

    /**
     * @param type a {@link Class} instance local to the inspection
     * @return surrogate for the equivalent {@link ClassActor} in the VM, null if not known.
     */
    TeleClassActor findTeleClassActor(Class type);

    /**
     * Does a particular class name refer to a special internal class that cannot
     * be present in the VM's ordinary class registry.
     *
     * @param name a class name
     * @return whether the class name refers to a special, unregistered class
     */
    boolean isUnregisteredClassName(String name);

    /**
     * Writes current statistics concerning inspection of the VM's heap.
     *
     * @param printStream stream to which to write
     * @param indent number of spaces to indent each line
     * @param verbose possibly write extended information when true
     */
    void printSessionStats(PrintStream printStream, int indent, boolean verbose);
}
