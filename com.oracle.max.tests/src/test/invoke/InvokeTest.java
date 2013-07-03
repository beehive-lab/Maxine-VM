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
package test.invoke;

import java.lang.reflect.*;


public class InvokeTest {

    /**
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("invokee missing");
        }

        Method testMethod = findMethod(args[0], "test");

        Class<?>[] params = testMethod.getParameterTypes();
        Object[] callArgs = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            String argI = args[i + 1];
            Class<?> param = params[i];
            if (param == boolean.class) {
                callArgs[i] = Boolean.parseBoolean(argI);
            } else if (param == byte.class) {
                callArgs[i] = Byte.parseByte(argI);
            } else if (param == char.class) {
                callArgs[i] = argI.charAt(0);
            } else if (param == int.class) {
                callArgs[i] = Integer.parseInt(argI);
            } else if (param == long.class) {
                callArgs[i] = Long.parseLong(argI);
            } else if (param == String.class) {
                callArgs[i] = argI;
            } else if (param == Object.class) {
                callArgs[i] = argI;
            } else if (Object.class.isAssignableFrom(param)) {
                callArgs[i] = Class.forName(argI).newInstance();
            } else if (param == float.class) {
                callArgs[i] = Float.parseFloat(argI);
            } else if (param == double.class) {
                callArgs[i] = Double.parseDouble(argI);
            }
        }

        Object result = testMethod.invoke(null, callArgs);
        System.out.println("Result: " + result);
    }

    private static Method findMethod(String className, String methodName) throws Exception {
        Class<?> testClass = Class.forName(className);
        Method[] methods = testClass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new Exception("method: " + methodName + " not found");
    }

}
