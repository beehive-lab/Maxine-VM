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
