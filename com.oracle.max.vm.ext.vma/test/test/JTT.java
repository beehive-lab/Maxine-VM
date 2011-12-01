/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package test;

import java.lang.reflect.*;

/**
 * A reflective way to invoke any of the tests in jtt.*, e.g., all the bytecode tests.
 */
public class JTT {

    public static void main(String[] args) throws Exception {
        // usage: testclass [args]
        String jttClassName = "jtt.bytecode." + args[0];
        Class<?> jttClass = Class.forName(jttClassName);
        Method[] methods = jttClass.getDeclaredMethods();
        Method jttTest = null;
        for (Method m : methods) {
            if (m.getName().equals("test")) {
                jttTest = m;
                break;
            }
        }
        Class<?>[] params = jttTest.getParameterTypes();
        Object[] oArgs = new Object[params.length];
        for (int p = 0; p < params.length; p++) {
            Class<?> pClass = params[p];
            String arg = args[p + 1];
            Object oArg = arg;
            if (pClass == int.class) {
                oArg = Integer.parseInt(arg);
            } else if (pClass == long.class) {
                oArg = Long.parseLong(arg);
            } else {
                assert false;
            }
            oArgs[p] = oArg;
        }
        jttTest.invoke(null, oArgs);
    }

}
