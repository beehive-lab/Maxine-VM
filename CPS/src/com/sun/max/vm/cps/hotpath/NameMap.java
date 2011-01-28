/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.hotpath;

import com.sun.max.collect.*;
import com.sun.max.vm.cps.hotpath.compiler.Console.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.cps.tir.pipeline.*;

public class NameMap {
    public static final Color CONSTANT_COLOR = Color.LIGHTTEAL;
    public static final Color LOCAL_COLOR = Color.LIGHTGREEN;
    public static final Color NESTED_LOCAL_COLOR = Color.LIGHTMAGENTA;

    public static final MapFunction<TirInstruction, String> COMPACT = new MapFunction<TirInstruction, String>() {
        public String map(TirInstruction from) {
            final String[] result = new String[1];
            from.accept(new TirInstructionAdapter() {
                @Override
                public void visit(TirLocal local) {
                    result[0] = Color.color(NameMap.LOCAL_COLOR, local.kind().typeDescriptor + "$" + local.slot());
                }

                @Override
                public void visit(TirNestedLocal local) {
                    result[0] = Color.color(NameMap.NESTED_LOCAL_COLOR, "@" + local.slot());
                }

                @Override
                public void visit(TirConstant constant) {
                    result[0] = Color.color(CONSTANT_COLOR, "#" + constant.value().toString());
                }

                @Override
                public void visit(TirBuiltinCall call) {
                    result[0] = call.builtin().executable.name.toString();
                }

                @Override
                public void visit(TirDirCall call) {
                    result[0] = call.method().name.toString();
                }

                @Override
                public void visit(TirInstruction instruction) {
                    result[0] = instruction.toString();
                }
            });
            return result[0];
        }
    };

    private static IdentityHashMapping<TirTree, String> treeNameMap = new IdentityHashMapping<TirTree, String>();

    public static String nameOf(TirTree tree) {
        String name = treeNameMap.get(tree);
        if (name == null) {
            final int number = treeNameMap.length();
            if (number < 'Z' - 'A') {
                name = String.valueOf((char) ('A' + number));
            } else {
                name = "T:" + number;
            }
            treeNameMap.put(tree, name);
        }
        return name;
    }

    private static IdentityHashMapping<TirTree, MapFunction<TirInstruction, String>> treeLabelMap = new IdentityHashMapping<TirTree, MapFunction<TirInstruction, String>>();

    public static MapFunction<TirInstruction, String> labelMap(TirTree tree) {
        return treeLabelMap.get(tree);
    }

    public static void updateLabelMap(TirTree tree, MapFunction<TirInstruction, String> labelMap) {
        treeLabelMap.put(tree, labelMap);
    }

    public static String nameOf(TirTree tree, TirInstruction instruction) {
        final MapFunction<TirInstruction, String> map = NameMap.labelMap(tree);
        if (map != null) {
            return map.map(instruction);
        }
        return instruction.toString();
    }
}
