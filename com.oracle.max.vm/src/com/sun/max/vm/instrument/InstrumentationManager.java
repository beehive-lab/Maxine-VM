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
package com.sun.max.vm.instrument;

import java.lang.instrument.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;

import com.sun.max.program.*;

/**
 * Maxine support for java.lang.instrument.*.
 */

public class InstrumentationManager {

    private static Instrumentation instrumentation;
    private static Method transformMethod;
    /*
     * Meta-circularity can cause runaway recursion and initialization ordering problems when loading
     * an agent that does class transformation, if we naively call transform on the agent classes
     * as they are loaded. Our current solution is simply to disable transformation while loading the agent classes.
     */
    private static Set<URL> registeredAgents = Collections.synchronizedSet(new HashSet<URL>());

    /**
     * Returns the Instrumentation instance or null if it has not yet been created by an agent invocation.
     * Can be used to check if any instrumentation is active.
     * @return Instrumentation instance or null
     */
    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     * Returns an instance of the class that implements the {@link Instrumentation} interface, creating it if necessary.
     * @return Instrumentation instance
     */
    public static Instrumentation createInstrumentation() {
        if (instrumentation == null) {
            try {
                final Class<?> klass = Class.forName("sun.instrument.InstrumentationImpl");
                final Constructor<?> cons = klass.getDeclaredConstructor(long.class, boolean.class, boolean.class);
                cons.setAccessible(true);
                transformMethod = klass.getDeclaredMethod("transform", ClassLoader.class, String.class, Class.class, ProtectionDomain.class, byte[].class, boolean.class);
                transformMethod.setAccessible(true);
                instrumentation = (Instrumentation) cons.newInstance(0, true, true);
            } catch (Exception ex) {
                throw ProgramError.unexpected("failed to instantatiate sun.instrument.InstrumentationImpl: " + ex);
            }
        }
        return instrumentation;
    }

    public static void registerAgent(URL url) {
        registeredAgents.add(url);
    }

    public static byte[] transform(ClassLoader loader, String classname, Class classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer, boolean isRetransformer) {
        if (protectionDomain != null && registeredAgents.contains(protectionDomain.getCodeSource().getLocation())) {
            return null;
        }
        try {
            final byte[] result = (byte[]) transformMethod.invoke(instrumentation, loader, classname, classBeingRedefined, protectionDomain, classfileBuffer, isRetransformer);
            return result;
        } catch (Exception ex) {
            throw ProgramError.unexpected("failed to invoke sun.instrument.InstrumentationImpl.transform:" + ex);
        }
    }

}
