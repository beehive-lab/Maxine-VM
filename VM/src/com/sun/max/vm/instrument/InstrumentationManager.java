/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.instrument;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.net.*;
import java.security.ProtectionDomain;
import java.util.*;

import com.sun.max.program.*;

/**
 * Maxine support for java.lang.instrument.*.
 *
 * @author Mick Jordan
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
     * Returns an instance of the class that implements the @see java.lang.instrument.Instrumentation interface, creating it if necessary.
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
                ProgramError.unexpected("failed to instantatiate sun.instrument.InstrumentationImpl: " + ex);
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
            ProgramError.unexpected("failed to invoke sun.instrument.InstrumentationImpl.transform:" + ex);
            return null;
        }
    }

}
