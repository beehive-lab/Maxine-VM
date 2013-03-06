/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import java.lang.reflect.*;

import com.oracle.graal.phases.*;
import com.sun.max.vm.*;


public class MaxGraalOptions {

    private static boolean optionsRegistered;

    /**
     * Hosted variant, options on command line.
     */
    static void initialize() {
        if (MaxineVM.isHosted() && !optionsRegistered) {
            VMOptions.addFieldOptions("-G:", GraalOptions.class, null);
            optionsRegistered = true;
        }
    }

    /**
     * VM extension variant, options in {@code argList}, no {@code -G:} prefix.
     * Comma separated list, with value args indicated by {@code arg=value}, boolean args by {@code -arg, +arg}.
     * @param args
     */
    static void initialize(String argList) {
        if (argList == null) {
            return;
        }
        String[] args = argList.split(",");
        for (int i = 0; i < args.length; i++) {
            Class<?> argClass = String.class;
            boolean booleanValue = false;
            String stringValue = null;
            String arg = args[i];
            String argName = null;
            if (arg.charAt(0) == '-' || arg.charAt(0) == '+') {
                argClass = boolean.class;
                argName = arg.substring(1);
                booleanValue = arg.charAt(0) == '+';
            } else {
                int eqx = arg.indexOf('=');
                if (eqx > 0) {
                    argName = arg.substring(0, eqx);
                    stringValue = arg.substring(eqx + 1);
                } else {
                    error("missing argument value: " + arg);
                }
            }
            Field field = findArg(argName);
            if (field == null) {
                error("Graal option: " + argName + " not found");
            }
            Class declaredArgClass = field.getType();
            if (declaredArgClass == boolean.class) {
                if (argClass != boolean.class) {
                    error("boolean value expected for: " + argName);
                }
                try {
                    field.set(null, booleanValue);
                } catch (IllegalAccessException ex) {
                    error("failed to set option: " + argName);
                }
            } else if (declaredArgClass == String.class) {
                if (argClass != String.class) {
                    error("String value expected for: " + argName);
                }
                try {
                    field.set(null, stringValue);
                } catch (IllegalAccessException ex) {
                    error("failed to set option: " + argName);
                }
            } else {
                error("unimplemented option type: " + argName);
            }
        }
    }

    private static void error(String msg) {
        System.err.println(msg);
        MaxineVM.exit(1);
    }

    private static Field findArg(String argName) {
        Field[] fields = GraalOptions.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals(argName)) {
                return field;
            }
        }
        return null;
    }
}
