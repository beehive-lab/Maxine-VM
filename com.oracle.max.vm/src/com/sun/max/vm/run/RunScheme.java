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
package com.sun.max.vm.run;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;

/**
 * The {@code RunScheme} interface defines what the VM is configured to execute
 * after it has started its basic services and is ready to set up and run a language
 * environment, e.g. Java program.
 */
public interface RunScheme extends VMScheme {

    /**
     * The run method for the main Java thread.
     */
    void run() throws Throwable;

    /**
     * While bootstrapping, gather static native methods in JDK classes that need to be re-executed at target startup.
     * Typically, such are methods called "initIDs" in JDK classes and
     * they assign JNI method and field IDs to static C/C++ variables.
     *
     * Note that this method may be called numerous times during the bootstrapping phase and so the data structure
     * maintained by this run scheme to record the methods should take this into account.
     *
     * @return the set of methods gathered
     */
    @HOSTED_ONLY
    List<? extends MethodActor> gatherNativeInitializationMethods();

    /**
     * At target startup, run the above methods.
     */
    void runNativeInitializationMethods();
}
