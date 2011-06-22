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
package com.sun.max.vm;

import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;

/**
 */
public abstract class AbstractVMScheme {

    public final String name;
    private final Class<? extends VMScheme> specification;

    protected AbstractVMScheme() {
        name = getClass().getSimpleName();
        Class<? extends VMScheme> specification = null;
        Class<?> implementation = getClass();
        ProgramError.check(VMScheme.class.isAssignableFrom(implementation), "Subclass of " + AbstractVMScheme.class + " must implement " + VMScheme.class + ": " + implementation);
        final Class<Class<? extends VMScheme>> type = null;
        Class<? extends VMScheme> last = Utils.cast(type, implementation);
        while (!implementation.equals(AbstractVMScheme.class) && specification == null) {
            for (Class<?> interfaceClass : implementation.getInterfaces()) {
                if (!VMScheme.class.equals(interfaceClass) && VMScheme.class.isAssignableFrom(interfaceClass)) {
                    specification = Utils.cast(type, interfaceClass);
                    break;
                }
            }
            implementation = implementation.getSuperclass();
            if (!VMScheme.class.equals(implementation) && VMScheme.class.isAssignableFrom(implementation)) {
                last = Utils.cast(type, implementation);
            }
        }
        if (specification == null) {
            specification = last;
        }
        ProgramError.check(specification != null, "Cannot find specification for scheme implemented by " + getClass());
        this.specification = specification;
    }

    public Class<? extends VMScheme> specification() {
        return specification;
    }

    public void initialize(MaxineVM.Phase phase) {
        // default: do nothing.
    }

    public String name() {
        return name;
    }

    public String about() {
        return getClass().getName();
    }

    /**
     * Gets a configuration value from either a given system property or a command line option.
     * Each source is probed until a non-null value is obtained.
     *
     * @param property a system property defining the configuration value
     * @param option an option defining the configuration value
     * @param aliasMap if non {@code null}, then this is used to try and resolve the value as an alias
     */
    @HOSTED_ONLY
    public static String configValue(String property, Option<String> option, Map<String, String> aliasMap) {
        String value = null;
        if (property != null) {
            value =  System.getProperty(property);
        }
        if (value == null && option != null) {
            value = option.getValue();
        }
        if (value == null) {
            return value;
        }
        if (aliasMap != null && aliasMap.containsKey(value)) {
            return aliasMap.get(value);
        }
        return value;
    }

    public Properties properties() {
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
