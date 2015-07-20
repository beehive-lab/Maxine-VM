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
package com.oracle.max.vma.tools.gen.vma.runtime;

import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.graal.phases.*;
import com.oracle.max.vma.tools.gen.vma.*;


public class VMAPhaseGenerator {
//    private static class
    private static Map<String, EnumSet<AdviceMode>> map = new HashMap<>();

    public static void main(String[] args) throws Exception {
        createGenerator(VMAPhaseGenerator.class);
        generateAutoComment();

        for (Method m : BytecodeAdvice.class.getDeclaredMethods()) {
            createMap(m);
        }
        out.println("    static {");
        for (Map.Entry<String, EnumSet<AdviceMode>> entry : map.entrySet()) {
            out.printf("        nodeMap.put(NodeClass.get(%s.class), %s);%n", entry.getKey(), adviceModeSetString(entry.getValue()));
        }
        out.println("    }");
        AdviceGeneratorHelper.updateSource(AdvicePhase.class, null, false);
    }

    private static String adviceModeSetString(EnumSet<AdviceMode> adviceModeSet) {
        String result = "AdviceMode.";
        if (adviceModeSet.contains(AdviceMode.BEFORE)) {
            result += "BEFORE";
            if (adviceModeSet.contains(AdviceMode.AFTER)) {
                result += "_AFTER";
            }
        } else if (adviceModeSet.contains(AdviceMode.AFTER)) {
            result += "AFTER";
        }
        return result + "_SET";
    }

    private static void createMap(Method m) {
        String name = m.getName();
        String nodeClassName = null;
        String unresolvedNodeClassName = null;
        if (name.contains("MethodEntry")) {
            nodeClassName = "StartNode";
        } else if (name.contains("PutField") || name.contains("PutStatic")) {
            nodeClassName = "StoreFieldNode";
            unresolvedNodeClassName = "UnresolvedStoreFieldNode";
        } else if (name.contains("GetField") || name.contains("GetStatic")) {
            nodeClassName = "LoadFieldNode";
            unresolvedNodeClassName = "UnresolvedLoadFieldNode";
        } else if (name.contains("ArrayLoad")) {
            nodeClassName = "LoadIndexedNode";
        } else if (name.contains("ArrayStore")) {
            nodeClassName = "StoreIndexedNode";
        } else if (name.contains("New")) {
            if (name.contains("Array")) {
                if (name.contains("Multi")) {
                    nodeClassName = "NewArrayNode";
                } else {
                    nodeClassName = "NewMultiArrayNode";
                }
            } else {
                nodeClassName = "NewInstanceNode";
            }
        } else if (name.contains("ArrayLength")) {
            nodeClassName = "ArrayLengthNode";
        } else if (name.contains("Invoke")) {
            nodeClassName = "InvokeWithExceptionNode";
        } else if (name.contains("Return")) {
            nodeClassName = "ReturnNode";
        } else if (name.contains("If")) {
            nodeClassName = "IfNode";
        }

        if (nodeClassName != null) {
            addNodeClass(name, nodeClassName);
        }
        if (unresolvedNodeClassName != null) {
            addNodeClass(name, unresolvedNodeClassName);
        }
    }

    private static void addNodeClass(String name, String nodeClassName) {
        EnumSet<AdviceMode> x = map.get(nodeClassName);
        if (x == null) {
            x = EnumSet.noneOf(AdviceMode.class);
            map.put(nodeClassName, x);
        }
        if (name.contains("Before")) {
            x.add(AdviceMode.BEFORE);
        } else if (name.contains("After")) {
            x.add(AdviceMode.AFTER);
        } else {
            assert false;
        }

    }
}
