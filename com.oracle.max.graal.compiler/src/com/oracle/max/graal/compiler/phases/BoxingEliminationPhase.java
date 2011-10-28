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
package com.oracle.max.graal.compiler.phases;

import java.util.*;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.sun.cri.ri.*;

public class BoxingEliminationPhase extends Phase {

    private static final HashMap<RiRuntime, Set<RiMethod>> boxingMethodsMap = new HashMap<RiRuntime, Set<RiMethod>>();
    private static final HashMap<RiRuntime, Set<RiMethod>> unboxingMethodsMap = new HashMap<RiRuntime, Set<RiMethod>>();
    private final RiRuntime runtime;

    public BoxingEliminationPhase(GraalContext context, RiRuntime runtime) {
        super(context);
        this.runtime = runtime;
    }

    @Override
    protected void run(Graph<EntryPointNode> graph) {
        // TODO: Implement
    }

    public static synchronized boolean isUnboxingMethod(RiRuntime runtime, RiMethod method) {
        if (!unboxingMethodsMap.containsKey(runtime)) {
            unboxingMethodsMap.put(runtime, createUnboxingMethodSet(runtime));
        }
        return unboxingMethodsMap.get(runtime).contains(method);
    }

    public static synchronized boolean isBoxingMethod(RiRuntime runtime, RiMethod method) {
        if (!boxingMethodsMap.containsKey(runtime)) {
            boxingMethodsMap.put(runtime, createBoxingMethodSet(runtime));
        }
        return boxingMethodsMap.get(runtime).contains(method);
    }

    private static Set<RiMethod> createBoxingMethodSet(RiRuntime runtime) {
        Set<RiMethod> boxingMethods = new HashSet<RiMethod>();
        try {
            boxingMethods.add(runtime.getRiMethod(Boolean.class.getDeclaredMethod("valueOf", Boolean.TYPE)));
            boxingMethods.add(runtime.getRiMethod(Byte.class.getDeclaredMethod("valueOf", Byte.TYPE)));
            boxingMethods.add(runtime.getRiMethod(Character.class.getDeclaredMethod("valueOf", Character.TYPE)));
            boxingMethods.add(runtime.getRiMethod(Short.class.getDeclaredMethod("valueOf", Short.TYPE)));
            boxingMethods.add(runtime.getRiMethod(Integer.class.getDeclaredMethod("valueOf", Integer.TYPE)));
            boxingMethods.add(runtime.getRiMethod(Long.class.getDeclaredMethod("valueOf", Long.TYPE)));
            boxingMethods.add(runtime.getRiMethod(Float.class.getDeclaredMethod("valueOf", Float.TYPE)));
            boxingMethods.add(runtime.getRiMethod(Double.class.getDeclaredMethod("valueOf", Double.TYPE)));
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return boxingMethods;
    }

    private static Set<RiMethod> createUnboxingMethodSet(RiRuntime runtime) {
        Set<RiMethod> unboxingMethods = new HashSet<RiMethod>();
        try {
            unboxingMethods.add(runtime.getRiMethod(Boolean.class.getDeclaredMethod("booleanValue")));
            unboxingMethods.add(runtime.getRiMethod(Byte.class.getDeclaredMethod("byteValue")));
            unboxingMethods.add(runtime.getRiMethod(Character.class.getDeclaredMethod("charValue")));
            unboxingMethods.add(runtime.getRiMethod(Short.class.getDeclaredMethod("shortValue")));
            unboxingMethods.add(runtime.getRiMethod(Integer.class.getDeclaredMethod("intValue")));
            unboxingMethods.add(runtime.getRiMethod(Long.class.getDeclaredMethod("longValue")));
            unboxingMethods.add(runtime.getRiMethod(Float.class.getDeclaredMethod("floatValue")));
            unboxingMethods.add(runtime.getRiMethod(Double.class.getDeclaredMethod("doubleValue")));
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return unboxingMethods;
    }
}
